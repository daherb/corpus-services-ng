/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uni_hamburg.corpora.validation;

import de.uni_hamburg.corpora.Corpus;
import de.uni_hamburg.corpora.CorpusData;
import de.uni_hamburg.corpora.CorpusFunction;
import de.uni_hamburg.corpora.Report;
import java.io.IOException;
import java.util.Collection;
import java.util.regex.Pattern;
import org.jdom.Document;
import org.jdom.JDOMException;
import org.jdom.xpath.XPath;
import org.xml.sax.SAXException;
import de.uni_hamburg.corpora.utilities.TypeConverter;
import java.net.URISyntaxException;
import java.util.List;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpressionException;
import org.jdom.Element;


/**
 *
 * @author bay7303
 */
public class ElanPunctuationChecker extends Checker implements CorpusFunction {

    boolean badPunctuation = false;
    String xpathContext = "/ANNOTATION_DOCUMENT/TIER[@LINGUISTIC_TYPE_REF!='no']/ANNOTATION/*";
    XPath context;
    Document doc;

    public ElanPunctuationChecker() {
        //fixing option not available
        super(false);
    }

    /**
     * Checks ELAN files for punctuation marks causing problems when import to
     * FLEX is being done.
     */
    @Override
    public Report function(CorpusData cd, Boolean fix) 
            throws SAXException, IOException, ParserConfigurationException, URISyntaxException, JDOMException, TransformerException, XPathExpressionException {
        Report stats = new Report();         // create a new report
        doc = TypeConverter.String2JdomDocument(cd.toSaveableString()); // read the file as a doc
        Pattern colonPattern = Pattern.compile(":$");
        Pattern dotPattern = Pattern.compile("\\..+$");
        Pattern qmarkPattern = Pattern.compile("(?<=[^\\?])\\?{1,2}(?=[^\\?]).+?");
        context = XPath.newInstance(xpathContext);
        List allContextInstances = context.selectNodes(doc);
        String start = "";
        String finish = "";
        if (!allContextInstances.isEmpty()) {
            for (int i = 0; i < allContextInstances.size(); i++) {
                Object o = allContextInstances.get(i);
                if (o instanceof Element) {
                    Element e = (Element) o;
                    String s = e.getChildText("ANNOTATION_VALUE");
                    if (colonPattern.matcher(s).find() || dotPattern.matcher(s).find() || qmarkPattern.matcher(s).find()) {
                        Object a = "";
                        System.out.println(e.getAttributeValue("ANNOTATION_REF"));
                        if (e.getAttributeValue("ANNOTATION_REF") != null) {
                            String att = e.getAttributeValue("ANNOTATION_REF");
                            String xpathAnnID = "//ALIGNABLE_ANNOTATION[@ANNOTATION_ID='" + att + "']";
                            XPath annID = XPath.newInstance(xpathAnnID);
                            a = annID.selectSingleNode(doc);
                        } else {
                            a = o;
                        }
                        if (a instanceof Element) {
                            Element ee = (Element) a;
                            String begin = ee.getAttributeValue("TIME_SLOT_REF1");
                            String end = ee.getAttributeValue("TIME_SLOT_REF2");
                            String xpathBegTime = "//TIME_SLOT[@TIME_SLOT_ID='" + begin + "']";
                            String xpathEndTime = "//TIME_SLOT[@TIME_SLOT_ID='" + end + "']";
                            XPath beginTime = XPath.newInstance(xpathBegTime);
                            Object bt = beginTime.selectSingleNode(doc);
                            if (bt instanceof Element) {
                                Element eb = (Element) bt;
                                start = eb.getAttributeValue("TIME_VALUE");
                                System.out.println(start);
                                XPath endTime = XPath.newInstance(xpathEndTime);
                                Object et = endTime.selectSingleNode(doc);
                                if (et instanceof Element) {
                                    Element ec = (Element) et;
                                    finish = ec.getAttributeValue("TIME_VALUE");
                                    System.out.println(finish);
                                }
                            }
                        badPunctuation = true;
                        stats.addWarning(function, cd, "Bad punctuation in : " + s + " Start: " + start +  " Finish: " + finish);
                        }
                    } 
                }
            }
            if (!badPunctuation) {
                stats.addCorrect(function, cd, "Punctuation OK");
            }
        } else {
            stats.addCorrect(function, cd, "The file does not contain any tiers");
        }
        return stats;
    }

    /**
     * Default function which determines for what type of files (basic
     * transcription, segmented transcription, coma etc.) this feature can be
     * used.
     */
    @Override
    public Collection<Class<? extends CorpusData>> getIsUsableFor() throws ClassNotFoundException {
        Class cl = Class.forName("de.uni_hamburg.corpora.ELANData");
        IsUsableFor.add(cl);
        return IsUsableFor;
    }

    /**
     * Default function which returns a two/three line description of what this
     * class is about.
     */
    @Override
    public String getDescription() {
        String description = "This class issues warnings if the ELAN file contains punctuation marks causing problems when import to FLEX is being done.";
        return description;
    }

    @Override
    public Report function(Corpus c, Boolean fix) throws SAXException, IOException, ParserConfigurationException, URISyntaxException, JDOMException, TransformerException, XPathExpressionException {
        Report stats = new Report();
        for (CorpusData cdata : c.getELANData()) {
            stats.merge(function(cdata, fix));
        }
        return stats;
    }

}