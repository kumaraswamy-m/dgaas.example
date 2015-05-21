package com.ibm.dgaasx.servlet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import com.ibm.dgaasx.config.DGaaSInfo;
import com.ibm.dgaasx.config.EnvironmentInfo;
import com.ibm.dgaasx.utils.JSONUtils;
import com.ibm.rpe.web.service.docgen.api.Parameters;
import com.ibm.rpe.web.service.docgen.api.model.DocgenJob;
import com.ibm.rpe.web.service.docgen.api.model.ModifyData;
import com.ibm.rpe.web.service.docgen.api.model.Report;
import com.ibm.rpe.web.service.docgen.api.model.ReportTemplate;
import com.ibm.rpe.web.service.docgen.api.model.ReportTemplate.ReportDataSource;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;
import com.sun.jersey.api.client.filter.HTTPBasicAuthFilter;
import com.sun.jersey.core.util.MultivaluedMapImpl;
import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;

@Path("/rss2pdf")
@Api(value = "/rss2pdf", description = "Service for rendering an RSS 2.0 feed in PDF.")
public class RSS2PDFService extends BasicService
{
	private static final String TEMPLATE_DATA = "templateData";
	private static final String NEW_OUTPUT = "newOutput";
	private static final String BBC_NEW_FEED = "http://feeds.bbci.co.uk/news/rss.xml";
	
	public static final class DocgenConfiguration
	{
		public DGaaSInfo info = null;
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
		public String secret = null;

		public int previewLimit = 0;

		public DocgenConfiguration(DGaaSInfo info, Report report)
		{
			this.info = info;
			this.report = report;
		}

		public DocgenConfiguration(DGaaSInfo info, Report report, String jobsServiceURL, String fileServiceURL)
		{
			this.info = info;
			this.report = report;
			this.jobsServiceURL = jobsServiceURL;
			this.fileServiceURL = fileServiceURL;
		}
		
		public DGaaSInfo getDGaaSInfo()
		{
			return info;
		}
	}

	private Report buildReport(DGaaSInfo info, String dataSoure) throws Exception
	{
		WebResource dgaas = client.resource(UriBuilder.fromUri(info.getURL()).build());

		MultivaluedMap<String, String> formData = new MultivaluedMapImpl();

		ModifyData modifyData = new ModifyData();
		modifyData.setUrl( EnvironmentInfo.getBaseURL()+"/data/newsfeed.dta");

		List<ModifyData> templateData = new ArrayList<ModifyData>();
		templateData.add(modifyData);

		formData.add(TEMPLATE_DATA, JSONUtils.writeValue(templateData));
		//formData.add(NEW_OUTPUT, "Word");
		formData.add(NEW_OUTPUT, "PDF");

		// create the report
		ClientResponse response = dgaas.path("builder").path("change")
									.header(Parameters.BluemixHeader.INSTANCEID, info.getInstanceID())
									.header(Parameters.BluemixHeader.REGION, info.getRegion())
									.accept(MediaType.APPLICATION_JSON).post(ClientResponse.class, formData);
		String reportJSON = response.getEntity(String.class);
		if ( !checkResponse( response))
		{
			throw new Exception("Could not create report");
		}

		// modify the report, set the data source URI
		Report report = (Report) JSONUtils.fromJSON(reportJSON, Report.class);
		for (ReportTemplate template : report.getTemplates())
		{
			for (ReportDataSource ds : template.getDataSources())
			{
				ds.setProperty("URI", dataSoure == null || dataSoure.trim().isEmpty() ? BBC_NEW_FEED : dataSoure);
			}
		}

		return report;
	}

	protected String runReport(DocgenConfiguration dgaasConfig) throws Exception
	{
		WebResource dgaas = client.resource(UriBuilder.fromUri( dgaasConfig.getDGaaSInfo().getURL()).build());

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
			jobService = client.resource(UriBuilder.fromUri(dgaasConfig.getDGaaSInfo().getURL() + "/data/jobs").build());
		}

		WebResource fileService = null;
		if (dgaasConfig.fileServiceURL != null)
		{
			runData.add(Parameters.Form.FILE_SERVICE, dgaasConfig.fileServiceURL);
			fileService = client.resource(UriBuilder.fromUri(dgaasConfig.fileServiceURL).build());
		}
		else
		{
			fileService = client.resource(UriBuilder.fromUri(dgaasConfig.getDGaaSInfo().getURL() + "/data/files").build());
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

		ClientResponse response = dgaas.path("docgen")
								.header(Parameters.Header.SECRET, dgaasConfig.secret)
								.header(Parameters.BluemixHeader.INSTANCEID, dgaasConfig.info.getInstanceID())
								.header(Parameters.BluemixHeader.REGION, dgaasConfig.info.getRegion())
								.accept(MediaType.APPLICATION_JSON).post(ClientResponse.class, runData);
		if ( !checkResponse( response))
		{
			throw new Exception("Cannot start document generation");
		}

		return response.getEntity(String.class);
	}

	@POST
	@Produces( MediaType.APPLICATION_JSON)
	@ApiOperation(value = "Convert an RSS 2.0 feed to PDF", notes = "Uses a predefined template to produce a PDF document rendering the news feed.", response = DocgenJob.class, produces="application/json")
	@ApiResponses(value = { @ApiResponse(code = 400, message = "Invalid value") })
	public Response rss2pdf( @ApiParam(value = "The RSS Feed to convert to PDF", required = false)  @QueryParam(value="rss") String rss,
							 @ApiParam(value = "A secret to secure the document generation with", required = false) @QueryParam(value="secret") String secret) throws IOException
	{
		DGaaSInfo info = EnvironmentInfo.getDGaaSInfo();

		Report report = null;
		try
		{
			report = buildReport( info, rss);
		}
		catch (Exception e)
		{
			log.error("Could not create report.", e);
			return Response.status(Response.Status.BAD_REQUEST).entity("Could not create report").build();
		}

		String jobJSON = null;
		try
		{
			DocgenConfiguration configuration = new DocgenConfiguration( info, report);
			if ( secret!=null && !secret.trim().isEmpty())
			{
				configuration.secret = secret.trim();
			}
			
			jobJSON = runReport(configuration);
		}
		catch (Exception e)
		{
			log.error("Could not start docgen.", e);
			return Response.status(Response.Status.BAD_REQUEST).entity("Could not start docgen job").build();
		}

		return Response.ok().entity( jobJSON).build();
	}
}
