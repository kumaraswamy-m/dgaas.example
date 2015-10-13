# Document Generation as a Service

Example for using the RPE Document Generation Services on bluemix. This example is maintained by the IBM Publishing Engine team.
This instance of the example is running at http://dgaasx_ddc.mybluemix.net/

## How to fork the example
1. On Github 
	1. Fork https://github.com/dgaas/dgaas.example.git

2. On IBM DevOPs for Bluemix
	1. Create a new project https://hub.jazz.net/create
	
	2. Select Link to a Github repository
	
	3. Select the project fork from your Github account
	
	4. Name the newly created project dgaas_example as dgaas.example is not accepted by IBM DevOps for Bluemix
	
## IBM DevOps for Bluemix - Build pipeline
1. Build stage
	1. Create a build stage and a Maven build job
	
	2. Clear the Working Directory and Build Archive Directory properties
	
2. Deploy stage
	1. Create a deploy stage and a deploy job
	2. Ensure the app name is "dgaas_example" even if you used a different name for the project
	
	NOTE: the app name in the deploy stage must match the app name property from manifest.yml.  

3. From the Bluemix Dashboard add a "Document Generation" service to your workspace. The name of the service must be "Document Generation" as used in the manifest.yml file.

	NOTE: you will find the Document Generation service in the Labs area of the Bluemix catalog https://console.ng.bluemix.net/catalog/labs/
	
4. Select a unique name for your deployment of DgaaSX
	1. Edit manifest.yml
	
	2. Change the host property to a unique value ( in the context of mybluemix.net)
	
	3. Commit and push the changes - this will also start the pipeline
	
