package com.ibm.dgaasx.servlet;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriBuilder;
import javax.xml.bind.JAXBException;

import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ibm.dgaasx.config.EnvironmentInfo;
import com.ibm.dgaasx.utils.ConnectionUtils;
import com.ibm.dgaasx.utils.JSONUtils;
import com.ibm.rpe.web.service.docgen.api.Parameters;
import com.ibm.rpe.web.service.docgen.api.model.DocgenJob;
import com.ibm.rpe.web.service.docgen.api.model.ModifyData;
import com.ibm.rpe.web.service.docgen.api.model.Report;
import com.ibm.rpe.web.service.docgen.api.model.ReportTemplate;
import com.ibm.rpe.web.service.docgen.api.model.ReportTemplate.ReportDataSource;
import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientHandlerException;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.UniformInterfaceException;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;

@Path("/docgen")
@Api(value = "/docgen", description = "Document generation functionality")
public class DocgenService
{
	private final static Logger log = LoggerFactory.getLogger(DocgenService.class);

	public static final String TEMPLATE_DATA = "templateData";
	public static final String NEW_OUTPUT = "newOutput";
	
	private Client client = ConnectionUtils.createClient();

	public static final class DocgenConfiguration
	{
		public String dgaasURL = null;
		public Report report = null;

		public String jobsServiceURL = null;
		public String jobServiceType = null;
		public String jobServiceUser = null;
		public String jobServicePassword = null;

		public String fileServiceURL = null;
		public String fileServiceType = null;
		public String fileServiceUser = null;
		public String fileServicePassword = null;

		public String jobid = null;
		public String secret = UUID.randomUUID().toString();

		public int previewLimit = 0;

		public DocgenConfiguration(String dgaasURL, Report report)
		{
			this.dgaasURL = dgaasURL;
			this.report = report;
		}

		public DocgenConfiguration(String dgaasURL, Report report, String jobsServiceURL, String fileServiceURL)
		{
			this.dgaasURL = dgaasURL;
			this.report = report;
			this.jobsServiceURL = jobsServiceURL;
			this.fileServiceURL = fileServiceURL;
		}
	}

	private Report buildReport(String dgaasURL) throws IOException
	{
		WebResource dgaas = client.resource(UriBuilder.fromUri(dgaasURL).build());

		MultivaluedMap<String, String> formData = new MultivaluedMapImpl();

		ModifyData modifyData = new ModifyData();
		modifyData.setUrl("http://localhost:8080/dgaasx/data/requisitepro.dta");

		List<ModifyData> templateData = new ArrayList<ModifyData>();
		templateData.add(modifyData);

		formData.add(TEMPLATE_DATA, JSONUtils.writeValue(templateData));
		formData.add(NEW_OUTPUT, "Word");
		formData.add(NEW_OUTPUT, "PDF");

		// create the report
		ClientResponse response = dgaas.path("builder").path("change").accept(MediaType.APPLICATION_JSON).post(ClientResponse.class, formData);
		String reportJSON = response.getEntity(String.class);
		if (Response.Status.OK.getStatusCode() != response.getStatus())
		{
			// /throw new Exception("Could not create report");
		}

		// modify the report, set the data source URI
		Report report = (Report) JSONUtils.fromJSON(reportJSON, Report.class);
		for (ReportTemplate template : report.getTemplates())
		{
			for (ReportDataSource ds : template.getDataSources())
			{
				ds.setProperty("URI", "http://localhost:8080/dgaasx/data/requisitepro.xml");
			}
		}

		return report;
	}

	protected boolean handleResponse(ClientResponse response)
	{
		if (!response.getStatusInfo().getFamily().equals(Response.Status.Family.SUCCESSFUL))
		{
			log.info( ">>> ERROR: " + response.getStatusInfo().getStatusCode());
			log.info( ">>> Reason phrase: " + response.getStatusInfo().getReasonPhrase());
			log.info( ">>> Details: " + response.getEntity(String.class));
		}

		return Response.Status.Family.SUCCESSFUL == response.getStatusInfo().getFamily();
	}

