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
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;

import com.ibm.dgaasx.config.DGaaSInfo;
import com.ibm.dgaasx.config.EnvironmentInfo;
import com.ibm.dgaasx.servlet.BasicService;
import com.ibm.dgaasx.servlet.template.RSS2TemplateService;
import com.ibm.dgaasx.utils.DGaaSXConstants;
import com.ibm.dgaasx.utils.JSONUtils;
import com.ibm.rpe.web.service.docgen.api.Parameters;
import com.ibm.rpe.web.service.docgen.api.model.ModifyData;
import com.ibm.rpe.web.service.docgen.api.model.Report;
import com.ibm.rpe.web.service.docgen.api.model.ReportTemplate;
import com.ibm.rpe.web.service.docgen.api.model.ReportTemplate.ReportDataSource;

@Path("/rss2pdf")
public class RSS2PDFService extends BasicService
{
	private static final String TEMPLATE_DATA = "templateData";
	private static final String NEW_OUTPUT = "newOutput";

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

	private Report buildReport(URI baseURI, DGaaSInfo info, String dataSoure, String templateUrl) throws Exception
	{
		WebTarget dgaas = client.target(UriBuilder.fromUri(info.getURL()).build());

		if (templateUrl == null || "".equals(templateUrl))
		{
			boolean isRss = RSS2TemplateService.isRssFeed(dataSoure);
			if (isRss)
			{
				templateUrl = baseURI.resolve("../data/newsfeed.dta").toString();
			}
			else
			{
				templateUrl = baseURI.resolve("../data/requisitepro.dta").toString();
			}
		}

		ModifyData modifyData = new ModifyData();
		modifyData.setUrl(templateUrl);

		List<ModifyData> templateData = new ArrayList<ModifyData>();
		templateData.add(modifyData);

		Form form = new Form();
		form.param(TEMPLATE_DATA, JSONUtils.writeValue(templateData));
		form.param(NEW_OUTPUT, "PDF");

		// create the report
		Response response = dgaas.path("builder").path("change").request(MediaType.APPLICATION_JSON).header(Parameters.BluemixHeader.INSTANCEID, info.getInstanceID()).header(Parameters.BluemixHeader.REGION, info.getRegion()).post(Entity.entity(form,MediaType.APPLICATION_FORM_URLENCODED_TYPE));

		if (!checkResponse(response))
		{
			throw new Exception("Could not create report");
		}

		String reportJSON = response.readEntity( String.class);

		// modify the report, set the data source URI
		Report report = (Report) JSONUtils.fromJSON(reportJSON, Report.class);
		for (ReportTemplate template : report.getTemplates())
		{
			for (ReportDataSource ds : template.getDataSources())
			{
				ds.setProperty("URI", dataSoure == null || dataSoure.trim().isEmpty() ? DGaaSXConstants.BBC_NEW_FEED : dataSoure);
			}
		}

		return report;
	}

	protected String runReport(DocgenConfiguration dgaasConfig) throws Exception
	{
		WebTarget dgaas = client.target(UriBuilder.fromUri(dgaasConfig.getDGaaSInfo().getURL()).build());

		// start dogen
		Form runData = new Form();
		runData.param(Parameters.Form.REPORT, JSONUtils.writeValue(dgaasConfig.report));
		// runData.add(Parameters.Form.TYPE, dgaasConfig.reportType);

		WebTarget jobService = null;
		if (dgaasConfig.jobsServiceURL != null)
		{
			runData.param(Parameters.Form.JOB_SERVICE, dgaasConfig.jobsServiceURL);
			jobService = client.target(UriBuilder.fromUri(dgaasConfig.jobsServiceURL).build());
		}
		else
		{
			jobService = client.target(UriBuilder.fromUri(dgaasConfig.getDGaaSInfo().getURL() + "/data/jobs").build());
		}

		WebTarget fileService = null;
		if (dgaasConfig.fileServiceURL != null)
		{
			runData.param(Parameters.Form.FILE_SERVICE, dgaasConfig.fileServiceURL);
			fileService = client.target(UriBuilder.fromUri(dgaasConfig.fileServiceURL).build());
		}
		else
		{
			fileService = client.target(UriBuilder.fromUri(dgaasConfig.getDGaaSInfo().getURL() + "/data/files").build());
		}

		if (dgaasConfig.jobid != null)
		{
			runData.param(Parameters.Form.JOBID, dgaasConfig.jobid);
		}

		runData.param(Parameters.Form.JOB_SERVICE_TYPE, dgaasConfig.jobServiceType);
		runData.param(Parameters.Form.JOB_SERVICE_USER, dgaasConfig.jobServiceUser);
		runData.param(Parameters.Form.JOB_SERVICE_PASSWORD, dgaasConfig.jobServicePassword);

		if (dgaasConfig.jobServiceUser != null)
		{
			jobService.register( HttpAuthenticationFeature.basicBuilder().nonPreemptive().credentials( dgaasConfig.jobServiceUser, dgaasConfig.jobServicePassword).build());
		}

		runData.param(Parameters.Form.FILE_SERVICE_TYPE, dgaasConfig.fileServiceType);
		runData.param(Parameters.Form.FILE_SERVICE_USER, dgaasConfig.fileServiceUser);
		runData.param(Parameters.Form.FILE_SERVICE_PASSWORD, dgaasConfig.fileServicePassword);
		runData.param(Parameters.Form.PREVIEW_LIMIT, Integer.toString(dgaasConfig.previewLimit));

		if (dgaasConfig.previewLimit > 0)
		{
			fileService.register( HttpAuthenticationFeature.basicBuilder().nonPreemptive().credentials( dgaasConfig.fileServiceUser, dgaasConfig.fileServicePassword).build());
		}

		log.info("Starting docgen ...");

		Response response = dgaas.path("docgen").request(MediaType.APPLICATION_JSON).header(Parameters.Header.SECRET, dgaasConfig.secret).header(Parameters.BluemixHeader.INSTANCEID, dgaasConfig.info.getInstanceID()).header(Parameters.BluemixHeader.REGION, dgaasConfig.info.getRegion()).post(Entity.entity(runData,MediaType.APPLICATION_FORM_URLENCODED_TYPE));
		if (!checkResponse(response))
		{
			log.info("Cannot start document generation");
			throw new Exception("Cannot start document generation");
		}

		return response.readEntity(String.class);
	}

	@POST
	@Produces(MediaType.APPLICATION_JSON)
	public Response rss2pdf(@Context UriInfo uriInfo, @QueryParam(value = "rss") String rss,
							@QueryParam( value = "templateUrl") String templateUrl, 
							@QueryParam( value = "secret") String secret) throws IOException
	{
		DGaaSInfo info = EnvironmentInfo.getDGaaSInfo();

		Report report = null;
		try
		{
			report = buildReport(uriInfo.getBaseUri(), info, rss, templateUrl);
		}
		catch (Exception e)
		{
			log.error("Could not create report.", e);
			return Response.status(Response.Status.BAD_REQUEST).entity("Could not create report").build();
		}

		String jobJSON = null;
		try
		{
			DocgenConfiguration configuration = new DocgenConfiguration(info, report);
			if (secret != null && !secret.trim().isEmpty())
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

		return Response.ok().entity(jobJSON).build();
	}
}
