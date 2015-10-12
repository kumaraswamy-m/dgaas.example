/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2015. All Rights Reserved.
 * 
 * Note to U.S. Government Users Restricted Rights:
 * Use, duplication or disclosure restricted by GSA ADP Schedule
 * Contract with IBM Corp. 
 *******************************************************************************/
package com.ibm.dgaasx.utils;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class CommonUtils
{
	public static boolean isNullOrEmpty(String value)
	{
		return value == null || value.isEmpty();
	}

	public static boolean isNullOrEmpty(List<?> list)
	{
		return list == null || list.isEmpty();
	}

	public static boolean isNullOrEmpty(Map<String, String> value)
	{
		return value == null || value.isEmpty();
	}

	private static String getCommaSeparatedValue(Iterator<String> it)
	{
		boolean first = true;
		StringBuffer result = new StringBuffer();

		while (it.hasNext())
		{
			if (first)
			{
				first = false;
			}
			else
			{
				result.append(","); //$NON-NLS-1$
			}

			result.append(it.next());
		}

		return result.toString();
	}

	public static String getCommaSeparatedValue(List<String> list)
	{
		if (isNullOrEmpty(list))
		{
			return null;
		}

		return getCommaSeparatedValue(list.iterator());
	}

	public static String getCommaSeparatedValue(Map<String, String> dataMap, String delimiter, String assigner)
	{
		if (dataMap == null || dataMap.size() == 0)
		{
			return null;
		}

		StringBuffer buf = new StringBuffer(""); //$NON-NLS-1$
		for (Map.Entry<String, String> entry : dataMap.entrySet())
		{
			buf.append(entry.getKey()).append(assigner).append(entry.getValue()).append(delimiter);
		}

		String value = buf.toString();
		if (!value.isEmpty())
		{
			value = value.substring(0, value.length() - 1);
		}
		else
		{
			value = null;
		}

		return value;
	}

	public static int parseInt(String prop, int defValue)
	{
		if (prop == null)
		{
			return defValue;
		}

		try
		{
			return Integer.parseInt(prop);
		}
		catch (Exception e)
		{
			return defValue;
		}
	}

}
