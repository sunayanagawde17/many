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

public class Link
{
	Node endNode;
	Node startNode;
	double probability; // probability of the word (not normalized and not posterior)
	double posterior; // variable set by the forward-backward algorithm
	int wordId;
	int id;
	
	public Link(Node from, Node target, double prob, double post, int word, int i)
	{
		this.startNode = from;
		this.endNode = target;
		this.probability = prob;
		this.posterior = post;
		this.wordId = word;
		this.id = i;
		
		from.nextLinks.add(this);
		target.backLinks.add(this);
		
	}
	
	public Link(Node from, Node target, double prob, int word, int i)
	{
		this(from, target, prob, prob, word, i);
	}
    
	public Link(Link l)
	{
		this(l.startNode, l.endNode, l.probability, l.posterior, l.wordId, l.id);
	}
}
