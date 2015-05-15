package com.ibm.dgaasx.servlet;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

import com.ibm.dgaasx.config.EnvironmentInfo;
import com.wordnik.swagger.jaxrs.config.BeanConfig;

@ApplicationPath("/api")
public class DGaaSXApplication extends Application
{
	private static final String API = "/api";

	public DGaaSXApplication()
	{
		BeanConfig beanConfig = new BeanConfig();
		beanConfig.setContact("dragos.cojocari@ro.ibm.com");
		beanConfig.setDescription( "This API is an example of using DGaaS (Document Generation as a Service) and produces PDF documents from RSS 2.0 feeds." +
								   "<br/><br/>The procedure is:" +
								   "<ol><li>Invoke /dgasaax/api/rss2pdf with the RSS feed you want to convert to PDF.</li>"+
								   "<li>monitor the process at /dgasaax/api/job/<jobid> returned by the first call.</li>"+
								   "<li>access the PDF at /dgasaax/api/result/<resultid> if the job completes succesfully ( the status of the job becomes finished).</li>"+
								   "</ol>" + 
								   "<br/>For questions and support please see the Contact the Developer link."+
								   "<br/><br/>The source code is hosted on Github at <a href='https://github.com/dgaas/dgaas.example'>https://github.com/dgaas/dgaas.example</a>.");
		beanConfig.setVersion("1.0.0");
		beanConfig.setBasePath(EnvironmentInfo.getBaseURL() + API);
		beanConfig.setResourcePackage(CtoFService.class.getPackage().getName());
		beanConfig.setScan(true);
	}

	@Override
	public Set<Class<?>> getClasses()
	{
		Set<Class<?>> resources = new HashSet<Class<?>>();

		resources.add(com.wordnik.swagger.jersey.listing.ApiListingResource.class);
		resources.add(com.wordnik.swagger.jersey.listing.JerseyApiDeclarationProvider.class);
		resources.add(com.wordnik.swagger.jersey.listing.ApiListingResourceJSON.class);
		resources.add(com.wordnik.swagger.jersey.listing.JerseyResourceListingProvider.class);

		//resources.add(CtoFService.class);
		//resources.add(MathService.class);
		resources.add(RSS2PDFService.class);
		resources.add(JobProxyService.class);
		resources.add(ResultProxyService.class);

		return resources;
	}
}