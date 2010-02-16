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
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.regex.Pattern;
public class Graph
{
	public static final String null_node = "NULL";
	private final String node_prefix = "I=";
	private final String link_prefix = "J=";

	private final String CN_NAME_PREFIX = "name";
	private final String CN_NUMALIGNS_PREFIX = "numaligns";
	private final String CN_ALIGN_PREFIX = "align";

	public Vector<Node> nodes = null;
	Node firstNode = null, lastNode = null;
	Integer nbLinks;
	Integer ids; // for word ids
	Integer nodeIds, linkIds;
	Hashtable<String, Integer> wordsID = null;
	Hashtable<Integer, String> idToWord = null;
	Hashtable<Float, Node> timeToNode = null;
	Hashtable<Integer, Node> idToNode = null;
	public Graph()
	{
		init();
	}

	public Graph(ArrayList<ArrayList<Comparable<String>>> cn)
	{
		if (cn.size() == 0)
		{
			System.err.println("Impossible to create empty graph ... exiting");
			System.exit(0);
		}

		float st = 0.0f;
		float inc = 1.0f;
		int wID;
		float score = 0.0f;
		init();
		Node startNode = createNode(st), endNode = null;
		firstNode = startNode;
		st += inc;

		for (ArrayList<Comparable<String>> mesh : cn)
		{
			endNode = createNode(st);
			st += inc;

			score = 1.0f / (float) mesh.size();
			for (Comparable<String> w : mesh)
			{
				wID = createWord((String) w);
				createLink(startNode, endNode, score, score, wID);
			}

			startNode = endNode;
		}
		lastNode = endNode;
	}

	public Graph(ArrayList<ArrayList<ArrayList<Comparable<String>>>> aligns,
			ArrayList<ArrayList<ArrayList<Float>>> aligns_scores, Float[] priors)
	{
		init();
		Node curNode;
		Vector<Node> precNodes = new Vector<Node>(aligns.size());
		int wID;
		float st = 0.0f;
		firstNode = createNode(st++);
		int pos = 0;
		boolean over = false;
		if (aligns.size() != aligns_scores.size() || aligns.size() != priors.length)
		{
			System.err.println("Graph() : number of systems different from number of scores ... exiting");
			return;
		}
		for (int i = 0; i < aligns.size(); i++) // foreach backbone system
		{
			curNode = createNode(st++);
			// link NULL word, probability of backbone
			createLink(firstNode, curNode, priors[i], priors[i], 0);
			// System.err.println("Le precNode du CN "+i+" devient "+curNode.time);
			precNodes.add(i, curNode);
		}
		// int b = 0;
		while (!over)
		{
			// System.err.println("******boucle "+b); b++;
			over = true;
			int i = 0;
			for(int nb=0; nb<aligns.size(); nb++)
			//for (ArrayList<ArrayList<Comparable<String>>> curCN : aligns) // foreach backbone system
			{
				ArrayList<ArrayList<Comparable<String>>> curCN = aligns.get(nb);
				// System.err.println("***********system "+i+" precNode = "+precNodes.elementAt(i).time+" "+precNodes.size());
				if (pos < curCN.size())
				{
					ArrayList<ArrayList<Float>> curCN_scores = aligns_scores.get(nb);
					// System.err.println("*************il convient ");
					over = false;
					curNode = createNode(st++);
					// System.err.println("*************apres createNode pos = "+pos);
					// copy all nextLinks of CN between precNodes[i] and curNode
					// System.err.println("************* nb link = "+curGraph.nodes.elementAt(pos).nextLinks.size());
					
					ArrayList<Comparable<String>> mesh = curCN.get(pos);
					ArrayList<Float> mesh_scores = curCN_scores.get(pos);
					
					for(int nw=0; nw < curCN.get(pos).size(); nw++)
					//for (Comparable<String> w : curCN.get(pos))
					{
						// System.err.println("*************le link "+l);
						// System.err.println("*************le link : "+curGraph.idToWord.get(l.wordId);
						wID = createWord((String)mesh.get(nw));
						// System.err.println("Importation du mot "+curGraph.idToWord.get(l.wordId)+" donne ID="+wID);
						createLink(precNodes.elementAt(i), curNode, mesh_scores.get(nw), mesh_scores.get(nw), wID);
						// System.err.println("sys "+i+" createLink : "+precNodes.elementAt(i).time+" "+curNode.time);
					}
					precNodes.removeElementAt(i);
					precNodes.add(i, curNode);
					// System.err.println("BOUCLE : Le precNode du CN " + i +
					// " devient " + curNode.time);
					i++;
				}
			}
			pos++;
			// if(over) System.err.println("on a fini !!");
		}
		lastNode = createNode(st);
		for (int i = 0; i < aligns.size(); i++) // foreach backbone system
		{
			// link NULL word, probability of backbone
			// System.err.println("-------le link : "+precNodes.elementAt(i).time+" <. "+lastNode.time);
			createLink(precNodes.elementAt(i), lastNode, 1.0, 1.0, 0);
		}
		// System.err.println("Graph::Graph() : END");
	}

