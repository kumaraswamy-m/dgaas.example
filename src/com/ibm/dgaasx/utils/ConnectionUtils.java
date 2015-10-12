/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2015. All Rights Reserved.
 * 
 * Note to U.S. Government Users Restricted Rights:
 * Use, duplication or disclosure restricted by GSA ADP Schedule
 * Contract with IBM Corp. 
 *******************************************************************************/
package com.ibm.dgaasx.utils;

import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.config.ClientConfig;
import com.sun.jersey.api.client.config.DefaultClientConfig;
import com.sun.jersey.client.urlconnection.HTTPSProperties;

public class ConnectionUtils
{
	private static class DummyHostnameVerifier implements HostnameVerifier 
	{
        @Override
        public boolean verify( String s, SSLSession sslSession ) 
        {
            return true;
        }
    }
	private static TrustManager[] certs = new TrustManager[]
	{
		new X509TrustManager()
		{
			@Override
			public X509Certificate[] getAcceptedIssuers()
			{
				return null;
			}
	
			@Override
			public void checkServerTrusted(X509Certificate[] chain, String authType) throws CertificateException
			{
			}
	
			@Override
			public void checkClientTrusted(X509Certificate[] chain, String authType) throws CertificateException
			{
			}
		}
    };
	
	private static final HostnameVerifier hostnameVerifier = new DummyHostnameVerifier();
	
	private static SSLContext getSSLContext() throws GeneralSecurityException
	{
		SSLContext context = null;

		try
		{
			context = SSLContext.getInstance("SSL_TLS"); //$NON-NLS-1$
		}
		catch (NoSuchAlgorithmException e)
		{
		}
		
		// fallback to TLS
		if (context == null)
		{
			context = SSLContext.getInstance("TLS"); //$NON-NLS-1$
		}
		
		return context;
	}
	
	public static Client createClient() 
	{
		ClientConfig config = new DefaultClientConfig();
		
		try
		{
			SSLContext ctx = getSSLContext();
			ctx.init(null, certs, null);
			config.getProperties().put(HTTPSProperties.PROPERTY_HTTPS_PROPERTIES, new HTTPSProperties(hostnameVerifier, ctx));
		}
		catch( GeneralSecurityException e)
		{
			throw new RuntimeException(e);
		}
		
		return Client.create(config);
	}
}
