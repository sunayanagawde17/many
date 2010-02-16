/*
 * Copyright 2009 Loic BARRAULT.  
 * Portions Copyright BBN and UMD (see LICENSE_TERP.txt).  
 * Portions Copyright 1999-2008 CMU (see LICENSE_SPHINX4.txt).
 * All Rights Reserved.  Use is subject to license terms.
 * 
 * See the file "LICENSE.txt" for information on usage and
 * redistribution of this file, and for a DISCLAIMER OF ALL 
 * WARRANTIES.
 *
 */
package com.bbn.mt.terp;

import java.util.Iterator;

public abstract class TERutilities
{
	private TERutilities()
	{
	}

	public static String join(String[] s, String delimiter)
	{
		StringBuilder buffer = new StringBuilder();
		for (int i = 0; i < s.length; ++i)
		{
			if (i > 0)
			{
				buffer.append(delimiter);
			}
			buffer.append(s[i]);
		}
		return buffer.toString();
	}

	public static String join(final String delimiter, final Comparable<String>[] objs)
	{
		if (objs == null)
	        return "";
		if (objs.length == 0)
			return "";
		String delim = delimiter;
		if(delimiter == null)
			delim = "";
		
		StringBuilder buffer = new StringBuilder(String.valueOf(objs[0]));
		for(int i=1; i<objs.length; i++)
		{
			buffer.append(delim).append(String.valueOf(objs[i]));
		}
		return buffer.toString();
	}
	
	public static <T> String join(final String delimiter, final Iterable<T> objs)
	{
		if (objs == null)
	        return "";
		Iterator<T> iter = objs.iterator();
		if (!iter.hasNext())
			return "";
		String delim = delimiter;
		if(delimiter == null)
			delim = "";
		StringBuilder buffer = new StringBuilder(String.valueOf(iter.next()));
		while (iter.hasNext())
			buffer.append(delim).append(String.valueOf(iter.next()));
		return buffer.toString();
	}
	
	public static String join(String delimiter, String format, double[] arr)
	{
		if (arr == null)
			return "";
		String delim = delimiter;
		if (delim == null)
			delim = "";
		StringBuilder buffer = new StringBuilder(String.format(format, arr[0]));
		for (int i = 1; i < arr.length; i++)
		{
			buffer.append(delim).append(String.format(format, arr[i]));
		}
		return buffer.toString();
	}
	public static String join(String delimiter, double[] arr)
	{
		if (arr == null)
	        return "";
		if (arr.length == 0)
			return "";
		String delim = delimiter;
		if(delimiter == null)
			delim = "";
		
		StringBuilder buffer = new StringBuilder(String.valueOf(arr[0]));
		for(int i=1; i<arr.length; i++)
		{
			buffer.append(delim).append(String.valueOf(arr[i]));
		}
		return buffer.toString();
	}
	public static String join(String delimiter, int[] arr)
	{
		if (arr == null)
	        return "";
		if (arr.length == 0)
			return "";
		String delim = delimiter;
		if(delimiter == null)
			delim = "";
		
		StringBuilder buffer = new StringBuilder(String.valueOf(arr[0]));
		for(int i=1; i<arr.length; i++)
		{
			buffer.append(delim).append(String.valueOf(arr[i]));
		}
		return buffer.toString();
	}
	public static String join(String delimiter, String format, float[] arr)
	{
		if (arr == null)
			return "";
		String delim = delimiter;
		if (delim == null)
			delim = "";
		StringBuilder buffer = new StringBuilder(String.format(format, arr[0]));
		for (int i = 1; i < arr.length; i++)
		{
			buffer.append(delim).append(String.format(format, arr[i]));
		}
		return buffer.toString();
	}
	public static String join(String delimiter, float[] arr)
	{
		if (arr == null)
	        return "";
		if (arr.length == 0)
			return "";
		String delim = delimiter;
		if(delimiter == null)
			delim = "";
		
		StringBuilder buffer = new StringBuilder(String.valueOf(arr[0]));
		for(int i=1; i<arr.length; i++)
		{
			buffer.append(delim).append(String.valueOf(arr[i]));
		}
		return buffer.toString();
	}
	/*
	public static String join(String delim, List<String> arr)
	{
		if (arr == null)
			return "";
		if (delim == null)
			delim = new String("");
		String s = new String("");
		for (int i = 0; i < arr.size(); i++)
		{
			if (i == 0)
			{
				s += arr.get(i);
			}
			else
			{
				s += delim + arr.get(i);
			}
		}
		return s;
	}*/
	public static String join(String delimiter, char[] arr)
	{
		if (arr == null)
	        return "";
		if (arr.length == 0)
			return "";
		String delim = delimiter;
		if(delimiter == null)
			delim = "";
		
		StringBuilder buffer = new StringBuilder(String.valueOf(arr[0]));
		for(int i=1; i<arr.length; i++)
		{
			buffer.append(delim).append(String.valueOf(arr[i]));
		}
		return buffer.toString();
	}
	
	public static class Index_Score implements Comparable<Index_Score>
	{
		public int index = -1;
		public double score = 0.0;
		private Index_Score(){};
		public Index_Score(int idx, double sc)
		{
			index = idx;
			score = sc;
		}
		//@Override
		public int compareTo(Index_Score o)
		{
			if(score > o.score)
				return 1;
			if(score < o.score)
				return -1;
			return 0;
		}
		
		public String toString()
		{
			return ""+index+" ("+score+")";
		}
		
		
	}

}
