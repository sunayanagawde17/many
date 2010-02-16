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
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.logging.Logger;
import edu.cmu.sphinx.linguist.WordSequence;
import edu.cmu.sphinx.linguist.dictionary.Dictionary;
import edu.cmu.sphinx.linguist.dictionary.SimpleDictionary;
import edu.cmu.sphinx.linguist.dictionary.Word;
import edu.cmu.sphinx.linguist.language.ngram.LanguageModelOnServer;
import edu.cmu.sphinx.linguist.language.ngram.large.LargeTrigramModel;
import edu.cmu.sphinx.util.LogMath;
import edu.cmu.sphinx.util.props.Configurable;
import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;
import edu.cmu.sphinx.util.props.PropertyType;
import edu.cmu.sphinx.util.props.Registry;

public class TokenPassDecoder implements Configurable
{
	public final static String PROP_LOG_MATH = "logMath";
	/**
	 * A sphinx property for the language model to be used by this grammar
	 */
	public final static String PROP_LANGUAGE_MODEL = "trigramModel";
	public final static String PROP_LANGUAGE_MODEL_ON_SERVER = "lmonserver";
	/**
	 * Property that defines the dictionary to use for this grammar
	 */
	public final static String PROP_DICTIONARY = "dictionary";
	/**
	 * Property that defines the default value for maximum number of tokens considered
	 */
	public final static int PROP_MAX_NB_TOKENS_DEFAULT = 1000;
	/**
	 * Property that defines the maximum number of tokens considered
	 */
	public final static String PROP_MAX_NB_TOKENS = "max_nb_tokens";
	/**
	 * Property that defines the fudge factor
	 */
	public final static String PROP_FUDGE_FACTOR = "fudge";
	/**
	 * Property that defines the default fudge factor value
	 */	
	public final static float PROP_FUDGE_FACTOR_DEFAULT = 1.0f;
	public final static String PROP_NULL_PENALTY = "null_penalty";
	public final static float PROP_NULL_PENALTY_DEFAULT = 1.0f;
	public final static String PROP_LENGTH_PENALTY = "length_penalty";
	public final static float PROP_LENGTH_PENALTY_DEFAULT = 1.0f;
	public final static String PROP_NBEST_LENGTH = "nbest_length";
	public final static int PROP_NBEST_LENGTH_DEFAULT = 0;
	
	public final static String PROP_USE_LM_3G = "uselm3g";
	public final static boolean PROP_USE_LM_3G_DEFAULT = false;
	
	private LargeTrigramModel languageModel;
	boolean DEBUG = false;
	private LogMath logMath;
	private Dictionary dictionary;
	private Logger logger;
	private String name;
	private TokenList tokens[];
	private TokenList lastTokens;
	private LanguageModelOnServer lm;
	private int maxNbTokens;
	private float fudge;
	private float null_penalty;
	private float length_penalty;
	private int nbest_length;
	private boolean useLM3g;
	
	public void allocate() throws java.io.IOException
	{
		dictionary.allocate();
		
		if(useLM3g == true)
		{
			languageModel.allocate();
		}
		else
		{
			lm.allocate();
		}
		tokens = new TokenList[2];
		for (int i = 0; i < tokens.length; i++)
			tokens[i] = new TokenList();
		lastTokens = new TokenList();
	}
	public void deallocate()
	{  
		dictionary.deallocate();
		
		if(useLM3g)
			languageModel.deallocate();
		else
			lm.deallocate();
	}
	
	public void register(String name, Registry registry)
			throws PropertyException
	{
		this.name = name;
		registry.register(PROP_LOG_MATH, PropertyType.COMPONENT);
		registry.register(PROP_DICTIONARY, PropertyType.COMPONENT);
		registry.register(PROP_MAX_NB_TOKENS, PropertyType.INT);
		registry.register(PROP_FUDGE_FACTOR, PropertyType.FLOAT);
		registry.register(PROP_NULL_PENALTY, PropertyType.FLOAT);
		registry.register(PROP_LENGTH_PENALTY, PropertyType.FLOAT);
		registry.register(PROP_NBEST_LENGTH, PropertyType.INT);
		registry.register(PROP_LANGUAGE_MODEL, PropertyType.COMPONENT);
		registry.register(PROP_LANGUAGE_MODEL_ON_SERVER, PropertyType.COMPONENT);
		registry.register(PROP_USE_LM_3G, PropertyType.BOOLEAN);
	}
	
