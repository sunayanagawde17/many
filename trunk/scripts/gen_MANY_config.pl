#!/usr/bin/perl

use strict;
use warnings;

my $i=0;
my $fudge=0.1;
my $null=0.3;
my $length=0.1;

my $priors = "0.3333 0.3333 0.3333";
my $costs = "1.0 1.0 1.0 1.0 1.0 0.0 1.0";
my $wordnet = "/opt/mt/WordNet-3.0/dict/";
my $paraphrases = "/opt/mt/terp/terp.v1/data/phrases.db";
my $stoplist = "/opt/mt/terp/terp.v1/data/shift_word_stop_list.txt";
my $terpparams = "terp.params";
my $lmserverhost = "n2";
my $lmserverport = "1234";
my $languagemodel = "./lm.DMP32";
my $nb_sys = 3;
my $nb_threads = 1;
my $useNGramLM = 0;
my $max_nb_tokens = 1000;
my $nbest_length = 0;
my $debug_decode = "false";

my $usage = "gen_MANY_config.pl \
-nbsys #_of_systems : 		default is $nb_sys\
-c costs            : 		default is \"$costs\" \
-dd                 :       default is $debug_decode \
-f fudge            : 		default is $fudge\
-h LM server host   : 		default is $lmserverhost (this set the uselm3g variable to FALSE)\
-port LM server port   :	default is $lmserverport\
-ngramlm n-gram LM     :	default is $languagemodel, format is ARPA, DMP or DMP32 (this set the useNGramLM variable to TRUE)\
-l length_penalty   : 		default is $length\
-m multithread      :       default is \"$nb_threads\" \
-maxtok max_nb_tokens :     default is $max_nb_tokens\
-n null_penalty     : 		default is $null\
-nbest nbest_list_size : 	default is $nbest_length\
-para paraphrases   : 		default is $paraphrases \
-p priors           : 		default is \"$priors\" \
-s shift_word_stop_list : 	default is $stoplist \
-t terp params file : 		default is $terpparams \
-w wordnet          : 		default is $wordnet \
-help               :           print this help and exit \
";

