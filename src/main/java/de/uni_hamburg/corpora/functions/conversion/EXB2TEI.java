/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.uni_hamburg.corpora.functions.conversion;

import de.uni_hamburg.corpora.utilities.TypeConverter;
import de.uni_hamburg.corpora.utilities.XSLTransformer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import org.exmaralda.partitureditor.jexmaralda.BasicTranscription;
import org.exmaralda.partitureditor.jexmaralda.ListTranscription;
import org.exmaralda.partitureditor.jexmaralda.SegmentedTranscription;
import org.exmaralda.partitureditor.jexmaralda.segment.SegmentedToListInfo;

/**
 *
 * @author sesv009
 * @author Herbert Lange
 * @version 20230105
 */
public class EXB2TEI {
    
    private String segmentationAlgorithm = "GENERIC";
    private String language = "en";
    private static final String STYLESHEET_PATH = "/org/exmaralda/partitureditor/jexmaralda/xsl/EXMARaLDA2TEI.xsl";
    
    /** Creates a new instance of EXB2TEI */
    public EXB2TEI() {
        
    }
               
    /** reads the EXB as String specified by basicTranscrition and returns a TEI Transcription
     * @param basicTranscription Representation of EXMARaLDA basic transcrition in String
     * @return Representation of ISO-TEI Spoken transcription in a String */
    
    public String convert(String basicTranscription){
        return convert(basicTranscription, segmentationAlgorithm);
    }
    
    public String convert(String basicTranscription, String algorithmName){
        
        String result = null;
        
        switch (algorithmName) {
            case "GENERIC":
                result = BasicTranscriptionToGenericISOTEI(basicTranscription);
                break;
            case "HIAT":
                result = BasicTranscriptionToGenericISOTEI(basicTranscription);
                break;
            default:
                throw new IllegalArgumentException("Invalid segmentation algoritm: " + algorithmName);
        }         
                        
        return result;
    }
    
    public void setAlgorithm(String algorithmName){
        segmentationAlgorithm = algorithmName;
    }
    
    public String getAlgorithm(){
        return segmentationAlgorithm;
    }
    
    public void setLanguage(String lang){
        language = lang;
    }
    
    public String getLanguage(){
        return language;
    }
    
    private String BasicTranscriptionToGenericISOTEI(String btString) {
        
        String result = null;
        
        try{
            BasicTranscription bt = TypeConverter.String2BasicTranscription(btString);

            SegmentedTranscription st = bt.toSegmentedTranscription();
            ListTranscription lt = st.toListTranscription(new SegmentedToListInfo(st, SegmentedToListInfo.TURN_SEGMENTATION));

            lt.getBody().sort();
            // read the XSL stylesheet into a String
            String xsl = TypeConverter.InputStream2String(org.exmaralda.partitureditor.jexmaralda.convert.TEIConverter.class.getResourceAsStream(STYLESHEET_PATH));

            // perform XSLT transformation
            XSLTransformer xt = new XSLTransformer();
            result = xt.transform(lt.toXML(), xsl);
        
        } catch (TransformerConfigurationException ex) {
            Logger.getLogger(EXB2TEI.class.getName()).log(Level.SEVERE, null, ex);
        } catch (TransformerException ex) {
            Logger.getLogger(EXB2TEI.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return result;
    }
    
}
