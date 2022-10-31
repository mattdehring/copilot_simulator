package com.rjginc.copilot.copilot_simulator;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.LongBuffer;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.PostConstruct;
import javax.inject.Named;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Stopwatch;
import com.rjginc.esm.core.model.CouchBaseType;
import com.rjginc.esm.core.model.display.MachineState;
import com.rjginc.esm.core.model.edart.ChangeLog;
import com.rjginc.esm.core.model.edart.Copilot;
import com.rjginc.esm.core.model.edart.CopilotState;
import com.rjginc.esm.core.model.job.Cycle;
import com.rjginc.esm.core.model.job.Cycle.Counts;
import com.rjginc.esm.core.model.job.DownEvent;
import com.rjginc.esm.core.model.job.Job;
import com.rjginc.esm.core.model.job.JobAlarms;
import com.rjginc.esm.core.model.job.JobLegend;
import com.rjginc.esm.core.model.job.JobSummaryLegend;
import com.rjginc.esm.core.model.job.cycle.SummaryDataElement;
import com.rjginc.esm.core.model.sync.CopilotActivated;
import com.rjginc.esm.core.model.sync.CopilotConnected;
import com.rjginc.esm.core.model.sync.DocumentUpdate;
import com.rjginc.esm.core.model.sync.SyncAck;
import com.rjginc.esm.core.model.sync.SyncData;
import com.rjginc.esm.core.model.sync.SyncRawData;
import com.rjginc.esm.persistance.JsonConverter;
import com.rjginc.interop.messages.InteropMessage;
import com.rjginc.leapfrog.domain.LeapEntity;
import com.rjginc.leapfrog.job.domain.Machine;
import com.rjginc.leapfrog.job.domain.Mold;
import com.rjginc.leapfrog.job.domain.Process;
import com.rjginc.leapfrog.job.domain.SetupSheet;
import com.rjginc.leapfrog.web.JsonViews.InternalFull;

@Component
@Scope("prototype")
public class SimulatedCopilot
{
	private static final Logger log = LoggerFactory.getLogger(SimulatedCopilot.class);
	
	
	private static class SimulatorState
	{
		DownEvent downEvent = null;
		int downEventCount = 0;
		int downEventIndex = 0;
		int downEventIteration = 0;
		long nextDownEventTime = 0;

		int cycleCount = 0;
		int cycleIndex = 0;
		int cycleIteration = 0;
		long nextCycleTime = 0;
		
		int requestedCycles = 10;
		
		long jobStartTime = -1;
		long currentJobTime = -1;
		long endTime = Long.MAX_VALUE;
		long endJobTime = Long.MAX_VALUE;
		long timeOffset = -1;
		long jobTimeDuration;
		public Counts cycleCounts;
		
		List<ChangeLog> changeLogs = new ArrayList<>();
		int changeLogPos = 0;		
		
		public SimulatorState()
		{
			super();
		}

		public SimulatorState(SimulatorState simulatorState)
		{
			super();
			this.downEvent = simulatorState.downEvent;
			this.downEventCount = simulatorState.downEventCount;
			this.downEventIndex = simulatorState.downEventIndex;
			this.downEventIteration = simulatorState.downEventIteration;
			this.nextDownEventTime = simulatorState.nextDownEventTime;
			this.cycleCount = simulatorState.cycleCount;
			this.cycleIndex = simulatorState.cycleIndex;
			this.cycleIteration = simulatorState.cycleIteration;
			this.nextCycleTime = simulatorState.nextCycleTime;
			this.jobStartTime = simulatorState.jobStartTime;
			this.currentJobTime = simulatorState.currentJobTime;
			this.endTime = simulatorState.endTime;
			this.endJobTime = simulatorState.endJobTime;
			this.timeOffset = simulatorState.timeOffset;
			this.jobTimeDuration = simulatorState.jobTimeDuration;
			this.requestedCycles = simulatorState.requestedCycles;
			this.cycleCounts = simulatorState.cycleCounts;
			
			
		}
	}

	@Autowired
	HubNettyClient connection;
	HubClientHandler hubClientHandler = null;

	Copilot copilot;
	CopilotState copilotState;

	@Autowired
	EntityConfigurationService machineSimulatorService;

	@Autowired
	CopilotService copilotService;

	@Autowired
	JobDataCacheService jobDataCacheService;

	@Autowired
	private TaskScheduler taskExecutor;
	
	@Autowired
    JsonConverter jsonConverter;

	JobDataCache jobDataCache;

	Job jobData = null;

	Job currentJob = null;
	JobLegend jobLegend = null;

