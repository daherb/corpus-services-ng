/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uni_hamburg.corpora.conversion;

import de.uni_hamburg.corpora.Corpus;
import de.uni_hamburg.corpora.CorpusData;
import de.uni_hamburg.corpora.CorpusFunction;
import de.uni_hamburg.corpora.Report;
import de.uni_hamburg.corpora.validation.ValidatorSettings;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpressionException;
import org.exmaralda.partitureditor.fsm.FSMException;
import org.exmaralda.partitureditor.jexmaralda.JexmaraldaException;
import org.jdom.JDOMException;
import org.xml.sax.SAXException;

/**
 *
 * an abstract class to be extended by additional validators or checkers This
 * Class reads a File and outputs errors but doesn't change it The commandline
 * input is the file to be checked as a string
 *
 *
 * How to also put another file as input for an check?
 *
 */
public abstract class Converter implements CorpusFunction {

    /**
     *
     *
     */
    //I will keep the settings for now, so they can stay as they are for the Moment 
    //and we know where to refactor when we change them 
    ValidatorSettings settings;
    Report report;
    Collection<Class<? extends CorpusData>> IsUsableFor = new ArrayList<Class<? extends CorpusData>>();
    final String function;
    Boolean canfix = false;
    protected Properties props;

    public Converter(Properties properties) {
        function = this.getClass().getSimpleName();
        props = properties;
    }

    public Report execute(CorpusData cd) {
        report = new Report();
        try {
            report = function(cd);
            return report;
        } catch (JexmaraldaException je) {
            report.addException(function, je, cd, "Unknown parsing error");
        } catch (JDOMException jdome) {
            report.addException(function, jdome, cd, "Unknown parsing error");
        } catch (SAXException saxe) {
            report.addException(function, saxe, cd, "Unknown parsing error");
        } catch (IOException ioe) {
            report.addException(function, ioe, cd, "File reading error");
        } catch (FSMException ex) {
            report.addException(function, ex, cd, "File reading error");
        } catch (URISyntaxException ex) {
            report.addException(function, ex, cd, "File reading erro");
        } catch (ParserConfigurationException ex) {
            report.addException(function, ex, cd, "File reading error");
        } catch (TransformerException ex) {
            report.addException(function, ex, cd, "File reading error");
        } catch (XPathExpressionException ex) {
            report.addException(function, ex, cd, "File reading error");
        } catch (ClassNotFoundException ex) {
            report.addException(function, ex, cd, "File reading error");
        } catch (NoSuchAlgorithmException ex) {
            report.addException(function, ex, cd, "File reading error");
        } catch (Exception ex) {
            Logger.getLogger(Converter.class.getName()).log(Level.SEVERE, null, ex);
        }
        return report;
    }

    public Report execute(Corpus c) {
        report = new Report();
        try {
            report = function(c);
            return report;
        } catch (JexmaraldaException je) {
            report.addException(function, je, "Unknown parsing error");
        } catch (JDOMException jdome) {
            report.addException(function, jdome, "Unknown parsing error");
        } catch (SAXException saxe) {
            report.addException(function, saxe, "Unknown parsing error");
        } catch (IOException ioe) {
            report.addException(function, ioe, "File reading error");
        } catch (FSMException ex) {
            report.addException(function, ex, "File reading error");
        } catch (URISyntaxException ex) {
            report.addException(function, ex, "File reading erro");
        } catch (ParserConfigurationException ex) {
            report.addException(function, ex, "File reading error");
        } catch (TransformerException ex) {
            report.addException(function, ex, "File reading error");
        } catch (XPathExpressionException ex) {
            report.addException(function, ex, "File reading error");
        } catch (ClassNotFoundException ex) {
            report.addException(function, ex, "File reading error");
        } catch (NoSuchAlgorithmException ex) {
            report.addException(function, ex, "File reading error");
        } catch (Exception ex) {
            Logger.getLogger(Converter.class.getName()).log(Level.SEVERE, null, ex);
        }
        return report;
    }

    public Report execute(CorpusData cd, boolean fix) {
        return execute(cd);
    }

    public Report execute(Corpus c, boolean fix) {
        return execute(c);
    }

    public abstract Report function(Corpus c) throws Exception, NoSuchAlgorithmException, ClassNotFoundException, FSMException, URISyntaxException, SAXException, IOException, ParserConfigurationException, JexmaraldaException, TransformerException, XPathExpressionException, JDOMException;

    ;

    public abstract Report function(CorpusData cd) throws Exception, NoSuchAlgorithmException, ClassNotFoundException, FSMException, URISyntaxException, SAXException, IOException, ParserConfigurationException, JexmaraldaException, TransformerException, XPathExpressionException, JDOMException;

    ;


    @Override
    public abstract Collection<Class<? extends CorpusData>> getIsUsableFor();

    public void setIsUsableFor(Collection<Class<? extends CorpusData>> cdc) {
        for (Class cl : cdc) {
            IsUsableFor.add(cl);
        }
    }

    public String getFunction() {
        return function;
    }

    public Boolean getCanFix() {
        return canfix;
    }

    @Override
    public Map<String, String> getParameters() {
        return new HashMap<>();
    }
}
