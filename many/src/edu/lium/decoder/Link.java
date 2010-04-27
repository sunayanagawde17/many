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
	public int getWordId(){ return wordId; }
	public Node getEndNode(){ return endNode; }
	public float getPosterior(){ return posterior; }
	
	Node startNode;
	float probability; // probability of the word (not normalized and not posterior)
	float posterior; // variable set by the forward-backward algorithm
	int wordId;
	int id;
	int sysid;
	
	public Link(Node from, Node target, float prob, float post, int word, int sysid, int i)
	{
		this.startNode = from;
		this.endNode = target;
		this.probability = prob;
		this.posterior = post;
		this.wordId = word;
		this.id = i;
		this.sysid = sysid;
		from.nextLinks.add(this);
		target.backLinks.add(this);
		
	}
	
	public Link(Node from, Node target, float prob, float post, int word, int i)
	{
		this(from, target, prob, prob, word, -1, i);
	}
	
	public Link(Node from, Node target, float prob, int word, int i)
	{
		this(from, target, prob, prob, word, i);
	}
    
	public Link(Link l)
	{
		this(l.startNode, l.endNode, l.probability, l.posterior, l.wordId, l.id);
	}
	
	@Override
	public String toString()
	{
		String s = "["+startNode.time+","+endNode.time+" : w="+wordId+", p="+probability+"]";
		return s;
	}
	
	@Override
	public boolean equals(Object obj)
	{
		if((obj instanceof Link) == false)
			return false;
		
		boolean error = false;
		Link l = (Link) obj;
		
		if(probability != l.probability) 
		{
			error = true;
			System.err.println("*** orig.probability="+probability+" vs new.probability"+l.probability+"  [BAD]");
		}
		
		if(posterior != l.posterior) 
		{
			error = true;
			System.err.println("*** orig.posterior="+posterior+" vs new.posterior"+l.posterior+"  [BAD]");
		}
		
		if(wordId != l.wordId)
		{
			error = true;
			System.err.println("*** orig.wordId="+wordId+" vs new.wordId"+l.wordId+"  [BAD]");
		}
		
		if(id != l.id)
		{
			error = true;
			System.err.println("*** orig.id="+id+" vs new.id"+l.id+"  [BAD]");
		}
		
		if(startNode.id != l.startNode.id || startNode.time != l.startNode.time)
			error = true;
		if(endNode.id != l.endNode.id || endNode.time != l.endNode.time)
			error = true;
		
		return !error;
	}
	
	
}
