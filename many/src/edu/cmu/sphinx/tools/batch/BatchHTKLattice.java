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
package edu.cmu.sphinx.tools.batch;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.util.logging.Logger;
import edu.cmu.sphinx.instrumentation.ConfigMonitor;
import edu.cmu.sphinx.util.props.Configurable;
import edu.cmu.sphinx.util.props.ConfigurationManager;
import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;
import edu.cmu.sphinx.util.props.PropertyType;
import edu.cmu.sphinx.util.props.Registry;
import edu.lium.decoder.Graph;
import edu.lium.decoder.TokenPassDecoder;
public class BatchHTKLattice implements Configurable
{
	/**
	 * The SphinxProperty name for how many files to skip at begin.
	 */
	public final static String PROP_BEGIN_SKIP = "beginSkip";
	/**
	 * The default value for the property PROP__BEGIN_SKIP.
	 */
	public final static int PROP_BEGIN_SKIP_DEFAULT = 0;
	/**
	 * The SphinxProperty name for how many utterances to process
	 */
	public final static String PROP_COUNT = "count";
	/**
	 * The default value for the property PROP_COUNT.
	 */
	public final static int PROP_COUNT_DEFAULT = 1000000;
	/**
	 * The sphinx property that Dictionary
	 */
	public final static String PROP_DICTIONARY = "dictionary";
	private String name;
	private int beginSkip;
	private int totalCount;
	private Logger logger;
	private ConfigurationManager cm;
	// private String ctmIn, ctmOut;
	// private String list, output;
	// private Majuscule majuscule;
	private TokenPassDecoder decoder;
	
	public String getName()
	{
		return name;
	}
	public void newProperties(PropertySheet ps) throws PropertyException
	{
		logger = ps.getLogger();
		cm = ps.getPropertyManager();
		beginSkip = ps.getInt(PROP_BEGIN_SKIP, PROP_BEGIN_SKIP_DEFAULT);
		totalCount = ps.getInt(PROP_COUNT, PROP_COUNT_DEFAULT);
		// majuscule = (Majuscule) ps.getComponent("majuscule",
		// Majuscule.class);
		decoder = (TokenPassDecoder) ps.getComponent("decoder",
				TokenPassDecoder.class);
		if (totalCount <= 0)
		{
			totalCount = Integer.MAX_VALUE;
		}
	}
	public void register(String name, Registry registry)
			throws PropertyException
	{
		this.name = name;
		registry.register(PROP_BEGIN_SKIP, PropertyType.INT);
		registry.register(PROP_COUNT, PropertyType.INT);
		// registry.register("majuscule", PropertyType.COMPONENT);
		registry.register("decoder", PropertyType.COMPONENT);
	}
	
	/*public void testLM() throws IOException
	{
		decoder.allocate();
		decoder.testLM();
		decoder.deallocate();
	}*/
	
	public void decode(String list, String output) throws IOException
	{
		System.err.println("BatchHTKLattice START : " + list + " " + output);
		//PrintStream outWriter = System.out;
		BufferedWriter outWriter = new BufferedWriter(new OutputStreamWriter(System.out));
		decoder.allocate();
		if (output != null)
			try
			{
				//outWriter = new PrintStream(output, "ISO8859_1"); 
				//outWriter = new PrintStream(output, "UTF-8");
				outWriter = new BufferedWriter(new FileWriter(output));
			}
			catch (IOException ioe)
			{
				System.err.println("I/O erreur durant lecture list file "
						+ output + " " + ioe);
			}
		int count = 0;
		String fichier = null;
		try
		{
			BufferedReader reader = new BufferedReader(new FileReader(list));
			// beginSkip est lue dans la config xml
			while ((beginSkip > 0) && ((fichier = reader.readLine()) != null))
				if (fichier.length() > 0)
					beginSkip--;
			while ((fichier = reader.readLine()) != null
					&& count++ < totalCount)
			{
				if (fichier.length() > 0)
				{
					System.err.println("BatchHTKLattice : creation graph "
							+ fichier);
					Graph g = new Graph();
					g.readHTK(fichier);
					decoder.decode(g, outWriter);
					logger.info(fichier + " traite ...");
				}
			}
			reader.close();
			outWriter.close();
			decoder.deallocate();
		}
		catch (IOException ioe)
		{
			System.err
					.println("IO erreur durant list file " + list + " " + ioe);
		}
	}
	
	
	
	/**
	 * Main method of this BatchDecoder.
	 * 
	 * @param argv
	 *            argv[0] : config.xml argv[1] : a file listing all the audio
	 *            files to decode
	 */
	public static void main(String[] argv) throws IOException
	{
		if (argv.length < 3)
		{
			System.out
					.println("Usage: BatchHTKLattice propertiesFile list output");
			System.exit(1);
		}
		String cmFile = argv[0];
		String list = argv[1];
		String output = argv[2];
		ConfigurationManager cm;
		BatchHTKLattice bmr = null;
		// BatchModeRecognizer recognizer;
		try
		{
			URL url = new File(cmFile).toURI().toURL();
			cm = new ConfigurationManager(url);
			bmr = (BatchHTKLattice) cm.lookup("batchHTKLattice");
		}
		catch (IOException ioe)
		{
			System.err.println("I/O error during initialization: \n   " + ioe);
			return;
		}
		catch (InstantiationException e)
		{
			System.err.println("Error during initialization: \n  " + e);
			return;
		}
		catch (PropertyException e)
		{
			System.err.println("Error during initialization: \n  " + e);
			return;
		}
		try
		{
			ConfigMonitor config = (ConfigMonitor) cm.lookup("configMonitor");
			config.run();// peut etre faut-il faire un thread
		}
		catch (InstantiationException e)
		{
			System.err.println("Error during config: \n  " + e);
			// return;
		}
		catch (PropertyException e)
		{
			System.err.println("Error during config: \n  " + e);
			// return;
		}
		
		if (bmr == null)
		{
			System.err.println("Can't find tokenPassDecoder" + cmFile);
			return;
		}
		System.gc();
		
		//bmr.testLM();
		//System.exit(0);
		bmr.decode(list, output);
		
	}
}
