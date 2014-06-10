package btrplace.cloudsim;

import org.cloudbus.cloudsim.Pe;
import org.cloudbus.cloudsim.VmScheduler;
import org.cloudbus.cloudsim.power.PowerHost;
import org.cloudbus.cloudsim.power.models.PowerModel;
import org.cloudbus.cloudsim.provisioners.BwProvisioner;
import org.cloudbus.cloudsim.provisioners.RamProvisioner;

import java.util.List;

/**
 * @author Fabien Hermenier
 */
public class MyPowerHost extends PowerHost {

    private boolean isOnline;

    private int offlineConsumption;

    public MyPowerHost(int id,
                                   RamProvisioner ramProvisioner,
                                   BwProvisioner bwProvisioner,
                                   int offlineConsumption,
                                   long storage,
                                   List<? extends Pe> peList,
                                   VmScheduler vmScheduler,
                                   PowerModel powerModel) {
        super(id, ramProvisioner, bwProvisioner, storage, peList, vmScheduler, powerModel);
        isOnline = true;
        this.offlineConsumption = offlineConsumption;
    }

    @Override
    protected double getPower(double utilization) {
        if (!isOnline) {
            return offlineConsumption;
        }
        if ((getVmList().isEmpty() && getVmsMigratingIn().isEmpty())
            || utilization == 0) {
            //idle,
            return getPowerModel().getPower(0.1); //Otherwise, it returns 0 :/
        }
        return super.getPower(utilization);
    }

    public void isOnline(boolean o) {
        isOnline = o;
    }

    public boolean isOnline() {
        return isOnline;
    }

    public String toString() {
        return "node#" + getId();
    }
}
