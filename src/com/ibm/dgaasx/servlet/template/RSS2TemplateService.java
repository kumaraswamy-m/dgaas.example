/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2015. All Rights Reserved.
 * 
 * Note to U.S. Government Users Restricted Rights:
 * Use, duplication or disclosure restricted by GSA ADP Schedule
 * Contract with IBM Corp. 
 *******************************************************************************/
package com.ibm.dgaasx.servlet.template;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.io.IOUtils;

import com.ibm.dgaasx.config.DGaaSInfo;
import com.ibm.dgaasx.config.EnvironmentInfo;
import com.ibm.dgaasx.servlet.BasicService;
import com.ibm.dgaasx.utils.CommonUtils;
import com.ibm.dgaasx.utils.DGaaSXConstants;
import com.ibm.dgaasx.utils.JSONUtils;
import com.ibm.dgaasx.utils.TemplateConstants;
import com.ibm.rpe.web.service.docgen.api.Parameters;
import com.ibm.rpe.web.service.template.api.model.Operation;
import com.ibm.rpe.web.service.template.api.model.Template;
import com.ibm.rpe.web.service.template.api.model.TemplateElement;
import com.ibm.rpe.web.service.template.api.model.TemplateSchema;

@Path("/rss2template")
@SuppressWarnings("nls")
public class RSS2TemplateService extends BasicService
{
	private static Map<String, String> templateIdToPathMap = new HashMap<String, String>();
	DGaaSInfo info = null;

