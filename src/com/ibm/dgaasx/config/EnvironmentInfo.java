package com.ibm.dgaasx.config;

public class EnvironmentInfo
{
	public static final String getDGaaSURL()
	{
		String dgaasURL = System.getenv( "dgaas.url");
		return dgaasURL == null || dgaasURL.isEmpty() ? "http://giediprime.cluj.ro.ibm.com:9080/dgaas" : dgaasURL; 
	}
	
	public static final String getBaseURL()
	{
		String baseURL = System.getenv( "base.url");
		return baseURL == null || baseURL.isEmpty() ? "http://giediprime.cluj.ro.ibm.com:9080/dgaasx" : baseURL; 
	}
}
