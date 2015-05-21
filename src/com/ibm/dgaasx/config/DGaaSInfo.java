package com.ibm.dgaasx.config;

public class DGaaSInfo
{
	public String url;
	public String instanceid;
	public String region;
	
	public String getURL()
	{
		return url;
	}

	public void setURL(String url)
	{
		this.url = url;
	}

	public String getInstanceID()
	{
		return instanceid;
	}

	public void setInstanceID(String instanceid)
	{
		this.instanceid = instanceid;
	}

	public String getRegion()
	{
		return region;
	}

	public void setRegion(String region)
	{
		this.region = region;
	}

	public DGaaSInfo()
	{
		
	}
	
	public DGaaSInfo(String url, String instanceid, String region)
	{
		this.url = url;
		this.instanceid = instanceid;
		this.region = region;
	}
}