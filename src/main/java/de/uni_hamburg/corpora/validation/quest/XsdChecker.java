package de.uni_hamburg.corpora.validation.quest;

import com.google.common.collect.Sets;
import de.uni_hamburg.corpora.*;
import de.uni_hamburg.corpora.validation.Checker;
import org.exmaralda.partitureditor.fsm.FSMException;
import org.exmaralda.partitureditor.jexmaralda.JexmaraldaException;
import org.jdom2.JDOMException;
import org.reflections.Reflections;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.SchemaFactory;
import javax.xml.xpath.XPathExpressionException;
import java.io.*;
import java.lang.reflect.Modifier;
import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class XsdChecker  extends Checker implements CorpusFunction {

    private final Logger logger = Logger.getLogger(getFunction());

    private Map<String,String> schemas = new HashMap<>();

    static final String JAXP_SCHEMA_LANGUAGE =
            "http://java.sun.com/xml/jaxp/properties/schemaLanguage";
    static final String W3C_XML_SCHEMA =
            "http://www.w3.org/2001/XMLSchema";

    public XsdChecker(Properties properties) {
        super(false, properties);
        // Map for external schema files needed if the schema is not linked in the file format
        schemas.put(ELANData.class.getSimpleName(),"xsd/eaf.xsd");
        schemas.put(EXMARaLDATranscriptionData.class.getSimpleName(),"xsd/exmaralda_exb.xsd");
        schemas.put(EXMARaLDASegmentedTranscriptionData.class.getSimpleName(),"xsd/exmaralda_exs.xsd");
    }

    @Override
    public String getDescription() {
        return "Checks if an XML file conforms to a XSD schema";
    }

    @Override
    public Report function(CorpusData cd, Boolean fix) throws NoSuchAlgorithmException, ClassNotFoundException, FSMException, URISyntaxException, SAXException, IOException, ParserConfigurationException, JexmaraldaException, TransformerException, XPathExpressionException, JDOMException {
        Report report = new Report();
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        try {
            // Add external schema if necessary
            if (schemas.containsKey(cd.getClass().getSimpleName())) {
                logger.info(schemas.get(cd.getClass().getSimpleName()));
                InputStream is =
                        this.getClass().getClassLoader().getResourceAsStream(schemas.get(cd.getClass().getSimpleName()));
                dbf.setSchema(SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI)
                        .newSchema(new StreamSource(is)));
            }
            else {
                // Otherwise set the schema language
                dbf.setAttribute(JAXP_SCHEMA_LANGUAGE, W3C_XML_SCHEMA);
                dbf.setValidating(true);
            }
            DocumentBuilder db = dbf.newDocumentBuilder();
            db.setErrorHandler(new ErrorHandler() {
                @Override
                public void warning(SAXParseException e) throws SAXException {
                    throw e;
                }

                @Override
                public void error(SAXParseException e) throws SAXException {
                    throw e;
                }

                @Override
                public void fatalError(SAXParseException e) throws SAXException {
                    throw e;
                }
            });
            db.parse(new File(cd.getURL().toURI()));
        }
        catch (SAXParseException e) {
            report.addWarning(getFunction(),ReportItem.newParamMap(
                    new ReportItem.Field[]{ReportItem.Field.Function, ReportItem.Field.Filename, ReportItem.Field.Description, ReportItem.Field.Exception},
                    new Object[]{getFunction(),cd.getFilename(),"Exception when parsing XML file",e}));
        }

        return report;
    }

    @Override
    public Report function(Corpus c, Boolean fix) throws NoSuchAlgorithmException, ClassNotFoundException, FSMException, URISyntaxException, SAXException, IOException, ParserConfigurationException, JexmaraldaException, TransformerException, XPathExpressionException, JDOMException {
        Report report = new Report();
        for (CorpusData cd : c.getCorpusData()) {
            if (getIsUsableFor().contains(cd.getClass())) {
                report.merge(function(cd,fix));
            }
        }
        return report;
    }

    @Override
    public Collection<Class<? extends CorpusData>> getIsUsableFor() {
        List<Class<? extends CorpusData>> usable = new ArrayList<>();
        Reflections reflections = new Reflections("de.uni_hamburg.corpora");
        // Get all classes derived from both XMLData and CorpusData
        for (Class c : Sets.intersection(reflections.getSubTypesOf(XMLData.class),
                reflections.getSubTypesOf(CorpusData.class)).stream().collect(Collectors.toSet())) {
            // Check if it is a proper class, ie public and not abstract
            if (Modifier.isPublic(c.getModifiers()) && !Modifier.isAbstract(c.getModifiers())) {
                usable.add(c);
            }
        }
        return usable;
    }
}
