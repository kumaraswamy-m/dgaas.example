/*******************************************************************************
 * Licensed Materials - Property of IBM
 * © Copyright IBM Corporation 2015. All Rights Reserved.
 * 
 * Note to U.S. Government Users Restricted Rights:
 * Use, duplication or disclosure restricted by GSA ADP Schedule
 * Contract with IBM Corp. 
 *******************************************************************************/
package com.ibm.dgaasx.utils;

public class SystemUtils
{
	/**
	 * Returns the runtime property with the given name. 
	 * 
	 * @param propertyName
	 * @param defaultValue
	 * @return
	 */
	public static String getSystemProperty(String property, String defaultValue)
	{
		String value = null;

		if (CommonUtils.isNullOrEmpty(value))
		{
			value = System.getProperty( property);
		}
		
		if (CommonUtils.isNullOrEmpty(value))
		{
			value = System.getenv(property);
		}

		return value != null ? value : defaultValue;
	}
}
