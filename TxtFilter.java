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


public  class TxtFilter implements FileFilter {
	String Ending;
 	String[] FilterPhrases = new String[100];
	int Length;
	boolean foldVal;
	int foldNum;
	boolean needsFolds;
	
	public TxtFilter(String in, String[] FilterPhraseIn){
		Ending = in;
		for(int i=0; i < FilterPhraseIn.length; i++)
			FilterPhrases[i] = FilterPhraseIn[i];
		Length = FilterPhraseIn.length;
		this.needsFolds = false;
	}


	public TxtFilter(String in, String[] FilterPhraseIn, boolean foldVal, int foldNum){
		Ending = in;
		for(int i=0; i < FilterPhraseIn.length; i++)
			FilterPhrases[i] = FilterPhraseIn[i];
		Length = FilterPhraseIn.length;
		this.foldVal = foldVal;
		this.foldNum = foldNum;
		this.needsFolds = true;
	}


	public String cleanString (String in){
		String [] check = in.split("/");
		return check[check.length-1];	
	}

	private boolean testAnnotation(File file, String FilterPhrase) throws IOException {
//This code tells me how to extract values from our 'correct' side of things

		boolean out = false;
		String CheckFileName = "./CogPOTerms/TaggedAbstracts/";
		char [] in = new char[1000];
	        String AbstractName = cleanString(file.toString());
	        FileReader fis = new FileReader(new File(CheckFileName+AbstractName));
	        fis.read(in);

		fis.close(); 
		String [] check = (new String(in)).split(" ");
		for(int i = 0; i < check.length; i ++){
			if(check[i].equals(FilterPhrase)){
				out = true;			
			}
		}
		return out;	
	}

        /** Test whether the string representation of the file 
         *   ends with the correct extension. Note that {@ref FileIterator}
         *   will only call this filter if the file is not a directory,
         *   so we do not need to test that it is a file.
         */

	public boolean foldTest(File file){
		if(needsFolds){
			String CheckFileName = "./CogPOTerms/FoldAbstracts" + this.foldNum + "/";

			char [] in = new char[10];
	        	String AbstractName = cleanString(file.toString());
			try{
	        		FileReader fis = new FileReader(new File(CheckFileName+AbstractName));
	        		fis.read(in);
				fis.close();
			}
			catch (IOException e){
				return false;
			} 	
			int count = 0;
			int inLength = 0;
			for(int i = 0; i < in.length; i ++)
				if(in[i] != (char)0)
					inLength ++;
				else
					i = in.length;	
			for(int i = 0; i < inLength; i ++)
				count += ((int) Math.pow(10.0, ((double) (inLength - i - 1)))) * ((int)in[i]-48);  
			return (((count == this.foldNum) && !this.foldVal) || ((count != this.foldNum) && this.foldVal));
		}
		else
			return true;

	}

        public boolean accept(File file){
 	boolean foldTest = foldTest(file); 
	    boolean annotationTest = true;
	    String [] check = file.toString().split("/");
	    if(!Character.isDigit(check[check.length-1].charAt(0)))
		return false;
	    for(int i = 0; i < Length; i ++)
		try{
			annotationTest &= testAnnotation(file, FilterPhrases[i]);}
		catch (IOException e) {
			System.out.print("Oh no, there was a missing file in the annotation check!\n");
		}
            return (file.toString().endsWith(Ending) && annotationTest && foldTest);
        }
    }
