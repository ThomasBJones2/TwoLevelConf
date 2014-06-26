import cc.mallet.classify.*;
import cc.mallet.util.*;
import cc.mallet.types.*;
import java.io.*;
import java.util.*;
import java.util.regex.*;

import cc.mallet.pipe.*;
import cc.mallet.pipe.iterator.*;
import cc.mallet.types.*;
import cc.mallet.pipe.Pipe;
import java.net.URI;


public  class Scorer {

    Pipe pipe;
    int foldNum;
    static int FOLDS = 10;
    static int NUMTREES = 5; //Please note that many items use "Numtrees - 1.0" has a divisor - this is correct, the number of trees must be decreased by 
				//1 due to dropping of the 'human scored' tree for each category
	String CheckFileName = "./CogPOTerms/TaggedAbstracts/";
	
	String OutFileName = "./CogPOTerms/TestAbstracts";

	String HumanFileName = "./CogPOTerms/Human";

	String[] empty = new String[0];

	String SMterminator[] = new String[]{"Auditory","Gustatory",
				"Interoceptive","None",
				"Olfactory","Tactile","Visual"};

	String STterminator[] = new String[]{"3DObjects","Accupuncture",
				"AsianCharacters","BrailleDots",
				"BreathableGas","ChordSequences","Clicks",
				"Digits", "ElectricalStimulation",
				"Faces", "FalseFonts", "FilmClip",
				"FixationPoint", "FlashingCheckerboard",
				"Food","Fractals","Heat","InfraredLaser",
				"Infusion", "Music", "Noise", "None",
				"NonverbalVocalSounds", "NonvocalSounds",
				"Objects", "Odor", "Pain", "Pictures", "Point",
				"PointsofLight", "Pseudowords", "RandomDots",
				"ReversedSpeech", "Shapes", "Syllables", 
				"Symbols", "TactileStimulation", "TMS",
				"Tones", "VibratoryStimulation", "Words"};

	String Iterminator[] = new String[]{"Attend","Count","Detect","Discriminate",
				"Encode","Fixate","Generate","Imagine",
				"Move","Name","None","Passive","Read",
				"Recall","Repeat","Sing","Smile","Track"};

	String RMterminator[] = new String[]{"Facial",
				"Foot","Hand","None","Ocular"};

	String RTterminator[] = new String[]{"ButtonPress","FingerTapping",
				"Flexion","Grasp",
				"manipulate","None","Saccades",
				"Speech"};

	double[][][] SMDen = new double[NUMTREES][FOLDS][SMterminator.length + 1];
	double[][][] SMNum = new double[NUMTREES][FOLDS][SMterminator.length + 1];

	double[][][] STDen = new double[NUMTREES][FOLDS][STterminator.length + 1];
	double[][][] STNum = new double[NUMTREES][FOLDS][STterminator.length + 1];

	double[][][] RMDen = new double[NUMTREES][FOLDS][RMterminator.length + 1];
	double[][][] RMNum = new double[NUMTREES][FOLDS][RMterminator.length + 1];

	double[][][] RTDen = new double[NUMTREES][FOLDS][RTterminator.length + 1];
	double[][][] RTNum = new double[NUMTREES][FOLDS][RTterminator.length + 1];

	double[][][] IDen = new double[NUMTREES][FOLDS][Iterminator.length + 1];
	double[][][] INum = new double[NUMTREES][FOLDS][Iterminator.length + 1];

    public Scorer() {
        pipe = buildPipe();
	foldNum = 0;


	for(int i = 0; i < NUMTREES; i++){
		for(int j = 0; j < FOLDS; j ++){
			for(int k = 0; k < SMterminator.length + 1; k ++){
				SMDen[i][j][k] = 0;
				SMNum[i][j][k] = 0;
			}
			for(int k = 0; k < STterminator.length + 1; k ++){
				STDen[i][j][k] = 0;
				STNum[i][j][k] = 0;
			}
			for(int k = 0; k < RMterminator.length + 1; k ++){
				RMDen[i][j][k] = 0;
				RMNum[i][j][k] = 0;
			}
			for(int k = 0; k < RTterminator.length + 1; k ++){
				RTDen[i][j][k] = 0;
				RTNum[i][j][k] = 0;
			}
			for(int k = 0; k < Iterminator.length + 1; k ++){
				IDen[i][j][k] = 0;
				INum[i][j][k] = 0;
			}
		}
	}
    }

