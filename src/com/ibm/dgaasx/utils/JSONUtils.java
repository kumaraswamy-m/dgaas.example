/*******************************************************************************
 * Licensed Materials - Property of IBM
 * © Copyright IBM Corporation 2014, 2015. All Rights Reserved.
 * 
 * Note to U.S. Government Users Restricted Rights:
 * Use, duplication or disclosure restricted by GSA ADP Schedule
 * Contract with IBM Corp. 
 *******************************************************************************/
package com.ibm.dgaasx.utils;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Map;
import java.util.SimpleTimeZone;

import com.fasterxml.jackson.databind.ObjectMapper;

public class JSONUtils
{
	public static String toJSON(Object object) throws IOException
	{
		return writeValue(object);
	}

	public static Object fromJSON(String value, Class<?> clazz) throws IOException
	{
		return readValue(value, clazz);
	}

	public static SimpleDateFormat getSDFISO8601()
	{
		SimpleDateFormat sdf = new ISO8601DateTimeFormat();
		sdf.setTimeZone(new SimpleTimeZone(SimpleTimeZone.UTC_TIME, "UTC")); //$NON-NLS-1$
		return sdf;
	}
	
	public static String writeValue(Object object) throws IOException
	{
		ObjectMapper mapper = new ObjectMapper();
		mapper.setDateFormat( getSDFISO8601());
		return mapper.writeValueAsString(object);
	}

	public static Object readValue(String value, Class<?> clazz) throws IOException
	{
		ObjectMapper mapper = new ObjectMapper();
		return mapper.readValue(value, clazz);
	}
	
	@SuppressWarnings("unchecked")
	public static Map<String, String> getMapFromJson(String datajson) throws IOException
	{
		if ( datajson == null)
		{
			return null;
		}

		return (Map<String, String>) JSONUtils.fromJSON(datajson, Map.class);
	}
}
