package com.rjginc.copilot.copilot_simulator;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rjginc.leapfrog.job.domain.Machine;
import com.rjginc.leapfrog.job.domain.Mold;
import com.rjginc.leapfrog.job.domain.Process;
import com.rjginc.leapfrog.job.domain.SetupSheet;

//import com.rjginc.esm.core.model.machine.Machine;
//import com.rjginc.esm.core.model.machine.Mold;
//import com.rjginc.esm.core.model.machine.Process;

@Component
public class EntityConfigurationService
{
	private static final Logger log = LoggerFactory.getLogger(EntityConfigurationService.class);
	
	
	int machineCount = 0;
	Random machineRand = new Random(100);
	Random moldRand = new Random(101);
	Random processRand = new Random(102);
	Random rand = new Random(103);
	
	Map<UUID, Machine> machines = new HashMap<>();
	Map<UUID, Mold> molds = new HashMap<>();
	LinkedList<Mold> freeMolds = new LinkedList<>();
	Map<UUID,LinkedList<Process>> freeProcesses = new HashMap<>();
	Map<UUID, Process> processes = new HashMap<>();
	
	List<String> dataSets =new ArrayList<>();
	
	List<String> machineNames = new ArrayList<>();
	Set<String> moldNames = new HashSet<>();
	Set<String> processNames = new HashSet<>();
	
	Map<String, AtomicInteger> machineCountLookup = new HashMap<>();
	Map<String, AtomicInteger> moldCountLookup = new HashMap<>();
	Map<String, AtomicInteger> processCountLookup = new HashMap<>();
	
	Map<String,Machine> machineTemplates = new HashMap<>();
	Map<String,Mold> moldTemplates = new HashMap<>();
	Map<String,Process> processTemplates = new HashMap<>();
	Map<String,SetupSheet> setupSheetTemplates = new HashMap<>();
	
	Map<SetupSheetHash,SetupSheet> setupSheets = new HashMap<>();
	
	@Inject
	JobDataCacheService jobDataCacheService;
	
	@Inject
	ObjectMapper objectMapper;
	
