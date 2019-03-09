package csi311;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

import csi311.MachineSpec.StateTransitions; 

// Our app. 
public class TruState {
	
	// Keep a list of orders, keyed by order id
	private Map<String,Order> orders = new HashMap<String,Order>(); 
	// The state machine for this run
	private MachineSpec machineSpec; 

	public TruState() {
	}

	
	// Read in the state machine and the order file.  Make a report.  
    public void run(String stateFilename, String orderFilename) throws Exception {
    	System.out.println("Tru State");
   		String json = processStateFile(stateFilename); 
   		machineSpec = parseJson(json);
   		// dumpMachine(machineSpec); 
   		processOrderFile(orderFilename); 
   		makeReport();
    }

    
    // For debugging, dump out the state machine we think we read in.
    @SuppressWarnings("unused")
	private void dumpMachine(MachineSpec machineSpec) {
    	if (machineSpec == null) {
    		return;
    	}
    	for (StateTransitions st : machineSpec.getMachineSpec()) {
    		System.out.println(st.getState() + " : " + st.getTransitions());
    	}
    }
    
    
    // Read in the state machine file to a string.  Convert newline characters if needed.
    private String processStateFile(String filename) throws Exception {
    	BufferedReader br = new BufferedReader(new FileReader(filename));  
    	String json = "";
    	String line; 
    	while ((line = br.readLine()) != null) {
    		json += " " + line; 
    	} 
    	br.close();
    	// Get rid of special characters - newlines, tabs.  
    	return json.replaceAll("\n", " ").replaceAll("\t", " ").replaceAll("\r", " "); 
    }


    // Use the Jackson library to deserialize the state machine from a string into an object.
    private MachineSpec parseJson(String json) {
        ObjectMapper mapper = new ObjectMapper();
        try { 
        	MachineSpec machineSpec = mapper.readValue(json, MachineSpec.class);
        	return machineSpec; 
        }
        catch (Exception e) {
            e.printStackTrace(); 
        }
        return null;  	
    }
    
    
    // Iterate over the lines of the order file.  
    private void processOrderFile(String orderFilename) throws Exception {
    	// Open the file and connect it to a buffered reader.
    	BufferedReader br = new BufferedReader(new FileReader(orderFilename));  
    	String line = null;  
    	// Get lines from the file one at a time until there are no more.
    	while ((line = br.readLine()) != null) {
    		// Process the line
    		processOrder(line);
    	} 
    	// Close the buffer and the underlying file.
    	br.close();
    }
    
    
    // Process one line in the order file - a single order.
    private void processOrder(String line) {	
    	try { 
    		// Parse the line item on comma, populate an Order object
    		String[] tokens = line.split(",");
    		Order order = new Order();  
    		order.setTimeMs(Long.valueOf(tokens[0].trim())); 
    		order.setOrderId(tokens[1].trim());
    		order.setCustomerId(tokens[2].trim());
    		order.setState(tokens[3].trim().toLowerCase());
    		order.setDescription(tokens[4].trim());
    		order.setQuantity(Integer.valueOf(tokens[5].trim())); 
    		order.setCost(Float.valueOf(tokens[6].trim())); 
    		
    		// Validate and sttore the order in our cache of orders for later reporting.
    		updateOrder(order); 
    	}
    	catch (Exception e) {
    		e.printStackTrace();
    	}
    }
    
    
    // We're keeping a cache of orders we've processed.  When we see an order, see if its 
    // an order we've seen before. 
    private void updateOrder(Order newOrder) {
    	// Have we seen this order before?  If not, put it in the cache as the baseline.
    	boolean isNew = false; 
    	if (!orders.containsKey(newOrder.getOrderId())) {
    		orders.put(newOrder.getOrderId(), newOrder);
    		isNew = true; 
    	}
    	// Now look at the baseline order info in the cache and compare it to the new order info.
		Order oldOrder = orders.get(newOrder.getOrderId());
		// Is the new order info valid?  Are the fields valid and is it a valid state transition 
		// from the baseline order info?
		if ( 	(!newOrder.validateOrderFields()) ||  
				(newOrder.getTimeMs() < oldOrder.getTimeMs()) ||
				(!newOrder.getCustomerId().equals(oldOrder.getCustomerId())) || 
				(!MachineSpec.isValidTransition(machineSpec, oldOrder.getState(), newOrder.getState(), isNew)) 
			) {
			// The order info is not valid somehow.  Either the fields are not valid or its 
			// not a valid state transition.
			System.out.println("Flagging order " + newOrder.getOrderId());
			oldOrder.setFlagged(true);
		}
		// Put the new order info in the cache as the new basline.  If the old baseline was 
		// flagged, then this info is flagged too (even if the info itself was valid, the history of 
		// this order# shows the order is bad).
		newOrder.setFlagged(oldOrder.isFlagged());
		orders.put(oldOrder.getOrderId(), newOrder);
    }
    

    // Create a report on the processed orders.  
    private void makeReport() {
    	// Count up the orders in each state (including those "flagged").  Also sum up 
    	// the dollar value of orders in each state. 
    	Map<String,Integer> countMap = new HashMap<String,Integer>();
    	Integer countFlagged = 0; 
    	Map<String,Float> valueMap = new HashMap<String,Float>();
    	for (String key : orders.keySet()) {
    		Order o = orders.get(key);
    		if (!countMap.containsKey(o.getState())) {
    			countMap.put(o.getState(), 0);
    			valueMap.put(o.getState(), 0.0f);
    		}
    		if (o.isFlagged()) {
    			countFlagged++; 
    		}
    		else {
    			countMap.put(o.getState(), countMap.get(o.getState()) + 1);
    			valueMap.put(o.getState(), valueMap.get(o.getState()) + o.getCost());
    		}
    	}
    	
    	// For the report, for each state note if its a terminal state and output the metrics
    	// for that state.
    	for (String state : countMap.keySet()) {
    		Float cost = valueMap.get(state);
    		if (cost == null) {
    			cost = 0.0f;
    		}
    		String terminal = "";
    		if (MachineSpec.isTerminalState(machineSpec, state)) {
    			terminal = "(terminal)";
    		}
    		System.out.println(state + " " + countMap.get(state) + " $" + cost + " " + terminal);
    	}
    	// Plus output the count of flagged orders.
    	System.out.println("flagged " + countFlagged);
    }
    
    
    // Our entry point when our app is invoked.  Takes two command line arguments - 
    // the state machine filename, and the order filename.
    public static void main(String[] args) {
    	TruState theApp = new TruState();
    	String stateFilename = null;
    	String orderFilename = null; 
    	if (args.length > 1) {
    		stateFilename = args[0];
    		orderFilename = args[1]; 
    	}
    	try { 
    		theApp.run(stateFilename, orderFilename);
    	}
    	catch (Exception e) {
    		System.out.println("Something bad happened!");
    		e.printStackTrace();
    	}
    }	
	
	

}
