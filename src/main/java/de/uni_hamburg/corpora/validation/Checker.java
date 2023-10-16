/*
 *   A command-line interface for checking corpus files.
 *
 *  @author Anne Ferger
 *  @author HZSK
 */
package de.uni_hamburg.corpora.validation;

import de.uni_hamburg.corpora.*;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;
import java.util.*;
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
 * @author ???
 * @author Herbert Lange
 * @version 20230105
 */
public abstract class Checker implements CorpusFunction {

    // Same as below with report
    CorpusData cd;
    // This is dangerous and should be solved differently
    // Report report = new Report();
    final String function;
    Boolean canfix;
    protected Properties props;


    public Checker(boolean hasfixingoption, Properties properties) {
        function = this.getClass().getSimpleName();
        canfix = hasfixingoption;
        props = properties;
    }


    public Report execute(Corpus c) {
        return execute(c, false);
    }

    public Report execute(CorpusData cd) {
        return execute(cd, false);
    }

    public Report execute(CorpusData cd, boolean fix) {
        Report report = new Report();
        try {
            if (fix) {

                if (canfix) {
                    report = function(cd, fix);
                } else {
                    report.addCritical(function,
                            "Automatic fix is not available, doing check instead.");
                    report = function(cd, false);
                }

                return report;
            } else {
                report = function(cd, fix);
            }
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

    public Report execute(Corpus c, boolean fix) {
        Report report = new Report();
        try {
            if (fix) {

                if (canfix) {
                    report = function(c, fix);
                } else {
                    report.addCritical(function,
                            "Automatic fix is not yet supported.");
                }
                return report;
            } else {
                report = function(c, fix);
            }
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
        }
        return report;
    }

    //To implement in the class
    public abstract Report function(CorpusData cd, Boolean fix) throws NoSuchAlgorithmException, ClassNotFoundException, FSMException, URISyntaxException, SAXException, IOException, ParserConfigurationException, JexmaraldaException, TransformerException, XPathExpressionException, JDOMException;

    //To implement in the class
    public abstract Report function(Corpus c, Boolean fix) throws NoSuchAlgorithmException, ClassNotFoundException, FSMException, URISyntaxException, SAXException, IOException, ParserConfigurationException, JexmaraldaException, TransformerException, XPathExpressionException, JDOMException;

    public abstract Collection<Class<? extends CorpusData>> getIsUsableFor() ;

    public String getFunction() {
        return function;
    }

    public Boolean getCanFix() {
        return canfix;
    }

    /**
     * Lists all supported parameter for the checker
     * @return The map of all parameters and their description
     */
    public Map<String, String> getParameters() {
        return new HashMap<>();
    }
}
