/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uni_hamburg.corpora.publication;

import de.uni_hamburg.corpora.Corpus;
import de.uni_hamburg.corpora.CorpusData;
import de.uni_hamburg.corpora.CorpusFunction;
import de.uni_hamburg.corpora.Report;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpressionException;
import org.exmaralda.partitureditor.fsm.FSMException;
import org.exmaralda.partitureditor.jexmaralda.JexmaraldaException;
import org.jdom.JDOMException;
import org.xml.sax.SAXException;

/**
 *
 * @author fsnv625
 */
public abstract class Publisher implements CorpusFunction {

    CorpusData cd;
    Report report;
    Collection<Class<? extends CorpusData>> IsUsableFor = new ArrayList<Class<? extends CorpusData>>();
    final String function;
    Boolean canfix = false;

    public Publisher(){
        function = this.getClass().getSimpleName();
    }

    public Report execute(Corpus c) {
            report = new Report();
        try {

            report = function(c);
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
        }
        return report;
    }

    public Report execute(CorpusData cd) {
        report = new Report();
        try {
            report = function(cd);
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
        }
        return report;
    }

    //no fix boolean needed
    public Report execute(CorpusData cd, boolean fix) {
        return execute(cd);
    }

    //no fix boolean needed
    public Report execute(Corpus c, boolean fix) {
        return execute(c);
    }

    //to be implemented in class
    public abstract Report function(CorpusData cd) throws NoSuchAlgorithmException, ClassNotFoundException, FSMException, URISyntaxException, SAXException, IOException, ParserConfigurationException, JexmaraldaException, TransformerException, XPathExpressionException, JDOMException;


    //to be implemented in class
    public abstract Report function(Corpus c) throws NoSuchAlgorithmException, ClassNotFoundException, FSMException, URISyntaxException, SAXException, IOException, ParserConfigurationException, JexmaraldaException, TransformerException, XPathExpressionException, JDOMException;


    public abstract Collection<Class<? extends CorpusData>> getIsUsableFor();

    public void setIsUsableFor(Collection<Class<? extends CorpusData>> cdc) {
        for (Class<? extends CorpusData> cl : cdc) {
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
        return Collections.EMPTY_MAP;
    }
}