	@POST
	@Path("/json")
	@Produces(MediaType.APPLICATION_JSON)
	public Response rss2templatejson(@Context UriInfo uriInfo, @QueryParam(value = "url") String xmlUrl,
			@QueryParam(value = "secret") String secret) throws IOException
	{
		info = EnvironmentInfo.getDGaaSInfo();

		if (xmlUrl == null || "".equals(xmlUrl.trim()))
		{
			xmlUrl = DGaaSXConstants.BBC_NEW_FEED;
		}

		if (xmlUrl == null || "".equals(xmlUrl.trim()))
		{
			String errMsg = "URL is empty";
			System.out.println(errMsg);
			return Response.status(Response.Status.BAD_REQUEST).type(MediaType.TEXT_PLAIN).entity(errMsg).build();
		}
		boolean isRss = isRssFeed(xmlUrl);

		String jsonTemplate = null;

		try
		{
			if (isRss)
			{
				jsonTemplate = buildReqRssTemplate(xmlUrl, false);
			}
			else
			{
				jsonTemplate = buildReqProTemplate(xmlUrl, uriInfo.getBaseUri().resolve("../data/Desert.jpg").toString(), false);

			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		if (jsonTemplate == null || "".equals(jsonTemplate.trim()))
		{
			String errMsg = "JSON template could not be generated";
			System.out.println(errMsg);
			return Response.status(Response.Status.BAD_REQUEST).type(MediaType.TEXT_PLAIN).entity(errMsg).build();
		}

		return Response.ok().entity(jsonTemplate).build();
	}

	@POST
	@Path("/dta")
	@Produces(MediaType.TEXT_PLAIN)
	public Response rss2templatedta( @FormParam( value = "template") String templateJson,
									 @QueryParam( value = "secret") String secret) throws IOException
	{
		if (templateJson == null || "".equals(templateJson.trim()))
		{
			String errMsg = "JSON template could not be generated";
			System.out.println(errMsg);
			return Response.status(Response.Status.BAD_REQUEST).type(MediaType.TEXT_PLAIN).entity(errMsg).build();
		}

		info = EnvironmentInfo.getDGaaSInfo();

		Form form = new Form();
		form.param("template", templateJson);

		WebTarget dtaService = client.target(UriBuilder.fromUri(info.getURL()).path("/template/createdta").build());

		Response response = dtaService.request(MediaType.APPLICATION_OCTET_STREAM).header(Parameters.Header.SECRET, secret).header(Parameters.BluemixHeader.INSTANCEID, info.getInstanceID()).header(Parameters.BluemixHeader.REGION, info.getRegion()).post( Entity.entity(form,MediaType.APPLICATION_FORM_URLENCODED_TYPE));
		if (!checkResponse(response))
		{
			return Response.status(Response.Status.NOT_FOUND).entity("Result cannot be retrieved. Verify the ID and secret.").build();
		}

		InputStream inputStream = response.readEntity(InputStream.class);

		String templatePath = System.getProperty("java.io.tmpdir") + "template_" + UUID.randomUUID().toString() + ".dta";
		FileOutputStream fos = new FileOutputStream(templatePath);
		IOUtils.copy(inputStream, fos);
		fos.flush();
		fos.close();

		System.out.println("Template path: \n" + templatePath);

		String templateId = UUID.randomUUID().toString();

		templateIdToPathMap.put(templateId, templatePath);

		return Response.ok().entity(templateId).build();
	}

	@GET
	@Path("/{templateID}")
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	public Response downloadTemplate( @PathParam(value = "templateID") String templateID, @QueryParam( value = "secret") String secret) throws FileNotFoundException
	{
		String templatePath = templateIdToPathMap.get(templateID);

		String fileName = "template_" + UUID.randomUUID() + ".dta";
		final InputStream is = new FileInputStream(templatePath);

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

	private String buildReqRssTemplate(String xmlUrl, boolean printTemplate) throws Exception
	{
		Template template = createInitialTemplate("Generic XML", "RSS", info.getURL() + "/utils/xmltoxsd?url=" + xmlUrl, null);
		String templateJSON = JSONUtils.writeValue(template);

		// add top container
		TemplateElement containerElement = createElementBean("top container", TemplateConstants.ELEMENT_CONTAINER, null, null, null, null, null);
		containerElement.setId(null);
		containerElement.setSchema("RSS");
		containerElement.setQuery("rss/channel");
		templateJSON = callAPI(buildCommand(TemplateConstants.OPERATION_ADD, containerElement, null, null), templateJSON, printTemplate, "Template after adding container");
		template = (Template) JSONUtils.readValue(templateJSON, Template.class);
		String topContainerId = template.getLastActedElement().getId();

		// add paragraph title
		TemplateElement paragraphElement = createElementBean(null, TemplateConstants.ELEMENT_PARAGPRAPH, null, null, null, null, null);
		paragraphElement.setId(null);
		paragraphElement.addProperty(TemplateConstants.STYLE_NAME, "Title");
		templateJSON = callAPI(buildCommand(TemplateConstants.OPERATION_ADD, paragraphElement, topContainerId, TemplateConstants.LOCATION_CHILD), templateJSON, printTemplate, "Template after adding title paragraph");
		template = (Template) JSONUtils.readValue(templateJSON, Template.class);

		// add title
		TemplateElement titleElement = createElementBean(null, TemplateConstants.ELEMENT_TEXT, null, topContainerId, null, "rss/channel/title", null);
		titleElement.setId(null);
		templateJSON = callAPI(buildCommand(TemplateConstants.OPERATION_ADD, titleElement, template.getLastActedElement().getId(), TemplateConstants.LOCATION_CHILD), templateJSON, printTemplate, "Template after adding title text");
		template = (Template) JSONUtils.readValue(templateJSON, Template.class);

		// add image container
		containerElement = createElementBean(null, TemplateConstants.ELEMENT_CONTAINER, null, null, null, null, null);
		containerElement.setId(null);
		containerElement.setContext(topContainerId);
		containerElement.setSchema("RSS");
		containerElement.setQuery("rss/channel/image");
		templateJSON = callAPI(buildCommand(TemplateConstants.OPERATION_ADD, containerElement, topContainerId, TemplateConstants.LOCATION_CHILD), templateJSON, printTemplate, "Template after adding container");
		template = (Template) JSONUtils.readValue(templateJSON, Template.class);
		String imageContainerId = template.getLastActedElement().getId();

		// add paragraph title
		paragraphElement = createElementBean(null, TemplateConstants.ELEMENT_PARAGPRAPH, null, null, null, null, null);
		paragraphElement.setId(null);
		paragraphElement.addProperty("paragraph alignment", "center");
		templateJSON = callAPI(buildCommand(TemplateConstants.OPERATION_ADD, paragraphElement, template.getLastActedElement().getId(), TemplateConstants.LOCATION_CHILD), templateJSON, printTemplate, "Template after adding title paragraph");
		template = (Template) JSONUtils.readValue(templateJSON, Template.class);

		// add image
		TemplateElement imageElement = createElementBean(null, TemplateConstants.ELEMENT_IMAGE, null, null, null, "Sample Requirements", null);
		imageElement.setId(null);
		imageElement.setContext(imageContainerId);
		imageElement.setContent("rss/channel/image/url");
		templateJSON = callAPI(buildCommand(TemplateConstants.OPERATION_ADD, imageElement, template.getLastActedElement().getId(), TemplateConstants.LOCATION_CHILD), templateJSON, printTemplate, "Template after adding image");
		template = (Template) JSONUtils.readValue(templateJSON, Template.class);

		// add empty paragraph
		templateJSON = addEmptyParagraph(topContainerId, templateJSON, printTemplate);
		template = (Template) JSONUtils.readValue(templateJSON, Template.class);

		// add paragraph description
		paragraphElement = createElementBean(null, TemplateConstants.ELEMENT_PARAGPRAPH, null, null, null, null, null);
		paragraphElement.setId(null);
		paragraphElement.addProperty("paragraph alignment", "center");
		templateJSON = callAPI(buildCommand(TemplateConstants.OPERATION_ADD, paragraphElement, topContainerId, TemplateConstants.LOCATION_CHILD), templateJSON, printTemplate, "Template after adding title paragraph");
		template = (Template) JSONUtils.readValue(templateJSON, Template.class);

		// add title
		TemplateElement textElement = createElementBean(null, TemplateConstants.ELEMENT_TEXT, null, topContainerId, null, "rss/channel/title", null);
		textElement.setId(null);
		textElement.setContext(topContainerId);
		textElement.setContent("rss/channel/description");
		textElement.addProperty("italic", "true");
		templateJSON = callAPI(buildCommand(TemplateConstants.OPERATION_ADD, textElement, template.getLastActedElement().getId(), TemplateConstants.LOCATION_CHILD), templateJSON, printTemplate, "Template after adding title text");
		template = (Template) JSONUtils.readValue(templateJSON, Template.class);

		// add empty paragraph
		templateJSON = addEmptyParagraph(topContainerId, templateJSON, printTemplate);
		template = (Template) JSONUtils.readValue(templateJSON, Template.class);

		// add empty paragraph
		templateJSON = addEmptyParagraph(topContainerId, templateJSON, printTemplate);
		template = (Template) JSONUtils.readValue(templateJSON, Template.class);

		containerElement = createElementBean(null, TemplateConstants.ELEMENT_CONTAINER, null, null, null, null, null);
		containerElement.setId(null);
		containerElement.setContext(topContainerId);
		containerElement.setQuery("rss/channel/item");
		containerElement.setSchema("RSS");
		containerElement.addProperty(TemplateConstants.QUERY_LIMIT, "25");
		templateJSON = callAPI(buildCommand(TemplateConstants.OPERATION_ADD, containerElement, topContainerId, TemplateConstants.LOCATION_CHILD), templateJSON, printTemplate, "Template after adding container");
		template = (Template) JSONUtils.readValue(templateJSON, Template.class);
		String itemContainerId = template.getLastActedElement().getId();

		paragraphElement = createElementBean(null, TemplateConstants.ELEMENT_PARAGPRAPH, null, null, null, null, null);
		paragraphElement.setId(null);
		paragraphElement.addProperty(TemplateConstants.STYLE_NAME, "InternalHyperlink");
		templateJSON = callAPI(buildCommand(TemplateConstants.OPERATION_ADD, paragraphElement, itemContainerId, TemplateConstants.LOCATION_CHILD), templateJSON, printTemplate, "Template after adding title paragraph");
		template = (Template) JSONUtils.readValue(templateJSON, Template.class);

		// add hyperlink
		TemplateElement hyperlinkElement = createElementBean(null, TemplateConstants.ELEMENT_HYPERLINK, null, itemContainerId, null, null, null);
		hyperlinkElement.setId(null);
		hyperlinkElement.setContent("rss/channel/item/link");
		hyperlinkElement.addProperty(TemplateConstants.HYPERLINK_DISPLAY, "rss/channel/item/title");
		templateJSON = callAPI(buildCommand(TemplateConstants.OPERATION_ADD, hyperlinkElement, template.getLastActedElement().getId(), TemplateConstants.LOCATION_CHILD), templateJSON, printTemplate, "Template after adding image");
		template = (Template) JSONUtils.readValue(templateJSON, Template.class);

		// add paragraph for item
		paragraphElement = createElementBean(null, TemplateConstants.ELEMENT_PARAGPRAPH, null, null, null, null, null);
		paragraphElement.setId(null);
		templateJSON = callAPI(buildCommand(TemplateConstants.OPERATION_ADD, paragraphElement, itemContainerId, TemplateConstants.LOCATION_CHILD), templateJSON, printTemplate, "Template after adding title paragraph");
		template = (Template) JSONUtils.readValue(templateJSON, Template.class);
		String paraId = template.getLastActedElement().getId();

		textElement = createElementBean(null, TemplateConstants.ELEMENT_TEXT, null, null, null, "rss/channel/title", null);
		textElement.setId(null);
		textElement.setContext(itemContainerId);
		textElement.setContent("rss/channel/item/pubDate");
		textElement.addProperty("bold", "true");
		templateJSON = callAPI(buildCommand(TemplateConstants.OPERATION_ADD, textElement, paraId, TemplateConstants.LOCATION_CHILD), templateJSON, printTemplate, "Template after adding title text");
		template = (Template) JSONUtils.readValue(templateJSON, Template.class);

		textElement = createElementBean(null, TemplateConstants.ELEMENT_TEXT, null, topContainerId, null, "rss/channel/title", null);
		textElement.setId(null);
		textElement.setContent(" ");
		templateJSON = callAPI(buildCommand(TemplateConstants.OPERATION_ADD, textElement, paraId, TemplateConstants.LOCATION_CHILD), templateJSON, printTemplate, "Template after adding title text");
		template = (Template) JSONUtils.readValue(templateJSON, Template.class);

		textElement = createElementBean(null, TemplateConstants.ELEMENT_TEXT, null, topContainerId, null, "rss/channel/title", null);
		textElement.setId(null);
		textElement.setContext(itemContainerId);
		textElement.setContent("rss/channel/item/description");
		templateJSON = callAPI(buildCommand(TemplateConstants.OPERATION_ADD, textElement, paraId, TemplateConstants.LOCATION_CHILD), templateJSON, printTemplate, "Template after adding title text");
		template = (Template) JSONUtils.readValue(templateJSON, Template.class);

		// add empty paragraph
		templateJSON = addEmptyParagraph(itemContainerId, templateJSON, printTemplate);
		template = (Template) JSONUtils.readValue(templateJSON, Template.class);

		// add empty paragraph
		templateJSON = addEmptyParagraph(itemContainerId, templateJSON, printTemplate);
		template = (Template) JSONUtils.readValue(templateJSON, Template.class);

		return templateJSON;
	}

	private String addEmptyParagraph(String locationId, String templateJSON, boolean printTemplate) throws IOException
	{
		// add empty paragraph
		TemplateElement paragraphElement = createElementBean(null, TemplateConstants.ELEMENT_PARAGPRAPH, null, null, null, null, null);
		paragraphElement.setId(null);
		return callAPI(buildCommand(TemplateConstants.OPERATION_ADD, paragraphElement, locationId, TemplateConstants.LOCATION_CHILD), templateJSON, printTemplate, "Template after adding title paragraph");
	}

	private String buildReqProTemplate(String xmlUrl, String imageUrl, boolean printTemplate) throws Exception
	{
		Template template = createInitialTemplate("Generic XML", "reqpro", info.getURL() + "/utils/xmltoxsd?url=" + xmlUrl, null);
		String templateJSON = JSONUtils.writeValue(template);

		// add top container
		TemplateElement containerElement = createElementBean("top container", TemplateConstants.ELEMENT_CONTAINER, null, null, null, null, null);
		containerElement.setId(null);

		templateJSON = callAPI(buildCommand(TemplateConstants.OPERATION_ADD, containerElement, null, null), templateJSON, printTemplate, "Template after adding container");
		template = (Template) JSONUtils.readValue(templateJSON, Template.class);
		String containerId = template.getLastActedElement().getId();

		// add paragraph title
		TemplateElement paragraphElement = createElementBean(null, TemplateConstants.ELEMENT_PARAGPRAPH, null, null, null, null, null);
		paragraphElement.setId(null);
		paragraphElement.addProperty(TemplateConstants.STYLE_NAME, "Title");
		templateJSON = callAPI(buildCommand(TemplateConstants.OPERATION_ADD, paragraphElement, containerId, TemplateConstants.LOCATION_CHILD), templateJSON, printTemplate, "Template after adding title paragraph");
		template = (Template) JSONUtils.readValue(templateJSON, Template.class);

		// add title
		TemplateElement titleElement = createElementBean(null, TemplateConstants.ELEMENT_TEXT, null, null, null, "Sample Requirements", null);
		titleElement.setId(null);
		templateJSON = callAPI(buildCommand(TemplateConstants.OPERATION_ADD, titleElement, template.getLastActedElement().getId(), TemplateConstants.LOCATION_CHILD), templateJSON, printTemplate, "Template after adding title text");
		template = (Template) JSONUtils.readValue(templateJSON, Template.class);

		// add image
		{
			// add container for image
			containerElement = createElementBean(null, TemplateConstants.ELEMENT_CONTAINER, null, null, null, null, null);
			containerElement.setId(null);
			templateJSON = callAPI(buildCommand(TemplateConstants.OPERATION_ADD, containerElement, containerId, TemplateConstants.LOCATION_CHILD), templateJSON, printTemplate, "Template after adding container for image");
			template = (Template) JSONUtils.readValue(templateJSON, Template.class);

			// add image
			TemplateElement imageElement = createElementBean(null, TemplateConstants.ELEMENT_IMAGE, null, null, null, "Sample Requirements", null);
			imageElement.setId(null);
			imageElement.setContent(imageUrl);
			templateJSON = callAPI(buildCommand(TemplateConstants.OPERATION_ADD, imageElement, template.getLastActedElement().getId(), TemplateConstants.LOCATION_CHILD), templateJSON, printTemplate, "Template after adding image");
			template = (Template) JSONUtils.readValue(templateJSON, Template.class);
		}

		// add page break
		{
			// add container for image
			TemplateElement breakElement = createElementBean(null, TemplateConstants.ELEMENT_PAGE_BREAK, null, null, null, null, null);
			breakElement.setId(null);
			templateJSON = callAPI(buildCommand(TemplateConstants.OPERATION_ADD, breakElement, containerId, TemplateConstants.LOCATION_CHILD), templateJSON, printTemplate, "Template after adding page break");
			template = (Template) JSONUtils.readValue(templateJSON, Template.class);
		}

		// add table of contents
		{
			paragraphElement = createElementBean(null, TemplateConstants.ELEMENT_PARAGPRAPH, null, null, null, null, null);
			paragraphElement.setId(null);
			templateJSON = callAPI(buildCommand(TemplateConstants.OPERATION_ADD, paragraphElement, containerId, TemplateConstants.LOCATION_CHILD), templateJSON, printTemplate, "Template after adding toc paragraph");
			template = (Template) JSONUtils.readValue(templateJSON, Template.class);

			// add toc
			TemplateElement tocElement = createElementBean(null, TemplateConstants.ELEMENT_TABLE_OF_CONTENTS, null, null, null, null, null);
			tocElement.setId(null);
			templateJSON = callAPI(buildCommand(TemplateConstants.OPERATION_ADD, tocElement, template.getLastActedElement().getId(), TemplateConstants.LOCATION_CHILD), templateJSON, printTemplate, "Template after adding toc");
			template = (Template) JSONUtils.readValue(templateJSON, Template.class);
		}

		// add table
		TemplateElement tableElement = createElementBean(null, TemplateConstants.ELEMENT_TABLE, null, null, null, null, null);
		tableElement.setId(null);
		templateJSON = callAPI(buildCommand(TemplateConstants.OPERATION_ADD, tableElement, containerId, TemplateConstants.LOCATION_CHILD), templateJSON, printTemplate, "Template after adding table");
		template = (Template) JSONUtils.readValue(templateJSON, Template.class);
		String tableId = template.getLastActedElement().getId();

		// add header row
		TemplateElement rowElement = createElementBean(null, TemplateConstants.ELEMENT_ROW, null, null, null, null, null);
		rowElement.setId(null);
		templateJSON = callAPI(buildCommand(TemplateConstants.OPERATION_ADD, rowElement, tableId, TemplateConstants.LOCATION_CHILD), templateJSON, printTemplate, "Template after adding header row");
		template = (Template) JSONUtils.readValue(templateJSON, Template.class);
		String rowId = template.getLastActedElement().getId();

		// add header cell
		TemplateElement cellElement = createElementBean(null, TemplateConstants.ELEMENT_CELL, null, null, null, null, null);
		cellElement.setId(null);
		cellElement.addProperty(TemplateConstants.STYLE_NAME, "Intense Emphasis");
		templateJSON = callAPI(buildCommand(TemplateConstants.OPERATION_ADD, cellElement, rowId, TemplateConstants.LOCATION_CHILD), templateJSON, printTemplate, "Template after adding cell");
		template = (Template) JSONUtils.readValue(templateJSON, Template.class);

		// add text
		TemplateElement textElement = createElementBean(null, TemplateConstants.ELEMENT_TEXT, null, null, null, null, null);
		textElement.setId(null);
		textElement.setContent("Full Tag");
		templateJSON = callAPI(buildCommand(TemplateConstants.OPERATION_ADD, textElement, template.getLastActedElement().getId(), TemplateConstants.LOCATION_CHILD), templateJSON, printTemplate, "Template after adding cell");
		template = (Template) JSONUtils.readValue(templateJSON, Template.class);

		// add header cell
		cellElement = createElementBean(null, TemplateConstants.ELEMENT_CELL, null, null, null, null, null);
		cellElement.setId(null);
		cellElement.addProperty(TemplateConstants.STYLE_NAME, "Intense Emphasis");
		templateJSON = callAPI(buildCommand(TemplateConstants.OPERATION_ADD, cellElement, rowId, TemplateConstants.LOCATION_CHILD), templateJSON, printTemplate, "Template after adding cell");
		template = (Template) JSONUtils.readValue(templateJSON, Template.class);

		// add text
		textElement = createElementBean(null, TemplateConstants.ELEMENT_TEXT, null, null, null, null, null);
		textElement.setId(null);
		textElement.setContent("Text");
		templateJSON = callAPI(buildCommand(TemplateConstants.OPERATION_ADD, textElement, template.getLastActedElement().getId(), TemplateConstants.LOCATION_CHILD), templateJSON, printTemplate, "Template after adding cell");
		template = (Template) JSONUtils.readValue(templateJSON, Template.class);

		// add header cell
		cellElement = createElementBean(null, TemplateConstants.ELEMENT_CELL, null, null, null, null, null);
		cellElement.setId(null);
		cellElement.addProperty(TemplateConstants.STYLE_NAME, "Intense Emphasis");
		templateJSON = callAPI(buildCommand(TemplateConstants.OPERATION_ADD, cellElement, rowId, TemplateConstants.LOCATION_CHILD), templateJSON, printTemplate, "Template after adding cell");
		template = (Template) JSONUtils.readValue(templateJSON, Template.class);

		// add text
		textElement = createElementBean(null, TemplateConstants.ELEMENT_TEXT, null, null, null, null, null);
		textElement.setId(null);
		textElement.setContent("GUID");
		templateJSON = callAPI(buildCommand(TemplateConstants.OPERATION_ADD, textElement, template.getLastActedElement().getId(), TemplateConstants.LOCATION_CHILD), templateJSON, printTemplate, "Template after adding cell");
		template = (Template) JSONUtils.readValue(templateJSON, Template.class);

		// add data row
		rowElement = createElementBean(null, TemplateConstants.ELEMENT_ROW, null, null, null, null, null);
		rowElement.setId(null);
		rowElement.setSchema("reqpro");
		rowElement.setQuery("Project/Requirements/PRRequirement");
		templateJSON = callAPI(buildCommand(TemplateConstants.OPERATION_ADD, rowElement, rowId, TemplateConstants.LOCATION_AFTER), templateJSON, printTemplate, "Template after adding data row");
		template = (Template) JSONUtils.readValue(templateJSON, Template.class);
		rowId = template.getLastActedElement().getId();

		// add data cell
		cellElement = createElementBean(null, TemplateConstants.ELEMENT_CELL, null, null, null, null, null);
		cellElement.setId(null);
		templateJSON = callAPI(buildCommand(TemplateConstants.OPERATION_ADD, cellElement, rowId, TemplateConstants.LOCATION_CHILD), templateJSON, printTemplate, "Template after adding data cell");
		template = (Template) JSONUtils.readValue(templateJSON, Template.class);

		// add query text
		textElement = createElementBean(null, TemplateConstants.ELEMENT_TEXT, null, null, null, null, null);
		textElement.setId(null);
		textElement.setContent("Project/Requirements/PRRequirement/FullTag");
		textElement.setContext(rowId);
		templateJSON = callAPI(buildCommand(TemplateConstants.OPERATION_ADD, textElement, template.getLastActedElement().getId(), TemplateConstants.LOCATION_CHILD), templateJSON, printTemplate, "Template after adding cell");
		template = (Template) JSONUtils.readValue(templateJSON, Template.class);

		// add data cell
		cellElement = createElementBean(null, TemplateConstants.ELEMENT_CELL, null, null, null, null, null);
		cellElement.setId(null);
		templateJSON = callAPI(buildCommand(TemplateConstants.OPERATION_ADD, cellElement, rowId, TemplateConstants.LOCATION_CHILD), templateJSON, printTemplate, "Template after adding data cell");
		template = (Template) JSONUtils.readValue(templateJSON, Template.class);

		// add query text
		textElement = createElementBean(null, TemplateConstants.ELEMENT_TEXT, null, null, null, null, null);
		textElement.setId(null);
		textElement.setContent("Project/Requirements/PRRequirement/Text");
		textElement.setContext(rowId);
		templateJSON = callAPI(buildCommand(TemplateConstants.OPERATION_ADD, textElement, template.getLastActedElement().getId(), TemplateConstants.LOCATION_CHILD), templateJSON, printTemplate, "Template after adding cell");
		template = (Template) JSONUtils.readValue(templateJSON, Template.class);

		// add data cell
		cellElement = createElementBean(null, TemplateConstants.ELEMENT_CELL, null, null, null, null, null);
		cellElement.setId(null);
		templateJSON = callAPI(buildCommand(TemplateConstants.OPERATION_ADD, cellElement, rowId, TemplateConstants.LOCATION_CHILD), templateJSON, printTemplate, "Template after adding data cell");
		template = (Template) JSONUtils.readValue(templateJSON, Template.class);

		// add query text
		textElement = createElementBean(null, TemplateConstants.ELEMENT_TEXT, null, null, null, null, null);
		textElement.setId(null);
		textElement.setContent("Project/Requirements/PRRequirement/GUID");
		textElement.setContext(rowId);
		templateJSON = callAPI(buildCommand(TemplateConstants.OPERATION_ADD, textElement, template.getLastActedElement().getId(), TemplateConstants.LOCATION_CHILD), templateJSON, printTemplate, "Template after adding cell");
		template = (Template) JSONUtils.readValue(templateJSON, Template.class);

		// add hyperlink
		{
			String hyperlinkContainerId = null;
			// add container for image
			containerElement = createElementBean(null, TemplateConstants.ELEMENT_CONTAINER, null, null, null, null, null);
			containerElement.setId(null);
			containerElement.setSchema("reqpro");
			containerElement.setQuery("Project/Requirements/PRRequirement");
			templateJSON = callAPI(buildCommand(TemplateConstants.OPERATION_ADD, containerElement, containerId, TemplateConstants.LOCATION_CHILD), templateJSON, printTemplate, "Template after adding container for hyperlink");
			template = (Template) JSONUtils.readValue(templateJSON, Template.class);
			hyperlinkContainerId = template.getLastActedElement().getId();

			// add paragraph
			paragraphElement = createElementBean(null, TemplateConstants.ELEMENT_PARAGPRAPH, null, null, null, null, null);
			paragraphElement.setId(null);
			templateJSON = callAPI(buildCommand(TemplateConstants.OPERATION_ADD, paragraphElement, hyperlinkContainerId, TemplateConstants.LOCATION_CHILD), templateJSON, printTemplate, "Template after adding toc paragraph");
			template = (Template) JSONUtils.readValue(templateJSON, Template.class);

			// add hyperlink
			TemplateElement hyperlinkElement = createElementBean(null, TemplateConstants.ELEMENT_HYPERLINK, null, hyperlinkContainerId, null, null, null);
			hyperlinkElement.setId(null);
			hyperlinkElement.setContent("Project/Requirements/PRRequirement/href");
			hyperlinkElement.addProperty(TemplateConstants.HYPERLINK_DISPLAY, "Project/Requirements/PRRequirement/FullTag");
			hyperlinkElement.addProperty(TemplateConstants.STYLE_NAME, "hyperlink");
			templateJSON = callAPI(buildCommand(TemplateConstants.OPERATION_ADD, hyperlinkElement, template.getLastActedElement().getId(), TemplateConstants.LOCATION_CHILD), templateJSON, printTemplate, "Template after adding image");
			template = (Template) JSONUtils.readValue(templateJSON, Template.class);
		}

		return templateJSON;
	}

	private Template
			createInitialTemplate(String schemaType, String schemaName, String schemaUrl, String containerQuery)
					throws IOException
	{
		String templateJSON = null;

		TemplateSchema schema = createSchemaBean(schemaName, schemaType, null, schemaUrl, null);
		schema.setId(null);

		String commandJson = null;
		if (!CommonUtils.isNullOrEmpty(containerQuery))
		{
			TemplateElement element = createElementBean("top container", TemplateConstants.ELEMENT_CONTAINER, null, null, containerQuery, null, schema.getName());
			element.setId(null);

			Operation operation = new Operation(TemplateConstants.OPERATION_ADD, element);
			commandJson = JSONUtils.writeValue(operation);
		}

		Response response = changeTemplate(null, JSONUtils.writeValue(schema), commandJson);

		checkResponse(response);

		templateJSON = response.readEntity(String.class);

		return (Template) JSONUtils.readValue(templateJSON, Template.class);
	}

	private String callAPI(Operation operation, String templateJson, boolean printResponse, String prefix)
			throws IOException
	{
		Response response = changeTemplate(templateJson, null, JSONUtils.writeValue(operation));

		checkResponse(response);
		String returnJson = response.readEntity(String.class);
		if (printResponse)
		{
			if (!CommonUtils.isNullOrEmpty(prefix))
			{
				System.out.print(prefix + ": ");
			}
			System.out.println(returnJson);
		}

		return returnJson;
	}

	private Operation buildCommand(String opType, TemplateElement element, String locationId, String location)
	{
		Operation operation = new Operation(opType, element);
		operation.setLocation(location);
		operation.setLocationId(locationId);

		return operation;
	}

	private Response changeTemplate(String templateJson, String schemaJson, String commandJson) throws IOException
	{
		Form form = new Form();

		form.param(Parameters.Form.TEMPLATE, templateJson);
		form.param(Parameters.Form.COMMAND_JSON, commandJson);
		form.param(Parameters.Form.SCHEMA, schemaJson);

		WebTarget service = client.target(UriBuilder.fromUri(info.getURL()).build());

		return service.path("template").path("change").request(MediaType.APPLICATION_JSON).post(Entity.entity(form,MediaType.APPLICATION_FORM_URLENCODED_TYPE));
	}

	private TemplateSchema createSchemaBean(String name, String type, String description, String url, String id)
	{
		TemplateSchema schema = new TemplateSchema(name, type, description);
		schema.setUri(url);
		if (id != null)
		{
			schema.setId(id);
		}
		return schema;
	}

	private TemplateElement createElementBean(String name, String type, String id, String context, String query,
			String content, String schema)
	{
		TemplateElement element = new TemplateElement(name, type, "");
		if (id != null)
		{
			element.setId(id);
		}

		element.setContent(content);
		element.setContext(context);
		element.setQuery(query);
		element.setSchema(schema);

		return element;
	}

	public static boolean isRssFeed(String xmlUrl)
	{
		boolean isRss = true;

		if (xmlUrl != null && xmlUrl.toLowerCase().indexOf("requisitepro") != -1)
		{
			isRss = false;
		}

		return isRss;
	}
}
