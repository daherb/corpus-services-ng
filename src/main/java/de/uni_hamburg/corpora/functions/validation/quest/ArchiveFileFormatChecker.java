package de.uni_hamburg.corpora.functions.validation.quest;

import de.uni_hamburg.corpora.Corpus;
import de.uni_hamburg.corpora.Report;
import de.uni_hamburg.corpora.data.CorpusData;
import de.uni_hamburg.corpora.functions.Checker;
import de.uni_hamburg.corpora.functions.CorpusFunction;
import org.exmaralda.partitureditor.fsm.FSMException;
import org.exmaralda.partitureditor.jexmaralda.JexmaraldaException;
import org.jdom.JDOMException;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.Collections;
import java.util.Properties;

/**
 * Checker if files are suitable for archiving. Work in progress
 *
 * @author Herbert Lange
 * @version 20230105
 */
public class ArchiveFileFormatChecker extends Checker implements CorpusFunction {

    // Formats following: https://www.loc.gov/preservation/resources/rfs/index.html
    enum RecommendedFormats {
        XML_MARKUP_PLUS_SCHEMA,
        PDF_UA, // ISO 14289-1 compliant
        PDF_A, // ISO 19005-compliant
        PDF, // highest quality available, with features such as searchable text, embedded fonts, lossless compression, high resolution images, device-independent specification of colorspace, content tagging
        PDF_X,
    } ;
    enum AcceptableFormats {
        XHTML_PLUS_DOCTYPE,
        HTML_PLUS_DOCTYPE,
        XML_DOCUMENT, // Includes DOCX/OOXML 2012 (ISO 29500), ODF (ISO/IEC 26300) and OOXML (ISO/IEC 29500).
        MS_DOC,
        RTF,
        TXT
    } ;
    public ArchiveFileFormatChecker(Properties properties) {
        super(false, properties);
    }

    @Override
    public String getDescription() {
        return "Checks all files in a directory if they follow recommendations for long-term archiving";
    }

    @Override
    public Report function(CorpusData cd, Boolean fix) throws NoSuchAlgorithmException, ClassNotFoundException, FSMException, URISyntaxException, SAXException, IOException, ParserConfigurationException, JexmaraldaException, TransformerException, XPathExpressionException, JDOMException {
        return new Report();
    }

    @Override
    public Report function(Corpus c, Boolean fix) throws NoSuchAlgorithmException, ClassNotFoundException, FSMException, URISyntaxException, SAXException, IOException, ParserConfigurationException, JexmaraldaException, TransformerException, XPathExpressionException, JDOMException {
        Report report = new Report();
        URL directory = c.getBaseDirectory();

        return report;
    }

    @Override
    public Collection<Class<? extends CorpusData>> getIsUsableFor() {
        // Does not really match any corpus data
        return Collections.EMPTY_LIST;
    }
}