    public Pipe buildPipe() {
        ArrayList pipeList = new ArrayList();

        // Read data from File objects
        pipeList.add(new Input2CharSequence("UTF-8"));

        // Regular expression for what constitutes a token.
        //  This pattern includes Unicode letters, Unicode numbers, 
        //   and the underscore character. Alternatives:
        //    "\\S+"   (anything not whitespace)
        //    "\\w+"    ( A-Z, a-z, 0-9, _ )
        //    "[\\p{L}\\p{N}_]+|[\\p{P}]+"   (a group of only letters and numbers OR
        //                                    a group of only punctuation marks)
        Pattern tokenPattern =
            Pattern.compile("[\\p{L}\\p{N}_]+");

        // Tokenize raw strings
        pipeList.add(new CharSequence2TokenSequence(tokenPattern));

        // Normalize all tokens to all lowercase
        pipeList.add(new TokenSequenceLowercase());

        // Remove stopwords from a standard English stoplist.
        //  options: [case sensitive] [mark deletions]
        pipeList.add(new TokenSequenceRemoveStopwords(false, false));

        // Rather than storing tokens as strings, convert 
        //  them to integers by looking them up in an alphabet.
        pipeList.add(new TokenSequence2FeatureSequence());

        // Do the same thing for the "target" field: 
        //  convert a class label string to a Label object,
        //  which has an index in a Label alphabet.
        pipeList.add(new Target2Label());

        // Now convert the sequence of features to a sparse vector,
        //  mapping feature IDs to counts.
        pipeList.add(new FeatureSequence2FeatureVector());

        // Print out the features and the label
 //       pipeList.add(new PrintInputAndTarget());

        return new SerialPipes(pipeList);
    }    



	public InstanceList readDirectory(File directory, String[] FilterPhrases) {
        return readDirectories(new File[] {directory}, FilterPhrases);
    }

