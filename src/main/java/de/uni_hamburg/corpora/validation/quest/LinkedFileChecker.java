package de.uni_hamburg.corpora.validation.quest;
import de.uni_hamburg.corpora.validation.Checker;
import de.uni_hamburg.corpora.*;
import net.sf.jmimemagic.Magic;
import net.sf.jmimemagic.MagicException;
import net.sf.jmimemagic.MagicMatchNotFoundException;
import net.sf.jmimemagic.MagicParseException;
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
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Checks files linked in a corpus to adhere to some criteria
 */
public class LinkedFileChecker extends Checker implements CorpusFunction {

    public static class FileInfo {
        String name ;
        URI uri ;
        String type ;

        public FileInfo(String name, URI uri, String type) {
            this.name = name;
            this.uri = uri;
            this.type = type;
        }

        public String getName() {
            return name;
        }

        public URI getUri() {
            return uri;
        }

        public String getType() {
            return type;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            FileInfo fileInfo = (FileInfo) o;
            return name.equals(fileInfo.name) && uri.equals(fileInfo.uri) ; // && type.equals(fileInfo.type);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, uri, type);
        }
    }
    Logger logger = Logger.getLogger(this.getClass().getName());
    List<FileInfo> corpusFiles;
    public LinkedFileChecker(Properties properties) {
        this(false,properties);
    }
    public LinkedFileChecker(boolean hasfixingoption, Properties properties) {
        super(hasfixingoption, properties);
    }

    public List<FileInfo> getReferencedFiles(Report report, CorpusData cd) {
        List<FileInfo> files = new ArrayList<>();
        if (cd instanceof ComaData) {
            try {
                List<Element> transcriptionFiles =
                        XPath.newInstance("//Transcription | //transcription | //Media | //media")
                                .selectNodes(((ComaData) cd).getJdom());
                for (Element fileElement : transcriptionFiles) {
                    // Get filename
                    String fileName = //fileElement.getChildText("Filename");
                            ((Element) XPath.newInstance("//Filename | //filename").selectSingleNode(fileElement))
                                    .getText();
                    // Get uri
                    URI fileUri =
                            new File(((Element) XPath.newInstance("//NSLink | //nslink").selectSingleNode(fileElement))
                                    .getText()).toURI();
                    // Try to get file type
                    String fileType = "unknown";
                    if (fileName.toLowerCase().endsWith("exs") || fileName.toLowerCase().endsWith("exb")) {
                        fileType = "text/xml";
                    }
                    else {
                        report.addWarning(getFunction(),"Unknown file type: "+ fileName);
                    }
                    files.add(new FileInfo(fileName, fileUri, fileType));
                }
                List<Element> mediaFiles = XPath.newInstance("//File | //file").selectNodes(((ComaData) cd).getJdom());
                for (Element mediaElement : mediaFiles) {
                    // Get filename
                    String fileName =
                            ((Element) XPath.newInstance("//Filename | //filename").selectSingleNode(mediaElement))
                                    .getText();
                    // Get uri
                    URI fileUri = new File(((Element) XPath.newInstance("//RelPath | //relPath | // relpath")
                            .selectSingleNode(mediaElement)).getText()).toURI();
                    // Try to get file type
                    String fileType =
                            ((Element) XPath.newInstance("//MineType | //mimeType | //mimetype").selectSingleNode(mediaElement))
                                    .getText();
                    /*Element fileName = (Element) XPath.newInstance("//Filename | //filename").selectSingleNode(mediaElement);
                    Element fileType =
                            (Element) XPath.newInstance("//Mimetype | //mimetype").selectSingleNode(mediaElement);*/
                    files.add(new FileInfo(fileName, fileUri, fileType));
                }

            }
            catch (JDOMException e) {
                report.addCritical(getFunction(),ReportItem.newParamMap(
                        new String[] {"function", "exception", "description"},
                        new Object[] {getFunction(),e,"Exception when trying to extract files from: " + cd.getFilename()}));
            }
        }
        else if (cd instanceof EXMARaLDACorpusData) {
            try {
                List<Element> referencedFiles = XPath.newInstance("//referenced-file").selectNodes(((EXMARaLDACorpusData) cd).getJdom());
                for (Element file : referencedFiles) {
                    File tmpFile = new File(new URL(cd.getParentURL() + file.getAttribute("url").getValue()).toURI());
                    String fileName = tmpFile.getName();
                    URI fileUri = tmpFile.toURI();
                    String fileType = "unknown";
                    try {
                        fileType = Magic.getMagicMatch(tmpFile, true).getMimeType();
                    }
                    catch (MagicMatchNotFoundException | MagicParseException | MagicException e) {
                        report.addCritical(getFunction(), ReportItem.newParamMap(
                                new String[]{"function", "exception", "description"},
                                new Object[]{getFunction(),e,
                                        "Exception when trying to extract file type of file: " + tmpFile}));
                    }
                    files.add(new FileInfo(fileName,fileUri,fileType));
                }
            }
            catch (JDOMException | MalformedURLException | URISyntaxException e) {
                report.addCritical(getFunction(), ReportItem.newParamMap(
                        new String[]{"function", "exception", "description"},
                        new Object[]{getFunction(),e,
                                "Exception when trying to extract files from: " + cd.getFilename()}));
            }
        }
        else {
            report.addCritical(getFunction(),
                    ReportItem.newParamMap(
                            new String[] {"function", "description"},
                            new Object[] {getFunction(),
                                    "No way to get file referenced from " + cd.getClass().toString()}));
        }
        logger.info("Files: " + files.size());
        for (FileInfo f : files) {
            logger.info(f.getName() + ":" + f.getUri());
        }
        return files;
    }

