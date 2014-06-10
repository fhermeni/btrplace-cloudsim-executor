package btrplace.cloudsim;

import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.core.SimEntity;
import org.cloudbus.cloudsim.core.SimEvent;

/**
 * @author Fabien Hermenier
 */
public class MyEntity extends SimEntity {

    public MyEntity() {
        super("foo");
    }

    @Override
    public void startEntity() {
        Log.printLine("Start " + getName());
    }

    @Override
    public void processEvent(SimEvent e) {
        Log.printLine("Process " + e);

    }

    @Override
    public void shutdownEntity() {
        Log.printLine("Stop " + getName());
    }
}
