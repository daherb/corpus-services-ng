/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uni_hamburg.corpora.validation;

import de.uni_hamburg.corpora.*;
import de.uni_hamburg.corpora.utilities.TypeConverter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.*;
import java.util.regex.Pattern;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpressionException;
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
    String replace = "'";
    String replacement = "´";
    boolean coma = false;
    String xpathContext = "//*";
    Document doc = null;
    XPath context;

    public CorpusDataRegexReplacer(Properties properties) {
        super(true, properties);
    }

    /**
     * One of the main functionalities of the feature; issues warnings if the
     * coma file contains containsRegEx ’and add that warning to the report
     * which it returns.
     */
    @Override
    public Report function(CorpusData cd, Boolean fix) // check whether there's any regEx instances on specified XPath
            throws SAXException, IOException, ParserConfigurationException, URISyntaxException, JDOMException, TransformerException, XPathExpressionException {

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
                    if (replacePattern.matcher(s).find()) {          // if file contains the RegEx then issue warning
                        containsRegEx = true;
                        if (fix) {
                            String snew = s.replaceAll(replace, replacement);    //replace all replace with replacement
                            //TODO Attributes?
                            e.setText(snew);
                            stats.addFix(function, cd, "Replaced " + escapeHtml4(replace) + " with " + escapeHtml4(replacement) + " at " + escapeHtml4(xpathContext) + " here: " + escapeHtml4(s) + " with " + escapeHtml4(snew));
                        } else {
                            System.out.println("CorpusData file is containing " + escapeHtml4(replace) + " at " + escapeHtml4(xpathContext) + ": " + escapeHtml4(s));
                            stats.addCritical(function, cd, "CorpusData file is containing " + escapeHtml4(replace) + " at " + escapeHtml4(xpathContext) + ": " + escapeHtml4(s));
                        }
                    }
                } else if (o instanceof Attribute) {
                    Attribute a = (Attribute) o;
                    s = a.getValue();
                    if (fix) {
                        System.out.println("Attributes cannot be replaced yet at " + escapeHtml4(xpathContext));
                        stats.addCritical(function, cd, "Attributes cannot be replaced yet at " + escapeHtml4(xpathContext));
                    }
                } else {
                    stats.addWarning(function, cd, "Xpath " + escapeHtml4(xpathContext) + " does not lead to Element or Attribute");
                    s = "";
                }

            }
            if (!containsRegEx) {
                stats.addCorrect(function, cd, "CorpusData file does not contain " + escapeHtml4(replace) + " at " + escapeHtml4(xpathContext));
            } else if (fix) {
                CorpusIO cio = new CorpusIO();
                cd.updateUnformattedString(TypeConverter.JdomDocument2String(doc));
                cio.write(cd, cd.getURL());
            } else {
                stats.addCorrect(function, cd, "CorpusData file does not contain anything at " + escapeHtml4(xpathContext));
            }
        }
        return stats; // return the report with warnings
    }

    @Override
    public Collection<Class<? extends CorpusData>> getIsUsableFor() {
        Set<Class<? extends CorpusData>> IsUsableFor = new HashSet<>();
        if (coma) {
            IsUsableFor.add(ComaData.class);
        } else {
            IsUsableFor.add(EXMARaLDACorpusData.class);
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

    public void setComa(String s) throws IllegalArgumentException {
        if (s.equalsIgnoreCase("true") || s.equalsIgnoreCase("wahr") || s.equalsIgnoreCase("ja")) {
            coma = true;
        } else if (s.equalsIgnoreCase("false") || s.equalsIgnoreCase("falsch") || s.equalsIgnoreCase("nein")) {
            coma = false;
        } else {
            throw new IllegalArgumentException("Parameter coma not recognized: " + escapeHtml4(s));
        }
    }

    /**
     * Default function which returns a two/three line description of what this
     * class is about.
     */
    @Override
    public String getDescription() {
        String description = "This class issues warnings if a file contains a certain RegEx and can also replace";
        return description;
    }

    @Override
    public Report function(Corpus c, Boolean fix) throws SAXException, IOException, ParserConfigurationException, URISyntaxException, JDOMException, TransformerException, XPathExpressionException {
        Report stats = new Report();
        if (coma) {
            cd = c.getComaData();
            stats = function(cd, fix);

        } else {
            for (EXMARaLDACorpusData btd : c.getBasicTranscriptionData()) {
                stats.merge(function(btd, fix));
            }
        }
        return stats;
    }
}
