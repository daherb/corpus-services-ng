/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package de.idsmannheim.lza.publication;

import de.idsmannheim.lza.datacitejavaapi.DataciteAPI;
import de.idsmannheim.lza.datacitejavaapi.DataciteAPITools;
import de.idsmannheim.lza.utilities.publication.InvenioTools;
import de.idsmannheim.lza.inveniojavaapi.InvenioAPI;
import de.idsmannheim.lza.inveniojavaapi.InvenioAPITools;
import de.uni_hamburg.corpora.Corpus;
import de.uni_hamburg.corpora.CorpusData;
import de.uni_hamburg.corpora.publication.Publisher;

import de.uni_hamburg.corpora.CorpusFunction;
import de.uni_hamburg.corpora.Report;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpressionException;
import org.datacite.ApiException;
import org.exmaralda.partitureditor.fsm.FSMException;
import org.exmaralda.partitureditor.jexmaralda.JexmaraldaException;
import org.jdom2.JDOMException;
import org.xml.sax.SAXException;

/**
 * Simple function to delete all unpublished drafts (and all draft DOIs if Datacite credentials are give)
 * @author Herbert Lange <lange@ids-mannheim.de>
 *
 * Last updated
 * @author Herbert Lange
 * @version 20240322
 */
public class InvenioDeleteDrafts extends Publisher implements CorpusFunction {

    InvenioTools tools;
    InvenioAPI api;
    InvenioAPITools apiTools;
    boolean setUp = false;
    private Optional<DataciteAPI> datacite = Optional.empty();
        // This is the base Uri
    // Either for testing
    private final String dataciteBaseUri = "https://api.test.datacite.org/";
    // Or for productive use
    // private final String dataciteBaseUri = "https://api.datacite.org/";
    private Optional<String> datacitePrefix = Optional.empty();
    
    public InvenioDeleteDrafts(Properties properties) throws IOException, URISyntaxException {
        super(properties);
        if (properties.containsKey("invenio-host") && properties.containsKey("invenio-token")) {
            api = new InvenioAPI(properties.getProperty("invenio-host"), properties.getProperty("invenio-token"));
            apiTools = new InvenioAPITools(api);
            tools = new InvenioTools(api);
            setUp = true;
        }
        if (properties.containsKey("datacite-repository-id") && 
                properties.containsKey("datacite-repository-password") && 
                properties.containsKey("datacite-prefix")) {
            String repositoryId = properties.getProperty("datacite-repository-id");
            String password = properties.getProperty("datacite-repository-password");
            datacite = Optional.of(new DataciteAPI(new URI(dataciteBaseUri), repositoryId, password));
            datacitePrefix = Optional.of(properties.getProperty("datacite-prefix"));
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
                rollback(datacite, datacitePrefix);
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
    
    private void rollback(Optional<DataciteAPI> datacite, Optional<String> datacitePrefix) throws URISyntaxException, NoSuchAlgorithmException, NoSuchAlgorithmException, KeyManagementException, IOException, InterruptedException {
        apiTools.deleteDraftRecords();
        // Potentially delete draft DOIs
        datacite.ifPresent((dc) -> datacitePrefix.ifPresent((prefix) -> {
            try {
                new DataciteAPITools(dc).deleteAllDraftDOIs(prefix);
            } catch (ApiException ex) {
                Logger.getLogger(InvenioTools.class.getName()).log(Level.SEVERE, null, ex);
            }
        }));
    }
        
    @Override
    public Map<String, String> getParameters() {
        Map<String,String> params = super.getParameters();
        params.put("invenio-host", "The host providing Invenio API access");
        params.put("invenio-token", "The API token used for the access");
        params.put("datacite-repository-id", "Optional repository ID for Datacite DOIs if draft DOIs should be removed");
        params.put("datacite-repository-password", "Optional epository password for Datacite DOIs if draft DOIs should be removed");
        return params;
    }
}
