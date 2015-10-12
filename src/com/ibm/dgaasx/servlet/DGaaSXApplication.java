/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2015. All Rights Reserved.
 * 
 * Note to U.S. Government Users Restricted Rights:
 * Use, duplication or disclosure restricted by GSA ADP Schedule
 * Contract with IBM Corp. 
 *******************************************************************************/
package com.ibm.dgaasx.servlet;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.Application;

import com.ibm.dgaasx.servlet.docgen.JobProxyService;
import com.ibm.dgaasx.servlet.docgen.RSS2PDFService;
import com.ibm.dgaasx.servlet.docgen.ResultProxyService;
import com.ibm.dgaasx.servlet.template.RSS2TemplateService;
import com.ibm.dgaasx.utils.SystemUtils;

@SuppressWarnings("nls")
// @ApplicationPath("/*")
public class DGaaSXApplication extends Application
{
	private static final String SWAGGER_BASE_PATH = "SWAGGER_BASE_PATH"; //$NON-NLS-1$

	public DGaaSXApplication()
	{
	}

	@Override
	public Set<Class<?>> getClasses()
	{
		Set<Class<?>> resources = new HashSet<Class<?>>();

		resources.add(RSS2PDFService.class);
		resources.add(JobProxyService.class);
		resources.add(ResultProxyService.class);

		resources.add(RSS2TemplateService.class);

		return resources;
	}

}