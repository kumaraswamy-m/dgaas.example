/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2015. All Rights Reserved.
 * 
 * Note to U.S. Government Users Restricted Rights:
 * Use, duplication or disclosure restricted by GSA ADP Schedule
 * Contract with IBM Corp.
 *******************************************************************************/

// *********************************
// change history
// *********************************
// Early 2015	: Generate PDF
// October 2015	: Generate Template
// *********************************

//var baseURL = null;
//var jobsURL = null;
//var resultsURL = null;
var progressVal = 0;
var progressMax = 5;
var templateMonitor = null;

String.prototype.capitalize = function() {
    return this.charAt(0).toUpperCase() + this.slice(1);
};

function getServiceUrl(serviceType) {
	var url = location.href;
	if(url.endsWith('/')) {
		url += 'api/' + serviceType;
	} else {
		url += '/api/' + serviceType;
	}
	
	return url;
}

function monitorReport(jobURL, templateURL)
{
	progressVal = (progressVal + 1) % progressMax;
	$( "#progressbar" ).progressbar({value: progressVal});
	
	$.ajax({
		type: "GET",
		url: jobURL,
		xhrFields: {
			 withCredentials: true
		},
		dataType: "json",
		success: function (job) {
			$( "#progresstext").html(job.status.capitalize() + "...");
			if (job.status.toUpperCase() == "FINISHED")
			{
				if(templateURL) {
					$('#download_template_pdf').attr('href', getServiceUrl("result/") + job.results[0].uri);
					openSuccessDialog('#results_dialog_template', '#download_template', templateURL);
				} else {
					openSuccessDialog('#results_dialog_pdf', '#download_pdf', getServiceUrl("result/") + job.results[0].uri);
				}
				
				enableDisableButton('#generate_template', true);
				enableDisableButton('#generate_pdf', true);
			}
			else if ( job.status.toUpperCase() == "FAILED" || job.status.toUpperCase() == "ERROR")
			{
				$( "#progresstext" ).html( "An error has ocurred. Please try again.");
				$( "#progressbar" ).progressbar({value: 0});
				
				enableDisableButton('#generate_template', true);
				enableDisableButton('#generate_pdf', true);
			}
			else
			{
				setTimeout( function(){ monitorReport(jobURL, templateURL);}, 1000);
			}
		},
		
		error: function(error, status)
		{
			console.error("Status is: " + status);
			console.error( JSON.stringify(error));
			
			alert( "Failed to read  job data. Status is: " + status);
			enableDisableButton('#generate_template', true);
			enableDisableButton('#generate_pdf', true);
		}
	});
}

function runReport(rssURL, templateURL)
{
	$( "#progresstext").html("Generating document...");
	var url = getServiceUrl("rss2pdf") + "?rss=" + rssURL;
	if(templateURL) {
		url += '&templateUrl=' + templateURL;
	}
	$.ajax({
		type: "POST",
		url:  url,
		xhrFields: {
			 withCredentials: true
		},
		data : {},
		dataType: "json",
		success: function (job) {
			if(typeof job !== "object") {
				console.error("Data returned is in invalid format.");
				return;
			}

			setTimeout( function(){ monitorReport(getServiceUrl("job/") +job.id, templateURL);}, 1000);
		},
		error: function(error, status)
		{
			enableDisableButton('#generate_template', true);
			enableDisableButton('#generate_pdf', true);
			$( "#progresstext").html("Failed. Error is: " + error.responseText);
			console.error("Status is: " + status);
			console.error(error);
		}
	});
}

function waitForTemplateGeneration() {
	if(templateMonitor) {
		progressVal = (progressVal + 1) % progressMax;
		$( "#progressbar" ).progressbar({value: progressVal});
		setTimeout( function(){ waitForTemplateGeneration();}, 1000);
	}
}

function generateTemplateImpl(xmlUrl) {
	$( "#progresstext").html("Building template...");
	templateMonitor = setTimeout( function(){ waitForTemplateGeneration();}, 1000);
	$.ajax({
		type: "POST",
		url: getServiceUrl("rss2template") + '/json' + "?url="+xmlUrl,
		xhrFields: {
			 withCredentials: true
		},
		data : {},
		dataType: "json",
		success: function (template) {
			if(typeof template !== "object") {
				console.error("Data returned is in invalid format.");
				return;
			}
			
			templateMonitor = null;
			
			createTemplateDta(JSON.stringify(template), xmlUrl);
		},
		error: function(error, status)
		{
			templateMonitor = null;
			enableDisableButton('#generate_template', true);
			enableDisableButton('#generate_pdf', true);
			$("#progresstext").html("Failed. Error is: " + error.responseText);
			console.error("Status is: " + status);
			console.error(error);
		}
	});
}

function createTemplateDta(templateJson, xmlUrl) {
	$( "#progresstext").html("Generating template...");
	templateMonitor = setTimeout( function(){ waitForTemplateGeneration();}, 1000);
	
	$.ajax({
		type: "POST",
		url: getServiceUrl("rss2template") + '/dta',
		xhrFields: {
			 withCredentials: true
		},
		dataType: "text",
		data : {template: templateJson},
		success: function (templateId) {
			templateMonitor = null;
			runReport(xmlUrl, getServiceUrl("rss2template/") + templateId);
			
			enableDisableButton('#generate_template', true);
			enableDisableButton('#generate_pdf', true);
		},
		error: function(error, status)
		{
			templateMonitor = null;
			enableDisableButton('#generate_template', true);
			enableDisableButton('#generate_pdf', true);
			$("#progresstext").html("Failed. Error is: " + error.responseText);
			console.error("Status is: " + status);
			console.error(error);
		}
	});
}

function openSuccessDialog(dialogId, linkId, url)
{
	$("#progressbar" ).progressbar({value: progressMax});
    
	$(linkId).attr('href', url);
	
	$(dialogId).dialog({
		modal:true,
		dialogClass: "dialogWithDropShadow",
		buttons: [
			{
				text: "OK",
				click: function() {
					$( this ).dialog( "close" );
					$( "#progresstext" ).html( "Ready ...");
					$( "#progressbar" ).progressbar({value: 0});
				}
			}
		]
	}).parent().position({ my: 'center', at: 'center', of: '#centerDiv' });
}

function generateDocument() {
	$( "#progressbar" ).progressbar({value: 0});
	 $("#progressbar > div").css({ 'background': '#B9C9C8' });
	 
	var rssURL = document.getElementById("rss").value;
	$( "#progresstext").html("Starting ...");
	enableDisableButton('#generate_template', false);
	enableDisableButton('#generate_pdf', false);
	runReport(rssURL);
}

function generateTemplate() {
	$( "#progressbar" ).progressbar({value: 0});
	var rssURL = document.getElementById("rss").value;
	enableDisableButton('#generate_pdf', false);
	enableDisableButton('#generate_template', false);
	generateTemplateImpl(rssURL);
}

function enableDisableButton(el, enabled)
{
	$(el).prop('disabled', !enabled);
}

function initialize() 
{
	$("#generate_pdf").click(generateDocument);
	$("#generate_template").click(generateTemplate);
	$("#progressbar").progressbar({value: 0, max: progressMax});
}
