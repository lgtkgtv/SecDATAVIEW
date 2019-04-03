package dataview.models;
import java.util.ArrayList;
import java.util.List;

/* We only consider DAG workflows in this implementation. We have explored more sophisticated 
 * dataflow constructs in the VIEW system, see Xubo Fei and Shiyong Lu, "A Dataflow-Based Scientific Workflow Composition Framework", 
 * IEEE Transactions on Services Computing (TSC), 5(1), pp.45-58, 2012, impact factor: 1.47.
 * Future developers can use the ideas there to implement more sophisticated dataflow constructs
 * based on an extension of the DAG model.
 * 
 * 
 */
public class Workflow {
	public String workflowName;
	public String workflowDescription;
	private List<Task> myTasks;
	private List<WorkflowEdge> myEdges;
	private List<String> srcFilenames;
	private List<String> destFilenames;
	private List<Stage> myStages;
	
	public Workflow(String workflowName, String workflowDescription)
	{
		//System.out.println("000000000000000000");
		this.workflowName = workflowName;
		this.workflowDescription = workflowDescription;
		myTasks = new ArrayList<>();
		myEdges = new ArrayList<>();
		srcFilenames  = new ArrayList<>();
		destFilenames  = new ArrayList<>();
		myStages = new ArrayList<>();
	}
	
	public int getNumOfNodes()
	{
		return srcFilenames.size()+myTasks.size()+destFilenames.size();
	}
	
	public int getNumOfSrcFiles()
	{
		return srcFilenames.size();
	}
	
	public int getNumOfTasks()
	{
		return myTasks.size();
	}

	public String getSrcFilename(int i) {
		return this.srcFilenames.get(i);
	}
	
	public String getDestFilename(int i) {
		return this.destFilenames.get(i);
	}

	
	public Task getTask(int i) {
		return this.myTasks.get(i);
	}

	public List<WorkflowEdge> getEdges()
	{
		return myEdges;
	}
	
	public int getNumOfDestFiles()
	{
		return destFilenames.size();
	}

	
	public int getIndexOfSrcFilename(String srcFilename)
	{
		for(int i= 0; i <srcFilenames.size(); i++)
			if(srcFilenames.get(i).equals(srcFilename)) return i;
		
		return -1;
	}
	
	public int getIndexOfTask(Task t)
	{
		for(int i= 0; i <myTasks.size(); i++)
			if(myTasks.get(i).equals(t)) return i;
		
		return -1;
	}
	
	
	
	private void addSrcFilename(String srcFilename) {
		if(getIndexOfSrcFilename(srcFilename) == -1) srcFilenames.add(srcFilename);
		else return;
	}

	
	public int getIndexOfDestFilename(String destFilename)
	{
		for(int i= 0; i <destFilenames.size(); i++)
			if(destFilenames.get(i).equals(destFilename)) return i;
		
		return -1;
	}
	
	
	private void addDestFilename(String destFilename) {
		if(getIndexOfDestFilename(destFilename) == -1) destFilenames.add(destFilename);
		else return;
	}


	public void addEdge(Task srcTask, int outputPortIndex, Task destTask, int inputPortIndex)
	{
		//System.out.println("11111111111111");
		myEdges.add(new WorkflowEdge(srcTask, outputPortIndex, destTask, inputPortIndex));
	}
	
	public void addEdge(Task srcTask,Task destTask)
	{
		//System.out.println("11111111111111");
		myEdges.add(new WorkflowEdge(srcTask, 0, destTask, 0));
	}
	
	
	public void addEdge(String srcFilename, Task destTask, int inputPortIndex) {
		addSrcFilename(srcFilename);
		//System.out.println("22222222222222");
		myEdges.add(new WorkflowEdge(srcFilename, destTask, inputPortIndex));
	}
	
	public void addEdge(String srcFilename, Task destTask) {
		addSrcFilename(srcFilename);
		//System.out.println("22222222222222");
		myEdges.add(new WorkflowEdge(srcFilename, destTask, 0));
	}

	
	public void addEdge(Task srcTask, int outputPortIndex, String destFilename) {
		//System.out.println("33333333333333333333");
		addDestFilename(destFilename);
		myEdges.add(new WorkflowEdge(srcTask, outputPortIndex, destFilename));
	}

	public void addEdge(Task srcTask, String destFilename) {
		//System.out.println("33333333333333333333");
		addDestFilename(destFilename);
		myEdges.add(new WorkflowEdge(srcTask, 0, destFilename));
	}


	
	public void run() 
	{
		// GlobalSchedule gsc = AlphaWorkflowPlanner();
		// WorkflowExecutor we = new WorkflowExector(gsc);
		// we.run();
	}
	
	
		
