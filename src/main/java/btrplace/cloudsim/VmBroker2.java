package btrplace.cloudsim;

import org.cloudbus.cloudsim.BtrpDatacenter2;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.CloudSimTags;
import org.cloudbus.cloudsim.core.SimEntity;
import org.cloudbus.cloudsim.core.SimEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Broker that support Service or time-bounded VMs with the possibility to re-submit on failure.
 * @author Fabien Hermenier
 */
public class VmBroker2 extends SimEntity {

    private List<Object[]> initQueue;

    private Map<Integer, Double> walltimes;

    private Map<Integer, Integer> nbRetries;

    private double retryDelay = 60;

    private ConstraintBroker cBroker;

    private Logger logger = LoggerFactory.getLogger(getClass());
    /**
     * Created a new broker object.
     *
     * @param name name to be associated with this entity (as required by Sim_entity class from
     *             simjava package)
     * @throws Exception the exception
     */
    public VmBroker2(String name, ConstraintBroker cBroker) throws Exception {
        super(name);
        this.cBroker = cBroker;
        walltimes = new HashMap<>();
        nbRetries = new HashMap<>();
        initQueue = new ArrayList<>();
    }

    public void destroyNow(Vm v) {
        destroy(v, CloudSim.clock());
    }

    public void destroy(Vm v, double at) {
        cBroker.submit(getId(), at, "ready", Collections.<Object>singletonList(v));
    }

    public void submitNow(Vm v) {
        submitNow(v, -1, 0);
    }

    public void submitNow(Vm v, double w, int maxRetry ) {
        submit(v, CloudSim.clock(), w, maxRetry);
    }

    public void submit(Vm v, double at, double w, int maxRetry) {
        if (CloudSim.running()) {
            int cId = cBroker.submit(getId(), at, "running", Collections.<Object>singletonList(v));
            walltimes.put(cId, w);
            nbRetries.put(cId, maxRetry);
        } else {
            initQueue.add(new Object[]{v, at, w, maxRetry});
        }
    }

    /**
     * Processes events available for this Broker.
     *
     * @param ev a SimEvent object
     */
    @Override
    public void processEvent(SimEvent ev) {
        switch (ev.getTag()) {
            // Resource characteristics request
            case CloudSimTags.RESOURCE_CHARACTERISTICS_REQUEST:
                sendNow(getId(), CloudSimTags.RESOURCE_CHARACTERISTICS, getId());
                break;
            // Resource characteristics answer
            case CloudSimTags.RESOURCE_CHARACTERISTICS:
                for (Object [] o : initQueue) {
                    submit((Vm)o[0], (double)o[1], (double)o[2], (int)o[3]);
                }
                initQueue.clear();
                break;
            case BtrpDatacenter2.CSTR_STATUS:
                commitStatusUpdate((int) ev.getData());
                break;
            // if the simulation finishes
            case CloudSimTags.END_OF_SIMULATION:
                shutdownEntity();
                break;
            // other unknown tags are processed by this method
            default:
                break;
        }
    }

    private void commitStatusUpdate(int cId) {
        ConstraintStatus cs = cBroker.getStatus(cId);
        ConstraintStatus.Status st = cs.currentStatus();
        if (cs.getConstraintName().equals("running")) {
            if (st == ConstraintStatus.Status.satisfied) {
                //walltime ? -> revoke(running) and submit(ready)
                double w = walltimes.get(cId);
                if (w > 0) {
                    walltimes.put(cId, 0d); //Prevent from multiple revocations
                    cBroker.revoke(cId, w);
                    destroy((Vm) cs.getParams().iterator().next(), w);
                }
            } /*else if (st == ConstraintStatus.Status.violated) {
                cBroker.revokeNow(cId);
                if (nbRetries.get(cId) > 0) {
                    submit((Vm) cs.getParams().iterator().next(), retryDelay, walltimes.get(cId), nbRetries.get(cId) - 1);
                }
                //else , revoked considered as a rejection
            }   */
        }
    }

    @Override
    public void startEntity() {
        logger.debug(getName() + " is starting...");
        schedule(getId(), 0, CloudSimTags.RESOURCE_CHARACTERISTICS_REQUEST);
    }

    @Override
    public void shutdownEntity() {
        logger.debug(getName() + " is shutting down...");
    }

    public double getRetryDelay() {
        return retryDelay;
    }

    public void setRetryDelay(double retryDelay) {
        this.retryDelay = retryDelay;
    }
}