	public Graph(Vector<Graph> vec, Vector<Double> scores)
	{
		// System.err.println("Graph::Graph() : START");
		init();
		Node curNode;
		Vector<Node> precNodes = new Vector<Node>(vec.size());
		int wID;
		float st = 0.0f;
		firstNode = createNode(st++);
		int pos = 0;
		boolean over = false;
		if (vec.size() != scores.size())
		{
			System.err.println("Graph() : number of systems different from number of scores ... exiting");
			return;
		}
		for (int i = 0; i < scores.size(); i++) // foreach backbone system
		{
			curNode = createNode(st++);
			// link NULL word, probability of backbone
			createLink(firstNode, curNode, scores.elementAt(i), scores.elementAt(i), 0);
			// System.err.println("Le precNode du CN "+i+" devient "+curNode.time);
			precNodes.add(i, curNode);
		}
		// int b = 0;
		while (!over)
		{
			// System.err.println("******boucle "+b); b++;
			over = true;
			int i = 0;
			for (Graph curGraph : vec) // foreach backbone system
			{
				// System.err.println("***********system "+i+" precNode = "+precNodes.elementAt(i).time+" "+precNodes.size());
				if (pos < curGraph.nodes.size() - 1)
				{
					// System.err.println("*************il convient ");
					over = false;
					curNode = createNode(st++);
					// System.err.println("*************apres createNode pos = "+pos);
					// copy all nextLinks of CN between precNodes[i] and curNode
					// System.err.println("************* nb link = "+curGraph.nodes.elementAt(pos).nextLinks.size());
					for (Link l : curGraph.nodes.elementAt(pos).nextLinks)
					{
						// System.err.println("*************le link "+l);
						// System.err.println("*************le link : "+curGraph.idToWord.get(l.wordId);
						wID = createWord(curGraph.idToWord.get(l.wordId));
						// System.err.println("Importation du mot "+curGraph.idToWord.get(l.wordId)+" donne ID="+wID);
						createLink(precNodes.elementAt(i), curNode, l.posterior, l.posterior, wID);
						// System.err.println("sys "+i+" createLink : "+precNodes.elementAt(i).time+" "+curNode.time);
					}
					precNodes.removeElementAt(i);
					precNodes.add(i, curNode);
					// System.err.println("BOUCLE : Le precNode du CN " + i +
					// " devient " + curNode.time);
					i++;
				}
			}
			pos++;
			// if(over) System.err.println("on a fini !!");
		}
		lastNode = createNode(st);
		for (int i = 0; i < scores.size(); i++) // foreach backbone system
		{
			// link NULL word, probability of backbone
			// System.err.println("-------le link : "+precNodes.elementAt(i).time+" <. "+lastNode.time);
			createLink(precNodes.elementAt(i), lastNode, 1.0, 1.0, 0);
		}
		// System.err.println("Graph::Graph() : END");
	}

