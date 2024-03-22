/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uni_hamburg.corpora;

import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.Objects;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpressionException;

import org.jdom2.JDOMException;
import org.xml.sax.SAXException;

/**
 *
 * @author fsnv625
 *
 * Last updated
 * @author Herbert Lange
 * @version 20240322
 */
public interface CorpusData {

    /**
     * Class representing a location in a corpus given by a tier id and an annotation id
     */
    class Location {
        public String tier;
        public String segment;

        public Location(String tier, String segment) {
            this.tier = tier;
            this.segment = segment;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Location location = (Location) o;
            return tier.equals(location.tier) && segment.equals(location.segment);
        }

        @Override
        public int hashCode() {
            return Objects.hash(tier, segment);
        }

        @Override
        public String toString() {
            return "Location{" +
                    "tier='" + tier + '\'' +
                    ", segment='" + segment + '\'' +
                    '}';
        }
    }

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

    Location getLocation(String token) throws JDOMException;
}
