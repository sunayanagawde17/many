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
package com.bbn.mt.terp;

import java.util.ArrayList;
import java.util.HashMap;

public class TERalignmentCN extends TERalignment
{
	public ArrayList<ArrayList<Comparable<String>>> cn = null;
	public ArrayList<ArrayList<Float>> cn_scores = null;
	public ArrayList<Comparable<String>[]> hyps = null ;
	public String[] orig_hyps = null;

	public ArrayList<float[]> hyps_scores = null ;
	public String[] orig_hyps_scores = null;
	public float[] aftershift_scores;

	public float null_score = 0.0f;
	private boolean DEBUG = false;
	public TERalignmentCN(TERcost costfunc)
	{
		super(costfunc);
	}

	private String prtShift(Comparable[][] ref, TERshift[] allshifts)
	{
		String to_return = "";
		int ostart, oend, odest;
		int nstart, nend;
		int dist;
		String direction = "";
		if (allshifts != null)
		{
			for (int i = 0; i < allshifts.length; ++i)
			{
				TERshift[] oneshift = new TERshift[1];
				ostart = allshifts[i].start;
				oend = allshifts[i].end;
				odest = allshifts[i].newloc;
				if (odest >= oend)
				{
					// right
					nstart = odest + 1 - allshifts[i].size();
					nend = nstart + allshifts[i].size() - 1;
					dist = odest - oend;
					direction = "right";
				}
				else
				{
					// left
					nstart = odest + 1;
					nend = nstart + allshifts[i].size() - 1;
					dist = ostart - odest - 1;
					direction = "left";
				}
				to_return += "\nShift " + allshifts[i].shifted + " " + dist
					+ " words " + direction;
				oneshift[0] = new TERshift(ostart, oend, allshifts[i].moveto,
						odest);
				to_return += getPraStr(ref, allshifts[i].aftershift,
						allshifts[i].alignment, oneshift, true);
			}
			to_return += "\n";
		}
		return to_return;
	}

