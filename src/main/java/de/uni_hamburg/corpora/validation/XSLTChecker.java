package de.uni_hamburg.corpora.validation;

import de.uni_hamburg.corpora.*;

import static de.uni_hamburg.corpora.CorpusMagician.exmaError;

import de.uni_hamburg.corpora.utilities.TypeConverter;
import de.uni_hamburg.corpora.utilities.XSLTransformer;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.List;
import java.util.Scanner;
import javax.xml.transform.TransformerException;
import org.exmaralda.partitureditor.jexmaralda.JexmaraldaException;
import org.jdom.JDOMException;
import org.xml.sax.SAXException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;
import org.exmaralda.partitureditor.fsm.FSMException;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.xpath.XPath;

/**
 *
 * @author Daniel Jettka, daniel.jettka@uni-hamburg.de
 *
 * This class runs many little checks specified in a XSLT stylesheet and adds
 * them to the report.";
 *
 */
public class XSLTChecker extends Checker implements CorpusFunction {

    String xslresource = "/xsl/nslc-checks.xsl";
    String filename = "";
    String UTTERANCEENDSYMBOLS = "[.!?…:]";
    String FSMpath = "";

    public XSLTChecker() {
        //fixing is not possible
        super(false);
    }

    @Override
    public Report function(CorpusData cd, Boolean fix) throws SAXException, JexmaraldaException, TransformerException, ParserConfigurationException, IOException, XPathExpressionException, MalformedURLException, JDOMException, URISyntaxException {

        Report r = new Report();
        filename = cd.getURL().getFile().subSequence(cd.getURL().getFile().lastIndexOf('/') + 1, cd.getURL().getFile().lastIndexOf('.')).toString();

            //get UtteranceEndSymbols form FSM if supplied
            if (!FSMpath.equals("")) {
                setUtteranceEndSymbols(FSMpath);
            }
            // get the XSLT stylesheet
            String xsl = TypeConverter.InputStream2String(getClass().getResourceAsStream(xslresource));

            // create XSLTransformer and set the parameters 
            XSLTransformer xt = new XSLTransformer();

            xt.setParameter("filename", filename);
            xt.setParameter("UTTERANCEENDSYMBOL", UTTERANCEENDSYMBOLS);
            // perform XSLT transformation
            String result = xt.transform(cd.toSaveableString(), xsl);

            //read lines and add to Report
            Scanner scanner = new Scanner(result);

            int i = 1;
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();

                //split line by ;
                String[] lineParts = line.split(";", -1);
                if (lineParts.length != 5) {
                    String message = "";
                    for (String s : lineParts) {
                        message = message + s;
                    }
                    r.addCritical(lineParts[0], cd, "There was an exception while creating the error probably because of a semicolon or newline in an event: " + message);
                } else {
                    switch (lineParts[1].toUpperCase()) {
                        case "WARNING":
                            r.addWarning(lineParts[0], cd, lineParts[2]);
                            /* if (cd.getFilename().endsWith(".exb")) {
                                exmaError.addError("XSLTChecker", cd.getURL().getFile(), lineParts[2], lineParts[3], false, lineParts[1]);
                            } */
                            break;
                        case "CRITICAL":
                            r.addCritical(lineParts[0], cd, lineParts[2]);
                            if (cd.getFilename().endsWith(".exb")) {
                                exmaError.addError(lineParts[0], cd.getURL().getFile(), lineParts[3], lineParts[4], false, lineParts[2]);

                            }
                            break;
                        case "NOTE":
                            r.addNote(lineParts[0], cd, lineParts[2]);
                            break;
                        case "MISSING":
                            r.addMissing(lineParts[0], cd, lineParts[2]);
                            if (cd.getFilename().endsWith(".exb")) {
                                exmaError.addError(lineParts[0], cd.getURL().getFile(), lineParts[3], lineParts[4], false, lineParts[2]);
                            }
                            break;
                        default:
                            r.addCritical(lineParts[0], cd, "(Unrecognized report type): " + lineParts[2]);
                            if (cd.getFilename().endsWith(".exb")) {
                                exmaError.addError(lineParts[0], cd.getURL().getFile(), lineParts[3], lineParts[4], false, lineParts[2]);
                            }
                    }
                }

                i++;
            }

            scanner.close();
        return r;

    }

    public void setXSLresource(String s) {
        xslresource = s;
    }

    @Override
    public Collection<Class<? extends CorpusData>> getIsUsableFor() {
        try {
            Class cl = Class.forName("de.uni_hamburg.corpora.BasicTranscriptionData");
            IsUsableFor.add(cl);
            Class cl1 = Class.forName("de.uni_hamburg.corpora.ComaData");
            IsUsableFor.add(cl1);
            //Class cl2 = Class.forName("de.uni_hamburg.corpora.UnspecifiedXMLData");
            //IsUsableFor.add(cl2);
        } catch (ClassNotFoundException ex) {
            report.addException(ex, "unknown class not found error");
        }
        return IsUsableFor;
    }

    public void setUtteranceEndSymbols(String fsmPath) throws MalformedURLException, JDOMException, IOException, URISyntaxException {
        //now get the UtteranceEndSymbols from the FSM XML file
        //XPath: "//fsm/char-set[@id='UtteranceEndSymbols']/char"
        UTTERANCEENDSYMBOLS = "";
        CorpusIO cio = new CorpusIO();
        URL url = Paths.get(fsmPath).toUri().toURL();
        String fsmstring = cio.readExternalResourceAsString(url.toString());
        Document fsmdoc = de.uni_hamburg.corpora.utilities.TypeConverter.String2JdomDocument(fsmstring);
        XPath xpath = XPath.newInstance("//fsm/char-set[@id='UtteranceEndSymbols']/char");
        List allContextInstances = xpath.selectNodes(fsmdoc);
        if (!allContextInstances.isEmpty()) {
            for (int i = 0; i < allContextInstances.size(); i++) {
                Object o = allContextInstances.get(i);
                if (o instanceof Element) {
                    Element e = (Element) o;
                    String symbol = e.getText();
                    System.out.println(symbol);
                    UTTERANCEENDSYMBOLS = UTTERANCEENDSYMBOLS + symbol;
                }
            }
        }
        //needs to be a RegEx (set)
        UTTERANCEENDSYMBOLS = "[" + UTTERANCEENDSYMBOLS + "]";
        System.out.println(UTTERANCEENDSYMBOLS);
    }

    public void setFSMpath(String s) {
        FSMpath = s;
    }

    /**
     * Default function which returns a two/three line description of what this
     * class is about.
     */
    @Override
    public String getDescription() {
        String description = "This class runs many little checks specified"
                + " in a XSLT stylesheet and adds them to the report. ";
        return description;
    }

    @Override
    public Report function(Corpus c, Boolean fix) throws SAXException, JDOMException, IOException, JexmaraldaException, TransformerException, ParserConfigurationException, UnsupportedEncodingException, XPathExpressionException, NoSuchAlgorithmException, ClassNotFoundException, FSMException, URISyntaxException {
        Report stats = new Report();
        ComaData cdata = c.getComaData();
        // Only if coma data exists, run the function
        if (cdata != null){
            stats.merge(function(cdata, fix));
        }
        for (CorpusData bdata : c.getBasicTranscriptionData()) {
            stats.merge(function(bdata, fix));
        }
        return stats;
    }
}
