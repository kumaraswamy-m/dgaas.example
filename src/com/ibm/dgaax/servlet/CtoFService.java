package com.ibm.dgaax.servlet;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;

@Path("/ctof")
@Api( value = "/ctof", description = "Celsius to Fahrenheit")
public class CtoFService {
	
	@GET
	@Produces("text/plain")
	@ApiOperation(value = "Convert Celsius to Fahrenheit", notes = "More notes about this method", response = String.class)
	@ApiResponses(value = {
	  @ApiResponse(code = 400, message = "Invalid value") 
	})
	public String convertCtoF( @ApiParam(value = "Value in Celsius", required = true) @QueryParam(value="value")  double celsius) 
	{
		return Double.toString( ((celsius * 9) / 5) + 32);
	}
}
