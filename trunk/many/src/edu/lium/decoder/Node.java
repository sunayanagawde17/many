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

import java.util.Vector;

public class Node
{
	public float time;
	public int id;
	
	public Vector<Link> backLinks;
	public Vector<Link> nextLinks;
	
	public Node(int i, float t)
	{
		id = i;
		time = t;
		backLinks = new Vector<Link>();
		nextLinks = new Vector<Link>();
	}
	
}
