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
package edu.lium.decoder;

import java.util.HashMap;
import java.util.Map.Entry;
import com.bbn.mt.terp.BLEUcn;

public class NGramToken extends Token
{
	public HashMap<NGram, Integer> all_ngrams = null;
	public HashMap<NGram, Integer> previous_ngrams = null;
	public HashMap<NGram, Integer> new_ngrams = null;
	
	public NGramToken(float score, Token pred, Node node)
	{
		super(score, pred, node, null, null);
		all_ngrams = new HashMap<NGram, Integer>();
		previous_ngrams = new HashMap<NGram, Integer>();
		new_ngrams = new HashMap<NGram, Integer>();
	}
	
	public void addNGram(NGram ngram)
	{
		if(all_ngrams.containsKey(ngram) == false)
			all_ngrams.put(ngram, 1);
		else
			all_ngrams.put(ngram, all_ngrams.get(ngram)+1);
		
		if(new_ngrams.containsKey(ngram) == false)
			new_ngrams.put(ngram, 1);
		/*else
			previous_ngrams.put(ngram, previous_ngrams.get(ngram)+1);*/
	}
	
	public void maybeAddNGram(NGram ngram, HashMap<NGram, Integer> refNgrams)
	{
		if(refNgrams.containsKey(ngram))
		{
			addNGram(ngram);
		}
	}
	
	/**
	 * extends all previous_ngrams and put them in new_ngrams
	 * @param ws
	 */
	public void extendNGrams(String ws)
	{
		//System.err.println("extendNGrams START : "+previous_ngrams);
		for(Entry<NGram, Integer> entry : previous_ngrams.entrySet())
		{
			//extends ngrams
			if(entry.getKey().size() < BLEUcn.max_ngram_size)
			{
				//logger.info("extending ngram "+ngram+" with word "+ws);
				NGram ng = new NGram(entry.getKey(), ws);
				
				if(all_ngrams.containsKey(ng) == false)
					all_ngrams.put(ng, 1);
				else
					all_ngrams.put(ng, all_ngrams.get(ng)+1);
				
				if(new_ngrams.containsKey(ng) == false)
					new_ngrams.put(ng, 1);
				else
					new_ngrams.put(ng, new_ngrams.get(ng)+1); 
			}
		}
		//System.err.println("extendNGrams END ");
	}
	
	public void extendUsefulNGrams(String ws, HashMap<NGram, Integer> refNgrams)
	{
		//System.err.println("extendNGrams START : "+previous_ngrams);
		for(Entry<NGram, Integer> entry : previous_ngrams.entrySet())
		{
			//extends ngrams
			if(entry.getKey().getOrder() < BLEUcn.max_ngram_size)
			{
				//logger.info("extending ngram "+ngram+" with word "+ws);
				NGram ng = new NGram(entry.getKey(), ws);
				maybeAddNGram(ng, refNgrams);
			}
		}
		//System.err.println("extendNGrams END ");
	}
	
	
	/**
	 * 
	 * @param node
	 */
	public void goToNode(Node node)
	{
		this.node = node;
		previous_ngrams = new_ngrams;
		new_ngrams = new HashMap<NGram, Integer>();
	}



	
	
}
