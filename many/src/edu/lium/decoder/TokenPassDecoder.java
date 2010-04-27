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
import edu.cmu.sphinx.linguist.language.ngram.NetworkLanguageModel;
import edu.cmu.sphinx.linguist.language.ngram.large.LargeNGramModel;
import edu.cmu.sphinx.util.LogMath;
import edu.cmu.sphinx.util.props.Configurable;
import edu.cmu.sphinx.util.props.PropertyException;
import edu.cmu.sphinx.util.props.PropertySheet;
import edu.cmu.sphinx.util.props.S4Boolean;
import edu.cmu.sphinx.util.props.S4Component;
import edu.cmu.sphinx.util.props.S4Double;
import edu.cmu.sphinx.util.props.S4Integer;

public class TokenPassDecoder implements Configurable
{
	/** The property that defines the logMath component. */
	@S4Component(type = LogMath.class)
	public final static String PROP_LOG_MATH = "logMath";
	
	/** The property that defines the network language model component. */
	@S4Component(type = NetworkLanguageModel.class)
	public final static String PROP_LANGUAGE_MODEL_ON_SERVER = "lmonserver";
	
	/** The property that defines the local ngram language model component. */
	@S4Component(type = LargeNGramModel.class)
	public final static String PROP_LANGUAGE_MODEL = "ngramModel";
	
	/** The property that defines the dictionary component. */
	@S4Component(type = SimpleDictionary.class)
	public final static String PROP_DICTIONARY = "dictionary";
	
	/** The property that defines the max number of tokens considered */
	@S4Integer(defaultValue = 1000)
	public final static String PROP_MAX_NB_TOKENS = "max_nb_tokens";

	/** The property that defines the fudge factor */
	@S4Double(defaultValue = 1.0)
	public final static String PROP_FUDGE_FACTOR = "fudge";
	
	/** The property that defines the penalty when crossing null arc */
	@S4Double(defaultValue = 1.0)
	public final static String PROP_NULL_PENALTY = "null_penalty";
	
	/** The property that defines the length penalty */
	@S4Double(defaultValue = 1.0)
	public final static String PROP_LENGTH_PENALTY = "length_penalty";
	
	/** The property that defines the size of the nbest-list */
	@S4Integer(defaultValue = 0)
	public final static String PROP_NBEST_LENGTH = "nbest_length";
	
	/** The property that defines the debugging */
	@S4Boolean(defaultValue = false)
	public final static String PROP_DEBUG = "debug";
	
	/** The property that determines whether to use the local lm or network lm */
	@S4Boolean(defaultValue = false)
	public final static String PROP_USE_NGRAM_LM = "useNGramModel";
	
	private LargeNGramModel languageModel;
	private boolean DEBUG = false;
	private LogMath logMath;
	private Dictionary dictionary;
	private Logger logger;
	private String name;
	private TokenList tokens[];
	private TokenList lastTokens;
	private NetworkLanguageModel networklm;
	private int maxNbTokens;
	private float fudge;
	private float null_penalty;
	private float length_penalty;
	private int nbest_length;
	private boolean useNGramModel;
	
	public void allocate() throws java.io.IOException
	{
		//logger.info("TokenPassDecoder::allocate");
		dictionary.allocate();
		
		if(useNGramModel == true)
		{
			languageModel.allocate();
		}
		else
		{
			networklm.allocate();
		}
		tokens = new TokenList[2];
		for (int i = 0; i < tokens.length; i++)
			tokens[i] = new TokenList();
		lastTokens = new TokenList();
		//logger.info("TokenPassDecoder::allocate OK");
	}
	public void deallocate()
	{  
		dictionary.deallocate();
		
		if(useNGramModel)
			languageModel.deallocate();
		else
			networklm.deallocate();
	}
	
