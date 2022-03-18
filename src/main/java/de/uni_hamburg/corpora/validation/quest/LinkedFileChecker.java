package de.uni_hamburg.corpora.validation.quest;
import com.google.common.collect.Sets;
import de.uni_hamburg.corpora.validation.Checker;
import de.uni_hamburg.corpora.*;
import org.exmaralda.partitureditor.fsm.FSMException;
import org.exmaralda.partitureditor.jexmaralda.JexmaraldaException;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.xpath.XPath;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpressionException;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Extracts a list of referenced files from a corpus
 *
 * @author bba1792, Dr. Herbert Lange
 */
public class LinkedFileChecker extends Checker implements CorpusFunction {

    Logger logger = Logger.getLogger(this.getClass().getName());
    List<URI> corpusFiles = new ArrayList<>();
    public LinkedFileChecker(Properties properties) {
        this(false,properties);
    }
    public LinkedFileChecker(boolean hasfixingoption, Properties properties) {
        super(hasfixingoption, properties);
    }

//    public List<FileInfo> getReferencedFiles(Report report, CorpusData cd) {
//        List<FileInfo> files = new ArrayList<>();
//        if (cd instanceof ComaData) {
//            try {
//                List<Element> transcriptionFiles =
//                        XPath.newInstance("//Transcription | //transcription | //Media | //media")
//                                .selectNodes(((ComaData) cd).getJdom());
//                for (Element fileElement : transcriptionFiles) {
//                    // Get filename
//                    String fileName = //fileElement.getChildText("Filename");
//                            ((Element) XPath.newInstance("//Filename | //filename").selectSingleNode(fileElement))
//                                    .getText();
//                    // Get uri
//                    URI fileUri =
//                            new File(((Element) XPath.newInstance("//NSLink | //nslink").selectSingleNode(fileElement))
//                                    .getText()).toURI();
//                    // Try to get file type
//                    String fileType = "unknown";
//                    if (fileName.toLowerCase().endsWith("exs") || fileName.toLowerCase().endsWith("exb")) {
//                        fileType = "text/xml";
//                    }
//                    else {
//                        report.addWarning(getFunction(),"Unknown file type: "+ fileName);
//                    }
//                    files.add(new FileInfo(fileName, fileUri, fileType));
//                }
//                List<Element> mediaFiles = XPath.newInstance("//File | //file").selectNodes(((ComaData) cd).getJdom());
//                for (Element mediaElement : mediaFiles) {
//                    // Get filename
//                    String fileName =
//                            ((Element) XPath.newInstance("//Filename | //filename").selectSingleNode(mediaElement))
//                                    .getText();
//                    // Get uri
//                    URI fileUri = new File(((Element) XPath.newInstance("//RelPath | //relPath | // relpath")
//                            .selectSingleNode(mediaElement)).getText()).toURI();
//                    // Try to get file type
//                    String fileType =
//                            ((Element) XPath.newInstance("//MineType | //mimeType | //mimetype").selectSingleNode(mediaElement))
//                                    .getText();
//                    /*Element fileName = (Element) XPath.newInstance("//Filename | //filename").selectSingleNode(mediaElement);
//                    Element fileType =
//                            (Element) XPath.newInstance("//Mimetype | //mimetype").selectSingleNode(mediaElement);*/
//                    files.add(new FileInfo(fileName, fileUri, fileType));
//                }
//
//            }
//            catch (JDOMException e) {
//                report.addCritical(getFunction(),ReportItem.newParamMap(
//                        new String[] {"function", "exception", "description"},
//                        new Object[] {getFunction(),e,"Exception when trying to extract files from: " + cd.getFilename()}));
//            }
//        }
//        else if (cd instanceof EXMARaLDACorpusData) {
//            try {
//                List<Element> referencedFiles = XPath.newInstance("//referenced-file").selectNodes(((EXMARaLDACorpusData) cd).getJdom());
//                for (Element file : referencedFiles) {
//                    File tmpFile = new File(new URL(cd.getParentURL() + file.getAttribute("url").getValue()).toURI());
//                    String fileName = tmpFile.getName();
//                    URI fileUri = tmpFile.toURI();
//                    String fileType = "unknown";
//                    try {
//                        fileType = Magic.getMagicMatch(tmpFile, true).getMimeType();
//                    }
//                    catch (MagicMatchNotFoundException | MagicParseException | MagicException e) {
//                        report.addCritical(getFunction(), ReportItem.newParamMap(
//                                new String[]{"function", "exception", "description"},
//                                new Object[]{getFunction(),e,
//                                        "Exception when trying to extract file type of file: " + tmpFile}));
//                    }
//                    files.add(new FileInfo(fileName,fileUri,fileType));
//                }
//            }
//            catch (JDOMException | MalformedURLException | URISyntaxException e) {
//                report.addCritical(getFunction(), ReportItem.newParamMap(
//                        new String[]{"function", "exception", "description"},
//                        new Object[]{getFunction(),e,
//                                "Exception when trying to extract files from: " + cd.getFilename()}));
//            }
//        }
//        else {
//            report.addCritical(getFunction(),
//                    ReportItem.newParamMap(
//                            new String[] {"function", "description"},
//                            new Object[] {getFunction(),
//                                    "No way to get file referenced from " + cd.getClass().toString()}));
//        }
//        logger.info("Files: " + files.size());
//        for (FileInfo f : files) {
//            logger.info(f.getName() + ":" + f.getUri());
//        }
//        return files;
//    }

