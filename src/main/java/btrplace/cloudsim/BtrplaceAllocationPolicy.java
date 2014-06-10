package btrplace.cloudsim;

import btrplace.model.DefaultModel;
import btrplace.model.Model;
import btrplace.model.Node;
import btrplace.model.VM;
import btrplace.model.constraint.SatConstraint;
import btrplace.model.view.ShareableResource;
import btrplace.plan.ReconfigurationPlan;
import btrplace.plan.event.*;
import btrplace.solver.SolverException;
import btrplace.solver.choco.ChocoReconfigurationAlgorithm;
import btrplace.solver.choco.DefaultChocoReconfigurationAlgorithm;
import btrplace.solver.choco.duration.ConstantActionDuration;
import btrplace.solver.choco.duration.LinearToAResourceActionDuration;
import org.cloudbus.cloudsim.Host;
import org.cloudbus.cloudsim.Vm;

import java.lang.reflect.Constructor;
import java.util.*;


/**
 * @author Fabien Hermenier
 */
public class BtrPlaceAllocationPolicy {

    private ChocoReconfigurationAlgorithm cra;

    private ShareableResource ram;

    private ShareableResource mips;

    private ShareableResource bw;

    private Model model;

    private Map<Vm, VM> vmMapping;

    private Map<Host, Node> hostMapping;
    private Map<Node, Host> nodeMapping;

    private Map<Integer, Vm> intToVm;
    private List<? extends Host> hostList;

    public BtrPlaceAllocationPolicy(List<Host> hosts) {
        hostList = hosts;
        vmMapping = new HashMap<>();
        hostMapping = new HashMap<>();
        nodeMapping = new HashMap<>();

        intToVm = new HashMap<>();
        cra = new DefaultChocoReconfigurationAlgorithm();
        cra.setVerbosity(0);
        cra.getDurationEvaluators().register(BootVM.class, new ConstantActionDuration(300)); //5 minutes
        cra.getDurationEvaluators().register(ShutdownVM.class, new ConstantActionDuration(30)); //30 seconds
        cra.getDurationEvaluators().register(MigrateVM.class, new LinearToAResourceActionDuration("ram", 0.01, 3)); //30 seconds
        cra.getDurationEvaluators().register(BootNode.class, new ConstantActionDuration(4 * 60)); //4 minutes
        cra.getDurationEvaluators().register(ShutdownNode.class, new ConstantActionDuration(30)); //30 sec.
        //cra.doRepair(true);
        cra.setTimeLimit(60);
        model = new DefaultModel();
        ram = new ShareableResource("ram");
        mips = new ShareableResource("mips");
        bw = new ShareableResource("bw");
        model.attach(ram);
        model.attach(mips);
        model.attach(bw);

        for (Host h : hosts) {
            Node n = getHost(h);
            hostMapping.put(h, n);
            nodeMapping.put(n, h);
            model.getMapping().addOnlineNode(n);
        }
    }

    public ReconfigurationPlan solve(Collection<SatConstraint> cstrs) throws SolverException {
        return cra.solve(model, cstrs);
    }

    private Host toHost(Node n) {
        return nodeMapping.get(n);
    }

    public Host getHost(Vm vm) {
        return vm.getHost();
    }

    public Host getHost(int i, int i2) {
        return intToVm.get(i).getHost();
    }

    public Node getHost(Host h) {
        Node n = hostMapping.get(h);
        if (n == null) {
            n = model.newNode();
            model.getAttributes().put(n, "cloudsim_id", h.getId());
            hostMapping.put(h, n);
        }
        ram.setCapacity(n, h.getRam());
        mips.setCapacity(n, h.getTotalMips());
        bw.setCapacity(n, (int) h.getBw());
        return n;
    }

    public Vm getVm(int i) {
        return intToVm.get(i);
    }

    public VM getVm(Vm v) {
        VM vm = vmMapping.get(v);
        if (vm == null) {
            vm = model.newVM();
            model.getAttributes().put(vm, "cloudsim_id", v.getId());
            model.getMapping().addReadyVM(vm);
            vmMapping.put(v, vm);
            intToVm.put(vm.id(), v);
        }
        ram.setConsumption(vm, v.getRam());
        bw.setConsumption(vm, (int)v.getBw());
        mips.setConsumption(vm, (int)v.getCurrentRequestedTotalMips());
        return vm;
    }

    public Vm getVm(VM v) {
        return intToVm.get(v.id());
    }

    public Host getHost(Node n) {
        return nodeMapping.get(n);
    }
    public List<? extends Host> getHostList() {
        return hostList;
    }

    public Model getModel() {
        return model;
    }

    public SatConstraint build(String cstrName, List<Object> params) throws Exception {
        String clName = cstrName.substring(0, 1).toUpperCase() + cstrName.substring(1);
        Class<SatConstraint> cl = (Class<SatConstraint>) Class.forName("btrplace.model.constraint." + clName);
        List<Object> values = new ArrayList<>(params.size());
        for (Object c : params) {
            values.add(convertParam(c));
        }
        for (Constructor c : cl.getConstructors()) {
            if (c.getParameterTypes().length == values.size()) {
                try {
                    return (SatConstraint) c.newInstance(values.toArray());
                } catch (Exception e) {
                    //We want ot try other constructors that may match
                }
            }
        }

        throw new IllegalArgumentException("No constructors compatible for constraint " + cl.getSimpleName() + " with values '" + values + "'");
    }

    private Object convertParam(Object c) {
        if (c instanceof Vm) {
            return getVm((Vm) c);
        } else if (c instanceof Host) {
            return getHost((Host) c);
        } else if (c instanceof Collection) {
            Collection res;
            if (c instanceof Set) {
                res = new HashSet<>();
            } else {
                res  = new ArrayList<>();
            }
            for (Object o : (Collection)c) {
                res.add(convertParam(o));
            }
            return res;
        } else {
            //Primitive types
            return c;
        }
    }
}
