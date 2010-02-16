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
package edu.cmu.sphinx.linguist.language.ngram;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.logging.Logger;

import edu.cmu.sphinx.util.props.Configurable;
import edu.cmu.sphinx.util.props.ConfigurationManager;
import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;
import edu.cmu.sphinx.util.props.PropertyType;
import edu.cmu.sphinx.util.props.Registry;

public class LanguageModelServer implements Configurable
{
	/**
	 * The SphinxProperty name for lmserver port.
	 */
	public final static String PROP_LMSERVER_PORT = "lmserverport";
	/**
	 * The default value for the property PROP_LMSERVER_PORT.
	 */
	public final static int PROP_LMSERVER_PORT_DEFAULT = 1234;
	/**
	 * The SphinxProperty name for lmserver port.
	 */
	public final static String PROP_LMSERVER_HOST = "lmserverhost";
	/**
	 * The default value for the property PROP_LMSERVER_PORT.
	 */
	public final static String PROP_LMSERVER_HOST_DEFAULT = "localhost";
	/**
     * The Sphinx Property specifying the maximum depth reported by the
     * language model (from a getMaxDepth()) call. If this property is set to
     * (-1) (the default) the language model reports the implicit depth of the
     * model. This property allows a deeper language model to be used. For
     * instance, a trigram language model could be used as a bigram model by
     * setting this property to 2. Note if this property is set to a value
     * greater than the implicit depth, the implicit depth is used. Legal
     * values for this property are 1..N and -1.
     */
    public final static String PROP_MAX_DEPTH = "maxDepth";
    /**
     * The default value for PROP_MAX_DEPTH.
     */
    public final static int PROP_MAX_DEPTH_DEFAULT = -1;
    /**
     * The Sphinx Property specifying the location of the language model.
     */
    public final static String PROP_LOCATION = "location";
    /**
     * The default value of PROP_LOCATION.
     */
    public final static String PROP_LOCATION_DEFAULT = ".";
    /**
     * The Sphinx Property specifying the location of the language model.
     */
    public final static String PROP_UNK = "unk";
    /**
     * The default value of PROP_LOCATION.
     */
    public final static boolean PROP_UNK_DEFAULT = false;
    
    
	private Logger logger;
	private ConfigurationManager cm;
	private String name;
	private String host;
	private int port;
	private int maxDepth;
	private boolean unk;
	private File location;
	
	public String getName()
	{
		return name;
	}
	public void newProperties(PropertySheet ps) throws PropertyException
	{
		logger = ps.getLogger();
		cm = ps.getPropertyManager();
		port = ps
				.getInt(PROP_LMSERVER_PORT, PROP_LMSERVER_PORT_DEFAULT);
		host = ps.getString(PROP_LMSERVER_HOST, PROP_LMSERVER_HOST_DEFAULT);
		maxDepth = ps.getInt(LanguageModel.PROP_MAX_DEPTH,
				LanguageModel.PROP_MAX_DEPTH_DEFAULT);
		unk = ps.getBoolean(PROP_UNK,PROP_UNK_DEFAULT); 
		location = new File(ps.getString(PROP_LOCATION, PROP_LOCATION_DEFAULT));
	}
	public void register(String name, Registry registry)
			throws PropertyException
	{
		this.name = name;
		registry.register(PROP_LMSERVER_PORT, PropertyType.INT);
		registry.register(PROP_LMSERVER_HOST, PropertyType.STRING);
		registry.register(PROP_MAX_DEPTH, PropertyType.INT);
		registry.register(PROP_LOCATION, PropertyType.STRING);
		registry.register(PROP_UNK, PropertyType.BOOLEAN);
		registry.register("lmserver", PropertyType.COMPONENT);
		
	}
	
	/**
	 * Create the language model server
	 */
	public void allocate() throws IOException
	{
		
		//lancer la commande suivante dans un process
		// ngram -order 4 -lm location.getAbsolutePath() -server-port port -unk
		
		
	}
	
	
}
