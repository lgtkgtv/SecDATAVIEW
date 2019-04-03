package dataview.models;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.*;



/**
 * 
 * @author ishtiaqahmed
 * Global schedule consists of multiple virtual machines.
 *
 */
public class GlobalSchedule {
	private List<LocalSchedule> lschs;

	/**
	 * Constructs and initialize multiple local schedule
	 * @param localSchedules
	 * localSchedules multiple local schedule
	 */
	public GlobalSchedule() {
		lschs = new ArrayList<LocalSchedule>();
	}

	public void addLocalSchedule(LocalSchedule lsch)
	{
		lschs.add(lsch);
	}

	/**
	 * Return the number of local schedules, which is the number of VMs that are needed. 
	 */
	public int length()
	{
		return lschs.size();
	}

	public LocalSchedule getLocalSchedule(int i)
	{
		return lschs.get(i);
	}

	public JSONObject getSpecification()
	{
		JSONObject obj = new JSONObject();

		// add the specifications for all task schedules in this local schedule   	
		JSONArray spec = new JSONArray();
		for(int i=0; i< lschs.size(); i++) {
			JSONObject lsch_spec = lschs.get(i).getSpecification();
			spec.add(new JSONValue(lsch_spec));	      
		}
		obj.put("localSchedules", new JSONValue(spec));

		return obj;			
	}

	public String getIP(Task t)
	{
		for(int i= 0; i<lschs.size(); i++ ){
			LocalSchedule lsch = lschs.get(i);
			if(lsch.containsTask(t)){
				return lsch.getIP();
			}
		}

		return null;	
	}
	
	/**
	 * This method will return the ip address associated with task by their taskInstanceIDß
	 * @return
	 */
	public HashMap<String, String> mapTaskToVM() {
		HashMap<String, String> map = new HashMap<String, String>();
		for(int i= 0; i<lschs.size(); i++ ){
			LocalSchedule lsch = lschs.get(i);
			for (int j = 0; j < lsch.length(); j++) {
				TaskSchedule tsch = lsch.getTaskSchedule(j);
				map.put(tsch.getTask().toString(), lsch.getIP());
			}
		}
		return map;
	}
	
	
	public int totalRegularInstance() {
		int regularInstance = 0;
		for (int i = 0; i < lschs.size(); i++) {
			LocalSchedule lsch = lschs.get(i);
			if (!lsch.getVmType().equalsIgnoreCase("SGX")) {
				regularInstance++;
			}
		}
		return regularInstance;
	}
	

	/**
	 * This method is necessary to propagate all IPS to the outgoing data channels of each task schedule before
	 * we send each local schedule to each VM.
	 */
	public void completeIPAssignment()
	{
		for(int i= 0;  i < lschs.size(); i++ ){
			LocalSchedule lsch = lschs.get(i);
			for(int j=0; j<lsch.length(); j++){
				TaskSchedule tsch = lsch.getTaskSchedule(j);
				List<OutgoingDataChannel> outdcs = tsch.getOutgoingDataChannels();
				for(OutgoingDataChannel outdc: outdcs){
					if(outdc.destTask == null) continue;
					outdc.setDestIP(getIP(outdc.destTask));
				}
			}
		}

	}
	
	

	/**
	 * This method will return the localSchedule id number for certain task
	 */
	
	public Integer getLocalScheduleIDNumber(String taskName) {
		for (int i = 0; i < lschs.size(); i++) {
			LocalSchedule lsch = lschs.get(i);
			for(int j=0; j<lsch.length(); j++){
				TaskSchedule tsch = lsch.getTaskSchedule(j);
				if (tsch.getTask().toString().equalsIgnoreCase(taskName)) {
					return i;
				}
			}
		}
		return null;
	}
	

	
	/**
	 * This method will return the total number of tasks
	 */
	
	public int getTotalNumberOfTasks() {
		int totalTasks = 0;
		for (int i = 0; i < lschs.size(); i++) {
			LocalSchedule lsch = lschs.get(i);
			totalTasks += lsch.length();
		}
		return totalTasks;
	}


}
