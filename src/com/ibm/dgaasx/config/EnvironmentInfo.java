/*******************************************************************************
 * Licensed Materials - Property of IBM
 * © Copyright IBM Corporation 2015. All Rights Reserved.
 * 
 * Note to U.S. Government Users Restricted Rights:
 * Use, duplication or disclosure restricted by GSA ADP Schedule
 * Contract with IBM Corp. 
 *******************************************************************************/
package com.ibm.dgaasx.config;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ibm.dgaasx.utils.SystemUtils;

@SuppressWarnings("nls")
public class EnvironmentInfo
{
	protected static final Logger log = LoggerFactory.getLogger(EnvironmentInfo.class);

	private static final DGaaSInfo DGAAS_INFO = parseVCAPS();

	public static final DGaaSInfo getDGaaSInfo()
	{
		return DGAAS_INFO;
	}

	public static final DGaaSInfo parseVCAPS()
	{
		DGaaSInfo info = new DGaaSInfo();

		String vcaps = System.getenv("VCAP_SERVICES");

		if (vcaps == null || vcaps.trim().isEmpty())
		{
			String dgaasURL = SystemUtils.getSystemProperty("DGAAS_URL", null);
			info.setURL(dgaasURL == null || dgaasURL.isEmpty() ? "http://localhost:8080/dgaas" : dgaasURL);
			return info;
		}

		JSONObject jsonRoot = new JSONObject(vcaps);
		JSONObject docgenJSON = (JSONObject) jsonRoot.getJSONArray("Document Generation").get(0);
		JSONObject credentialsJSON = docgenJSON.getJSONObject("credentials");

		info.setURL(credentialsJSON.getString("url"));
		info.setInstanceID(credentialsJSON.getString("instanceid"));
		info.setRegion(credentialsJSON.getString("region"));

		return info;
	}
}
