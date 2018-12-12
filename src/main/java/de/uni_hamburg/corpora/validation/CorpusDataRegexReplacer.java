/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uni_hamburg.corpora.validation;

import de.uni_hamburg.corpora.CorpusData;
import de.uni_hamburg.corpora.CorpusFunction;
import de.uni_hamburg.corpora.CorpusIO;
import de.uni_hamburg.corpora.Report;
import de.uni_hamburg.corpora.utilities.TypeConverter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;
import javax.xml.parsers.ParserConfigurationException;
import org.exmaralda.partitureditor.jexmaralda.JexmaraldaException;
import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.xpath.XPath;
import org.xml.sax.SAXException;
import static org.apache.commons.lang3.StringEscapeUtils.escapeHtml4;

/**
 *
 * @author fsnv625
 */
public class CorpusDataRegexReplacer extends Checker implements CorpusFunction {

    //ToDo 
    boolean containsRegEx = false;
    String cdrr = "CorpusDataRegexReplacer";
    String replace = "'";
    String replacement = "´";
    boolean coma = false;
    String xpathContext = "/test";
    Document doc = null;
    XPath context;

    @Override
    public Report check(CorpusData cd) throws SAXException, JexmaraldaException {
        Report stats = new Report();
        try {
            stats = exceptionalCheck(cd);
        } catch (ParserConfigurationException pce) {
            stats.addException(pce, cdrr, cd, "Unknown parsing error");
        } catch (SAXException saxe) {
            stats.addException(saxe, cdrr, cd, "Unknown parsing error");
        } catch (IOException ioe) {
            stats.addException(ioe, cdrr, cd, "Unknown file reading error");
        } catch (URISyntaxException ex) {
            stats.addException(ex, cdrr, cd, "Unknown file reading error");
        } catch (JDOMException ex) {
            stats.addException(ex, cdrr, cd, "Unknown parsing error");
        }
        return stats;
    }

    /**
     * One of the main functionalities of the feature; issues warnings if the
     * coma file contains containsRegEx ’and add that warning to the report
     * which it returns.
     */
    private Report exceptionalCheck(CorpusData cd) // check whether there's any regEx instances on specified XPath
            throws SAXException, IOException, ParserConfigurationException, URISyntaxException, JDOMException {
        Report stats = new Report();         // create a new report
        doc = TypeConverter.String2JdomDocument(cd.toSaveableString()); // read the file as a doc
        Pattern replacePattern = Pattern.compile(replace);
        context = XPath.newInstance(xpathContext);
        List allContextInstances = context.selectNodes(doc);
        String s;
        if (!allContextInstances.isEmpty()) {
            for (int i = 0; i < allContextInstances.size(); i++) {
                Object o = allContextInstances.get(i);
                if (o instanceof Element) {
                    Element e = (Element) o;
                    s = e.getText();
                } else if (o instanceof Attribute) {
                    Attribute a = (Attribute) o;
                    s = a.getValue();
                }
                else {
                    stats.addWarning(cdrr, cd, "Xpath " + escapeHtml4(xpathContext) + " does not lead to Element or Attribute");
                    s ="";
                }
                if (replacePattern.matcher(s).find()) {          // if file contains the RegEx then issue warning
                    containsRegEx = true;
                    System.err.println("CorpusData file is containing " + escapeHtml4(replace) + " at " + escapeHtml4(xpathContext) + ": " + escapeHtml4(s));
                    stats.addCritical(cdrr, cd, "CorpusData file is containing " + escapeHtml4(replace) + " at " + escapeHtml4(xpathContext) + ": " + escapeHtml4(s));
                }

            }
            if (!containsRegEx) {
                stats.addCorrect(cdrr, cd, "CorpusData file does not contain " + escapeHtml4(replace) + " at " + escapeHtml4(xpathContext));
            }
        } else {
            stats.addCorrect(cdrr, cd, "CorpusData file does not contain anything at " + escapeHtml4(xpathContext));
        }
        return stats; // return the report with warnings
    }

    @Override
    public Report fix(CorpusData cd) throws SAXException, JDOMException, IOException, JexmaraldaException {
        Report stats = new Report();         // create a new report
        Pattern replacePattern = Pattern.compile(replace);
        doc = TypeConverter.String2JdomDocument(cd.toSaveableString()); // read the file as a doc
        context = XPath.newInstance(xpathContext);
        List allContextInstances = context.selectNodes(doc);
        if (!allContextInstances.isEmpty()) {
            for (int i = 0; i < allContextInstances.size(); i++) {
                Object o = allContextInstances.get(i);
                //TODO Attributes?
                Element e = (Element) o;
                String s = e.getText();
                if (replacePattern.matcher(s).find()) {          // if file contains the RegEx then issue warning
                    containsRegEx = true;
                    String snew = s.replaceAll(replace, replacement);    //replace all replace with replacement
                    //TODO Attributes?
                    e.setText(snew);
                    stats.addCorrect(cdrr, cd, "Replaced " + escapeHtml4(replace) + " with " + escapeHtml4(replacement) + " at " + escapeHtml4(xpathContext) + " here: " + escapeHtml4(s) + " with " + escapeHtml4(snew));
                }
            }
            if (containsRegEx) {
                CorpusIO cio = new CorpusIO();
                cio.write(doc, cd.getURL());
            } else {
                stats.addCorrect(cdrr, cd, "CorpusData file does not contain " + escapeHtml4(replace) + " at " + escapeHtml4(xpathContext));
            }
        } else {
            stats.addCorrect(cdrr, cd, "CorpusData file does not contain anything at " + escapeHtml4(xpathContext));
        }
        return stats;
    }

    @Override
    public Collection<Class<? extends CorpusData>> getIsUsableFor() {
        try {
            if (coma) {
                Class cl3 = Class.forName("de.uni_hamburg.corpora.ComaData");
                IsUsableFor.add(cl3);
            } else {
                Class cl = Class.forName("de.uni_hamburg.corpora.BasicTranscriptionData");
                IsUsableFor.add(cl);
            }
        } catch (ClassNotFoundException ex) {
            report.addException(ex, "Usable class not found.");
        }
        return IsUsableFor;
    }

    public void setReplace(String s) {
        replace = s;
    }

    public void setReplacement(String s) {
        replacement = s;
    }

    public void setXpathContext(String s) {
        xpathContext = s;
    }

    public void setComa(String s) {
        if (s.equalsIgnoreCase("true") || s.equalsIgnoreCase("wahr") || s.equalsIgnoreCase("ja")) {
            coma = true;
        } else if (s.equalsIgnoreCase("false") || s.equalsIgnoreCase("falsch") || s.equalsIgnoreCase("nein")) {
            coma = false;
        } else {
            report.addCritical(cdrr, cd, "Parameter coma not recognized: " + escapeHtml4(s));
        }
    }

}