	@PostConstruct
	public void init()
	{
		File dir = new File("data");
		for ( String path : dir.list())
		{
			if (path.endsWith(".zip"))
			{
				String p = "data/"+ path;
				dataSets.add(p);
				try
				{
					JobDataCache jobDataCache = jobDataCacheService.getJobDataCache(p);
					machineNames.add(jobDataCache.getJob().getMachineName());
					Machine machineTemplate = jobDataCache.getMachine();
					machineTemplates.put(jobDataCache.getJob().getMachineName(), machineTemplate);
					Mold moldTemplate = jobDataCache.getMold();
					moldTemplates.put(jobDataCache.getJob().getMoldName(), moldTemplate);
					Process processTemplate = jobDataCache.getProcess();
					processTemplates.put(jobDataCache.getJob().getProcessName(), processTemplate);
					SetupSheet setupSheetTemplate = jobDataCache.getSetupSheet();
					setupSheetTemplates.put( String.format("%s-%s", jobDataCache.getJob().getMachineName(), jobDataCache.getJob().getProcessName()) , setupSheetTemplate);					
				}
				catch (IOException e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}			
	}
		
	public synchronized Machine getMachine()
	{
		
		//Machine machine = new Machine();
			
		String name = machineNames.get( machineCount%machineNames.size());
		Machine machineTemplate = machineTemplates.get(name);
		Machine machine = objectMapper.convertValue(machineTemplate, Machine.class);		
		
		AtomicInteger count = machineCountLookup.get(name);
		if (count == null)
		{
			count = new AtomicInteger(0);
			machineCountLookup.put(name, count);
		}
		
		machine.setName(String.format("%s %03d", name, count.incrementAndGet()));
		long low = machineRand.nextLong();
		long high = machineRand.nextLong();		
		machine.setId( UUID.fromString( String.format("%08x-%04x-%04x-%04x-%012x",(high>>> 32) & 0xffffffff, (high>>> 16) & 0xffff,high & 0xffff , (low >>> 48) & 0xffffl, low & 0xffffffffffffl )));
		machine.setRev(UUID.randomUUID());
		machine.setCurrent(true);
		machineCount++;
		
		log.debug("machine {}-{}",machine.getId(), machine.getName());
		machines.put(machine.getId(), machine);
		
		return machine;
	}
	
	public Machine getMachine(UUID key)
	{
		return machines.get(key);
	}
	
	public synchronized Mold getMold()
	{
		Mold mold;
		if (freeMolds.isEmpty() || rand.nextDouble() * freeMolds.size() < .50 )
		{		
			String dataSet = dataSets.get( molds.size() % dataSets.size());
			JobDataCache jobDataCache;
			mold = new Mold();
			try
			{
				jobDataCache = jobDataCacheService.getJobDataCache(dataSet);
				Mold moldTemplate = jobDataCache.getMold();
				mold = objectMapper.convertValue(moldTemplate, Mold.class);
				String name = jobDataCache.getJob().getMoldName();
				AtomicInteger count = moldCountLookup.get(name);
				if (count == null)
				{
					count = new AtomicInteger(0);
					moldCountLookup.put(name, count);
				}
				
				mold.setName(String.format("%s %03d",name,count.incrementAndGet()));
				long low = moldRand.nextLong();
				long high = moldRand.nextLong();
				mold.setId( UUID.fromString(String.format("%08x-%04x-%04x-%04x-%012x",(high>>> 32) & 0xffffffff, (high>>> 16) & 0xffff,high & 0xffff , (low >>> 48) & 0xffffl, low & 0xffffffffffffl )));
				mold.setRev(UUID.randomUUID());
				mold.setCurrent(true);
				mold.setProperty("dataset", dataSet);
				
				log.debug("mold {}-{}",mold.getId(), mold.getName());
				molds.put(mold.getId(), mold);
			}
			catch (IOException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		else
		{
			mold = freeMolds.removeFirst();
		}				
		return mold;
	}
	
	public synchronized void releaseMold(UUID moldId)
	{
		freeMolds.push(molds.get(moldId));
	}
	
	public synchronized void releaseMold(Mold mold)
	{
		freeMolds.push(mold);
	}
	
	public Mold getMold(UUID key)
	{
		return molds.get(key);
	}
	
	public synchronized Process getProcess( Mold mold)
	{
		LinkedList<Process> linkedList = freeProcesses.get(mold.getId());
		Process process;
		if (linkedList == null || linkedList.isEmpty() || rand.nextDouble() * linkedList.size() < .50 )
		{
			process = new Process();
			String dataSet = (String) mold.getProperties().get("dataset");
			JobDataCache jobDataCache;
			try
			{
				jobDataCache = jobDataCacheService.getJobDataCache(dataSet);
				Process processTemplate = jobDataCache.getProcess();
				process = objectMapper.convertValue(processTemplate, Process.class);
				String name = mold.getName() + "/" + jobDataCache.getJob().getProcessName();
				AtomicInteger count = processCountLookup.get(name);
				if (count == null)
				{
					count = new AtomicInteger(0);
					processCountLookup.put(name, count);
				}
				process.setName(String.format("%s %03d", processTemplate.getName(), count.incrementAndGet()));
				process.setMoldId(mold.getId());
				process.setMoldName(mold.getName());				
				long low = processRand.nextLong();
				long high = processRand.nextLong();
				process.setId(UUID.fromString(String.format("%08x-%04x-%04x-%04x-%012x", (high >>> 32) & 0xffffffff, (high >>> 16) & 0xffff, high & 0xffff, (low >>> 48) & 0xffffl, low & 0xffffffffffffl)));
				process.setRev(UUID.randomUUID());
				process.setCurrent(true);
				process.setProperty("dataset", dataSet);
				
				log.debug("process {}-{}",process.getId(), process.getName());				
				processes.put(process.getId(), process);
			}
			catch (IOException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		else
		{
			process = linkedList.removeFirst();
		}
				
		return process;
	}
	
	public Process getProcess(UUID key)
	{
		return processes.get(key);
	}
	
	public synchronized void freeProcess(Process process)
	{
		try
		{
		System.out.printf("%s %s\n" ,process.getMoldId(),freeProcesses);
		LinkedList<Process> linkedList = freeProcesses.get( process.getMoldId() );
		if (linkedList == null)
		{
			linkedList = new LinkedList<>();
			freeProcesses.put(process.getMoldId(), linkedList);
		}
		linkedList.add(process);
		}catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	static class SetupSheetHash
	{
		UUID processId;
		UUID machineId;
		
		public SetupSheetHash(UUID processId, UUID machineId)
		{
			super();
			this.processId = processId;
			this.machineId = machineId;
		}

		public UUID getProcessId()
		{
			return processId;
		}

		public UUID getMachineId()
		{
			return machineId;
		}

		@Override
		public int hashCode()
		{
			final int prime = 31;
			int result = 1;
			result = prime * result + ((machineId == null) ? 0 : machineId.hashCode());
			result = prime * result + ((processId == null) ? 0 : processId.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj)
		{
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			SetupSheetHash other = (SetupSheetHash) obj;
			if (machineId == null)
			{
				if (other.machineId != null)
					return false;
			}
			else if (!machineId.equals(other.machineId))
				return false;
			if (processId == null)
			{
				if (other.processId != null)
					return false;
			}
			else if (!processId.equals(other.processId))
				return false;
			return true;
		}
	}
	
	public synchronized SetupSheet getSetupSheet( Process process, Machine machine)
	{
		SetupSheetHash setupSheetHash = new SetupSheetHash(process.getId(), machine.getId());
		SetupSheet setupSheet = setupSheets.get(setupSheetHash);
		
		if(setupSheet == null)
		{
			String dataSet = (String)process.getProperties().get("dataset");
			JobDataCache jobDataCache;
			try
			{
				jobDataCache = jobDataCacheService.getJobDataCache(dataSet);
				SetupSheet setupSheetTemplate = jobDataCache.getSetupSheet();
				if(setupSheetTemplate == null)
				{
					//TODO implement setupsheet transfer from process to get existing alarms and such
					setupSheet = new SetupSheet();
				}
				else
				{
					setupSheet = objectMapper.convertValue(setupSheetTemplate, SetupSheet.class);
				}
				setupSheet.setMachineId(machine.getId());
				setupSheet.setMachineName(machine.getName());
				setupSheet.setMoldId(process.getMoldId());
				setupSheet.setMoldName(process.getMoldName());
				setupSheet.setProcessId(process.getId());
				setupSheet.setProcessName(process.getName());
				setupSheet.setBarrelAssembly(machine.getInjectionUnit().getCurrentBarrel());
				
				long low = process.getId().getLeastSignificantBits();
				long high = machine.getId().getLeastSignificantBits();
				setupSheet.setId(UUID.fromString(String.format("%08x-%04x-%04x-%04x-%012x", (high >>> 32) & 0xffffffff, (high >>> 16) & 0xffff, high & 0xffff, (low >>> 48) & 0xffffl, low & 0xffffffffffffl)));
				setupSheet.setRev(UUID.randomUUID());
				setupSheet.setCurrent(true);
				log.debug("setupsheet {}",setupSheet.getId());
				setupSheets.put(setupSheetHash, setupSheet);
			}
			catch (IOException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		
		return setupSheet;
		
//		LinkedList<Process> linkedList = freeProcesses.get(mold.getId());
//		Process process;
//		if (linkedList == null || linkedList.isEmpty() || rand.nextDouble() * linkedList.size() < .50 )
//		{
//			process = new Process();
//			String dataSet = (String) mold.getProperties().get("dataset");
//			JobDataCache jobDataCache;
//			try
//			{
//				jobDataCache = jobDataCacheService.getJobDataCache(dataSet);
//				Process processTemplate = jobDataCache.getProcess();
//				process = objectMapper.convertValue(processTemplate, Process.class);
//				String name = mold.getName() + "/" + jobDataCache.getJob().getProcessName();
//				AtomicInteger count = processCountLookup.get(name);
//				if (count == null)
//				{
//					count = new AtomicInteger(0);
//					processCountLookup.put(name, count);
//				}
//				process.setName(String.format("%s %03d", processTemplate.getName(), count.incrementAndGet()));
//				process.setMoldId(mold.getId());
//				process.setMoldName(mold.getName());				
//				long low = processRand.nextLong();
//				long high = processRand.nextLong();
//				process.setId(UUID.fromString(String.format("%08x-%04x-%04x-%04x-%012x", (high >>> 32) & 0xffffffff, (high >>> 16) & 0xffff, high & 0xffff, (low >>> 48) & 0xffffl, low & 0xffffffffffffl)));
//				process.setRev(process.getId());
//				processes.put(process.getId(), process);
//			}
//			catch (IOException e)
//			{
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//		}
//		else
//		{
//			process = linkedList.removeFirst();
//		}
//				
//		return process;
	}
	
//	public Process getSetupSheet(UUID key)
//	{
//		return processes.get(key);
//	}
	
//	public synchronized void freeSetupSheet(SetupSheet setupSheet)
//	{
//		try
//		{
//		System.out.printf("%s %s\n" ,process.getMoldId(),freeProcesses);
//		LinkedList<Process> linkedList = freeProcesses.get( process.getMoldId() );
//		if (linkedList == null)
//		{
//			linkedList = new LinkedList<>();
//			freeProcesses.put(process.getMoldId(), linkedList);
//		}
//		linkedList.add(process);
//		}catch(Exception e) {
//			e.printStackTrace();
//		}
//	}
}
