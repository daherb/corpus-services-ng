/**
 * @file ExbSegmenter.java
 *
 * A command-line tool / non-graphical interface for checking errors in
 * exmaralda's EXB files.
 *
 * @author Anne Ferger <anne.ferger@uni-hamburg.de>
 * @author INEL
 */
package de.uni_hamburg.corpora.validation;

import de.uni_hamburg.corpora.*;
import de.uni_hamburg.corpora.EXMARaLDATranscriptionData;

import static de.uni_hamburg.corpora.CorpusMagician.exmaError;

import de.uni_hamburg.corpora.utilities.TypeConverter;
import java.io.IOException;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpressionException;

import org.xml.sax.SAXException;

import org.exmaralda.partitureditor.jexmaralda.BasicTranscription;
import org.exmaralda.partitureditor.jexmaralda.JexmaraldaException;
import org.exmaralda.partitureditor.jexmaralda.segment.AbstractSegmentation;
import org.exmaralda.partitureditor.fsm.FSMException;
import org.exmaralda.partitureditor.jexmaralda.SegmentedTranscription;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;

import org.exmaralda.coma.helpers.*;

/**
 * This class checks Exmaralda exb files for segmentation problems and creates
 * segmented exs from the exbs.
 *
 */
public class ExbSegmentationChecker extends Checker implements CorpusFunction {

    static String filename;
    static BasicTranscription bt;
    static EXMARaLDATranscriptionData btd;
    static File exbfile;
    AbstractSegmentation segmentation;
    static ValidatorSettings settings;
    String segmentationName = "GENERIC";
    String path2ExternalFSM = "";

    public ExbSegmentationChecker(Properties properties) {
        //fixing is possible
        super(true, properties);
    }

