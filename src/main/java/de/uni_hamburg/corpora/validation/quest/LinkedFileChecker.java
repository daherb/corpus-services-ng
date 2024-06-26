package de.uni_hamburg.corpora.validation.quest;
import com.google.common.collect.Sets;
import de.uni_hamburg.corpora.validation.Checker;
import de.uni_hamburg.corpora.*;
import org.exmaralda.partitureditor.fsm.FSMException;
import org.exmaralda.partitureditor.jexmaralda.JexmaraldaException;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.filter.ElementFilter;
import org.jdom2.xpath.XPathBuilder;
import org.jdom2.xpath.XPathExpression;
import org.jdom2.xpath.jaxen.JaxenXPathFactory;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpressionException;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Extracts a list of referenced files from a corpus
 *
 * Last updated
 * @author Herbert Lange
 * @version 20240322
 */
public class LinkedFileChecker extends Checker implements CorpusFunction {

    Logger logger = Logger.getLogger(this.getClass().getName());

    // List of URIs for all linked files
    List<URI> corpusFiles = new ArrayList<>();
    public LinkedFileChecker(Properties properties) {
        super(false,properties);
    }

    @Override
    public Report function(CorpusData cd, Boolean fix) throws NoSuchAlgorithmException, ClassNotFoundException, FSMException, URISyntaxException, SAXException, IOException, ParserConfigurationException, JexmaraldaException, TransformerException, XPathExpressionException, JDOMException {
        Report report = new Report();
        List<URI> refFiles = new ArrayList<>();
        try {
            if (cd instanceof ComaData)
                refFiles.addAll(getReferencedFiles((ComaData) cd));
            else if (cd instanceof EXMARaLDATranscriptionData)
                refFiles.addAll(getReferencedFiles((EXMARaLDATranscriptionData) cd));
            else if (cd instanceof EXMARaLDASegmentedTranscriptionData)
                refFiles.addAll(getReferencedFiles((EXMARaLDASegmentedTranscriptionData) cd));
            else if (cd instanceof ELANData)
                refFiles.addAll(getReferencedFiles((ELANData) cd));
            else if (cd instanceof IMDIData)
                refFiles.addAll(getReferencedFiles((IMDIData) cd));
        }
        catch (JDOMException | MalformedURLException | URISyntaxException e) {
            report.addCritical(getFunction(), ReportItem.newParamMap(
                    new ReportItem.Field[]{ReportItem.Field.Function, ReportItem.Field.Exception, ReportItem.Field.Description},
                    new Object[]{getFunction(),e,
                            "Exception when trying to extract files from: " + cd.getFilename()}));
        }
        if (refFiles.isEmpty())
            report.addWarning(getFunction(),ReportItem.newParamMap(
                    new ReportItem.Field[]{ReportItem.Field.Function, ReportItem.Field.Description},
                    new Object[]{getFunction(),"No linked files found in " + cd.getFilename()}));
        corpusFiles.addAll(refFiles);
        return report;
    }

    @Override
    public Report function(Corpus c, Boolean fix) throws NoSuchAlgorithmException, ClassNotFoundException, FSMException, URISyntaxException, SAXException, IOException, ParserConfigurationException, JexmaraldaException, TransformerException, XPathExpressionException, JDOMException {
        Report report = new Report();
        for (CorpusData cd : c.getCorpusData())
            if (getIsUsableFor().contains(cd.getClass()))
                report.merge(function(cd, fix));
        // Also load all files referenced in the refco documentation
        if (props.containsKey("refco-file")) {
            RefcoChecker rc = new RefcoChecker(props);
            rc.setRefcoFile(Paths.get(Paths.get(c.getBaseDirectory().toURI()).toString(),props.getProperty("refco-file")).toString());
            corpusFiles.addAll(rc.getDocumentedFiles().stream().map((f) -> Paths.get(c.getBaseDirectory().getPath(),
                    f).toFile().toURI()).collect(Collectors.toList()));
        }
        return report;
    }

    @Override
    public Collection<Class<? extends CorpusData>> getIsUsableFor() {
        Set<Class<? extends CorpusData>> usableFor = new HashSet<>();
        usableFor.add(ComaData.class);
        usableFor.add(EXMARaLDATranscriptionData.class);
        usableFor.add(EXMARaLDASegmentedTranscriptionData.class);
        usableFor.add(ELANData.class);
        usableFor.add(IMDIData.class);
//        usableFor.add(TEIData.class);
//        usableFor.add(FlextextData.class);
//        usableFor.add(ChildesMetadata.class);
//        usableFor.add(FolkerData.class);
        return usableFor;
    }

