/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package de.idsmannheim.lza.publication;

import de.idsmannheim.lza.inveniojavaapi.InvenioAPI;
import de.idsmannheim.lza.inveniojavaapi.InvenioAPITools;
import de.uni_hamburg.corpora.Corpus;
import de.uni_hamburg.corpora.CorpusData;
import de.uni_hamburg.corpora.CorpusFunction;
import de.uni_hamburg.corpora.Report;
import de.uni_hamburg.corpora.publication.Publisher;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpressionException;
import org.apache.commons.lang3.time.StopWatch;
import org.exmaralda.partitureditor.fsm.FSMException;
import org.exmaralda.partitureditor.jexmaralda.JexmaraldaException;
import org.jdom.JDOMException;
import org.xml.sax.SAXException;

/**
 * Class using the REST API to retrieve a corpus from Invenio
 * @author Herbert Lange <lange@ids-mannheim.de>
 */
public class InvenioDissemination extends Publisher implements CorpusFunction {

    InvenioTools tools;
    InvenioAPI api;
    InvenioAPITools apiTools;
    boolean setUp = false;
    String recordId;
    String recordTitle;
    
    public InvenioDissemination(Properties properties) throws IllegalAccessException, IOException {
        super(properties);
        if (properties.containsKey("invenio-host") && properties.containsKey("invenio-token") && (properties.containsKey("invenio-record-id") || properties.containsKey("invenio-record-title"))) {
            api = new InvenioAPI(properties.getProperty("invenio-host"), properties.getProperty("invenio-token"));
            apiTools = new InvenioAPITools(api);
            tools = new InvenioTools(api);
            // Either record id or title have to be given
            recordId = properties.getProperty("invenio-record-id",null);
            recordTitle = properties.getProperty("invenio-record-title", null);
            if (recordId != null || recordTitle != null)
                setUp = true;
        }
    
    }
    private static final Logger LOG = Logger.getLogger(InvenioIngest.class.getName());

    @Override
    public Report function(CorpusData cd) throws NoSuchAlgorithmException, ClassNotFoundException, FSMException, URISyntaxException, SAXException, IOException, ParserConfigurationException, JexmaraldaException, TransformerException, XPathExpressionException, JDOMException {
        throw new UnsupportedOperationException("Not supported for corpus data."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public Report function(Corpus c) throws NoSuchAlgorithmException, ClassNotFoundException, FSMException, URISyntaxException, SAXException, IOException, ParserConfigurationException, JexmaraldaException, TransformerException, XPathExpressionException, JDOMException {
        Report report = new Report();
        if (setUp) {
            LOG.info("Start SIP download");
            StopWatch watch = new StopWatch();
            watch.start();
            try {
                if (recordId == null) {
                    recordId = apiTools.getRecordIdForTitle(recordTitle);
                }
                tools.downloadObject(recordId, Path.of(c.getBaseDirectory().toURI()), report);
            } catch (Exception e) {
                report.addException(e, "Exception when downloading information package");
            }
            watch.stop();
            LOG.info("Done");
            report.addNote(getFunction(), "Download took " + watch.getTime(TimeUnit.SECONDS) + " seconds");
            
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
        return "Downloads a corpus as a DIP (SIP) from the IDS Invenio LZA";
    }
    
    @Override
    public Map<String, String> getParameters() {
        Map<String,String> params = super.getParameters();
        params.put("invenio-host", "The host providing Invenio API access");
        params.put("invenio-token", "The API token used for the access");
        params.put("invenio-record-id", "The record id to be downloaded, optional if record title is given");
        params.put("invenio-record-title", "The record title to be downloaded, optional if record id is given");
        return params;
    }
}
