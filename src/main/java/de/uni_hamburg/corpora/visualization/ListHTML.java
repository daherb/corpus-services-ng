/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uni_hamburg.corpora.visualization;

import de.uni_hamburg.corpora.EXMARaLDACorpusData;
import de.uni_hamburg.corpora.Corpus;
import de.uni_hamburg.corpora.CorpusData;
import de.uni_hamburg.corpora.CorpusIO;
import de.uni_hamburg.corpora.Report;
import de.uni_hamburg.corpora.utilities.TypeConverter;
import de.uni_hamburg.corpora.utilities.XSLTransformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import org.exmaralda.common.corpusbuild.FileIO;
import org.exmaralda.common.jdomutilities.IOUtilities;
import org.exmaralda.partitureditor.jexmaralda.ListTranscription;
import org.exmaralda.partitureditor.jexmaralda.SegmentedTranscription;
import org.exmaralda.partitureditor.jexmaralda.segment.CHATSegmentation;
import org.exmaralda.partitureditor.jexmaralda.segment.GATSegmentation;
import org.exmaralda.partitureditor.jexmaralda.segment.GenericSegmentation;
import org.exmaralda.partitureditor.jexmaralda.segment.IPASegmentation;
import org.exmaralda.partitureditor.jexmaralda.segment.SegmentedToListInfo;
import org.jdom.Document;
import org.xml.sax.SAXException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.exmaralda.partitureditor.jexmaralda.segment.HIATSegmentation;

/**
 *
 * @author Daniel Jettka
 *
 * This class creates an html visualization in the List format from an exb.
 *
 */
public class ListHTML extends Visualizer {

    private String utteranceList = null;

    // resources loaded from directory supplied in pom.xml
    private static final String STYLESHEET_PATH = "/xsl/HIAT2ListHTML.xsl";
    private static final String GAT_STYLESHEET_PATH = "/xsl/GAT2ListHTML.xsl";
    private static final String GENERIC_STYLESHEET_PATH = "/xsl/Generic2ListHTML.xsl";
    private static final String SERVICE_NAME = "ListHTML";
    //static String INEL_FSM = "/de/uni_hamburg/corpora/utilities/segmentation/INEL_Segmentation_FSM.xml";
    //static String INEL_FSM = "/org/exmaralda/partitureditor/fsm/xml/HIAT_UtteranceWord.xml";

    URL targeturl;
    CorpusData cd;
    String corpusname = "";
    String path2ExternalFSM = "";
    String segmentationAlgorithm = "GENERIC";

    public ListHTML() {
    }

    public ListHTML(String btAsString, String segmAlgorithm) {
        Report stats = new Report();
        try {
            createFromBasicTranscription(btAsString, segmAlgorithm);
        } catch (Exception ex) {
            stats.addException(SERVICE_NAME, ex, "Exception");
        }
    }