	public void newProperties(PropertySheet ps) throws PropertyException
	{
		logger = ps.getLogger();
		logMath = (LogMath) ps.getComponent(PROP_LOG_MATH, LogMath.class);
		dictionary = (Dictionary) ps.getComponent(PROP_DICTIONARY,
				Dictionary.class);
		
		useLM3g = ps.getBoolean(PROP_USE_LM_3G, PROP_USE_LM_3G_DEFAULT);
		if(useLM3g)
		{
			languageModel = (LargeTrigramModel) ps.getComponent(PROP_LANGUAGE_MODEL, LargeTrigramModel.class);
		}
		else
		{	lm = (LanguageModelOnServer) ps.getComponent(PROP_LANGUAGE_MODEL_ON_SERVER,
				LanguageModelOnServer.class);
		}
		maxNbTokens = ps.getInt(PROP_MAX_NB_TOKENS, PROP_MAX_NB_TOKENS_DEFAULT);
		fudge = 10.0f * ps.getFloat(PROP_FUDGE_FACTOR, PROP_FUDGE_FACTOR_DEFAULT);
		null_penalty = ps.getFloat(PROP_NULL_PENALTY, PROP_NULL_PENALTY_DEFAULT);
		length_penalty = 10.0f * ps.getFloat(PROP_LENGTH_PENALTY, PROP_LENGTH_PENALTY_DEFAULT);
		nbest_length = ps.getInt(PROP_NBEST_LENGTH, PROP_NBEST_LENGTH_DEFAULT);
	}
	
	public String getName()
	{
		return name;
	}
	
