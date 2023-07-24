/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uni_hamburg.corpora.visualization;

import de.uni_hamburg.corpora.*;
import de.uni_hamburg.corpora.EXMARaLDATranscriptionData;
import de.uni_hamburg.corpora.utilities.TypeConverter;
import de.uni_hamburg.corpora.utilities.XSLTransformer;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpressionException;
import org.exmaralda.common.corpusbuild.FileIO;
import org.exmaralda.partitureditor.interlinearText.HTMLParameters;
import org.exmaralda.partitureditor.interlinearText.InterlinearText;
import org.exmaralda.partitureditor.jexmaralda.BasicTranscription;
import org.exmaralda.partitureditor.jexmaralda.TierFormatTable;
import org.exmaralda.partitureditor.jexmaralda.convert.ItConverter;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.filter.ElementFilter;
import org.jdom.output.XMLOutputter;
import org.jdom.xpath.XPath;
import org.xml.sax.SAXException;

/**
 *
 * @author Daniel Jettka
 *
 * This class creates an html visualization in the Score format from an exb.
 *
 */
public class ScoreHTML extends Visualizer {

    // resources loaded from directory supplied in pom.xml
    private static final String STYLESHEET_PATH = "/xsl/Score2HTML.xsl";
    private final String SERVICE_NAME = "ScoreHTML";
    private Integer width = 900;
    URL targeturl;
    CorpusData cd;
    String corpusname = "";

    public ScoreHTML(Properties properties) {
        super(properties);
    }

    public ScoreHTML(String btAsString) throws JDOMException, TransformerException, TransformerConfigurationException, IOException {
        this(new Properties());
        createFromBasicTranscription(btAsString);
    }

    /**
     * This method deals performs the transformation of EXB to horizontal Score
     * HTML
     *
     * @param btAsString the EXB file represented in a String object
     * @return
     */
    public String createFromBasicTranscription(String btAsString) throws JDOMException, TransformerConfigurationException, TransformerException, IOException {
        basicTranscriptionString = btAsString;
        basicTranscription = TypeConverter.String2BasicTranscription(btAsString);

        String result = null;

        BasicTranscription bt = basicTranscription;
        bt.normalize();

        TierFormatTable tft = new TierFormatTable(bt);

        ItConverter itc = new ItConverter();
        InterlinearText it = itc.BasicTranscriptionToInterlinearText(bt, tft, 0);

        //setting HTML parameters
        HTMLParameters param = new HTMLParameters();
        param.setWidth(width);
        param.stretchFactor = 1.2;
        param.smoothRightBoundary = true;
        param.includeSyncPoints = true;
        param.putSyncPointsOutside = true;
        param.outputAnchors = true;
        param.frame = "lrtb";
        param.frameStyle = "Solid";
        param.setFrameColor(new java.awt.Color(153, 153, 153));

        it.trim(param);

        String itAsString = it.toXML();
        String styles = "/* EMTPY TIER FORMAT TABLE!!! */";
        if (bt.getTierFormatTable() != null) {
            styles = bt.getTierFormatTable().toTDCSS();
        }

        final Document itDocument = FileIO.readDocumentFromString(itAsString);
        Document btDocument = bt.toJDOMDocument();

        // remove "line" elements (die stoeren nur)
        Iterator i = itDocument.getRootElement().getDescendants(new ElementFilter("line"));
        Vector toBeRemoved = new Vector();
        while (i.hasNext()) {
            toBeRemoved.addElement(i.next());
        }
        for (int pos = 0; pos < toBeRemoved.size(); pos++) {
            Element e = (Element) (toBeRemoved.elementAt(pos));
            e.detach();
        }

        XPath xpath1 = XPath.newInstance("//common-timeline");
        Element timeline = (Element) (xpath1.selectSingleNode(btDocument));
        timeline.detach();

        XPath xpath2 = XPath.newInstance("//head");
        Element head = (Element) (xpath2.selectSingleNode(btDocument));
        head.detach();

        XPath xpath3 = XPath.newInstance("//tier");
        List tiers = xpath3.selectNodes(btDocument);
        Element tiersElement = new Element("tiers");
        for (int pos = 0; pos < tiers.size(); pos++) {
            Element t = (Element) (tiers.get(pos));
            t.detach();
            t.removeContent();
            tiersElement.addContent(t);
        }

        Element tableWidthElement = new Element("table-width");
        tableWidthElement.setAttribute("table-width", Long.toString(Math.round(param.getWidth())));

        Element btElement = new Element("basic-transcription");
        //            btElement.addContent(nameElement);
        btElement.addContent(tableWidthElement);
        btElement.addContent(head);
        btElement.addContent(timeline);
        btElement.addContent(tiersElement);

        itDocument.getRootElement().addContent(btElement);

        XMLOutputter xmOut = new XMLOutputter();
        String xml = xmOut.outputString(itDocument);

        String xsl = TypeConverter.InputStream2String(getClass().getResourceAsStream(STYLESHEET_PATH));

        XSLTransformer xt = new XSLTransformer();
        xt.setParameter("EMAIL_ADDRESS", EMAIL_ADDRESS);
        xt.setParameter("WEBSERVICE_NAME", SERVICE_NAME);
        xt.setParameter("HZSK_WEBSITE", HZSK_WEBSITE);
        xt.setParameter("STYLES", styles);
        xt.setParameter("TRANSCRIPTION_NAME", cd.getFilenameWithoutFileEnding());
        if (!corpusname.equals("")) {
            xt.setParameter("CORPUS_NAME", corpusname);
        }

        // perform XSLT transformation
        result = xt.transform(xml, xsl);

        // insert JavaScript for highlighting
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
        String js = TypeConverter.InputStream2String(getClass().getResourceAsStream(JS_HIGHLIGHTING_PATH));
        result = result.replace("<!--jsholder-->", js);

        setHTML(result);

        return result;
    }

