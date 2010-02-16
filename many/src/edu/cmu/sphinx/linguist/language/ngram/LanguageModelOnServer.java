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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import edu.cmu.sphinx.linguist.WordSequence;
import edu.cmu.sphinx.util.LogMath;
import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;
import edu.cmu.sphinx.util.props.PropertyType;
import edu.cmu.sphinx.util.props.Registry;
public class LanguageModelOnServer implements LanguageModel
{
	/**
	 * The SphinxProperty name for lmserver.
	 */
	public final static String PROP_LMSERVER = "lmserver";
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
	 * Sphinx property for the name of the file that logs all the queried
	 * N-grams. If this property is set to null, it means that the queried
	 * N-grams are not logged.
	 */
	public static final String PROP_QUERY_LOG_FILE = "queryLogFile";
	/**
	 * The default value for PROP_QUERY_LOG_FILE.
	 */
	public static final String PROP_QUERY_LOG_FILE_DEFAULT = null;
	public final static String PROP_LOG_MATH = "logMath";
	/**
	 * A sphinx property that defines that maxium number of trigrams to be
	 * cached
	 */
	public static final String PROP_NGRAM_CACHE_SIZE = "ngramCacheSize";

	/**
	 * The default value for the PROP_NGRAM_CACHE_SIZE property
	 */
	public static final int PROP_NGRAM_CACHE_SIZE_DEFAULT = 100000;

	private String name;
	private Logger logger;
	//private ConfigurationManager cm;
	private int lmServerPort;
	private Socket socket;
	private String lmServerHost;
	private int maxDepth;
	private String ngramLogFile;
	private PrintWriter logFile;
	private LogMath logMath;
	private int ngramCacheSize;
	private LRUCache ngramCache;
	private BufferedReader reader;
	private PrintWriter writer;

	// private LanguageModelOnServer(){}
	public String getName()
	{
		return name;
	}
	public void newProperties(PropertySheet ps) throws PropertyException
	{
		logger = ps.getLogger();
		//cm = ps.getPropertyManager();
		lmServerPort = ps.getInt(PROP_LMSERVER_PORT, PROP_LMSERVER_PORT_DEFAULT);
		lmServerHost = ps.getString(PROP_LMSERVER_HOST, PROP_LMSERVER_HOST_DEFAULT);
		maxDepth = ps.getInt(LanguageModel.PROP_MAX_DEPTH, LanguageModel.PROP_MAX_DEPTH_DEFAULT);
		ngramLogFile = ps.getString(PROP_QUERY_LOG_FILE, PROP_QUERY_LOG_FILE_DEFAULT);
		logMath = (LogMath) ps.getComponent(PROP_LOG_MATH, LogMath.class);
		ngramCacheSize = ps.getInt(PROP_NGRAM_CACHE_SIZE, PROP_NGRAM_CACHE_SIZE_DEFAULT);

	}
	public void register(String name, Registry registry) throws PropertyException
	{
		this.name = name;
		registry.register(PROP_LMSERVER_PORT, PropertyType.INT);
		registry.register(PROP_LMSERVER_HOST, PropertyType.STRING);
		registry.register(PROP_QUERY_LOG_FILE, PropertyType.STRING);
		registry.register(PROP_MAX_DEPTH, PropertyType.INT);
		registry.register(PROP_LOG_MATH, PropertyType.COMPONENT);
		registry.register(PROP_LMSERVER, PropertyType.COMPONENT);
		registry.register(PROP_NGRAM_CACHE_SIZE, PropertyType.INT);

	}
	/**
	 * Create the language model
	 */
	public void allocate() throws IOException
	{
		if (ngramLogFile != null)
		{
			logFile = new PrintWriter(new FileOutputStream(ngramLogFile));
		}
		socket = new Socket(lmServerHost, lmServerPort);
		reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

		// maybe have to read "ready" from the server
		String s = reader.readLine();
		logger.info("Le lmserver te dit bonjour : " + s);

		writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(socket.getOutputStream())), true);

		ngramCache = new LRUCache(ngramCacheSize);

	}
	/**
	 * Deallocate resources allocated to this language model
	 */
	public void deallocate()
	{
		if (socket != null)
		{
			try
			{
				socket.close();
			}
			catch (IOException ioe)
			{
			}
		}
	}
	/**
	 * Returns the maximum depth of the language model
	 * 
	 * @return the maximum depth of the language model
	 */
	public int getMaxDepth()
	{
		return maxDepth;
	}
	/**
	 * Gets the ngram probability of the word sequence represented by the word
	 * list
	 * 
	 * @param wordSequence
	 *            the word sequence
	 * 
	 * @return the probability of the word sequence. Probability is in logMath
	 *         log base
	 * 
	 */
	public float getProbability(WordSequence wordSequence)
	{
		float proba = -Float.MAX_VALUE;

		if (wordSequence == null)
			return proba;

		String ret = null;
		int numberWords = wordSequence.size();

		if (numberWords > maxDepth) { throw new Error("Unsupported N-gram (size too big): " + wordSequence.size()); }
		Float resultat = ngramCache.get(wordSequence);
		if (resultat != null)
		{

			if (logFile != null)
			{
				logFile.println(wordSequence.toText() + " " + resultat);
			}
			return resultat;
		}
		String ngram = wordSequence.toText();
		// logger.info("Ask LM Server for prob of : "+ngram);
		writer.println(ngram);

		try
		{
			ret = reader.readLine();
			proba = Float.parseFloat(ret);
			// logger.info("LM Server returns prob : "+proba);
		}
		catch (IOException e)
		{
			throw new Error("Read error: " + e);
		}
		catch (NumberFormatException nfe)
		{
			throw new Error("LM Server returned wrong number: " + ret + " " + nfe);
		}

		if (logFile != null)
		{
			logFile.println(wordSequence.toText() + " " + ret);
		}

		if (proba != -Float.MAX_VALUE)
		{
			// return proba;
			// logger.info("proba="+proba+" en logMath.logbase (source base e): "+logMath.lnToLog(proba)+" source base 10 : "+logMath.log10ToLog(proba));
			resultat = logMath.log10ToLog(proba);
			ngramCache.put(wordSequence, resultat);
			return resultat; // logMath.log10ToLog(proba);
		}
		throw new Error("Unsupported N-gram: " + wordSequence.size());
	}

	public float getSmear(WordSequence wordSequence)
	{
		// TODO Auto-generated method stub
		return 0;
	}
	public Set getVocabulary()
	{
		// TODO Auto-generated method stub
		return null;
	}
	public void start()
	{
		// TODO Auto-generated method stub
	}
	public void stop()
	{
		// TODO Auto-generated method stub

	}
	class LRUCache extends LinkedHashMap<WordSequence, Float>
	{
		int maxSize;

		/**
		 * Creates an LRU cache with the given maximum size
		 * 
		 * @param maxSize
		 *            the maximum size of the cache
		 */
		LRUCache(int maxSize)
		{
			this.maxSize = maxSize;
		}

		/**
		 * Determines if the eldest entry in the map should be removed.
		 * 
		 * @param eldest
		 *            the eldest entry
		 * 
		 * @return true if the eldest entry should be removed
		 */
		protected boolean removeEldestEntry(Map.Entry eldest)
		{
			return size() > maxSize;
		}
	}

}