    @Override
    public Report function(CorpusData cd, Boolean fix) throws NoSuchAlgorithmException, ClassNotFoundException, FSMException, URISyntaxException, SAXException, IOException, ParserConfigurationException, JexmaraldaException, TransformerException, XPathExpressionException, JDOMException {
        Report report = new Report();
        List<URI> refFiles = new ArrayList<>();
        try {
            if (cd instanceof ComaData)
                refFiles.addAll(getReferencedFiles(report, (ComaData) cd));
            else if (cd instanceof EXMARaLDACorpusData)
                refFiles.addAll(getReferencedFiles(report, (EXMARaLDACorpusData) cd));
            else if (cd instanceof ELANData)
                refFiles.addAll(getReferencedFiles(report, (ELANData) cd));
            else if (cd instanceof IMDIData)
                refFiles.addAll(getReferencedFiles(report, (IMDIData) cd));
        }
        catch (JDOMException | MalformedURLException | URISyntaxException e) {
            report.addCritical(getFunction(), ReportItem.newParamMap(
                    new String[]{"function", "exception", "description"},
                    new Object[]{getFunction(),e,
                            "Exception when trying to extract files from: " + cd.getFilename()}));
        }
        if (refFiles.isEmpty())
            report.addWarning(getFunction(),ReportItem.newParamMap(new String[]{"function","description"},
                    new Object[]{getFunction(),"No linked files found in " + cd.getFilename()}));
        corpusFiles.addAll(refFiles);
        return report;
    }

    @Override
    public Report function(Corpus c, Boolean fix) throws NoSuchAlgorithmException, ClassNotFoundException, FSMException, URISyntaxException, SAXException, IOException, ParserConfigurationException, JexmaraldaException, TransformerException, XPathExpressionException, JDOMException {
        Report report = new Report();
        for (CorpusData cd : c.getCorpusData())
            report.merge(function(cd, fix));
        return report;
    }

