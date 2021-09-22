/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uni_hamburg.corpora.validation;

import de.uni_hamburg.corpora.Corpus;
import de.uni_hamburg.corpora.CorpusData;
import de.uni_hamburg.corpora.CorpusFunction;
import de.uni_hamburg.corpora.CorpusIO;
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
import static org.apache.commons.lang3.StringEscapeUtils.escapeHtml4;

/**
 *
 * @author fsnv625
 *
 * This class issues warnings if the exb file contains linebreaks or fixes
 * linebreaks in the events and adds those warnings to the report which it
 * returns.
 *
 */
public class ExbEventLinebreaksChecker extends Checker implements CorpusFunction {

    boolean linebreak = false;
    String xpathContext = "//event";
    XPath context;
    Document doc;

    public ExbEventLinebreaksChecker() {
        //fixing option available
        super(true);
    }

    /**
     * One of the main functionalities of the feature; issues warnings if the
     * exb file contains linebreaks in events and add that warning to the report
     * which it returns.
     */
    @Override
    public Report function(CorpusData cd, Boolean fix) // check whether there's any illegal apostrophes '
            throws SAXException, IOException, ParserConfigurationException, URISyntaxException, JDOMException, TransformerException, XPathExpressionException {
        Report stats = new Report();         // create a new report
        doc = TypeConverter.String2JdomDocument(cd.toSaveableString()); // read the file as a doc
        Pattern replacePattern = Pattern.compile("[\r\n]");
        context = XPath.newInstance(xpathContext);
        List allContextInstances = context.selectNodes(doc);
        CorpusIO cio = new CorpusIO();
        String s = "";
        if (!allContextInstances.isEmpty()) {
            for (int i = 0; i < allContextInstances.size(); i++) {
                Object o = allContextInstances.get(i);
                if (o instanceof Element) {
                    Element e = (Element) o;
                    s = e.getText();
                    if (replacePattern.matcher(s).find()) {          // if file contains the RegEx then issue warning
                        linebreak = true;
                        if (fix) {
                            String snew = s.replaceAll("[\r\n]", "");    //replace all replace with replacement
                            //TODO Attributes?
                            e.setText(snew);
                            cd.updateUnformattedString(doc.toString());
                            cio.write(cd, cd.getURL());
                            stats.addFix(function, cd, "Removed line ending in an event: " + escapeHtml4(s) + " with " + escapeHtml4(snew));
                        } else {
                            System.out.println("Exb is containing line ending in an event: " + escapeHtml4(s));
                            stats.addCritical(function, cd, "Exb is containing line ending in an event: " + escapeHtml4(s));
                        }
                    }
                }
            }
            if (!linebreak) {
                stats.addCorrect(function, cd, "CorpusData file does not contain line ending in an event");
            }
        } else {
            stats.addCorrect(function, cd, "CorpusData file does not contain any event");
        }
        return stats; // return the report with warnings
    }

    /**
     * Default function which determines for what type of files (basic
     * transcription, segmented transcription, coma etc.) this feature can be
     * used.
     */
    @Override
    public Collection<Class<? extends CorpusData>> getIsUsableFor() throws ClassNotFoundException {
        Class cl = Class.forName("de.uni_hamburg.corpora.EXMARaLDACorpusData");
        IsUsableFor.add(cl);
        return IsUsableFor;
    }

    /**
     * Default function which returns a two/three line description of what this
     * class is about.
     */
    @Override
    public String getDescription() {
        String description = "This class issues warnings if the exb file contains "
                + "linebreaks or fixes linebreaks in the events and adds those "
                + "warnings to the report which it returns.";
        return description;
    }

    @Override
    public Report function(Corpus c, Boolean fix) throws SAXException, IOException, ParserConfigurationException, URISyntaxException, JDOMException, TransformerException, XPathExpressionException {
        Report stats = new Report();
        for (CorpusData cdata : c.getBasicTranscriptionData()) {
            stats.merge(function(cdata, fix));
        }
        return stats;
    }

}
