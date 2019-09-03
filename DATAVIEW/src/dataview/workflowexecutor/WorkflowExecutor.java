package dataview.workflowexecutor;
import dataview.models.*;

/**
 * This workflow executor will launch the necessary virtual machines necessarily, and send the task schedule information
 * to task Executor running in Virtual Machine by client-server strategy.
 */

public class WorkflowExecutor {
	public static final int WorkflowExecutor_Local = 0;
	public static final int WorkflowExecutor_Alpha = 1;
	public static final int WorkflowExecutor_Beta = 2;
	
	protected GlobalSchedule gsch;
	
	public WorkflowExecutor()
	{
		
	}
	
	public WorkflowExecutor(GlobalSchedule gsch) 
	{
		this.gsch = gsch;
	}
	
	public void execute()
	{
	
	}
}
