/*
 * This class runs an xsl transformation on a file
 */
package de.uni_hamburg.corpora.validation;

import de.uni_hamburg.corpora.*;
import de.uni_hamburg.corpora.utilities.PrettyPrinter;
import de.uni_hamburg.corpora.utilities.XSLTransformer;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collection;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import static org.apache.commons.lang3.StringEscapeUtils.escapeHtml4;
import org.jdom2.JDOMException;
import org.xml.sax.SAXException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

/**
 *
 * @author fsnv625
 */
public class GeneralTransformer extends Checker {

    String pathToXSL = "";
    String outputFilename = "";
    URL urlToOutput;
    boolean overwritefiles = false;
    boolean coma = false;
    boolean exb = false;
    boolean exs = false;

    public GeneralTransformer(Properties properties) {
        //fixing is possible
        super(true, properties);
    }

    @Override
    public Report function(CorpusData cd, Boolean fix) throws JDOMException, IOException, URISyntaxException, TransformerConfigurationException, TransformerException, ParserConfigurationException, UnsupportedEncodingException, SAXException, XPathExpressionException {
        Report report = new Report();
        if (fix) {
            CorpusIO cio = new CorpusIO();
            String corpusdata = cd.toUnformattedString();
            String stylesheet = cio.readExternalResourceAsString(pathToXSL);
            XSLTransformer xslt = new XSLTransformer();
            String result
                    = xslt.transform(corpusdata, stylesheet);
            if (result != null) {
                report.addFix(function, cd,
                        "XSL Transformation was successful");

                PrettyPrinter pp = new PrettyPrinter();
                result = pp.indent(result, "event");
            }
            if (overwritefiles) {
                cd.updateUnformattedString(result);
                cio.write(cd, cd.getURL());
            } else {
                cio.write(result, urlToOutput);
            }
        } else {
            report.addCritical(function, cd,
                    "XSL Transformation cannot be checked, only fixed (use -f)");
        }
        return report;
    }

    @Override
    public Collection<Class<? extends CorpusData>> getIsUsableFor() {
        Set<Class<? extends CorpusData>> IsUsableFor = new HashSet<>();
        if (exb) {
            IsUsableFor.add(EXMARaLDATranscriptionData.class);
        }
        if (exs) {
            IsUsableFor.add(UnspecifiedXMLData.class);
        }
        if (coma) {
            IsUsableFor.add(ComaData.class);
        }
        return IsUsableFor;
    }

    public void setPathToXSL(String s) {
        pathToXSL = s;
    }

    public void setOutputFileName(String s) throws MalformedURLException {
        urlToOutput = new URL(cd.getParentURL() + s);
    }

    public void setOverwriteFiles(String s) throws IllegalArgumentException {
        if (s.equalsIgnoreCase("true") || s.equalsIgnoreCase("wahr") || s.equalsIgnoreCase("ja")) {
            overwritefiles = true;
        } else if (s.equalsIgnoreCase("false") || s.equalsIgnoreCase("falsch") || s.equalsIgnoreCase("nein")) {
            overwritefiles = false;
        } else {
            throw new IllegalArgumentException("Parameter coma not recognized: " + escapeHtml4(s));
        }
    }

    public void setComa(String s) throws IllegalArgumentException {
        if (s.equalsIgnoreCase("true") || s.equalsIgnoreCase("wahr") || s.equalsIgnoreCase("ja")) {
            coma = true;
        } else if (s.equalsIgnoreCase("false") || s.equalsIgnoreCase("falsch") || s.equalsIgnoreCase("nein")) {
            coma = false;
        } else {
            throw new IllegalArgumentException("Parameter coma not recognized: " + escapeHtml4(s));
        }
    }

    public void setExb(String s) throws IllegalArgumentException {
        if (s.equalsIgnoreCase("true") || s.equalsIgnoreCase("wahr") || s.equalsIgnoreCase("ja")) {
            exb = true;
        } else if (s.equalsIgnoreCase("false") || s.equalsIgnoreCase("falsch") || s.equalsIgnoreCase("nein")) {
            exb = false;
        } else {
            throw new IllegalArgumentException("Parameter coma not recognized: " + escapeHtml4(s));
        }
    }

    public void setExs(String s) throws IllegalArgumentException {
        if (s.equalsIgnoreCase("true") || s.equalsIgnoreCase("wahr") || s.equalsIgnoreCase("ja")) {
            exs = true;
        } else if (s.equalsIgnoreCase("false") || s.equalsIgnoreCase("falsch") || s.equalsIgnoreCase("nein")) {
            exs = false;
        } else {
            throw new IllegalArgumentException("Parameter coma not recognized: " + escapeHtml4(s));
        }
    }

    /**
     * Default function which returns a two/three line description of what this
     * class is about.
     */
    @Override
    public String getDescription() {
        String description = "This class runs an xsl transformation on files. ";
        return description;
    }

    @Override
    public Report function(Corpus c, Boolean fix) throws SAXException, IOException, ParserConfigurationException, URISyntaxException, JDOMException, TransformerException, TransformerConfigurationException, UnsupportedEncodingException, XPathExpressionException {
        Report stats = new Report();
        if (exb) {
            for (CorpusData cdata : c.getBasicTranscriptionData()) {
                stats.merge(function(cdata, fix));
            }
        }
        if (exs) {
            for (CorpusData scdata : c.getSegmentedTranscriptionData()) {
                stats.merge(function(scdata, fix));
            }
        }
        if (coma) {
            cd = c.getComaData();
            stats = function(cd, fix);
        }
        return stats;
    }

}
