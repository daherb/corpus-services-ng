/**
 * @file ExbErrorChecker.java
 *
 * A command-line tool / non-graphical interface for checking errors in
 * exmaralda's EXB files.
 *
 * @author Tommi A Pirinen <tommi.antero.pirinen@uni-hamburg.de>
 * @author HZSK
 */
package de.uni_hamburg.corpora.validation;

import de.uni_hamburg.corpora.*;
import de.uni_hamburg.corpora.EXMARaLDACorpusData;

import java.io.IOException;
import java.io.File;
import java.util.Collections;
import java.util.Hashtable;
import java.util.Collection;
import org.xml.sax.SAXException;
import static de.uni_hamburg.corpora.CorpusMagician.exmaError;
import java.net.URISyntaxException;
import java.util.Properties;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpressionException;
import org.exmaralda.partitureditor.jexmaralda.BasicTranscription;
import org.exmaralda.partitureditor.jexmaralda.JexmaraldaException;
import org.jdom.JDOMException;

/**
 * This class checks basic transcription files for structural anomalies.
 *
 */
public class ExbStructureChecker extends Checker implements CorpusFunction {

    BasicTranscription bt;
    File exbfile;
    ValidatorSettings settings;
    String filename;

    public ExbStructureChecker(Properties properties) {
        //fixing is not possible
        super(false, properties);
    }
    
    /**
     * Main functionality of the feature; checks basic transcription files for
     * structural anomalies.
     */
    @Override
    public Report function(CorpusData cd, Boolean fix)
            throws SAXException, JDOMException, IOException, JexmaraldaException {
        Report stats = new Report();
        EXMARaLDACorpusData btd = (EXMARaLDACorpusData) cd;
        filename = cd.getFilename();
        bt = btd.getEXMARaLDAbt();

        String[] duplicateTranscriptionTiers
                = bt.getDuplicateTranscriptionTiers();
        String[] orphanedTranscriptionTiers
                = bt.getOrphanedTranscriptionTiers();
        String[] orphanedAnnotationTiers = bt.getOrphanedAnnotationTiers();
        String[] temporalAnomalies
                = bt.getBody().getCommonTimeline().getInconsistencies();
        Hashtable<String, String[]> annotationMismatches
                = bt.getAnnotationMismatches();
        if (duplicateTranscriptionTiers.length == 0 && orphanedTranscriptionTiers.length == 0 && orphanedAnnotationTiers.length == 0 && temporalAnomalies.length == 0) {
            stats.addCorrect(function, cd, "No structure errors found.");
        } else {
            for (String tierID : duplicateTranscriptionTiers) {
                stats.addCritical(function, cd,
                        "More than one transcription tier for one "
                        + "speaker. Tier: " + tierID + "Open in PartiturEditor, "
                        + "change tier type or merge tiers.");
                exmaError.addError(function, filename, tierID, "", false,
                        "More than one transcription tier for one speaker. Tier: "
                        + tierID + ". Change tier type or merge tiers.");
            }
            for (String tliID : temporalAnomalies) {
                stats.addCritical(function, cd,
                        "Temporal anomaly at timeline item: " + tliID);
                exmaError.addError(function, filename, "", "", false,
                        "Temporal anomaly at timeline item: " + tliID);
            }
            for (String tierID : orphanedTranscriptionTiers) {
                stats.addCritical(function, cd,
                        "Orphaned transcription tier:" + tierID);
                exmaError.addError(function, filename, tierID, "", false,
                        "Orphaned transcription tier:" + tierID);
            }
            for (String tierID : orphanedAnnotationTiers) {
                stats.addCritical(function, cd,
                        "Orphaned annotation tier:" + tierID);
                exmaError.addError(function, filename, tierID, "", false,
                        "Orphaned annotation tier:" + tierID);
            }
            for (String tierID : annotationMismatches.keySet()) {
                String[] eventIDs = annotationMismatches.get(tierID);
                for (String eventID : eventIDs) {
                    stats.addCritical(function, cd,
                            "Annotation mismatch: tier " + tierID
                            + " event " + eventID);
                    exmaError.addError(function, filename, tierID, eventID, false,
                            "Annotation mismatch: tier " + tierID
                            + " event " + eventID);
                }
            }
        }

        return stats;
    }

    /**
     * Default function which determines for what type of files (basic
     * transcription, segmented transcription, coma etc.) this feature can be
     * used.
     */
    @Override
    public Collection<Class<? extends CorpusData>> getIsUsableFor() {
        return Collections.singleton(EXMARaLDACorpusData.class);
    }

    /**
     * Default function which returns a two/three line description of what this
     * class is about.
     */
    @Override
    public String getDescription() {
        String description = "This class checks basic transcription files for structural anomalies. ";
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