    @Override
    public Report function(CorpusData cd, Boolean fix) throws SAXException, JDOMException, IOException, JexmaraldaException, FSMException, TransformerException, ParserConfigurationException, UnsupportedEncodingException, XPathExpressionException, URISyntaxException {
        Report stats = new Report();

        btd = new EXMARaLDATranscriptionData(cd.getURL());
        if (segmentationName.equals("HIAT")) {
            segmentation = new org.exmaralda.partitureditor.jexmaralda.segment.HIATSegmentation();
        } else if (segmentationName.equals("GAT")) {
            segmentation = new org.exmaralda.partitureditor.jexmaralda.segment.GATSegmentation();
        } else if (segmentationName.equals("cGAT_MINIMAL")) {
            segmentation = new org.exmaralda.partitureditor.jexmaralda.segment.cGATMinimalSegmentation();
        } else if (segmentationName.equals("CHAT")) {
            segmentation = new org.exmaralda.partitureditor.jexmaralda.segment.CHATSegmentation();
        } else if (segmentationName.equals("CHAT_MINIMAL")) {
            segmentation = new org.exmaralda.partitureditor.jexmaralda.segment.CHATMinimalSegmentation();
        } else if (segmentationName.equals("DIDA")) {
            segmentation = new org.exmaralda.partitureditor.jexmaralda.segment.DIDASegmentation();
        } else if (segmentationName.equals("IPA")) {
            segmentation = new org.exmaralda.partitureditor.jexmaralda.segment.IPASegmentation();
        } else {
            segmentation = new org.exmaralda.partitureditor.jexmaralda.segment.GenericSegmentation();
        }
        if (!path2ExternalFSM.equals("")) {
            segmentation.pathToExternalFSM = path2ExternalFSM;
        }
        CorpusIO cio = new CorpusIO();
        List v = segmentation.getSegmentationErrors(btd.getEXMARaLDAbt());
            if (v.isEmpty()) {
                if (fix){
                SegmentedTranscription st = segmentation.BasicToSegmented(btd.getEXMARaLDAbt());
                st.setEXBSource(cd.getFilename());
                //add the udMetadata!!!!
                //finally found the missing method :) :)
                org.exmaralda.partitureditor.jexmaralda.segment.SegmentCountForMetaInformation
                        .count(st);
                Document doc = TypeConverter.String2JdomDocument(st.toXML());
                URL url = new URL(cd.getParentURL() + cd.getFilenameWithoutFileEnding() + "_s.exs");
                cio.write(doc, url);
                stats.addFix(function, cd, "Exs successfully created at " + url);
                } else{
                    stats.addCorrect(function, cd, "No segmentation errors found with segmentation " + segmentationName);
                }
            } else {
                for (Object o : v) {
                    FSMException fsme = (FSMException) o;
                    String text = fsme.getMessage();
                    stats.addCritical(function, cd, text);
                    exmaError.addError(function, filename, fsme.getTierID(), fsme.getTLI(), false, text);
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
        return Collections.singleton(EXMARaLDATranscriptionData.class);
    }

    public void setSegmentation(String s) {
        segmentationName = s;
    }

    public void setExternalFSM(String s) {
        path2ExternalFSM = s;
    }

    public Document setMetadataInformation(Document partitur) {
        //SEE org.exmaralda.coma.models.TranscriptionMetadata
        Element root = partitur.getRootElement();
        String id = "";
        HashMap<String, HashMap<String, String>> speakers = new HashMap<String, HashMap<String, String>>();
        HashMap<String, String> metadata = new HashMap<String, String>();
        Element metaInformation;
        Element spkTable;
        HashMap<String, String> machineTags = new HashMap<String, String>();
        HashSet<String> mediaFiles = new HashSet<String>();
        if ((root.getName().equals("basic-transcription"))
                || (root.getName().equals("segmented-transcription"))) {
            if (root.getAttributeValue("Id") == null) {
                id = "CID" + new GUID().makeID();
                root.setAttribute("Id", id);
                // attribut machen
                // speichern
            } else {
                id = root.getAttributeValue("Id");
            }
            metaInformation = root.getChild("head").getChild(
                    "meta-information");
            metadata.put("project-name",
                    metaInformation.getChild("project-name").getText()
                    .trim());
            metadata.put("transcription-name",
                    metaInformation.getChild("transcription-name")
                    .getText());
            metadata.put("comment", metaInformation.getChild("comment")
                    .getText());
            metadata.put("transcription-convention", metaInformation
                    .getChild("transcription-convention").getText().trim());
            for (Element e : (List<Element>) metaInformation
                    .getChildren("referenced-file")) {
                if (e.getAttributeValue("url").length() > 0) {
                    mediaFiles.add(e.getAttributeValue("url"));
                }
            }
            for (Element e : (List<Element>) metaInformation.getChild(
                    "ud-meta-information").getChildren()) {
                if ((e.getAttributeValue("attribute-name").startsWith("#"))) {
                    machineTags.put(e.getAttributeValue("attribute-name")
                            .substring(2), e.getText().trim());
                } else {
                    metadata.put(
                            "ud_" + e.getAttributeValue("attribute-name"),
                            e.getText().trim());
                }
            }
            spkTable = (Element) root.getChild("head").getChild(
                    "speakertable");
            for (Element s : (List<Element>) spkTable.getChildren()) {
                String sid = "SID" + new GUID().makeID();
                speakers.put(sid, new HashMap<String, String>());
                speakers.get(sid).put("id", s.getAttributeValue("id"));
                speakers.get(sid).put("@abbreviation",
                        s.getChildText("abbreviation"));
                speakers.get(sid).put(
                        "@sex",
                        (s.getChild("sex").getAttributeValue("value")
                        .equals("m") ? "male" : "female"));
                speakers.get(sid).put("@abbreviation",
                        s.getChildText("abbreviation"));
                int count = 0;
                for (Element ul : (List<Element>) s.getChild(
                        "languages-used").getChildren()) {
                    count++;
                    metadata.put("@language-used-" + count,
                            ul.getAttributeValue("lang"));
                }
                count = 0;
                for (Element l1e : (List<Element>) s.getChild("l1")
                        .getChildren()) {
                    speakers.get(sid).put("@l1" + count,
                            l1e.getAttributeValue("lang"));
                    count++;
                }
                count = 0;
                for (Element l2e : (List<Element>) s.getChild("l2")
                        .getChildren()) {
                    speakers.get(sid).put("@l2" + count,
                            l2e.getAttributeValue("lang"));

                    count++;
                }

                for (Element udi : (List<Element>) s.getChild(
                        "ud-speaker-information").getChildren()) {
                    speakers.get(sid).put(
                            "ud_"
                            + udi.getAttributeValue(
                                    "attribute-name").trim(),
                            udi.getText());

                    // }
                }
                if (s.getChild("comment").getText().length() > 0) {
                    speakers.get(sid).put("comment",
                            s.getChild("comment").getText());
                }
            }
        }
        return partitur;
    }

    /**
     * Default function which returns a two/three line description of what this
     * class is about.
     */
    @Override
    public String getDescription() {
        String description = "This class checks Exmaralda exb files for segmentation problems, returns the errors in the Report and in the ExmaErrors and if the fix option is specified it creates "
                + "segmented exs from the exbs that don't contain errors.";
        return description;
    }
    
         @Override
    public Report function(Corpus c, Boolean fix) throws SAXException, IOException, ParserConfigurationException, URISyntaxException, JDOMException, TransformerException, XPathExpressionException, JexmaraldaException, FSMException {
        Report stats = new Report();
        for (CorpusData cdata : c.getBasicTranscriptionData()) {
            stats.merge(function(cdata, fix));
        }
        return stats;
    }
}