	protected Map<String, String> runReport(DocgenConfiguration dgaasConfig) throws ClientHandlerException, UniformInterfaceException, IOException, JAXBException
	{
		if (dgaasConfig.secret == null)
		{
			dgaasConfig.secret = Long.toString(System.currentTimeMillis());
		}

		WebResource dgaas = client.resource(UriBuilder.fromUri(dgaasConfig.dgaasURL).build());

		// start dogen
		MultivaluedMap<String, String> runData = new MultivaluedMapImpl();
		runData.add(Parameters.Form.REPORT, JSONUtils.writeValue(dgaasConfig.report));
		// runData.add(Parameters.Form.TYPE, dgaasConfig.reportType);

		WebResource jobService = null;
		if (dgaasConfig.jobsServiceURL != null)
		{
			runData.add(Parameters.Form.JOB_SERVICE, dgaasConfig.jobsServiceURL);
			jobService = client.resource(UriBuilder.fromUri(dgaasConfig.jobsServiceURL).build());
		}
		else
		{
			jobService = client.resource(UriBuilder.fromUri(dgaasConfig.dgaasURL + "/data/jobs").build());
		}

		WebResource fileService = null;
		if (dgaasConfig.fileServiceURL != null)
		{
			runData.add(Parameters.Form.FILE_SERVICE, dgaasConfig.fileServiceURL);
			fileService = client.resource(UriBuilder.fromUri(dgaasConfig.fileServiceURL).build());
		}
		else
		{
			fileService = client.resource(UriBuilder.fromUri(dgaasConfig.dgaasURL + "/data/files").build());
		}

		if (dgaasConfig.jobid != null)
		{
			runData.add(Parameters.Form.JOBID, dgaasConfig.jobid);
		}

		runData.add(Parameters.Form.JOB_SERVICE_TYPE, dgaasConfig.jobServiceType);
		runData.add(Parameters.Form.JOB_SERVICE_USER, dgaasConfig.jobServiceUser);
		runData.add(Parameters.Form.JOB_SERVICE_PASSWORD, dgaasConfig.jobServicePassword);

		if (dgaasConfig.jobServiceUser != null)
		{
			jobService.addFilter(new HTTPBasicAuthFilter(dgaasConfig.jobServiceUser, dgaasConfig.jobServicePassword));
		}

		runData.add(Parameters.Form.FILE_SERVICE_TYPE, dgaasConfig.fileServiceType);
		runData.add(Parameters.Form.FILE_SERVICE_USER, dgaasConfig.fileServiceUser);
		runData.add(Parameters.Form.FILE_SERVICE_PASSWORD, dgaasConfig.fileServicePassword);
		runData.add(Parameters.Form.PREVIEW_LIMIT, Integer.toString(dgaasConfig.previewLimit));

		if (dgaasConfig.previewLimit > 0)
		{
			fileService.addFilter(new HTTPBasicAuthFilter(dgaasConfig.fileServiceUser, dgaasConfig.fileServicePassword));

		}

		log.info( "Starting docgen ...");

		ClientResponse response = dgaas.path("docgen").header(Parameters.Header.SECRET, dgaasConfig.secret).accept(MediaType.APPLICATION_JSON).post(ClientResponse.class, runData);
		handleResponse(response);

		String jobJSON = response.getEntity(String.class);
		DocgenJob job = (DocgenJob) JSONUtils.fromJSON(jobJSON, DocgenJob.class);

		String jobID = job.getID();
		log.info( ">>> JOB ID: " + jobID);
		log.info( ">>> JOB URL: " + job.getHref());

		Map<String, String> result = new HashMap<String, String>();
		result.put( "id", job.getID());
		result.put( "href", job.getHref());
		result.put( "secret", dgaasConfig.secret);
		
		return result;
	}
	
	
	@GET
	@Path( "/job")
	@Produces( MediaType.APPLICATION_JSON)
	@ApiOperation(value = "Get information about a document generation job", notes = "More notes about this method", response = String.class)
	@ApiResponses(value = { @ApiResponse(code = 400, message = "Invalid value") })
	public Response job( @QueryParam(value="jobURL") String jobURL, @QueryParam(value="secret") String secret)
	{
		WebResource jobService = client.resource(UriBuilder.fromUri(jobURL).build());
		
		ClientResponse response = jobService.header(Parameters.Header.SECRET, secret).accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);
		handleResponse( response);
		
		String jobJSON = response.getEntity(String.class);
		
		return Response.ok().entity( jobJSON).build();
	}
	
	@GET
	@Path( "/result")
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	@ApiOperation(value = "Download the result of a document generation job", notes = "More notes about this method", response = String.class)
	@ApiResponses(value = { @ApiResponse(code = 400, message = "Invalid value") })
	public Response result( @QueryParam(value="resultURL") String resultURL, @QueryParam(value="secret") String secret)
	{
		WebResource resultService = client.resource(UriBuilder.fromUri(resultURL).build());
		
		ClientResponse response = resultService.header(Parameters.Header.SECRET,secret).type(MediaType.APPLICATION_OCTET_STREAM).get(ClientResponse.class); 
		handleResponse( response);
		
		final InputStream is = response.getEntityInputStream();
		
		// OR: use a custom StreamingOutput and set to Response
		StreamingOutput stream = new StreamingOutput() 
		{
			@Override
			public void write(OutputStream output) throws IOException
			{
				IOUtils.copy(is, output);
			}
		};

		String name = "xyz.doc";

		return Response.ok(stream, MediaType.APPLICATION_OCTET_STREAM).header("content-disposition", "attachment; filename = " + name).build(); //$NON-NLS-1$ //$NON-NLS-2$
	}
	

	@POST
	@Produces("text/plain")
	@ApiOperation(value = "Request a document to be produced by DGaaS", notes = "More notes about this method", response = String.class)
	@ApiResponses(value = { @ApiResponse(code = 400, message = "Invalid value") })
	public Response docgen() throws IOException
	{
		String dgaasURL = EnvironmentInfo.getDGaaSURL();

		Report report = null;
		try
		{
			report = buildReport(dgaasURL);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return Response.status(Response.Status.BAD_REQUEST).entity("Could not create report").build();
		}

		Map<String, String> jobInfo = null;
		try
		{
			DocgenConfiguration configuration = new DocgenConfiguration(dgaasURL, report);
			jobInfo = runReport(configuration);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return Response.status(Response.Status.BAD_REQUEST).entity("Could not create report").build();
		}

		return Response.ok().entity( JSONUtils.writeValue(jobInfo)).build();
	}
}
