package com.ibm.dgaasx.servlet;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ibm.dgaasx.config.EnvironmentInfo;
import com.ibm.dgaasx.utils.ConnectionUtils;
import com.ibm.rpe.web.service.docgen.api.Parameters;
import com.ibm.rpe.web.service.docgen.api.model.DocgenJob;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;

@Path("/job")
@Api(value = "/job", description = "A proxy to the Job Service used by DGaaS")
public class JobProxyService
{
	private final static Logger log = LoggerFactory.getLogger(JobProxyService.class);
	
	private Client client = ConnectionUtils.createClient();

	protected boolean checkResponse(ClientResponse response)
	{
		if (Response.Status.Family.SUCCESSFUL != response.getStatusInfo().getFamily())
		{
			log.info( ">>> ERROR: " + response.getStatusInfo().getStatusCode());
			log.info( ">>> Reason phrase: " + response.getStatusInfo().getReasonPhrase());
			log.info( ">>> Details: " + response.getEntity(String.class));
		}

		return Response.Status.Family.SUCCESSFUL == response.getStatusInfo().getFamily();
	}
	
	@GET
	@Path( "/{jobID}")
	@Produces( MediaType.APPLICATION_JSON)
	@ApiOperation(value = "Request information for a document generation job", notes = "Request information for a document generation job using the job's ID and the secret token. The information return consists of job status, events and if the job is finished, the results.", response = DocgenJob.class, produces="application/json")
	@ApiResponses(value = { @ApiResponse(code = 400, message = "Invalid value"), @ApiResponse(code = 404, message = "Job information cannot be retrieved. Verify the ID and secret.") })
	public Response job( @ApiParam(value = "The id of the job as returned by the /docgen method", required = true)  @PathParam(value="jobID") String jobID, 
						 @ApiParam(value = "Job secret as returned by the /docgen method", required = true)   @QueryParam(value="secret") String secret)
	{
		
		WebResource jobService = client.resource(UriBuilder.fromUri(EnvironmentInfo.getDGaaSURL()).path("/data/jobs").path( jobID).build());
		
		ClientResponse response = jobService.header(Parameters.Header.SECRET, secret).accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);
		if ( !checkResponse( response))
		{
			return Response.status(Response.Status.NOT_FOUND).entity("Job information cannot be retrieved. Verify the ID and secret.").build();
		}
		
		String jobJSON = response.getEntity(String.class);
		
		return Response.ok().entity( jobJSON).build();
	}
	
	
}