    @Override
    public Report function(CorpusData cd, Boolean fix) throws NoSuchAlgorithmException, ClassNotFoundException, FSMException, URISyntaxException, SAXException, IOException, ParserConfigurationException, JexmaraldaException, TransformerException, XPathExpressionException, JDOMException {
        Report report = new Report();
        List<FileInfo> refFiles = getReferencedFiles(report,cd);
        for(File f : refFiles.stream().map((fn) -> new File(fn.getUri())).collect(Collectors.toList())) {
            if (!f.exists()) {
                report.addWarning(getFunction(),ReportItem.newParamMap(
                        new String[]{"function", "filename", "description", "howtoFix"},
                        new String[]{getFunction(), cd.getFilename(), "File does not exist: " + f.toURI(),
                                "Add missing file or remove reference"}));
            }
        }
        corpusFiles.removeAll(refFiles);
        return report;
    }

    @Override
    public Report function(Corpus c, Boolean fix) throws NoSuchAlgorithmException, ClassNotFoundException, FSMException, URISyntaxException, SAXException, IOException, ParserConfigurationException, JexmaraldaException, TransformerException, XPathExpressionException, JDOMException {
        Report report = new Report();
        corpusFiles = listFilesInPath(report,new File(c.getBaseDirectory().toURI()));
        for (CorpusData cd : c.getCorpusData()) {
            report.merge(function(cd, fix));
        }
        if (!corpusFiles.isEmpty()) {
            report.addWarning(getFunction(),ReportItem.newParamMap(
                    new String[]{"function","filename","description","howtoFix"},
                    new Object[]{getFunction(),c.getBaseDirectory().toString(),
                            "Unreferenced files found in folder:" + corpusFiles.stream().map(FileInfo::getName
    ).collect(Collectors.joining(","))}));
        }
        return report;
    }

    @Override
    public Collection<Class<? extends CorpusData>> getIsUsableFor() {
        Set<Class<? extends CorpusData>> usableFor = new HashSet<>();
        usableFor.add(ELANData.class);
        usableFor.add(EXMARaLDACorpusData.class);
        usableFor.add(ComaData.class);
        usableFor.add(TEIData.class);
        usableFor.add(FlextextData.class);
        usableFor.add(IMDIData.class);
        usableFor.add(ChildesMetadata.class);
        usableFor.add(FolkerData.class);
        return usableFor;
    }

    @Override
    public String getDescription() {
        return "Checks all non-corpus-data files referenced in a corpus to follow some best practice";
    }

    List<FileInfo> listFilesInPath(Report report, File path) {
        List<FileInfo> fileList = new ArrayList<>();
        try {
            DirectoryStream<Path> stream = Files.newDirectoryStream(Paths.get(path.toURI()));
            for (Path p : stream) {
                // Skip files and directories starting with .
                if (!p.toFile().getName().startsWith(".")) {
                    // Descend into directory
                    if (Files.isDirectory(p)) {
                        fileList.addAll(listFilesInPath(report, p.toFile()));
                    }
                    // Just add the file
                    else {
                        String fileType = "unknown";
                        fileList.add(new FileInfo(p.toFile().getName(), p.toFile().toURI(), fileType));
                    }
                }
            }
        } catch (IOException e) {
           report.addCritical(getFunction(), ReportItem.newParamMap(
                   new String[]{"function", "description", "exception"},
                   new Object[]{getFunction(),"Exception when listing all files in " + path,e}));
        }
        return fileList;
    }
}
