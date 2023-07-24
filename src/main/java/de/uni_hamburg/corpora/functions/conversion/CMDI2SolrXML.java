/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.uni_hamburg.corpora.functions.conversion;

import de.uni_hamburg.corpora.utilities.TypeConverter;
import de.uni_hamburg.corpora.utilities.XSLTransformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;

/**
 *
 * @author Daniel Jettka
 * @author Herbert Lange
 * @version 20230105
 */
public class CMDI2SolrXML {
    
    /** the XSLT stylesheet for converting an EXMARaLDA basic transcription to an EAF document 
        (path applies when in context of a class in exmaralda package, see below) **/
    static final String STYLESHEET_PATH = "/xsl/CMDI2SolrDocument.xsl";
    
    
    
    public String convert(String cmdiString) throws TransformerConfigurationException, TransformerException {
                        
        // read the XSL stylesheet into a String
        String xsl = TypeConverter.InputStream2String(getClass().getResourceAsStream(STYLESHEET_PATH));

        // perform XSLT transformation
        XSLTransformer xt = new XSLTransformer();
        String result = xt.transform(cmdiString, xsl);

        return result;
         
    }
    
    
}
