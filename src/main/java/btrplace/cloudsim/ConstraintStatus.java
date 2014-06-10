package btrplace.cloudsim;

import org.cloudbus.cloudsim.BtrpDatacenter2;
import org.cloudbus.cloudsim.core.CloudSim;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * @author Fabien Hermenier
 */
public class ConstraintStatus {

    public static enum Status {invalid, pending, satisfied, violated, revoked}

    public static class StatusHistory {

        private double date;

        private Status st;

        public StatusHistory(double d, Status s) {
            date = d;
            st = s;
        }

        public Status status() {
            return st;
        }

        public double date() {
            return date;
        }

        @Override
        public String toString() {
            return BtrpDatacenter2.clockFormat.format(date) + ":" + st;
        }
    }

    private int id;

    private int cbId;
    private List<StatusHistory> history;

    private String cName;

    private List<Object> params;

    public ConstraintStatus(int id, double at, int cbId, String cName, List<Object> params) {
        this.id = id;
        this.cbId = cbId;
        this.cName = cName;
        this.params = params;
        history = new ArrayList<>();
        history.add(new StatusHistory(at, Status.pending));
    }

    public int id() {
        return id;
    }

    public int callbackId() {
        return cbId;
    }

    public Status currentStatus() {
        return history.get(history.size() - 1).status();
    }

    public void updateStatus(Status st) {
        if (st != currentStatus()) {
            history.add(new StatusHistory(CloudSim.clock(), st));
        }
    }

    public List<StatusHistory> getHistory() {
        return Collections.unmodifiableList(history);
    }

    public String getConstraintName() {
        return cName;
    }

    public List<Object> getParams() {
        return params;
    }

    @Override
    public String toString() {
        return "(" + id  + ") " + cName + "(" + params + "): " + history;
    }
}
