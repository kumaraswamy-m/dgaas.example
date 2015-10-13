/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2015. All Rights Reserved.
 * 
 * Note to U.S. Government Users Restricted Rights:
 * Use, duplication or disclosure restricted by GSA ADP Schedule
 * Contract with IBM Corp. 
 *******************************************************************************/
package com.ibm.dgaasx.servlet.docgen;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriBuilder;

import org.apache.commons.io.IOUtils;

import com.ibm.dgaasx.config.DGaaSInfo;
import com.ibm.dgaasx.config.EnvironmentInfo;
import com.ibm.dgaasx.servlet.BasicService;
import com.ibm.dgaasx.utils.DGaaSXConstants;
import com.ibm.rpe.web.service.docgen.api.Parameters;

@Path("/result")
@SuppressWarnings("nls")
public class ResultProxyService extends BasicService
{
	@GET
	@Path("/{resultID}")
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	public Response result( @PathParam( value = "resultID") String resultID,  @QueryParam(value = "secret") String secret)
	{
		DGaaSInfo info = EnvironmentInfo.getDGaaSInfo();

		WebTarget resultService = client.target(UriBuilder.fromUri(info.getURL()).path("/data/files").path(resultID).build());

		Response response = resultService.request(MediaType.APPLICATION_OCTET_STREAM).header(Parameters.Header.SECRET, secret).header(Parameters.BluemixHeader.INSTANCEID, info.getInstanceID()).header(Parameters.BluemixHeader.REGION, info.getRegion()).get();
		if (!checkResponse(response))
		{
			return Response.status(Response.Status.NOT_FOUND).entity("Result cannot be retrieved. Verify the ID and secret.").build();
		}

		String contentDisposition = (String) response.getHeaders().getFirst(DGaaSXConstants.CONTENT_DISPOSITION);
		String fileName = "result";
		if (contentDisposition != null)
		{
			int pos = contentDisposition.indexOf(DGaaSXConstants.CONTENT_DISPOSITION_FILENAME);
			if (pos >= 0)
			{
				fileName = contentDisposition.substring(pos + DGaaSXConstants.CONTENT_DISPOSITION_FILENAME.length());
			}
		}

		log.info( "Downloading result ...");
		
		final InputStream is = (InputStream) response.getEntity();
		
		// OR: use a custom StreamingOutput and set to Response
		StreamingOutput stream = new StreamingOutput()
		{
			@Override
			public void write(OutputStream output) throws IOException
			{
				IOUtils.copy(is, output);
			}
		};

		return Response.ok(stream, MediaType.APPLICATION_OCTET_STREAM).header("content-disposition", "attachment; filename = " + fileName).build(); //$NON-NLS-1$ //$NON-NLS-2$
	}

}
