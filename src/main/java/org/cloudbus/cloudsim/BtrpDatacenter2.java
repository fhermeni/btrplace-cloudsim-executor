package org.cloudbus.cloudsim;

import btrplace.cloudsim.BtrPlaceAllocationPolicy;
import btrplace.model.constraint.SatConstraint;
import btrplace.plan.ReconfigurationPlan;
import btrplace.plan.event.Action;
import btrplace.solver.SolverException;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.CloudSimTags;
import org.cloudbus.cloudsim.core.SimEvent;
import org.cloudbus.cloudsim.power.PowerDatacenter;
import org.cloudbus.cloudsim.power.PowerHost;
import org.cloudbus.cloudsim.util.ExecutionTimeMeasurer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Fabien Hermenier
 */
public class BtrpDatacenter2 extends PowerDatacenter {

    private boolean inReconfiguration = false;

    private boolean solveDelayed = false;

    /** The vm provisioner. */
    private BtrPlaceAllocationPolicy btrp;

    public static final int START_RECONFIGURATION = 201;

    public static final int START_RECONFIGURATION_APPLICATION = 208;

    public static final int RECONFIGURATION_ENDED = 202;

    public static final int CSTR_REVOKE = 300;

    public static final int CSTR_SUBMIT = 301;

    public static final int CHECK_CONSTRAINTS = 302;

    public static final int CSTR_STATUS = 303;

    public static final int START_ACTION = 304;

    public static final int END_ACTION = 305;
    private Map<Integer, SatConstraint> constraints;

    private Logger logger;

    private int cBrokerId;

    private DatacenterActuator actuator;
    /**
     * Allocates a new PowerDatacenter object.
     *
     * @param name the name to be associated with this entity (as required by Sim_entity class from
     *            simjava package)
     * @param characteristics an object of DatacenterCharacteristics
     * @param storageList a LinkedList of storage elements, for data simulation
     * @param  schedulingInterval minimum interval btw. two scheduling phase.
     * @throws Exception if an error occurs
     */
    public BtrpDatacenter2(
            String name,
            DatacenterCharacteristics characteristics,
            List<Storage> storageList,
            double schedulingInterval) throws Exception {
        super(name, characteristics, null, storageList, schedulingInterval);
        btrp = new BtrPlaceAllocationPolicy(getHostList());
        constraints = new HashMap<>();
        logger = LoggerFactory.getLogger(name);
        actuator = new DatacenterActuator(this, btrp);
    }

    /**
     * Processes events or services that are available for this PowerDatacenter.
     *
     * @param ev a Sim_event object
     */
    @Override
    public void processEvent(SimEvent ev) {
        switch (ev.getTag()) {
            case START_RECONFIGURATION:
                solve();
                checkPower();
                break;
            case START_RECONFIGURATION_APPLICATION:
                applyReconfiguration((ReconfigurationPlan) ev.getData());
                break;
            case RECONFIGURATION_ENDED:
                log("reconfiguration completed");
                inReconfiguration = false;
                if (solveDelayed) {
                    solveDelayed = false;
                    solve();
                } else {
                    send(getId(), getSchedulingInterval(), START_RECONFIGURATION);
                }
                break;
            case CSTR_SUBMIT:
                processConstraintSubmission(ev);
                break;
            case CSTR_REVOKE:
                processConstraintRevokation(ev);
                break;
            case CHECK_CONSTRAINTS:
                checkConstraints();
                break;
            case START_ACTION:
                log("starting  " + ev.getData());
                actuator.begin((Action) ev.getData());
                break;
            case END_ACTION:
                log("terminating  " + ev.getData());
                actuator.terminate((Action) ev.getData());
                break;
            default:
                super.processEvent(ev);
        }
    }

    private void checkConstraints() {
        log("Checking constraints");
        for (Map.Entry<Integer, SatConstraint> e : constraints.entrySet()) {
            SatConstraint c = e.getValue();
            int cId = e.getKey();
            checkConstraint(c, cId);
        }
    }

    private void checkConstraint(SatConstraint c, int cId) {
        boolean ok = c.isSatisfied(btrp.getModel());
        if (ok) {
            sendNow(cBrokerId, CSTR_STATUS, new int[]{cId, CloudSimTags.TRUE});
        } else {
            sendNow(cBrokerId, CSTR_STATUS, new int[]{cId, CloudSimTags.FALSE});
        }
    }

    private void checkPower() {
        for (PowerHost h : this.<PowerHost>getHostList()) {
            log(h.toString() + " " + h.getStateHistory() + " " + h.getPower());
        }
    }

    private void processConstraintSubmission(SimEvent ev) {
        if (constraints.isEmpty()) {
            send(getId(), getSchedulingInterval(), START_RECONFIGURATION);
        }
        Object [] data = (Object [])ev.getData();
        int bId = (int) data[0];
        int cId = (int) data[1];
        cBrokerId = bId;
        String cName = (String) data[2];
        List<Object> ps = (List<Object>) data[3];
        try {
            SatConstraint c = btrp.build(cName, ps);
            log("submitted " + c);
            checkConstraint(c, cId);
            constraints.put(cId, c);
        } catch (Exception e) {
            sendNow(bId, BtrpDatacenter2.CSTR_SUBMIT, new int[]{cId, CloudSimTags.FALSE});
            logger.error("{}: {}", getName(), e.getMessage());
        }
    }

    private void processConstraintRevokation(SimEvent ev) {
        int cId = (int)ev.getData();
        log("revoking " + constraints.remove(cId));
        sendNow(cBrokerId, BtrpDatacenter2.CSTR_REVOKE, new int[]{cId, CloudSimTags.TRUE});
    }

    private void solve() {
        if (inReconfiguration) {
            solveDelayed = true;
            log("Solving process delayed by a pending reconfiguration");
        } else {
            solveDelayed = false;
            log("start a solving process");
            try {
                ExecutionTimeMeasurer.start("btrp");
                ReconfigurationPlan p = btrp.solve(constraints.values());
                double d = ExecutionTimeMeasurer.end("btrp");
                if (p == null) {
                    log("No solution");
                    send(getId(), getSchedulingInterval(), START_RECONFIGURATION);
                } else {
                    if (p.getSize() > 0) {
                        send(getId(), d, START_RECONFIGURATION_APPLICATION, p);
                    } else {
                        send(getId(), getSchedulingInterval(), START_RECONFIGURATION);
                    }
                }
            } catch (SolverException ex) {
                log(ex.getMessage());
                ExecutionTimeMeasurer.end("btrp");
            }
        }
    }

    private void applyReconfiguration(ReconfigurationPlan p) {
        inReconfiguration = true;
        for (Action a : p) {
            send(getId(), a.getStart(), START_ACTION, a);
            send(getId(), a.getEnd() - CloudSim.getMinTimeBetweenEvents(), END_ACTION, a);
            send(getId(), a.getEnd(), CHECK_CONSTRAINTS);
        }
        send(getId(), p.getDuration(), RECONFIGURATION_ENDED);
    }

    public static DecimalFormat clockFormat = new DecimalFormat("###");

    private void log(String msg) {

        String str = String.format("%6s: %s", clockFormat.format(CloudSim.clock()), msg);
        logger.warn(str);
        //Log.printLine(str);
    }
}
