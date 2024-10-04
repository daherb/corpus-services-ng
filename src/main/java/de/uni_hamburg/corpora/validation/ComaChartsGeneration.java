/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uni_hamburg.corpora.validation;

import de.uni_hamburg.corpora.*;
import de.uni_hamburg.corpora.utilities.TypeConverter;
import de.uni_hamburg.corpora.utilities.XSLTransformer;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpressionException;

import org.jdom2.JDOMException;
import org.xml.sax.SAXException;

/**
 *
 * @author fsnv625
 *
 * This class creates a sort- and filterable html overview in table form of the
 * content of the coma file to make error checking and harmonizing easier.
 *
 * Last update
 * @author Herbert Lange
 * @version 20241004
 */
public class ComaChartsGeneration extends Checker implements CorpusFunction {

    boolean inel = false;
    String xslpath = "/xsl/Coma2Charts.xsl";
    
    public ComaChartsGeneration(Properties properties) {
        //no fixing available
        super(false,properties);
    }

    @Override
    public Report function(CorpusData cd, Boolean fix) {
        Report r = new Report();
        String xsl;
        try {

            // get the XSLT stylesheet as String
            xsl = TypeConverter.InputStream2String(getClass().getResourceAsStream(xslpath));
            // create XSLTransformer and set the parameters 
            XSLTransformer xt = new XSLTransformer();
            //set an parameter for INEL
            if(inel){  
                xt.setParameter("mode", "inel");
            }
            // perform XSLT transformation
            String result = xt.transform(cd.toSaveableString(), xsl);
            
            
            //get location to save new result
            URL overviewurl = new URL(cd.getParentURL(), "resources/charts.html");
            CorpusIO cio = new CorpusIO();
            //save it
            cio.write(result, overviewurl);
            //everything worked
            r.addCorrect(function, cd, "created html charts at " + overviewurl);
           

        } catch (TransformerConfigurationException ex) {
            r.addException(function, ex, cd, "Transformer configuration error");
        } catch (TransformerException ex) {
            r.addException(function, ex, cd, "Transformer error");
        } catch (MalformedURLException ex) {
            r.addException(function, ex, cd, "Malformed URL error");
        } catch (IOException ex) {
            r.addException(function, ex, cd, "Unknown input/output error");
        } catch (ParserConfigurationException ex) {
            r.addException(function, ex, cd, "Unknown Parser error");
        } catch (SAXException ex) {
            r.addException(function, ex, cd, "Unknown XML error");
        } catch (JDOMException ex) {
            r.addException(function, ex, cd, "Unknown XPath error");
        }

        return r;

    }


    @Override
    public Collection<Class<? extends CorpusData>> getIsUsableFor() {
        return Collections.singleton(ComaData.class);
    }

    /**
     * Default function which returns a two/three line description of what this
     * class is about.
     */
    @Override
    public String getDescription() {
        String description = "This class creates a sort- and filterable html overview in table form "
                + " of the content of the coma file to make error checking and harmonizing easier. ";
        return description;
    }

    public void setInel() {
        inel = true;
    }

    @Override
    public Report function(Corpus c, Boolean fix) {
        Report stats;
        cd = c.getComaData();
        stats = function(cd, fix);
        return stats;
    }

}
