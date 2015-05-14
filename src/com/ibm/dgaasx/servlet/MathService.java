package com.ibm.dgaasx.servlet;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import com.wordnik.swagger.annotations.ApiResponse;
import com.wordnik.swagger.annotations.ApiResponses;

@Path("/add")
@Api(value = "/add", description = "Math functions")
public class MathService
{
	@Path("/sum")
	@GET
	@Produces("text/plain")
	@ApiOperation(value = "Add two numbers", notes = "More notes about this method", response = String.class)
	@ApiResponses(value = { @ApiResponse(code = 400, message = "Invalid value") })
	public String sum(@ApiParam(value = "Left operand", required = true) @QueryParam(value = "left") double left,
					  @ApiParam(value = "Left operand", required = true) @QueryParam(value = "right") double right)
	{
		return Double.toString( left + right);
	}
	
	@Path("/multiply")
	@GET
	@Produces("text/plain")
	@ApiOperation(value = "Multiply two numbers", notes = "More notes about this method", response = String.class)
	@ApiResponses(value = { @ApiResponse(code = 400, message = "Invalid value") })
	public String multiply(@ApiParam(value = "Left operand", required = true) @QueryParam(value = "left") double left,
					  @ApiParam(value = "Left operand", required = true) @QueryParam(value = "right") double right)
	{
		return Double.toString( left * right);
	}
}