	private String getPraStr(Comparable[][] ref, Comparable[] aftershift,
			char[] alignment, TERshift[] allshifts, boolean shiftonly)
	{
		String to_return = "";
		String rstr = "";
		String hstr = "";
		String estr = "";
		String sstr = "";
		HashMap align_info = new HashMap();
		ArrayList shift_dists = new ArrayList();
		int anum = 1;
		int ind_start = 0;
		int ind_end = 1;
		int ind_from = 2;
		int ind_in = 3;
		int ostart, oend, odest;
		int slen = 0;
		int nstart, nend, nfrom, dist;
		int non_inserr = 0;
		if (allshifts != null)
		{
			for (int i = 0; i < allshifts.length; ++i)
			{
				ostart = allshifts[i].start;
				oend = allshifts[i].end;
				odest = allshifts[i].newloc;
				slen = allshifts[i].size();
				if (odest >= oend)
				{
					// right
					nstart = odest + 1 - slen;
					nend = nstart + slen - 1;
					nfrom = ostart;
					dist = odest - oend;
				}
				else
				{
					// left
					nstart = odest + 1;
					nend = nstart + slen - 1;
					nfrom = ostart + slen;
					dist = (ostart - odest - 1) * -1;
				}
				// dist =
				// (allshifts[i].leftShift())?-1*allshifts[i].distance():allshifts[i].distance();
				shift_dists.add(dist);
				// System.out.println("[" + hyp[ostart] + ".." + hyp[oend] +
				// " are shifted " + dist);
				if (anum > 1)
					performShiftArray(align_info, ostart, oend, odest,
							alignment.length);
				Object val = align_info.get(nstart + "-" + ind_start);
				if (val == null)
				{
					ArrayList al = new ArrayList();
					al.add(anum);
					align_info.put(nstart + "-" + ind_start, al);
				}
				else
				{
					ArrayList al = (ArrayList) val;
					al.add(anum);
				}
				val = align_info.get(nend + "-" + ind_end);
				if (val == null)
				{
					ArrayList al = new ArrayList();
					al.add(anum);
					align_info.put(nend + "-" + ind_end, al);
				}
				else
				{
					ArrayList al = (ArrayList) val;
					al.add(anum);
				}
				val = align_info.get(nfrom + "-" + ind_from);
				if (val == null)
				{
					ArrayList al = new ArrayList();
					al.add(anum);
					align_info.put(nfrom + "-" + ind_from, al);
				}
				else
				{
					ArrayList al = (ArrayList) val;
					al.add(anum);
				}
				/*
				 * val = align_info.get("60-"+ind_start); if(val != null)
				 * System.out.println(((ArrayList) val).get(0)); else
				 * System.out.println("empty");
				 * 
				 * System.out.println("nstart: " + nstart + ", nend:" + nend +
				 * "," + ostart +"," + oend +","+ odest + "," +
				 * align_info.size());
				 */
				if (slen > 0)
				{
					for (int j = nstart; j <= nend; ++j)
					{
						val = align_info.get(j + "-" + ind_in);
						if (val == null)
						{
							ArrayList al = new ArrayList();
							al.add(anum);
							align_info.put(j + "-" + ind_in, al);
						}
						else
						{
							ArrayList al = (ArrayList) val;
							al.add(anum);
						}
					}
				}
				anum++;
			}
		}
		int hyp_idx = 0;
		int ref_idx = 0;
		Object val = null;
		if (alignment != null)
		{
			for (int i = 0; i < alignment.length; ++i)
			{
				String shift_in_str = "";
				String ref_wd = (ref_idx < ref.length) ? String
					.valueOf(TERutilities.join("|", ref[ref_idx])) : "";
				String hyp_wd = (hyp_idx < hyp.length) ? String
					.valueOf(aftershift[hyp_idx]) : "";
				int l = 0;
				if (alignment[i] != 'D')
				{
					val = align_info.get(hyp_idx + "-" + ind_from);
					if (val != null)
					{
						// System.out.println("hyp_idx: " + hyp_idx + "," +
						// hyp_wd);
						ArrayList list = (ArrayList) val;
						for (int j = 0; j < list.size(); ++j)
						{
							String s = "" + list.get(j);
							hstr += " @";
							rstr += "  ";
							estr += "  ";
							sstr += " " + s;
							for (int k = 1; k < s.length(); ++k)
							{
								hstr += " ";
								rstr += " ";
								estr += " ";
							}
						}
					}
					val = align_info.get(hyp_idx + "-" + ind_start);
					if (val != null)
					{
						// System.out.println("hyp_idx: " + hyp_idx + "," +
						// hyp_wd + "," + alignment.length);
						ArrayList list = (ArrayList) val;
						for (int j = 0; j < list.size(); ++j)
						{
							String s = "" + list.get(j);
							hstr += " [";
							rstr += "  ";
							estr += "  ";
							sstr += " " + s;
							for (int k = 1; k < s.length(); ++k)
							{
								hstr += " ";
								rstr += " ";
								estr += " ";
							}
						}
					}
					if (slen > 0)
					{
						val = align_info.get(hyp_idx + "-" + ind_in);
						if (val != null)
							shift_in_str = TERsgml.join(",", (ArrayList) val);
						// if(val != null) System.out.println("shiftstr: " +
						// ref_idx + "," + hyp_idx + "-" + ind_in + ":" +
						// shift_in_str);
					}
				}
				switch (alignment[i])
				{
					case ' ' :
						l = Math.max(ref_wd.length(), hyp_wd.length());
						hstr += " " + hyp_wd;
						rstr += " " + ref_wd;
						estr += " ";
						sstr += " ";
						for (int j = 0; j < l; ++j)
						{
							if (hyp_wd.length() <= j)
								hstr += " ";
							if (ref_wd.length() <= j)
								rstr += " ";
							estr += " ";
							sstr += " ";
						}
						hyp_idx++;
						ref_idx++;
						non_inserr++;
						break;
					case 'S' :
					case 'Y' :
					case 'T' :
						l = Math.max(ref_wd.length(), Math.max(hyp_wd.length(),
									Math.max(1, shift_in_str.length())));
						hstr += " " + hyp_wd;
						rstr += " " + ref_wd;
						if (hyp_wd.equals("") || ref_wd.equals(""))
							System.out.println("unexpected empty: sym="
									+ alignment[i] + " hyp_wd=" + hyp_wd
									+ " ref_wd=" + ref_wd + " i=" + i
									+ " alignment=" + TERutilities.join(",", alignment));
						estr += " " + alignment[i];
						sstr += " " + shift_in_str;
						for (int j = 0; j < l; ++j)
						{
							if (hyp_wd.length() <= j)
								hstr += " ";
							if (ref_wd.length() <= j)
								rstr += " ";
							if (j > 0)
								estr += " ";
							if (j >= shift_in_str.length())
								sstr += " ";
						}
						ref_idx++;
						hyp_idx++;
						non_inserr++;
						break;
					case 'P' :
						int min = alignment_r[i];
						if (alignment_h[i] < min)
							min = alignment_h[i];
						for (int k = 0; k < min; k++)
						{
							ref_wd = (ref_idx < ref.length) ? String
								.valueOf(ref[ref_idx]) : "";
							hyp_wd = (hyp_idx < hyp.length) ? String
								.valueOf(aftershift[hyp_idx]) : "";
							// System.out.println("Saying that " + ref_wd +
							// " & " + hyp_wd + " are P. " + alignment_r[i] +
							// " " +
							// alignment_h[i]);
							l = Math.max(ref_wd.length(), Math.max(hyp_wd
										.length(), Math.max(1, shift_in_str
											.length())));
							hstr += " " + hyp_wd;
							rstr += " " + ref_wd;
							if (hyp_wd.equals("") || ref_wd.equals(""))
								System.out.println("unexpected empty: sym="
										+ alignment[i] + " hyp_wd=" + hyp_wd
										+ " ref_wd=" + ref_wd + " i=" + i
										+ " alignment=" + TERutilities.join(",", alignment));
							estr += " " + alignment[i];
							sstr += " " + shift_in_str;
							for (int j = 0; j < l; ++j)
							{
								if (hyp_wd.length() <= j)
									hstr += " ";
								if (ref_wd.length() <= j)
									rstr += " ";
								if (j > 0)
									estr += " ";
								if (j >= shift_in_str.length())
									sstr += " ";
							}
							ref_idx++;
							hyp_idx++;
							non_inserr++;
						}
						if (alignment_h[i] > alignment_r[i])
						{
							for (int k = alignment_r[i]; k < alignment_h[i]; k++)
							{
								ref_wd = (ref_idx < ref.length) ? String
									.valueOf(TERutilities.join("|", ref[ref_idx])) : "";
								hyp_wd = (hyp_idx < hyp.length) ? String
									.valueOf(aftershift[hyp_idx]) : "";
								l = Math.max(hyp_wd.length(), shift_in_str
										.length());
								hstr += " " + hyp_wd;
								rstr += " ";
								estr += " P";
								sstr += " " + shift_in_str;
								for (int j = 0; j < l; ++j)
								{
									rstr += "*";
									if (j >= hyp_wd.length())
										hstr += " ";
									if (j > 0)
										estr += " ";
									if (j >= shift_in_str.length())
										sstr += " ";
								}
								hyp_idx++;
							}
						}
						else if (alignment_r[i] > alignment_h[i])
						{
							for (int k = alignment_h[i]; k < alignment_r[i]; k++)
							{
								ref_wd = (ref_idx < ref.length) ? String
									.valueOf(TERutilities.join("|", ref[ref_idx])) : "";
								hyp_wd = (hyp_idx < hyp.length) ? String
									.valueOf(aftershift[hyp_idx]) : "";
								l = ref_wd.length();
								hstr += " ";
								rstr += " " + ref_wd;
								estr += " P";
								sstr += " ";
								for (int j = 0; j < l; ++j)
								{
									hstr += "*";
									if (j > 0)
										estr += " ";
									sstr += " ";
								}
								ref_idx++;
								non_inserr++;
							}
						}
						break;
					case 'D' :
						l = ref_wd.length();
						hstr += " ";
						rstr += " " + ref_wd;
						estr += " D";
						sstr += " ";
						for (int j = 0; j < l; ++j)
						{
							hstr += "*";
							if (j > 0)
								estr += " ";
							sstr += " ";
						}
						ref_idx += alignment_r[i];
						hyp_idx += alignment_h[i];
						non_inserr++;
						break;
					case 'I' :
						l = Math.max(hyp_wd.length(), shift_in_str.length());
						hstr += " " + hyp_wd;
						rstr += " ";
						estr += " I";
						sstr += " " + shift_in_str;
						for (int j = 0; j < l; ++j)
						{
							rstr += "*";
							if (j >= hyp_wd.length())
								hstr += " ";
							if (j > 0)
								estr += " ";
							if (j >= shift_in_str.length())
								sstr += " ";
						}
						hyp_idx++;
						break;
				}
				if (alignment[i] != 'D')
				{
					val = align_info.get((hyp_idx - 1) + "-" + ind_end);
					if (val != null)
					{
						ArrayList list = (ArrayList) val;
						for (int j = 0; j < list.size(); ++j)
						{
							String s = "" + list.get(j);
							hstr += " ]";
							rstr += "  ";
							estr += "  ";
							sstr += " " + s;
							for (int k = 1; k < s.length(); ++k)
							{
								hstr += " ";
								rstr += " ";
								estr += " ";
							}
						}
					}
				}
			}
		}
		// if(non_inserr != ref.length && ref.length > 1)
		// System.out.println("** Error, unmatch non-insertion erros " +
		// non_inserr +
		// " and reference length " + ref.length );
		String indent = "";
		if (shiftonly)
			indent = " ";
		to_return += "\n" + indent + "REF: " + rstr;
		to_return += "\n" + indent + "HYP: " + hstr;
		if (!shiftonly)
		{
			to_return += "\n" + indent + "EVAL:" + estr;
			to_return += "\n" + indent + "SHFT:" + sstr;
		}
		to_return += "\n";
		return to_return;
	}

