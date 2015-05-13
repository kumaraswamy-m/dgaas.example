package com.ibm.dgaax.servlet;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

import com.wordnik.swagger.jaxrs.config.BeanConfig;

@ApplicationPath("/app")
public class DGaaSXApplication extends Application 
{
	public DGaaSXApplication() 
	{
		BeanConfig beanConfig = new BeanConfig();
		beanConfig.setVersion("1.0.1");
		beanConfig.setBasePath("http://localhost:8080/dgaasx/app");
		beanConfig.setResourcePackage( CtoFService.class.getPackage().getName());
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

		resources.add( CtoFService.class);

		return resources;
	}
}