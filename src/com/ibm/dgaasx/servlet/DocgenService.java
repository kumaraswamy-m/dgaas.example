package com.ibm.dgaasx.servlet;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.xml.bind.JAXBException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ibm.dgaasx.config.EnvironmentInfo;
import com.ibm.dgaasx.utils.ConnectionUtils;
import com.ibm.dgaasx.utils.JSONUtils;
import com.ibm.rpe.web.service.docgen.api.Parameters;
import com.ibm.rpe.web.service.docgen.api.model.DocgenJob;
import com.ibm.rpe.web.service.docgen.api.model.DocgenJob.ReportResult;
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
		Client client = ConnectionUtils.createClient();

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

	protected boolean handleResponse(String jobID, ClientResponse response)
	{
		if (!response.getStatusInfo().getFamily().equals(Response.Status.Family.SUCCESSFUL))
		{
			logJobMessage(jobID, ">>> ERROR: " + response.getStatusInfo().getStatusCode());
			logJobMessage(jobID, ">>> Reason phrase: " + response.getStatusInfo().getReasonPhrase());
			logJobMessage(jobID, ">>> Details: " + response.getEntity(String.class));
		}

		return Response.Status.Family.SUCCESSFUL == response.getStatusInfo().getFamily();
	}

	private void logJobMessage(String jobid, String message)
	{
		log.info(jobid + ": " + message);
	}

	protected String runReport(DocgenConfiguration dgaasConfig) throws ClientHandlerException, UniformInterfaceException, IOException, JAXBException
	{
		if (dgaasConfig.secret == null)
		{
			dgaasConfig.secret = Long.toString(System.currentTimeMillis());
		}

		Client client = ConnectionUtils.createClient();

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

		logJobMessage("", "Starting docgen ...");

		ClientResponse response = dgaas.path("docgen").header(Parameters.Header.SECRET, dgaasConfig.secret).accept(MediaType.APPLICATION_JSON).post(ClientResponse.class, runData);
		handleResponse("-", response);

		String jobJSON = response.getEntity(String.class);
		DocgenJob job = (DocgenJob) JSONUtils.fromJSON(jobJSON, DocgenJob.class);

		String jobID = job.getID();
		logJobMessage(jobID, ">>> JOB ID: " + jobID);
		logJobMessage(jobID, ">>> JOB URL: " + job.getHref());

		/*
		long eventFrom = 0;
		long shownEvents = 0;
		String lastStatus = "";
		while (true)
		{
			response = jobService.path(jobID).queryParam("eventfrom", Long.toString(eventFrom)).header(Parameters.Header.SECRET, dgaasConfig.secret).accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);
			handleResponse(jobID, response);

			jobJSON = response.getEntity(String.class);

			job = (DocgenJob) JSONUtils.fromJSON(jobJSON, DocgenJob.class);
			if (Response.Status.OK.getStatusCode() != response.getStatus())
			{
				break;
			}

			int curPos = 0;
			for (String event : job.getEvents())
			{
				// if the service cannot respond with paged events, show only
				// the new events
				if (curPos >= shownEvents)
				{
					logJobMessage(jobID, "\t" + event);
					++shownEvents;
				}

				++curPos;
			}

			if (!lastStatus.equals(job.getStatus()))
			{
				logJobMessage(jobID, ">>> Status: " + job.getStatus());
				lastStatus = job.getStatus();
			}

			// inc event pos
			eventFrom = eventFrom + job.getEvents().size();

			try
			{
				Thread.sleep(100);
			}
			catch (InterruptedException e)
			{
				e.printStackTrace();
			}

			if (job.getStatus().equals("finished") || job.getStatus().equals("error"))
			{
				break;
			}
		}

		// read the results from the storage service
		if (job.getStatus().equals("finished"))
		{
			for (ReportResult result : job.getResults())
			{
				// setup the access to the resource
				String resultURL = null;

				logJobMessage(jobID, "Result URI: " + result.getURI());

				if (URI.create(result.getURI()).isAbsolute())
				{
					resultURL = result.getURI();
				}
				else
				{
					resultURL = fileService.getURI().toString() + "/" + result.getURI();
				}

				logJobMessage(jobID, "Result URL: " + resultURL);

			
			}
		}
*/
		
		return job.getHref();
	}

	/*
	 * WebResource resultResource =
	 * client.resource(UriBuilder.fromUri(resultURL).build());
	 * 
	 * if ( dgaasConfig.fileServiceUser != null) {
	 * resultResource.addFilter(new
	 * HTTPBasicAuthFilter(dgaasConfig.fileServiceUser,
	 * dgaasConfig.fileServicePassword)); }
	 * 
	 * response = resultResource.header(Parameters.Header.SECRET,
	 * dgaasConfig
	 * .secret).type(MediaType.APPLICATION_OCTET_STREAM).get
	 * (ClientResponse.class); handleResponse(response);
	 * 
	 * InputStream is = response.getEntityInputStream();
	 * 
	 * File localFile = new File("d:\\tmp\\results\\" +
	 * result.getName()); localFile.getParentFile().mkdirs();
	 * IOUtils.copy(is, new FileOutputStream(localFile));
	 * 
	 * logJobMessage( jobID, "File downloaded to: " +
	 * localFile.getAbsolutePath());
	 */
	
	@POST
	@Produces("text/plain")
	@ApiOperation(value = "Request a document to be produced by DGaaS", notes = "More notes about this method", response = String.class)
	@ApiResponses(value = { @ApiResponse(code = 400, message = "Invalid value") })
	public Response docgen()
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

		String docgenURL = null;
		try
		{
			DocgenConfiguration configuration = new DocgenConfiguration(dgaasURL, report);
			docgenURL = runReport(configuration);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return Response.status(Response.Status.BAD_REQUEST).entity("Could not create report").build();
		}

		return Response.ok().entity(docgenURL).build();
	}
}
