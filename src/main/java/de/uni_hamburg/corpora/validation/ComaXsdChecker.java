/**
 * @file ComaErrorChecker.java
 *
 * Collection of checks for coma errors for HZSK repository purposes.
 *
 * @author Tommi A Pirinen <tommi.antero.pirinen@uni-hamburg.de>
 * @author HZSK
 */

package de.uni_hamburg.corpora.validation;

import de.uni_hamburg.corpora.Corpus;
import de.uni_hamburg.corpora.Report;
import de.uni_hamburg.corpora.CorpusData;
import de.uni_hamburg.corpora.CorpusFunction;
import de.uni_hamburg.corpora.utilities.TypeConverter;
import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpressionException;
import org.exmaralda.partitureditor.jexmaralda.JexmaraldaException;
import org.jdom.JDOMException;
import org.xml.sax.SAXException;



/**
 * This class validates the coma file with the respective XML schema.
 * 
 */
public class ComaXsdChecker extends Checker implements CorpusFunction {

    public ComaXsdChecker() {
        super(false);
    }
    
    /**
    * Main functionality of the feature; validates a coma file with XML schema from internet.
    */
    public Report function(CorpusData cd, Boolean fix)
            throws SAXException, JDOMException, IOException, JexmaraldaException, TransformerException, ParserConfigurationException, XPathExpressionException{
        System.out.println("Checking COMA file against schema...");
        URL COMA_XSD = new URL("http://www.exmaralda.org/xml/comacorpus.xsd");
        Source xmlStream = new StreamSource(TypeConverter.String2InputStream(cd.toSaveableString()));
        SchemaFactory schemaFactory =
            SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        Schema schema = schemaFactory.newSchema(COMA_XSD);
        Validator validator = schema.newValidator();
        ComaErrorReportGenerator eh = new ComaErrorReportGenerator();
        validator.setErrorHandler(eh);
        validator.validate(xmlStream);
        return eh.getErrors();
    }
    
    /**
    * Default function which determines for what type of files (basic transcription, 
    * segmented transcription, coma etc.) this feature can be used.
    */
    @Override
    public Collection<Class<? extends CorpusData>> getIsUsableFor() throws ClassNotFoundException {
        Class cl = Class.forName("de.uni_hamburg.corpora.ComaData");
        IsUsableFor.add(cl);
        return IsUsableFor;
    }

    /**Default function which returns a two/three line description of what 
     * this class is about.
     */
    @Override
    public String getDescription() {
        String description = "This class validates the coma file with the respective XML schema. ";
        return description;
    }

    @Override
    public Report function(Corpus c, Boolean fix) throws SAXException, JDOMException, IOException, JexmaraldaException, TransformerException, ParserConfigurationException, XPathExpressionException {
        Report stats;
        cd = c.getComaData();
        stats = function(cd, fix);
        return stats;
    }

}