	public void buildCN()
	{
		if(DEBUG)
		{
			System.out.println("Size alignment : "+alignment.length);
			for (int i = 0; i < alignment.length; i++)
			{
				System.out.println("al["+i+"] "+alignment[i]);
			}
		}
		
		//System.err.println("\n---------------- buildCN : le cn :\n "+toCNString()+" ---------------- \n");

		//suppose that ref, hyp and alignment are specified
		int hi=0, pos=0; //ri=0;
		for (int i = 0; i < alignment.length; i++)
		{
			switch(alignment[i])
			{
				case ' ': // correct
					addUnique(pos, hi);
					pos++;
					//ri++;
					hi++;
					break;
				case 'I' : // insertions
					cn.add(pos, new ArrayList<Comparable<String>>());
					cn_scores.add(pos, new ArrayList<Float>());
					cn.get(pos).add(aftershift[hi]);
					cn_scores.get(pos).add(aftershift_scores[hi]);
					cn.get(pos).add("NULL");
					cn_scores.get(pos).add(null_score);
					pos++;
					//ri++;
					hi++;
					break;
				case 'S' : // shift
				case 'Y' : // synonymes
				case 'T' : // stems
					addUnique(pos, hi);
					pos++;
					hi++;
					//ri++;
					break;  
				case 'P' : // paraphrase
					int hl = alignment_h[i];
					int rl = alignment_r[i];
					if(DEBUG)
					{	System.err.println("Buildcn - paraphrase : ref_len="+rl+" hyp_len="+hl);
						System.err.println("ref :"); 
						for(int t=0; t<hl; t++) 
							System.err.print(""+alignment[t]);
					}
					
					if(hl > rl) // hyp side is longer than ref side of paraphrase
					{
						for(int j=0; j<hl; j++)
						{
							if(j < rl)
							{	
								addUnique(pos, hi);
								pos++;
								//ri++;
								hi++;
							}
							else
							{
								cn.add(pos, new ArrayList<Comparable<String>>());
								cn_scores.add(pos, new ArrayList<Float>());
								cn.get(pos).add(aftershift[hi]);
								cn_scores.get(pos).add(aftershift_scores[hi]);
								cn.get(pos).add("NULL");
								cn_scores.get(pos).add(null_score);
								pos++;
								hi++;
							}	
						}
					}
					else if(rl > hl) // hyp side is shorter than ref side
					{
						for(int j=0; j<rl; j++)
						{
							if(j < hl)
							{
								addUnique(pos, hi);
								pos++;
								//ri++;
								hi++;
							}
							else
							{
								addUniqueNULL(pos);
								pos++;
								//ri++;
							}
						}
					}
					else //equal size
					{
						for(int j=0; j<rl; j++)
						{
							addUniqueNULL(pos);
							pos++;
							//ri++;
							hi++;
						}
					}


					break;
				case 'D' :
					addUniqueNULL(pos);
					pos++;
					//ri++;
					break;
				default :
					System.err.println("Unknown alignment type : "+alignment[i]);
					break;
			}
		}
	}
	
