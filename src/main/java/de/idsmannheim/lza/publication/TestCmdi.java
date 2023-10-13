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
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpressionException;
import org.exmaralda.partitureditor.fsm.FSMException;
import org.exmaralda.partitureditor.jexmaralda.JexmaraldaException;
import org.jdom.JDOMException;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.xml.sax.SAXException;

/**
 *
 * @author Herbert Lange <lange@ids-mannheim.de>
 */
public class TestCmdi extends Publisher implements CorpusFunction {

    public TestCmdi(Properties properties) {
        super(properties);
    }

    
    @Override
    public Report function(CorpusData cd) throws NoSuchAlgorithmException, ClassNotFoundException, FSMException, URISyntaxException, SAXException, IOException, ParserConfigurationException, JexmaraldaException, TransformerException, XPathExpressionException, JDOMException {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public Report function(Corpus c) throws NoSuchAlgorithmException, ClassNotFoundException, FSMException, URISyntaxException, SAXException, IOException, ParserConfigurationException, JexmaraldaException, TransformerException, XPathExpressionException, JDOMException {
        Report report = new Report();
        try {
            Document document = new SAXBuilder().build(new File("/tmp/test.cmdi"));
            Element root = document.getRootElement();
            Element header = root.getChild("Header",root.getNamespace());
            LOG.info(new XMLOutputter(Format.getPrettyFormat()).outputString(header));
            header.getChild("MdSelfLink",root.getNamespace()).setText("Test");
            LOG.info(new XMLOutputter(Format.getPrettyFormat()).outputString(header));
//            Element resourceProxyList = root.getChild("Resources", root.getNamespace()).getChild("ResourceProxyList", root.getNamespace());
//            LOG.info(new XMLOutputter(Format.getPrettyFormat()).outputString(resources));
        } catch (org.jdom2.JDOMException ex) {
            Logger.getLogger(TestCmdi.class.getName()).log(Level.SEVERE, null, ex);
        }
        return report;
    }
    private static final Logger LOG = Logger.getLogger(TestCmdi.class.getName());

    @Override
    public Collection<Class<? extends CorpusData>> getIsUsableFor() {
        return new ArrayList<>();
    }

    @Override
    public String getDescription() {
        return "Tests Cmdi stuff";
    }
    
}
