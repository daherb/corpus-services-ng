package de.uni_hamburg.corpora.validation;

import de.uni_hamburg.corpora.*;
import org.exmaralda.partitureditor.fsm.FSMException;
import org.exmaralda.partitureditor.jexmaralda.JexmaraldaException;
import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.JDOMException;
import org.jdom.xpath.XPath;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

public class ELANFileReferenceChecker extends Checker implements CorpusFunction {

    public ELANFileReferenceChecker() {
        super(false);
    }
    public ELANFileReferenceChecker(boolean hasfixingoption) {
        super(hasfixingoption);
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
        XPath languageQuery = XPath.newInstance("//MEDIA_DESCRIPTOR/@RELATIVE_MEDIA_URL");
        for (Object node : languageQuery.selectNodes(doc)) {
            Attribute relFileName = (Attribute) node;
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
        Collection usable = this.getIsUsableFor();
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
