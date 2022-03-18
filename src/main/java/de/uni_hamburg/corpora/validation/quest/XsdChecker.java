package de.uni_hamburg.corpora.validation.quest;

import com.google.common.collect.Sets;
import de.uni_hamburg.corpora.*;
import de.uni_hamburg.corpora.validation.Checker;
import org.exmaralda.partitureditor.fsm.FSMException;
import org.exmaralda.partitureditor.jexmaralda.JexmaraldaException;
import org.jdom.JDOMException;
import org.reflections.Reflections;
import org.xml.sax.ErrorHandler;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpressionException;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class XsdChecker  extends Checker implements CorpusFunction {

    private final Logger logger = Logger.getLogger(getFunction());

    static final String JAXP_SCHEMA_LANGUAGE =
            "http://java.sun.com/xml/jaxp/properties/schemaLanguage";
    static final String W3C_XML_SCHEMA =
            "http://www.w3.org/2001/XMLSchema";

    public XsdChecker(Properties properties) {
        super(false, properties);
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
        dbf.setValidating(true);
        try {
            dbf.setAttribute(JAXP_SCHEMA_LANGUAGE, W3C_XML_SCHEMA);
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
                    new String[]{"function","filename","description","exception"},
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
