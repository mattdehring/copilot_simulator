package com.rjginc.copilot.copilot_simulator;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.lang.reflect.ParameterizedType;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.zip.ZipException;

import javax.inject.Inject;
import javax.inject.Named;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.rjginc.esm.core.model.edart.ChangeLog;
import com.rjginc.esm.core.model.job.Cycle;
import com.rjginc.esm.core.model.job.Job;
//import com.rjginc.esm.core.model.job.JobConfigs;
import com.rjginc.esm.core.model.job.JobLegend;
import com.rjginc.esm.core.model.job.JobSummaryLegend;
import com.rjginc.esm.core.model.job.cycle.CycleDataCompressor;
import com.rjginc.esm.core.model.job.cycle.EncodedCycleData;
import com.rjginc.esm.persistance.ArchivalHelper;
import com.rjginc.esm.persistance.IESummaryDataWrapper;
import com.rjginc.esm.persistance.IEWrapper;
//import com.rjginc.leapfrog.domain.LeapEntity;
import com.rjginc.leapfrog.job.domain.Machine;
import com.rjginc.leapfrog.job.domain.Mold;
import com.rjginc.leapfrog.job.domain.Process;
import com.rjginc.leapfrog.job.domain.SetupSheet;

@Component
@Scope("prototype")
public class JobDataCache
{
	@Autowired
	@Named("cborObjectMapper")
	ObjectMapper mapper;

	@Autowired
	@Named("leapCborMapper")
	ObjectMapper leapCborMapper;

	@Autowired
	CycleDataCompressor compressor;

	String path;

	Job job;
	JobLegend jobLegend;

	Map<String, Float[]> summaryData = new HashMap<>();

	Machine machine;
	Mold mold;
	Process process;
	SetupSheet setupSheet;
	List<ChangeLog> changeLogs;

	@Inject
	ArchivalHelper archivalHelper;

	List<String> documents = new ArrayList<>();
	
	//TODO might need to add a caching layer so the zip file can be kept open and be faster

	public void init(String path) throws IOException
	{
		File file = new File(path);
		if (!file.exists())
		{
			return;
		}

		//		archivalHelper.get	
		//		
		//		this.path = path;
		//		raf = new RandomAccessFile(path,"r");
		//		
		//		raf.seek(raf.length()-8);
		//		int headerPos = raf.readInt();
		//		int headerSize = raf.readInt();
		//		raf.seek(headerPos);
		//		byte[] data = new byte[headerSize];
		//		int read = raf.read(data);
		//		header = mapper.readValue(data,   new TypeReference<HashMap<String,byte[]>>() {} );

		job = archivalHelper.getJob(file);
		job.setDatafile(file.getCanonicalPath());
		try
		{
			archivalHelper.setJobContext(job);

			jobLegend = archivalHelper.getJobLegend();
			machine = archivalHelper.getMachine();
			mold = archivalHelper.getMold();
			process = archivalHelper.getProcess();
			setupSheet = archivalHelper.getSetupSheet();
			changeLogs = archivalHelper.getChangeLogs();
			//		jobLegend = getObject( "JobLegend",  JobLegend.class);
			//		jobConfigs = getObject( "JobConfigs",  JobConfigs.class);

			for (Entry<String, JobSummaryLegend> summaryLegend : jobLegend.getSummaryLegends().entrySet())
			{
				//			data = getByteArray(String.format("SummaryData|%s", summaryLegend.getKey()));
				//			ByteBuffer byteBuffer = ByteBuffer.wrap(data);
				//			FloatBuffer floatBuffer = byteBuffer.asFloatBuffer();
				//			float[] summaryData = new float[floatBuffer.remaining()];
				//			floatBuffer.get(summaryData);
				//			this.summaryData.put(summaryLegend.getKey(), summaryData);
				IESummaryDataWrapper ieSummaryDataWrapper = archivalHelper.getSummaryData(summaryLegend.getKey());
				this.summaryData.put(summaryLegend.getKey(), ieSummaryDataWrapper.getData());
			}
			
			documents = archivalHelper.getEntries();
		}
		catch (Exception e)
		{
			System.out.println(e);
		}
		finally
		{
			archivalHelper.setJobContext(null);
		}

	}

