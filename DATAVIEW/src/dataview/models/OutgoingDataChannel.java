package dataview.models;

public class OutgoingDataChannel {

		public int myOutputPortIndex;        // from
		
		public Task destTask;  // to
		public String destIP;
		public int inputPortIndex;        // to

		public String destFilename;       // to
		
		
		/* connect the output port of a task to the inputport of another task */
		public OutgoingDataChannel(int myOutputPortIndex, Task destTask, int inputPortIndex)
		{
			this.myOutputPortIndex = myOutputPortIndex;  // from
			
			
			this.destTask = destTask;  // to
			this.inputPortIndex = inputPortIndex; // to
			this.destFilename = null;  // NA
		}
		
		/* Connect the ouput port of a task to an output file*/
		public OutgoingDataChannel(int myOutputPortIndex, String destFilename){
			this.myOutputPortIndex = myOutputPortIndex;  // from
			
			
			this.destFilename = destFilename;   // to
			
			this.destTask = null;   // NA
			this.inputPortIndex = -1;  // NA
		}	
		
		public void setDestIP(String destIP)
		{
		    this.destIP = destIP;
		}
		
		public Task getDestTask()
		{
			return destTask;
		}
		
		public int getInputPortIndex()
		{
			return inputPortIndex;
		}
		
		public JSONObject getSpecification()
		{
	    	JSONObject obj = new JSONObject();
	    	
	  		obj.put("myOutputPortIndex", new JSONValue(myOutputPortIndex+""));
	  		 

	    	if(destTask != null){
	    		obj.put("destTask", new JSONValue(destTask.toString()));
	    		obj.put("destIP", new JSONValue(destIP));
	    	}
	    	else
	    		obj.put("destTask", new JSONValue(""));


	    	if(inputPortIndex != -1)
	    		obj.put("inputPortIndex", new JSONValue(inputPortIndex+""));
	    	else
	    		obj.put("inputPortIndex", new JSONValue("-1"));

	    	
	    	if(destFilename != null)
	    		obj.put("destFilename", new JSONValue(destFilename));
	    	else
	    		obj.put("destFilename", new JSONValue(""));
	    	
	    	
	    	return obj;
		}				
	}