	public void newProperties(PropertySheet ps) throws PropertyException
	{
		logger = ps.getLogger();
		logMath = (LogMath) ps.getComponent(PROP_LOG_MATH);
		dictionary = (Dictionary) ps.getComponent(PROP_DICTIONARY);
		
		useNGramModel = ps.getBoolean(PROP_USE_NGRAM_LM);
		if(useNGramModel)
		{
			languageModel = (LargeNGramModel) ps.getComponent(PROP_LANGUAGE_MODEL);
		}
		else
		{	networklm = (NetworkLanguageModel) ps.getComponent(PROP_LANGUAGE_MODEL_ON_SERVER);
		}
		maxNbTokens = ps.getInt(PROP_MAX_NB_TOKENS);
		fudge = 10.0f * ps.getFloat(PROP_FUDGE_FACTOR);
		null_penalty = ps.getFloat(PROP_NULL_PENALTY);
		length_penalty = 10.0f * ps.getFloat(PROP_LENGTH_PENALTY);
		nbest_length = ps.getInt(PROP_NBEST_LENGTH);
		DEBUG = ps.getBoolean(PROP_DEBUG);
		System.err.println("DEBUG : "+DEBUG);
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
		
		if(DEBUG)
		{
			System.err.println("TokenPassDecoder::decode parameters ");
			System.err.println(" - fudge : "+fudge);
			System.err.println(" - null penalty : " + null_penalty);
			System.err.println(" - length penalty : "+length_penalty);
			System.err.println(" - Max nb tokens : "+maxNbTokens);
			System.err.println(" - N-Best length : "+nbest_length);
		}
		
		int origine = 0, cible = 1;
		int maxHist = 0;
		if(useNGramModel)
			maxHist = languageModel.getMaxDepth();
		else
			maxHist = networklm.getMaxDepth();

		//logger.info("maxHist = "+maxHist);
		ArrayList<Node> nodes = new ArrayList<Node>();
		Node n = lat.firstNode;
		nodes.add(n);
		
		double maxScore = -Double.MAX_VALUE;
		
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
			
			maxScore = -Double.MAX_VALUE;
			
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
						if(Graph.null_node.equals(ws)) //this is a first link !!!
						{
							//ns = WordSequence.getWordSequence(new Word(ws, null, true));
							ns = new WordSequence(new Word[]{new Word(ws, null, true)});
							lmns = new WordSequence(new Word[]{new Word(ws, null, true)});
							//logger.info("... Creating word sequence for first link "+ws+" (null_node) gives -> "+ns.toText()+" [(1) for LM : "+lmns.toText()+" ]");
							//logger.info("l.posterior = "+l.posterior);
							//nscore += logMath.linearToLog(null_penalty); //null_penalty
							nscore += logMath.linearToLog(l.posterior);
						}
						else
						{
							w = dictionary.getWord(ws);
							if(w == ((SimpleDictionary)dictionary).getUnknownWord())
							{
								ns = new WordSequence(new Word[]{new Word(ws, null, false)});
							}
							else
							{
								ns = new WordSequence(new Word[]{w});
							}
							lmns = new WordSequence(new Word[]{w});
							//logger.info("... Creating word sequence with only "+ws+" gives -> "+ns.toText()+" [(2) for LM : "+lmns.toText()+" ]");
							
							float lmscore = 0.0f;
							if(useNGramModel == true)
							{
								lmscore = languageModel.getProbability(ns.withoutWord(Graph.null_node));
								//logger.info("Proba lm : "+lmscore);
							}
							else
							{
								lmscore = networklm.getProbability(lmns.withoutWord(Graph.null_node));
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
							if(useNGramModel == true)
							{
								lmscore = languageModel.getProbability(ns.withoutWord(Graph.null_node));
								//logger.info("Proba lm : "+lmscore);
							}
							else
							{
								lmscore = networklm.getProbability(lmns.withoutWord(Graph.null_node));
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
						logger.info(best.history.getNewestWord().getSpelling() + " " + best.node.id+" time="+best.node.time);
					if(Graph.null_node.equals(best.history.getNewestWord().getSpelling()) == false)
					{	
						obest.add(0, best.history.getNewestWord().getSpelling());
					}
					else if(DEBUG) 
						logger.info("found null_node : "+best.history.getNewestWord().getSpelling());
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
			double max = -Double.MAX_VALUE;
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
					logger.info(best.history.getNewestWord().getSpelling() + " " + best.node.id+" time="+best.node.time);
				if(Graph.null_node.equals(best.history.getNewestWord().getSpelling()) == false)
				{	
					obest.add(0, best.history.getNewestWord().getSpelling());
				}
				else if(DEBUG) logger.info("un null_node : "+best.history.getNewestWord().getSpelling());
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
	
	private void normalizeToken(TokenList lesTokens, double maxi) 
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
