/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package de.uni_hamburg.corpora.publication.ids;

import de.idsmannheim.lza.inveniojavaapi.API;
import de.uni_hamburg.corpora.Corpus;
import de.uni_hamburg.corpora.CorpusData;
import de.uni_hamburg.corpora.CorpusFunction;
import de.uni_hamburg.corpora.Report;
import de.uni_hamburg.corpora.ReportItem;
import de.uni_hamburg.corpora.publication.Publisher;
import de.uni_hamburg.corpora.utilities.publication.ids.InvenioAPITools;
import de.uni_hamburg.corpora.utilities.publication.ids.InvenioAPITools.InvenioSIP;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.security.KeyManagementException;
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
 * Class using the REST API to publish a corpus to Invenio
 * @author Herbert Lange <lange@ids-mannheim.de>
 */
public class InvenioSIPUpload extends Publisher implements CorpusFunction {

    InvenioAPITools tools;
    boolean publicFiles = false;
    boolean setUp = false;
    
    public InvenioSIPUpload(Properties properties) throws IllegalAccessException, IOException {
        super(properties);
        if (properties.containsKey("invenio-host") && properties.containsKey("invenio-token")) {
            tools = new InvenioAPITools(new API(properties.getProperty("invenio-host"), properties.getProperty("invenio-token")));
            setUp = true;
        }
        if (properties.contains("invenio-public-files")) {
            publicFiles = properties.getProperty("invenio-public-files").equalsIgnoreCase("true");
        }
    
    }
    private static final Logger LOG = Logger.getLogger(InvenioSIPUpload.class.getName());

    
    @Override
    public Report function(CorpusData cd) throws NoSuchAlgorithmException, ClassNotFoundException, FSMException, URISyntaxException, SAXException, IOException, ParserConfigurationException, JexmaraldaException, TransformerException, XPathExpressionException, JDOMException {
        throw new UnsupportedOperationException("Not supported for corpus data."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public Report function(Corpus c) throws NoSuchAlgorithmException, ClassNotFoundException, FSMException, URISyntaxException, SAXException, IOException, ParserConfigurationException, JexmaraldaException, TransformerException, XPathExpressionException, JDOMException {
        Report report = new Report();
        if (setUp) {
        try {
            report.addNote(getFunction(), "Start SIP upload");
            StopWatch watch = new StopWatch();
            watch.start();
            InvenioSIP sipIds = tools.uploadDraftSip(Path.of(c.getBaseDirectory().toURI()), publicFiles);
            report.addNote(getFunction(), "Created new records " + sipIds);
            watch.stop();
            LOG.info("Done");
            report.addNote(getFunction(), "Upload took " + watch.getTime(TimeUnit.SECONDS) + " seconds");
            
        }
        catch (IOException | InterruptedException | URISyntaxException | KeyManagementException | NoSuchAlgorithmException | org.jdom2.JDOMException | CloneNotSupportedException e) {
            report.addCritical(getFunction(), ReportItem.newParamMap(
                            new ReportItem.Field[]{ReportItem.Field.Function, ReportItem.Field.Exception, ReportItem.Field.Description}, 
                            new Object[]{getFunction(), e, "Exception when uploading SIP"}
                    ));
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
        return "Publishes a corpus as a SIP into the IDS Invenio LZA";
    }

    @Override
    public Map<String, String> getParameters() {
        Map<String,String> params = super.getParameters();
        params.put("invenio-host", "The host providing Invenio API access");
        params.put("invenio-token", "The API token used for the access");
        params.put("invenio-public-files", "Optional flag if files will be publicly accessible");
        return params;
    }
    
    
}
