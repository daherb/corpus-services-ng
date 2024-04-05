/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uni_hamburg.corpora.visualization;

import de.uni_hamburg.corpora.*;

import java.io.IOException;
import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.regex.Pattern;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpressionException;
import org.exmaralda.partitureditor.fsm.FSMException;
import org.exmaralda.partitureditor.jexmaralda.BasicTranscription;
import org.exmaralda.partitureditor.jexmaralda.JexmaraldaException;
import org.jdom2.JDOMException;
import org.xml.sax.SAXException;

/**
 *
 * This now should be a normal, not abstract class that has an implementation of
 * "Visualize" as its field (that would be e.g. ListHTML) which seems to make
 * things a lot easier
 *
 * @author Daniel Jettka
 *
 * Last updated
 * @author Herbert Lange
 * @version 20240322
 */
public abstract class Visualizer implements CorpusFunction {

    private Properties props;
    private String html = null;
    protected BasicTranscription basicTranscription = null;
    protected String basicTranscriptionString = null;

    // resources loaded from directory supplied in pom.xml
    protected String STYLESHEET_PATH = null;
    protected String JS_HIGHLIGHTING_PATH = "/js/timelight-0.1.min.js";

    protected String EMAIL_ADDRESS = "corpora@uni-hamburg.de";
    protected String SERVICE_NAME = null;
    protected String HZSK_WEBSITE = "https://corpora.uni-hamburg.de/";
    protected String RECORDING_PATH = null;
    protected String RECORDING_TYPE = null;

    CorpusData cd;
    Report report;
    Collection<Class<? extends CorpusData>> IsUsableFor = new ArrayList<>();
    final String function;
    Boolean canfix = false;

    public Visualizer(Properties properties) {
        props = properties;
        function = this.getClass().getSimpleName();
    }

    /**
     * Manually set the HTML content of the visualization
     *
     * @param c content to be set as HTML of the visualization
     */
    public void setHTML(String c) {
        html = c;
    }

    /**
     * Get the HTML content of the visualization
     *
     * @return the HTML content of the visualization
     */
    public String getHTML() {
        return html;
    }

    /**
     * set a media element (video or audio depending on recordingType) in the
     * HTML content of the visualization
     *
     * @param recordingId path/URL to the recording file
     * @param recordingType type of the recording (e.g. wav, mp3, mpg, webm)
     */
    public void setMedia(String recordingId, String recordingType) {

        String newMediaElem = "";

        recordingType = recordingType.toLowerCase();

        if (recordingType.matches("^(wav|mp3|ogg)$")) {

            newMediaElem = "<audio controls=\"controls\" data-tlid=\"media\">\n"
                    + "   <source src=\"" + recordingId + "\" type=\"audio/" + recordingType + "\"/>\n"
                    + "</audio>";
        }

        if (recordingType.matches("^(mpeg|mpg|webm)$")) {
            newMediaElem = "<video controls=\"controls\" data-tlid=\"media\">\n"
                    + "   <source src=\"" + recordingId + "\" type=\"video/" + recordingType + "\"/>\n"
                    + "</video>";
        }

        setHTML(Pattern.compile("<div[^>]*id=\"mediaplayer\".*?</div>", Pattern.DOTALL).matcher(html).replaceAll("<div id=\"mediaplayer\" class=\"sidebarcontrol\">" + newMediaElem + "</div>"));

    }

    /**
     * remove content from media element in the HTML content of the
     * visualization
     */
    public void removeMedia() {

        setHTML(Pattern.compile("<div[^>]*id=\"mediaplayer\".*?</div>", Pattern.DOTALL).matcher(html).replaceAll("<div id=\"mediaplayer\" class=\"sidebarcontrol\"></div>"));

    }

    @Override
    public Report execute(CorpusData cd) {
        report = new Report();
        try {
            report = function(cd);
        } catch (JDOMException | SAXException jdome) {
            report.addException(function, jdome, cd, "Unknown parsing error");
        } catch (IOException | JexmaraldaException | NoSuchAlgorithmException | ClassNotFoundException |
                 XPathExpressionException | TransformerException | ParserConfigurationException | FSMException ioe) {
            report.addException(function, ioe, cd, "File reading error");
        } catch (URISyntaxException ex) {
            report.addException(function, ex, cd, "File reading erro");
        }

        return report;
    }

    //no fix boolean needed
    @Override
    public Report execute(CorpusData cd, boolean fix) {
        return execute(cd);
    }

    @Override
    public Report execute(Corpus c) {
        report = new Report();
        try {
            report = function(c);
        } catch (JexmaraldaException | SAXException | JDOMException je) {
            report.addException(function, je, cd, "Unknown parsing error");
        } catch (IOException | NoSuchAlgorithmException | ClassNotFoundException | XPathExpressionException |
                 TransformerException | ParserConfigurationException | FSMException ioe) {
            report.addException(function, ioe, cd, "File reading error");
        } catch (URISyntaxException ex) {
            report.addException(function, ex, cd, "File reading erro");
        }

        return report;
    }

    //no fix boolean needed
    @Override
    public Report execute(Corpus c, boolean fix) {
        return execute(c);
    }

    //TODO
    public abstract Report function(CorpusData cd) throws NoSuchAlgorithmException, ClassNotFoundException, FSMException, URISyntaxException, SAXException, IOException, ParserConfigurationException, JexmaraldaException, TransformerException, XPathExpressionException, JDOMException;

    public abstract Report function(Corpus c) throws NoSuchAlgorithmException, ClassNotFoundException, FSMException, URISyntaxException, SAXException, IOException, ParserConfigurationException, JexmaraldaException, TransformerException, XPathExpressionException, JDOMException;

    public abstract Collection<Class<? extends CorpusData>> getIsUsableFor();

    public void setIsUsableFor(Collection<Class<? extends CorpusData>> cdc) {
        IsUsableFor.addAll(cdc);
    }

    @Override
    public String getFunction() {
        return function;
    }

    @Override
    public Boolean getCanFix() {
        return canfix;
    }

    @Override
    public Map<String, String> getParameters() {
        return (Map<String,String>) Collections.EMPTY_MAP;
    }
}
