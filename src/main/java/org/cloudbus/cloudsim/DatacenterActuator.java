package org.cloudbus.cloudsim;

import btrplace.cloudsim.BtrPlaceAllocationPolicy;
import btrplace.cloudsim.MyPowerHost;
import btrplace.plan.event.*;
import org.cloudbus.cloudsim.core.CloudSim;

/**
 * @author Fabien Hermenier
 */
public class DatacenterActuator  {

    private BtrPlaceAllocationPolicy btrp;

    private BtrpDatacenter2 dc;

    public DatacenterActuator(BtrpDatacenter2 dc, BtrPlaceAllocationPolicy btrp) {
        this.btrp = btrp;
        this.dc = dc;
    }

    public void begin(Action a) {
        if (a instanceof BootVM) {
            begin((BootVM) a);
        } else if (a instanceof ShutdownVM) {
            begin((ShutdownVM) a);
        } else if (a instanceof MigrateVM) {
            begin((MigrateVM) a);
        } else if (a instanceof ShutdownNode) {
            begin((ShutdownNode) a);
        } else if (a instanceof BootNode) {
            begin((BootNode) a);
        }
        else {
            System.out.println("Unsupported action begin '" + a + "'");
            System.exit(1);
        }
    }

    public void terminate(Action a) {
        if (a instanceof BootVM) {
            terminate((BootVM) a);
        } else if (a instanceof ShutdownVM) {
            terminate((ShutdownVM) a);
        } else if (a instanceof MigrateVM) {
            terminate((MigrateVM) a);
        } else if (a instanceof ShutdownNode) {
            terminate((ShutdownNode) a);
        } else if (a instanceof BootNode) {
            terminate((BootNode) a);
        }
        else {
            System.out.println("Unsupported action terminate '" + a + "'");
            System.exit(1);

        }
    }

    public void begin(BootVM a) {
        Vm v = btrp.getVm(a.getVM());
        Host host = btrp.getHost(a.getDestinationNode());
        if (!host.vmCreate(v)) {
            Log.printLine(CloudSim.clock() + ": unable to create VM#" + v.getId());
        } else {
            if (!btrp.getModel().getMapping().addRunningVM(a.getVM(), a.getDestinationNode())) {
                System.exit(1);
            }
        }
    }

    public void terminate(BootVM a) {}

    /**
     * Process the event for an User/Broker who wants to migrate a VM. This PowerDatacenter will
     * then send the status back to the User/Broker.
     */
    public void begin(MigrateVM a) {
        Host src = btrp.getHost(a.getSourceNode());
        Host dst = btrp.getHost(a.getDestinationNode());
        Vm v = btrp.getVm(a.getVM());

        //TODO: fix System.exit(0);
        dst.addMigratingInVm(v);

        //Initiate the migration.
        //TODO: finer model
        /** VM migration delay = RAM / bandwidth **/
        // we use BW / 2 to model BW available for migration purposes, the other
        // half of BW is for VM communication
        // around 16 seconds for 1024 MB using 1 Gbit/s network
        //Basically, restrict to 1 migration at a time
        //double duration = v.getRam() / ((double) dst.getBw() / (2 * 8000));
        //Log.printLine(CloudSim.clock() + ": start migration of " + v.getId() + " from " + src.getId() + " to " + dst.getId());
    }


    public void terminate(MigrateVM a) {

        Vm vm = btrp.getVm(a.getVM());
        Host src = btrp.getHost(a.getSourceNode());
        Host dst = btrp.getHost(a.getDestinationNode());

        dst.removeMigratingInVm(vm);
        src.vmDestroy(vm);
        dst.vmCreate(vm);
        if (!btrp.getModel().getMapping().addRunningVM(btrp.getVm(vm), btrp.getHost(dst))) {
            System.exit(0);
        }
        vm.setInMigration(false);
    }

    public void begin(ShutdownNode a) {
        if (!btrp.getModel().getMapping().getRunningVMs(a.getNode()).isEmpty()) {
            System.out.println("Node " + a.getNode() + " is not idle");
            System.exit(1);
        }
    }

    public void terminate(ShutdownNode a) {
        MyPowerHost h = (MyPowerHost)btrp.getHost(a.getNode());
        if (!btrp.getModel().getMapping().addOfflineNode(a.getNode())) {
            Log.printLine(CloudSim.clock() + ": Unable to shutdown node#" + h.getId());
            System.exit(1);
        }
        h.isOnline(false);
    }

    public void begin(BootNode a) {
        MyPowerHost h = (MyPowerHost)btrp.getHost(a.getNode());
        if (!btrp.getModel().getMapping().addOnlineNode(a.getNode())) {
            Log.printLine(CloudSim.clock() + ": Unable to boot node#" + h.getId());
            System.exit(1);
        }
        h.isOnline(true);
        //TODO: power consumption while booting
    }

    public void terminate(BootNode a) {
        Host h = btrp.getHost(a.getNode());
        if (!btrp.getModel().getMapping().addOnlineNode(a.getNode())) {
            System.exit(1);
        }
    }

    public void begin(ShutdownVM a) {}
    public void terminate(ShutdownVM a) {
        Vm v = btrp.getVm(a.getVM());
        Host h = btrp.getHost(v);
        h.vmDeallocate(v);
        btrp.getModel().getMapping().addReadyVM(a.getVM());
    }

}
