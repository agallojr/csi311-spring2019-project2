package csi311;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;

import csi311.MachineSpec.StateTransitions; 

public class TruState {
	
	private Map<String,Order> orders = new HashMap<String,Order>(); 
	private MachineSpec machineSpec; 

	public TruState() {
	}

	
    public void run(String stateFilename, String orderFilename) throws Exception {
    	System.out.println("Tru State");
   		String json = processStateFile(stateFilename); 
   		machineSpec = parseJson(json);
   		dumpMachine(machineSpec); 
   		processOrderFile(orderFilename); 
   		makeReport();
    }

    
    private void dumpMachine(MachineSpec machineSpec) {
    	if (machineSpec == null) {
    		return;
    	}
    	for (StateTransitions st : machineSpec.getMachineSpec()) {
    		System.out.println(st.getState() + " : " + st.getTransitions());
    	}
    }
    
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
    
    
    private void processOrderFile(String orderFilename) throws Exception {
    	// Open the file and connect it to a buffered reader.
    	BufferedReader br = new BufferedReader(new FileReader(orderFilename));  
    	String line = null;  
    	// Get lines from the file one at a time until there are no more.
    	while ((line = br.readLine()) != null) {
    		processOrder(line);
    	} 
    	// Close the buffer and the underlying file.
    	br.close();
    }
    
    
    private void processOrder(String line) {	
    	try { 
    		// Parse the line item.
    		String[] tokens = line.split(",");
    		Order order = new Order();  
    		order.setTimeMs(Long.valueOf(tokens[0].trim())); 
    		order.setOrderId(tokens[1].trim());
    		order.setCustomerId(tokens[2].trim());
    		order.setState(tokens[3].trim().toLowerCase());
    		order.setDescription(tokens[4].trim());
    		order.setQuantity(Integer.valueOf(tokens[5].trim())); 
    		order.setCost(Float.valueOf(tokens[6].trim())); 
    		
    		updateOrder(order); 
    	}
    	catch (Exception e) {
    		e.printStackTrace();
    	}
    }
    
    
    private void updateOrder(Order newOrder) {
    	boolean isNew = false; 
    	if (!orders.containsKey(newOrder.getOrderId())) {
    		orders.put(newOrder.getOrderId(), newOrder);
    		isNew = true; 
    	}
		Order oldOrder = orders.get(newOrder.getOrderId());
		if ( 	(!newOrder.validateOrderFields()) ||  
				(newOrder.getTimeMs() < oldOrder.getTimeMs()) ||
				(!newOrder.getCustomerId().equals(oldOrder.getCustomerId())) || 
				(!MachineSpec.isValidTransition(machineSpec, oldOrder.getState(), newOrder.getState(), isNew)) 
			) {
			System.out.println("Flagging order " + newOrder.getOrderId());
			oldOrder.setFlagged(true);
		}
		newOrder.setFlagged(oldOrder.isFlagged());
		orders.put(oldOrder.getOrderId(), newOrder);
    }
    

    private void makeReport() {
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
    	System.out.println("flagged " + countFlagged);
    }
    
    
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
