/*******************************************************************************
 * Licensed Materials - Property of IBM
 * © Copyright IBM Corporation 2015. All Rights Reserved.
 * 
 * Note to U.S. Government Users Restricted Rights:
 * Use, duplication or disclosure restricted by GSA ADP Schedule
 * Contract with IBM Corp. 
 *******************************************************************************/
package com.ibm.dgaasx.servlet;

import java.io.IOException;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

public class CORSFilter implements Filter
{
	private static final String ACCESS_CONTROL_ALLOW_METHODS = "Access-Control-Allow-Methods"; //$NON-NLS-1$
	private static final String ALL_METHODS = "GET,POST,DELETE,PUT,OPTIOS,HEAD"; //$NON-NLS-1$
	private static final String ACCESS_CONTROL_ALLOW_ORIGIN = "Access-Control-Allow-Origin"; //$NON-NLS-1$
	private static final String ACCESS_CONTROL_ALLOW_CREDENTIALS = "Access-Control-Allow-Credentials"; //$NON-NLS-1$
	private static final String ACCESS_CONTROL_ALLOW_HEADERS = "Access-Control-Allow-Headers"; //$NON-NLS-1$
	private static final String ALL_DOMAINS = "*"; //$NON-NLS-1$

	private static final String CORS_DOMAINS = "CORS_DOMAINS"; //$NON-NLS-1$

	@Override
	public void destroy()
	{
	}

	@Override
	public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain) throws IOException,
			ServletException
	{
		if (res instanceof HttpServletResponse)
		{
			String domains = System.getenv(CORS_DOMAINS);
			if (domains == null || domains.isEmpty())
			{
				domains = ALL_DOMAINS;
			}

			HttpServletResponse response = (HttpServletResponse) res;
			response.setHeader(ACCESS_CONTROL_ALLOW_ORIGIN, domains);
			response.setHeader(ACCESS_CONTROL_ALLOW_METHODS, ALL_METHODS);
			response.setHeader(ACCESS_CONTROL_ALLOW_CREDENTIALS, "true"); //$NON-NLS-1$
			response.setHeader(ACCESS_CONTROL_ALLOW_HEADERS, "Origin,X-Requested-With,Content-Type,Accept,secret"); //$NON-NLS-1$
		}

		chain.doFilter(req, res);
	}

	@Override
	public void init(FilterConfig arg0) throws ServletException
	{
		// new DGaaSXApplication();
	}
}
