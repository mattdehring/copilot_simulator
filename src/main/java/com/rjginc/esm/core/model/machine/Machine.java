package com.rjginc.esm.core.model.machine;

import java.util.UUID;

import com.rjginc.esm.core.model.CouchBaseType;

public class Machine extends CouchBaseType
{
	String path;
	//	String currentJobId;

	UUID uuid = null;

	public String getPath()
	{
		return path;
	}

	public void setPath(String path)
	{
		this.path = path;
	}

	public UUID getUuid()
	{
		return uuid;
	}

	public void setUuid(UUID uuid)
	{
		this.uuid = uuid;
	}

	@Override
	public String getKey()
	{
		//Note this is to support copilots with unique generated id  
		if (uuid == null)
		{
			return getKey(path);
		}
		else //Note this is to support edarts that the path is the identifier 
		{
			return uuid.toString();
		}
	}

	static public String getKey(String path)
	{
		return String.format("Machine|%s", path);
	}

	static public String stripKey(String key)
	{
		return key.substring(8);
	}
}