	public void decode(Graph lat, BufferedWriter outWriter) throws IOException
	{
		if(DEBUG)
			logger.info("TokenPassDecoder::decode START ");
		if(lat == null || outWriter == null)
		{
			logger.info("Lattice or writer is null");
			return;
		}
		else if(DEBUG)
		{
			logger.info("Initialization OK !");
		}
		
		System.err.println("TokenPassDecoder::decode parameters ");
		System.err.println(" - fudge : "+fudge);
		System.err.println(" - null penalty : " + null_penalty);
		System.err.println(" - length penalty : "+length_penalty);
		System.err.println(" - Max nb tokens : "+maxNbTokens);
		System.err.println(" - N-Best length : "+nbest_length);
		
		int origine = 0, cible = 1;
		//int maxHist = languageModel.getMaxDepth();
		int maxHist = lm.getMaxDepth();

		//logger.info("maxHist = "+maxHist);
		ArrayList<Node> nodes = new ArrayList<Node>();
		Node n = lat.firstNode;
		nodes.add(n);
		
		float maxScore = -Float.MAX_VALUE;
		
		int i=0;  
		Token t = new Token(0.0f, null, n, null, null);
		tokens[origine].clear();
		tokens[origine+1].clear();
		lastTokens.clear();  

		tokens[origine].add(t);  
		//logger.info("just before entering decode loop" );		
		
		while(tokens[origine].isEmpty() == false)
		{
			//logger.info("entering decode loop");		
			//float theMax = -Float.MAX_VALUE;
			// deployer les tokens
			/*if(tokens == null)
				logger.info("tokens is null ...");
			else
				logger.info("tokens is not null ...");
			if(tokens[origine] == null)
				logger.info("tokens["+origine+"] is null ...");
			else
				logger.info("tokens[origine] is not null ...");*/
			
			maxScore = -Float.MAX_VALUE;
			
			for (Token pred : tokens[origine])
			{
				/*if(pred == null)
					logger.info("pred is null ");
				if(pred.node == null)
					logger.info("pred.node is null ");
				if(pred.node.nextLinks == null)
					logger.info("pred.node.nextLinks is null ");*/
				//logger.info("pred node = "+pred.node.id);
				
				for(Link l : pred.node.nextLinks)
				{
					String ws = lat.idToWord.get(l.wordId);
					//logger.info("## link #"+i+" id="+l.wordId+" w="+ws);
					//logger.info("next node = "+l.endNode.id);
					i++;
					WordSequence ns = null;
					WordSequence lmns = null;
					Word w = null;
					float nscore = pred.score;
				
					if(pred.history == null)
					{
						if(Graph.null_node.equals(ws))
						{
							//ns = WordSequence.getWordSequence(new Word(ws, null, true));
							ns = WordSequence.getWordSequence(new Word(ws, null, true));
							lmns = WordSequence.getWordSequence(new Word(ws, null, true));
							//logger.info("... Creating word sequence with only "+ws+" (null_node) gives -> "+ns.toText()+" [(1) for LM : "+lmns.toText()+" ]");
							
							nscore += logMath.linearToLog(null_penalty); //null_penalty
						}
						else
						{
							w = dictionary.getWord(ws);
							if(w == ((SimpleDictionary)dictionary).getUnknownWord())
							{
								ns = WordSequence.getWordSequence(new Word(ws, null, false));
							}
							else
							{
								ns = WordSequence.getWordSequence(w);
							}
							lmns = WordSequence.getWordSequence(w);
							//logger.info("... Creating word sequence with only "+ws+" gives -> "+ns.toText()+" [(2) for LM : "+lmns.toText()+" ]");
							
							float lmscore = 0.0f;
							if(useLM3g == true)
							{
								lmscore = languageModel.getProbability(ns.withoutWord(Graph.null_node));
								//logger.info("Proba lm : "+lmscore);
							}
							else
							{
								lmscore = lm.getProbability(lmns.withoutWord(Graph.null_node));
								//logger.info("Proba lmonserver : "+lmscore);
							}
							nscore = nscore + (fudge * lmscore);
							nscore += logMath.linearToLog(length_penalty); // length_penalty	
						}
					}
					else
					{
						if(Graph.null_node.equals(ws))
						{
							ns = pred.history.addWord(new Word(ws, null, true), maxHist);
							lmns = pred.history.addWord(new Word(ws, null, true), maxHist);
							//logger.info("... Adding "+ws+" (null_node) to the word sequence, -> "+ns.toText()+" [(3) for LM : "+lmns.toText()+" ]");
							nscore += logMath.linearToLog(null_penalty); //null_penalty
						}
						else  
						{	
							w = dictionary.getWord(ws);
							if(w == ((SimpleDictionary)dictionary).getUnknownWord())
							{
								ns = pred.history.addWord(new Word(ws, null, false), maxHist);
							}
							else
							{
								ns = pred.history.addWord(w, maxHist);
							}
							lmns = pred.lmhistory.addWord(w, maxHist);
							//logger.info("... Adding "+ws+" to the word sequence, -> "+ns.toText()+" [(4) for LM : "+lmns.toText()+" ]");
							
							float lmscore = 0.0f;
							if(useLM3g == true)
							{
								lmscore = languageModel.getProbability(ns.withoutWord(Graph.null_node));
								//logger.info("Proba lm : "+lmscore);
							}
							else
							{
								lmscore = lm.getProbability(lmns.withoutWord(Graph.null_node));
								//logger.info("Proba lmonserver : "+lmscore);
							}
							nscore = nscore + (fudge * lmscore);
							nscore += logMath.linearToLog(length_penalty); // length_penalty
						}
					}
					
					if(Graph.null_node.equals(ws) == false)
					{
						//logger.info("Link proba : "+l.posterior+" -> en log :"+logMath.linearToLog(l.posterior));
						nscore += logMath.linearToLog(l.posterior);
					}
						
					
					if(nscore > maxScore)
						maxScore = nscore;
					
					Token tok = new Token(nscore, pred, l.endNode, ns, lmns);
					if(l.endNode == lat.lastNode)
					{
						//logger.info("adding "+tok.node.time+" to lastTokens");
						lastTokens.add(tok);
					}
					else
					{
						tokens[cible].add(tok);
					}
				}
			}
			//logger.info("pruning tokens");
			// pruning tokens can be done on nthe number of tokens or on a prob threshold
			if(tokens[cible].size() > maxNbTokens)
			{
				/*System.err.println(" BEFORE pruning ... :");
				for(int r=0; r<tokens[cible].size() && r<5; r++)
				{
					System.err.print(" "+tokens[cible].get(r).score);
				}
				System.err.println(" --------------- ");
			
				logger.info("size before pruning " + tokens[cible].size());*/
				Collections.sort(tokens[cible], Collections.reverseOrder());
				tokens[cible].removeRange(maxNbTokens, tokens[cible].size());	//taking only maxNbTokens best tokens
				/*logger.info("size after pruning " + tokens[cible].size());
			
				System.err.println(" AFTER pruning ... :");
				for(int r=0; r<tokens[cible].size() && r<5; r++)
				{
					System.err.print(" "+tokens[cible].get(r).score);
				}
				System.err.println(" --------------- ");*/
			}
			
			normalizeToken(tokens[cible], maxScore);
			
			tokens[origine].clear();
			cible = origine;
			origine = (cible + 1) % 2;
		}
		
		//looking for best tokens in lastTokens
		Collections.sort(lastTokens, Collections.reverseOrder());
		
		if(nbest_length > 0) // generate a nbest-list (BTEC format)
		{
			int nn = Math.min(lastTokens.size(), nbest_length);
			if(DEBUG)
				logger.info("Looking for "+nn+" best tokens (nbest list format) ...");
			ArrayList<String> obest = new ArrayList<String>();
			for(int nb=0; nb<nn; nb++)
			{
				obest.clear();
				Token best = lastTokens.get(nb);
				while (best != null && best.node != lat.firstNode)
				{
					if(DEBUG) 
						logger.info(best.history.getNewWord().getSpelling() + " " + best.node.id+" time="+best.node.time);
					if(Graph.null_node.equals(best.history.getNewWord().getSpelling()) == false)
					{	
						obest.add(0, best.history.getNewWord().getSpelling());
					}
					else if(DEBUG) 
						logger.info("found null_node : "+best.history.getNewWord().getSpelling());
					best = best.pred;
				}
				
				outWriter.write("#"+"#"+0+"#");
				if(obest.size() > 0)
				{
					outWriter.write(obest.get(0));
					for(int no=1; no<obest.size(); no++)
					{
						outWriter.write(" ");
						outWriter.write(obest.get(no));
					}
				}
				outWriter.write("#0.000000e+00#"+best.score+"#");
				outWriter.newLine();
			}
		}
		else
		{
			if(DEBUG)
				logger.info("Looking for best token (1 hypo per line format) ...");
			Token best = null;
			float max = -Float.MAX_VALUE;
			for (Token last : lastTokens)
			{	if (last.score > max)
				{
					max = last.score;
					best = last;
				}
			}
			//logger.info("best score: " + max + " best="+best.node.time);
			
			// reproducing 1-best
			ArrayList<String> obest = new ArrayList<String>();
			while (best != null && best.node != lat.firstNode)
			{
				if(DEBUG)
					logger.info(best.history.getNewWord().getSpelling() + " " + best.node.id+" time="+best.node.time);
				if(Graph.null_node.equals(best.history.getNewWord().getSpelling()) == false)
				{	
					obest.add(0, best.history.getNewWord().getSpelling());
				}
				else if(DEBUG) logger.info("un null_node : "+best.history.getNewWord().getSpelling());
				best = best.pred;
			}
			
			if(obest.size() > 0)
			{
				outWriter.write(obest.get(0));
				for(int no=1; no<obest.size(); no++)
				{
					outWriter.write(" ");
					outWriter.write(obest.get(no));
				}
			}
			outWriter.newLine();
		}
	}
	
	private void normalizeToken(TokenList lesTokens, float maxi) 
	{
		for (Token t: lesTokens) 
		    t.score -=maxi;
	}
	
	/*public void testLM()
	{
		try
		{
			lm.allocate();
		}
		catch(IOException ioe)
		{
			ioe.printStackTrace();
		}
		System.err.println("MaxDepth : "+lm.getMaxDepth());
		
		//WordSequence ws = WordSequence.getWordSequence(new Word("a", null, false));
		Word[] words = new Word[3];
		words[0] = new Word("a", null, false);
		words[1] = new Word("red", null, false);
		words[2] = new Word("car", null, false);
		
		WordSequence ws = WordSequence.getWordSequence(words);
		
		System.err.println("The wordSequence : "+ws.toText());
		
		float proba = lm.getProbability(ws);
		System.err.println("Prob of "+ws.toText()+" is "+proba);
		
		lm.deallocate();
	}*/
	
	
}
