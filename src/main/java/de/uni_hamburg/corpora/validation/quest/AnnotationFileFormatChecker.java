package de.uni_hamburg.corpora.validation.quest;

import de.uni_hamburg.corpora.*;
import de.uni_hamburg.corpora.utilities.quest.FileTools;
import de.uni_hamburg.corpora.validation.Checker;
import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlElementWrapper;
import jakarta.xml.bind.annotation.XmlRootElement;

import org.apache.commons.io.FilenameUtils;
import org.exmaralda.partitureditor.fsm.FSMException;
import org.exmaralda.partitureditor.jexmaralda.JexmaraldaException;
import org.jdom2.JDOMException;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpressionException;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Checker that checks for common annotation file formats and which can give feedback about support for the file
 * format for several archives based on the data provided by CLARIN SIS (https://standards.clarin.eu/sis/)
 *
 * Last updated
 * @author Herbert Lange
 * @version 20240322
 */
public class AnnotationFileFormatChecker extends Checker implements CorpusFunction {

    // The following data structures are used to de-serialize the XML file specifying the supported formats based on
    // Clarin SIS
    // Data structure representing a centre
    static class CenterInfo {
        @XmlElement
        String id;
        @XmlElement
        String name;
        @XmlElement
        String url;
        @XmlElement
        String domain;
        @XmlElement
        String level;

        @Override
        public String toString() {
            return "CenterInfo{" +
                    "id='" + id + '\'' +
                    ", name='" + name + '\'' +
                    ", url='" + url + '\'' +
                    ", domain='" + domain + '\'' +
                    ", level='" + level + '\'' +
                    "}\n";
        }
    }

    // Data structure representing a file format
    static class FormatInfo {
        @XmlElement
        String name;
        @XmlElementWrapper(name="fileExts")
        @XmlElement(name="fileExt")
        List<String> fileExt;
        @XmlElementWrapper(name="mimeTypes")
        @XmlElement(name="mimeType")
        List<String> mimeType;

        @Override
        public String toString() {
            return "FormatInfo{" +
                    "name='" + name + '\'' +
                    ", fileExt=" + fileExt +
                    ", mimeType=" + mimeType +
                    "}\n";
        }
    }

    // Data structure representing a Format specific to a centre
    static class Format {
        @XmlElement
        FormatInfo formatInfo ;
        @XmlElementWrapper(name = "centreInfo")
        @XmlElement(name="centre")
        List<CenterInfo> centerInfo;

        @Override
        public String toString() {
            return "Format{" +
                    "formatInfo=" + formatInfo +
                    ", centerInfos=" + centerInfo +
                    "}\n";
        }
    }

    // Data type for the list of all supported formats
    @XmlRootElement
    static class Formats {
        @XmlElement(name="format")
        List<Format> formats;

        @Override
        public String toString() {
            return "Formats{" +
                    "formats=" + formats +
                    '}';
        }
    }

    // The default logger
    Logger logger = Logger.getLogger(this.toString());

    // List of all known formats
    List<Format> formats = new ArrayList<>();

    // The center we are interested in
    private String intendedCenter = "";

    public AnnotationFileFormatChecker(Properties properties) throws JAXBException {
        super(false, properties);
        // Deserialize the file formats
        JAXBContext ctx = JAXBContext.newInstance(Formats.class);
        formats.addAll(((Formats) ctx.createUnmarshaller().unmarshal(this.getClass().getClassLoader()
                .getResourceAsStream("sis-recommendations.xml"))).formats);
        // Update the intended center, if any
        if (properties.containsKey("center")) {
            intendedCenter = properties.getProperty("center");
        }
    }

    @Override
    public String getDescription() {
        return "Checks annotation files in a directory if they follow recommendations for archives based on " +
                "information provided by CLARIN SIS.";
    }

    @Override
    public Report function(CorpusData cd, Boolean fix) throws NoSuchAlgorithmException, ClassNotFoundException, FSMException, URISyntaxException, SAXException, IOException, ParserConfigurationException, JexmaraldaException, TransformerException, XPathExpressionException, JDOMException {
        // New report
        Report report = new Report();
        // We don't do anything here
        report.addCritical(getFunction(),"Checker does not have a checker function for " +
                "individual corpus files");
        return report;
    }

    @Override
    public Report function(Corpus corpus, Boolean fix) throws NoSuchAlgorithmException, ClassNotFoundException,
            FSMException, URISyntaxException, SAXException, IOException, ParserConfigurationException, JexmaraldaException, TransformerException, XPathExpressionException, JDOMException {
        // New report
        Report report = new Report();
        // The corpus base directory
        URL directory = corpus.getBaseDirectory();
        // All files in the corpus directory
        Set<URI> corpusFiles = FileTools.listFiles(Paths.get(directory.toURI()));
        logger.info(corpusFiles.stream().map(URI::toString).collect(Collectors.joining(", ")));
        // Keep track if we ever encountered acceptable file formats
        boolean acceptableFiles = false;
        // Check all the files
        for (URI fileUri : corpusFiles) {
            File fileInfo = new File(fileUri);
            // Ignore directories
            if (!fileInfo.isDirectory()) {
                String ext = "." + FilenameUtils.getExtension(fileInfo.getName());
                for (Format format: formats) {
                    // Find the correct format
                    if (format.formatInfo.fileExt.stream().map((f) -> f.equalsIgnoreCase(ext))
                            .reduce(Boolean::logicalOr).orElse(false))  {
                        // If we know the center we are aiming for
                        if (!intendedCenter.isEmpty()) {
                            List<String> levels =
                            format.centerInfo.stream().filter((c) -> c.name.equalsIgnoreCase(intendedCenter))
                                    .map((c) -> c.level.toLowerCase()).collect(Collectors.toList());
                            logger.info("Format: " + format.formatInfo.name + " - Level: " + levels);
                            if (levels.contains("recommended")) {
                                report.addCorrect(getFunction(),
                                        ReportItem.newParamMap(new ReportItem.Field[]{ReportItem.Field.Function, ReportItem.Field.Filename, ReportItem.Field.Description},
                                                new Object[]{getFunction(), fileUri.toString(), // new File(fileUri).getName(),
                                                        "File format " + format.formatInfo.name + " recommended by " +
                                                                "the archive " + intendedCenter
                                                }));
                                acceptableFiles = true;
                            }
                            else if (levels.contains("acceptable")) {
                                report.addCorrect(getFunction(),
                                        ReportItem.newParamMap(new ReportItem.Field[]{ReportItem.Field.Function, ReportItem.Field.Filename, ReportItem.Field.Description},
                                                new Object[]{getFunction(), fileUri.toString(), // new File(fileUri).getName(),
                                                        "File format " + format.formatInfo.name + " is acceptable for" +
                                                                " the archive " + intendedCenter
                                                }));
                                acceptableFiles = true;
                            }
                            else if (levels.contains("deprecated")){
                                report.addCorrect(getFunction(),
                                        ReportItem.newParamMap(new ReportItem.Field[]{ReportItem.Field.Function, ReportItem.Field.Filename, ReportItem.Field.Description},
                                                new Object[]{getFunction(), fileUri.toString(), // new File(fileUri).getName(),
                                                        "File format " + format.formatInfo.name + " is " +
                                                                "considered deprecated for the archive " + intendedCenter
                                                }));
                                acceptableFiles = true;
                            }
                            else {
                                report.addCritical(getFunction(),
                                        ReportItem.newParamMap(new ReportItem.Field[]{ReportItem.Field.Function, ReportItem.Field.Filename, ReportItem.Field.Description},
                                                new Object[]{getFunction(), fileUri.toString(), // new File(fileUri).getName(),
                                                        "File format " + format.formatInfo.name + " is not recognized" +
                                                                " by the archive " + intendedCenter
                                                }));
                            }
                        }
                        else {
                            // Try to find centres where the format is recommended
                            if (format.centerInfo.stream().map((c) -> c.level.equalsIgnoreCase("recommended"))
                                    .reduce(Boolean::logicalOr).orElse(false)) {
                                report.addCorrect(getFunction(),
                                        ReportItem.newParamMap(new ReportItem.Field[]{ReportItem.Field.Function, ReportItem.Field.Filename, ReportItem.Field.Description},
                                                new Object[]{getFunction(), fileUri.toString(), // new File(fileUri).getName(),
                                                        "File format " + format.formatInfo.name + " recommended by " +
                                                                "the archives " +
                                                                format.centerInfo.stream().filter((c) ->
                                                                                c.level.equalsIgnoreCase("recommended"))
                                                                        .map((c) -> c.name)
                                                                        .collect(Collectors.joining(", "))
                                                }));
                                acceptableFiles = true;
                            }
                            // Otherwise, try to find at least to find where it is acceptable
                            else if (format.centerInfo.stream().map((c) -> c.level.equalsIgnoreCase("acceptable"))
                                    .reduce(Boolean::logicalOr).orElse(false)) {
                                report.addCorrect(getFunction(),
                                        ReportItem.newParamMap(new ReportItem.Field[]{ReportItem.Field.Function, ReportItem.Field.Filename, ReportItem.Field.Description},
                                                new Object[]{getFunction(), fileUri.toString(), // new File(fileUri).getName(),
                                                        "File format " + format.formatInfo.name + " is acceptable for" +
                                                                " the archives " +
                                                                format.centerInfo.stream().filter((c) ->
                                                                                c.level.equalsIgnoreCase("acceptable"))
                                                                        .map((c) -> c.name)
                                                                        .collect(Collectors.joining(", "))
                                                }));
                                acceptableFiles = true;
                            }
                            // Finally, check centers that declare the format as deprecated
                            else if (format.centerInfo.stream().map((c) -> c.level.equalsIgnoreCase("deprecated"))
                                    .reduce(Boolean::logicalOr).orElse(false)) {
                                report.addWarning(getFunction(),
                                        ReportItem.newParamMap(new ReportItem.Field[]{ReportItem.Field.Function, ReportItem.Field.Filename, ReportItem.Field.Description},
                                                new Object[]{getFunction(), fileUri.toString(), // new File(fileUri).getName(),
                                                        "File format " + format.formatInfo.name + " is considered " +
                                                                "deprecated at the archives " +
                                                                format.centerInfo.stream().filter((c) ->
                                                                                c.level.equalsIgnoreCase("deprecated"))
                                                                        .map((c) -> c.name)
                                                                        .collect(Collectors.joining(", "))
                                                }));
                                acceptableFiles = true;
                            }
                            // No center found at all
                            else {
                                report.addCritical(getFunction(),
                                        ReportItem.newParamMap(new ReportItem.Field[]{ReportItem.Field.Function, ReportItem.Field.Filename, ReportItem.Field.Description},
                                                new Object[]{getFunction(), new File(fileUri).getName(),
                                                        "File format " + format.formatInfo.name + "is not recognized " +
                                                                "for any archive"}));
                            }
                        }
                    }
                }
            }
        }
        if (!acceptableFiles) {
            report.addCritical(getFunction(),
                    ReportItem.newParamMap(new ReportItem.Field[]{ReportItem.Field.Function, ReportItem.Field.Description},
                            new Object[]{getFunction(),"No acceptable files for any archive found in corpus"}));
        }
        return report;
    }

    @Override
    public Collection<Class<? extends CorpusData>> getIsUsableFor() {
        // Does not really match any corpus data
        return new ArrayList<>();
    }

    @Override
    public Map<String, String> getParameters() {
        Map<String,String> params = super.getParameters();
        params.put("center", "Name of the intended center");
        return params;
    }
}