	public void init()
	{
		// System.err.println("Graph::init START : ");

		// if (nodes != null) {
		// System.err.println("Graph:init : clearing nodes "); nodes.clear();}
		// else { System.err.println("Graph:init : allocating nodes "); nodes =
		// new Vector<Node>();}
		if (nodes != null)
		{
			nodes.clear();
		}
		else
		{
			nodes = new Vector<Node>();
		}

		firstNode = lastNode = null;
		nbLinks = 0;

		// if(wordsID != null) {
		// System.err.println("Graph:init : clearing wordsId ");
		// wordsID.clear(); }
		// else { System.err.println("Graph:init : allocating wordsId ");
		// wordsID = new Hashtable<String, Integer>(); }
		if (wordsID != null)
		{
			wordsID.clear();
		}
		else
		{
			wordsID = new Hashtable<String, Integer>();
		}

		// if(idToWord != null) {
		// System.err.println("Graph:init : clearing idToWord ");
		// idToWord.clear();}
		// else { System.err.println("Graph:init : allocating idToWord ");
		// idToWord = new Hashtable<Integer, String>(); }
		if (idToWord != null)
		{
			idToWord.clear();
		}
		else
		{
			idToWord = new Hashtable<Integer, String>();
		}

		ids = 0; // for word ids
		createWord(null_node);
		nodeIds = 0;

		// if(timeToNode != null) {
		// System.err.println("Graph:init : clearing timeToNode ");
		// timeToNode.clear();}
		// else { System.err.println("Graph:init : allocating timeToNode ");
		// timeToNode = new Hashtable<Float, Node>();}
		if (timeToNode != null)
		{
			timeToNode.clear();
		}
		else
		{
			timeToNode = new Hashtable<Float, Node>();
		}

		// if(idToNode != null) {
		// System.err.println("Graph:init : clearing idToNode ");
		// idToNode.clear(); }
		// else { System.err.println("Graph:init : allocating idToNode ");
		// idToNode = new Hashtable<Integer, Node>(); }
		if (idToNode != null)
		{
			idToNode.clear();
		}
		else
		{
			idToNode = new Hashtable<Integer, Node>();
		}

		linkIds = 0;
		nbLinks = 0;
		// System.err.println("Graph END");
	}

	private Link createLink(Node startNode, Node endNode, double probability, Integer wID)
	{
		nbLinks++;
		return new Link(startNode, endNode, probability, probability, wID, linkIds++);
	}
	private Link createLink(Node startNode, Node endNode, double likelihood, double probability, int wID)
	{
		nbLinks++;
		return new Link(startNode, endNode, likelihood, probability, wID, linkIds++);
	}
	public void deleteLink(Link l)
	{
		nbLinks--;
		l = null;
	}
	private int createWord(String w)
	{
		Integer wID;
		if (wordsID.contains(w)) // the word is known, get its ID
		{
			// cout << "Known word '" << word << "' ... id=" << wordsID[word] <<
			// endl;
			wID = wordsID.get(w); // on recupere son id
		}
		else
		{
			// add word int the map
			// cout << "Adding word >" << word << "< in the map, id=" << Ids <<
			// " ... ";
			String s = new String(w);
			Integer i = new Integer(ids);
			wordsID.put(s, i);
			wID = i;
			idToWord.put(i, s);
			ids++;
			// cout << "OK -" << wordsID[s] << "-" << endl;
		}
		return wID;
	}

	private Node createNode(float time)
	{
		Node node = null;
		if (timeToNode.contains(time))
		{
			node = timeToNode.get(time);
			// System.err.println("Graph::createNode : on connait le node "+time);
		}
		else
		{
			// System.err.println("Graph::createNode : Ajout du node id="+nodeIds+" t="+time);
			node = new Node(nodeIds, time);
			timeToNode.put(time, node);
			idToNode.put(nodeIds, node);
			nodes.add(node);
			nodeIds++;
		}
		return node;
	}

