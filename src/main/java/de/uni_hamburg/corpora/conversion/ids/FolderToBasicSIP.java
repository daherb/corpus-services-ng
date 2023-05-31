/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package de.uni_hamburg.corpora.conversion.ids;

import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import de.uni_hamburg.corpora.Corpus;
import de.uni_hamburg.corpora.CorpusData;
import de.uni_hamburg.corpora.conversion.Converter;

import de.uni_hamburg.corpora.CorpusFunction;
import de.uni_hamburg.corpora.Report;
import de.uni_hamburg.corpora.utilities.publication.ids.mapper.MapFile;
import de.uni_hamburg.corpora.utilities.publication.ids.mapper.MapRecord;
import de.uni_hamburg.corpora.utilities.publication.ids.mapper.MapRootRecord;
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
import java.util.HashMap;
import java.util.HashSet;
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
public class FolderToBasicSIP extends Converter implements CorpusFunction {

    public FolderToBasicSIP(Properties props) {
        super(props);
    }
    
    @Override
    public Report function(Corpus c) throws Exception, NoSuchAlgorithmException, ClassNotFoundException, FSMException, URISyntaxException, SAXException, IOException, ParserConfigurationException, JexmaraldaException, TransformerException, XPathExpressionException, JDOMException {
        Report report = new Report();
        Path path = Path.of(c.getBaseDirectory().toURI());
        
        Set<File> metadataFiles = listMetadataFiles(path);
        Set<File> contentFiles = listContentFiles(path);
        MapRootRecord record;
        try {
            LOG.info("Create record map");
            String title;
            if (props.containsKey("root-title")) {
                title = props.getProperty("root-title");
            }
            else {
                title = c.getCorpusName();
            }
            record = bundleFiles(path, title, metadataFiles,contentFiles);
        }
        catch (IOException e) {
            report.addException(getFunction(), e, "Exception when creating record map");
            return  report;
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
                Files.createLink(file.toPath(), Path.of(file.toString().replace(path.toString(), outputPath.toString())));
            }
            else {
                FileUtils.copyFile(file, Path.of(file.toString().replace(path.toString(), outputPath.toString())).toFile());
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
    private Set<File> listMetadataFiles(Path path) {
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
    private Set<File> listContentFiles(Path path) {
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
    private MapRootRecord bundleFiles(Path path, String title, Set<File> metadataFiles, Set<File> contentFiles) throws IOException {
        
        Map<File, Set<File>> records = new HashMap<>();
        // Set of all metadata files that don't have matching content files
        Set<File> noContentMetadata = new HashSet<>();
        // Set of all content files that don't have matching metadata
        Set<File> noMetadataContent = contentFiles.stream().collect(Collectors.toSet());
        for (File mf : metadataFiles) {
            // Convert metadata filename into content file prefix
            String mfName = mf.toString().replace("/Metadata/","/Content/").replace(".cmdi","");
            // Find all content files starting with this prefix
            Set<File> recordFiles = contentFiles.stream().filter((cf) -> cf.toString().startsWith(mfName)).collect(Collectors.toSet());
            noMetadataContent.removeAll(recordFiles);
            // If we have content files 
            if (!recordFiles.isEmpty()) {
                records.put(mf, 
                    recordFiles
                    );
            }
            else {
                noContentMetadata.add(mf);
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
                recordMap.setMetadata(noContentMetadata.iterator().next().toString().replace(path.toString(),"data"));
            }
            // Set top-level files, i.e. files without separate metadata
            recordMap.setFiles(noMetadataContent.stream().map((f) -> new MapFile(f.toString().replace(path.toString(), "data"))).toList());
            // Create all record bundles by iterating over all metadata files
            recordMap.setRecords(records.keySet().stream().map((mf) -> {
                MapRecord r = new MapRecord();
                r.setMetadata(mf.toString().replace(path.toString(),"data"));
                // Get the common prefix as a bundle title
                r.setTitle(FilenameUtils.getBaseName(mf.toString()));
                r.setFiles(records.get(mf).stream().map((f) -> new MapFile(f.toString().replace(path.toString(),"data"))).toList());
                return r;
            }).toList());
            return recordMap;
        }
    }

    @Override
    public Map<String, String> getParameters() {
        Map<String,String> parameters = super.getParameters();
        parameters.put("output-path", "The path where the BagIt SIP will be written. Defaults to the \"../output\" subfolder");
        parameters.put("root-title", "The title of the root record. Defaults to corpus name");
        parameters.put("root-metadata-file", "Root-level metadata file. Defaults to the only metadata file not matching any content files");
        parameters.put("create-hard-links", "Flag to create hard links instead of copying files");
        return parameters;
    }
    
}
