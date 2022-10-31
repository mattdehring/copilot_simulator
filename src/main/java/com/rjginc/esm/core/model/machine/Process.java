package com.rjginc.esm.core.model.machine;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.google.common.collect.Sets;
import com.rjginc.esm.core.model.CouchBaseType;

public class Process extends CouchBaseType
{
	String path;
	String moldKey;
	String uuid = null;

	Set<String> templates = Sets.newHashSet();

	public Process()
	{
		super();
	}

	public Process(String path)
	{
		super();
		//this.path = path;
		setPath(path);
	}

	public String getPath()
	{
		return path;
	}

	public void setPath(String path)
	{
		this.path = path;
		moldKey = Mold.getKey(getMold());
	}

	public String getMoldKey()
	{
		return moldKey;
	}

	public void setMoldKey(String moldKey)
	{
		this.moldKey = moldKey;
	}

	public String getUuid()
	{
		return uuid;
	}

	public void setUuid(String uuid)
	{
		this.uuid = uuid;
	}

	public Set<String> getTemplates()
	{
		return templates;
	}

	public void setTemplates(Set<String> templates)
	{
		this.templates = templates;
	}

	@JsonIgnore
	public String getName()
	{
		int index = path.indexOf('/');
		if (index == -1)
		{
			return "";
		}
		else
		{
			return path.substring(index + 1);
		}
	}

	@JsonIgnore
	public String getMold()
	{
		int index = path.indexOf('/');
		if (index == -1)
		{
			return path;
		}
		else
		{
			return path.substring(0, index);
		}
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
		return String.format("Process|%s", path);
	}

	static public String stripKey(String key)
	{
		return key.substring(8);
	}
}