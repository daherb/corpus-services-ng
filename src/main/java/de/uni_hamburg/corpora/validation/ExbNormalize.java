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
 * This class normalises the basic transcription data using the EXMARaLDA
 * function and fixes white spaces if set by a parameter.
 *
 */
public class ExbNormalize extends Checker implements CorpusFunction {

    Document doc = null;
    EXMARaLDACorpusData btd = null;
    Boolean fixWhiteSpaces = false;

    public ExbNormalize() {
        super(true);
    }

    @Override
    public Report function(CorpusData cd, Boolean fix) throws TransformerException, ParserConfigurationException, SAXException, IOException, XPathExpressionException, JDOMException {
        Report report = new Report();
        if (fix) {
            btd = (EXMARaLDACorpusData) cd;
            BasicTranscription bt = btd.getEXMARaLDAbt();
            bt.normalize();
            if (fixWhiteSpaces) {
                bt.normalizeWhiteSpace();
            }
            btd.setReadbtasjdom(bt.toJDOMDocument());
            btd.setOriginalString(bt.toXML());
            //btd.updateReadbtasjdom();
            cd = (CorpusData) btd;
            CorpusIO cio = new CorpusIO();
            cio.write(cd, cd.getURL());
            if (cd != null) {
                report.addFix(function, cd, "normalized the file");
            } else {
                report.addCritical(function, cd, "normalizing was not possible");
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

    public void setfixWhiteSpaces(String s) {
        fixWhiteSpaces = false;
        if (s.equals("true") || s.equals("wahr") || s.equals("ja") || s.equals("yes")) {
            fixWhiteSpaces = true;
        }
    }

    /**
     * Default function which returns a two/three line description of what this
     * class is about.
     */
    @Override
    public String getDescription() {
        String description = "This class normalises the basic transcription data using "
                + "the EXMARaLDA function and fixes white spaces if set by a parameter. ";
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
