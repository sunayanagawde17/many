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
package edu.lium.mt;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Logger;
import com.bbn.mt.terp.TERalignment;
import com.bbn.mt.terp.TERalignmentCN;
import com.bbn.mt.terp.TERoutput;
import com.bbn.mt.terp.TERplus;
import com.bbn.mt.terp.TERutilities;
import edu.cmu.sphinx.util.props.Configurable;
import edu.cmu.sphinx.util.props.ConfigurationManager;
import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;
import edu.cmu.sphinx.util.props.PropertyType;
import edu.cmu.sphinx.util.props.Registry;
import edu.lium.decoder.Graph;
import edu.lium.decoder.TokenPassDecoder;

public class MANY implements Configurable
{
	private String name;
	private Logger logger;
	private TERplus terp = null;
	private TokenPassDecoder decoder = null;

	private String outfile;
	public final static String PROP_OUTPUT_FILE = "output";
	public final static String PROP_OUTPUT_FILE_DEFAULT = "many.output";

	private String terpParamsFile;
	public final static String PROP_TERP_PARAMS_FILE = "terpParams";
	public final static String PROP_TERP_PARAMS_FILE_DEFAULT = "terp.params";

	private String hypotheses;
	public final static String PROP_HYPOTHESES_FILES = "hypotheses";
	public final static String PROP_HYPOTHESES_FILES_DEFAULT = "";

	private String hypotheses_scores;
	public final static String PROP_HYPS_SCORES_FILES = "hyps_scores";
	public final static String PROP_HYPS_SCORES_FILES_DEFAULT = "";

	private String terp_costs;
	public final static String PROP_COSTS = "costs";
	public final static String PROP_COSTS_DEFAULT = "1.0 1.0 1.0 1.0 1.0 0.0 1.0";

	private String priors_str;
	public final static String PROP_PRIORS = "priors";
	public final static String PROP_PRIORS_DEFAULT = "";

	private String wordnet;
	public final static String PROP_WORD_NET = "wordnet";
	public final static String PROP_WORD_NET_DEFAULT = "";

	String shift_word_stop_list;
	public final static String PROP_STOP_LIST = "shift_word_stop_list";
	public final static String PROP_STOP_LIST_DEFAULT = "";

	String paraphrases;
	public final static String PROP_PARAPHRASES = "paraphrases";
	public final static String PROP_PARAPHRASES_DEFAULT = "";

	private String[] hyps = null, scores = null;
	private String[] costs = null;
	private Float[] priors = null;

	/**
	 * Main method of this MTSyscomb tool.
	 * 
	 * @param argv
	 *            argv[0] : config.xml
	 */
	public static void main(String[] args)
	{
		if (args.length < 1)
		{
			MANY.usage();
			System.exit(0);
		}

		ConfigurationManager cm;
		MANY syscomb;

		try
		{
			URL url = new File(args[0]).toURI().toURL();
			cm = new ConfigurationManager(url);
			syscomb = (MANY) cm.lookup("MANY");
		}
		catch (IOException ioe)
		{
			System.err.println("I/O error during initialization: \n   " + ioe);
			return;
		}
		catch (InstantiationException e)
		{
			System.err.println("Error during initialization: \n  " + e + "\n Message : " + e.getMessage());
			e.printStackTrace();
			return;
		}
		catch (PropertyException e)
		{
			System.err.println("Error during initialization: \n  " + e);
			return;
		}

		/*
		 * try { ConfigMonitor config = (ConfigMonitor)
		 * cm.lookup("configMonitor"); config.run();// peut etre faut-il faire
		 * un thread } catch (InstantiationException e) {
		 * System.err.println("Error during config: \n  " + e); // return; }
		 * catch (PropertyException e) {
		 * System.err.println("Error during config: \n  " + e); // return; }
		 */

		if (syscomb == null)
		{
			System.err.println("Can't find MANY" + args[0]);
			return;
		}
		System.gc();

		syscomb.allocate();
		syscomb.combine();
	}

	private void allocate()
	{
		hyps = hypotheses.split("\\s+");
		scores = hypotheses_scores.split("\\s+");
		costs = terp_costs.split("\\s+");
		String[] lst = priors_str.split("\\s+");
		priors = new Float[lst.length];
		// System.err.println("priors : ");
		for (int i = 0; i < lst.length; i++)
		{
			// System.err.print(" >"+lst[i]+"< donne ");
			priors[i] = Float.parseFloat(lst[i]);
			// System.err.println(" >"+priors_lst[i]+"< ");
		}
	}

	public MANY()
	{

	}

	public void combine()
	{
		ArrayList<TERoutput> outputs = new ArrayList<TERoutput>();
		/* This an attempt to re-use TERp alignments with same costs -> this cause problem with scores on words actually
		 * String out = outfile+".cn"+TERutilities.join(costs, "_"); File f =
		 * new File(out); if(f.exists()) {
		 * logger.info("file "+out+" already exists ... loading CNs from it"); }
		 * else {
		 */
		for (int i = 0; i < hyps.length; ++i)
		{
			ArrayList<String> lst = new ArrayList<String>();
			lst.addAll(Arrays.asList(hyps));
			lst.remove(i);

			ArrayList<String> lst_sc = new ArrayList<String>();
			lst_sc.addAll(Arrays.asList(scores));
			lst_sc.remove(i);

			String ref = hyps[i];
			String ref_scores = scores[i];

			logger.info("combine : " + ref + " (" + ref_scores + ") is the reference ....");
			logger.info("combine : " + TERutilities.join(" ", lst) + " (" + TERutilities.join(" ", lst_sc)
					+ ") are the hypotheses ....");

			// generate terp.params file
			generateParams(terpParamsFile, ref, ref_scores, lst, lst_sc, costs);
			logger.info("TERp params file generated ...");

			logger.info("combine : launching TERp ");
			terp.setTerpParams(terpParamsFile);
			terp.allocate();
			TERoutput output = terp.run();

			if (output == null)
			{
				logger.info("output is null ...");
				System.exit(-1);
			}
			// else { logger.info("we have an output ...");}

			outputs.add(output);
			terp.deallocate();
		}
		// }
		int nbSentences = outputs.get(0).getResults().size();

		// init decoder
		try
		{
			decoder.allocate();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}

		// init output
		// PrintStream outWriter = System.out;
		FileWriter fw = null;
		BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(System.out));
		if (outfile != null)
		{
			try
			{
				// outWriter = new PrintStream(outfile, "ISO8859_1");
				// outWriter = new PrintStream(outfile, "UTF-8");
				fw = new FileWriter(outfile);
				bw = new BufferedWriter(fw);

			}
			catch (IOException ioe)
			{
				System.err.println("I/O erreur durant creation output file " + String.valueOf(outfile) + " " + ioe);
			}
		}