	Machine machine = null;
	MachineState machineState = null;
	Mold mold = null;
	Process process = null;
	SetupSheet setupSheet = null;
	
	AtomicInteger documentsSend = new AtomicInteger();
	AtomicInteger byteArraySent = new AtomicInteger();
	AtomicInteger ackRecieved = new AtomicInteger();

//	DownEvent downEvent = null;
//	int downEventCount = 0;
//	int downEventIndex = 0;
//	int downEventIteration = 0;
//	long nextDownEventTime = 0;
//
//	int cycleCount = 0;
//	int cycleIndex = 0;
//	int cycleIteration = 0;
//	long nextCycleTime = 0;
//
//	int requestedCycles = 10;
//
//	
//	long jobStartTime = -1;
//	long currentJobTime = -1;
//	long endTime = Long.MAX_VALUE;
//	long endJobTime = Long.MAX_VALUE;
//	long timeOffset = -1;
//	long jobTimeDuration;
	
	SimulatorState simulatorState = new SimulatorState();
	SimulatorState previousSimulatorState = simulatorState; 
	
	Cycle cycle;

	//Boolean writable = false;
	AtomicBoolean writable = new AtomicBoolean(false);
	Boolean connected = false;

	@Autowired
	Settings settings;
	
	@Named("couchObjectMapper")
	@Autowired
	ObjectMapper mapper;
	
	@Autowired
	@Named("leapObjectMapper")	
	ObjectMapper leapMapper;
	
	

	static int count = 0;

	static class SyncProperties
	{
		String key;
		Instant time;
//		InteropMessage message;
		int revision = 0;
		SimulatorState simulatorState = null;
	}

	Map<String, SyncProperties> syncMap = new LinkedHashMap<>();
	Map<UUID, SyncProperties> crossCopySyncMap = new LinkedHashMap<>();

	NamedRunnable nextTask = null;
	Date nextTaskTime = null;
	ScheduledFuture<?> nextTaskScheduled = null;

	Random rand = new Random();

	boolean complete = false;

	public SimulatedCopilot()
	{
		super();
	}

	@PostConstruct
	public void init()
	{
		count++;
		copilot = copilotService.getCopilot();
		copilotState = copilotService.getCopilotState(copilot);

		machine = machineSimulatorService.getMachine();
		MDC.put("machine", machine.getName());
//		copilotState.setMachineKey(machine.getKey());
		copilotState.setMachineId(machine.getId());

		connection.setSimulatedCopilot(this);
		connection.connect();

		//		currentJobTime = Instant.now().minusSeconds(60*60*24).toEpochMilli();

		if (settings.getStartDate() != null)
		{
			simulatorState.currentJobTime = settings.getStartDate().getTime();
		}
		else if (settings.getStartTime() != null)
		{
			simulatorState.currentJobTime = Instant.now().plus(settings.getStartTime()).toEpochMilli();
		}
		else
		{
			simulatorState.currentJobTime = Instant.now().toEpochMilli();
		}
		if (settings.getEndDate() != null)
		{
			simulatorState.endTime = settings.getEndDate().getTime();
		}
		else if (settings.getEndTime() != null)
		{
			simulatorState.endTime = Instant.now().plus(settings.getEndTime()).toEpochMilli();
		}

		//		complete = !settings.isKeepRunning();
		MDC.clear();

		simulatorState.currentJobTime += rand.nextGaussian() * settings.jobStopTime.toMillis() * .25 + settings.jobStopTime.toMillis(); // this should be something is the configuration
		scheduleNextEvent();
	}

	public void onConnect(HubClientHandler hubClientHandler)
	{
		log.info("Connected");
		this.hubClientHandler = hubClientHandler;		
		this.hubClientHandler.writeAndFlush(new CopilotConnected(System.currentTimeMillis(), copilot, copilotState));
				
		//instead of saving the messages I am going to reset the simulator state back to the oldest unacknowledged message
		

//		List<SyncProperties> docs = new ArrayList<>(syncMap.values());
//		for (int i = 0; i < docs.size(); i++)
//		{
//			SyncProperties syncProperties = docs.get(i);
//			while (!hubClientHandler.isWritable()) // wait for connection to be writable
//			{
//				try
//				{
//					if (!hubClientHandler.isConnected()) { return; }
//					Thread.sleep(100);
//				}
//				catch (InterruptedException e)
//				{
//					// TODO Auto-generated catch block
//					log.warn("Sleep Interupted", e);
//				}
//			}
//			if (hubClientHandler.isConnected())
//			{
//				syncProperties.time = Instant.now();
//				hubClientHandler.writeAndFlush(syncProperties.message);
//			}
//			else
//			{
//				// 
//				return;
//			}
//		}
		connected = true;
//		onWritabilityChanged(true);
	}

