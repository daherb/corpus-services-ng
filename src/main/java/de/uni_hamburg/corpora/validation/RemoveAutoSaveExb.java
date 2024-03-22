/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uni_hamburg.corpora.validation;

import de.uni_hamburg.corpora.*;

import java.io.IOException;
import java.util.*;

import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.filter.Filters;
import org.jdom2.xpath.XPathBuilder;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.jaxen.JaxenXPathFactory;
import org.xml.sax.SAXException;
import static de.uni_hamburg.corpora.CorpusMagician.exmaError;
import de.uni_hamburg.corpora.utilities.TypeConverter;
import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpressionException;
import org.exmaralda.partitureditor.fsm.FSMException;
import org.exmaralda.partitureditor.jexmaralda.JexmaraldaException;

/**
 *
 * @author fsnv625
 */
public class RemoveAutoSaveExb extends Checker implements CorpusFunction {

    Document doc = null;

    public RemoveAutoSaveExb(Properties properties) {
        //fixing is possible
        super(true, properties);
    }

    @Override
    public Report function(CorpusData cd, Boolean fix) throws TransformerException, ParserConfigurationException, SAXException, IOException, XPathExpressionException {
        Report report = new Report();
        try {
            XMLData xml = (XMLData) cd;
            List al = findAllAutoSaveInstances(xml);
            //if there is no autosave, nothing needs to be done
            if (al.isEmpty()) {
                report.addCorrect(function, cd, "there is no autosave info left, nothing to do");
            } else if (fix) {
                for (Object o : al) {
                    Element e = (Element) o;
                    System.out.println(e);
                    //remove it
                    e.getParent().removeContent(e);
                }
                //then save file
                //add a report message
                xml.setJdom(doc);
                cd = (CorpusData) xml;
                cd.updateUnformattedString(TypeConverter.JdomDocument2String(doc));
                CorpusIO cio = new CorpusIO();
                cio.write(cd, cd.getURL());
                report.addFix(function, cd, "removed AutoSave info");
            } else {
                report.addCritical(function, cd, "autosave info needs to be removed");
                exmaError.addError("RemoveAutoSaveExb", cd.getURL().getFile(), "", "", false, "autosave info needs to be removed");
            }
        } catch (JDOMException ex) {
            report.addException(function, ex, cd, "Jdom Exception");
        }
        return report;
    }

    @Override
    public Collection<Class<? extends CorpusData>> getIsUsableFor() {
        Set<Class<? extends CorpusData>> IsUsableFor = new HashSet<>();
        IsUsableFor.add(EXMARaLDATranscriptionData.class);
        IsUsableFor.add(EXMARaLDASegmentedTranscriptionData.class);
        return IsUsableFor;
    }

    public List<Element> findAllAutoSaveInstances(XMLData xml) throws JDOMException {
        doc = xml.getJdom();
        //working for exs too
        XPathExpression<Element> xp1 = new XPathBuilder<>("//head/meta-information/ud-meta-information/ud-information[@attribute-name='AutoSave']", Filters.element()).compileWith(new JaxenXPathFactory());
        return xp1.evaluate(doc);
    }

    /**
     * Default function which returns a two/three line description of what this
     * class is about.
     */
    @Override
    public String getDescription() {
        String description = "This class removes auto save information present in"
                + " exb and exs files.";
        return description;
    }

    @Override
    public Report function(Corpus c, Boolean fix) throws SAXException, IOException, ParserConfigurationException, JexmaraldaException, TransformerException, XPathExpressionException, NoSuchAlgorithmException, ClassNotFoundException, FSMException, URISyntaxException, JDOMException {
        Report stats = new Report();
        for (CorpusData cdata : c.getBasicTranscriptionData()) {
            stats.merge(function(cdata, fix));
        }
        for (CorpusData sdata : c.getSegmentedTranscriptionData()) {
            stats.merge(function(sdata, fix));
        }
        return stats;
    }
}