if ( $#ARGV < 1 )
{
	print STDERR "Usage : $usage";
	exit();
}

while( $i <= $#ARGV)
{
#	print STDERR "arg : $ARGV[$i] ";
	for ($ARGV[$i]) 
	{
#		print STDERR "arg : $_\n";
		
		/-dd$/ && do {
					#print "debug decode is ON\n";
					$debug_decode = "true";
					last;
		};
		/-nbsys$/ && do { $i++;
					 #print "# of systems : $ARGV[$i]\n";
					 $nb_sys=$ARGV[$i];
					 last;
		};
		/-maxtok$/ && do { $i++;
					 #print "max # of tokens : $ARGV[$i]\n";
					 $max_nb_tokens=$ARGV[$i];
					 last;
		};
		/-nbest$/ && do { $i++;
					 #print "nbest-list size : $ARGV[$i]\n";
					 $nbest_length=$ARGV[$i];
					 last;
		};
		/-f$/ && do { $i++;
					 #print "fudge : $ARGV[$i]\n";
					 $fudge=$ARGV[$i];
					 last;
		};
		/-h$/ && do { $i++;
					 #print "lmserverhost : $ARGV[$i]\n";
					 $lmserverhost=$ARGV[$i];
					 last;
		};
		/-port$/ && do { $i++;
					 #print "lmserverport : $ARGV[$i]\n";
					 $lmserverport=$ARGV[$i];
					 last;
		};
		/-ngramlm$/ && do { $i++;
					 #print "languagemodel : $ARGV[$i]\n";
					 $languagemodel=$ARGV[$i];
					 $useNGramLM = 1;
					 last;
		};
		/-m$/ && do { $i++;
					 #print "# of threads : $ARGV[$i]\n";
					 $nb_threads=$ARGV[$i];
					 last;
		};
		/-maxtok$/ && do { $i++;
					#print "Max # of tokens : $ARGV[$i]\n";
					$max_nb_tokens=$ARGV[$i];
                    last;
		};
	 	/-n$/ && do { $i++;
					 #print "null : $ARGV[$i]\n";
					 $null=$ARGV[$i]; 
					 last; 
		};
	 	/-nbest$/ && do { $i++;
					 #print "nbest length : $ARGV[$i]\n";
					 $nbest_length=$ARGV[$i]; 
					 last; 
		};
		/-l$/ && do { $i++;
					 #print "length : $ARGV[$i]\n";
					 $length=$ARGV[$i];
					 last; 
		};
		/-c$/ && do { $i++;
					 $costs = "";
					 $costs .= "$ARGV[$i]";
					 $i++;
					 #print "costs : $ARGV[$i]\n";
					 while($i < $#ARGV && ! ($ARGV[$i] =~ /^-.+/))
					 {
					 	$costs .= " $ARGV[$i]";
					 	$i++;
					 }
					 #print STDERR "costs : $costs\n";
					 $i--;
					 last; 
		};
		/-p$/ && do { $i++;
					 $priors = "";
					 $priors .= "$ARGV[$i]";
					 $i++;
					 #print "priors : $ARGV[$i]\n";
					 while($i <= $#ARGV && ! ($ARGV[$i] =~ /^-.+/))
					 {
					 	$priors .= " $ARGV[$i]";
					 	$i++;
					 }
					 #print STDERR "priors : $priors\n";
					 $i--;
					 last; 
	    	};
	    	/-s$/ && do { $i++;
					 #print "stop_list : $ARGV[$i]\n";
					 $stoplist=$ARGV[$i];
					 last;
		};
		/-t$/ && do { $i++;
					 #print "terpparams : $ARGV[$i]\n";
					 $terpparams=$ARGV[$i];
					 last;
		};
	        /-w$/ && do { $i++;
					 #print "wordnet : $ARGV[$i]\n";
					 $wordnet=$ARGV[$i];
					 last;
		};
		/-para$/ && do { $i++;
					 #print "paraphrases : $ARGV[$i]\n";
					 $paraphrases=$ARGV[$i];
					 last;
		};
		/-help$/ && do { $i++;
					 print STDERR "Usage : $usage";
					 last;
		};
		/-h$/ && do { $i++;
					 print STDERR "Usage : $usage";
					 last;
		};
	    die "Invalid arg : $ARGV[$i]\n";
	}
	$i++;
}



print '<?xml version="1.0" encoding="UTF-8"?>'."\n";
print '<config>'."\n";
print "\n";
print '<property name="logLevel"     value="INFO"/>'."\n";
print '<property name="showCreations"     value="true"/>'."\n";
    
print "\n";

print '<component  name="decoder" type="edu.lium.decoder.TokenPassDecoder">'."\n";
print '<property name="dictionary" value="dictionary"/>'."\n";
print '<property name="logMath" value="logMath"/>'."\n";
print '<property name="useNGramModel" value="';
$useNGramLM == 0 ? print "false": print "true";
print '"/>'."\n";
print '<property name="logLevel"     value="INFO"/>'."\n";
print '<property name="lmonserver" value="lmonserver"/>'."\n";
print '<property name="ngramModel" value="ngramModel"/>'."\n";

print '<property name="fudge" value="';
print $fudge;
print '"/>'." (This value is multiplied by 10 in the software) \n";

print '<property name="null_penalty" value="';
print $null;
print '"/>'."\n";

print '<property name="length_penalty" value="';
print $length;
print '"/>'." (This value is multiplied by 10 in the software)\n";

print '<property name="max_nb_tokens" value="';
print $max_nb_tokens;
print '"/>'."\n";

print '<property name="nbest_length" value="';
print $nbest_length;
print '"/>'."\n";

print '<property name="debug" value="';
print $debug_decode;
print '"/>'."\n";

print '</component>'."\n";

print "\n";

#<!-- create the configMonitor  -->
print '<component name="configMonitor" type="edu.cmu.sphinx.instrumentation.ConfigMonitor">'."\n";
print '<property name="showConfig" value="true"/>'."\n";
print '<property name="showConfigAsGDL" value="true"/>'."\n";
print '</component>'."\n";

print "\n";

print '<component name="logMath" type="edu.cmu.sphinx.util.LogMath">'."\n";
print '<property name="logBase" value="1.0001"/>'."\n";
print '<property name="useAddTable" value="true"/>'."\n";
print '</component>'."\n";

print "\n";

print '<component name="dictionary" type="edu.cmu.sphinx.linguist.dictionary.SimpleDictionary">'."\n";
print '<property name="dictionaryPath" value="file:lm.vocab"/>'."\n";
print '<property name="fillerPath" value=""/>'."\n";
print '</component>'."\n";

#<!-- ******************************************************** -->
#<!-- The unit manager configuration                           -->
#<!-- ******************************************************** -->
#print "\n";

#print '<component name="unitManager" type="edu.cmu.sphinx.linguist.acoustic.UnitManager">'."\n";
#print '</component>'."\n";

print "\n";

print '<component  name="lmonserver" type="edu.cmu.sphinx.linguist.language.ngram.NetworkLanguageModel">'."\n";
print '<property name="port" value="1234"/>'."\n";
print '<property name="host" value="';
print $lmserverhost;
print '"/>'."\n";
print '<property name="maxDepth" value="4"/>'."\n";
print '<property name="logMath" value="logMath"/>'."\n";
print '</component>'."\n";

print "\n";

print '<component name="ngramModel" type="edu.cmu.sphinx.linguist.language.ngram.large.LargeNGramModel">'."\n";
print '<property name="location" value="';
print $languagemodel; # nc4_en.wlwmt09b.4g.kn-int.1-1-1-1.cmu.arpa.gz.DMP32
print '" />'."\n";
print '<property name="logMath" value="logMath"/>'."\n";
print '<property name="dictionary" value="dictionary"/>'."\n";
print '<property name="maxDepth" value="4"/>'."\n";
print '<property name="logLevel" value="SEVERE"/>'."\n";
print '<property name="unigramWeight" value=".7"/>'."\n";
print '</component>'."\n";

print "\n";

print '<component name="MANY" type="edu.lium.mt.MANY">'."\n";
print '<property name="decoder" value="decoder"/>'."\n";
print '<property name="terp" value="terp"/>'."\n";
print '<property name="output" value="output.many"/>'."\n";

print '<property name="priors" value="';
print $priors;
print '"/>'."\n";

print '<property name="hypotheses" value="hyp0.id';
for(my $n=1; $n<$nb_sys; $n++)
{
	print " hyp$n.id";
}
print '" />'."\n";
print '<property name="hyps_scores" value="hyp0_sc.id';
for(my $n=1; $n<$nb_sys; $n++)
{
	print " hyp$n\_sc.id";
}
print '" />'."\n";

print '<property name="costs" value="';
print $costs; #1.0 1.0 1.0 1.0 1.0 0.0 1.0
print '" />'."\n";
print '<!--                          del stem syn ins sub match shift-->'."\n";

print '<property name="terpParams"     value="';
print $terpparams;
print '"/>'."\n";
        
print '<property name="wordnet"     value="';
print $wordnet;
print '"/>'."\n";
print '<property name="shift_word_stop_list"     value="';
print $stoplist;
print '"/>'."\n";
print '<property name="paraphrases"     value="';
print $paraphrases;
print '"/>'."\n";

print '<property name="multithread"     value="';
print $nb_threads;
print '"/>'."\n";


print '</component>'."\n";


print '</config>'."\n";
