/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package de.idsmannheim.lza.publication;

import de.idsmannheim.lza.utilities.publication.InvenioTools;
import de.idsmannheim.lza.inveniojavaapi.InvenioAPI;
import de.idsmannheim.lza.inveniojavaapi.InvenioAPITools;
import de.uni_hamburg.corpora.Corpus;
import de.uni_hamburg.corpora.CorpusData;
import de.uni_hamburg.corpora.publication.Publisher;

import de.uni_hamburg.corpora.CorpusFunction;
import de.uni_hamburg.corpora.Report;
import java.io.IOException;
import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpressionException;
import org.exmaralda.partitureditor.fsm.FSMException;
import org.exmaralda.partitureditor.jexmaralda.JexmaraldaException;
import org.jdom.JDOMException;
import org.xml.sax.SAXException;

/**
 * Simple function to delete all unpublished drafts
 * @author Herbert Lange <lange@ids-mannheim.de>
 */
public class InvenioDeleteDrafts extends Publisher implements CorpusFunction {

    InvenioTools tools;
    InvenioAPI api;
    InvenioAPITools apiTools;
    boolean setUp = false;
    
    public InvenioDeleteDrafts(Properties properties) throws IOException {
        super(properties);
        if (properties.containsKey("invenio-host") && properties.containsKey("invenio-token")) {
            api = new InvenioAPI(properties.getProperty("invenio-host"), properties.getProperty("invenio-token"));
            apiTools = new InvenioAPITools(api);
            tools = new InvenioTools(api);
            setUp = true;
        }
    }

    
    @Override
    public Report function(CorpusData cd) throws NoSuchAlgorithmException, ClassNotFoundException, FSMException, URISyntaxException, SAXException, IOException, ParserConfigurationException, JexmaraldaException, TransformerException, XPathExpressionException, JDOMException {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public Report function(Corpus c) throws NoSuchAlgorithmException, ClassNotFoundException, FSMException, URISyntaxException, SAXException, IOException, ParserConfigurationException, JexmaraldaException, TransformerException, XPathExpressionException, JDOMException {
        Report report = new Report();
        if (setUp) {
            try {
                apiTools.deleteDraftRecords();
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
        else {
            report.addCritical(getFunction(),"Not set up properly");
        } 
        return report;
    }

    @Override
    public Collection<Class<? extends CorpusData>> getIsUsableFor() {
        
        // We only work on complete corpora, not separate corpus data
        return Collections.EMPTY_LIST;
    }

    @Override
    public String getDescription() {
        return "Delete all pending drafts";
    }
    
    @Override
    public Map<String, String> getParameters() {
        Map<String,String> params = super.getParameters();
        params.put("invenio-host", "The host providing Invenio API access");
        params.put("invenio-token", "The API token used for the access");
        return params;
    }
}
