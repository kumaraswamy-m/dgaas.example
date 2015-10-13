/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2015. All Rights Reserved.
 * 
 * Note to U.S. Government Users Restricted Rights:
 * Use, duplication or disclosure restricted by GSA ADP Schedule
 * Contract with IBM Corp. 
 *******************************************************************************/
package com.ibm.dgaasx.servlet.docgen;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import com.ibm.dgaasx.config.DGaaSInfo;
import com.ibm.dgaasx.config.EnvironmentInfo;
import com.ibm.dgaasx.servlet.BasicService;
import com.ibm.rpe.web.service.docgen.api.Parameters;

@Path("/job")
public class JobProxyService extends BasicService
{
	@GET
	@Path("/{jobID}")
	
	public Response job( @PathParam(value = "jobID") String jobID, @QueryParam(value = "secret") String secret)
	{
		DGaaSInfo info = EnvironmentInfo.getDGaaSInfo();

		WebTarget jobService = client.target(UriBuilder.fromUri(info.getURL()).path("/data/jobs").path(jobID).build()); //$NON-NLS-1$

		Response response = jobService.request(MediaType.APPLICATION_JSON).header(Parameters.Header.SECRET, secret).header(Parameters.BluemixHeader.INSTANCEID, info.getInstanceID()).header(Parameters.BluemixHeader.REGION, info.getRegion()).get();

		if (!checkResponse(response))
		{
			return Response.status(Response.Status.NOT_FOUND).entity("Job information cannot be retrieved. Verify the ID and secret.").build(); //$NON-NLS-1$
		}

		String jobJSON = response.readEntity( String.class);

		return Response.ok().entity(jobJSON).build();
	}

}