    /**
     * This method deals with transforming EXB to list HTML
     *
     * @param btAsString the EXB file represented in a String object
     * @return
     */
    private String createFromBasicTranscription(String btAsString, String segmAlgorithm) throws Exception {

        basicTranscriptionString = btAsString;
        basicTranscription = TypeConverter.String2BasicTranscription(basicTranscriptionString);
        segmentationAlgorithm = segmAlgorithm;

        String result = null;
        // create an utterance list as XML basis for transformation
        createUtteranceList();
        if (getUtteranceList() != null) {
            // get the XSLT stylesheet
            String xsl = "";
            if (segmAlgorithm.equals("HIAT")) {
                xsl = TypeConverter.InputStream2String(
                        getClass().getResourceAsStream(STYLESHEET_PATH));
            } else if (segmAlgorithm.equals("GAT")) {
                xsl = TypeConverter.InputStream2String(
                        getClass().getResourceAsStream(GAT_STYLESHEET_PATH));
            } else if (segmAlgorithm.equals("GENERIC")) {
                xsl = TypeConverter.InputStream2String(
                        getClass().getResourceAsStream(GENERIC_STYLESHEET_PATH));
            } else {
                xsl = TypeConverter.InputStream2String(
                        getClass().getResourceAsStream(GENERIC_STYLESHEET_PATH));
            }

            // create XSLTransformer and set the parameters
            XSLTransformer xt = new XSLTransformer();
            xt.setParameter("EMAIL_ADDRESS", EMAIL_ADDRESS);
            xt.setParameter("WEBSERVICE_NAME", SERVICE_NAME + " (" + segmAlgorithm + ")");
            xt.setParameter("HZSK_WEBSITE", HZSK_WEBSITE);
            xt.setParameter("TRANSCRIPTION_NAME", cd.getFilenameWithoutFileEnding());
            //xt.setParameter("CORPUS_NAME", cd.getFilenameWithoutFileEnding());
            if (!corpusname.equals("")) {
                xt.setParameter("CORPUS_NAME", corpusname);
            }
            // perform XSLT transformation
            result = xt.transform(getUtteranceList(), xsl);
            if (result != null) {

                // replace JS/CSS placeholders from XSLT output
                Pattern regex = Pattern.compile("(<hzsk\\-pi:include( xmlns:hzsk\\-pi=\"https://corpora\\.uni\\-hamburg\\.de/hzsk/xmlns/processing\\-instruction\")?>([^<]+)</hzsk\\-pi:include>)", Pattern.DOTALL);
                Matcher m = regex.matcher(result);
                StringBuffer sb = new StringBuffer();
                while (m.find()) {
                    String insertion = TypeConverter.InputStream2String(getClass().getResourceAsStream(m.group(3)));
                    m.appendReplacement(sb, m.group(0).replaceFirst(Pattern.quote(m.group(1)), insertion));
                }
                m.appendTail(sb);
                result = sb.toString();
                setHTML(result);
            }
        }
        //System.out.println(result);
        return result;
    }

    /**
     * This method creates the intermediate utterance list needed for List
     * visualization
     *
     * @param
     * @return
     */
    private void createUtteranceList() throws Exception {

        String list = null;

        switch (segmentationAlgorithm) {
            case "HIAT": {
                HIATSegmentation hS = new HIATSegmentation();
                if (!path2ExternalFSM.equals("")) {
                    hS.pathToExternalFSM = path2ExternalFSM;
                }
                ListTranscription lt = hS.BasicToUtteranceList(basicTranscription);
                final Document listXML = FileIO.readDocumentFromString(lt.toXML());
                list = IOUtilities.documentToString(listXML);
                break;
            }
            case "CHAT": {
                CHATSegmentation cS = new CHATSegmentation();
                if (!path2ExternalFSM.equals("")) {
                    cS.pathToExternalFSM = path2ExternalFSM;
                }
                ListTranscription lt = cS.BasicToUtteranceList(basicTranscription);
                final Document listXML = FileIO.readDocumentFromString(lt.toXML());
                list = IOUtilities.documentToString(listXML);
                break;
            }
            case "GAT": {
                GATSegmentation gS = new GATSegmentation();
                if (!path2ExternalFSM.equals("")) {
                    gS.pathToExternalFSM = path2ExternalFSM;
                }
                ListTranscription lt = gS.BasicToIntonationUnitList(basicTranscription);
                final Document listXML = FileIO.readDocumentFromString(lt.toXML());
                list = IOUtilities.documentToString(listXML);
                break;
            }
            case "IPA": {
                IPASegmentation ipaS = new IPASegmentation();
                if (!path2ExternalFSM.equals("")) {
                    ipaS.pathToExternalFSM = path2ExternalFSM;
                }
                SegmentedTranscription st = ipaS.BasicToSegmented(basicTranscription);
                ListTranscription lt = st.toListTranscription(new SegmentedToListInfo(st, SegmentedToListInfo.TURN_SEGMENTATION));
                final Document listXML = FileIO.readDocumentFromString(lt.toXML());
                list = IOUtilities.documentToString(listXML);
                break;
            }
            case "Generic": {
                GenericSegmentation genS = new GenericSegmentation();
                if (!path2ExternalFSM.equals("")) {
                    genS.pathToExternalFSM = path2ExternalFSM;
                }
                SegmentedTranscription st = genS.BasicToSegmented(basicTranscription);
                ListTranscription lt = st.toListTranscription(new SegmentedToListInfo(st, SegmentedToListInfo.TURN_SEGMENTATION));
                final Document listXML = FileIO.readDocumentFromString(lt.toXML());
                list = IOUtilities.documentToString(listXML);
                break;
            }
            default:
                throw new Exception("createUtteranceList - unsupported parameter segmAlgorithm='" + segmentationAlgorithm + "'");
        }

        setUtteranceList(list);
    }

