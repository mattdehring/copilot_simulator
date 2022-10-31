package com.rjginc.copilot.copilot_simulator;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rjginc.esm.core.model.job.Job;
import com.rjginc.esm.core.model.job.JobLegend;

/**
 * Hello world!
 *
 */
public class CopilotSimulator 
{
	ObjectMapper mapper;
	
//	public <T> T getObject(RandomAccessFile raf,Map<String, byte[]> header, String key, Class<T> t) throws IOException
//	{
//		byte[] arr =  header.get(key);
//		ByteBuffer byteBuffer = ByteBuffer.wrap(arr);
//		int position = byteBuffer.getInt();
//		int size = byteBuffer.getInt();
//		raf.seek(position);
//		byte[] data = new byte[size];
//		raf.read(data);
//		return mapper.readValue(data, t);
//	}
	
	public CopilotSimulator()
	{
		System.setProperty("spring.profiles.active", "simulator");		
		ApplicationContext ctx = new AnnotationConfigApplicationContext(CopilotSimulatorConfig.class);	
				
	}
	
	
    public static void main( String[] args )
    {
        new CopilotSimulator();
    }
}