	private void getNextCycleTime()
	{
		try
		{
			simulatorState.cycleIndex = simulatorState.cycleCount % jobData.getTotalCount();
			simulatorState.cycleIteration = simulatorState.cycleCount / jobData.getTotalCount();
			cycle = jobDataCache.getCycle(simulatorState.cycleIndex);
			simulatorState.nextCycleTime = computeTime(cycle.getTimestamp(), simulatorState.cycleIteration);
		}
		catch (Exception e)
		{
			log.warn("Could not read cycle", e);
		}
	}

	private long computeTime(long oldTime, int iteration)
	{
		return oldTime + simulatorState.timeOffset + iteration * simulatorState.jobTimeDuration;
	}

	static abstract class NamedRunnable implements Runnable
	{
		String name;

		public NamedRunnable(String name)
		{
			super();
			this.name = name;
		}
		//		@Override
		//		public void run()
		//		{
		//			// TODO Auto-generated method stub
		//			
		//		}
	}

	private void scheduleNextEvent()
	{		
		previousSimulatorState = new SimulatorState(simulatorState);
		if (complete) { return; }
		synchronized (this.writable)
		{
			if (jobData == null)
			{
				Runnable r = () -> {
					onStartJob();
				};
				scheduleNextEvent(r, new Date(simulatorState.currentJobTime), "Job Start"); //this will not delay
			}
			else if (simulatorState.currentJobTime > simulatorState.endTime)
			{
				Runnable r = () -> {
					complete = true;
					if (!currentJob.isRunning())
					{
						log.info("already stopped");
					}
					onStopJob();
				};
				scheduleNextEvent(r, new Date(simulatorState.currentJobTime), "Job Stop1"); //this will not delay
			}
			else if (simulatorState.currentJobTime > simulatorState.endJobTime)
			{
				Runnable r = () -> {
					onStopJob();
				};
				scheduleNextEvent(r, new Date(simulatorState.currentJobTime), "Job Stop2"); //this will not delay
			}
			else if (simulatorState.cycleCount >= simulatorState.requestedCycles)
			{
				Runnable r = () -> {
					onStopJob();
				};
				scheduleNextEvent(r, new Date(simulatorState.currentJobTime), "Job Stop3"); //this will not delay
			}
			else if (simulatorState.downEvent == null)
			{
				Runnable r = () -> {
					endDownTime();
				};
				scheduleNextEvent(r, new Date(simulatorState.nextDownEventTime), "End Down " + simulatorState.downEventCount);
			}
			else if (simulatorState.nextDownEventTime < simulatorState.nextCycleTime)
			{
				Runnable r = () -> {
					startDownTime();
				};
				scheduleNextEvent(r, new Date(simulatorState.nextDownEventTime), "Start Down " + simulatorState.cycleCount);
			}
			else
			{
				Runnable r = () -> {
					onCycle();
				};
				scheduleNextEvent(r, new Date(simulatorState.nextCycleTime), "Job Cycle " + simulatorState.cycleCount);
			}
		}
	}

	private void scheduleNextEvent(Runnable task, Date time, String name)
	{
		synchronized (this.writable)
		{
			NamedRunnable r = new NamedRunnable(name)
				{
					@Override
					public void run()
					{
						if (!connected) {
							log.info("Disconnected don't execute task {}",name);
							return;
						}
						
						synchronized (SimulatedCopilot.this.writable)
						{
							try {							
							
								MDC.put("machine", machine.getName());
								log.trace("Start task {}", name);
								nextTask = null;
								nextTaskTime = null;
								nextTaskScheduled = null;
								task.run();
								scheduleNextEvent();
							}
							catch (Exception e) {
								log.error("Exception in {}", name,e);
							}
							finally {
								MDC.clear();
							}
							
						}
					}
				};
			
		
			if (nextTaskScheduled != null)
			{
				log.info("Task already scheduled");
			}
	
			nextTask = r;
			nextTaskTime = time;
			simulatorState.currentJobTime = time.getTime();
			if (this.writable.get())
			{
				log.trace("Next event {} {}", time, name);
				nextTaskScheduled = taskExecutor.schedule(r, time);
			}
			else
			{
				log.trace("Buffer full wait till {} {}", time, name);
			}
		}
	}

