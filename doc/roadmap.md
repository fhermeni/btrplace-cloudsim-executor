# Roadmap to integrate BtrPlace inside Cloudsim

1. *VMBroker* to provision VMs for clients
2. *NodeBroker* to provision idle nodes or offline nodes for operators*

2. *Cloud Scheduler*

    1. to support VM boot and shutdown   OK
    2. to support optimisation (migrations)
    3. resource re-allocation

3. *Node state management*

## Cloud broker ##


- Support for VM instantiation and desallocation.
- Schedule VM immediately or at a given moment
- a possible walltime
- possible retries

## Node broker ##

- Ask for idle or offline nodes
- walltime or not
- retries or not

## Cloud scheduler ##

- supported actions:
    - bootVM OK
    - shutdownVM OK
    - migrateVM OK
        - currently use the duration estimator of BtrPlace
        - naive model (non-blocking network and no migrations in // on the same end wires OK
        - prevent migrations in parallel if source or destination are identical as a solution ?
        - not naive model
    - bootNode K
    - shutdownNode K
    - Allocate

- schedule frequency
    - a sliding window after the first submission. May be postponed if a reconfiguration is pending OK
    - support for early scheduling: act on model immediately for booting VM, delay shutdown VM


## PowerModel ##
- power model for the node. A linear one


