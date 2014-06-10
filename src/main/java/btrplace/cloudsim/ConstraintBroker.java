package btrplace.cloudsim;

import org.cloudbus.cloudsim.BtrpDatacenter2;
import org.cloudbus.cloudsim.Datacenter;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.CloudSimTags;
import org.cloudbus.cloudsim.core.SimEntity;
import org.cloudbus.cloudsim.core.SimEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Broker that support submission of constraints to a datacenter controlled by BtrPlace.
 * @author Fabien Hermenier
 */
public class ConstraintBroker extends SimEntity {

    private List<ConstraintStatus> constraints;

    private List<Object[]> preQueued;
    private Datacenter dc;

    private Logger logger;
    /**
     * Created a new broker object.
     *
     * @param name name to be associated with this entity (as required by Sim_entity class from
     *             simjava package)
     * @throws Exception the exception
     */
    public ConstraintBroker(String name, Datacenter dc) throws Exception {
        super(name);
        constraints = new ArrayList<>();
        this.dc = dc;
        logger = LoggerFactory.getLogger(name);
        preQueued = new ArrayList<>();
    }


    public boolean revokeNow(int cId) {
        return revoke(cId, CloudSim.clock());
    }

    public boolean revoke(int cId, double at) {
        ConstraintStatus cs = constraints.get(cId);
        if (cs == null) {
            return false;
        }
        send(dc.getId(), at, BtrpDatacenter2.CSTR_REVOKE, cId);
        return true;
    }

    public int submitNow(int cbId, String cstrName, List<Object> params) {
        return submit(cbId, CloudSim.clock(), cstrName, params);
    }

    public int submit(int cbId, double at, String cstrName, List<Object> params) {
        if (CloudSim.running()) {
            ConstraintStatus c = new ConstraintStatus(constraints.size(), CloudSim.clock() + at, cbId, cstrName, params);
            constraints.add(c);
            //the simulation is running so emit to me an ENQUEUE event
            send(dc.getId(), at, BtrpDatacenter2.CSTR_SUBMIT, new Object[]{getId(), c.id(), cstrName, params});
            logger.debug("@" + at + ": submit " + c.getConstraintName() + "(" + c.getParams() + ") with id" + c.id());
            return c.id();
        } else {
            ConstraintStatus c = new ConstraintStatus(preQueued.size(), at, cbId, cstrName, params);
            preQueued.add(new Object[]{at, c});
            return c.id();
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
                processResourceCharacteristicsRequest(ev);
                break;
            case CloudSimTags.RESOURCE_CHARACTERISTICS:
                emptyPrequeued();
                break;
            // VM Creation answer
            case BtrpDatacenter2.CSTR_SUBMIT:
                commitSubmission(ev);
                break;
            case BtrpDatacenter2.CSTR_REVOKE:
                commitRevokation(ev);
                break;
            case BtrpDatacenter2.CSTR_STATUS:
                commitStatus(ev);
                break;
            case CloudSimTags.END_OF_SIMULATION:
                shutdownEntity();
                break;
            // other unknown tags are processed by this method
            default:
                logger.error("Unsupported event " + ev);
                break;
        }
    }

    private void commitStatus(SimEvent ev) {
        int [] data = (int[]) ev.getData();
        int cId = data[0];
        int res = data[1];
        ConstraintStatus c = constraints.get(cId);
        if (res == CloudSimTags.TRUE) {
            c.updateStatus(ConstraintStatus.Status.satisfied);
            sendNow(c.callbackId(), BtrpDatacenter2.CSTR_STATUS, cId);
        } else if (res == CloudSimTags.FALSE) {
            c.updateStatus(ConstraintStatus.Status.violated);
            sendNow(c.callbackId(), BtrpDatacenter2.CSTR_STATUS, cId);
        } else if (res == BtrpDatacenter2.CSTR_REVOKE) {
            c.updateStatus(ConstraintStatus.Status.revoked);
            sendNow(c.callbackId(), BtrpDatacenter2.CSTR_STATUS, cId);
        }
        //else: no change
    }

    private void emptyPrequeued() {
        for (Object [] o : preQueued) {
            double at = (double) o[0];
            ConstraintStatus c = (ConstraintStatus) o[1];
            submit(c.callbackId(), at, c.getConstraintName(), c.getParams());
        }
        preQueued.clear();
    }

    private void commitRevokation(SimEvent ev) {
        int [] data = (int[])ev.getData();
        int cId = data[0];
        if (data[1] == 1) {
            ConstraintStatus c = constraints.get(cId);
            c.updateStatus(ConstraintStatus.Status.revoked);
            logger.debug("@" + CloudSim.clock() + ": revoked " + c.getConstraintName() + "(" + c.getParams() + ") with id" + c.id());
        }

    }

    private void commitSubmission(SimEvent ev) {
        int [] data = (int[])ev.getData();
        int cId = data[0];
        ConstraintStatus c = constraints.get(cId);
        if (data[1] == 0) {
            constraints.get(cId).updateStatus(ConstraintStatus.Status.satisfied);
            logger.debug("@" + CloudSim.clock() + ": satisfied " + c.getConstraintName() + "(" + c.getParams() + ") with id" + c.id());
        } else if (data[1] == 1) {
            constraints.get(cId).updateStatus(ConstraintStatus.Status.invalid);
            logger.debug("@" + CloudSim.clock() + ": invalid " + c.getConstraintName() + "(" + c.getParams() + ") with id" + c.id());
        }
    }

    /**
     * Process a request for the characteristics of a PowerDatacenter.
     *
     * @param ev a SimEvent object
     */
    public void processResourceCharacteristicsRequest(SimEvent ev) {
        sendNow(dc.getId(), CloudSimTags.RESOURCE_CHARACTERISTICS, getId());
    }

    @Override
    public void shutdownEntity() {
        logger.debug(getName() + " is shutting down...");
    }

    @Override
    public void startEntity() {
        logger.debug(getName() + " is starting...");
        //Send the signal to create the VMs
        schedule(getId(), 0, CloudSimTags.RESOURCE_CHARACTERISTICS_REQUEST);
    }
    public ConstraintStatus getStatus(int cId) {
        return constraints.get(cId);
    }

    public List<ConstraintStatus> listStatus() {
        return constraints;
    }
}
