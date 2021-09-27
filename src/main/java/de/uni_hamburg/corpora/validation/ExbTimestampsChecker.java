/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uni_hamburg.corpora.validation;

import de.uni_hamburg.corpora.*;

import java.io.IOException;
import java.util.Collection;
import org.jdom.Document;
import org.jdom.JDOMException;
import org.jdom.xpath.XPath;
import org.xml.sax.SAXException;
import de.uni_hamburg.corpora.utilities.TypeConverter;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpressionException;
import org.jdom.Element;
import static de.uni_hamburg.corpora.CorpusMagician.exmaError;

/**
 *
 * @author bay7303
 *
 * This class looks for missing timestamps and issues warnings if finds them.
 * 
 */

public class ExbTimestampsChecker extends Checker implements CorpusFunction {

    boolean missingTimestamp = false;
    Document doc;

    public ExbTimestampsChecker() {
        //fixing option not available
        super(false);
    }
        
    /**
     * One of the main functionalities of the feature; issues warnings if the
     * exs file has missing timestamps at the beginnings or ends of a segment chain.  
     */
    @Override
    public Report function(CorpusData cd, Boolean fix) // check whether there's any illegal apostrophes '
            throws SAXException, IOException, ParserConfigurationException, URISyntaxException, JDOMException, TransformerException, XPathExpressionException {
        Report stats = new Report();         // create a new report
        doc = TypeConverter.String2JdomDocument(cd.toSaveableString()); 
        String xpathSegment = "//segmentation/ts";
        XPath segment = XPath.newInstance(xpathSegment);
        List allSegments = segment.selectNodes(doc);
        CorpusIO cio = new CorpusIO();     
        for (int i = 0; i < allSegments.size(); i++) {
            Object o = allSegments.get(i);
            if (o instanceof Element) {
                Element e = (Element) o;
                String start = e.getAttributeValue("s");
                String end = e.getAttributeValue("e");
                String xpathStart = "//tli[@id='" + start + "']";
                XPath timelineStart = XPath.newInstance(xpathStart);
                List tliStart = timelineStart.selectNodes(doc);
                Object sTli = tliStart.get(0);
                if (sTli instanceof Element) {
                    Element el = (Element) sTli;
                    String id = el.getAttributeValue("id");
                    String time = el.getAttributeValue("time");
                    if (time == null) {
                        missingTimestamp = true;
                        String message = "Missing timestamp at the start of the segment chain at " + id;
                        exmaError.addError(function, cd.getURL().getFile(), "", id, false, message);
                        stats.addWarning(function, cd, message);
                    }
                }
                String xpathEnd = "//tli[@id='" + end + "']";
                XPath timelineEnd = XPath.newInstance(xpathEnd);
                List tliEnd = timelineEnd.selectNodes(doc);
                Object eTli = tliEnd.get(0);
                if (eTli instanceof Element) {
                    Element el = (Element) eTli;
                    String id = el.getAttributeValue("id");
                    String time = el.getAttributeValue("time");
                    if (time == null) {
                        missingTimestamp = true;
                        String message = "Missing timestamp at the end of the segment chain at " + id;
                        exmaError.addError(function, cd.getURL().getFile(), "", id, false, message);
                        stats.addWarning(function, cd, message);
                    }
                }
            }
        }
        if (!missingTimestamp) {
            stats.addCorrect(function, cd, "Timestamps OK");
        }
        return stats; // return the report with warnings
    }

    /**
     * Default function which determines for what type of files (basic
     * transcription, segmented transcription, coma etc.) this feature can be
     * used.
     */
    @Override
    public Collection<Class<? extends CorpusData>> getIsUsableFor() {
        return Collections.singleton(SegmentedEXMARaLDATranscription.class);
    }

    /**
     * Default function which returns a two/three line description of what this
     * class is about.
     */
    @Override
    public String getDescription() {
        String description = "This class issues warnings if it finds missing "
                + "timestamps in the timeline at the beginning or end of a segment chein";
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
 
