/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uni_hamburg.corpora.validation;

import de.uni_hamburg.corpora.*;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Properties;
import java.util.regex.Pattern;
import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.jdom2.filter.Filters;
import org.jdom2.xpath.XPathBuilder;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.jaxen.JaxenXPathFactory;
import org.xml.sax.SAXException;
import de.uni_hamburg.corpora.utilities.TypeConverter;
import java.net.URISyntaxException;
import java.util.List;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpressionException;
import org.jdom2.Element;


/**
 *
 * @author bay7303
 *
 * Last updated
 * @author Herbert Lange
 * @version 20240322
 */
public class FlextextPunctuationChecker extends Checker implements CorpusFunction {

    boolean badPunctuation = false;
    String xpathContext = "//item[@type='punct']";
    Document doc;
    String flexFile = "";

    public FlextextPunctuationChecker(Properties properties) {
        //fixing is possible
        super(true, properties);
    }

    /**
     * One of the main functionalities of the feature; issues warnings if the
     * flextext file contains punctuation marks causing problems when conversion
     * to EXMARaLDA is being done.
     */
    @Override
    public Report function(CorpusData cd, Boolean fix) // check whether there's any illegal apostrophes '
            throws SAXException, IOException, ParserConfigurationException, URISyntaxException, JDOMException, TransformerException, XPathExpressionException {
        Report stats = new Report();         // create a new report
        flexFile = cd.toSaveableString();
        doc = TypeConverter.String2JdomDocument(flexFile); // read the file as a doc
        Pattern bracketsPattern = Pattern.compile("[«»]");
        Pattern slashPattern = Pattern.compile("^…\\/$");
        XPathExpression<Element> context = new XPathBuilder<>(xpathContext, Filters.element()).compileWith(new JaxenXPathFactory());
        List<Element> allContextInstances = context.evaluate(doc);
        CorpusIO cio = new CorpusIO();
        String s = "";
        if (!allContextInstances.isEmpty()) {
            for (Element e : allContextInstances) {
                s = e.getText();
                if (bracketsPattern.matcher(s).find()) {          // if file contains the RegEx then issue warning
                    badPunctuation = true;
                    stats.addCritical(function, cd, "Remove «» brackets in :" + s);
                }
                if (slashPattern.matcher(s).find()) {
                    badPunctuation = true;
                    stats.addCritical(function, cd, "Add a slash before " + s);
                }
            }
            if (badPunctuation && fix) {
                flexFile = flexFile.replaceAll("«", "“");
                flexFile = flexFile.replaceAll("»", "”");
                /// TODO RegEx
                //testRegEx = flexFile.matches("(?:[^\\/])…\\/");
                flexFile = flexFile.replaceAll("(?<=\\>)…\\/", "\\/…\\/");
                //flexFile = flexFile.replaceAll("…\\/", "\\/…\\/");
                //flexFile = flexFile.replaceAll("\\/\\/…", "\\/…");
                cd.updateUnformattedString(flexFile);
                cio.write(cd, cd.getURL());
                stats.addFix(function, cd, "Corrected punctuation");
            }
            if (!badPunctuation) {
                stats.addCorrect(function, cd, "CorpusData file does not contain bad punctuation");
            }
        } else {
            stats.addCorrect(function, cd, "CorpusData file does not contain any transcription");
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
        return Collections.singleton((FlextextData.class));
    }


    /**
     * Default function which returns a two/three line description of what this
     * class is about.
     */
    @Override
    public String getDescription() {
        String description = "This class issues warnings if the flextext file contains punctuation marks causing problems when conversion to EXMARaLDA is being done";
        return description;
    }

    @Override
    public Report function(Corpus c, Boolean fix) throws SAXException, IOException, ParserConfigurationException, URISyntaxException, JDOMException, TransformerException, XPathExpressionException {
        Report stats = new Report();
        for (CorpusData cdata : c.getFlextextData()) {
            stats.merge(function(cdata, fix));
        }
        return stats;
    }

}