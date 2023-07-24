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

/** *
 * 
 * This class represents a converter for EXMARaLDA Basic Transcriptions into ELAN transcriptions.
 * It operates on String instances of the transcriptions.
 * The class re-uses classes and methods from the EXMARaLDA package.
 *
 * @author Daniel Jettka
 * @author Herbert Lange
 * @version 20230105
 */
public class EXB2EAF {
    
    /** the XSLT stylesheet for converting an EXMARaLDA basic transcription to an EAF document 
        (path applies when in context of a class in exmaralda package, see below) **/
    static final String EX2ELAN_STYLESHEET = "/org/exmaralda/partitureditor/jexmaralda/xsl/BasicTranscription2EAF.xsl";
    
    
    /** Creates a new instance of EXB2EAF */
    public EXB2EAF() {
        
    }
    
    
    /** reads the EXB as String specified by basicTranscrition and returns an ELAN Transcription
     * @param basicTranscription Representation of EXMARaLDA basic transcrition in String
     * @return Representation of ELAN transcription in a String */
    public String EXB2EAF(String basicTranscription){
        return convert(basicTranscription);
    }
    
        
    /** reads the EXB as String specified by basicTranscrition and returns an ELAN Transcription
     * @param basicTranscription Representation of EXMARaLDA basic transcrition in String
     * @return Representation of ELAN transcription in a String */
    public String convert(String basicTranscription){
        
        String result = null;
        
        try{
            /* ELANConverter in EXMARaLDA works with BasicTranscription object */
            BasicTranscription bt = TypeConverter.String2BasicTranscription(basicTranscription);

            /* NOTE: conversion method from ELANConverter in EXMARaLDA cannot be used directly (private),
               so that directives from private method BasicTranscriptionToELAN method from ELANConverter 
               have to replicated here */


            // interpolate the timeline, i.e. calculate absoulute time values for timeline items
            // that don't have an absolute time value assigned
            // (is this necessary or can ELAN also handle time slots without absolute time values?)
            bt.getBody().getCommonTimeline().completeTimes();

            // read BasicTranscription into a String
            String exb = bt.toXML();

            // read the XSL stylesheet into a String
            String xsl = TypeConverter.InputStream2String(org.exmaralda.partitureditor.jexmaralda.convert.ELANConverter.class.getResourceAsStream(EX2ELAN_STYLESHEET));

            // create a class for performing a stylesheet transformation
            XSLTransformer xt = new XSLTransformer();
            result = xt.transform(exb, xsl);
            
        } catch (TransformerConfigurationException ex) {
            Logger.getLogger(EXB2EAF.class.getName()).log(Level.SEVERE, null, ex);
        } catch (TransformerException ex) {
            Logger.getLogger(EXB2EAF.class.getName()).log(Level.SEVERE, null, ex);
        }

        return result;
    }
    
}
