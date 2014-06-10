package btrplace.cloudsim;

import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.VmAllocationPolicy;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * @author Fabien Hermenier
 */
public class BtrPlaceEntryPoint extends VmAllocationPolicy {

    private List<Object[]> buffer;

    private BtrPlaceEntity btrpe;

    public BtrPlaceEntryPoint(List<Host> hosts) {
        super(hosts);
        buffer = new ArrayList<>();
    }

    @Override
    public boolean allocateHostForVm(Vm vm) {
        buffer.add(new Object[]{1, vm});
        return true;
    }

    @Override
    public void deallocateHostForVm(Vm vm) {
        buffer.add(new Object[]{-1, vm});
    }

    @Override
    public boolean allocateHostForVm(Vm vm, Host host) {
        Log.printLine("Allocate VM " + vm + " on " + host);
        return false;
    }

    @Override
    public List<Map<String, Object>> optimizeAllocation(List<? extends Vm> vms) {
        return null;
    }

    @Override
    public Host getHost(Vm vm) {
        return vm.getHost();
    }

    @Override
    public Host getHost(int i, int i2) {
        return null;
    }

    public List<Object[]> emptyBuffer() {
        List<Object[]> l = new ArrayList<>(buffer);
        buffer.clear();
        return l;
    }
}
