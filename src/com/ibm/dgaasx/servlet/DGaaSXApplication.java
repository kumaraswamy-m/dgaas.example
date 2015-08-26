/*******************************************************************************
 * Licensed Materials - Property of IBM
 * © Copyright IBM Corporation 2015. All Rights Reserved.
 * 
 * Note to U.S. Government Users Restricted Rights:
 * Use, duplication or disclosure restricted by GSA ADP Schedule
 * Contract with IBM Corp. 
 *******************************************************************************/
package com.ibm.dgaasx.servlet;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

import com.ibm.dgaasx.servlet.docgen.JobProxyService;
import com.ibm.dgaasx.servlet.docgen.RSS2PDFService;
import com.ibm.dgaasx.servlet.docgen.ResultProxyService;
import com.ibm.dgaasx.servlet.template.RSS2TemplateService;
import com.ibm.dgaasx.utils.SystemUtils;
import com.wordnik.swagger.jaxrs.config.BeanConfig;

@SuppressWarnings("nls")
@ApplicationPath("/api")
public class DGaaSXApplication extends Application
{
	private static final String SWAGGER_BASE_PATH = "SWAGGER_BASE_PATH"; //$NON-NLS-1$

	public DGaaSXApplication()
	{
		BeanConfig beanConfig = new BeanConfig();
		beanConfig.setTitle("Example for Document Generation as a Service"); //$NON-NLS-1$
		beanConfig.setDescription("This API is an example of using DGaaS (Document Generation as a Service) and produces PDF documents from RSS 2.0 feeds." + "<br/><br/>The procedure is:" + "<ol>" + "<li>Invoke /dgasaax/api/rss2pdf with the RSS feed you want to convert to PDF. Optionally provide a secret token to secure your operation.</li>" + "<li>Monitor the process at /dgasaax/api/job/<jobid> using the jobid returned by the first call. Pass the same secret token you used in #1.</li>" + "<li>Once the job completes succesfully ( the status of the job becomes finished) access the PDF at /dgasaax/api/result/<resulturi>. Pass the same secret token you used in #1.</li>" + "</ol>" + "<br/>The source code is hosted on Github at <a href='https://github.com/dgaas/dgaas.example'>https://github.com/dgaas/dgaas.example</a>. For questions and support please see the Contact the Developer link." + "<br/>");
		beanConfig.setContact("dragos.cojocari@ro.ibm.com");
		beanConfig.setVersion("1.0.0");
		beanConfig.setBasePath(SystemUtils.getSystemProperty(SWAGGER_BASE_PATH, "")); //$NON-NLS-1$
		beanConfig.setResourcePackage(DGaaSXApplication.class.getPackage().getName());
		beanConfig.setScan(true);
	}

	@Override
	public Set<Class<?>> getClasses()
	{
		Set<Class<?>> resources = new HashSet<Class<?>>();

		resources.add(RSS2PDFService.class);
		resources.add(JobProxyService.class);
		resources.add(ResultProxyService.class);
		
		resources.add(RSS2TemplateService.class);

		resources.add(com.wordnik.swagger.jersey.listing.ApiListingResource.class);
		resources.add(com.wordnik.swagger.jersey.listing.JerseyApiDeclarationProvider.class);
		resources.add(com.wordnik.swagger.jersey.listing.ApiListingResourceJSON.class);
		resources.add(com.wordnik.swagger.jersey.listing.JerseyResourceListingProvider.class);

		return resources;
	}

}