    /**
     * Get the width that was set for the Score HTML visualization
     *
     * @return
     */
    public Integer getWidth() {
        return width;
    }

    /**
     * Set the width for the Score HTML visualization
     *
     * @param width width in px for the HTML visualization
     * @return
     */
    public void setWidth(Integer width) {
        this.width = width;
    }

    public static void main(String[] args) {
        ScoreHTML shtml = new ScoreHTML(new Properties());
        Report stats = shtml.doMain(args);
        System.out.println(stats.getSummaryLines());
        System.out.println(stats.getErrorReports());
    }

    @Override
    public Report function(CorpusData cod) throws JDOMException, TransformerException, MalformedURLException, TransformerConfigurationException, IOException, ParserConfigurationException, SAXException, XPathExpressionException {
        Report stats = new Report();
        cd = cod;
        String result = createFromBasicTranscription(cd.toSaveableString());
        targeturl = new URL(cd.getParentURL() + cd.getFilenameWithoutFileEnding() + "_score.html");
        CorpusIO cio = new CorpusIO();
        cio.write(result, targeturl);
        stats.addCorrect(SERVICE_NAME, cd, "Visualization of file was successfully saved at " + targeturl);
        return stats;
    }

    @Override
    public Report function(Corpus co) throws JDOMException, TransformerException, TransformerConfigurationException, IOException, MalformedURLException, ParserConfigurationException, SAXException, XPathExpressionException {
        Report stats = new Report();
        Collection<EXMARaLDATranscriptionData> btc = co.getBasicTranscriptionData();
        for (EXMARaLDATranscriptionData bt : btc) {
            stats.merge(function(bt));
        }
        return stats;
    }

    @Override
    public Collection<Class<? extends CorpusData>> getIsUsableFor() {
        try {
            Class cl = Class.forName("de.uni_hamburg.corpora.EXMARaLDATranscriptionData");
            IsUsableFor.add(cl);
        } catch (ClassNotFoundException ex) {
            report.addException(ex, "Usable class not found.");
        }
        return IsUsableFor;
    }

    public Report doMain(String[] args) {
        Report stats = new Report();
        try {
            if (args.length == 0) {
                System.out.println("Usage: " + ScoreHTML.class.getName()
                        + "EXB [HTML]");
                System.exit(1);
            } else {
                byte[] encoded = Files.readAllBytes(Paths.get(args[0]));
                String btString = new String(encoded, "UTF-8");
                ScoreHTML score = new ScoreHTML(btString);
                if (args.length >= 2) {
                    PrintWriter htmlOut = new PrintWriter(args[1]);
                    htmlOut.print(score.getHTML());
                    htmlOut.close();
                } else {
                    System.out.println(score.getHTML());
                }
            }
        } catch (UnsupportedEncodingException uee) {
            stats.addException(SERVICE_NAME, uee, "encoding exception");
        } catch (IOException ioe) {
            stats.addException(SERVICE_NAME, ioe, "input output exception");
        } catch (JDOMException ex) {
            Logger.getLogger(ScoreHTML.class.getName()).log(Level.SEVERE, null, ex);
        } catch (TransformerException ex) {
            Logger.getLogger(ScoreHTML.class.getName()).log(Level.SEVERE, null, ex);
        }
        return stats;
    }

    public URL getTargetURL() {
        return targeturl;
    }

    public void setCorpusName(String s) {
        corpusname = s;
    }

    @Override
    public String getDescription() {
        String description = "This class creates an html visualization "
                + "in the Score format from an exb. ";
        return description;
    }
}
