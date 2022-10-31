package com.rjginc.copilot.copilot_simulator;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Provider;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class JobDataCacheService
{
	@Autowired
	Provider<JobDataCache> jobDataProvider;
		
	Map<String, JobDataCache> jobDataCache = new HashMap<>();
	
	public synchronized JobDataCache getJobDataCache(String jobDataPath) throws IOException
	{
		JobDataCache jobData = jobDataCache.get(jobDataPath);
		
		if(jobData == null)
		{
			jobData = jobDataProvider.get();
			jobDataCache.put(jobDataPath, jobData);
			jobData.init(jobDataPath);			
		}
		
		return jobData;
	}		
}
