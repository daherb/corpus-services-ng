/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uni_hamburg.corpora;

import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpressionException;
import org.xml.sax.SAXException;

/**
 *
 * @author fsnv625
 */
public interface CorpusData {

    URL getURL();
    
    URL getParentURL();
    
    String getFilename();
    
    String getFilenameWithoutFileEnding();

    String toSaveableString() throws TransformerException, ParserConfigurationException, SAXException, IOException, XPathExpressionException;

    String toUnformattedString();
    
    //needed if there were changes to the file so they are represented in the object too
    void updateUnformattedString(String newUnformattedString);

    /**
     * Gets the collection of all expected file extensions for the file type.
     *
     * @return the file extensions in lower case
     * @author bba1792, Dr. Herbert lange
     * @version 20210924
     */
    Collection<String> getFileExtensions();

    Object clone() ;
}
