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
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Logger;
import com.bbn.mt.terp.BLEUcn;  
import com.bbn.mt.terp.BLEUtask;
import com.bbn.mt.terp.NormalizeText;
import com.bbn.mt.terp.TERid;
import com.bbn.mt.terp.TERinput;
import com.bbn.mt.terp.TERoutput;
import com.bbn.mt.terp.TERtask;
import com.bbn.mt.terp.TERutilities;
import com.bbn.mt.terp.BLEUcn.BLEUcounts;
import edu.cmu.sphinx.util.props.Configurable;
import edu.cmu.sphinx.util.props.ConfigurationManager;
import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;
import edu.cmu.sphinx.util.props.S4Integer;
import edu.cmu.sphinx.util.props.S4String;

public class MANYbleu implements Configurable
{
	private boolean DEBUG = true;
	//private String name;
	private Logger logger;
	
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

	/** The property that defines the Wordnet database location */
	@S4String(defaultValue = "")
	public final static String PROP_WORD_NET = "wordnet";

	/** The property that defines the stop word list filename */
	@S4String(defaultValue = "")
	public final static String PROP_STOP_LIST = "shift_word_stop_list";

	/** The property that defines the paraphrase-table filename */
	@S4String(defaultValue = "")
	public final static String PROP_PARAPHRASES = "paraphrases";

	/**
	 * The property that defines the evaluation method (can be MIN, MEAN or MAX)
	 */
	@S4String(defaultValue = "MEAN")
	public final static String PROP_METHOD = "method";

	/** The property that defines the number of threads used */
	@S4Integer(defaultValue = 0)
	public final static String PROP_MULTITHREADED = "multithread";

	private String outfile;
	private String terpParamsFile;
	private String reference;
	private String hypotheses;
	private String hypotheses_scores;
	private String terp_costs;
	private String priors_str;
	private String wordnet;
	private String shift_word_stop_list;
	private String paraphrases;
	private String method;
	private int nb_threads;
	private boolean allocated;

