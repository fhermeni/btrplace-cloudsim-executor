# Workflows


## Node state management

- How to integrate boot|shutdown of nodes ?

VM_CREATE
 ->


# Custom broker

## Basic features

- launch VM (service VM or time-bounded duration)
	- now
	- at a given moment known in advance
	- during the simulation
	- delayable or not
- stop VM
    - some predefined moment from the walltime
    - the same
- need to know the waiting time    	

## Broker    	
a FIFO basic: launch every VM scheduled now.

### Version 1
 
+ submit(Vm v, int at, int w, boolean r)
  -> emit VM_ENQUEUE(me, at, VM_ENQUEUE, {v, w, r})
  
consume VM_ENQUEUE(me, {v,w,r})
 -> send VM_CREATE_ACK ... to DC
 
consume VM_CREATE_ACK (v)
  -> if succeeded:    
    if (w > 0) {
    	emit VM_DESTROY_ACK to DC
  -> if fail
    if (retry) {
    	emit VM_ENQUEUE(me, now + retry_window, {v, w, r})
    }  
    
  
 
param: retry_window

emit to Broker VM_ENQUEUE (vm, walltime, retryable)

emit to DC VM_CREATE_ACK
emit to DC VM_DESTROY_ACK

consume VM_ENQUEUE:
 
		
- FIFO
- Retryable launch ?
- Non-retryable launchs

##Scheduler

Etat de l'entity:
- reconfiguring
- idle

vide buffer si timeout && !reconfiguring
/!\ !timeout & reconfiguring -> trop lent


Lancement d'une reconfiguration:
 -> fin d'une précédente reconfiguration
 ||
 -> fin forcée
 
 consume RECONFIGURATION_TERMINATION:
  buffer.empty ?
  	emit FORCED_RECONFIGURATION@X
  else
    new buffer    
    compute reconfiguration & start
    emit RECONFIGURATION_TERMINATION@horizon
  fi
  
  
Objectives: 
- integrate computation time and actuation time

impact:
-> asking for an allocation does not necessarily lead to a call to the scheduler
  - it might be already computing sth.
  - it might want to buffer some events

+ allocate()
+ desalocate()
  -> aggregate
  
if (buffer == null) {
	//nvx buffer
	//on empile
}

fin de reconfiguration: emission event CLOSE_SCHEDULING_WND

if (CLOSE_SCHEDULING_WND) {
	launch the computation
	send actuation events
}  

##Support for optimisation

##Support for node-state management

##Support for a power-model

##Support for central_system

##Support for EASCs