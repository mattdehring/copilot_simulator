package com.rjginc.copilot.copilot_simulator;

import java.io.File;
import java.io.IOException;
import java.util.Set;

import org.reflections.Reflections;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Scope;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.InjectableValues;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.dataformat.cbor.CBORFactory;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.google.common.collect.RangeSet;
import com.google.common.eventbus.EventBus;
import com.rjginc.esm.core.model.CouchBaseType;
import com.rjginc.esm.persistance.ArchivalHelper;
import com.rjginc.esm.persistance.JsonConverter;
import com.rjginc.esm.persistance.RangeSetDeserializer;
import com.rjginc.esm.persistance.RangeSetSerializer;
import com.rjginc.interop.messages.HubCopilotMessageCodec;
import com.rjginc.interop.messages.InteropMessage;
import com.rjginc.interop.messages.JsonMapperSetup;
import com.rjginc.leapfrog.domain.LeapDoc;
import com.rjginc.leapfrog.domain.LeapEntity;
import com.rjginc.leapfrog.domain.LeapRestMessage;
//@ComponentScan({"com.rjginc.esm.core","com.rjginc.interop","com.rjginc.leapfrog"})

@Configuration
@EnableScheduling
@EnableAsync
@ComponentScan(basePackages= {"com.rjginc.copilot.copilot_simulator", "com.rjginc.config","com.rjginc.esm.core","com.rjginc.interop" }, excludeFilters={
		 @ComponentScan.Filter(type=FilterType.REGEX,pattern="com.rjginc.esm.core.services")})
public class CopilotSimulatorConfig //implements AsyncConfigurer
{
	@Bean
	public HubAddress hubAddress()
	{
		Settings settings = settings();
		return new HubAddress(settings.getHubAddress(), settings.getHubPort());
	}

	@Bean
	public EventBus eventBus()
	{
		return new EventBus();
	}

	@Bean
	public TaskScheduler taskScheduler()
	{
		ThreadPoolTaskScheduler tpts = new ThreadPoolTaskScheduler();
		tpts.setPoolSize(10);
		tpts.setRemoveOnCancelPolicy(true); //remove any queued tasks at time of cancellation
		return tpts;
	}
	
	@Bean(name = "leapObjectMapper")
	public ObjectMapper objectMapper()
	{
		ObjectMapper mapper = new ObjectMapper();
		JsonMapperSetup.setupLeapMapper(mapper);

		return mapper;
	}
	
	@Bean(name = "leapCborMapper")
	public ObjectMapper leapCborMapper()
	{
		ObjectMapper mapper = new ObjectMapper(new CBORFactory());
		JsonMapperSetup.setupLeapMapper(mapper);
		return mapper;
	}

	@Bean(name = "cborObjectMapper")
	public ObjectMapper cborObjectMapper()
	{
		ObjectMapper mapper = new ObjectMapper(new CBORFactory());
		JsonMapperSetup.setupLeapMapper(mapper);
		mapper.findAndRegisterModules();						
		return mapper;
	}

	@Primary
	@Bean(name = "couchObjectMapper")
	public ObjectMapper couchObjectMapper()
	{
		ObjectMapper mapper = new ObjectMapper();
		JsonMapperSetup.setupLeapMapper(mapper);
		mapper.findAndRegisterModules();//
		return mapper;
	}
	
	@Bean()
	@Scope("prototype")
	public HubCopilotMessageCodec hubCopilotMessageCodec()
	{
		return new HubCopilotMessageCodec();
	}
	
	@Bean()
	public JsonConverter jsonConverter()
	{
		return new JsonConverter();
	}
	
	@Bean()
	public ArchivalHelper archivalHelper()
	{
		return new ArchivalHelper();
	}
	
	@Bean 
	public Settings settings()
	{
		
		Settings settings = null;
		try
		{
			settings = couchObjectMapper().readValue(new File("config.json"), Settings.class);
		}
		catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
			settings = new Settings();
		}		
		return  settings;
	}
}
