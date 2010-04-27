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
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Logger;
import com.bbn.mt.terp.TERalignment;
import com.bbn.mt.terp.TERalignmentCN;
import com.bbn.mt.terp.TERoutput;
import com.bbn.mt.terp.TERtask;
import com.bbn.mt.terp.TERutilities;
import edu.cmu.sphinx.util.props.Configurable;
import edu.cmu.sphinx.util.props.ConfigurationManager;
import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;
import edu.cmu.sphinx.util.props.S4Component;
import edu.cmu.sphinx.util.props.S4Integer;
import edu.cmu.sphinx.util.props.S4String;
import edu.lium.decoder.Graph;
import edu.lium.decoder.TokenPassDecoder;

public class MANY implements Configurable
{
	private String name;
	private Logger logger;
	private TokenPassDecoder decoder = null;

	/** The property that defines the decoder component. */
	@S4Component(type = TokenPassDecoder.class)
	public final static String PROP_DECODER = "decoder";

	/** The property that defines the output filename */
	@S4String(defaultValue = "many.output")
	public final static String PROP_OUTPUT_FILE = "output";

	/** The property that defines the TERp parameters filename */
	@S4String(defaultValue = "terp.params")
	public final static String PROP_TERP_PARAMS_FILE = "terpParams";

	/** The property that defines the reference filename */
	@S4String(defaultValue = "")
	public final static String PROP_REFERENCE_FILE = "reference";

	/** The property that defines the hypotheses filenames */
	@S4String(defaultValue = "")
	public final static String PROP_HYPOTHESES_FILES = "hypotheses";

	/** The property that defines the hypotheses scores filenames */
	@S4String(defaultValue = "")
	public final static String PROP_HYPS_SCORES_FILES = "hyps_scores";

	/** The property that defines the TERp costs */
	@S4String(defaultValue = "1.0 1.0 1.0 1.0 1.0 0.0 1.0")
	public final static String PROP_COSTS = "costs";

	/** The property that defines the system priors */
	@S4String(defaultValue = "")
	public final static String PROP_PRIORS = "priors";

	/** The property that defines the wordnet database location */
	@S4String(defaultValue = "")
	public final static String PROP_WORD_NET = "wordnet";

	/** The property that defines the stop word list filename */
	@S4String(defaultValue = "")
	public final static String PROP_STOP_LIST = "shift_word_stop_list";

	/** The property that defines the paraphrase-table filename */
	@S4String(defaultValue = "")
	public final static String PROP_PARAPHRASES = "paraphrases";

	/** The property that defines the evaluation method (can be MIN, MEAN or MAX) */
	@S4String(defaultValue = "MEAN")
	public final static String PROP_METHOD = "method";

	/** The property that defines the number of threads used */
	@S4Integer(defaultValue = 0)
	public final static String PROP_MULTITHREADED = "multithread";

	private String outfile;
	private String terpParamsFile;
	private String hypotheses;
	private String hypotheses_scores;
	private String terp_costs;
	private String priors_str;
	private String wordnet;
	private String shift_word_stop_list;
	private String paraphrases;
	private int nb_threads;
	private boolean allocated;

	private String[] hyps = null, hyps_scores = null;
	private String[] costs = null;
	private float[] priors = null;

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
		catch (PropertyException e)
		{
			System.err.println("Error during initialization: \n  " + e);
			return;
		}

		if (syscomb == null)
		{
			System.err.println("Can't find MANY" + args[0]);
			return;
		}
		
