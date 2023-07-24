/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uni_hamburg.corpora.data;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collection;

/**
 *
 * @author fsnv625
 */
public interface Metadata extends CorpusData {
    
    public Collection<URL> getReferencedCorpusDataURLs() throws MalformedURLException, URISyntaxException; 
}
