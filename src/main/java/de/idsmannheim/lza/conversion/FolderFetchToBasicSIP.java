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
import gov.loc.repository.bagit.creator.CreatePayloadManifestsVistor;
import gov.loc.repository.bagit.creator.CreateTagManifestsVistor;
import gov.loc.repository.bagit.domain.Bag;
import gov.loc.repository.bagit.domain.FetchItem;
import gov.loc.repository.bagit.domain.Manifest;
import gov.loc.repository.bagit.hash.Hasher;
import gov.loc.repository.bagit.hash.StandardSupportedAlgorithms;
import gov.loc.repository.bagit.hash.SupportedAlgorithm;
import gov.loc.repository.bagit.writer.BagWriter;
import gov.loc.repository.bagit.writer.ManifestWriter;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
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
import org.jdom.JDOMException;
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
 */
public class FolderFetchToBasicSIP extends Converter implements CorpusFunction {

    Path metadataPath;
    Path contentFilePath;
    boolean setUp = false;
    public FolderFetchToBasicSIP(Properties props) {
        super(props);
        if (props.containsKey("metadata-path")) {
            metadataPath = Path.of(props.getProperty("metadata-path"));
            setUp = true;
        }
    }
    
    @Override
    public Report function(Corpus c) throws Exception, NoSuchAlgorithmException, ClassNotFoundException, FSMException, URISyntaxException, SAXException, IOException, ParserConfigurationException, JexmaraldaException, TransformerException, XPathExpressionException, JDOMException {
        Report report = new Report();
        if (setUp) {
            FolderToBasicSIP folderToBasicSIP = new FolderToBasicSIP(new Properties());
            contentFilePath = Path.of(c.getBaseDirectory().toURI());
            Set<File> metadataFiles = folderToBasicSIP.listMetadataFiles(metadataPath);
            Set<File> contentFiles = folderToBasicSIP.listContentFiles(contentFilePath);
            MapRootRecord record;
            if (props.containsKey("record-map-file")) {
                ObjectMapper om = new ObjectMapper();
                om.findAndRegisterModules();
                record = om.readValue(new File(props.getProperty("record-map-file")), MapRootRecord.class);
            }
            else {
                LOG.info("Create record map");
                String title;
                if (props.containsKey("root-title")) {
                    title = props.getProperty("root-title");
                }
                else {
                    title = c.getCorpusName();
                }
                record = folderToBasicSIP.bundleFiles(metadataPath, contentFilePath, title, metadataFiles,contentFiles);
            }
            LOG.info("Check and/or create output");
            // Copy files to output
            // First find output path
            Path outputPath;
            if (props.containsKey("output-path")) {
                outputPath = Path.of(props.getProperty("output-path"));
            }
            else {
                outputPath = Path.of(metadataPath.toString(), "..", "output").toAbsolutePath();
            }
            // Try to create it if missing
            if (!outputPath.toFile().exists()) {
                if (!outputPath.toFile().mkdir()) {
                    report.addCritical("Error creating output");
                    return report;
                }
            }
            // Copy all files
            LOG.info("Copy files to output");
            for (File file : FileUtils.listFiles(metadataPath.toFile(), FileFileFilter.FILE, DirectoryFileFilter.DIRECTORY)) {
                File newFile = Path.of(file.toString().replace(metadataPath.toString(), Path.of(outputPath.toString(),"data","Metadata").toString()))
                        .toAbsolutePath().normalize().toFile();
                LOG.info(newFile.toString());
                try {
                    FileUtils.copyFile(file, newFile);
                }
                catch (IOException e) { 
                    e.getStackTrace();
                }
            }
            // Create a bag
            LOG.info("Create bag");
            //BagCreator.bagInPlace(outputPath, Collections.singleton(StandardSupportedAlgorithms.SHA512), false);
            Bag bag = new Bag();
            bag.setRootDir(outputPath);
            // Create file manifest
            Collection<SupportedAlgorithm> algorithms = Collections.singleton(StandardSupportedAlgorithms.SHA512);
            // Create payload manifest
            LOG.info("Create payload manifest");
            final Map<Manifest, MessageDigest> payloadFilesMap = Hasher.createManifestToMessageDigestMap(algorithms);
            final CreatePayloadManifestsVistor payloadVisitor = new CreatePayloadManifestsVistor(payloadFilesMap, false);
            Files.walkFileTree(Path.of(outputPath.toString(),"data"), payloadVisitor);
            bag.getPayLoadManifests().addAll(payloadFilesMap.keySet());
            // Create fetch file
            List<FetchItem> itemsToFetch = new ArrayList<>();
            for (File f : contentFiles) {
                try {
                    Path payloadFile = Path.of(f.toString().replace(contentFilePath.toString(), Path.of(outputPath.toString(),"data","Content").toString()));
                    // Add file to fetch list
                    itemsToFetch.add(new FetchItem(f.toURI().toURL(), f.length(), payloadFile));
                    // Also add it to payload
                    for (Manifest m : bag.getPayLoadManifests().stream().toList()) {
                        LOG.info("Compute checksum for " + f.toString());
                        m.getFileToChecksumMap().put(payloadFile,
                        Hasher.hash(f.toPath(), MessageDigest.getInstance(m.getAlgorithm().getMessageDigestName())));
            }
                }
                catch (MalformedURLException e) {
                    report.addCritical(this.getFunction(), e, "Exception when generating URL from file");
                }
            }
            bag.setItemsToFetch(itemsToFetch);
            LOG.info("Write bag");
            BagWriter.write(bag, outputPath);
            // Write payload manifest
            LOG.info("Write payload manifest");
            ManifestWriter.writePayloadManifests(bag.getPayLoadManifests(), outputPath, bag.getRootDir(), bag.getFileEncoding());
            // Write record map and add it to tag manifest
            LOG.info("Write record map");
            XmlMapper mapper = new XmlMapper();
            mapper.enable(SerializationFeature.INDENT_OUTPUT);
            mapper.findAndRegisterModules();
            Path recordmapPath = Path.of(outputPath.toString(), "recordmap.xml");
            mapper.writeValue(recordmapPath.toFile(), record);
            // Create tag manifest
            LOG.info("Create tag file");
            final Map<Manifest, MessageDigest> tagFilesMap = Hasher.createManifestToMessageDigestMap(algorithms);
            final CreateTagManifestsVistor tagVistor = new CreateTagManifestsVistor(tagFilesMap, true);
            Files.walkFileTree(outputPath, tagVistor);
            bag.getTagManifests().addAll(tagFilesMap.keySet());
            LOG.info("Write tag manifest");
            ManifestWriter.writeTagManifests(bag.getTagManifests(), outputPath, bag.getRootDir(), bag.getFileEncoding());
//            // Update bag
//            LOG.info("Update bag");
//            for (Manifest tm : bag.getTagManifests().stream().toList()) {
//                tm.getFileToChecksumMap().put(recordmapPath,
//                        Hasher.hash(recordmapPath, MessageDigest.getInstance(tm.getAlgorithm().getMessageDigestName())));
//            }
//            ManifestWriter.writeTagManifests(bag.getTagManifests(), outputPath, outputPath, Charset.forName("utf-8"));
        }
        else {
            report.addCritical("Not set up");
        }
        return report;
    }
    private static final Logger LOG = Logger.getLogger(FolderFetchToBasicSIP.class.getName());

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

//    /**
//     * List all CMDI files, preferably in a Metadata subfolder
//     * @param path The path containing all files
//     * @return a set of all found CMDI files
//     */
//    private Set<File> listMetadataFiles(Path path) {
//        Path metadataPath;
//        if (Path.of(path.toString(),"data","Metadata").toFile().exists()) {
//            metadataPath = Path.of(path.toString(),"data","Metadata");
//        }
//        else if (Path.of(path.toString(),"Metadata").toFile().exists()) {
//            metadataPath = Path.of(path.toString(),"Metadata");
//        }
//        else {
//            metadataPath = path;
//        }
//        return new HashSet<>(
//                FileUtils.listFiles(metadataPath.toFile(), 
//                    new SuffixFileFilter(".cmdi"), DirectoryFileFilter.INSTANCE)
//        );
//    }

//    /**
//     * Lists all content files, i.e. all files that are not CMDI files
//     * @param path The path containing all files
//     * @return The set of all content files
//     */
//    private Set<File> listContentFiles(Path path) {
//        return new HashSet<>(
//                FileUtils.listFiles(path.toFile(), 
//                    new NotFileFilter(new SuffixFileFilter(".cmdi")), DirectoryFileFilter.INSTANCE)
//        );
//    }

//    /**
//     * Bundles the files into records and creates a record map
//     * @param metadataPath The path containing all metadata files
//     * @param contentFilePath The path containing all content files
//     * @param title The title of the root record
//     * @param metadataFiles All metadata files to be included
//     * @param contentFiles All content files to be included
//     * @return The record map
//     * @throws IOException 
//     */
//    private MapRootRecord bundleFiles(Path metadataPath, Path contentFilePath, String title, Set<File> metadataFiles, Set<File> contentFiles) throws IOException {
//        
//        Map<File, Set<File>> records = new HashMap<>();
//        // Set of all metadata files that don't have matching content files
//        Set<File> noContentMetadata = new HashSet<>();
//        // Set of all content files that don't have matching metadata
//        Set<File> noMetadataContent = contentFiles.stream().collect(Collectors.toSet());
//        for (File mf : metadataFiles) {
//            // Convert metadata filename into content file prefix
//            String mfName = mf.toString()
//                    .replace(metadataPath.toString(),contentFilePath.toString())
//                    .replace("Metadata","Content")
//                    .replace(".cmdi","");
//            LOG.info(mfName);
//            // Find all content files starting with this prefix
//            Set<File> recordFiles = contentFiles.stream().filter((cf) -> cf.toString().startsWith(mfName)).collect(Collectors.toSet());
//            noMetadataContent.removeAll(recordFiles);
//            // If we have content files 
//            if (!recordFiles.isEmpty()) {
//                records.put(mf, 
//                    recordFiles
//                    );
//            }
//            else {
//                noContentMetadata.add(mf);
//            }
//        }
//        if (props.containsKey("root-metadata-file") && !noContentMetadata.stream().allMatch((f) -> f.getName().endsWith(props.getProperty("root-metadata-file")))) {
//            throw new IOException("Root metadata file not found " + props.getProperty("root-metadata-file"));
//        }
//        else if (noContentMetadata.size() > 1) {
//            throw new IOException("Too many candidates for root metadata file found " + noContentMetadata.toString());
//        }
//        else if (noContentMetadata.isEmpty()) {
//            throw new IOException("No candidate for root metadata file found");
//        }
//        else
//        {
//            MapRootRecord recordMap = new MapRootRecord();
//            // Set title
//            recordMap.setTitle(title);
//            // If we have been given a metadata file use it
//            if (props.containsKey("root-metadata-file")) {
//                recordMap.setMetadata(props.getProperty("root-metadata-file"));
//            }
//            // Set metadata to the first (and only metadata file without a content file
//            else {
//                recordMap.setMetadata(noContentMetadata.iterator().next().toString().replace(metadataPath.toString(),"data/Metadata"));
//            }
//            // Set top-level files, i.e. files without separate metadata
//            recordMap.setFiles(noMetadataContent.stream().map((f) -> new MapFile(f.toString().replace(contentFilePath.toString(), "data/Content"))).toList());
//            // Create all record bundles by iterating over all metadata files
//            recordMap.setRecords(records.keySet().stream().map((mf) -> {
//                MapRecord r = new MapRecord();
//                r.setMetadata(mf.toString().replace(metadataPath.toString(),"data/Metadata"));
//                // Get the common prefix as a bundle title
//                r.setTitle(FilenameUtils.getBaseName(mf.toString()));
//                r.setFiles(records.get(mf).stream().map((f) -> new MapFile(f.toString().replace(contentFilePath.toString(),"data/Content"))).toList());
//                return r;
//            }).toList());
//            return recordMap;
//        }
//    }

    @Override
    public Map<String, String> getParameters() {
        Map<String,String> parameters = super.getParameters();
        parameters.put("metadata-path", "The path where the metadata is located");
        parameters.put("file-path", "The path where the files are located");
        parameters.put("output-path", "Optional path where the BagIt SIP will be written. Defaults to the \"../output\" subfolder relative to the metadata location");
        parameters.put("root-title", "Optional title of the root record. Defaults to corpus name");
        parameters.put("root-metadata-file", "Optional root-level metadata file. Defaults to the only metadata file not matching any content files");
        parameters.put("record-map-file", "Optional filename of the already existing record map file");
        return parameters;
    }
    
}
