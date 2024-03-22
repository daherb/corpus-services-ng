/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package de.uni_hamburg.corpora.validation;

import de.uni_hamburg.corpora.*;
import de.uni_hamburg.corpora.utilities.TypeConverter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.*;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpressionException;
import org.jdom2.Document;
import org.jdom2.Element;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jdom2.JDOMException;
import org.jdom2.filter.Filters;
import org.jdom2.xpath.XPathBuilder;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.XPathFactory;
import org.jdom2.xpath.jaxen.JaxenXPathFactory;
import org.xml.sax.SAXException;


/**
 *
 * @author bay7303
 */

public class ExbReplaceGlosses extends Checker implements CorpusFunction {

    String replacementTier = null;
    String replacementPrefix = null;
    String originalGloss = null;
    String replacementSuffix = null;
    String newGloss = null;
    String contextValue = null;
    String contextTier = null;
    String contextPrefix = null;
    String contextSuffix = null;
    private final XPathFactory xpathFactory = new JaxenXPathFactory();

    public void setReplacementTier(String tier) {
        replacementTier = tier;
    }
    
    public void setReplacementPrefix (String prefix) {
        replacementPrefix = prefix;
    }
    
    public void setOriginalValue(String gloss) {
        originalGloss = gloss;
    }
    
    public void setReplacementSuffix (String suffix) {
        replacementSuffix = suffix;
    }
    
    public void setNewValue(String gloss) {
        newGloss = gloss;
    }
            
    public void setContextValue(String gloss) {
        contextValue = gloss;
    }
    
    public void setContextTier(String tier) {
        contextTier = tier;
    }
    
    public void setContextPrefix (String prefix) {
        contextPrefix = prefix;
    }
    
    public void setContextSuffix (String suffix) {
        contextSuffix = suffix;
    }
        
    Document doc;
    //XMLOutputter xmOut = new XMLOutputter(); //for testing

    public ExbReplaceGlosses(Properties props) {
        //fixing option available
        super(true, props);
    }
        
    /**
     * Description will be here
     */
    
    @Override
    public Report function(CorpusData cd, Boolean fix) 
            throws SAXException, IOException, ParserConfigurationException, URISyntaxException, JDOMException, TransformerException, XPathExpressionException {
        Report stats = new Report();      
        doc = TypeConverter.String2JdomDocument(cd.toSaveableString());
        String patternRegex = replacementPrefix + originalGloss + replacementSuffix;
        Pattern glossFinder = Pattern.compile(patternRegex);
        //System.out.println(glossFinder.toString());
        CorpusIO cio = new CorpusIO();
        String XPathGloss = "//tier[@category='" + replacementTier + "']/event";
        XPathExpression<Element> XGloss = new XPathBuilder<>(XPathGloss, Filters.element()).compileWith(xpathFactory);
        List<Element> allGlosses = XGloss.evaluate(doc);
        for (Element matchedElement : allGlosses) {
            String glossText = matchedElement.getText();
            //System.out.println(glossText + "is the gloss text I've found");
            //System.out.println(glossFinder.toString() + "is the regular expression");
            Matcher m = glossFinder.matcher(glossText);
            if (m.find()) {
                if (contextValue != null) {
                    //System.out.println("We have a match! Let's look for context... which should be " + contextValue);
                    String timeStamp = matchedElement.getAttributeValue("start");
                    String XPathContext = "//tier[@category='" + contextTier + "']/event[@start='" + timeStamp + "']";
                    XPathExpression<Element> XContext = new XPathBuilder<>(XPathContext, Filters.element()).compileWith(xpathFactory);
                    Element ce = XContext.evaluateFirst(doc);
                    String contextText = ce.getText();
                    String contextRegex = contextPrefix + contextValue + contextSuffix;
                    Pattern contextFinder = Pattern.compile(contextRegex);
                    //System.out.println(contextText + " is the context value");
                    //System.out.println(contextFinder.toString());
                    Matcher ctxm = contextFinder.matcher(contextText);
                    if (ctxm.find()) {
                        System.out.println(m.group() + " matched an expression you wanted to replace in " + glossText + " The context is " + contextText);
                        glossText = m.replaceAll(newGloss);
                        matchedElement.setText(glossText);
                        //System.out.println(matchedElement.getText());
                        if (fix) {
                            cd.updateUnformattedString(TypeConverter.JdomDocument2String(doc));
                            cio.write(cd, cd.getURL());
                            stats.addFix(function, cd, "Replaced " + originalGloss + " with " + newGloss + " in " + glossText);
                        }
                    } else {System.out.println("The context didn't match :(");}
                } else {
                    System.out.println(m.group() + " matched an expression you wanted to replace in " + glossText);
                    glossText = m.replaceAll(newGloss);
                    matchedElement.setText(glossText);
                    //System.out.println(matchedElement.getText());
                    if (fix) {
                        cd.updateUnformattedString(TypeConverter.JdomDocument2String(doc));
                        cio.write(cd, cd.getURL());
                        stats.addFix(function, cd, "Replaced " + originalGloss + " with " + newGloss + " in " + glossText);
                    }
                }
            }
        }
        
        return stats;
    }
    
    /**
     * Default function which determines for what type of files (basic
     * transcription, segmented transcription, coma etc.) this feature can be
     * used.
     */
    @Override
    public Collection<Class<? extends CorpusData>> getIsUsableFor()  {
        return Collections.singleton(EXMARaLDATranscriptionData.class);
    }

    /**
     * Default function which returns a two/three line description of what this
     * class is about.
     */
    @Override
    public String getDescription() {
        String description = "This class replaces glosses in EXBs";
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
