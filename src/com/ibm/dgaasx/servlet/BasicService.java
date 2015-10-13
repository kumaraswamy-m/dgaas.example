/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2015. All Rights Reserved.
 * 
 * Note to U.S. Government Users Restricted Rights:
 * Use, duplication or disclosure restricted by GSA ADP Schedule
 * Contract with IBM Corp. 
 *******************************************************************************/
package com.ibm.dgaasx.servlet;

import javax.ws.rs.client.Client;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ibm.dgaasx.utils.ConnectionUtils;

@SuppressWarnings("nls")
public class BasicService
{
	protected final Logger log = LoggerFactory.getLogger( this.getClass());
	
	protected Client client = ConnectionUtils.createClient();
	
	protected boolean checkResponse(Response response)
	{
		if (Response.Status.Family.SUCCESSFUL != response.getStatusInfo().getFamily())
		{
			log.info( ">>> ERROR: " + response.getStatusInfo().getStatusCode());
			log.info( ">>> Reason: " + response.getStatusInfo().getReasonPhrase());
			log.info( ">>> Content: " + response.readEntity( String.class));
		}

		return Response.Status.Family.SUCCESSFUL == response.getStatusInfo().getFamily();
	}
	
}
