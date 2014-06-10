package btrplace.cloudsim.event;

import org.cloudbus.cloudsim.Vm;

/**
 * @author Fabien Hermenier
 */
public class VmSubmission {

    public Vm vm;

    public double walltime;

    public int maxRetries;

    public int nbRetries = 0;

    public double delay = 0;

    public double enqueueTime;
    public double at;

    public static enum Status {enqueued, waiting, running, destroyed, rejected}

    public Status status;

    public VmSubmission(Vm v, double et, double at, double w, int maxRetries) {
        enqueueTime = et;
        this.at = at;
        vm = v;
        walltime = w;
        this.maxRetries = maxRetries;
        status = Status.enqueued;
    }

    @Override
    public String toString() {
        return "VmSubmission{" +
                "vm=" + vm.getId() +
                ", walltime=" + walltime +
                ", maxRetries=" + maxRetries +
                ", nbRetries=" + nbRetries +
                ", delay=" + delay +
                ", enqueueTime=" + enqueueTime +
                ", at=" + at +
                ", status=" + status +
                '}';
    }
}