	public boolean verify()
	{
		return true;
		// verify the structure of the workflow to make sure it is well-formed, and return 
		// true or false and write an error message when necessary?
	}	
	
    @Override
    public String toString() 
	{
		String str = "";
		
		for(WorkflowEdge e: myEdges) {
			if(e.srcFilename != null) 
				str = str + "File: " + e.srcFilename + " => " + e.destTask + ".inputport: " + e.inputPortIndex + "\n";
			else if (e.destFilename != null)
				str = str + e.srcTask + ".outputPort: " + e.outputPortIndex + " => " + "File: "+e.destFilename+"\n";
			else
				str = str + e.srcTask + ".outputPort: " + e.outputPortIndex + " => " + e.destTask + ".inputport: " + e.inputPortIndex + "\n";
		}		
		
		return str;		
	}
    
    /* add a single task */
    public Task addTask(String taskTypeName)
    {
    	Task  newtask = null;
    	
    	System.out.println("Attemp to add one task of type "+taskTypeName);
		Class<?> taskclass;
		try {
			taskclass = Class.forName(taskTypeName);
			newtask =  (Task) taskclass.newInstance();
				
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			Dataview.debugger.logException(e);
		} catch (InstantiationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			Dataview.debugger.logException(e);
		} catch (IllegalAccessException e) {
			System.out.println("Exception, possible reason: the constructor of class "+ taskTypeName+" is not public.");
			// TODO Auto-generated catch block
			e.printStackTrace();
			Dataview.debugger.logException(e);
		}
		myTasks.add(newtask);
		System.out.println("One task: "+newtask+ " is added.");
		myStages.add(new Stage(newtask));
		return newtask;		
    }
  
    
    /* add M number of tasks with task type taskTypeName, warning: do not call addTask from this method as this will affect the notion of stages. */
    public Task [] addTasks(String taskTypeName, int M)
    {
    	Task [] newtasks = new Task[M];
    	
    	System.out.println("Attemp to add "+M+" tasks of type "+taskTypeName);
		Class<?> taskclass;
		try {
			taskclass = Class.forName(taskTypeName);
			for(int i=0; i<M; i++) {
				newtasks[i] =  (Task) taskclass.newInstance();
				myTasks.add(newtasks[i]);
			}
				
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			Dataview.debugger.logException(e);
		} catch (InstantiationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			Dataview.debugger.logException(e);
		} catch (IllegalAccessException e) {
			System.out.println("Exception, possible reason: the constructor of class "+ taskTypeName+" is not public.");
			// TODO Auto-generated catch block
			e.printStackTrace();
			Dataview.debugger.logException(e);
		}
 
    				
		System.out.println(M+" tasks: "+taskTypeName+ " are added.");
		myStages.add(new Stage(newtasks));
		return newtasks;		
    }

    /* connect the first output of parent to each input of the M children */
    public void addEdges_SplitPattern(Task parent, Task [] children, int M)
    {
    	Dataview.debugger.logFalseCondition("The number of output ports for the parent task is not equal to the number of children.", parent.outs.length == children.length || children.length == M);
     	for(int i=0; i < M; i++){
    		addEdge(parent, i, children[i], 0);
    	} 	
    }

    /* connect the first output of parent to each input of the M children */
    public void addEdges_SplitPattern(String srcFileName, Task [] children, int M)
    {
    	
     	for(int i=0; i < M; i++){
    		addEdge(srcFileName, children[i], 0);
    	} 	
    }


    
    /* connect the first output of parent to each kth input of the M children */
    public void addEdges_SplitPattern(Task parent, Task [] children, int k, int M)
    {
    	Dataview.debugger.logFalseCondition("The number of output ports for the parent task is not equal to the number of children.", parent.outs.length == children.length || children.length == M); 	
     	for(int i=0; i < M; i++){
    		addEdge(parent, i, children[i], k);
    	} 	
    }

    /* connect the first output of parent to each input of the M children */
    public void addEdges_SplitPattern(String srcFileName, Task [] children, int k, int M)
    {
    	
     	for(int i=0; i < M; i++){
    		addEdge(srcFileName, children[i], k);
    	} 	
    }

    
    
    /* connect the output of M parents to the ith intput of child */  
    public void addEdges_JoinPattern(Task [] parents,  Task child, int M)
    {
    	Dataview.debugger.logFalseCondition("Workflow.java: The number of parents is not equal to the number of intput ports of the childre. ", parents.length == child.ins.length || parents.length == M);	
     	for(int i=0; i < M; i++){
    		addEdge(parents[i], 0, child, i);
    	} 	
    }
    
