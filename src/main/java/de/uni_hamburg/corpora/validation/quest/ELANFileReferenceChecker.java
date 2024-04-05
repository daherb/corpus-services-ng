package de.uni_hamburg.corpora.validation.quest;

import de.uni_hamburg.corpora.*;
import de.uni_hamburg.corpora.validation.Checker;
import org.exmaralda.partitureditor.fsm.FSMException;
import org.exmaralda.partitureditor.jexmaralda.JexmaraldaException;
import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.jdom2.filter.Filters;
import org.jdom2.xpath.XPathBuilder;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.jaxen.JaxenXPathFactory;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpressionException;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.Collections;
import java.util.Properties;

/**
 * Deprecated in favor of LinkedFileChecker
 *
 * Last updated
 * @author Herbert Lange
 * @version 20240405
 */
public class ELANFileReferenceChecker extends Checker implements CorpusFunction {

    public ELANFileReferenceChecker(Properties properties) {
        super(false, properties);
    }

    @Override
    public String getDescription() {
        return "Checks if all files referenced in the ELAN corpus exist";
    }

    @Override
    public Report function(CorpusData cd, Boolean fix) throws NoSuchAlgorithmException, ClassNotFoundException, FSMException, URISyntaxException, SAXException, IOException, ParserConfigurationException, JexmaraldaException, TransformerException, XPathExpressionException, JDOMException {
        Report stats = new Report();
        // Get the ELAN xml
        ELANData elanCorpus = (ELANData) cd ;
        Document doc = elanCorpus.getJdom() ;
        // Get all relative paths to media files
        XPathExpression<Attribute> languageQuery = new XPathBuilder<>("//MEDIA_DESCRIPTOR/@RELATIVE_MEDIA_URL", Filters.attribute()).compileWith(new JaxenXPathFactory());
        for (Attribute relFileName : languageQuery.evaluate(doc)) {
            // Resolve the relative file name given in the corpus by using the parentUrl of the corpus
            URI fileUri = Paths.get(elanCorpus.getParentURL().toURI().resolve(relFileName.getValue())).toUri() ;
            // Check if the file given by the uri exists
            if (!new File(fileUri).exists()) {
                stats.addWarning("ELANFileReferenceChecker", "Missing file " + fileUri);
            }
        }

        return stats ;
    }

    @Override
    public Report function(Corpus c, Boolean fix) throws NoSuchAlgorithmException, ClassNotFoundException, FSMException, URISyntaxException, SAXException, IOException, ParserConfigurationException, JexmaraldaException, TransformerException, XPathExpressionException, JDOMException {
        Report stats = new Report();
        // Apply function for each supported file
        Collection<Class<? extends CorpusData>> usable = this.getIsUsableFor();
        for (CorpusData cdata : c.getCorpusData()) {
            if (usable.contains(cdata.getClass())) {
                stats.merge(function(cdata, fix));
            }
        }
        return stats;
    }

    @Override
    public Collection<Class<? extends CorpusData>> getIsUsableFor()  {
        return Collections.singleton(ELANData.class);
    }
}
