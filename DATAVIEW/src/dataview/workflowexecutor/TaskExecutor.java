package dataview.workflowexecutor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

import dataview.models.Dataview;
import dataview.models.InputPort;
import dataview.models.JSONArray;
import dataview.models.JSONObject;
import dataview.models.JSONParser;
import dataview.models.Task;

public class TaskExecutor {
	private final Lock lock = new ReentrantLock();
	private SSLSocket server;
	private SSLServerSocket serverSocket;
	private BufferedReader input;
	private PrintStream output;
	private HashMap<String, String> childrenMap = new HashMap<String, String>();
	private HashMap<String, String> parentMap = new HashMap<String, String>();
	private HashMap<String, Condition> mapCondition = new HashMap<String, Condition>();
	private HashMap<String, Boolean> isExecuted = new HashMap<String, Boolean>();
	private HashMap<Task, String> tasks = new HashMap<Task, String>();
	private HashMap<String, String> taskVMmap = new HashMap<String, String>();
	private final int PORT_NO = 7700;
	// public static HashMap<String, Boolean> isConfidential = new HashMap<String,
	// Boolean>();
	public static HashSet<String> confidentialTasks = new HashSet<String>();

	private long startTime;
	private long finishTime;

	// VMProvisioner vmProvisioner;

	public static void main(String[] args) throws Exception {

		Dataview.debugger.logSuccessfulMessage("Starting TaskExecutor");
		TaskExecutor taskExecutor = new TaskExecutor();
		Dataview.debugger.logSuccessfulMessage("Loading Confidential tasks list from config.txt");
		taskExecutor.loadConfidentialTaskNames();
		taskExecutor.connectWithWorkflowExecutor();
	}

	public void run() throws Exception {
		Dataview.debugger.logSuccessfulMessage("Starting TaskExecutor");
		TaskExecutor taskExecutor = new TaskExecutor();
		taskExecutor.loadConfidentialTaskNames();
		taskExecutor.connectWithWorkflowExecutor();
	}

