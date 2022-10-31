package com.rjginc.esm.core.model.machine;

import java.util.List;
import java.util.Set;

import javax.swing.text.Keymap;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.rjginc.esm.core.model.CouchBaseType;

public class Mold extends CouchBaseType
{
	String path;

	Set<String> processKeys = Sets.newHashSet();

	String uuid = null;

	public String getPath()
	{
		return path;
	}

	public void setPath(String path)
	{
		this.path = path;
	}

	public Set<String> getProcessKeys()
	{
		return processKeys;
	}

	public void setProcessKeys(Set<String> processKeys)
	{
		this.processKeys = processKeys;
	}

	public String getUuid()
	{
		return uuid;
	}

	public void setUuid(String uuid)
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
			return getKey(uuid);
		}
	}

	static public String getKey(String path)
	{
		return String.format("Mold|%s", path);
	}

	static public String stripKey(String key)
	{
		return key.substring(5);
	}

}
