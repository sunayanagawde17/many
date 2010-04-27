package edu.lium.mt;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.logging.Logger;
import edu.cmu.sphinx.util.props.Configurable;
import edu.cmu.sphinx.util.props.ConfigurationManager;
import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;
import edu.cmu.sphinx.util.props.S4Component;
import edu.cmu.sphinx.util.props.S4String;
import edu.lium.decoder.Graph;
import edu.lium.decoder.MANYcn;
import edu.lium.decoder.TokenPassDecoder;

public class MANYdecode implements Configurable
{
	private boolean DEBUG = false;
	private String name;
	private Logger logger;
	
	/** The property that defines the token pass decoder component. */
	@S4Component(type = TokenPassDecoder.class)
	public final static String PROP_DECODER = "decoder";
	
	/* The property that defines the output filename */
	@S4String(defaultValue = "many.output")
    public final static String PROP_OUTPUT_FILE = "output";
	
	/* The property that defines the hypotheses ConfusionNetworks filenames */
	@S4String(defaultValue = "")
    public final static String PROP_HYPOTHESES_CN_FILES = "hypotheses_cn";
	
	/* The property that defines the system priors */
	@S4String(defaultValue = "")
	public final static String PROP_PRIORS = "priors";
	
	
	private String outfile;
	private String hypotheses_cn;
	private String priors_str;
	private boolean mustReWeight = false;
	private boolean allocated;
	
	private String[] hyps_cn = null;
	private float[] priors = null;
	
	private TokenPassDecoder decoder = null;
	
	/**
	 * Main method of this MANYdecode tool.
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
		MANYdecode decode;

		try
		{
			URL url = new File(args[0]).toURI().toURL();
			cm = new ConfigurationManager(url);
			decode = (MANYdecode) cm.lookup("MANYDECODE");
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

		if (decode == null)
		{
			System.err.println("Can't find MANYdecode" + args[0]);
			return;
		}
		
		decode.allocate();
		decode.run();
	}
	
	private void allocate()
	{
		allocated = true;
		logger.info("MANYdecode::allocate ...");
		hyps_cn = hypotheses_cn.split("\\s+");
		String[] lst = priors_str.split("\\s+");
		priors = new float[lst.length];
		System.err.println("priors : ");
		for (int i = 0; i < lst.length; i++)
		{
			System.err.print(" >"+lst[i]+"< donne ");
			priors[i] = Float.parseFloat(lst[i]);
			System.err.println(" >"+priors[i]+"< ");
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
		if (allocated) {
            throw new RuntimeException("Can't change properties after allocation");
        }
		
		logger = ps.getLogger();
		decoder = (TokenPassDecoder) ps.getComponent(PROP_DECODER);
		outfile = ps.getString(PROP_OUTPUT_FILE);
		hypotheses_cn = ps.getString(PROP_HYPOTHESES_CN_FILES);
		priors_str = ps.getString(PROP_PRIORS);
		if(priors_str.equals("") == false)
			mustReWeight = true;
	}

	public void run()
	{
		//logger.info("MANYdecode::run");
		// init decoder
		try
		{
			decoder.allocate();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		
		//load CNs for each system
		ArrayList<ArrayList<MANYcn>> all_cns = new ArrayList<ArrayList<MANYcn>>(); 
		int nbSentences = -1;
		logger.info("About to load "+hyps_cn.length+" CNs files ... ");
		for(int i=0; i<hyps_cn.length; i++)
		{
			logger.info("Loading CN file : "+hyps_cn[i]);
			ArrayList<MANYcn> fullcns = MANYcn.loadFullCNs(hyps_cn[i]);
			ArrayList<MANYcn> cns = new ArrayList<MANYcn>();
			//if we have to re-weight, then do it !
			if(mustReWeight)
			{
				MANYcn.changeSysWeights(fullcns, priors);
				MANYcn.outputFullCNs(fullcns,"output.fullcn.reweight."+i);
				
				cns = MANYcn.fullCNs2CNs(fullcns);
				MANYcn.outputCNs(cns,"output.cn.reweight."+i);
				
				fullcns = null;
			}
			
			all_cns.add(cns);
			
			if(i == 0)
			{
				nbSentences = all_cns.get(i).size();
			}
			else if(nbSentences != all_cns.get(i).size())
			{
				System.err.println("MANYdecode::run : not the same number of hhypotheses for each system ... exiting !");
				System.exit(0);
			}
		}
		
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

		ArrayList<MANYcn> cns = new ArrayList<MANYcn>();
		for (int i = 0; i < nbSentences; i++) // foreach sentences
		{
			cns.clear();
			// build a lattice from all the results
			for (int j = 0; j < hyps_cn.length; j++)
			{
				cns.add(all_cns.get(j).get(i));
			}
			
			//logger.info("run : Creating graph for sentence "+i	 );
			Graph g = new Graph(cns, priors);
			//g.printHTK("graph.htk_"+i+".txt");

			// Then we can decode this graph ...
			logger.info("run : decoding graph for sentence "+i);
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
		decoder.deallocate();
	}
	
}