		syscomb.allocate();
		syscomb.combine();
		syscomb.deallocate();
	}

	private void allocate()
	{
		if(!allocated)
		{
			allocated = true;
			hyps = hypotheses.split("\\s+");
			hyps_scores = hypotheses_scores.split("\\s+");
			costs = terp_costs.split("\\s+");
			String[] lst = priors_str.split("\\s+");
			priors = new float[lst.length];
			// System.err.println("priors : ");
			for (int i = 0; i < lst.length; i++)
			{
				// System.err.print(" >"+lst[i]+"< donne ");
				priors[i] = Float.parseFloat(lst[i]);
				// System.err.println(" >"+priors_lst[i]+"< ");
			}
		}
	}

	private void deallocate()
	{
		allocated = false;
		hyps = hyps_scores = costs = null;
		priors = null;
		decoder.deallocate();
	}

	public MANY()
	{

	}

	public void combine()
	{
		int nbSentences = 0;
		ArrayList<String> lst = null;
		ArrayList<String> lst_sc = null;
		String backbone = null;
		String backbone_scores = null;
		int[] hyps_idx;
		ArrayList<TERoutput> outputs = new ArrayList<TERoutput>();

		ArrayList<TERtask> tasks = new ArrayList<TERtask>();
		for (int i = 0; i < hyps.length; i++)
		{
			// 1. Generate CNs with system i as backbone for each sentence
			// 1.1 Init variables
			lst = new ArrayList<String>();
			lst.addAll(Arrays.asList(hyps));
			lst.remove(i);

			lst_sc = new ArrayList<String>();
			lst_sc.addAll(Arrays.asList(hyps_scores));
			lst_sc.remove(i);

			backbone = hyps[i];  
			backbone_scores = hyps_scores[i];

			logger.info("run : " + backbone + " (" + backbone_scores + ") is the reference ....");
			logger.info("run : " + TERutilities.join(" ", lst) + " (" + TERutilities.join(" ", lst_sc)
					+ ") are the hypotheses ....");

			hyps_idx = new int[lst.size()];
			for (int idx = 0, pos = 0; idx < hyps.length; idx++)
			{
				if (idx != i)
					hyps_idx[pos++] = idx;
			}

			// 1.2 Generate terp.params file
			String suffix = ".thread" + i;
			generateParams(terpParamsFile + suffix, outfile + suffix, backbone, backbone_scores, i, lst, lst_sc,
					costs, priors, hyps_idx);
			logger.info("TERp params file generated ...");

			tasks.add(new TERtask(i, terpParamsFile + suffix, outfile + ".cn." + i));
		}
		
		if (nb_threads > 1)
		{
			System.err.println("Launching in multithreaded : nb_threads = " + nb_threads);
			ExecutorService executor = Executors.newFixedThreadPool(nb_threads);
			List<Future<TERoutput>> results = null;
			try
			{
				results = executor.invokeAll(tasks);
			}
			catch (InterruptedException ie)
			{
				System.err.println("A task had a problem : " + ie.getMessage());
				ie.printStackTrace();
				System.exit(-1);
			}
			executor.shutdown(); // we don't need executor anymore

			for (int i = 0; i < results.size(); ++i)
			{
				try
				{
					outputs.add(results.get(i).get());

				}
				catch (InterruptedException ie)
				{
					System.err.println("The task " + i + " had a problem : " + ie.getMessage());
					ie.printStackTrace();
					System.exit(-1);
				}
				catch (ExecutionException ee)
				{
					System.err.println("The task " + i + " had a problem ... : " + ee.getMessage());
					ee.printStackTrace();
					System.exit(-1);
				}
			}
		}
		else
		{
			for (int i = 0; i < hyps.length; ++i) // foreach backbone
			{
				logger.info("run : launching TERp for system " + i + " as backbone ...");
				TERoutput output = null;
				
				try
				{
					output = tasks.get(i).call();
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
				
				
				if (output == null)
				{
					logger.info("output is null ... exiting !");
					System.exit(-1);
				}
				// else { logger.info("we have an output ...");}

				outputs.add(output);
			}
		}

		// Build the lattice and decode
		// init output
		BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(System.out));
		if (outfile != null)
		{
			try
			{
				bw = new BufferedWriter(new FileWriter(outfile));
			}
			catch (IOException ioe)
			{
				System.err.println("I/O erreur durant creation output file " + String.valueOf(outfile) + " " + ioe);
			}
		}


		// init decoder
		try
		{
			decoder.allocate();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		
		nbSentences = outputs.get(0).getResults().size();

		ArrayList<ArrayList<ArrayList<Comparable<String>>>> aligns = new ArrayList<ArrayList<ArrayList<Comparable<String>>>>();
		ArrayList<ArrayList<ArrayList<Float>>> aligns_scores = new ArrayList<ArrayList<ArrayList<Float>>>();

		for (int i = 0; i < nbSentences; i++) // foreach sentences
		{
			aligns.clear();
			aligns_scores.clear();
			// build a lattice with all the results
			for (int j = 0; j < outputs.size(); j++)
			{
				TERalignment al = outputs.get(j).getResults().get(i);
				if (al == null)
				{
					logger.info("combine : empty result for system " + j + "... continuing !");
				}
				else if (al instanceof TERalignmentCN)
				{
					aligns.add(((TERalignmentCN) al).cn);
					aligns_scores.add(((TERalignmentCN) al).cn_scores);
				}
				else
				{
					logger.info("combine : not a confusion network ... aborting !");
					System.exit(0);
				}
			}

			if (aligns.isEmpty())
			{
				logger.info("combine : no result for sentence " + i + "... continuing !");
				try
				{
					bw.newLine();
				}
				catch (IOException ioe)
				{
					ioe.printStackTrace();
					System.exit(0);
				}
				continue;
			}

			logger.info("combine : decoding graph for sentence "+i);
			Graph g = new Graph(aligns, aligns_scores, priors);
			// g.printHTK("graph.htk_"+i+".txt");

			// Then we can decode this graph ...
			// logger.info("combine : decoding graph ...");
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

		try
		{
			bw.close();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	
	// @Override
	public String getName()
	{
		return name;
	}

	@Override
	public void newProperties(PropertySheet ps) throws PropertyException
	{
		logger = ps.getLogger();

		// files
		outfile = ps.getString(PROP_OUTPUT_FILE);
		hypotheses = ps.getString(PROP_HYPOTHESES_FILES);
		hypotheses_scores = ps.getString(PROP_HYPS_SCORES_FILES);

		// decode
		decoder = (TokenPassDecoder) ps.getComponent(PROP_DECODER);
		priors_str = ps.getString(PROP_PRIORS);

		// TERp
		terpParamsFile = ps.getString(PROP_TERP_PARAMS_FILE);
		terp_costs = ps.getString(PROP_COSTS);
		wordnet = ps.getString(PROP_WORD_NET);
		shift_word_stop_list = ps.getString(PROP_STOP_LIST);
		paraphrases = ps.getString(PROP_PARAPHRASES);
		
		//Others
		nb_threads = ps.getInt(PROP_MULTITHREADED);
	}

	static String[] usage_ar = {"Usage : ", "java -Xmx8G -cp MANY.jar edu.lium.mt.MANY parameters.xml "};
	
	public static void usage()
	{
		System.err.println(TERutilities.join("\n", usage_ar));
	}

	public void generateParams(String paramsFile, String output, String ref, String ref_scores, int ref_idx,
			ArrayList<String> hyps, ArrayList<String> hyps_scores, String[] costs, float[] sysWeights, int[] hyps_idx)
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

}