	public void readHTK(String file)
	{
		String line = null;
		// cerr << "********readHTK : START" << endl;
		try
		{
			BufferedReader reader = new BufferedReader(new FileReader(file));
			while ((line = reader.readLine()) != null)
			{
				if (line.length() > 0)
				{
					if (line.startsWith(node_prefix))
					{
						parseNode(line);
					}
					else if (line.startsWith(link_prefix))
					{
						parseLink(line);
					}
					else
					{
						// System.err.println("Ignoring line >" + line + "<");
					}
				}
			}
		}
		catch (IOException ioe)
		{
			System.err.println("I/O erreur durant file" + file + " " + ioe);
		}
		// cerr << "********load : END" << endl;
	}
	public void printHTK(String fileOut)
	{
		BufferedWriter writer = null;
		try
		{
			writer = new BufferedWriter(new FileWriter(fileOut));
			writer.write("# Header");
			writer.newLine();
			writer.write("VERSION=1.0");
			writer.newLine();
			writer.write("UTTERANCE=utt_id");
			writer.newLine();
			writer.write("#Size definition");
			writer.newLine();
			writer.write("N=" + nodes.size() + "\tL=" + nbLinks);
			writer.newLine();
			writer.write("# Node definitions");
			writer.newLine();
			for (Node n : nodes)
			{
				writer.write("I=" + n.id + "\tt=" + n.time);
				writer.newLine();
			}
			writer.write("# Link definitions");
			writer.newLine();
			int j = 0;
			for (Node n : nodes)
			{
				for (Link l : n.nextLinks)
				{
					writer.write("J=" + j + "\tS=" + l.startNode.id + "\tE=" + l.endNode.id + "\tW="
							+ idToWord.get(l.wordId) + "\ta=" + l.posterior);
					writer.newLine();
					j++;
				}
			}
			writer.close();
		}
		catch (IOException ioe)
		{
			ioe.printStackTrace();
		}
	}
	private void parseNode(String line)
	{
		// System.err.println("********parseNode : START : >"+line+"<");
		Double posterior = 0.0;
		Float time = 0.0f;
		Pattern m = Pattern.compile("\\t");
		Pattern c = Pattern.compile("=");
		String fields[] = m.split(line);

		for (int i = 0; i < fields.length; i++)
		{
			// System.err.println("********parseNode : field["+i+"] : >"+fields[i]+"<");
			String t[] = c.split(fields[i]);
			if ("P".equals(t[0]) || "p".equals(t[0]))
			{
				posterior = Double.parseDouble(t[1]);
			}
			else if ("T".equals(t[0]) || "t".equals(t[0]))
			{
				time = Float.parseFloat(t[1]);
			}
		}
		Node n = createNode(time);
		if (firstNode == null)
			firstNode = n;
		lastNode = n;
	}
	private void parseLink(String line)
	{
		// System.err.println("********parseLink : START : >"+line+"<");

		String word;
		int startNode = 0, endNode = 0;
		double lmProb = 1;
		double prob = lmProb;
		Integer wID = -1, linkId;

		if (line == null || line.equals(""))
			return;

		Pattern m = Pattern.compile("\\t");
		Pattern c = Pattern.compile("=");
		String fields[] = m.split(line);

		for (int i = 0; i < fields.length; i++)
		{
			String t[] = c.split(fields[i]);
			if ("J".equals(t[0]) || "j".equals(t[0]))
			{
				linkId = Integer.parseInt(t[1]);
			}
			else if ("S".equals(t[0]) || "s".equals(t[0]))
			{
				startNode = Integer.parseInt(t[1]);
			}
			else if ("E".equals(t[0]) || "e".equals(t[0]))
			{
				endNode = Integer.parseInt(t[1]);
			}
			else if ("A".equals(t[0]) || "a".equals(t[0]))
			{
				prob = Double.parseDouble(t[1]);
			}
			else if ("W".equals(t[0]) || "w".equals(t[0]))
			{
				word = t[1];
				wID = createWord(word);
			}
		}

		// System.err.println(""+startNode+" -> "+idToNode.get(startNode).time+" et "+endNode+" -> "+idToNode.get(endNode).time);

		if (idToNode.get(startNode).time != idToNode.get(endNode).time)
		{
			createLink(idToNode.get(startNode), idToNode.get(endNode), prob, wID);
		}
		else
		{
			System.err.println(idToNode.get(startNode).time + " == " + idToNode.get(endNode).time
					+ " : aucun Link cree\n\t pour la ligne " + line);
		}
		// System.err.println("********parseLink : END");
	}
	public void readCN(String file)
	{
		String line = null;
		double score;
		int id, wID;
		// cerr << "********readHTK : START" << endl;
		try
		{
			BufferedReader reader = new BufferedReader(new FileReader(file));
			while ((line = reader.readLine()) != null)
			{
				if (line.length() > 0)
				{
					if (line.startsWith(CN_NAME_PREFIX))
					{
					}
					else if (line.startsWith(CN_NUMALIGNS_PREFIX))
					{
					}
					else if (line.startsWith(CN_ALIGN_PREFIX))
					{
						StringTokenizer st = new StringTokenizer(line);
						String s = st.nextToken();
						if (s != "align")
						{
							System.exit(0);
						}
						s = st.nextToken();
						try
						{
							id = Integer.parseInt(s);
						}
						catch (NumberFormatException nfe)
						{
							nfe.printStackTrace();
						}

						while (st.hasMoreTokens())
						{
							// un mot un score .. etc
							s = st.nextToken();
							wID = createWord(s);
							s = st.nextToken();
							try
							{
								score = Double.parseDouble(s);
							}
							catch (NumberFormatException nfe)
							{
								nfe.printStackTrace();
							}
						}
					}
					else
					{
						// System.err.println("Ignoring line >" + line + "<");
					}
				}
			}
		}
		catch (IOException ioe)
		{
			System.err.println("I/O erreur durant file" + file + " " + ioe);
		}
		// cerr << "********load : END" << endl;
	}
}
