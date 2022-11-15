package de.uni_hamburg.corpora.validation.quest;

import de.uni_hamburg.corpora.*;
import de.uni_hamburg.corpora.validation.Checker;
import org.exmaralda.partitureditor.fsm.FSMException;
import org.exmaralda.partitureditor.jexmaralda.JexmaraldaException;
import org.jdom.JDOMException;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.logging.Logger;

/**
 * Example checker for STTS tags in an EXMARaLDA corpus
 * @author bba1792, Dr. Herbert Lange
 * @version 20220328
 */
public class EXMARaLDASTTSChecker extends Checker implements CorpusFunction  {

    private final Logger logger = Logger.getLogger(getFunction());

    public EXMARaLDASTTSChecker(Properties properties) {
        super(false, properties);
    }

    @Override
    public String getDescription() {
        return "Checks STTS annotation in an EXMARaLDA corpus";
    }

    @Override
    public Report function(CorpusData cd, Boolean fix) throws NoSuchAlgorithmException, ClassNotFoundException, FSMException, URISyntaxException, SAXException, IOException, ParserConfigurationException, JexmaraldaException, TransformerException, XPathExpressionException, JDOMException {
        Report report = new Report();
        Properties tf_props = new Properties(props);
        tf_props.put("tier-pattern","[pos]");
        tf_props.put("attribute-name", "display-name");
        EXMARaLDATierFinder tf = new EXMARaLDATierFinder(tf_props);
        report.merge(tf.function(cd,fix));
        Properties ac_props = new Properties(props);
        ac_props.put("tier-ids", String.join(",", tf.getTierList()));
        // ac_props.put("tag-summary","true");
        ac_props.put("annotation-specification",
                "src/main/java/de/uni_hamburg/corpora/validation/quest/resources/AnnotationPanel_STTS.xml");
        EXMARaLDAAnnotationChecker ac = new EXMARaLDAAnnotationChecker(ac_props);
        report.merge(ac.function(cd,fix));
        return report;
    }

    @Override
    public Report function(Corpus c, Boolean fix) throws NoSuchAlgorithmException, ClassNotFoundException, FSMException, URISyntaxException, SAXException, IOException, ParserConfigurationException, JexmaraldaException, TransformerException, XPathExpressionException, JDOMException {
        Report report = new Report();
        Properties tf_props = new Properties(props);
        tf_props.put("tier-pattern","pos");
        tf_props.put("attribute-name", "display-name");
        EXMARaLDATierFinder tf = new EXMARaLDATierFinder(tf_props);
        report.merge(tf.function(c,fix));
        Properties ac_props = new Properties(props);
        ac_props.put("tier-ids", String.join(",", tf.getTierList()));
        // ac_props.put("tag-summary","true");
        ac_props.put("annotation-specification",
                "src/main/java/de/uni_hamburg/corpora/validation/quest/resources/AnnotationPanel_STTS.xml");
        EXMARaLDAAnnotationChecker ac = new EXMARaLDAAnnotationChecker(ac_props);
        report.merge(ac.function(c,fix));
        return report;
    }

    @Override
    public Collection<Class<? extends CorpusData>> getIsUsableFor() {
        List<Class<? extends CorpusData>> usableFor = new ArrayList<>();
        usableFor.add(EXMARaLDATranscriptionData.class);
        usableFor.add(EXMARaLDASegmentedTranscriptionData.class);
        return usableFor;
    }
}
