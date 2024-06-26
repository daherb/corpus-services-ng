/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package de.idsmannheim.lza.conversion;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import de.uni_hamburg.corpora.Corpus;
import de.uni_hamburg.corpora.CorpusData;
import de.uni_hamburg.corpora.conversion.Converter;

import de.uni_hamburg.corpora.CorpusFunction;
import de.uni_hamburg.corpora.Report;
import de.idsmannheim.lza.utilities.publication.mapper.MapFile;
import de.idsmannheim.lza.utilities.publication.mapper.MapRecord;
import de.idsmannheim.lza.utilities.publication.mapper.MapRootRecord;
import gov.loc.repository.bagit.creator.BagCreator;
import gov.loc.repository.bagit.domain.Bag;
import gov.loc.repository.bagit.domain.Manifest;
import gov.loc.repository.bagit.hash.Hasher;
import gov.loc.repository.bagit.hash.StandardSupportedAlgorithms;
import gov.loc.repository.bagit.writer.ManifestWriter;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpressionException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.FileFileFilter;
import org.apache.commons.io.filefilter.NotFileFilter;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.exmaralda.partitureditor.fsm.FSMException;
import org.exmaralda.partitureditor.jexmaralda.JexmaraldaException;
import org.jdom2.JDOMException;
import org.xml.sax.SAXException;

/**
 * Creates or updates a BagIt in the following way:
 * - records are defined by the name of a metadata file
 * - all files having the metadata filename as a prefix are grouped together
 * - there is exactly one metadata file which is not the prefix of any file
 * - this file will be used as the top-level metadata
 * 
 * An example is here, where root.cmdi is the top-level metadata and the files
 * will be grouped into three records aaa, aab and zzz. The record aaa contains the
 * metadata file aaa.cmdi and the file aaa.abc, the record aab contains the metadata
 * file aab.cmdi and all files starting with aab, i.e. aab.abc and aab.xzy, and the
 * record zzz contains the metadata file zzz.cmdi and the files zzz1.zyx and zzz42.xyz
 * ```
 * data/
 * |-Metadata/
 * | |-root.cmdi
 * | |-aaa.cmdi
 * | |-aab.cmdi
 * | \-zzz.cmdi
 * \-Content
 *   |-aaa.abc
 *   |-aab.abc
 *   |-aab.xyz
 *   |-zzz1.xyz
 *   \-zzz42.xyz
 * ```
 * @author Herbert Lange <lange@ids-mannheim.de>
 *
 * Last updated
 * @author Herbert Lange
 * @version 20240322
 */
public class FolderToBasicSIP extends Converter implements CorpusFunction {

    public FolderToBasicSIP(Properties props) {
        super(props);
    }
    
