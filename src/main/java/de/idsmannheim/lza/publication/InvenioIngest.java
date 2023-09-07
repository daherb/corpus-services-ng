/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package de.idsmannheim.lza.publication;

import de.idsmannheim.lza.utilities.publication.InvenioTools;
import de.idsmannheim.lza.datacitejavaapi.DataciteAPI;
import de.idsmannheim.lza.inveniojavaapi.InvenioAPI;
import de.uni_hamburg.corpora.Corpus;
import de.uni_hamburg.corpora.CorpusData;
import de.uni_hamburg.corpora.CorpusFunction;
import de.uni_hamburg.corpora.Report;
import de.uni_hamburg.corpora.publication.Publisher;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
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
public class InvenioIngest extends Publisher implements CorpusFunction {

    InvenioTools tools;
    DataciteAPI datacite;
    boolean publicFiles = false;
    boolean privateRecords = false;
    boolean setUp = false;
    // This is the base Uri
    // Either for testing
    private final String dataciteBaseUri = "https://api.test.datacite.org/";
    // Or for productive use
    // private final String dataciteBaseUri = "https://api.datacite.org/";
    private String datacitePrefix;
    
    public InvenioIngest(Properties properties) throws IllegalAccessException, IOException, URISyntaxException {
        super(properties);
        boolean dataciteSetUp = false;
        boolean invenioSetUp = false;
        // Setup datacite api and prefix
        if (properties.containsKey("datacite-repository-id") && 
                properties.containsKey("datacite-repository-password") && 
                properties.containsKey("datacite-prefix")) {
            String repositoryId = properties.getProperty("datacite-repository-id");
            String password = properties.getProperty("datacite-repository-password");
            datacite = new DataciteAPI(new URI(dataciteBaseUri), repositoryId, password);
            datacitePrefix = properties.getProperty("datacite-prefix");
            dataciteSetUp = true;
        }
        // Setup invenio tools
        if (properties.containsKey("invenio-host") && properties.containsKey("invenio-token")) {
            tools = new InvenioTools(new InvenioAPI(properties.getProperty("invenio-host"), properties.getProperty("invenio-token")));
            invenioSetUp = true;
        }
        // Additional invenio flags
        if (properties.containsKey("invenio-public-files")) {
            publicFiles = properties.getProperty("invenio-public-files").equalsIgnoreCase("true");
        }
        if (properties.containsKey("invenio-separate-private-records")) {
            privateRecords = properties.getProperty("invenio-separate-private-records").equalsIgnoreCase("true");
        }
        // Check if we are all set up
        setUp = dataciteSetUp && invenioSetUp;
    
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
            LOG.info("Start SIP upload");
            StopWatch watch = new StopWatch();
            watch.start();
            try {
                boolean update = props.getProperty("update-object", "false").equalsIgnoreCase("true");
                boolean publishDois = props.getProperty("publish-dois-i-know-what-i-am-doing-trust-me", "false").equals("IKNOWWHATIAMDOING");
                // "hidden" flag to not publish records
                boolean publishRecords = !props.getProperty("do-not-publish", "false").equalsIgnoreCase("true");
                Optional<String> id = tools.createOrUpdateObject(Path.of(c.getBaseDirectory().toURI()),Optional.of(datacite), Optional.of(datacitePrefix), publicFiles, privateRecords, update, publishRecords, publishDois, report);
                if (id.isPresent()) {
                    report.addNote(getFunction(), "Created new record " + id.get());
                }
                else {
                    report.addCritical(getFunction(), "No record created");
                }
            } catch (Exception e) {
                report.addException(e, "Exception when uploading information package");
            }
            watch.stop();
            LOG.info("Done");
            report.addNote(getFunction(), "Upload took " + watch.getTime(TimeUnit.SECONDS) + " seconds");
            
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
        params.put("invenio-public-files", "Optional flag if files will be publicly accessible by default if not specified as private");
        params.put("invenio-separate-private-records", "Optional flag if private files should be stored in a seprate record");
        params.put("update-object", "Optional flag if existing records with the same name should be updated. Otherwise the process is stopped as soon as a record already exists");
        params.put("datacite-repository-id", "Repository ID for Datacite DOIs");
        params.put("datacite-repository-password", "Repository password for Datacite DOIs");
        params.put("publish-dois-i-know-what-i-am-doing-trust-me", "Flag to publish draft DOIs by registering them. The value must be IKNOWWHATIAMDOING");
        return params;
    }
    
    
}
