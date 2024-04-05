package de.uni_hamburg.corpora.validation.quest;

import de.uni_hamburg.corpora.*;
import de.uni_hamburg.corpora.validation.Checker;
import org.exmaralda.partitureditor.fsm.FSMException;
import org.exmaralda.partitureditor.jexmaralda.BasicTranscription;
import org.exmaralda.partitureditor.jexmaralda.JexmaraldaException;
import org.exmaralda.partitureditor.jexmaralda.segment.HIATSegmentation;
import org.jdom2.JDOMException;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpressionException;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;


/**
 * Using the built-in EXMARaLDA file checker in corpus services
 *
 * Last updated
 * @author Herbert Lange
 * @version 20240405
 */
public class EXMARaLDAValidatorChecker extends Checker implements CorpusFunction {


    // The local logger that can be used for debugging
    Logger logger = Logger.getLogger(this.getClass().toString());

    // Flag to check hiat segmentation
    private boolean checkHIAT = false;

    public EXMARaLDAValidatorChecker(Properties properties) {
        super(false, properties) ;
        if (properties.containsKey("examaralda-check-hiat") && properties.getProperty("examaralda-check-hiat")
                .equalsIgnoreCase("true")) {
            checkHIAT = true;
        }
    }

    @Override
    public String getDescription() {
        return "Checker that runs the EXMARaLDA validator on an EXB file";
    }

    @Override
    public Report function(CorpusData cd, Boolean fix) throws NoSuchAlgorithmException, ClassNotFoundException, FSMException, URISyntaxException, SAXException, IOException, ParserConfigurationException, JexmaraldaException, TransformerException, XPathExpressionException, JDOMException {
        logger.info("Running function");
        boolean problem = false;
        Report report = new Report();
        URL fileUri = Paths.get(cd.getURL().toURI()).toAbsolutePath().toUri().toURL();
        BasicTranscription bt = new BasicTranscription();
        CorpusIO cio = new CorpusIO();
        bt.BasicTranscriptionFromString(cio.readFileURL(fileUri).toSaveableString());
        String[] duplicateTranscriptionTiers = bt.getDuplicateTranscriptionTiers();
        if (duplicateTranscriptionTiers.length > 0) {
            problem = true;
            report.addCritical(getFunction(),
                    ReportItem.newParamMap(new ReportItem.Field[]{ReportItem.Field.Function, ReportItem.Field.Filename, ReportItem.Field.Description},
                            new String[]{getFunction(), cd.getFilename(), "Duplicate transcription tiers: " + String.join(
                            ",", duplicateTranscriptionTiers)}));
        }
        String[] orphanedTranscriptionTiers = bt.getOrphanedTranscriptionTiers();
        if (orphanedTranscriptionTiers.length > 0) {
            problem = true;
            report.addWarning(getFunction(),
                    ReportItem.newParamMap(new ReportItem.Field[]{ReportItem.Field.Function, ReportItem.Field.Filename, ReportItem.Field.Description},
                            new String[]{getFunction(), cd.getFilename(), "Orphaned transcription tiers: " + String.join(
                            ",", orphanedTranscriptionTiers)}));
        }
        String[] orphanedAnnotationTiers = bt.getOrphanedAnnotationTiers();
        if (orphanedAnnotationTiers.length > 0) {
            problem = true;
            report.addWarning(getFunction(),
                    ReportItem.newParamMap(new ReportItem.Field[]{ReportItem.Field.Function, ReportItem.Field.Filename, ReportItem.Field.Description},
                            new String[]{getFunction(), cd.getFilename(), "Orphaned annotation tiers: " + String.join(
                            ",", orphanedAnnotationTiers)}));
        }
        String[] inconsistencies = bt.getBody().getCommonTimeline().getInconsistencies();
        if (inconsistencies.length > 0) {
            problem = true;
            report.addCritical(getFunction(),
                    ReportItem.newParamMap(new ReportItem.Field[]{ReportItem.Field.Function, ReportItem.Field.Filename, ReportItem.Field.Description},
                            new String[]{getFunction(), cd.getFilename(),
                                    "Inconsistencies in common timeline: " + String.join(
                            ",", inconsistencies)}));
        }
        Hashtable<String, String[]> annotationMismatches = bt.getAnnotationMismatches();
        // Only check tiers where we have a non-empty list in the map
        Set<String> missmatchTiers = annotationMismatches.keySet().stream().filter((k) ->
                annotationMismatches.get(k).length != 0).collect(Collectors.toSet());
        if (!missmatchTiers.isEmpty()) {
            problem = true;
            report.addCritical(getFunction(), ReportItem.newParamMap(new ReportItem.Field[]{ReportItem.Field.Function, ReportItem.Field.Filename, ReportItem.Field.Description},
                    new String[]{getFunction(), cd.getFilename(), "Annotation mismatch in tiers: " + String.join(",",
                            missmatchTiers)}));
        }
        if (checkHIAT) {
            Vector<FSMException> segmentationErrors = new HIATSegmentation().getSegmentationErrors(bt);
            // TODO the exact reason and form of segmentation errors is not clear
            if (!segmentationErrors.isEmpty()) {
                problem = true;
                for (Object o : segmentationErrors) {
                    report.addCritical(getFunction(), ReportItem.newParamMap(new ReportItem.Field[]{ReportItem.Field.Function, ReportItem.Field.Filename, ReportItem.Field.Description},
                            new String[]{getFunction(), cd.getFilename(),
                                    "HIAT Segmentation error: " + o.toString() + " - " + o.getClass()}));
                }
            }
        }
        if (!problem) {
            report.addCorrect(getFunction(),ReportItem.newParamMap(
                    new ReportItem.Field[]{ReportItem.Field.Function, ReportItem.Field.Filename, ReportItem.Field.Description},
                    new Object[]{getFunction(),cd.getFilename(),"No problems found in file"}
            ));
        }
        return report;
    }