    @Override
    public Report function(Corpus c) throws Exception, NoSuchAlgorithmException, ClassNotFoundException, FSMException, URISyntaxException, SAXException, IOException, ParserConfigurationException, JexmaraldaException, TransformerException, XPathExpressionException, JDOMException {
        Report report = new Report();
        Path path = Path.of(c.getBaseDirectory().toURI());
        Path metadataPath;
        if (Path.of(path.toString(),"data","Metadata").toFile().exists()) {
            metadataPath = Path.of(path.toString(),"data","Metadata");
        }
        else if (Path.of(path.toString(),"Metadata").toFile().exists()) {
            metadataPath = Path.of(path.toString(),"Metadata");
        }
        else {
            metadataPath = path;
        }
        Path contentPath;
        if (Path.of(path.toString(),"data","Content").toFile().exists()) {
            contentPath = Path.of(path.toString(),"data","Content");
        }
        else if (Path.of(path.toString(),"Content").toFile().exists()) {
            contentPath = Path.of(path.toString(),"Content");
        }
        else {
            contentPath = path;
        }
        Set<File> metadataFiles = listMetadataFiles(path);
        Set<File> contentFiles = listContentFiles(path);
        MapRootRecord record;
        if (props.containsKey("record-map-file")) {
            ObjectMapper om = new ObjectMapper();
            om.findAndRegisterModules();
            record = om.readValue(new File(props.getProperty("record-map-file")), MapRootRecord.class);
        }
        else {
            try {
                LOG.info("Create record map");
                String title;
                if (props.containsKey("root-title")) {
                    title = props.getProperty("root-title");
                }
                else {
                    title = c.getCorpusName();
                }
                record = bundleFiles(metadataPath, contentPath, title, metadataFiles,contentFiles);
            }
            catch (IOException e) {
                report.addException(getFunction(), e, "Exception when creating record map");
                return report;
            }
        }
        LOG.info("Check and/or create output");
        // Copy files to output
        // First find output path
        Path outputPath;
        if (props.containsKey("output-path")) {
            outputPath = Path.of(props.getProperty("output-path"));
        }
        else {
            outputPath = Path.of(path.toString(), "..", "output").toAbsolutePath();
        }
        // Try to create it if missing
        if (!outputPath.toFile().exists())
            if (!outputPath.toFile().mkdir()) {
                report.addCritical("Error creating output");
                return report;
            }
        // Copy all files
        LOG.info("Copy files to output");
        for (File file : FileUtils.listFiles(path.toFile(), FileFileFilter.FILE, DirectoryFileFilter.DIRECTORY)) {
            if (props.getProperty("create-hard-links", "False").equalsIgnoreCase("true")) {
                Path link = Path.of(file.toString().replace(path.toString(), outputPath.toString())).normalize().toAbsolutePath();
                Files.createDirectories(link.getParent());
                Files.createLink(link, file.toPath().toAbsolutePath());
            }
            else {
                FileUtils.copyFile(file, Path.of(file.toString().replace(path.toString(), outputPath.toString())).toAbsolutePath().normalize().toFile());
            }
        }
        // Create a bag inplace
        LOG.info("Create bag");
        Bag bag = BagCreator.bagInPlace(outputPath, Collections.singleton(StandardSupportedAlgorithms.SHA512), false);
        // Write record map and add it to tag manifest
        LOG.info("Write record map");
        XmlMapper mapper = new XmlMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.findAndRegisterModules();
        Path recordmapPath = Path.of(outputPath.toString(), "recordmap.xml");
        mapper.writeValue(recordmapPath.toFile(), record);
        // Update bag
        LOG.info("Update bag");
        for (Manifest tm : bag.getTagManifests().stream().toList()) {
            tm.getFileToChecksumMap().put(recordmapPath, 
                    Hasher.hash(recordmapPath, MessageDigest.getInstance(tm.getAlgorithm().getMessageDigestName())));
        }
        ManifestWriter.writeTagManifests(bag.getTagManifests(), outputPath, outputPath, Charset.forName("utf-8"));
        return report;
    }
    private static final Logger LOG = Logger.getLogger(FolderToBasicSIP.class.getName());

