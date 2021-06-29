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
import de.uni_hamburg.corpora.utilities.TypeConverter;
import de.uni_hamburg.corpora.utilities.XSLTransformer;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpressionException;
import org.exmaralda.partitureditor.jexmaralda.JexmaraldaException;
import org.jdom.JDOMException;
import org.xml.sax.SAXException;

/**
 *
 * @author fsnv625
 *
 * This class creates a sort- and filterable html overview in table form of the
 * content of the coma file to make error checking and harmonizing easier.
 */
public class ComaOverviewGeneration extends Checker implements CorpusFunction {

    boolean inel = false;
    String xslpath = "/xsl/Output_metadata_summary.xsl";
    
    public ComaOverviewGeneration() {
        //no fixing available
        super(false);
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
            URL overviewurl = new URL(cd.getParentURL(), "curation/coma_overview.html");
            CorpusIO cio = new CorpusIO();
            //save it
            cio.write(result, overviewurl);
            //everything worked
            r.addCorrect(function, cd, "created html overview at " + overviewurl);
           

        } catch (TransformerConfigurationException ex) {
            r.addException(ex, function, cd, "Transformer configuration error");
        } catch (TransformerException ex) {
            r.addException(ex, function, cd, "Transformer error");
        } catch (MalformedURLException ex) {
            r.addException(ex, function, cd, "Malformed URL error");
        } catch (IOException ex) {
            r.addException(ex, function, cd, "Unknown input/output error");
        } catch (ParserConfigurationException ex) {
            r.addException(ex, function, cd, "Unknown Parser error");
        } catch (SAXException ex) {
            r.addException(ex, function, cd, "Unknown XML error");
        } catch (XPathExpressionException ex) {
            r.addException(ex, function, cd, "Unknown XPath error");
        }

        return r;

    }


    @Override
    public Collection<Class<? extends CorpusData>> getIsUsableFor() throws ClassNotFoundException {
        Class cl = Class.forName("de.uni_hamburg.corpora.ComaData");
        IsUsableFor.add(cl);
        return IsUsableFor;
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