		ArrayList<ArrayList<ArrayList<Comparable<String>>>> aligns = new ArrayList<ArrayList<ArrayList<Comparable<String>>>>();
		ArrayList<ArrayList<ArrayList<Float>>> aligns_scores = new ArrayList<ArrayList<ArrayList<Float>>>();

		for (int i = 0; i < nbSentences; i++) // foreach sentences
		{
			// we build a lattice from all the results
			aligns.clear();
			aligns_scores.clear();
			for (int j = 0; j < hyps.length; j++)
			{
				TERalignment al = outputs.get(j).getResults().get(i);
				if (al instanceof TERalignmentCN)
				{
					aligns.add(((TERalignmentCN) al).cn);
					aligns_scores.add(((TERalignmentCN) al).cn_scores);
				}
				else
				{
					logger.info("combine : not a confusion network ... aborting");
					System.exit(0);
				}
			}

			logger.info("combine : Creating graph");
			Graph g = new Graph(aligns, aligns_scores, priors);
			//g.printHTK("graph.htk_"+i+".txt");

			// Then we can decode this graph ...
			// logger.info("combine : decoding graph ...");
			// decoder.decode(g, outWriter);

			try
			{
				decoder.decode(g, bw);

			}
			catch (IOException ioe)
			{
				ioe.printStackTrace();
				System.exit(0);
			}
		}

		// outWriter.close();
		try
		{
			bw.close();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		terp.deallocate();
		decoder.deallocate();
	}

	// @Override
	public String getName()
	{
		return name;
	}

	// @Override
	public void newProperties(PropertySheet ps) throws PropertyException
	{
		logger = ps.getLogger();
		decoder = (TokenPassDecoder) ps.getComponent("decoder", TokenPassDecoder.class);
		terp = (TERplus) ps.getComponent("terp", TERplus.class);
		outfile = ps.getString(PROP_OUTPUT_FILE, PROP_OUTPUT_FILE_DEFAULT);

		terpParamsFile = ps.getString(PROP_TERP_PARAMS_FILE, PROP_TERP_PARAMS_FILE_DEFAULT);
		hypotheses = ps.getString(PROP_HYPOTHESES_FILES, PROP_HYPOTHESES_FILES_DEFAULT);
		hypotheses_scores = ps.getString(PROP_HYPS_SCORES_FILES, PROP_HYPS_SCORES_FILES_DEFAULT);
		terp_costs = ps.getString(PROP_COSTS, PROP_COSTS_DEFAULT);
		priors_str = ps.getString(PROP_PRIORS, PROP_PRIORS_DEFAULT);

		wordnet = ps.getString(PROP_WORD_NET, PROP_WORD_NET_DEFAULT);
		shift_word_stop_list = ps.getString(PROP_STOP_LIST, PROP_STOP_LIST_DEFAULT);
		paraphrases = ps.getString(PROP_PARAPHRASES, PROP_PARAPHRASES_DEFAULT);

	}

	// @Override
	public void register(String name, Registry reg) throws PropertyException
	{
		this.name = name;
		reg.register("decoder", PropertyType.COMPONENT);
		reg.register("terp", PropertyType.COMPONENT);
		reg.register(PROP_OUTPUT_FILE, PropertyType.STRING);
		reg.register(PROP_TERP_PARAMS_FILE, PropertyType.STRING);
		reg.register(PROP_HYPOTHESES_FILES, PropertyType.STRING);
		reg.register(PROP_HYPS_SCORES_FILES, PropertyType.STRING);
		reg.register(PROP_COSTS, PropertyType.STRING);
		reg.register(PROP_PRIORS, PropertyType.STRING);
		reg.register(PROP_WORD_NET, PropertyType.STRING);
		reg.register(PROP_STOP_LIST, PropertyType.STRING);
		reg.register(PROP_PARAPHRASES, PropertyType.STRING);
	}

	static String[] usage_ar =
	{"Usage : ", "java -Xmx2G -cp MANY.jar edu.lium.mt.MANY parameters.xml "};
	public static void usage()
	{
		System.err.println(TERutilities.join("\n", usage_ar));
	}

	public void generateParams(String paramsFile, String ref, String ref_scores, ArrayList<String> hyps,
			ArrayList<String> hyps_scores, String[] costs)
	{
		StringBuilder sb = new StringBuilder();

		sb.append("Reference File (filename)                : ").append(ref);
		sb.append("\nReference Scores File (filename)         : ").append(ref_scores);
		sb.append("\nHypothesis Files (list)                  : ").append(TERutilities.join(" ", hyps));
		sb.append("\nHypothesis Scores Files (list)           : ").append(TERutilities.join(" ", hyps_scores));
		sb.append("\nOutput Prefix (filename)                 : ").append(outfile);
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
}
