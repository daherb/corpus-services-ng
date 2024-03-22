/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.uni_hamburg.corpora.visualization;

import de.uni_hamburg.corpora.utilities.TypeConverter;
import de.uni_hamburg.corpora.utilities.XSLTransformer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import org.exmaralda.partitureditor.fsm.FSMException;
import org.exmaralda.partitureditor.jexmaralda.BasicTranscription;
import org.exmaralda.partitureditor.jexmaralda.JexmaraldaException;
import org.exmaralda.partitureditor.jexmaralda.ListTranscription;
import org.exmaralda.partitureditor.jexmaralda.segment.GATSegmentation;
import org.xml.sax.SAXException;

/**
 *
 * @author Daniel Jettka
 *
 * Last updated
 * @author Herbert Lange
 * @version 20240322
 */
public class GATListHTML extends ListHTML {
    
    private static final String segmentationAlgorithm = "GAT";
    
    // resources loaded from directory supplied in pom.xml
    static final String STYLESHEET_PATH = "/xsl/GAT2ListHTML.xsl";  
    private static final String SERVICE_NAME = "GATListHTML";

        
    public GATListHTML(String btAsString) {
        super(btAsString, "GAT");
        createFromBasicTranscription(btAsString);
    }
        
    
	 /**
	 * This method deals performs the transformation of EXB to GATList HTML
	 *
	 * @param  btAsString  the EXB file represented in a String object
	 * @return  
	 */
    private void createFromBasicTranscription(String btAsString){
    
        basicTranscriptionString = btAsString;
        basicTranscription = TypeConverter.String2BasicTranscription(btAsString);
        
        String result = null;
              
        try {
            
            BasicTranscription bt = basicTranscription;
        
            // segment the basic transcription and transform it into a list transcription
            GATSegmentation segmenter = new org.exmaralda.partitureditor.jexmaralda.segment.GATSegmentation("");
            ListTranscription lt;   
            lt = segmenter.BasicToIntonationUnitList(bt);
                        
            // read the XSL stylesheet into a String
            String xsl = TypeConverter.InputStream2String(getClass().getResourceAsStream(STYLESHEET_PATH));

            // read segmented transcription into String
            org.jdom.Document document = GATSegmentation.toXML(lt);
            String xml = new org.jdom.output.XMLOutputter().outputString(document);

            XSLTransformer xt = new XSLTransformer();
            result = xt.transform(xml, xsl);
            
        } catch (FSMException ex) {
            Logger.getLogger(GATListHTML.class.getName()).log(Level.SEVERE, null, ex);
        } catch (SAXException ex) {
            Logger.getLogger(GATListHTML.class.getName()).log(Level.SEVERE, null, ex);
        } catch (JexmaraldaException ex) {
            Logger.getLogger(GATListHTML.class.getName()).log(Level.SEVERE, null, ex);
        } catch (TransformerConfigurationException ex) {
            Logger.getLogger(GATListHTML.class.getName()).log(Level.SEVERE, null, ex);
        } catch (TransformerException ex) {
            Logger.getLogger(GATListHTML.class.getName()).log(Level.SEVERE, null, ex);
        }
   
        setHTML(result); 
        
    }
    
}