	public EncodedCycleData getCycleData(int index) throws IOException
	{
		try
		{
			archivalHelper.setJobContext(job);
			EncodedCycleData encodedCycleData = null;
			//byte[] data = getByteArray(String.format("CycleData|%d", index));
			byte[] data = archivalHelper.getCycleData(index);
			encodedCycleData = compressor.getEncodedCycleData(data);
			return encodedCycleData;
		}
		finally
		{
			archivalHelper.setJobContext(null);
		}
	}

	public byte[] getRawCycleData(int index) throws IOException
	{
		try
		{
			archivalHelper.setJobContext(job);
			return archivalHelper.getCycleData(index);
		}
		finally
		{
			archivalHelper.setJobContext(null);
		}
	}

	public Cycle getCycle(int index) throws IOException
	{
		try
		{
			archivalHelper.setJobContext(job);
			return archivalHelper.getCycle(index);
		}
		finally
		{
			archivalHelper.setJobContext(null);
		}
		//return getObject(String.format("Cycle|%d", index), Cycle.class);
	}

	public Job getJob()
	{
		return job;
	}

	public JobLegend getJobLegend()
	{
		return jobLegend;
	}

	public Map<String, Float[]> getSummaryData()
	{
		return summaryData;
	}

	public Machine getMachine()
	{
		return machine;
	}

	public Mold getMold()
	{
		return mold;
	}

	public Process getProcess()
	{
		return process;
	}

	public SetupSheet getSetupSheet()
	{
		return setupSheet;
	}

	public List<ChangeLog> getChangeLogs()
	{
		return changeLogs;
	}

	public IEWrapper getJobObject(String name) throws ZipException, IOException
	{
		try
		{
			archivalHelper.setJobContext(job);
			return archivalHelper.getJobData(name);
		}
		finally
		{
			archivalHelper.setJobContext(null);
		}
	}

	public List<String> getDocuments()
	{
		return documents;
	}
	

	//	public <T> T getObject( String key, Class<T> t) throws IOException
	//	{
	//		byte[] data = getByteArray(key);
	//		if (data == null)
	//		{
	//			return null;
	//		}
	//		return mapper.readValue(data, t);
	//	}
	//	
	//	public <T> List<T> getList( String key, Class<T> t) throws IOException
	//	{
	//		byte[] data = getByteArray(key);
	//		if (data == null)
	//		{
	//			return null;
	//		}		
	//		
	//		List<T> list = new ArrayList<>();
	//		JsonNode node = mapper.readTree(data);
	//		for (JsonNode child : node)
	//		{
	//			((ObjectNode)child).put("type", "ChangeLog");
	//			list.add( mapper.convertValue(child, t));
	//		}
	//		
	//		return list;
	//	}
	//	
	//	public <T extends LeapEntity> T getLeapObject( String key, Class<T> t) throws IOException
	//	{
	//		byte[] data = getByteArray(key);
	//		if (data == null)
	//		{
	//			return null;
	//		}
	//		return leapCborMapper.readValue(data, t);
	//	}
	//	
	//	public synchronized byte[] getByteArray( String key) throws IOException
	//	{
	//		byte[] arr =  header.get(key);
	//		if (arr == null)
	//		{
	//			return null;
	//		}
	//		ByteBuffer byteBuffer = ByteBuffer.wrap(arr);
	//		int position = byteBuffer.getInt();
	//		int size = byteBuffer.getInt();
	//		raf.seek(position);
	//		byte[] data = new byte[size];
	//		raf.read(data);
	//		return data;
	//	}

	//	public JobConfigs getJobConfigs()
	//	{		
	//		return jobConfigs == null?new JobConfigs() : jobConfigs;
	//	}
}
