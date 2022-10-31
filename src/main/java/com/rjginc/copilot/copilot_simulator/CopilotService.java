package com.rjginc.copilot.copilot_simulator;

import java.io.File;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.inject.Provider;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Stopwatch;
import com.google.common.net.InetAddresses;
import com.rjginc.esm.core.model.edart.Copilot;
import com.rjginc.esm.core.model.edart.CopilotState;

@Component
@Profile("simulator")
public class CopilotService
{
	@Autowired
	ApplicationContext context;

	@Autowired
	EntityConfigurationService machineService;

	@Autowired
	Provider<SimulatedCopilot> simulatedCopilotpProvider;

	@Autowired
	ObjectMapper objectMapper;

	@Autowired
	Settings settings;

	int count = 0;
	Map<Copilot, CopilotState> lookup = new HashMap<>();

	int runningCopilots = 0;
	
	Stopwatch sw;
	
	String token = "Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJzdWIiOiJnb29kLmdvb2QiLCJmaXJzdG5hbWUiOiJEdWRlIiwicGVybWlzc2lvbnMiOlsiUk9MRV9TVVBFUlVTRVIiXSwiaWF0IjoxNTI4ODA5NTk2LCJjcmVhdGVkIjoxNTI4ODA5NTk2MzI1LCJyb2xlcyI6e30sImlzcyI6IlRoZSBIdWIiLCJleHAiOjE4NDQzODU1OTYsImxhc3RuYW1lIjoiTWFuIn0.hVLmLacPLbeQSZA6dsysJecIGdC1lgHVy4EtC1t5v_k";

	@PostConstruct
	public void init()
	{
		sw = Stopwatch.createStarted();
		for (int i = 0; i < settings.copilotCount; i++)
		{
			simulatedCopilotpProvider.get();
			runningCopilots++;
		}

		System.out.println("start " + runningCopilots + " copilots");
	}

	public synchronized Copilot getCopilot()
	{
		Copilot copilot = new Copilot();
		try
		{
			copilot.setIpAddress(Inet4Address.getByName("127.0.0.1"));
			copilot.setMac(intToMac(count));
			copilot.setNetmask(InetAddresses.fromInteger(0xffffff00));
			copilot.setSerialNumber(String.format("19COP%05x", count));

			count++;
		}
		catch (UnknownHostException e)
		{

		}
		return copilot;
	}

	public synchronized CopilotState getCopilotState(Copilot copilot)
	{
		CopilotState copilotState = null;
		try
		{
			copilotState = lookup.get(copilot);
			if (copilotState == null)
			{
				copilotState = new CopilotState(copilot.getKey());
			}
		}
		catch (Exception e)
		{
			System.out.println(e);
		}

		return copilotState;
	}

	private String intToMac(int address)
	{
		byte[] bytes = new byte[6];
		ByteBuffer bb = ByteBuffer.wrap(bytes);
		bb.putInt(address);
		return Copilot.convertMacToString(bytes);
	}

	public synchronized void CopilotComplete()
	{
		runningCopilots--;
		if (runningCopilots <= 0)
		{
			sw.stop();
			System.out.println("Elapsed time = " + sw.elapsed(TimeUnit.SECONDS)/60.0);
			//System.exit(0);
			((ConfigurableApplicationContext) context).close();
		}
	}
	
	@Scheduled(fixedDelay = 60000)
	public void timer() throws Exception, IOException
	{
		HttpClient httpClient = HttpClientBuilder.create().build();
		HttpGet getRequest = new HttpGet(String.format("http://%s:%d/rest/v1/jobs",settings.getHubAddress(),80));
		getRequest.addHeader("accept", "application/json");
		getRequest.addHeader("Authorization", token);

		Stopwatch sw = Stopwatch.createStarted();
		HttpResponse response = httpClient.execute(getRequest);
		sw.stop();
		System.out.printf("Jobs took %d ms\n", sw.elapsed(TimeUnit.MILLISECONDS));
		
		if (response.getStatusLine().getStatusCode() != 200) {
			throw new RuntimeException("Failed : HTTP error code : "
			   + response.getStatusLine().getStatusCode());
		}

	}
}
