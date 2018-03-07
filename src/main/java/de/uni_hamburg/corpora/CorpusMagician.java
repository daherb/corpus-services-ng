/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uni_hamburg.corpora;

import de.uni_hamburg.corpora.validation.PrettyPrintData;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.lang.String;
import java.util.Arrays;


/**
 * This class has a Corpus and a Corpus Function as a field and is able to run a
 * Corpus Function on a corpus in a main method.
 *
 * @author fsnv625
 */
public class CorpusMagician {

    //the whole corpus I want to run checks on
    Corpus corpus;
    //one file I want to run a check on 
    CorpusData corpusData;
    //all functions there are in the code
    Collection<String> allExistingCFs;
    //all functions that should be run
    Collection<String> chosencorpusfunctions = new ArrayList();
    //the final Report
    Report report = new Report();

    public CorpusMagician() {
    }

    //TODO main method
    //TODO we need a webservice for this functionality too
    //in the furture (for repo and external users)
    //this should work via commandline like that:
    //java -cp hzsk-corpus-services-0.1.1.jar de.uni_hamburg.corpora.validation.CorpusMagician {File:///URLtocorpusfolder} 
    //%cd%/report.txt(where and how report should be stored) PrettyPrintDataFix ComaNSLinkChecker(Functions that should be run) 
    public static void main(String[] args) {
        try {
            //one args needs to be the URL
            URL url = new URL(args[0]);
            CorpusMagician corpuma = new CorpusMagician();
            corpuma.initCorpusWithURL(url);
            //now the place where Report should end up
            URL reportlocation = new URL(args[1]);
            //now add the functionsstrings to array
            //other args(2 and more) need to be a strings for the wanted corpus functions
            ArrayList<String> corpusfunctionarraylist = new ArrayList();
            //please put the args into it here
            corpuma.setChosencorpusfunctions(corpusfunctionarraylist);
            for (int i = 2; i < args.length; i++) {
                //corpuma.chosencorpusfunctions.add("test");
                corpuma.chosencorpusfunctions.add(args[i]);
                System.out.println(corpuma.chosencorpusfunctions.toString());
            }
            Report report = corpuma.runChosencorpusfunctions();
            System.out.println(report.getFullReports());
            //TODO save the Report on the url
        } catch (MalformedURLException ex) {
            Logger.getLogger(CorpusMagician.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    //Give it a path to a parameters file that tells you
    //which functions with which parameters should be
    //run on which files
    public void readConfig(URL url) {
        //this depends on how this file will be structured
    }

    //this one can write a configfile with the workflow in the
    //selected format
    public void writeConfig(URL url) {
        //needs to have more params
        //this depends on how this file will be structured
    }

    public void registerCorpusFunction(CorpusFunction cf) {
        allExistingCFs.add(cf.getClass().getName());
    }

    //creates a new empty corpus object
    public void initCorpus() {
        corpus = new Corpus();
    }

    //creates a corpus object from an URL (filepath or "real" url)
    public void initCorpusWithURL(URL url) {
        corpus = new Corpus(url);
    }

    //checks which functions exist in the code by checking for implementations of the corpus function interface
    //this shows that it doesn't work to just check for implementations of corpus functions
    //probably need to check for implementations of CorpusFunction?
    //TODO
    public Collection<String> getAllExistingCFs() {

        this.allExistingCFs = new ArrayList<String>();
        allExistingCFs.add("PrettyPrintData");
        allExistingCFs.add("ComaAddTiersFromExbsCorrector");
        allExistingCFs.add("ComaErrorReportGenerator");
        allExistingCFs.add("ComaNSLinksChecker");
        allExistingCFs.add("ExbFileReferenceChecker");
        allExistingCFs.add("ExbSegmentationChecker");
        allExistingCFs.add("ExbStructureChecker");
        allExistingCFs.add("FileCoverageChecker");

//        Reflections reflections = new Reflections("de.uni_hamburg.corpora");
//        Set<Class<? extends CorpusFunction>> classes = reflections.getSubTypesOf(CorpusFunction.class);
//        for (Class c : classes) {
//            System.out.println(c.toString());
//            try {
//                Constructor cons = c.getConstructor();
//                try {
//                    CorpusFunction cf = (CorpusFunction) cons.newInstance();
//                    allExistingCFs.add(cf.getClass().getName());
//                } catch (InstantiationException ex) {
//                    Logger.getLogger(CorpusMagician.class.getName()).log(Level.SEVERE, null, ex);
//                } catch (IllegalAccessException ex) {
//                    Logger.getLogger(CorpusMagician.class.getName()).log(Level.SEVERE, null, ex);
//                } catch (IllegalArgumentException ex) {
//                    Logger.getLogger(CorpusMagician.class.getName()).log(Level.SEVERE, null, ex);
//                } catch (InvocationTargetException ex) {
//                    Logger.getLogger(CorpusMagician.class.getName()).log(Level.SEVERE, null, ex);
//                }
//            } catch (NoSuchMethodException ex) {
//                Logger.getLogger(CorpusMagician.class.getName()).log(Level.SEVERE, null, ex);
//            } catch (SecurityException ex) {
//                Logger.getLogger(CorpusMagician.class.getName()).log(Level.SEVERE, null, ex);
//            }
//        }
        for (String cf : allExistingCFs) {
            System.out.println(cf);
        }

        return allExistingCFs;
    }

    //TODO checks which functions can be run on specified data
    public Collection<CorpusFunction> getUsableFunctions(CorpusData cd) {
        //cf.IsUsableFor();
        //some switch or if else statements for the possible java objects 
        //and a list(?) which function can be apllied to what/which functions exist?
        Collection<CorpusFunction> usablecorpusfunctions = null;
        return usablecorpusfunctions;
    }

    //TODO return default functions, this is a list that needs to be somewhere
    //or maybe its an option a corpusfunction can have?
    public Collection<CorpusFunction> getDefaultUsableFunctions() {
        Collection<CorpusFunction> defaultcorpusfunctions = null;
        return defaultcorpusfunctions;
    }

    //TODO a dialog to choose functions you want to apply
    public Collection<String> chooseFunctionDialog() {
        chosencorpusfunctions = null;
        //add the chosen Functions
        return chosencorpusfunctions;
    }

    //TODO
    //run the chosen functions on the chosen corpus
    Report runChosencorpusfunctions() {
        for (String function : chosencorpusfunctions) {
            switch (function.toLowerCase()) {
                case "prettyprintdata":
                    PrettyPrintData cf = new PrettyPrintData();
                    report.merge(runCorpusFunction(corpus, cf));
                case "prettyprintdatafix":
                    cf = new PrettyPrintData();
                    report.merge(runCorpusFunction(corpus, cf, true));
                case "comaaddtiersfromexbscorrector": 
                    //cf = new ComaAddTiersFromExbsCorrector();
                    //rest .... usw.
            }
        }
        return report;
    }

    //run multiple functions on a corpus, that means all the files in the corpus
    //the function can run on 
    public Report runCorpusFunction(Corpus c, Collection<CorpusFunction> cfc) {
        Report report = new Report();
        for (CorpusFunction cf : cfc) {
            Report newReport = runCorpusFunction(c, cf);
            report.merge(newReport);
        }
        return report;
    }

    //run multiple functions on the set corpus, that means all the files in the corpus
    //the function can run on 
    public Report runCorpusFunction(Collection<CorpusFunction> cfc) {
        Report report = new Report();
        for (CorpusFunction cf : cfc) {
            Report newReport = runCorpusFunction(corpus, cf);
            report.merge(newReport);
        }
        return report;
    }

    //run one function on a corpus, that means all the files in the corpus
    //the funciton can run on 
    public Report runCorpusFunction(Corpus c, CorpusFunction cf) {
        Report report = new Report();
        //find out on which objects this corpus function can run
        //choose those from the corpus 
        //and run the checks on those files recursively
        for (Class cl : cf.getIsUsableFor()) {
            for (CorpusData cd : c.getCorpusData()) //if the corpus files are an instance 
            //of the class cl, run the function
            {
                if (cl.isInstance(cd)) {
                    Report newReport = runCorpusFunction(cd, cf);
                    report.merge(newReport);
                }
            }
        }
        return report;
    }
    
    //run one function on a corpus, that means all the files in the corpus
    //the funciton can run on 
    public Report runCorpusFunction(Corpus c, CorpusFunction cf, boolean fix) {
        Report report = new Report();
        //find out on which objects this corpus function can run
        //choose those from the corpus 
        //and run the checks on those files recursively
        for (Class cl : cf.getIsUsableFor()) {
            for (CorpusData cd : c.getCorpusData()) //if the corpus files are an instance 
            //of the class cl, run the function
            {
                if (cd !=null && cl.isInstance(cd)) {
                    Report newReport = runCorpusFunction(cd, cf, fix);
                    report.merge(newReport);
                }
            }
        }
        return report;
    }

    //run one function on a corpus, that means all the files in the corpus
    //the function can run on 
    public Report runCorpusFunction(CorpusFunction cf) {
        Report report = new Report();
        //find out on which objects this corpus function can run
        //choose those from the corpus 
        //and run the checks on those files recursively
        for (Class cl : cf.getIsUsableFor()) {
            Report newReport = runCorpusFunction(corpus, cf);
            report.merge(newReport);
        }
        return report;
    }

    public Report runCorpusFunction(CorpusData cd, CorpusFunction cf) {
        return cf.execute(cd);
    }

    public Report runCorpusFunction(CorpusData cd, CorpusFunction cf, boolean fix) {
        return cf.execute(cd, fix);
    }

    public Report runCorpusFunctions(CorpusData cd, Collection<CorpusFunction> cfc) {
        Report report = new Report();
        for (CorpusFunction cf : cfc) {
            Report newReport = (cf.execute(cd));
            report.merge(newReport);
        }
        return report;
    }

    //TODO what was this for again....?
    public void readParameters() {

    }

    public void setCorpusData(CorpusData corpusData) {
        this.corpusData = corpusData;
    }

    public void setChosencorpusfunctions(Collection<String> chosencorpusfunctions) {
        this.chosencorpusfunctions = chosencorpusfunctions;
    }

    public Corpus getCorpus() {
        return corpus;
    }

    public CorpusData getCorpusData() {
        return corpusData;
    }

    public Collection<String> getChosencorpusfunctions() {
        return chosencorpusfunctions;
    }

}