    /**
     * Checker function for a corpus
     * @param c the corpus
     * @param fix the fixing parameter
     * @return detailed report of the checker
     * @throws NoSuchAlgorithmException inherited
     * @throws ClassNotFoundException inherited
     * @throws FSMException inherited
     * @throws URISyntaxException inherited
     * @throws SAXException inherited
     * @throws IOException inherited
     * @throws ParserConfigurationException inherited
     * @throws JexmaraldaException inherited
     * @throws TransformerException inherited
     * @throws XPathExpressionException inherited
     * @throws JDOMException inherited
     */
    @Override
    public Report function(Corpus c, Boolean fix) throws NoSuchAlgorithmException, ClassNotFoundException, FSMException, URISyntaxException, SAXException, IOException, ParserConfigurationException, JexmaraldaException, TransformerException, XPathExpressionException, JDOMException {
        Report report = new Report();
        // Apply function for each supported file
        Collection<Class<? extends CorpusData>> usable = this.getIsUsableFor();
        for (CorpusData cdata : c.getCorpusData()) {
            if (usable.contains(cdata.getClass())) {
                report.merge(function(cdata, fix));
            }
        }
        return report;
    }

    @Override
    public Collection<Class<? extends CorpusData>> getIsUsableFor() {
        // Only works for EXB data
        return Collections.singleton(EXMARaLDATranscriptionData.class);
    }

    @Override
    public Map<String, String> getParameters() {
        Map<String,String> params = super.getParameters();
        params.put("exmaralda-check-hiat", "Flag if HIAT segmentation should be checked");
        return params;
    }

    public static void main(String[] args) {
        if (args.length < 2) {
            System.err.println("Missing file or path to be checked and/or output file");
            System.err.println(args.length);
        }
        else {
            CorpusIO cio = new CorpusIO();
            try {
                Report report = new Report();
                Corpus corpus = new Corpus(cio.read(Paths.get(args[0]).toAbsolutePath().normalize().toUri().toURL(),
                        report));
                EXMARaLDAValidatorChecker exv = new EXMARaLDAValidatorChecker(new Properties());
                report.merge(exv.function(corpus,false));
                BufferedWriter bw = new BufferedWriter(new FileWriter(args[1]));
                bw.write(ReportItem.generateHTML(report.getRawStatistics()));
                bw.close();
            } catch (URISyntaxException | IOException | SAXException | JexmaraldaException | ClassNotFoundException | XPathExpressionException | NoSuchAlgorithmException | ParserConfigurationException | JDOMException | FSMException | TransformerException e) {
                e.printStackTrace();
            }
        }
    }
}