    @Override
    public Collection<Class<? extends CorpusData>> getIsUsableFor() {
        Set<Class<? extends CorpusData>> usableFor = new HashSet<>();
        usableFor.add(ComaData.class);
        usableFor.add(EXMARaLDACorpusData.class);
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
     * @param report the report to store potential problems
     * @param cd the corpus file
     * @return the list of URIs for all referenced files
     * @throws JDOMException on problems accessing information using xpath
     * @throws MalformedURLException on problems creating URIs
     * @throws URISyntaxException on problems creating URIs
     */
    private List<URI> getReferencedFiles(Report report, ComaData cd) throws JDOMException, MalformedURLException, URISyntaxException {
        ArrayList<URI> files = new ArrayList<>();
        Set<String> part1 = new HashSet<>(Arrays.asList(new String[] {"Transcription", "transcription", "Media",
                "media"}));
        Set<String> part2 = new HashSet<>(Arrays.asList(new String[] {"NSLink", "nslink"}));
        List<Element> fileRefs =
                    XPath.newInstance(Sets.cartesianProduct(part1,part2)
                .stream().map((l) -> "//" + String.join("/",l))
                .collect(Collectors.joining("|"))).selectNodes(cd.getJdom());
        part1 = new HashSet<>(Arrays.asList(new String[]{"File", "file"}));
        part2 = new HashSet<>(Arrays.asList(new String[]{"RelPath", "Relpath", "relPath","relpath"}));
        fileRefs.addAll(XPath.newInstance(Sets.cartesianProduct(part1,part2)
                .stream().map((l) -> "//" + String.join("/",l))
                .collect(Collectors.joining("|"))).selectNodes(cd.getJdom()));
        for (Element file : fileRefs) {
             File tmpFile = new File(new URL(cd.getParentURL() +
                    file.getText()).toURI());
            URI fileUri = tmpFile.toURI();
            files.add(fileUri);
        }
        return files;
    }

    /**
     * Gets the list of files from an EXMARaLDA corpus file
     * 
     * @param report the report to store potential problems
     * @param cd the corpus file
     * @return the list of URIs for all referenced files
     * @throws JDOMException on problems accessing information using xpath
     * @throws MalformedURLException on problems creating URIs
     * @throws URISyntaxException on problems creating URIs
     */
    private List<URI> getReferencedFiles(Report report, EXMARaLDACorpusData cd) throws JDOMException, MalformedURLException, URISyntaxException {
        ArrayList<URI> files = new ArrayList<>();
        List<Element> referencedFiles = XPath.newInstance("//referenced-file").selectNodes(cd.getJdom());
        for (Element file : referencedFiles) {
            File tmpFile = new File(new URL(cd.getParentURL() +
                    file.getAttribute("url").getValue()).toURI());
            URI fileUri = tmpFile.toURI();
            files.add(fileUri);
        }
        return files;
    }

    /**
     * Gets the list of files from an ELAN corpus file
     *
     * @param report the report to store potential problems
     * @param cd the corpus file
     * @return the list of URIs for all referenced files
     * @throws JDOMException on problems accessing information using xpath
     * @throws MalformedURLException on problems creating URIs
     * @throws URISyntaxException on problems creating URIs
     */
    private List<URI> getReferencedFiles(Report report, ELANData cd) throws JDOMException, MalformedURLException, URISyntaxException {
        ArrayList<URI> files = new ArrayList<>();
        List<Element> referencedFiles = XPath.newInstance("//MEDIA_DESCRIPTOR").selectNodes(cd.getJdom());
        for (Element file : referencedFiles) {
            File tmpFile = new File(new URL(cd.getParentURL() +
                    file.getAttribute("RELATIVE_MEDIA_URL").getValue()).toURI());
            URI fileUri = tmpFile.toURI();
            files.add(fileUri);
        }
        return files;
    }

    /**
     * Gets the list of files from an IMDI corpus file
     *
     * @param report the report to store potential problems
     * @param cd the corpus file
     * @return the list of URIs for all referenced files
     * @throws JDOMException on problems accessing information using xpath
     * @throws MalformedURLException on problems creating URIs
     * @throws URISyntaxException on problems creating URIs
     */
    private List<URI> getReferencedFiles(Report report, IMDIData cd) throws JDOMException, MalformedURLException, URISyntaxException {
        ArrayList<URI> files = new ArrayList<>();
        List<Element> referencedFiles = XPath.newInstance("//MEDIA_DESCRIPTOR").selectNodes(cd.getJdom());
        for (Element file : referencedFiles) {
            File tmpFile = new File(new URL(cd.getParentURL() +
                    file.getAttribute("RELATIVE_MEDIA_URL").getValue()).toURI());
            URI fileUri = tmpFile.toURI();
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