	private void addUnique(int pos, int hi)
	{
		ArrayList<Comparable<String>> mesh = cn.get(pos); 
		Comparable<String> word = aftershift[hi];
		int j;
		if(TERpara.para().get_boolean(TERpara.OPTIONS.CASEON))
		{
			for(j=0; j<mesh.size(); j++)
			{
				String w = (String) mesh.get(j);
				if(w.equals((String) word))
				{
					if(DEBUG)
						System.err.println("proba for word "+w+" was "+cn_scores.get(pos).get(j)+" and becomes "+(cn_scores.get(pos).get(j) + aftershift_scores[hi]));
					cn_scores.get(pos).set(j, cn_scores.get(pos).get(j) + aftershift_scores[hi]);
					return;
				}
			} 
		}
		else
		{
			for(j=0; j<mesh.size(); j++)
			{
				String w = (String) mesh.get(j);
				if(w.equalsIgnoreCase((String) word))
				{
					if(DEBUG)
						System.err.println("proba for word "+w+" was "+cn_scores.get(pos).get(j)+" and becomes "+(cn_scores.get(pos).get(j) + aftershift_scores[hi]));
					cn_scores.get(pos).set(j, cn_scores.get(pos).get(j) + aftershift_scores[hi]);
					return;
				}
			}
		}
		//System.err.println("add word "+word+" with score = "+aftershift_scores[hi]);
		mesh.add(word);
		cn_scores.get(pos).add(aftershift_scores[hi]);
	}
	private void addUniqueNULL(int pos)
	{
		ArrayList<Comparable<String>> mesh = cn.get(pos); 
		Comparable<String> word = "NULL";
		int j;
		if(TERpara.para().get_boolean(TERpara.OPTIONS.CASEON))
		{
			for(j=0; j<mesh.size(); j++)
			{
				String w = (String) mesh.get(j);
				if(w.equals((String) word))
				{
					//System.err.println("proba for word "+w+" was "+cn_scores.get(ri).get(j)+" and becomes "+(cn_scores.get(ri).get(j) + null_score));
					cn_scores.get(pos).set(j, cn_scores.get(pos).get(j) + null_score);
					return;
				}
			}
		}
		else
		{
			for(j=0; j<mesh.size(); j++)
			{
				String w = (String) mesh.get(j);
				if(w.equalsIgnoreCase((String) word))
				{
					//System.err.println("proba for word "+w+" was "+cn_scores.get(ri).get(j)+" and becomes "+(cn_scores.get(ri).get(j) + null_score));
					cn_scores.get(pos).set(j, cn_scores.get(pos).get(j) + null_score);
					return;
				}
			}
		}
		mesh.add(word);
		cn_scores.get(pos).add(null_score);
	}

