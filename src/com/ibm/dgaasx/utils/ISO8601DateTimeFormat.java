/*******************************************************************************
 * Licensed Materials - Property of IBM
 * (c) Copyright IBM Corporation 2015. All Rights Reserved.
 * 
 * Note to U.S. Government Users Restricted Rights:
 * Use, duplication or disclosure restricted by GSA ADP Schedule
 * Contract with IBM Corp. 
 *******************************************************************************/
package com.ibm.dgaasx.utils;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * ISO8610 date formatter needed as Java 6 cannot produce the format directly
 * @author Spurlos
 *
 */
public class ISO8601DateTimeFormat extends SimpleDateFormat
{
	private static final long serialVersionUID = 6581118229505220070L;

	public ISO8601DateTimeFormat()
	{
		super( "yyyy-MM-dd'T'HH:mm:ssZ"); //$NON-NLS-1$
	}
	
	@Override
	public StringBuffer format(Date date, StringBuffer toAppendTo, java.text.FieldPosition pos)
	{
		StringBuffer toFix = super.format(date, toAppendTo, pos);
		return toFix.insert(toFix.length() - 2, ':');
	};
}
