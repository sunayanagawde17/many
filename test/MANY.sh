#!/bin/bash

#Copyright 2009 Loic BARRAULT.  
#See the file "LICENSE.txt" for information on usage and
#redistribution of this file, and for a DISCLAIMER OF ALL 
#WARRANTIES.

export LANG=en_US.UTF-8

########### VARIABLES DEFINITION

I_SMARTLY_SET_THE_VARIABLES=0;

# Tools
many_dir=TO_SET
add_id=$many_dir/scripts/add_id.sh
gen_MANY_config=$many_dir/scripts/gen_MANY_config.pl
many=$many_dir/many/lib/MANY.jar

# Files
many_config="many.config.xml"
terp_params="terp.params"
vocab="small_lm.vocab"
output="output.many"
current_dir=`pwd`
working_dir="many_syscomb"
hypotheses=("hyp0" "hyp1" "hyp2" "hyp3")

#TERp parameters
#costs=(del stem syn ins sub match shift)
costs=(1.0 1.0 1.0 1.0 1.0 0.0 1.0)

data_dir=TO_SET
wordnet=$data_dir/WordNet-3.0/dict/
paraphrases=$data_dir/phrases.db
stoplist=$data_dir/shift_word_stop_list.txt

#Decoder parameters
lmserverhost=TO_SET
lmserverport=TO_SET
lm3g=lm3g.DMP32
uselm3g=0


priors=(0.4 0.25 0.25 0.1)
fudge=0.1
null=0.00001
length=0.3
max_nb_tokens=1000
nbest_length=0

#Others
nb_sys=${#hypotheses[@]}
nb_threads=1
clean=1
declare -i i

########################

if [ $I_SMARTLY_SET_THE_VARIABLES == 0  ]
then
	echo "    MANY - Open Source Machine Translation System Combination"
	echo " Before using this script, please read the README file and set the variables in this script."
	echo " For this test, you have to set the LM server host and port, and set the variable (search for TO_SET)."
	echo " When you are ready, set the I_SMARTLY_SET_THE_VARIABLES to 1 and go !"
	exit
fi

########################

echo -n "MANY.sh - start time is : "
date

######################## PREPARE DATA
echo -n "Creating working directory : $working_dir ..."
mkdir -p $working_dir
echo " OK "

echo "Entering $working_dir ... "
pushd $working_dir

echo -n "Creating links to hypotheses ... "
i=0
while [ $i -lt $nb_sys ]
do
	#echo "CMD : ln -fs $current_dir/${hypotheses[$i]} hyp$i"
	ln -fs $current_dir/${hypotheses[$i]} hyp$i 
	i=`expr $i + 1`
done
echo " OK "

echo -n "Creating links to vocabulary file ... "
ln -fs $current_dir/$vocab lm.vocab 
echo " OK "

echo "Using $nb_sys systems ..."
nb_sent=`cat hyp0 | wc -l`
echo "$nb_sent sentences to process ..."
if [ $nb_sent -eq 0 ]
then
	echo "No sentence to process ... exiting !"
	exit
fi

## Sanity check of hypotheses
echo -n "Checking size of hypotheses files ..."
i=1
while [ $i -lt $nb_sys ]
do
	nsent=`cat hyp$i | wc -l`
	if [ $nsent != $nb_sent ] 
	then
		echo ""
		echo "ERROR : Hypotheses files have not the same size ... exiting !"
		exit
	fi
	i=`expr $i + 1`
done
echo " OK "

# Cleaning working dir
if [ $clean -gt 0 ]
then
	echo -n "Cleaning working directory $working_dir ..."
	rm -f hyp*.id
	rm -f hyp*_sc*
	rm -f $output
	echo " OK "
fi

######################## GENERATE SCORES FILES
# - This will be changed later when better weights will be available
i=0
while [ $i -lt $nb_sys ]
do
	if [ ! -e hyp${i}_sc  ] 
	then
		echo "CMD : perl -pne \"s/[^ \n]+/${priors[$i]}/g\" hyp${i} \> hyp${i}_sc" 
		echo -n "Starting generating score file for hyp${i} ..."
		perl -pe "s/[^ \n]+/${priors[$i]}/g" hyp${i} > hyp${i}_sc 
		echo " OK "
	else
		echo "hyp${i}_sc exists ... reusing !"
	fi
	i=`expr $i + 1`
done

######################## ADD IDs TO SENTENCES and SCORES FILES
i=0
while [ $i -lt $nb_sys ]
do
	if [ ! -e hyp${i}.id  ] 
	then
		echo 'CMD : $add_id hyp${i} hyp${i}.id'
		echo -n "Starting adding id to hyp${i} ..."
		$add_id hyp${i} hyp${i}.id
		echo " OK "
	else
		echo "hyp${i}.id exists ... reusing !"
	fi
	if [ ! -e hyp${i}_sc.id  ] 
	then
		echo 'CMD : $add_id hyp${i}_sc hyp${i}_sc.id'
		echo -n "Starting adding id to hyp${i}_sc ..."
		$add_id hyp${i}_sc hyp${i}_sc.id
		echo " OK "
	else
		echo "hyp${i}_sc.id exists ... reusing !"
	fi
	i=`expr $i + 1`
done

######################## GENERATE MANY CONFIG FILE

if [ $uselm3g == 0 ]
then
	echo "CMD : $gen_MANY_config -nbsys $nb_sys -h $lmserverhost -port $lmserverport -f $fudge -m $nb_threads -maxtok $max_nb_tokens \
	-nbest $nbest_length -n $null -l $length -c ${costs[@]} -p ${priors[@]} \
	-para $paraphrases -s $stoplist -w $wordnet \> $many_config";
	$gen_MANY_config -nbsys $nb_sys -h $lmserverhost -port $lmserverport -f $fudge -m $nb_threads -maxtok $max_nb_tokens \
	-nbest $nbest_length -n $null -l $length -c ${costs[@]} -p ${priors[@]} \
	-para $paraphrases -s $stoplist -w $wordnet -dd > $many_config;
	echo " done !\n";
else
	echo "CMD : $gen_MANY_config -nbsys $nb_sys -lm3g $lm3g -f $fudge -m $nb_threads -maxtok $max_nb_tokens \
	-nbest $nbest_length -n $null -l $length -c ${costs[@]} -p ${priors[@]} \
	-para $paraphrases -s $stoplist -w $wordnet \> $many_config";
	$gen_MANY_config -nbsys $nb_sys -lm3g $lm3g -f $fudge -m $nb_threads -maxtok $max_nb_tokens \
	-nbest $nbest_length -n $null -l $length -c ${costs[@]} -p ${priors[@]} \
	-para $paraphrases -s $stoplist -w $wordnet > $many_config;
fi

######################## CALL MANY syscomb

enc=UTF-8
#enc=ISO_8859_1

echo "CMD : java -Xmx8G -Dfile.encoding=$enc -cp $many edu.lium.mt.MANY $many_config"
echo -n "Starting MANY system combination ..."
java -Xmx8G -Dfile.encoding=$enc -cp $many edu.lium.mt.MANY $many_config
echo " OK "

popd 

echo -n "MANY.sh - end time is : "
date
