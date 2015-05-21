/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2014. All Rights Reserved.
 * 
 * Note to U.S. Government Users Restricted Rights:
 * Use, duplication or disclosure restricted by GSA ADP Schedule
 * Contract with IBM Corp.
 *******************************************************************************/

//var baseURL = null;
//var jobsURL = null;
//var resultsURL = null;
var progressVal = 0;

String.prototype.capitalize = function() {
    return this.charAt(0).toUpperCase() + this.slice(1);
};

function monitorReport( jobURL)
{
	progressVal = (progressVal + 1)%5;
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
			if ( job.status.toUpperCase() == "FINISHED")
			{
			    $( "#progressbar" ).progressbar({value: 5});
			    
		   		$( "#download_result").attr('href', "/api/result/" + job.results[0].uri);
			   
				$( "#results_dialog" ).dialog({
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
			else if ( job.status.toUpperCase() == "FAILED" || job.status.toUpperCase() == "ERROR")
			{
				$( "#progresstext" ).html( "An error has ocurred. Please try again.");
				$( "#progressbar" ).progressbar({value: 0});
			}
			else
			{
				setTimeout( function(){ monitorReport( jobURL);}, 1000);
			}
		},
		
		error: function(error, status)
		{
			console.error("Status is: " + status);
			console.error( JSON.stringify(error));
			
			alert( "Failed to read  job data. Status is: " + status);
		}
	});
}

function runReport( rssURL)
{
	$.ajax({
		type: "POST",
		url: "/api/rss2pdf?rss="+rssURL,
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

			setTimeout( function(){ monitorReport( "/api/job/" +job.id);}, 1000);
		},
		error: function(error, status)
		{
			$( "#progresstext").html("Failed. Error is: " + error.responseText);
			console.error("Status is: " + status);
			console.error(error);
		}
	});
}


function generateDocument() {

	var rssURL = document.getElementById("rss").value;
	$( "#progresstext").html("Starting ...");
	runReport( rssURL);
}


function initialize() 
{
	//baseURL = document.getElementById("dgaasx.js").src;
	//baseURL = baseURL.substr( 0, baseURL.indexOf("/dgaasx.js"));
	

	//jobsURL = baseURL + "/api/job";
	//resultsURL = baseURL + "/api/result";
	
	$( "#generate").click(function() { generateDocument();});
	$( "#progressbar" ).progressbar({value: 0,max:5});
}

