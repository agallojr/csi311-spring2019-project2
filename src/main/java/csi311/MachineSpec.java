package csi311;

import java.io.Serializable;
import java.util.List;

// A serializable class - we'll write/read this to/from JSON.
@SuppressWarnings("serial")
public class MachineSpec implements Serializable {

	// Since defined as an inner class, must be declared static or Jackson can't deal.
	// The MachineSpec is a list of these adjacancy lists.
	public static class StateTransitions implements Serializable {
		private String state; 
		private List<String> transitions;
		public StateTransitions() { }
		public String getState() { return state; }
		public void setState(String state) { this.state = state.toLowerCase(); } 
		public List<String> getTransitions() { return transitions; } 
		public void setTransitions(List<String> transitions) { 
			this.transitions = transitions;
			if (this.transitions != null) {
				for (int i = 0; i < transitions.size(); i++) {
					transitions.set(i, transitions.get(i).toLowerCase()); 
				}
			}
		} 
	}
	
	private List<StateTransitions> machineSpec;
	public MachineSpec() { }
	public List<StateTransitions> getMachineSpec() { return machineSpec; } 
	public void setMachineSpec(List<StateTransitions> machineSpec) { this.machineSpec = machineSpec; }
	
	
	// Does the adjacancy list for state1 include state2?  i.e. can you get to state2 from state1?
	public boolean stateTransitionsContain(String state1, String state2) {
		List<String> transitions = null; 
		for (StateTransitions sts : getMachineSpec()) {
			if (sts.getState().equals(state1)) {
				transitions = sts.getTransitions();
				break;
			}
		}
		if (transitions == null) {
			return false;
		}
		return transitions.contains(state2);
	}

	
	// Given the state machine "spec", is there a transition from state1 to state2?  If this 
	// is a new order, then the value of state1 must be a start state.
	public static boolean isValidTransition(MachineSpec spec, String state1, String state2, boolean isNew) {
		if (isNew) {
			// A new order's state is valid if its a start state
			return spec.stateTransitionsContain("start", state1);
		}
		// Its not a new order, so the state is valid if state1 --> state2 is a valid transition
		return spec.stateTransitionsContain(state1, state2); 
	}
	
	
	// Is state a terminal state?  It is if its not represented on the "left hand side" of an 
	// adjacency list.  
	public static boolean isTerminalState(MachineSpec spec, String state) {
		for (StateTransitions sts : spec.getMachineSpec()) {
			if (sts.getState().equals(state)) {
				if (sts.getTransitions().size() > 1) {
					// The state is in the table, and it has a non-zero adjacency list.  
					// Its not a terminal state.  
					return false; 
				}
				if (sts.getTransitions().get(0).equals(state)) {
					// Transition to itself is allowed.
					return true; 
				}
			}
		}
		// Its not in the list of state transitions.  Its a terminal state.
		return true; 
	}
	
}