    @Override
    public Report function(CorpusData cd) throws Exception, NoSuchAlgorithmException, ClassNotFoundException, FSMException, URISyntaxException, SAXException, IOException, ParserConfigurationException, JexmaraldaException, TransformerException, XPathExpressionException, JDOMException {
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    @Override
    public Collection<Class<? extends CorpusData>> getIsUsableFor() {
        // We do not look at corpus data but only at the corpus directory
        return Collections.EMPTY_LIST;
    }

    @Override
    public String getDescription() {
        return "Creates a IDS SIP from a folder";
    }

    /**
     * List all CMDI files, preferably in a Metadata subfolder
     * @param path The path containing all files
     * @return a set of all found CMDI files
     */
    Set<File> listMetadataFiles(Path metadataPath) {
        return new HashSet<>(
                FileUtils.listFiles(metadataPath.toFile(), 
                    new SuffixFileFilter(".cmdi"), DirectoryFileFilter.INSTANCE)
        );
    }

    /**
     * Lists all content files, i.e. all files that are not CMDI files
     * @param path The path containing all files
     * @return The set of all content files
     */
    Set<File> listContentFiles(Path path) {
        return new HashSet<>(
                FileUtils.listFiles(path.toFile(), 
                    new NotFileFilter(new SuffixFileFilter(".cmdi")), DirectoryFileFilter.INSTANCE)
        );
    }

    /**
     * Bundles the files into records and creates a record map
     * @param path The path containing all files
     * @param title The title of the root record
     * @param metadataFiles All metadata files to be included
     * @param contentFiles All content files to be included
     * @return The record map
     * @throws IOException 
     */
    MapRootRecord bundleFiles(Path metadataPath, Path contentFilePath, String title, Set<File> metadataFiles, Set<File> contentFiles) throws IOException {
        Map<File, Set<File>> records = new HashMap<>();
        // Set of all metadata files that don't have matching content files
        Set<File> noContentMetadata = new HashSet<>();
        // Set of all content files that don't have matching metadata
        Set<File> noMetadataContent = contentFiles.stream().collect(Collectors.toSet());
        // A hash map from record name (extracted from metadata file names) to the metadata file itself
        HashMap<String,File> recordMetadata = new HashMap<>();
        // A hash map from record name to content files to be contained
        HashMap<String,Set<File>> recordFiles = new HashMap<>();
        for (File metadataFile : metadataFiles.stream().toList()) {
            String prefix = metadataFile.getName().replace(".cmdi", "");
            recordMetadata.put(prefix, metadataFile);
            
        }
        for (File contentFile : contentFiles) {
            String fileName = contentFile.getName();
            // Find all record names that are a prefix of the current file name
            Optional<String> recordName = recordMetadata.keySet().stream()
                    .filter((prefix) -> fileName.startsWith(prefix))
                    // Sort by longest firts
                    .sorted(Comparator.comparing(String::length).reversed())
                    // Only get the first one if it exists
                    .findFirst();
            if (recordName.isPresent()) {
                // Put the content file into set of record files (create set if necessary)
                recordFiles.putIfAbsent(recordName.get(), new HashSet<>());
                recordFiles.get(recordName.get()).add(contentFile);
                // Remove from list of files without matching metadata
                noMetadataContent.remove(contentFile);
                }
        }
        // For all metadata files check if they have content files
        for (String recordName : recordMetadata.keySet()) {
            // If not keep track of problematic metadata files
            if (!recordFiles.containsKey(recordName) || recordFiles.get(recordName).isEmpty()) {
                noContentMetadata.add(recordMetadata.get(recordName));
            }
            // Otherwise build record
            else {
                records.put(recordMetadata.get(recordName), recordFiles.get(recordName));
            }
        }
        if (props.containsKey("root-metadata-file") && !noContentMetadata.stream().allMatch((f) -> f.getName().endsWith(props.getProperty("root-metadata-file")))) {
            throw new IOException("Root metadata file not found " + props.getProperty("root-metadata-file"));
        }
        else if (noContentMetadata.size() > 1) {
            throw new IOException("Too many candidates for root metadata file found " + noContentMetadata.toString());
        }
        else if (noContentMetadata.isEmpty()) {
            throw new IOException("No candidate for root metadata file found");
        }
        else
        {
            MapRootRecord recordMap = new MapRootRecord();
            // Set title
            recordMap.setTitle(title);
            // If we have been given a metadata file use it
            if (props.containsKey("root-metadata-file")) {
                recordMap.setMetadata(props.getProperty("root-metadata-file"));
            }
            // Set metadata to the first (and only metadata file without a content file
            else {
                recordMap.setMetadata(noContentMetadata.iterator().next().toString().replace(metadataPath.toString() ,Path.of("data","Metadata").toString()));
            }
            // Set top-level files, i.e. files without separate metadata
            recordMap.setFiles(noMetadataContent.stream().map((f) -> new MapFile(f.toString().replace(contentFilePath.toString(), Path.of("data","Content").toString()))).toList());
            // Create all record bundles by iterating over all metadata files
            recordMap.setRecords(records.keySet().stream().map((mf) -> {
                MapRecord r = new MapRecord();
                r.setMetadata(mf.toString().replace(metadataPath.toString(),Path.of("data","Metadata").toString()));
                // Get the common prefix as a bundle title
                r.setTitle(FilenameUtils.getBaseName(mf.toString()));
                r.setFiles(records.get(mf).stream().map((f) -> new MapFile(f.toString().replace(contentFilePath.toString(), Path.of("data","Content").toString()))).toList());
                return r;
            }).toList());
            return recordMap;
        }
    }

    @Override
    public Map<String, String> getParameters() {
        Map<String,String> parameters = super.getParameters();
        parameters.put("output-path", "Optional path where the BagIt SIP will be written. Defaults to the \"../output\" subfolder");
        parameters.put("root-title", "Optional title of the root record. Defaults to corpus name");
        parameters.put("root-metadata-file", "Optional root-level metadata file. Defaults to the only metadata file not matching any content files");
        parameters.put("record-map-file", "Optional filename of the already existing record map file");
        parameters.put("create-hard-links", "Optional flag to create hard links instead of copying files");
        return parameters;
    }
    
}