	private void loadConfidentialTaskNames() {
		String input = "";
		File file = new File("/home/ubuntu/config.txt");
		BufferedReader bufferredReader = null;
		try {
			bufferredReader = new BufferedReader(new FileReader(file));
			String line;
			while ((line = bufferredReader.readLine()) != null) {
				input += line;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		JSONParser parser = new JSONParser(input);
		JSONObject taskObjects = parser.parseJSONObject();
		JSONArray confidentialTasksJSONArray = taskObjects.get("confidentialTasks").toJSONArray();

		for (int i = 0; i < confidentialTasksJSONArray.size(); i++) {
			JSONObject task = confidentialTasksJSONArray.get(i).toJSONObject();
			String taskName = (String) task.get("taskName").toString().replace("\"", "");
			;
			confidentialTasks.add(taskName);
		}

	}

	void initialize(String localScheduleSpec) {
		// replaced special character by new line to construct original
		// localSchedulespec
		localScheduleSpec = localScheduleSpec.replace("$$&&", "\n");

		JSONParser parser = new JSONParser(localScheduleSpec);
		JSONObject lsch = parser.parseJSONObject();
		JSONArray tschs = lsch.get("taskSchedules").toJSONArray();

		int totalTasks = tschs.size();

		for (int i = 0; i < totalTasks; i++) {
			JSONObject taskspec = tschs.get(i).toJSONObject();
			String myTaskInstanceID = (String) taskspec.get("taskInstanceID").toString().replace("\"", "");

			// sample task instance id comes with sample '@32498'. Hence needs to trim for
			// colldcting task name
			String myTaskName = myTaskInstanceID.split("@")[0];

			try {
				Class<?> taskclass = Class.forName(myTaskName);
				Task myTask = (Task) taskclass.newInstance();

				tasks.put(myTask, myTaskInstanceID);
				Condition condition = lock.newCondition();
				mapCondition.put(myTaskInstanceID, condition);
				isExecuted.put(myTaskInstanceID, false);

				// for each input incoming data channel of t
				JSONArray indcs = taskspec.get("incomingDataChannels").toJSONArray();
				for (int j = 0; j < indcs.size(); j++) {

					JSONObject indc = indcs.get(j).toJSONObject();
					String parentTaskInstanceID = indc.get("srcTask").toString().replace("\"", "");
					if (!parentTaskInstanceID.isEmpty()) {
						String parentOutputPortIndex = indc.get("outputPortIndex").toString().replace("\"", "");
						parentMap.put(myTaskInstanceID + "_" + j, parentTaskInstanceID + "_" + parentOutputPortIndex);
						isExecuted.put(parentTaskInstanceID, false);

						if (confidentialTasks.contains(myTaskInstanceID.split("@")[0])
								|| confidentialTasks.contains(parentTaskInstanceID.split("@")[0])) {
							myTask.ins[j].setLocation("/home/ubuntu/" + myTaskInstanceID + "_in" + j + ".enc");
						} else {
							myTask.ins[j].setLocation("/home/ubuntu/" + myTaskInstanceID + "_in" + j + ".txt");
						}

					} else {
						String srcFilename = indc.get("srcFilename").toString().replace("\"", "");
						myTask.ins[j].setLocation("/home/ubuntu/" + srcFilename);
					}
				}

				// for each outgoing data channel of t
				JSONArray outdcs = taskspec.get("outgoingDataChannels").toJSONArray();
				for (int j = 0; j < outdcs.size(); j++) {

					JSONObject outdc = outdcs.get(j).toJSONObject();
					String childTaskInstanceID = outdc.get("destTask").toString().replace("\"", "");
					;
					if (!childTaskInstanceID.isEmpty()) {
						String childInputPortIndex = outdc.get("inputPortIndex").toString().replace("\"", "");
						childrenMap.put(myTaskInstanceID + "_" + j, childTaskInstanceID + "_" + childInputPortIndex);
						String childVMIP = outdc.get("destIP").toString().replace("\"", "");
						taskVMmap.put(childTaskInstanceID, childVMIP);
						String val = myTaskInstanceID.split("@")[0];

						@SuppressWarnings("rawtypes")
						Iterator iterator = confidentialTasks.iterator();
						System.out.println(val + "XXXX");
						// check values
						while (iterator.hasNext()) {
							System.out.println("Value: " + iterator.next() + "XXX");
						}

						if (confidentialTasks.contains(val)
								|| confidentialTasks.contains(childTaskInstanceID.split("@")[0])) {
							myTask.outs[j].setLocation("/home/ubuntu/" + myTaskInstanceID + "_out" + j + ".enc");
						} else {
							myTask.outs[j].setLocation("/home/ubuntu/" + myTaskInstanceID + "_out" + j + ".txt");
						}

					} else {
						String srcFilename = outdc.get("destFilename").toString().replace("\"", "");
						myTask.outs[j].setLocation("/home/ubuntu/" + srcFilename);
					}
				}

			} catch (Exception e) {
				e.printStackTrace();
			}

		}

		startTasks();

	}

	void startTasks() {
		for (Task task : tasks.keySet()) {
			new Thread(new Runnable() {
				@Override
				public void run() {
					onTaskRunStart(task);
					executeTask(task);
					onTaskRunFinish(task);

				}
			}).start();

		}
	}

	void connectWithWorkflowExecutor() throws IOException {



		try {
			Dataview.debugger.logSuccessfulMessage("Creating secure Socket connection thorugh port 7700");
			// loading certificate keystore from a given file
			KeyStore ks = KeyStore.getInstance("JKS");
			ks.load(new FileInputStream("/home/ubuntu/clientkeystore"), "dataview".toCharArray());
			KeyManagerFactory kmf = KeyManagerFactory.getInstance("sunX509");
			kmf.init(ks, "dataview".toCharArray());
			TrustManagerFactory tmf = TrustManagerFactory.getInstance("sunX509");
			tmf.init(ks);
			SSLContext sc = SSLContext.getInstance("TLS");
			TrustManager[] trustManagers = tmf.getTrustManagers();
			sc.init(kmf.getKeyManagers(), trustManagers, null);
			SSLServerSocketFactory ssf = sc.getServerSocketFactory();
			serverSocket= (SSLServerSocket) ssf.createServerSocket(PORT_NO);
			System.out.println("SSL ServerSocket started");
			server = (SSLSocket) serverSocket.accept();
			System.out.println("SSL ServerSocket Accepted");

			input = new BufferedReader(new InputStreamReader(server.getInputStream()));
			output = new PrintStream(server.getOutputStream());

		} catch(Exception exception) {
			Dataview.debugger.logException(exception);
		}





		// vmProvisioner = new VMProvisioner(1);

		while (server.isConnected()) {
			String message = input.readLine();
			if (message != null) {
				Dataview.debugger.logSuccessfulMessage("Client : " + message);

				String[] parts = message.split("XX");
				if (parts[0].equals("initialization")) {
					Dataview.debugger.logSuccessfulMessage("Initialization signal is received");
					initialize(parts[1]);
				} else if (parts[0].equals("signal")) {
					lock.lock();
					if (!isExecuted.get(parts[1])) {
						isExecuted.put(parts[2], true);
						Dataview.debugger.logSuccessfulMessage("Signal details : " + message);
						mapCondition.get(parts[1]).signal();
					}
					lock.unlock();

				} else if (parts[0].equals("terminate")) {
					Dataview.debugger.logSuccessfulMessage("Terminating Task executor");
					output.println("terminateThread");
					server.close();
					serverSocket.close();
					System.exit(0);
				}

				// log += "Ending time : " + System.currentTimeMillis();
			}
		}
		serverSocket.close();
	}

	/**
	 * Checking whether parent task is finished
	 * 
	 * @param id
	 * @return boolean
	 */
	boolean isParentExecuted(Task task) {

		for (int i = 0; i < task.ins.length; i++) {
			String thisTaskInstanceID = tasks.get(task);

			// incoming port contains file
			if (!parentMap.containsKey(thisTaskInstanceID + "_" + i)) {
				continue;
			}
			// since parentMap values return parentIP with port, we need to eliminate the
			// port number
			String parentTaskInstanceID = parentMap.get(thisTaskInstanceID + "_" + i).split("_")[0];

			if (!isExecuted.get(parentTaskInstanceID)) {
				return false;
			}
		}

		return true;
	}

	private void onTaskRunStart(Task task) {

		while (!isParentExecuted(task)) {
			lock.lock();
			try {
				Dataview.debugger.logSuccessfulMessage("Waiting : " + tasks.get(task));
				mapCondition.get(tasks.get(task)).await();
				Dataview.debugger.logSuccessfulMessage("Woke up : " + tasks.get(task));
			} catch (Exception e) {
				e.printStackTrace();
			}
			lock.unlock();
		}

	}

	void executeTask(Task task) {
		Dataview.debugger.logSuccessfulMessage("Starting execution : " + tasks.get(task));
		startTime = System.currentTimeMillis();
		task.run();
	}

	private void onTaskRunFinish(Task task) {
		lock.lock();
		Dataview.debugger.logSuccessfulMessage("Finished execution : " + tasks.get(task));
		ArrayList<String> childTaskNamesAssociatedWithDifferentIP = new ArrayList<String>();
		for (int i = 0; i < task.outs.length; i++) {

			String input = "";
			/*
			 * File file = new File(task.outs[i].getFileName()); BufferedReader
			 * bufferredReader = null; try { bufferredReader = new BufferedReader(new
			 * FileReader(file)); String line; while ((line = bufferredReader.readLine()) !=
			 * null) { input += line; } } catch (Exception e) { e.printStackTrace(); }
			 */

			// write to the file
			Task child = null;
			String[] parts = null;
			// BufferedWriter writer = null;
			try {
				// output port is not connected to any task
				if (!childrenMap.containsKey(tasks.get(task) + "_" + i)) {
					continue;
				}
				String value = childrenMap.get(tasks.get(task) + "_" + i);
				parts = value.split("_");
				String location = null;
				for (Task t : tasks.keySet()) {
					if (tasks.get(t).equalsIgnoreCase(parts[0])) {
						child = t;
						break;
					}
				}

				if (child != null) {
					InputPort inputPort = child.ins[Integer.parseInt(parts[1])];
					location = inputPort.getFileName();

					////////// Code for adjusting the name that copies the old output as a new
					////////// input.

					try (FileInputStream in = new FileInputStream(task.outs[i].getFileName());
							FileOutputStream out = new FileOutputStream(location)) {
						byte[] ibuf = new byte[1];
						
						@SuppressWarnings("unused")
						int len;
						while ((len = in.read(ibuf)) != -1) {
							out.write(ibuf);
						}
						in.close();
						out.close();
					}
					Dataview.debugger.logSuccessfulMessage("new file  " + location + ":" + input);
				}
			} catch (Exception ex) {
				Dataview.debugger.logException(ex);
			}
			isExecuted.put(tasks.get(task), true);
			if (child != null && mapCondition.get(tasks.get(child)) != null) {
				Dataview.debugger.logSuccessfulMessage("Sending sginal for starting " + tasks.get(child));
				mapCondition.get(tasks.get(child)).signal();
			}

			// ******************
			// Sending output to remote
			else {
				childTaskNamesAssociatedWithDifferentIP.add(parts[0]);
				String transferredFileName = "";

				if (confidentialTasks.contains(parts[0].split("@")[0])
						|| confidentialTasks.contains(task.taskName.split("@")[0])) {
					transferredFileName = parts[0] + "_in" + parts[1] + ".enc";
				} else {
					transferredFileName = parts[0] + "_in" + parts[1] + ".txt";
				}
				String remoteMachineIP = taskVMmap.get(parts[0]);
				// copyFile(task.outs[i].getFileName(), "/home/ubuntu/", transferredFileName,
				// remoteMachineIP, "dataview1.pem");
				if (confidentialTasks.contains(parts[0].split("@")[0])) {
					copyFileSGX(task.outs[i].getFileName(), "/home/ubuntu/", transferredFileName, remoteMachineIP);
				} else {
					copyFileVM(task.outs[i].getFileName(), "/home/ubuntu/", transferredFileName, remoteMachineIP, "/home/ubuntu/dataview1.pem");
				}

				Dataview.debugger.logSuccessfulMessage(
						"Original file name : " + task.outs[i].getFileName() + "transferred file name : "
								+ transferredFileName + " transferred to remote machine : " + remoteMachineIP);
			}
			// ******************
		}

		if (server != null && server.isConnected()) {
			Dataview.debugger
			.logSuccessfulMessage("Sending sginal for successfully finished execution of " + tasks.get(task));
			String signal = "SuccessXX" + tasks.get(task);
			for (String childTaskName : childTaskNamesAssociatedWithDifferentIP) {
				signal += ("XX" + childTaskName);
			}

			finishTime = System.currentTimeMillis();
			long executionTime = finishTime - startTime;
			output.println(signal);
			output.println("ExecutionTimeXX" + tasks.get(task) + "XX" + executionTime);

		}
		lock.unlock();
	}

	void copyFileSGX(String SourceDIR, String DestinationDIR, String newFileName, String strHostName) {
		String SFTPHOST = strHostName;
		int SFTPPORT = 8000;
		String SFTPUSER = "ubuntu";
		String SFTPWORKINGDIR = DestinationDIR;
		String FILETOTRANSFER = SourceDIR;
		Session session = null;
		Channel channel = null;
		ChannelSftp channelSftp = null;
		try {
			JSch jsch = new JSch();
			// jsch.addIdentity(pemFileLocation);
			session = jsch.getSession(SFTPUSER, SFTPHOST, SFTPPORT);
			session.setPassword("dataview");
			java.util.Properties config = new java.util.Properties();
			config.put("StrictHostKeyChecking", "no");
			session.setTimeout(0);
			session.setConfig(config);
			session.setPort(8000);
			session.connect();
			channel = session.openChannel("sftp");
			channel.connect();
			channelSftp = (ChannelSftp) channel;
			channelSftp.cd(SFTPWORKINGDIR);
			File f = new File(FILETOTRANSFER);
			channelSftp.put(new FileInputStream(f), newFileName);

			channel.disconnect();
			session.disconnect();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	void copyFileVM(String SourceDIR, String DestinationDIR, String newFileName,
			String strHostName, String pemFileLocation) {
		String SFTPHOST = strHostName;
		int SFTPPORT = 22;
		String SFTPUSER = "ubuntu";
		String SFTPWORKINGDIR = DestinationDIR;
		String FILETOTRANSFER = SourceDIR;
		Session session = null;
		Channel channel = null;
		ChannelSftp channelSftp = null;
		try {
			JSch jsch = new JSch();
			jsch.addIdentity(pemFileLocation);
			session = jsch.getSession(SFTPUSER, SFTPHOST, SFTPPORT);
			//			session.setPassword("123");
			java.util.Properties config = new java.util.Properties();
			config.put("StrictHostKeyChecking", "no");
			session.setTimeout(0);
			session.setConfig(config);
			session.setPort(22);
			session.connect();
			channel = session.openChannel("sftp");
			channel.connect();
			channelSftp = (ChannelSftp) channel;
			channelSftp.cd(SFTPWORKINGDIR);
			File f = new File(FILETOTRANSFER);
			channelSftp.put(new FileInputStream(f), newFileName);
			Dataview.debugger.logSuccessfulMessage("File " + f.getName() + " has been copied");
			channel.disconnect();
			session.disconnect();
		} catch (Exception ex) {
			ex.printStackTrace();
		}

	}
}
