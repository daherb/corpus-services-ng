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
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Collections;
import java.util.Properties;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;

import org.jdom.JDOMException;
import org.xml.sax.SAXException;
import javax.xml.xpath.XPathExpressionException;

/**
 *
 * @author anne
 *
 * This class adds the path to an MP3 file next to the WAV file linked as a
 * recording in an exb file.
 *
 */
public class ExbMP3Next2WavAdder extends Checker implements CorpusFunction {

    public ExbMP3Next2WavAdder(Properties properties) {
        //fixing option available
        super(true, properties);
    }

    @Override
    public Report function(CorpusData cd, Boolean fix) throws TransformerConfigurationException, TransformerException, ParserConfigurationException, SAXException, IOException, XPathExpressionException {
        Report r = new Report();
        // get the XSLT stylesheet
        String xsl = TypeConverter.InputStream2String(getClass().getResourceAsStream("/xsl/AddMP3next2WavExb.xsl"));
        // create XSLTransformer and set the parameters 
        XSLTransformer xt = new XSLTransformer();

        // perform XSLT transformation
        String result = xt.transform(cd.toSaveableString(), xsl);
        CorpusIO cio = new CorpusIO();
        //update the xml of the cd object

        if (fix) {
            cd.updateUnformattedString(result);
            //save it - overwrite exb
            cio.write(cd, cd.getURL());
            //everything worked
            r.addFix(function, cd, "Added mp3 next to wav.");
        } else {
            r.addCritical(function, cd, "Checking function is not available");
        }
        return r;

    }

    @Override
    public Collection<Class<? extends CorpusData>> getIsUsableFor() {
        return Collections.singleton(EXMARaLDATranscriptionData.class) ;
    }

    /**
     * Default function which returns a two/three line description of what this
     * class is about.
     */
    @Override
    public String getDescription() {
        String description = "This class adds the path to an MP3 file next to the WAV file "
                + "linked as a recording in an exb file.";
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
