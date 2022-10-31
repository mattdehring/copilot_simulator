package com.rjginc.copilot.copilot_simulator;

import java.time.Duration;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonFormat;

public class Settings
{
	String hubAddress = "localhost";
	int hubPort = 55333;
	int copilotCount = 1;
	int cycleCount = 100000;
	boolean keepRunning = false;
	boolean randomCycleCount = false;
	boolean useJobCount = false;
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
	Date startDate = null;
	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd")
	Date endDate = null;
	Duration startTime = null;
	Duration endTime = null;
	Duration jobTime = null;
	Duration jobStopTime = Duration.parse("PT2M");

	Map<String, Object> properties = new HashMap<String, Object>();

	public String getHubAddress()
	{
		return hubAddress;
	}

	public void setHubAddress(String hubAddress)
	{
		this.hubAddress = hubAddress;
	}

	public int getHubPort()
	{
		return hubPort;
	}

	public void setHubPort(int hubPort)
	{
		this.hubPort = hubPort;
	}

	public int getCopilotCount()
	{
		return copilotCount;
	}

	public void setCopilotCount(int copilotCount)
	{
		this.copilotCount = copilotCount;
	}

	public int getCycleCount()
	{
		return cycleCount;
	}

	public void setCycleCount(int cycleCount)
	{
		this.cycleCount = cycleCount;
	}

	public boolean isKeepRunning()
	{
		return keepRunning;
	}

	public void setKeepRunning(boolean keepRunning)
	{
		this.keepRunning = keepRunning;
	}

	public boolean isRandomCycleCount()
	{
		return randomCycleCount;
	}

	public void setRandomCycleCount(boolean randomCycleCount)
	{
		this.randomCycleCount = randomCycleCount;
	}

	public boolean isUseJobCount()
	{
		return useJobCount;
	}

	public void setUseJobCount(boolean useJobCount)
	{
		this.useJobCount = useJobCount;
	}

	public Date getStartDate()
	{
		return startDate;
	}

	public void setStartDate(Date startDate)
	{
		this.startDate = startDate;
	}

	public Date getEndDate()
	{
		return endDate;
	}

	public void setEndDate(Date endDate)
	{
		this.endDate = endDate;
	}

	public Duration getStartTime()
	{
		return startTime;
	}

	public void setStartTime(Duration startTime)
	{
		this.startTime = startTime;
	}

	public Duration getEndTime()
	{
		return endTime;
	}

	public void setEndTime(Duration endTime)
	{
		this.endTime = endTime;
	}

	public Duration getJobTime()
	{
		return jobTime;
	}

	public void setJobTime(Duration jobTime)
	{
		this.jobTime = jobTime;
	}

	public Duration getJobStopTime()
	{
		return jobStopTime;
	}

	public void setJobStopTime(Duration jobStopTime)
	{
		this.jobStopTime = jobStopTime;
	}

	@JsonAnySetter
	public void setProperty(String key, Object value)
	{
		properties.put(key, value);
	}

	@JsonAnyGetter
	public Map<String, Object> getProperties()
	{
		return properties;
	}

}