    /**
     * Manually set the utterance list (intermediate transformation format) for
     * this visualization
     *
     * @param u the String representing the utterance list
     * @return
     */
    public void setUtteranceList(String u) {
        utteranceList = u;
    }

    /**
     * Get the utterance list (intermediate transformation format) for this
     * visualization
     *
     * @param
     * @return String representing the utterance list
     */
    public String getUtteranceList() {
        return utteranceList;
    }

    public static void main(String[] args) {
        ListHTML lhtml = new ListHTML();
        Report stats = lhtml.doMain(args);
        System.out.println(stats.getSummaryLines());
        System.out.println(stats.getErrorReports());
    }

    @Override
    public Report function(CorpusData ccd) {
        Report stats = new Report();
        try {
            cd = ccd;
            String result = createFromBasicTranscription(cd.toUnformattedString(), segmentationAlgorithm);
            targeturl = new URL(cd.getParentURL() + cd.getFilenameWithoutFileEnding() + "_list.html");
            CorpusIO cio = new CorpusIO();
            if (result == null) {
                stats.addCritical(SERVICE_NAME, cd, "Visualization of file was not possible!");
            } else {
                cio.write(result, targeturl);
                stats.addCorrect(SERVICE_NAME, cd, "Visualization of file was successfully saved at " + targeturl);
            }
        } catch (IOException ex) {
            stats.addException(SERVICE_NAME, ex, "IO Exception");
        } catch (Exception ex) {
            stats.addException(SERVICE_NAME, ex, "Exception");
        }
        return stats;
    }

    @Override
    public Report function(Corpus co) throws TransformerException, TransformerConfigurationException, IOException, SAXException {
        Report stats = new Report();
        Collection<EXMARaLDACorpusData> btc = co.getBasicTranscriptionData();
        for (EXMARaLDACorpusData bt : btc) {
            stats.merge(function(bt));
        }
        return stats;
    }

    @Override
    public Collection<Class<? extends CorpusData>> getIsUsableFor() {
        try {
            Class cl = Class.forName("de.uni_hamburg.corpora.EXMARaLDACorpusData");
            IsUsableFor.add(cl);
        } catch (ClassNotFoundException ex) {
            report.addException(ex, "Usable class not found.");
        }
        return IsUsableFor;
    }

    public Report
            doMain(String[] args) {
        try {
            if (args.length < 2) {
                System.out.println("Usage: " + ListHTML.class
                        .getName()
                        + "EXB SEGMENTATION [HTML]");
                System.out.println("\nSEGMENTATION is one of: "
                        + " HIAT, CHAT, IPA, Generic");
                System.exit(1);
            } else {
                byte[] encoded = Files.readAllBytes(Paths.get(args[0]));
                String btString = new String(encoded, "UTF-8");
                ListHTML list = new ListHTML(btString, args[1]);
                if (args.length >= 3) {
                    PrintWriter htmlOut = new PrintWriter(args[2]);
                    htmlOut.print(list.getHTML());
                    htmlOut.close();
                } else {
                    System.out.println(list.getHTML());
                }
            }
        } catch (UnsupportedEncodingException uee) {
            uee.printStackTrace();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        return report;
    }

    public URL getTargetURL() {
        return targeturl;
    }

    public String getSegmentation() {
        return segmentationAlgorithm;
    }

    public void setSegmentation(String s) {
        segmentationAlgorithm = s;
    }

    public void setCorpusName(String s) {
        corpusname = s;
    }

    public void setExternalFSM(String s) {
        path2ExternalFSM = s;
    }

    @Override
    public String getDescription() {
        String description = "This class creates an html visualization "
                + "in the List format from an exb. ";
        return description;
    }

}