	private String[] hyps = null, hyps_scores = null;
	private String[] costs = null;
	private float[] priors = null;
	private Map<TERid, List<String>> refs;
	private Map<TERid, List<String[]>> refs_tok;

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
			MANYbleu.usage();
			System.exit(0);
		}

		ConfigurationManager cm;
		MANYbleu compute;
		URL url = null;
		try
		{
			url = new File(args[0]).toURI().toURL();
			cm = new ConfigurationManager(url);
			compute = (MANYbleu) cm.lookup("MANYBLEU");
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

		if (compute == null)
		{
			System.err.println("Can't find MANY : " + url+ "("+args[0]+")");
			return;
		}
	
		compute.allocate();
		compute.run();
	}

	private void allocate()
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
			// System.err.println(" >"+priors[i]+"< ");
		}
		refs = new TreeMap<TERid, List<String>>();
		refs_tok = new TreeMap<TERid, List<String[]>>();
		TERinput.load_file(reference, refs);
		for(Entry<TERid, List<String>> entry : refs.entrySet())
		{
			ArrayList<String[]> rlst = new ArrayList<String[]>();
			for(String r : entry.getValue())
			{
				NormalizeText.setLowerCase(false);
				String[] rt = NormalizeText.process(r);
				rlst.add(rt);
			}
			refs_tok.put(entry.getKey(), rlst);
		}
	}

	public MANYbleu()
	{

	}

	public void run()
	{
		//int nbSentences = 0;
		BLEUcounts[] bleu_scores = new BLEUcounts[hyps.length];
		ArrayList<String> lst = null;
		ArrayList<String> lst_sc = null;
		String backbone = null;
		String backbone_scores = null;
		int[] hyps_idx;

		//preparing TERp tasks
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
			TERutilities.generateParams(terpParamsFile + suffix, outfile + suffix, backbone, backbone_scores, i,
					lst, lst_sc, costs, priors, hyps_idx, wordnet, shift_word_stop_list, paraphrases);
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

			ArrayList<BLEUtask> bleutasks = new ArrayList<BLEUtask>();
			for (int i = 0; i < results.size(); ++i)
			{
				TERoutput output = null;
				// print this CN in a file named OUTPFX.cn.0, OUTPFX.cn.1 ...
				try
				{
					output = results.get(i).get();
				}
				catch (InterruptedException ie)
				{
					System.err.println("The task " + i + " had a problem : " + ie.getMessage());
					ie.printStackTrace();
					System.exit(-1);
				}
				catch (ExecutionException ee)
				{
					System.err.println("The task " + i + " had a problem : " + ee.getMessage());
					ee.printStackTrace();
					System.exit(-1);
				}

				// 2. Calculate BLEU between theref and CN
				logger.info("run : adding BLEU for ref eval with system " + i + " as backbone ...");
				bleutasks.add(new BLEUtask(i, output, refs_tok));
			}
			// launch threads
			ExecutorService ref_executor = Executors.newFixedThreadPool(nb_threads);
			List<Future<BLEUcn.BLEUcounts>> results_ref = null;
			try
			{
				results_ref = ref_executor.invokeAll(bleutasks);
			}
			catch (InterruptedException ie)
			{
				System.err.println("A task for ref eval had a problem : " + ie.getMessage());
				ie.printStackTrace();
				System.exit(-1);
			}
			ref_executor.shutdown(); // we don't need ref_executor anymore

			for (int i = 0; i < results_ref.size(); ++i)
			{
				try
				{
					bleu_scores[i] = results_ref.get(i).get();
				}
				catch (InterruptedException ie)
				{
					System.err.println("The task " + i + " had a problem : " + ie.getMessage());
					ie.printStackTrace();
					System.exit(-1);
				}
				catch (ExecutionException ee)
				{
					System.err.println("The task " + i + " had a problem : " + ee.getMessage());
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
				catch (Exception e1)
				{
					e1.printStackTrace();
				}
				
				if (output == null)
				{
					logger.info("output is null ... exiting !");
					System.exit(-1);
				}
				// else { logger.info("we have an output ...");}

				// 2. Calculate pseudo-BLEU between the ref and CN
				logger.info("run : launching BLEU for ref eval with system " + i + " as backbone ...");
				try
				{
					bleu_scores[i] = new BLEUtask(i, output, refs_tok).call();
				}
				catch (Exception e)
				{
					System.err.println("ERROR : MANYbleu::run : ");
					e.printStackTrace();
					System.exit(0);
				}
			}
		}
		// 3. Output scores according to aggregation function (set by 'method'
		// parameter)
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

		logger.info("run : Calculating final score for each sentence (according to 'method' parameter) ...");
		double bleuScore = 0.0;
		BLEUcn.BLEUcounts bleu_counts = new BLEUcn(-1).new BLEUcounts();
		
		for (int i = 0; i < bleu_scores.length; i++) // for each backbone
		{
			if (DEBUG)
				System.err.println("Backbone #" + i);

			//sum up everything
			bleu_counts.closest_ref_length += bleu_scores[i].closest_ref_length;  
			bleu_counts.translation_length += bleu_scores[i].translation_length;
			
			for(int b=0; b<BLEUcn.max_ngram_size; b++)
			{
				bleu_counts.ngram_counts[b] += bleu_scores[i].ngram_counts[b];
				bleu_counts.ngram_counts_ref[b] += bleu_scores[i].ngram_counts_ref[b];
				bleu_counts.ngram_counts_clip[b] += bleu_scores[i].ngram_counts_clip[b];
			}	
		}
		
		bleuScore = bleu_counts.computeBLEU();
		//bleuScore = bleu_counts.computeRecall();
		
		if (DEBUG)
			System.err.println(" score : " + bleuScore);
		
		try
		{
			bw.write(""+bleuScore);
			//bw.newLine();
			bw.close();
		}
		catch (IOException ioe)
		{
			ioe.printStackTrace();
			System.exit(-1);
		}
		
		/*try
		{
			bw.close();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}*/
	}

	
	@Override
	public void newProperties(PropertySheet ps) throws PropertyException
	{
		if (allocated) { throw new RuntimeException("Can't change properties after allocation"); }

		logger = ps.getLogger();
		// Files
		reference = ps.getString(PROP_REFERENCE_FILE);
		hypotheses = ps.getString(PROP_HYPOTHESES_FILES);
		hypotheses_scores = ps.getString(PROP_HYPS_SCORES_FILES);
		outfile = ps.getString(PROP_OUTPUT_FILE);

		// TERp
		terpParamsFile = ps.getString(PROP_TERP_PARAMS_FILE);
		terp_costs = ps.getString(PROP_COSTS);

		wordnet = ps.getString(PROP_WORD_NET);
		shift_word_stop_list = ps.getString(PROP_STOP_LIST);
		paraphrases = ps.getString(PROP_PARAPHRASES);

		// Other
		method = ps.getString(PROP_METHOD);
		priors_str = ps.getString(PROP_PRIORS);
		nb_threads = ps.getInt(PROP_MULTITHREADED);
	}

	static String[] usage_ar =
	{"Usage : ", "java -Xmx8G -cp MANY.jar edu.lium.mt.ComputeTERcn parameters.xml "};
	public static void usage()
	{
		System.err.println(TERutilities.join("\n", usage_ar));
	}

}