    public InstanceList readDirectories(File[] directories, String[]  FilterPhrases) {
        
        // Construct a file iterator, starting with the 
        //  specified directories, and recursing through subdirectories.
        // The second argument specifies a FileFilter to use to select
        //  files within a directory.
        // The third argument is a Pattern that is applied to the 
        //   filename to produce a class label. In this case, I've 
        //   asked it to use the last directory name in the path.
        FileIterator iterator =
            new FileIterator(directories,
                             new TxtFilter(".txt", FilterPhrases, false, this.foldNum),
                             FileIterator.LAST_DIRECTORY);

        // Construct a new instance list, passing it the pipe
        //  we want to use to process instances.
        InstanceList instances = new InstanceList(pipe);

        // Now process each instance provided by the iterator.
        instances.addThruPipe(iterator);

        return instances;
    }

public String cleanString (String in){
		String [] check = in.split("/");
		return check[check.length-1];	
	}

public boolean check(String in, String[] inlist){
	boolean out = false;
		for(int i = 0; i < inlist.length; i ++){
			if(in.equals(inlist[i]))
				out = true;
		}
	return out;	
}

public double stdev(double[][] in, double avg){
	double Accum = 0;
	for(int i = 0; i < FOLDS; i ++)
		for(int j = 0; j < NUMTREES - 1.0; j ++)
			Accum += (in[j][i] - avg)*(in[j][i] - avg);
	Accum = Accum/((double)FOLDS*(double)NUMTREES - 1.0);

	return Math.sqrt(Accum);
}

void score(int TreeNum) throws IOException{
	//Scorer();

	//Here I am grabbing the correct values.


	for(int k = 0; k < FOLDS; k++){
	this.foldNum = k;

	InstanceList testInstances = this.readDirectory(new File(OutFileName+this.foldNum+"/"), empty);


	//=============================================
	// Here I do the scoring for stimulus modality
	//============================================= 

	if(TreeNum >= 1){
	for(int i = 0; i < SMterminator.length; i ++){
		for(int index = 0; index < testInstances.size(); index ++){
			char [] in = new char[1000];
			char [] in2 = new char[1000];
			char [] in3 = new char[1000];
	    		String AbstractName = this.cleanString(testInstances.get(index).getName().toString());

	    		FileReader fis = new FileReader(new File(CheckFileName+AbstractName));
	    		fis.read(in);
			String [] Correct = (new String(in)).split(" ");
			fis.close();

			fis = new FileReader(new File(OutFileName + this.foldNum + "/" +AbstractName));
			fis.read(in2);
			String [] Guess = (new String(in2)).split(" ");
			fis.close();


			String[] HumanGuess;		
			try{
				fis = new FileReader(new File(HumanFileName + this.foldNum + "/" + AbstractName));
				fis.read(in3);
				HumanGuess = (new String(in3)).split(" ");
				fis.close();
			}
			catch(IOException e){
				HumanGuess = new String[0];
			} 


			if(!this.check("StimuluModality" + SMterminator[i], HumanGuess)){
				if(this.check("StimulusModality" + SMterminator[i], Correct))
					SMDen[TreeNum][k][i] ++;
				if(this.check("StimulusModality" + SMterminator[i], Guess)){
					SMDen[TreeNum][k][i] ++;
				}
				if(this.check("StimulusModality" + SMterminator[i], Guess) && 
					this.check("StimulusModality" + SMterminator[i], Correct)){
					SMNum[TreeNum][k][i] ++;
				}	
			}



		}
		SMNum[TreeNum - 1][k][SMterminator.length] += SMNum[TreeNum][k][i];
		SMDen[TreeNum - 1][k][SMterminator.length] += SMDen[TreeNum][k][i];
		
	}
	if(k == FOLDS-1 && TreeNum == NUMTREES-1){
		double out = 0;
		double stdev = 0;
		double [][] stdhelp = new double[NUMTREES][FOLDS];
		for(int v = 0; v < NUMTREES - 1; v ++){
			for(int l = 0; l < FOLDS; l ++){
				if(SMDen[v][l][SMterminator.length] > 0){
					stdhelp[v][l] = (2.0*SMNum[v][l][SMterminator.length]/SMDen[v][l][SMterminator.length]);
					out += (2.0*SMNum[v][l][SMterminator.length]/SMDen[v][l][SMterminator.length]);
				}
				else
					stdhelp[v][l] = 0;
			}
		}
		out = out/(FOLDS*(NUMTREES - 1.0));
		stdev = this.stdev(stdhelp , out);
		System.out.print("Stimulus Modality" + ": " + out + " +/-" + stdev + "\n\n");
	}
	}


	//=============================================
	// Here I do the scoring for stimulus type
	//============================================= 

	if(TreeNum != 1){
	for(int i = 0; i < STterminator.length; i ++){
		for(int index = 0; index < testInstances.size(); index ++){
			char [] in = new char[1000];
			char [] in2 = new char[1000];
			char [] in3 = new char[1000];

	    		String AbstractName = this.cleanString(testInstances.get(index).getName().toString());
	    		FileReader fis = new FileReader(new File(CheckFileName+AbstractName));
	    		fis.read(in);
			String [] Correct = (new String(in)).split(" ");
			fis.close();

			fis = new FileReader(new File(OutFileName + this.foldNum + "/" +AbstractName));
			fis.read(in2);
			String [] Guess = (new String(in2)).split(" ");
			fis.close();

			String[] HumanGuess;		
			try{
				fis = new FileReader(new File(HumanFileName + this.foldNum + "/" + AbstractName));
				fis.read(in3);
				HumanGuess = (new String(in3)).split(" ");
				fis.close();
			}
			catch(IOException e){
				HumanGuess = new String[0];
			} 

			if(!this.check("StimuluModality" + STterminator[i], HumanGuess)){

				if(this.check("StimulusType" + STterminator[i], Correct))
					STDen[TreeNum][k][i] ++;
				if(this.check("StimulusType" + STterminator[i], Guess)){
					STDen[TreeNum][k][i] ++;
				}
				if(this.check("StimulusType" + STterminator[i], Guess) && 
					this.check("StimulusType" + STterminator[i], Correct)){
					STNum[TreeNum][k][i] ++;
				}
			}

	
		}
		if(TreeNum < 1){
			STNum[TreeNum][k][STterminator.length] += STNum[TreeNum][k][i];
			STDen[TreeNum][k][STterminator.length] += STDen[TreeNum][k][i];
		}
		else{
			STNum[TreeNum-1][k][STterminator.length] += STNum[TreeNum][k][i];
			STDen[TreeNum-1][k][STterminator.length] += STDen[TreeNum][k][i];
		
		}
	}
	if(k == FOLDS-1 && TreeNum == NUMTREES-1){
		double out = 0;
		double stdev = 0;
		double [][] stdhelp = new double[NUMTREES][FOLDS];
		for(int v = 0; v < NUMTREES - 1; v ++){
			for(int l = 0; l < FOLDS; l ++){
				if(STDen[v][l][STterminator.length] > 0){
					stdhelp[v][l] = (2.0*STNum[v][l][STterminator.length]/STDen[v][l][STterminator.length]);
					out += (2.0*STNum[v][l][STterminator.length]/STDen[v][l][STterminator.length]);
				}
				else
					stdhelp[v][l] = 0;
			}
		}
		out = out/(FOLDS*(NUMTREES - 1.0));
		stdev = this.stdev(stdhelp , out);
		System.out.print("Stimulus Type" + ": " + out + " +/-" + stdev + "\n\n");
	}
	}



	//=============================================
	// Here I do the scoring for Response Modality
	//============================================= 

	if(TreeNum != 2){
	for(int i = 0; i < RMterminator.length; i ++){
		for(int index = 0; index < testInstances.size(); index ++){
			char [] in = new char[1000];
			char [] in2 = new char[1000];
			char [] in3 = new char[1000];

	    		String AbstractName = this.cleanString(testInstances.get(index).getName().toString());
	    		FileReader fis = new FileReader(new File(CheckFileName+AbstractName));
	    		fis.read(in);
			String [] Correct = (new String(in)).split(" ");
			fis.close();

			fis = new FileReader(new File(OutFileName + this.foldNum + "/" +AbstractName));
			fis.read(in2);
			fis.close();
			String [] Guess = (new String(in2)).split(" ");


			String[] HumanGuess;		
			try{
				fis = new FileReader(new File(HumanFileName + this.foldNum + "/" + AbstractName));
				fis.read(in3);
				HumanGuess = (new String(in3)).split(" ");
				fis.close();
			}
			catch(IOException e){
				HumanGuess = new String[0];
			} 


			if(!this.check("StimuluModality" + RMterminator[i], HumanGuess)){
				if(this.check("ResponseModality" + RMterminator[i], Correct))
					RMDen[TreeNum][k][i] ++;
				if(this.check("ResponseModality" + RMterminator[i], Guess)){
					RMDen[TreeNum][k][i] ++;
				}
				if(this.check("ResponseModality" + RMterminator[i], Guess) && 
					this.check("ResponseModality" + RMterminator[i], Correct)){
					RMNum[TreeNum][k][i] ++;
				}	
			}

		}
		if(TreeNum < 2){
			RMNum[TreeNum][k][RMterminator.length] += RMNum[TreeNum][k][i];
			RMDen[TreeNum][k][RMterminator.length] += RMDen[TreeNum][k][i];
		}
		else{
			RMNum[TreeNum - 1][k][RMterminator.length] += RMNum[TreeNum][k][i];
			RMDen[TreeNum - 1][k][RMterminator.length] += RMDen[TreeNum][k][i];
		}
	}
	if(k == FOLDS-1 && TreeNum == NUMTREES-1){
		double out = 0;
		double stdev = 0;
		double [][] stdhelp = new double[NUMTREES][FOLDS];
		for(int v = 0; v < NUMTREES - 1; v ++){
			for(int l = 0; l < FOLDS; l ++){
				if(RMDen[v][l][RMterminator.length] > 0){
					stdhelp[v][l] = (2.0*RMNum[v][l][RMterminator.length]/RMDen[v][l][RMterminator.length]);
					out += (2.0*RMNum[v][l][RMterminator.length]/RMDen[v][l][RMterminator.length]);
				}
				else
					stdhelp[v][l] = 0;
			}
		}
		out = out/(FOLDS*(NUMTREES - 1.0));
		stdev = this.stdev(stdhelp , out);
		System.out.print("Response Modality" + ": " + out + " +/-" + stdev + "\n\n");
	}
	}


	//=============================================
	// Here I do the scoring for Response Type
	//============================================= 
	if(TreeNum != 3){
	for(int i = 0; i < RTterminator.length; i ++){
		for(int index = 0; index < testInstances.size(); index ++){
			char [] in = new char[1000];
			char [] in2 = new char[1000];
			char [] in3 = new char[1000];

	    		String AbstractName = this.cleanString(testInstances.get(index).getName().toString());
	    		FileReader fis = new FileReader(new File(CheckFileName+AbstractName));
	    		fis.read(in);
			String [] Correct = (new String(in)).split(" ");
			fis.close();

			fis = new FileReader(new File(OutFileName + this.foldNum + "/" +AbstractName));
			fis.read(in2);
			String [] Guess = (new String(in2)).split(" ");
			fis.close();

			String[] HumanGuess;		
			try{
				fis = new FileReader(new File(HumanFileName + this.foldNum + "/" + AbstractName));
				fis.read(in3);
				HumanGuess = (new String(in3)).split(" ");
				fis.close();
			}
			catch(IOException e){
				HumanGuess = new String[0];
			} 


			if(!this.check("StimuluModality" + RTterminator[i], HumanGuess)){
				if(this.check("ResponseType" + RTterminator[i], Correct))
					RTDen[TreeNum][k][i] ++;
				if(this.check("ResponseType" + RTterminator[i], Guess)){
					RTDen[TreeNum][k][i] ++;
				}
				if(this.check("ResponseType" + RTterminator[i], Guess) && 
					this.check("ResponseType" + RTterminator[i], Correct)){
					RTNum[TreeNum][k][i] ++;
				}
			}

	
		}
		if(TreeNum < 3){	
			RTNum[TreeNum][k][RTterminator.length] += RTNum[TreeNum][k][i];
			RTDen[TreeNum][k][RTterminator.length] += RTDen[TreeNum][k][i];
		}
		else{
			RTNum[TreeNum - 1][k][RTterminator.length] += RTNum[TreeNum][k][i];
			RTDen[TreeNum - 1][k][RTterminator.length] += RTDen[TreeNum][k][i];
		}
	}
	if(k == FOLDS-1 && TreeNum == NUMTREES-1){
		double out = 0;
		double stdev = 0;
		double [][] stdhelp = new double[NUMTREES][FOLDS];
		for(int v = 0; v < NUMTREES - 1; v ++){
			for(int l = 0; l < FOLDS; l ++){
				if(RTDen[v][l][RTterminator.length] > 0){
					stdhelp[v][l] = (2.0*RTNum[v][l][RTterminator.length]/RTDen[v][l][RTterminator.length]);
					out += (2.0*RTNum[v][l][RTterminator.length]/RTDen[v][l][RTterminator.length]);
				}
				else
					stdhelp[v][l] = 0;
			}
		}
		out = out/(FOLDS*(NUMTREES - 1.0));
		stdev = this.stdev(stdhelp , out);
		System.out.print("Response Type" + ": " + out + " +/-" + stdev + "\n\n");
	}
	}

	//=============================================
	// Here I do the scoring for Instructions
	//============================================= 
	if(TreeNum < 4){
	for(int i = 0; i < Iterminator.length; i ++){
		for(int index = 0; index < testInstances.size(); index ++){
			char [] in = new char[1000];
			char [] in2 = new char[1000];
			char [] in3 = new char[1000];

	    		String AbstractName = this.cleanString(testInstances.get(index).getName().toString());
	    		FileReader fis = new FileReader(new File(CheckFileName+AbstractName));
	    		fis.read(in);
			String [] Correct = (new String(in)).split(" ");
			fis.close();

			fis = new FileReader(new File(OutFileName + this.foldNum + "/" +AbstractName));
			fis.read(in2);
			String [] Guess = (new String(in2)).split(" ");
			fis.close();

			String[] HumanGuess;		
			try{
				fis = new FileReader(new File(HumanFileName + this.foldNum + "/" + AbstractName));
				fis.read(in3);
				HumanGuess = (new String(in3)).split(" ");
				fis.close();
			}
			catch(IOException e){
				HumanGuess = new String[0];
			} 


			if(!this.check("StimuluModality" + Iterminator[i], HumanGuess)){
				if(this.check("Instructions" + Iterminator[i], Correct))
					IDen[TreeNum][k][i] ++;
				if(this.check("Instructions" + Iterminator[i], Guess)){
					IDen[TreeNum][k][i] ++;
				}
				if(this.check("Instructions" + Iterminator[i], Guess) && 
					this.check("Instructions" + Iterminator[i], Correct)){
					INum[TreeNum][k][i] ++;
				}
			}
	
		}
		INum[TreeNum][k][Iterminator.length] += INum[TreeNum][k][i];
		IDen[TreeNum][k][Iterminator.length] += IDen[TreeNum][k][i];
	}
	}
	if(k == FOLDS-1 && TreeNum == NUMTREES-1){
		double out = 0;
		double stdev = 0;
		double [][] stdhelp = new double[NUMTREES][FOLDS];
		for(int v = 0; v < NUMTREES - 1; v ++){
			for(int l = 0; l < FOLDS; l ++){
				if(IDen[v][l][Iterminator.length] > 0){
					stdhelp[v][l] = (2.0*INum[v][l][Iterminator.length]/IDen[v][l][Iterminator.length]);
					out += (2.0*INum[v][l][Iterminator.length]/IDen[v][l][Iterminator.length]);
				}
				else
					stdhelp[v][l] = 0;
			}
		}
		out = out/(FOLDS*(NUMTREES - 1.0));
		stdev = this.stdev(stdhelp , out);
		System.out.print("Instructions" + ": " + out + " +/-" + stdev + "\n\n");
	}
	}

}

}
