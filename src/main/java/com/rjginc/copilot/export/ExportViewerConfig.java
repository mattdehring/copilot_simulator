package com.rjginc.copilot.export;

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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.cbor.CBORFactory;
import com.google.common.eventbus.EventBus;
import com.rjginc.copilot.copilot_simulator.CopilotService;
import com.rjginc.copilot.copilot_simulator.HubAddress;
import com.rjginc.copilot.copilot_simulator.JobDataCache;
import com.rjginc.copilot.copilot_simulator.JobDataCacheService;
import com.rjginc.interop.messages.HubCopilotMessageCodec;
import com.rjginc.interop.messages.JsonMapperSetup;

@Configuration
@EnableScheduling
@EnableAsync
@ComponentScan(basePackages= {"com.rjginc.config","com.rjginc.esm.core","com.rjginc.interop" ,"com.rjginc.esm.persistance" } , excludeFilters={
		  @ComponentScan.Filter(type=FilterType.REGEX,pattern="com.rjginc.esm.core.services") ,@ComponentScan.Filter(type=FilterType.ASSIGNABLE_TYPE, value=CopilotService.class) })
public class ExportViewerConfig
{	
	@Bean
	public HubAddress hubAddress()
	{
		return new HubAddress("172.16.0.237", 55333);
	}
	
	@Bean
	public JobDataCacheService jobDataCacheService()
	{
		return new JobDataCacheService();
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
	
	@Bean(name={"cborObjectMapper","leapCborMapper"})
	public ObjectMapper cborObjectMapper()
	{
		ObjectMapper mapper = new ObjectMapper(new CBORFactory());
		JsonMapperSetup.setupLeapMapper(mapper);
        return mapper;
	}
	
    @Primary
    @Bean(name={"couchObjectMapper","leapObjectMapper"})
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        JsonMapperSetup.setupLeapMapper(mapper);
        return mapper;
	}
    
    @Bean()
	@Scope("prototype")
	public JobDataCache jobDataCache()
	{
		return new JobDataCache();
	}
	
	@Bean()
	@Scope("prototype")
	public HubCopilotMessageCodec hubCopilotMessageCodec()
	{
		return new HubCopilotMessageCodec();
	}	
}
