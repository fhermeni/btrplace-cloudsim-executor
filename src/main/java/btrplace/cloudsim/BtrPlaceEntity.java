package btrplace.cloudsim;

import btrplace.model.DefaultModel;
import btrplace.model.Model;
import btrplace.model.Node;
import btrplace.model.VM;
import btrplace.model.constraint.Ready;
import btrplace.model.constraint.Root;
import btrplace.model.constraint.Running;
import btrplace.model.constraint.SatConstraint;
import btrplace.model.view.ShareableResource;
import btrplace.plan.ReconfigurationPlan;
import btrplace.plan.event.Action;
import btrplace.plan.event.BootVM;
import btrplace.plan.event.ShutdownVM;
import btrplace.solver.SolverException;
import btrplace.solver.choco.ChocoReconfigurationAlgorithm;
import btrplace.solver.choco.DefaultChocoReconfigurationAlgorithm;
import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Log;
import org.cloudbus.cloudsim.Vm;
import org.cloudbus.cloudsim.core.CloudSim;
import org.cloudbus.cloudsim.core.CloudSimTags;
import org.cloudbus.cloudsim.core.SimEntity;
import org.cloudbus.cloudsim.core.SimEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Fabien Hermenier
 */
public class BtrPlaceEntity extends SimEntity {

    private ChocoReconfigurationAlgorithm cra;

    private ShareableResource ram;

    private ShareableResource mips;

    private ShareableResource bw;

    private Model model;

    private Map<Vm, VM> vmMapping;

    private Map<Host, Node> hostMapping;
    private Map<Node, Host> nodeMapping;

    private Map<Integer, Vm> intToVm;

    private static final String NAME = "BtrPlaceEntity";

    public static final int RECONFIGURATION_TERMINATION = 201;

    public static final int FORCED_RECONFIGURATION = 202;


    private int forcedReconfigurationDelay = 60;

    private BtrPlaceEntryPoint btrpEp;

    private List<Host> hosts;

    public BtrPlaceEntity(List<Host> hosts) {
        super(NAME);
        this.hosts = hosts;
        makeInitialModel();
    }

    private void makeInitialModel() {
        vmMapping = new HashMap<>();
        hostMapping = new HashMap<>();
        nodeMapping = new HashMap<>();
        intToVm  =new HashMap<>();
        cra = new DefaultChocoReconfigurationAlgorithm();
        model = new DefaultModel();
        ram = new ShareableResource("ram");
        mips = new ShareableResource("mips");
        bw = new ShareableResource("bw");
        model.attach(ram);
        model.attach(mips);
        model.attach(bw);

        for (Host h : hosts) {
            Node n = toNode(h);
            hostMapping.put(h, n);
            nodeMapping.put(n, h);
            model.getMapping().addOnlineNode(n);
        }
    }
    @Override
    public void startEntity() {
        Log.printLine("Starting " + NAME + "...");
        send(getId(), forcedReconfigurationDelay, FORCED_RECONFIGURATION);
    }

    @Override
    public void processEvent(SimEvent ev) {
        List<Object[]> buffer = btrpEp.emptyBuffer();
        switch (ev.getTag()) {
            case FORCED_RECONFIGURATION:
                solve(buffer);
                break;
            case RECONFIGURATION_TERMINATION:
                if (buffer.isEmpty()) {
                    send(getId(), forcedReconfigurationDelay, FORCED_RECONFIGURATION);
                } else {
                    solve(buffer);
                }
        }
    }

    private Node toNode(Host h) {
        Node n = model.newNode();
        ram.setCapacity(n, h.getRam());
        mips.setCapacity(n, h.getTotalMips());
        bw.setCapacity(n, (int) h.getBw());
        return n;
    }

    private VM getVm(Vm v) {
        VM vm = vmMapping.get(v);
        if (vm == null) {
            vm = model.newVM();
            vmMapping.put(v, vm);
        }
        ram.setConsumption(vm, v.getRam());
        bw.setConsumption(vm, (int)v.getBw());
        mips.setConsumption(vm, (int)v.getCurrentRequestedTotalMips());
        return vm;
    }

    private Host toHost(Node n) {
        return nodeMapping.get(n);
    }


    private void solve(List<Object[]> events) {
        List<SatConstraint> cstrs = new ArrayList<>();
        cstrs.addAll(Root.newRoots(model.getMapping().getAllVMs()));
        for (Object []o : events) {
            int op = (int)o[0];
            VM vm = getVm((Vm)o[1]);
            if (op == 1) {
                cstrs.add(new Running(vm));
            } else if (op == -1) {
                cstrs.add(new Ready(vm));
            }
        }
        try {
            ReconfigurationPlan plan = cra.solve(model, cstrs);

            if (plan == null) {
                Log.printLine(getName() + ": no solution");
                notifyFailure(events);
                return;
            }
            double at = plan.getSize() == 0 ? CloudSim.getMinTimeBetweenEvents() : plan.getDuration();
            for (Action a : plan) {
                if (a instanceof BootVM) {

                } else if (a instanceof ShutdownVM) {

                } else {
                    Log.printLine(getName() + " Unsupported action " + a);
                }
            }
            send(getId(), at, RECONFIGURATION_TERMINATION);
        } catch (SolverException ex) {
            Log.printLine(getName() + ": " + ex.getMessage());
            notifyFailure(events);
        }
    }

    private void notifyFailure(List<Object[]> events) {
        //send failures to every VM
        for (Object []o : events) {
            int op = (int)o[0];
            Vm vm = (Vm)o[1];
            sendNow(-1, op == 1 ? CloudSimTags.VM_CREATE_ACK:CloudSimTags.VM_DESTROY_ACK, new int[]{0, vm.getId(), CloudSimTags.FALSE});
        }

    }

    @Override
    public void shutdownEntity() {
        Log.printLine("Halting " + NAME + "...");
    }

    public int getForcedReconfigurationDelay() {
        return forcedReconfigurationDelay;
    }

    public void setForcedReconfigurationDelay(int forcedReconfigurationDelay) {
        this.forcedReconfigurationDelay = forcedReconfigurationDelay;
    }
}
