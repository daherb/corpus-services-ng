/**
 * @file ComaErrorChecker.java
 *
 * Collection of checks for exb errors for HZSK repository purposes.
 *
 * @author Ozan Ozdemir <ozan.oezdemir@studium.uni-hamburg.de>
 * @author HZSK
 */

package de.uni_hamburg.corpora.validation;

import de.uni_hamburg.corpora.Corpus;
import de.uni_hamburg.corpora.Report;
import de.uni_hamburg.corpora.CorpusData;
import de.uni_hamburg.corpora.CorpusFunction;
import de.uni_hamburg.corpora.utilities.TypeConverter;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
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
 * This class validates a exb file with its DTD file.
 */

public class ExbSchemaChecker extends Checker implements CorpusFunction {


    public ExbSchemaChecker() {
        super(false);
    }

    /**
     * Validate an exb file with a DTD file.
     */
    
    /**
    * Main functionality of the feature; validates an exb file with a DTD file.
    */
    @Override
    public Report function(CorpusData cd, Boolean fix)
            throws SAXException, JDOMException, IOException, JexmaraldaException, TransformerException, ParserConfigurationException, XPathExpressionException{
        System.out.println("Checking the exb file against DTD...");
        String exbSchemaPath = new File("src\\test\\java\\de\\uni_hamburg\\corpora\\resources\\schemas\\exb_schema.xsd").getAbsolutePath();
        URL exbSchema = Paths.get(exbSchemaPath).toUri().toURL();//;new URL(exbSchemaPath);
        Source xmlStream = new StreamSource(TypeConverter.String2InputStream(cd.toSaveableString()));
        SchemaFactory schemaFactory =
            SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        Schema schema = schemaFactory.newSchema(exbSchema);
        Validator validator = schema.newValidator();
        ExbErrorReportGenerator eh = new ExbErrorReportGenerator(cd.getFilename());
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
        Class cl = Class.forName("de.uni_hamburg.corpora.EXMARaLDACorpusData");
        IsUsableFor.add(cl);
        return IsUsableFor;
    }

    /**Default function which returns a two/three line description of what 
     * this class is about.
     */
    @Override
    public String getDescription() {
        String description = "This class validates a exb file with its DTD file. ";
        return description;
    }

    @Override
    public Report function(Corpus c, Boolean fix) throws SAXException, IOException, ParserConfigurationException, URISyntaxException, JDOMException, TransformerException, XPathExpressionException, JexmaraldaException {
        Report stats = new Report();
        for (CorpusData cdata : c.getBasicTranscriptionData()) {
            stats.merge(function(cdata, fix));
        }
        return stats;
    }
}

