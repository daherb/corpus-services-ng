package de.uni_hamburg.corpora.validation.quest;

import de.uni_hamburg.corpora.*;
import de.uni_hamburg.corpora.validation.Checker;
import org.exmaralda.partitureditor.fsm.FSMException;
import org.exmaralda.partitureditor.jexmaralda.BasicTranscription;
import org.exmaralda.partitureditor.jexmaralda.JexmaraldaException;
import org.jdom.Attribute;
import org.jdom.Document;
import org.jdom.JDOMException;
import org.jdom.xpath.XPath;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpressionException;
import java.io.*;
import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

/**
 *
 * @author bba1792
 *
 * This class checks all language codes to match the ISO 639-3 list
 *
 */

public class ExbLangCodes extends Checker implements CorpusFunction {
    private final ArrayList<String> langlist = new ArrayList<>();
    public ExbLangCodes() {
        super( false) ;
        try {
            InputStream in = getClass().getResourceAsStream("/iso-639-3.tab");
            BufferedReader bReader = new BufferedReader(new InputStreamReader(in));
            String line;
            while ((line = bReader.readLine()) != null) {
                langlist.add(line.split("\t")[0]);
            }
        }
        catch (IOException e) {
            System.err.println("Error loading language list:" + e.getMessage());
        }

    }
    @Override
    public String getDescription() {
        return "Checks the language codes used in the metadata to be valid ISO";
    }

    @Override
    public Report function(CorpusData cd, Boolean fix) throws NoSuchAlgorithmException, ClassNotFoundException, FSMException, URISyntaxException, SAXException, IOException, ParserConfigurationException, JexmaraldaException, TransformerException, XPathExpressionException, JDOMException {
        Report stats = new Report();
        // Get the xml data
        EXMARaLDACorpusData btd = (EXMARaLDACorpusData) cd;
        BasicTranscription bt = btd.getEXMARaLDAbt();
        Document doc = bt.toJDOMDocument();
        // find all language tags
        XPath languageQuery = XPath.newInstance("//language/@lang");
        for (Object node : languageQuery.selectNodes(doc)) {
            // get the language from the attribute and check if it is in the list
            Attribute langAttrib = (Attribute) node;
            if (!langlist.contains(langAttrib.getValue())) {
                stats.addWarning(getFunction(), "Unknown lang code " + langAttrib.getValue());
            }
        }

        return stats;
    }

    @Override
    public Report function(Corpus c, Boolean fix) throws NoSuchAlgorithmException, ClassNotFoundException, FSMException, URISyntaxException, SAXException, IOException, ParserConfigurationException, JexmaraldaException, TransformerException, XPathExpressionException, JDOMException {
        Report stats = new Report();
        for (CorpusData cdata : c.getBasicTranscriptionData()) {
            stats.merge(function(cdata, fix));
        }
        return stats;
    }

    @Override
    public Collection<Class<? extends CorpusData>> getIsUsableFor() {
        return Collections.singleton(EXMARaLDACorpusData.class);
    }
}