    /**
     * Gets the list of files from an Coma corpus file
     *
     * @param cd the corpus file
     * @return the list of URIs for all referenced files
     * @throws JDOMException on problems accessing information using xpath
     * @throws MalformedURLException on problems creating URIs
     * @throws URISyntaxException on problems creating URIs
     */
    private List<URI> getReferencedFiles(ComaData cd) throws JDOMException, MalformedURLException, URISyntaxException {
        ArrayList<URI> files = new ArrayList<>();
        Set<String> part1 = new HashSet<>(Arrays.asList("Transcription", "transcription", "Media",
                "media"));
        Set<String> part2 = new HashSet<>(Arrays.asList("NSLink", "nslink"));
        XPathExpression<Element> xpath = new XPathBuilder<Element>(Sets.cartesianProduct(part1,part2)
                .stream().map((l) -> "//" + String.join("/",l))
                .collect(Collectors.joining("|")), new ElementFilter()).compileWith(new JaxenXPathFactory());
        List<Element> fileRefs =
        		xpath.evaluate(cd.getJdom());
        part1 = new HashSet<>(Arrays.asList("File", "file"));
        part2 = new HashSet<>(Arrays.asList("RelPath", "Relpath", "relPath","relpath"));
        xpath = new XPathBuilder<Element>(Sets.cartesianProduct(part1,part2)
                .stream().map((l) -> "//" + String.join("/",l))
                .collect(Collectors.joining("|")), new ElementFilter()).compileWith(new JaxenXPathFactory());
        fileRefs.addAll(xpath.evaluate(cd.getJdom()));
        for (Element file : fileRefs) {
             File tmpFile = new File(new URI(cd.getParentURL() + file.getText()));
            URI fileUri = tmpFile.toURI();
            files.add(fileUri);
        }
        return files;
    }

    /**
     * Gets the list of files from an EXMARaLDA corpus file
     *
     * @param cd the corpus file
     * @return the list of URIs for all referenced files
     * @throws JDOMException on problems accessing information using xpath
     * @throws MalformedURLException on problems creating URIs
     * @throws URISyntaxException on problems creating URIs
     */
    private List<URI> getReferencedFiles(EXMARaLDATranscriptionData cd) throws JDOMException, MalformedURLException, URISyntaxException {
        ArrayList<URI> files = new ArrayList<>();
        XPathExpression<Element> xpath = new XPathBuilder<Element>("//referenced-file", new ElementFilter()).compileWith(new JaxenXPathFactory());
        List<Element> referencedFiles =
                new ArrayList<>(xpath.evaluate(cd.getJdom()));
        for (Element file : referencedFiles) {
            File tmpFile = new File(new URI(cd.getParentURL() + file.getAttribute("url").getValue()));
            URI fileUri = tmpFile.toURI();
            files.add(fileUri);
        }
        return files;
    }

    /**
     * Gets the list of files from an EXMARaLDA segmented corpus file
     *
     * @param cd the corpus file
     * @return the list of URIs for all referenced files
     * @throws JDOMException on problems accessing information using xpath
     * @throws MalformedURLException on problems creating URIs
     * @throws URISyntaxException on problems creating URIs
     */
    private List<URI> getReferencedFiles(EXMARaLDASegmentedTranscriptionData cd) throws JDOMException, MalformedURLException, URISyntaxException {
        ArrayList<URI> files = new ArrayList<>();
        XPathExpression<Element> xpath = new XPathBuilder<Element>("//referenced-file", new ElementFilter()).compileWith(new JaxenXPathFactory());
        List<Element> referencedFiles =
                new ArrayList<>(xpath.evaluate(cd.getJdom()));
        for (Element file : referencedFiles) {
            URI fileUri = new URI(cd.getParentURL() + file.getAttribute("url").getValue());
            files.add(fileUri);
        }
        return files;
    }

    /**
     * Gets the list of files from an ELAN corpus file
     *
     * @param cd the corpus file
     * @return the list of URIs for all referenced files
     * @throws JDOMException on problems accessing information using xpath
     * @throws MalformedURLException on problems creating URIs
     * @throws URISyntaxException on problems creating URIs
     */
    private List<URI> getReferencedFiles(ELANData cd) throws JDOMException, MalformedURLException, URISyntaxException {
        ArrayList<URI> files = new ArrayList<>();
        XPathExpression<Element> xpath = new XPathBuilder<Element>("//MEDIA_DESCRIPTOR", new ElementFilter()).compileWith(new JaxenXPathFactory());
        List<Element> referencedFiles =
                new ArrayList<>(xpath.evaluate(cd.getJdom()));
        for (Element file : referencedFiles) {
            logger.info(file.toString());
            if (file.getAttributeValue("RELATIVE_MEDIA_URL") != null) {
            	URI fileUri = new URI(cd.getParentURL() + file.getAttributeValue("RELATIVE_MEDIA_URL"));
                files.add(fileUri);
            }
        }
        return files;
    }

    /**
     * Gets the list of files from an IMDI corpus file
     *
     * @param cd the corpus file
     * @return the list of URIs for all referenced files
     * @throws JDOMException on problems accessing information using xpath
     * @throws MalformedURLException on problems creating URIs
     * @throws URISyntaxException on problems creating URIs
     */
    private List<URI> getReferencedFiles(IMDIData cd) throws JDOMException, MalformedURLException, URISyntaxException {
        ArrayList<URI> files = new ArrayList<>();
        XPathExpression<Element> xpath = new XPathBuilder<Element>("//MEDIA_DESCRIPTOR", new ElementFilter()).compileWith(new JaxenXPathFactory());
        List<Element> referencedFiles = xpath.evaluate(cd.getJdom());
        for (Element file : referencedFiles) {
            URI fileUri = new URI(cd.getParentURL() + file.getAttribute("RELATIVE_MEDIA_URL").getValue());
            files.add(fileUri);
        }
        return files;
    }

    @Override
    public String getDescription() {
        return "Extracts all files referenced in corpus files";
    }

    /**
     * Returns all files referenced in the corpus after reading it
     *
     * @return the list of all referenced files
     */
    public List<URI> getFileList() {
        return corpusFiles;
    }

}
