/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.uni_hamburg.corpora.visualization;

import de.uni_hamburg.corpora.utilities.TypeConverter;
import static de.uni_hamburg.corpora.utilities.TypeConverter.JdomDocument2String;
import static de.uni_hamburg.corpora.utilities.TypeConverter.JdomDocument2W3cDocument;
import de.uni_hamburg.corpora.utilities.XPathEvaluator;
import de.uni_hamburg.corpora.utilities.XSLTransformer;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.exmaralda.common.corpusbuild.FileIO;
import org.exmaralda.partitureditor.interlinearText.HTMLParameters;
import org.exmaralda.partitureditor.interlinearText.InterlinearText;
import org.exmaralda.partitureditor.jexmaralda.BasicTranscription;
import org.exmaralda.partitureditor.jexmaralda.JexmaraldaException;
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
 */
public class EXB2ScoreHTML {
    
    // resources loaded from directory supplied in pom.xml
    private static final String STYLESHEET_PATH = "/xsl/Score2HTML.xsl";
    private static final String JS_HIGHLIGHTING_PATH = "/js/timelight-0.1.min.js";
    
    private final String EMAIL_ADDRESS = "corpora@uni-hamburg.de";
    private final String SERVICE_NAME = "EXB2Score";
    private final String HZSK_WEBSITE = "https://corpora.uni-hamburg.de/";
    
    
    public String convert(String btAsString, String transcriptionId, String recordingId, String recordingType){
        
        String result = null;
        
        try{
        
            BasicTranscription bt = TypeConverter.String2BasicTranscription(btAsString);
            bt.normalize();

            TierFormatTable tft = new TierFormatTable(bt);

            ItConverter itc = new ItConverter();
            InterlinearText it = itc.BasicTranscriptionToInterlinearText(bt, tft, 0);

            //setting HTML parameters
            HTMLParameters param = new HTMLParameters();
            param.setWidth(900);
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
            xt.setParameter("RECORDING_PATH", recordingId);
            xt.setParameter("RECORDING_TYPE", recordingType);
            xt.setParameter("EMAIL_ADDRESS", EMAIL_ADDRESS);
            xt.setParameter("WEBSERVICE_NAME", SERVICE_NAME);
            xt.setParameter("HZSK_WEBSITE", HZSK_WEBSITE);

            // perform XSLT transformation
            result = xt.transform(xml, xsl);

            // insert JavaScript for highlighting
            String js = TypeConverter.InputStream2String(getClass().getResourceAsStream(JS_HIGHLIGHTING_PATH));
            result = result.replace("<!--jsholder-->", js);

        }
        catch(IOException ex){
            Logger.getLogger(EXB2ScoreHTML.class.getName()).log(Level.SEVERE, null, ex);
        } catch (TransformerException ex) {
            Logger.getLogger(EXB2ScoreHTML.class.getName()).log(Level.SEVERE, null, ex);
        } catch (JDOMException ex) {
            Logger.getLogger(EXB2ScoreHTML.class.getName()).log(Level.SEVERE, null, ex);
        }
            
        return result;
    }
    
    
}