	public void addHyp(Comparable<String>[] h)
	{
		if(hyps == null)
			hyps = new ArrayList<Comparable<String>[]>();
		hyps.add(h);
	}

	/*public String getCNString()
	{
		String s = "";

		for(ArrayList<Comparable<String>> mesh : cn)
		{
			boolean first = true;
			for(Comparable<String> word : mesh)
			{
				if(!first)
					s += ",";
				s += word;
				first = false;
			}
			s += " ";
		}

		return s;
	}*/

	public String toCNString()
	{
		StringBuilder s = new StringBuilder("name cn1best\nnumaligns "+cn.size()+"\n\n");

		for(int i=0; i<cn.size(); i++)
		{
			ArrayList<Comparable<String>> mesh = cn.get(i);
			ArrayList<Float> sc = cn_scores.get(i);
			s.append("align "+i+" ");

			for(int j=0; j<mesh.size(); j++)
			{
				s.append((String)mesh.get(j)).append(" ").append(sc.get(j)).append(" ");
			}	
			s.append("\n");
		}
		return s.toString();
	}
	public static String toCNString(ArrayList<ArrayList<Comparable<String>>> cn)
	{
		StringBuilder s = new StringBuilder("name cn1best\nnumaligns "+cn.size()+"\n\n");

		int i=0;
		for(ArrayList<Comparable<String>> mesh : cn)
		{
			s.append("align ").append(i).append(" ");
			i++;
			float proba = 1.0f / (float)mesh.size();
			for(Comparable<String> word : mesh)
			{
				s.append(word).append(" ").append(proba).append(" ");
			}
			s.append("\n");
		}

		return s.toString();
	}

	public String toString()
	{
		String s = "";
		if (orig_ref != null)
			s += "Original Reference: " + orig_ref + "\n";
		if (orig_hyp != null)
			s += "Original Hypothesis: " + orig_hyp + "\n";
		s += "Reference CN : \n" + toCNString();
		s += "\nHypothesis: " + TERutilities.join(" ", hyp)
			+ "\nHypothesis After Shift: " + TERutilities.join(" ", aftershift);
		if (alignment != null)
		{
			s += "\nAlignment: (";
			for (int i = 0; i < alignment.length; i++)
			{
				s += alignment[i];
			}
			s += ")";
		}
		if (allshifts == null)
		{
			s += "\nNumShifts: 0";
		}
		else
		{
			s += "\nNumShifts: " + allshifts.length;
			for (int i = 0; i < allshifts.length; i++)
			{
				s += "\n  " + allshifts[i];
			}
		}
		s += "\nScore: " + this.score() + " (" + this.numEdits + "/"
			+ this.numWords + ")";
		return s;
	}


}