	public void onStartJob()
	{

		if (mold == null)
		{
			mold = machineSimulatorService.getMold();
			mold.setModified(simulatorState.currentJobTime);
			crossCopyDocument(mold);
		}
		if (process == null)
		{
			process = machineSimulatorService.getProcess(mold);
			process.setModified(simulatorState.currentJobTime);
			crossCopyDocument(process);
			
			setupSheet = machineSimulatorService.getSetupSheet(process, machine);
			setupSheet.setModified(simulatorState.currentJobTime);
			crossCopyDocument(setupSheet);
		}

		try
		{
			jobDataCache = jobDataCacheService.getJobDataCache((String) mold.getProperties().get("dataset"));
			jobData = jobDataCache.getJob();
		}
		catch (IOException e)
		{
			log.warn("Could not load job data", e);
		}

		if (settings.isUseJobCount())
		{
			simulatorState.requestedCycles = jobData.getTotalCount();
		}
		else
		{
			simulatorState.requestedCycles = settings.getCycleCount();
		}

		if (settings.isRandomCycleCount())
		{
			simulatorState.requestedCycles = (int) (simulatorState.requestedCycles * .25 * rand.nextGaussian() + simulatorState.requestedCycles);
		}

		if (settings.getJobTime() != null)
		{
			simulatorState.endJobTime = Instant.ofEpochMilli(simulatorState.currentJobTime).plus(settings.getJobTime()).toEpochMilli();
		}

		log.info("Job cycles needed {}", simulatorState.requestedCycles);

		simulatorState.jobStartTime = simulatorState.currentJobTime;//  System.currentTimeMillis();
		simulatorState.timeOffset = simulatorState.jobStartTime - jobData.getStartTime();
		simulatorState.jobTimeDuration = jobData.getEndTime() - jobData.getStartTime();
		simulatorState.cycleCount = 0;
		simulatorState.downEventCount = 0;
		simulatorState.downEventIndex = 0;
		simulatorState.downEventIteration = 0;
		simulatorState.downEvent = null;

		currentJob = new Job(copilot.getKey(), simulatorState.jobStartTime);
		currentJob.setDataLevel(null);
		currentJob.setVersion(jobData.getVersion());
		currentJob.setJobAlarms(new JobAlarms(currentJob.getKey()));
		currentJob.setDevice(copilot);
		currentJob.setMachineId(machine.getId());
		currentJob.setMachineRev(machine.getRev());
		currentJob.setMachineName(machine.getName());
		currentJob.setMoldId(mold.getId());
		currentJob.setMoldRev(mold.getRev());
		currentJob.setMoldName(mold.getName());
		currentJob.setProcessId(process.getId());
		currentJob.setProcessRev(process.getRev());
		currentJob.setProcessName(process.getName());
		currentJob.setSetupSheetId(setupSheet.getId());
		currentJob.setSetupSheetRev(setupSheet.getRev());
				
		currentJob.setStandardCycleTime(jobDataCache.getJob().getStandardCycleTime());
		
		
		//      currentJob.getDownEvents().add(new com.rjginc.esm.core.model.job.DownEvent(jobStartTime, "Start of Job"));
		currentJob.startDownTime(simulatorState.jobStartTime, "Start of Job");
		simulatorState.nextDownEventTime = computeTime(jobData.getDownEvents().get(simulatorState.downEventIndex).getEndTimestamp(), simulatorState.downEventIteration);
		//		downEvent = jobData.getDownEvents().get(0); //we always start with a down time so we will just get it to start off with

		
//		JobConfigs jobConfigs = jobDataCache.getJobConfigs();
//		if (jobConfigs != null)
//		{
//			jobConfigs.setJobKey(currentJob.getKey());
//			syncDocument(jobConfigs);
//		}
		

//		if (jobData.getVersion() >= 4)
//		{
//			
//			com.rjginc.leapfrog.job.domain.Machine machineConfigs = jobDataCache.getMachine();
//			com.rjginc.leapfrog.job.domain.Mold moldConfigs = jobDataCache.getMold();
//			com.rjginc.leapfrog.job.domain.Process processConfigs = jobDataCache.getProcess();
//			com.rjginc.leapfrog.job.domain.SetupSheet setupSheet = jobDataCache.getSetupSheet();
//			
//			crossCopyDocument(machineConfigs);
//			crossCopyDocument(moldConfigs);
//			crossCopyDocument(processConfigs);
//			crossCopyDocument(setupSheet);
//			
//			currentJob.setMachineId(machineConfigs.getId());
//			currentJob.setMachineRev(machineConfigs.getRev());
//			currentJob.setMoldId(moldConfigs.getId());
//			currentJob.setMoldRev(moldConfigs.getRev());
//			currentJob.setProcessId(processConfigs.getId());
//			currentJob.setProcessRev(processConfigs.getRev());
//			currentJob.setSetupSheetId(setupSheet.getId());
//			currentJob.setSetupSheetRev(setupSheet.getRev());
//		
//		}
		
		machineState.setJobKey(currentJob.getKey());
		syncDocument(machineState);
		copilotState.setJobKey(currentJob.getKey());
		syncDocument(copilotState);

		jobLegend = new JobLegend();
		jobLegend.setJobKey(currentJob.getKey());
		jobLegend.setSummaryLegends(jobDataCache.getJobLegend().getSummaryLegends());
		jobLegend.setCycleLegends(jobDataCache.getJobLegend().getCycleLegends());
		syncDocument(jobLegend);
		syncDocument(currentJob);
		
		simulatorState.changeLogs = new ArrayList<>(jobDataCache.getChangeLogs());
		Collections.sort(simulatorState.changeLogs, new Comparator<ChangeLog>()
			{
				@Override
				public int compare(ChangeLog o1, ChangeLog o2)
				{
					return Long.compare(o1.getTimestamp(), o2.getTimestamp());
				}
			});

		getNextCycleTime();
		//onCycle();
	}

