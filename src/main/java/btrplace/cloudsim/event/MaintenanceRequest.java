package btrplace.cloudsim.event;

import org.cloudbus.cloudsim.Host;

/**
 * @author Fabien Hermenier
 */
public class MaintenanceRequest {

    public Host host;

    public double walltime;

    public double delay = 0;

    public double enqueueTime;

    public double at;

    public boolean offline;

    public static enum Status {enqueued, waiting, committed, rejected}

    public Status status;

    public MaintenanceRequest(Host h, double et, double at, double w, boolean offline) {
        enqueueTime = et;
        this.at = at;
        host = h;
        walltime = w;
        status = Status.enqueued;
        this.offline = offline;
    }

    @Override
    public String toString() {
        return "MaintenanceRequest{" +
                "host=" + host.getId() +
                ", walltime=" + walltime +
                ", delay=" + delay +
                ", enqueueTime=" + enqueueTime +
                ", at=" + at +
                ", offline=" + offline +
                ", status=" + status +
                '}';
    }
}
