/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uni_hamburg.corpora.validation;

import de.uni_hamburg.corpora.*;
import de.uni_hamburg.corpora.EXMARaLDACorpusData;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collection;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpressionException;
import org.jdom.Document;
import org.jdom.JDOMException;
import org.exmaralda.partitureditor.jexmaralda.BasicTranscription;
import org.xml.sax.SAXException;

/**
 *
 * @author fsnv625
 *
 * This class makes the timeline of exbs consistent by removing incorrect
 * timepoints and interpolates timeline items without time info if the parameter
 * is set.
 *
 */
public class ExbMakeTimelineConsistent extends Checker implements CorpusFunction {

    Document doc = null;
    EXMARaLDACorpusData btd = null;
    Boolean interpolateTimeline = false;

    public ExbMakeTimelineConsistent() {
        //fixing option available
        super(true);
    }

    @Override
    public Report function(CorpusData cd, Boolean fix) throws JDOMException, IOException, TransformerException, ParserConfigurationException, SAXException, XPathExpressionException {
        Report report = new Report();
        if (fix) {

            btd = (EXMARaLDACorpusData) cd;
            BasicTranscription bt = btd.getEXMARaLDAbt();
            bt.getBody().getCommonTimeline().makeConsistent();
            if (interpolateTimeline) {
                bt.getBody().getCommonTimeline().completeTimes();
            }

            btd.setReadbtasjdom(bt.toJDOMDocument());
            btd.setOriginalString(bt.toXML(bt.getTierFormatTable()));
            //btd.updateReadbtasjdom();
            cd = (CorpusData) btd;
            CorpusIO cio = new CorpusIO();
            cio.write(cd, cd.getURL());
            if (cd != null) {
                report.addFix(function, cd, "made timeline consistent");
            } else {
                report.addCritical(function, cd, "making timeline consistent not possible");
            }

        } else {
            report.addCritical(function, cd, "Checking option is not available");
        }
        return report;
    }

    @Override
    public Collection<Class<? extends CorpusData>> getIsUsableFor() throws ClassNotFoundException {
        Class cl = Class.forName("de.uni_hamburg.corpora.EXMARaLDACorpusData");
        IsUsableFor.add(cl);
        return IsUsableFor;
    }

    public void setInterpolateTimeline(String s) {
        interpolateTimeline = false;
        if (s.equals("true") || s.equals("wahr") || s.equals("ja") || s.equals("yes")) {
            interpolateTimeline = true;
        }
    }

    /**
     * Default function which returns a two/three line description of what this
     * class is about.
     */
    @Override
    public String getDescription() {
        String description = "This class makes the timeline of exbs consistent by removing "
                + "incorrect timepoints and interpolates timeline items without time "
                + "info if the parameter is set. ";
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