    /* connecting M parents to K children, such that the jth output of each parent will connect to the intput of the jth child
     * We assume each parent has outputs, and each child has M inputs.
     */
    public void addEdges_ShufflePattern(Task [] parents,  Task [] children, int M, int K)
    {
    	Dataview.debugger.logFalseCondition("Workflow.java: The number of parents is not equal to M", parents.length == M);	
    	Dataview.debugger.logFalseCondition("Workflow.java: The number of children is not equal to K", children.length == K);	
     	
    	for(int i=0; i<M; i++) {
    		for(int j=0; j<K; j++)
    			addEdge(parents[i], j, children[j], i); // jth output goes to jth child, like mapreduce
    	}
    }    
    
    /* M map jobs, and K reduce jobs, this pattern can simulate the MapReduce jobs */
    public void addEdges_SplitShuffleJoinPattern(Task split, Task [] mapjob, Task [] reducejob, Task join, int M, int K)
    {
       addEdges_SplitPattern(split, mapjob, M);
       addEdges_ShufflePattern(mapjob, reducejob, M, K);
       addEdges_JoinPattern(reducejob, join, K);
    }
    
    /* M parents, M children, each output port 0 is connected to the corresponding input port 0 of childre */
    public void addEdges_ParallelParallelPattern(Task [] parents , Task [] children, int M)
    {
    	Dataview.debugger.logFalseCondition("Workflow.java: The number of parents is not equal to the number of children.", parents.length == M && children.length == M);
    	
         	for(int i=0; i < M; i++)
    			addEdge(parents[i], 0, children[i], 0);
    }
    
    public Stage getStage(int i)
    {
    	if(i>=0) {
    		return myStages.get(i);
    	}
    	else // if negative
    		return myStages.get(myStages.size()+i);  // -1 is the last stage  	
    }
 
	public JSONObject getWorkflowSpecification()
	{
		JSONObject obj = new JSONObject();
		obj.put("workflowName", new JSONValue(workflowName));
		obj.put("workflowDescription", new JSONValue(workflowDescription));
		
		// add all the task instances
		JSONArray tasklib = new JSONArray();
		for(int i=0; i< myTasks.size(); i++) {
		      JSONObject taski = new JSONObject();
		      taski.put("taskInstanceID", new JSONValue(myTasks.get(i).toString()));
		      taski.put("taskType", new JSONValue(myTasks.get(i).getClass().getName()));
		      tasklib.add(new JSONValue(taski));		  
		}
		
		obj.put("taskInstances", new JSONValue(tasklib));
		
		// all input files
		JSONArray src = new JSONArray();
		for(int i=0; i< srcFilenames.size(); i++) {
			src.add(new JSONValue(srcFilenames.get(i)));
		}
		obj.put("srcFilenames", new JSONValue(src));
		
		// all output files
		JSONArray dest = new JSONArray();
		for(int i=0; i< destFilenames.size(); i++) {
			dest.add(new JSONValue(destFilenames.get(i)));
		}
		obj.put("destFilenames", new JSONValue(dest));

		
		// add all task edges
		JSONArray edgelib = new JSONArray();
		for(int i=0; i< myEdges.size(); i++) {
			JSONObject edgespec = myEdges.get(i).getWorkflowEdgeSpecification();
			edgelib.add(new JSONValue(edgespec));
		}
		obj.put("edges", new JSONValue(edgelib));
		
		return obj;		
	}

	public TaskSchedule getTaskSchedule(Task t) 
	{
		TaskSchedule tsch = new TaskSchedule(t);
		for(WorkflowEdge e: myEdges) {
		    if(e.destTask != null && e.destTask.equals(t)) { // found an incoming data channel
		    	System.out.println("found an incoming data channel..");
		    	if(e.srcFilename != null)
		    		tsch.AddIncomingDataChannel(new IncomingDataChannel(e.srcFilename, e.inputPortIndex));
		    	else
		          tsch.AddIncomingDataChannel(new IncomingDataChannel(e.srcTask, e.outputPortIndex, e.inputPortIndex));
		    }
		    
		    if(e.srcTask != null && e.srcTask.equals(t)) {  // then we found an outgoing data channel
		    	System.out.println("found an outgoing data channel..");
		    	if(e.destFilename != null)
		    		tsch.AddOutgoingDataChannel(new OutgoingDataChannel(e.outputPortIndex, e.destFilename));
		    	else
		          tsch.AddOutgoingDataChannel(new OutgoingDataChannel(e.outputPortIndex, e.destTask, e.inputPortIndex));

		    }		    		    
		} // end for
		
		return tsch;		
	}
	
	
	
}




