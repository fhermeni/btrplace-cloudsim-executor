package btrplace.cloudsim;

import org.cloudbus.cloudsim.*;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.power.PowerHost;
import org.cloudbus.cloudsim.power.models.PowerModelLinear;
import org.cloudbus.cloudsim.provisioners.BwProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.PeProvisionerSimple;
import org.cloudbus.cloudsim.provisioners.RamProvisionerSimple;

import java.util.*;


/**
 * @author Fabien Hermenier
 */
public class Run2 {

    private static ConstraintBroker cBroker;

    private static int nb;

    private static int nextCloudletId = 0;

    private static List<Vm> vms;

    private static List<Cloudlet> cloudlets;

    private static List<Vm> createVM(int userId, int vms) {
        LinkedList<Vm> list = new LinkedList<>();
        //VM Parameters
        long size = 10000; //image size (MB)
        int ram = 4096; //vm memory (MB)
        int mips = 3000;
        long bw = 1000;
        int pesNumber = 1; //number of cpus
        String vmm = "Xen"; //VMM name

        for(int i=0;i<vms;i++){
            Vm v = new Vm(i, userId, mips, pesNumber, ram, bw, size, vmm, new CloudletSchedulerTimeShared()) {
                public String toString() {
                    return "vm#" + getId();
                }
            };
            list.add(v);
        }
        return list;
    }

    private static List<PowerHost> createHosts(int qty, int nbCores, int mips, int ram, int storage, int bw) {
        List<PowerHost> nodes = new ArrayList<>();
        for (int i = 0; i < qty; i++) {
            List<Pe> pes = new ArrayList<>(nbCores);
            for (int x = 0; x < nbCores; x++) {
                pes.add(new Pe(x, new PeProvisionerSimple(mips)));
            }
            PowerModelLinear pwm = new PowerModelLinear(200, 0.5);
            PowerHost h = new MyPowerHost(nb++,
                    new RamProvisionerSimple(ram),
                    new BwProvisionerSimple(bw),
                    5,
                    storage,
                    pes,
                    new VmSchedulerTimeSharedOverSubscription(pes),
                    pwm);
            nodes.add(h);
        }
        return nodes;
    }

    private static BtrpDatacenter2 createDatacenter(String name,List<PowerHost> hosts) throws Exception {
        String arch = "x86";      // system architecture
        String os = "Linux";          // operating system
        String vmm = "Xen";
        double time_zone = 10.0;         // time zone this resource located
        double cost = 3.0;              // the cost of using processing in this resource
        double costPerMem = 0.05;               // the cost of using memory in this resource
        double costPerStorage = 0.1;    // the cost of using storage in this resource
        double costPerBw = 0.1;                 // the cost of using bw in this resource
        LinkedList<Storage> storageList = new LinkedList<Storage>();    //we are not adding SAN devices by now

        DatacenterCharacteristics characteristics = new DatacenterCharacteristics(
                arch, os, vmm, hosts, time_zone, cost, costPerMem, costPerStorage, costPerBw);
        return new BtrpDatacenter2(name, characteristics, storageList, 10);
    }

    private static void brokerStatistics() {
        System.out.println("-- Constraint Broker statistics --");
        for (ConstraintStatus st : cBroker.listStatus()) {
            System.out.println(st);
        }
    }

    /**
     * Creates main() to run this example
     */
    public static void main(String[] args) {
        try {
            // First step: Initialize the CloudSim package. It should be called
            // before creating any entities.
            int num_user = 1;   // number of grid users
            Calendar calendar = Calendar.getInstance();
            boolean trace_flag = false;  // mean trace events

            // Initialize the CloudSim library
            CloudSim.init(num_user, calendar, trace_flag, 0.1);

            //Edel nodes
            //32Gb, 4581 CPU mark,
            List<PowerHost> c1 = createHosts(3, 4, 4581, 24000, 250000, 40000);
            BtrpDatacenter2 dc = createDatacenter("Datacenter", c1);

            cBroker = new ConstraintBroker("ConstraintBroker", dc);
            VmBroker2 vmBroker = new VmBroker2("VmBroker", cBroker);
            vms = createVM(cBroker.getId(),9);
            cloudlets = createCloudlets(vms);

            CloudSim.terminateSimulation(1000);

            //With the cBroker
            for (Vm v : vms) {
                vmBroker.submit(v, 100, 60, 3);
            }

            cBroker.submit(-1, 200, "offline", Collections.<Object>singletonList(c1.get(0)));
            Log.disable();
            CloudSim.startSimulation();
            CloudSim.stopSimulation();
            brokerStatistics();
        }
        catch (Exception e) {
            e.printStackTrace();
            Log.printLine("The simulation has been terminated due to an unexpected error");
        }
    }

    private static List<Cloudlet> createCloudlets(List<Vm> vms) {
        List<Cloudlet> res = new ArrayList<>();
        for (Vm v : vms) {
            Cloudlet cl = new Cloudlet(nextCloudletId++,
                    1,
                    v.getNumberOfPes(),
                    1,
                    1,
                    new UtilizationModelStochastic(System.currentTimeMillis()), //cpu
                    new UtilizationModelStochastic(System.currentTimeMillis()), //ram
                    new UtilizationModelStochastic(System.currentTimeMillis()) //bw
            );
            cl.setVmId(v.getId());
        }
        return res;
    }
}
