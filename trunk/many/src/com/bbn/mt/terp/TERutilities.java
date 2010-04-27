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

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
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
	public static String join(String delimiter, Double[] arr)
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
	
	public static String join(String delimiter, String format, Float[] arr)
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
	public static String join(String delimiter, Float[] arr)
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
	
	public static String toCNString(ArrayList<ArrayList<Comparable<String>>> cn,
			ArrayList<ArrayList<Float>> cn_scores)
	{
		StringBuilder sb = new StringBuilder();
		for(int i=0; i<cn.size(); i++)
		{
			ArrayList<Comparable<String>> mesh = cn.get(i);
			ArrayList<Float> sc = cn_scores.get(i);
			sb.append("align "+i+" ");

			for(int j=0; j<mesh.size(); j++)
			{
				sb.append((String)mesh.get(j)).append(" ").append(sc.get(j)).append(" ");
			}	
			sb.append("\n");
		}
		return sb.toString();
	}
	
	public static void generateParams(String paramsFile, String output, String ref, String ref_scores, int ref_idx,
			ArrayList<String> hyps, ArrayList<String> hyps_scores, String[] costs, float[] sysWeights, int[] hyps_idx, 
			String wordnet, String shift_word_stop_list, String paraphrases)
	{
		StringBuilder sb = new StringBuilder();

		sb.append("Reference File (filename)                : ").append(ref);
		sb.append("\nReference Scores File (filename)         : ").append(ref_scores);
		sb.append("\nHypothesis Files (list)                  : ").append(TERutilities.join(" ", hyps));
		sb.append("\nHypothesis Scores Files (list)           : ").append(TERutilities.join(" ", hyps_scores));
		sb.append("\nOutput Prefix (filename)                 : ").append(output);
		sb.append("\nDefault Deletion Cost (float)            : ").append(costs[0]);
		sb.append("\nDefault Stem Cost (float)                : ").append(costs[1]);
		sb.append("\nDefault Synonym Cost (float)             : ").append(costs[2]);
		sb.append("\nDefault Insertion Cost (float)           : ").append(costs[3]);
		sb.append("\nDefault Substitution Cost (float)        : ").append(costs[4]);
		sb.append("\nDefault Match Cost (float)               : ").append(costs[5]);
		sb.append("\nDefault Shift Cost (float)               : ").append(costs[6]);

		sb.append("\nOutput Formats (list)                    : ").append("cn param");
		sb.append("\nCreate confusion Network (boolean)       : ").append("true");
		sb.append("\nUse Porter Stemming (boolean)            : ").append("true");
		sb.append("\nUse WordNet Synonymy (boolean)           : ").append("true");
		sb.append("\nCase Sensitive (boolean)                 : ").append("true");
		// sb.append("\nShift Constraint (string)                : ").append("exact");
		sb.append("\nShift Constraint (string)                : ").append("relax");
		sb.append("\nWordNet Database Directory (filename)    : ").append(wordnet);
		sb.append("\nShift Stop Word List (string)            : ").append(shift_word_stop_list);
		sb.append("\nPhrase Database (filename)               : ").append(paraphrases);

		sb.append("\nReference Index (integer)                : ").append(ref_idx);
		sb.append("\nHypotheses Indexes (integer list)        : ").append(TERutilities.join(" ", hyps_idx));
		sb.append("\nSystems weights (double list)            : ").append(TERutilities.join(" ", sysWeights));

		// PrintStream outWriter = null;
		BufferedWriter outWriter = null;
		if (paramsFile != null)
		{
			try
			{
				// outWriter = new PrintStream(paramsFile, "ISO8859_1");
				// outWriter = new PrintStream(paramsFile, "UTF-8");
				outWriter = new BufferedWriter(new FileWriter(paramsFile));
				outWriter.write(sb.toString());
				outWriter.close();
			}
			catch (IOException ioe)
			{
				System.err.println("I/O erreur durant creation output file " + String.valueOf(paramsFile) + " " + ioe);
			}
		}
	}

	public static void generateParams(String paramsFile, String output, String ref, ArrayList<String> hyps,
			ArrayList<String> hyps_scores, String[] costs, 
			String wordnet, String shift_word_stop_list, String paraphrases)
	{
		StringBuilder sb = new StringBuilder();

		sb.append("Reference File (filename)                : ").append(ref);
		sb.append("\nHypothesis Files (list)                  : ").append(TERutilities.join(" ", hyps));
		sb.append("\nHypothesis Scores Files (list)           : ").append(TERutilities.join(" ", hyps_scores));
		sb.append("\nOutput Prefix (filename)                 : ").append(output);
		sb.append("\nDefault Deletion Cost (float)            : ").append(costs[0]);
		sb.append("\nDefault Stem Cost (float)                : ").append(costs[1]);
		sb.append("\nDefault Synonym Cost (float)             : ").append(costs[2]);
		sb.append("\nDefault Insertion Cost (float)           : ").append(costs[3]);
		sb.append("\nDefault Substitution Cost (float)        : ").append(costs[4]);
		sb.append("\nDefault Match Cost (float)               : ").append(costs[5]);
		sb.append("\nDefault Shift Cost (float)               : ").append(costs[6]);
		sb.append("\nUse Porter Stemming (boolean)            : ").append("true");
		sb.append("\nUse WordNet Synonymy (boolean)           : ").append("true");
		sb.append("\nCase Sensitive (boolean)                 : ").append("true");
		// sb.append("\nShift Constraint (string)                : ").append("exact");
		sb.append("\nShift Constraint (string)                : ").append("relax");
		sb.append("\nWordNet Database Directory (filename)    : ").append(wordnet);
		sb.append("\nShift Stop Word List (string)            : ").append(shift_word_stop_list);
		sb.append("\nPhrase Database (filename)               : ").append(paraphrases);

		BufferedWriter outWriter = null;
		if (paramsFile != null)
		{
			try
			{
				outWriter = new BufferedWriter(new FileWriter(paramsFile));
				outWriter.write(sb.toString());
				outWriter.close();
			}
			catch (IOException ioe)
			{
				System.err.println("I/O erreur durant creation output file " + String.valueOf(paramsFile) + " " + ioe);
			}
		}
	}

	public static void generateParams(String paramsFile, String output, String ref, ArrayList<String> hyps,
			ArrayList<String> hyps_scores, 
			String wordnet, String shift_word_stop_list, String paraphrases)
	{
		StringBuilder sb = new StringBuilder();

		sb.append("Reference File (filename)                : ").append(ref);
		sb.append("\nHypothesis Files (list)                  : ").append(TERutilities.join(" ", hyps));
		sb.append("\nHypothesis Scores Files (list)           : ").append(TERutilities.join(" ", hyps_scores));
		sb.append("\nOutput Prefix (filename)                 : ").append(output);
		sb.append("\nDefault Deletion Cost (float)            : 1.0");
		sb.append("\nDefault Stem Cost (float)                : 1.0");
		sb.append("\nDefault Synonym Cost (float)             : 1.0");
		sb.append("\nDefault Insertion Cost (float)           : 1.0");
		sb.append("\nDefault Substitution Cost (float)        : 1.0");
		sb.append("\nDefault Match Cost (float)               : 0.0");
		sb.append("\nDefault Shift Cost (float)               : 1.0");
		sb.append("\nUse Porter Stemming (boolean)            : ").append("true");
		sb.append("\nUse WordNet Synonymy (boolean)           : ").append("true");
		sb.append("\nCase Sensitive (boolean)                 : ").append("true");
		// sb.append("\nShift Constraint (string)                : ").append("exact");
		sb.append("\nShift Constraint (string)                : ").append("relax");
		sb.append("\nWordNet Database Directory (filename)    : ").append(wordnet);
		sb.append("\nShift Stop Word List (string)            : ").append(shift_word_stop_list);
		sb.append("\nPhrase Database (filename)               : ").append(paraphrases);

		BufferedWriter outWriter = null;
		if (paramsFile != null)
		{
			try
			{
				outWriter = new BufferedWriter(new FileWriter(paramsFile));
				outWriter.write(sb.toString());
				outWriter.close();
			}
			catch (IOException ioe)
			{
				System.err.println("I/O erreur durant creation output file " + String.valueOf(paramsFile) + " " + ioe);
			}
		}
	}
	

}
