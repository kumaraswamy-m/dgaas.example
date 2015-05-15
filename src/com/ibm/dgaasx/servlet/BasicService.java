package com.ibm.dgaasx.servlet;

import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ibm.dgaasx.utils.ConnectionUtils;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;

public class BasicService
{
	protected final Logger log = LoggerFactory.getLogger( this.getClass());
	
	protected Client client = ConnectionUtils.createClient();
	
	protected boolean checkResponse(ClientResponse response)
	{
		if (Response.Status.Family.SUCCESSFUL != response.getStatusInfo().getFamily())
		{
			log.info( ">>> ERROR: " + response.getStatusInfo().getStatusCode());
			log.info( ">>> Reason: " + response.getStatusInfo().getReasonPhrase());
			log.info( ">>> Content: " + response.getEntity(String.class));
		}

		return Response.Status.Family.SUCCESSFUL == response.getStatusInfo().getFamily();
	}
	
}
