/*
 * To change this license header, choose License Headers in ProJect Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uni_hamburg.corpora;

import org.jdom2.Document;

/**
 *
 * @author fsnv625
 *
 * Last updated
 * @author Herbert Lange
 * @version 20240322
 */
public interface XMLData {
    
    public Document getJdom();
    
    public void setJdom(Document jdom);
}
