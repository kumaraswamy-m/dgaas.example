package com.ibm.dgaasx.config;

public class EnvironmentInfo
{
	private static DGaaSInfo DGAAS_INFO = parseVCAPS();
	
	public static final DGaaSInfo getDGaaSInfo()
	{
		return DGAAS_INFO;
	}
	
	public static final String getBaseURL()
	{
		String baseURL = System.getenv( "base.url");
		return baseURL == null || baseURL.isEmpty() ? "http://localhost:8080/dgaasx" : baseURL; 
	}
	
	
	public static final DGaaSInfo parseVCAPS()
	{
		DGaaSInfo info = new DGaaSInfo();
		
		String vcaps = System.getenv("VCAP_SERVICES");
		if ( vcaps == null || vcaps.trim().isEmpty())
		{
			String dgaasURL = System.getenv( "dgaas.url");
			info.setURL( dgaasURL == null || dgaasURL.isEmpty() ? "http://localhost:8080/dgaas" : dgaasURL);
			return info;
		}
		
		
		return info;
	}
}
