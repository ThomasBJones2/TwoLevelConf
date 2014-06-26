//package com.google.commons.tree;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.SortedMap;
import java.util.SortedSet;

import cc.mallet.classify.*;
import cc.mallet.util.*;
import cc.mallet.types.*;

/**
 * Author: Gregor Zeitlinger <gregor@zeitlinger.de> Date: 08.09.2011
 */
public class Tree {

        private final Tree[] children;

        private final String value;
	
	private final String[] title;

	private final Classifier[] LocalBayes;


	//Value: This is the restriction label on the current node, all clasifiers on this node have been
		//restricted to only inputs which have this label...
	//LocalBayes: These are the classifiers that are used to perform classifications on this node
	//Title(s): these are the categories which are categorized on this node 
	//Children: The trees that will be called after this node is called if they are appropriate
        Tree (final String value, final Classifier[] LocalBayes, final String[] title,
                        final Tree[] children) {
                this.value = value;
		this.LocalBayes = LocalBayes;
		this.children = children;
		this.title = title;
        }

        public Tree[] getChildren() {
                return children;
        }

        public String getValue() {
                return value;
        }

	public String[] getTitles(){
		return title;
	}

	public String getTitles(int i){
		return title[i];
	}

	public Classifier[] getClassifiers(){
		return LocalBayes;
	}

	public Classifier getClassifiers(int i){
		return LocalBayes[i];
	}

	Tree (Tree T){
		this.children = T.getChildren();
		this.value = T.getValue();
		this.title = T.getTitles();
		this.LocalBayes = T.getClassifiers();
	}


	public void print(int depth){
		System.out.print("Depth: " + depth + "\n");
		System.out.print("Value: " + this.value + "\n");
		System.out.print("Title: " + this.title[0] + "\n\n");
		Tree[] lchildren = this.getChildren();
		for(int i = 0; i < lchildren.length; i++)
			lchildren[i].print(depth + 1);
	}
}
