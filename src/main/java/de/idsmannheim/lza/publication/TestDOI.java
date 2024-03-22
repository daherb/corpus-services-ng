/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package de.idsmannheim.lza.publication;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import de.idsmannheim.lza.datacitejavaapi.DataciteAPI;
import de.idsmannheim.lza.datacitejavaapi.DataciteAPITools;
import de.uni_hamburg.corpora.Corpus;
import de.uni_hamburg.corpora.CorpusData;
import de.uni_hamburg.corpora.CorpusFunction;
import de.uni_hamburg.corpora.Report;
import de.uni_hamburg.corpora.publication.Publisher;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import java.util.Properties;
import java.util.logging.Logger;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpressionException;
import org.exmaralda.partitureditor.fsm.FSMException;
import org.exmaralda.partitureditor.jexmaralda.JexmaraldaException;
import org.jdom2.JDOMException;
import org.xml.sax.SAXException;

/**
 *
 * @author Herbert Lange <lange@ids-mannheim.de>
 */
public class TestDOI extends Publisher implements CorpusFunction {

    // Datacite user and passwords
    private String user = "";
    private String password = "";
    // This is the base Uri
    // Either for testing
    private final String baseUrl = "https://api.test.datacite.org/";
    // Or for productive use
    // private final String baseUrl = "https://api.datacite.org/";
    private boolean setup = false;
    
    public TestDOI(Properties properties) {
        super(properties);
        boolean userSet = false;
        boolean passwordSet = false;
        if (properties.containsKey("datacite-repository-id")) {
            user = properties.getProperty("datacite-repository-id");
            userSet = true;
        }
        if (properties.containsKey("datacite-repository-password")) {
            password = properties.getProperty("datacite-repository-password");
            passwordSet = true;
        }
        setup = userSet && passwordSet;
    }

    
    @Override
    public Report function(CorpusData cd) throws NoSuchAlgorithmException, ClassNotFoundException, FSMException, URISyntaxException, SAXException, IOException, ParserConfigurationException, JexmaraldaException, TransformerException, XPathExpressionException, JDOMException {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public Report function(Corpus c) throws NoSuchAlgorithmException, ClassNotFoundException, FSMException, URISyntaxException, SAXException, IOException, ParserConfigurationException, JexmaraldaException, TransformerException, XPathExpressionException, JDOMException {
        Report report = new Report();
        if (setup) {
            DataciteAPITools api = new DataciteAPITools(new DataciteAPI(new URI(baseUrl), user, password));
            String prefix = "10.82744";
            String newSuffix = "";
//            try {
//                JsonElement result = api.createDraftDOI(prefix, Optional.empty());
//                newSuffix = result.getAsJsonObject().getAsJsonObject("data")
//                                                    .getAsJsonObject("attributes")
//                                                    .getAsJsonPrimitive("suffix").getAsString();
//                
//                LOG.info(prettyPrintJson(result));
//            }
//            catch (Exception e) {
//                e.printStackTrace();
//            }
//            try {
//                LOG.info(prettyPrintJson(api.createDraftDOI(prefix, Optional.of("foobar"))));
//            }
//            catch (Exception e) {
//                e.printStackTrace();
//            }
//            System.out.println("Press any key");
//            System.in.read();
//            try {
//                LOG.info(prettyPrintJson(api.ListAllDOIs(prefix)));
//            }
//            catch (Exception e) {
//                e.printStackTrace();
//            }
//            System.out.println("Press any key");
//            System.in.read();
//            try {
//                LOG.info(String.valueOf(api.deleteDraftDOI(prefix, newSuffix)));
//            }
//            catch (Exception e) {
//                e.printStackTrace();
//            }
//                                    try {
//                LOG.info(String.valueOf(api.deleteDraftDOI(prefix, "foobar")));
//            }
//            catch (Exception e) {
//                e.printStackTrace();
//            }
//                                    System.out.println("Press any key");
//            System.in.read();
//            try {
//                LOG.info(prettyPrintJson(api.ListAllDOIs(prefix)));
//            }
//            catch (Exception e) {
//                e.printStackTrace();
//            }
        }
        else {
            report.addCritical("Function not properly set up");
        }
        return report;
    }
    private static final Logger LOG = Logger.getLogger(TestDOI.class.getName());

    @Override
    public Collection<Class<? extends CorpusData>> getIsUsableFor() {
        return new ArrayList<>();
    }

    @Override
    public String getDescription() {
        return "Tests the DataCite DOI api";
    }

    private String prettyPrintJson(JsonElement elem) {
        return new GsonBuilder().setPrettyPrinting().create().toJson(elem);
    }
    
}