	public void onStopJob()
	{
		jobData = null;
		currentJob.stopJob();
		currentJob.setEndTime(simulatorState.currentJobTime);
		syncDocument(currentJob);
		copilotState.setJobKey(null);
		syncDocument(copilotState);
		
		if ( rand.nextDouble() < .1)
		{
		    machineSimulatorService.freeProcess(process);		    
		    machineSimulatorService.releaseMold(mold);
		    mold = null;
		    process = null;
		}
		else if ( rand.nextDouble() < .3)
                {
		    machineSimulatorService.freeProcess(process);
		    process = null;
                }

		//		currentJobTime += rand.nextGaussian() * 30 * 1000 + 60 * 2 * 1000; // this should be something is the configuration
		simulatorState.currentJobTime += rand.nextGaussian() * settings.jobStopTime.toMillis() * .25 + settings.jobStopTime.toMillis();

		if (complete || !settings.isKeepRunning())
		{
			complete = true;
			
			Runnable r = new Runnable()
				{

					@Override
					public void run()
					{
						// TODO Auto-generated method stub
						Stopwatch timeout = Stopwatch.createStarted();

						log.info("Copilot is done waiting for unsynced docs {} sent {} ack {}", syncMap.size(), documentsSend.get()+ byteArraySent.get(), ackRecieved.get());
						while (!syncMap.isEmpty())
						{
							if (timeout.elapsed(TimeUnit.MINUTES) > 10) {
								break;
							}
							//wasn't getting the last job update, I am assuming it didn't get sent before it was disconnected
							try
							{
								Thread.sleep(100);
							}
							catch (InterruptedException e)
							{
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
						log.info("Copilot is done {} sent {} ack {}", syncMap.size(), documentsSend.get()+ byteArraySent.get(), ackRecieved.get());
						connection.disconnect();
						copilotService.CopilotComplete();

					}
				};
				taskExecutor.schedule(r, new Date());
			//TODO this should maybe notify something so we can stop the process when everything is done
		}
	}

	private void endDownTime()
	{
		currentJob.endDownTime(simulatorState.nextDownEventTime);
		simulatorState.downEventCount++;
		simulatorState.downEventIndex = simulatorState.downEventCount % jobData.getDownEvents().size();
		simulatorState.downEventIteration = simulatorState.downEventCount / jobData.getDownEvents().size();
		simulatorState.downEvent = jobData.getDownEvents().get(simulatorState.downEventIndex);
		simulatorState.nextDownEventTime = computeTime(simulatorState.downEvent.getStartTimestamp(), simulatorState.downEventIteration);
	}

	//	private void startDownTime(long downstart,String reason)
	private void startDownTime()
	{
		currentJob.startDownTime(simulatorState.nextDownEventTime, simulatorState.downEvent.getReason());
		simulatorState.nextDownEventTime = computeTime(simulatorState.downEvent.getEndTimestamp(), simulatorState.downEventIteration);
		simulatorState.downEvent = null;//currentJob.getDownEvents().get(downEventCount);
	}

	public void onCycle()
	{		
		simulatorState.cycleCounts = currentJob.getLastCycle() != null? currentJob.getLastCycle().getCounts():null;
		int index = currentJob.getTotalCount() % jobDataCache.getJob().getTotalCount();

		try
		{
			//			Cycle cycle = jobDataCache.getCycle(cycleIndex);
			cycle.setTimestamp(simulatorState.nextCycleTime);
			cycle.setJobKey(currentJob.getKey());
			cycle.setIndex(currentJob.getTotalCount());

			for (Entry<String, JobSummaryLegend> entry : jobLegend.getSummaryLegends().entrySet())
			{
				String name = entry.getKey();
				float value = Float.NaN;
				Float[] data = jobDataCache.getSummaryData().get(name);
				value = data[index];
				cycle.getSummaryData().add(new SummaryDataElement(name, value));
			}

			currentJob.addCycle(cycle);

			syncDocument(cycle);
			syncDocument(currentJob);
			
			if(!cycle.getAlarms().isEmpty())
			{
				syncDocument(currentJob.getJobAlarms());
			}

			//byte[] cycledata = jobDataCache.getByteArray(String.format("CycleData|%d", index));
			byte[] cycleData = jobDataCache.getRawCycleData(index);
			syncByteArray(cycle.getCycleDataKey(), cycleData);

			int count = ((currentJob.getTotalCount() - 1) % currentJob.getBlockSize()) + 1;
			int blockId = (currentJob.getTotalCount() - 1) / currentJob.getBlockSize();

			syncByteArray(String.format("Summary|%s_timestamp_%d", currentJob.getJobId(), blockId), currentJob.getTimestamps(), count);

			for (Entry<String, Float[]> e : currentJob.getSummaryData().entrySet())
			{
				try
				{
					String name = String.format("Summary|%s_%s_%d", currentJob.getJobId(), e.getKey(), blockId);
					syncByteArray(name, e.getValue(), count);
				}
				catch (Exception ex)
				{
					System.out.println(ex.getMessage());
				}
			}
						
			simulatorState.cycleCount++;			
			for (int i = simulatorState.changeLogPos; i < simulatorState.changeLogs.size(); i++)
			{
				ChangeLog changeLog = simulatorState.changeLogs.get(i);
				if (changeLog.getTimestamp() + simulatorState.timeOffset <= cycle.getTimestamp())
				{
					//NOTE need to handle job iterations and reset at beginning of job data 
					
					ChangeLog newChangelLog = new ChangeLog();
					newChangelLog.setDeviceKey(currentJob.getDeviceKey());
					newChangelLog.setJobKey(currentJob.getKey());
					newChangelLog.setMachineId(currentJob.getMachineId());
					newChangelLog.setMoldId(currentJob.getMoldId());
					newChangelLog.setTimestamp(cycle.getTimestamp());
					newChangelLog.setEvent(changeLog.getEvent());
					newChangelLog.setOldValue(changeLog.getOldValue());
					newChangelLog.setNewValue(changeLog.getNewValue());
					newChangelLog.setUnits(changeLog.getUnits());
					newChangelLog.setUser(changeLog.getUser());
					for (Entry<String, Object> propString : changeLog.getProperties().entrySet())
					{
						newChangelLog.setProperty(propString.getKey(), propString.getValue());
					}
					
					syncDocument(newChangelLog);
					
					simulatorState.changeLogPos = i + 1;
				}
				else {
					break;
				}
			}
			
			getNextCycleTime();
			
			//taskExecutor.schedule(() -> onCycle(), d);
		}
		catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void syncByteArray(String key, long[] data, int count)
	{
		byte[] bytes = new byte[count * Long.BYTES];

		ByteBuffer bb = ByteBuffer.wrap(bytes);
		LongBuffer fb = bb.asLongBuffer();
		fb.put(data, 0, count);
		syncByteArray(key, bytes);
	}

	private void syncByteArray(String key, Number[] data, int count)
	{
		byte[] bytes = new byte[count * 4];

		ByteBuffer bb = ByteBuffer.wrap(bytes);
		FloatBuffer fb = bb.asFloatBuffer();
		for (int i = 0; i < count; i++)
		{
			fb.put(data[i].floatValue());
		}
		syncByteArray(key, bytes);
	}

	private void syncByteArray(String key, byte[] data)
	{
		byteArraySent.incrementAndGet();
		SyncProperties syncProperties = syncMap.get(key);
		if (syncProperties == null)
		{
			syncProperties = new SyncProperties();
			syncProperties.key = key;
			syncMap.put(key, syncProperties);
		}
		syncProperties.revision++;
		SyncRawData syncRawData = new SyncRawData(syncProperties.revision, key, data);
		syncProperties.simulatorState = previousSimulatorState;
		syncProperties.time = Instant.now();
		if (hubClientHandler.isConnected())
		{
			hubClientHandler.writeAndFlush(syncRawData);
		}
		else 
		{
			log.warn("Could not send message {}",key);
		}
	}

	private void crossCopyDocument(LeapEntity document)
	{
		//TODO this should check connection status and add the message to a map to handle disconnects
//		documentsSend.incrementAndGet();
		SyncProperties syncProperties = crossCopySyncMap.get(document.getId());
		if (syncProperties == null)
		{
			syncProperties = new SyncProperties();
			syncProperties.key = document.getName();
			crossCopySyncMap.put(document.getId(), syncProperties);
		}
		String docType = JsonConverter.getJsonType(document.getClass());
	
		JsonNode jsonNode = leapMapper.valueToTree(document);
//		syncProperties.revision++;
//		SyncData syncData = new SyncData( syncProperties.revision, document.getKey(), jsonNode);
		DocumentUpdate documentUpdate = new DocumentUpdate(document.getId(), document.getRev(), document.getParentRev(), document.getParentId(), document.getSchemaVersion(), document.getVersion(), document.getCreated(), document.getModified(), document.getName(), docType, true, jsonNode,false,"","");
		syncProperties.simulatorState = previousSimulatorState;
		syncProperties.time = Instant.now();
		if (hubClientHandler.isConnected())
		{
			hubClientHandler.writeAndFlush(documentUpdate);
		}
		if(!hubClientHandler.isConnected()) {
			log.warn("Could not send message {}",document.getName());
		}
	}
	
	private void syncDocument(CouchBaseType document)
	{
		//TODO this should check connection status and add the message to a map to handle disconnects
		documentsSend.incrementAndGet();
		SyncProperties syncProperties = syncMap.get(document.getKey());
		if (syncProperties == null)
		{
			syncProperties = new SyncProperties();
			syncProperties.key = document.getKey();
			syncMap.put(document.getKey(), syncProperties);
		}
		JsonNode jsonNode = mapper.valueToTree(document);
		syncProperties.revision++;
		SyncData syncData = new SyncData( syncProperties.revision, document.getKey(), jsonNode);
		syncProperties.simulatorState = previousSimulatorState;
		syncProperties.time = Instant.now();
		if (hubClientHandler.isConnected())
		{
			hubClientHandler.writeAndFlush(syncData);
		}
		if(!hubClientHandler.isConnected()) {
			log.warn("Could not send message {}",document.getKey());
		}
	}
/*	
	private void syncDocument(LeapEntity entity)
	{
		//TODO this should check connection status and add the message to a map to handle disconnects
//		documentsSend.incrementAndGet();
		
		try
		{
		String json = mapper.writerWithView(InternalFull.class).writeValueAsString(entity);
		JsonNode node = mapper.readTree(json); //this could just be the json string? or eventually LeapEntity or CouchbaseType
		if (hubClientHandler.isConnected()) {
            hubClientHandler.writeAndFlush(new DocumentUpdate(entity.getId(), entity.getRev(), entity.getParentRev(), entity.getParentId(), entity.getCurrentSchemaVersion(), entity.getVersion(), entity.getCreated(), entity.getModified(), entity.getName(), jsonConverter.getJsonType(entity.getClass()), true, node,entity.isArchived(),entity.getModifiedByUsername(),entity.getCreatedByUsername()), 2);
        }
		if(!hubClientHandler.isConnected()) {
			log.warn("Could not send message {}",entity.getName());
		}
		}
		catch (JsonProcessingException e)
		{
			log.warn("Error serializing entity {]", entity.getName());
		}
	}
*/

	public void onDisconnect()
	{
		connected = false;
		log.info("Disconnected");
		
		if (nextTaskScheduled != null ) {
			if (nextTaskScheduled.cancel(false))
			{
				//task was cancelled before running				
			}
			else
			{
				log.error("Could not cancel task {} {}", nextTaskTime, nextTask.name);
				nextTask = null; //NOTE make sure that task doesn't get started twice
			}
		}		
		onWritabilityChanged(false);
	}

	public void onMessage(InteropMessage message)
	{
		//System.out.println(message);

		if (message instanceof SyncAck)
		{
			ackRecieved.incrementAndGet();
			SyncAck ack = (SyncAck) message;
			SyncProperties syncProperties = syncMap.get(ack.getKey());
			if (syncProperties != null && ack.getRev() == syncProperties.revision)
			{
				syncMap.remove(ack.getKey());
				//				long timeToResponse = System.currentTimeMillis() - syncProperties.time.getTime();
				Duration duration = Duration.between(syncProperties.time, Instant.now());
				if (duration.getSeconds() > 10)
				{
					log.trace("It took {} to store {}", duration.toString(), ack.getKey());
				}
			}
			else
			{
				//the document has been updated in the meantime so can't do much
			}
		}
		else if ( message instanceof CopilotActivated)
		{
			CopilotActivated copilotConnected = (CopilotActivated) message;
						
			if (copilotConnected.isActivated())
			{
				if (!syncMap.isEmpty())
				{
					//find the oldest message and reset the state
					Instant t = Instant.MAX;
					SimulatorState state = null;
					SyncProperties first = null;
					
					for (SyncProperties sp : syncMap.values())
					{
						if (sp.time.isBefore(t))
						{
							t = sp.time;
							state = sp.simulatorState;
							first = sp;
						}
					}					
					simulatorState = state;
					
					log.info("Reseting job state from {} to {}", currentJob.getTotal(), simulatorState.cycleCounts.getTotal());
					
					//reset job counts
					if (simulatorState.cycleCounts != null)
					{
						currentJob.setAlarm(simulatorState.cycleCounts.getAlarm());
						currentJob.setExcessiveRejectCount(simulatorState.cycleCounts.getExcessiveReject());
						currentJob.setGood(simulatorState.cycleCounts.getGood());
						currentJob.setInjectionDisabledCount(simulatorState.cycleCounts.getInjectionDisabled());
						currentJob.setMachineMatch(simulatorState.cycleCounts.getMachineMatch());
						currentJob.setManual(simulatorState.cycleCounts.getManual());
						currentJob.setMaterialMatch(simulatorState.cycleCounts.getMaterialMatch());
						currentJob.setMoldMatch(simulatorState.cycleCounts.getMoldMatch());
						currentJob.setOverCycleTime(simulatorState.cycleCounts.getOverCycleTime());
						currentJob.setReject(simulatorState.cycleCounts.getReject());
						currentJob.setSecondaryVpCount(simulatorState.cycleCounts.getSecondaryVp());
						currentJob.setSortingDisabledCount(simulatorState.cycleCounts.getSortingDisabled());
						currentJob.setSuspect(simulatorState.cycleCounts.getSuspect());
						currentJob.setTotal(simulatorState.cycleCounts.getTotal());
						currentJob.setWarning(simulatorState.cycleCounts.getWarning());
					}
//					getNextCycleTime();
					try
					{
						cycle = jobDataCache.getCycle(simulatorState.cycleIndex);
					}
					catch (IOException e)
					{
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					syncMap.clear();
					scheduleNextEvent();
				}
				
				if (copilotState.getMachineId() != null)
				{
					Machine machine = machineSimulatorService.getMachine(copilotState.getMachineId());
					machine.setModified(simulatorState.currentJobTime);
					crossCopyDocument(machine);

					machineState = new MachineState();
					machineState.setMachineId(machine.getId());
					machineState.setMachineName(machine.getName());
					machineState.setDeviceKey(copilot.getKey());
					machineState.setJobKey(currentJob == null ? null : currentJob.getKey());
					
//					machineState.getProperties().put("machineKey", String.format("MachineState|%s", machine.getUuid().toString()));
					syncDocument(machineState);
				}
				
				onWritabilityChanged(true);
			}
			else
			{
				System.out.println("We are over the license limit so lets stop now");
				//TODO stop this copilot
			}
		}
	}

	public void onWritabilityChanged(boolean writable)
	{
		//System.out.println(String.format("Writable %b", writable));
		MDC.put("machine", machine.getName());
		synchronized (this.writable)
		{
			this.writable.set(writable);
			if (!connected)
			{
				if (nextTaskScheduled != null)
				{
					nextTaskScheduled.cancel(false);
					nextTaskScheduled = null;
				}
				return;
			}
			if (writable)
			{
				if (nextTask != null && nextTaskScheduled == null)
				{
					log.trace("resuming {} {}", nextTaskTime, nextTask.name);
					taskExecutor.schedule(nextTask, nextTaskTime);
				}
			}
			else
			{
				//TODO handle this in sending?
				/*
				log.info("TCP buffer full, pausing data");
				if (nextTaskScheduled != null)
				{
					if (nextTaskScheduled.cancel(false))
					{
						//task was cancelled before running				
					}
					else
					{
						log.error("Could not cancel task {} {}", nextTaskTime, nextTask.name);
						nextTask = null; //NOTE make sure that task doesn't get started twice
					}
				}
				*/
			}
		}
		MDC.clear();
	}
}
