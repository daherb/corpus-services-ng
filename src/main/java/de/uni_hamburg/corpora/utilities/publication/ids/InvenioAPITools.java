/*
* Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
* Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
*/
package de.uni_hamburg.corpora.utilities.publication.ids;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import de.idsmannheim.lza.inveniojavaapi.API;
import de.idsmannheim.lza.inveniojavaapi.Access;
import de.idsmannheim.lza.inveniojavaapi.CMDI;
import de.idsmannheim.lza.inveniojavaapi.ControlledVocabulary;
import de.idsmannheim.lza.inveniojavaapi.DraftRecord;
import de.idsmannheim.lza.inveniojavaapi.Files;
import de.idsmannheim.lza.inveniojavaapi.FilesOptions;
import de.idsmannheim.lza.inveniojavaapi.Metadata;
import de.idsmannheim.lza.inveniojavaapi.Record;
import de.idsmannheim.lza.inveniojavaapi.Records;
import de.idsmannheim.lza.inveniojavaapi.cmdi.CollectionProfileMapper;
import de.idsmannheim.lza.inveniojavaapi.cmdi.OLACDcmiTermsMapper;
import de.idsmannheim.lza.inveniojavaapi.cmdi.SpeechCorpusProfileMapper;
import de.idsmannheim.lza.inveniojavaapi.cmdi.TextCorpusProfileMapper;
import de.idsmannheim.lza.xmlmagic.MimeType;
import de.idsmannheim.lza.xmlmagic.XmlMagic;
import de.uni_hamburg.corpora.Report;
import de.uni_hamburg.corpora.utilities.publication.ids.mapper.MapFile;
import de.uni_hamburg.corpora.utilities.publication.ids.mapper.MapRecord;
import de.uni_hamburg.corpora.utilities.publication.ids.mapper.MapRootRecord;
import gov.loc.repository.bagit.domain.Bag;
import gov.loc.repository.bagit.domain.FetchItem;
import gov.loc.repository.bagit.exceptions.CorruptChecksumException;
import gov.loc.repository.bagit.exceptions.FileNotInPayloadDirectoryException;
import gov.loc.repository.bagit.exceptions.InvalidBagitFileFormatException;
import gov.loc.repository.bagit.exceptions.MaliciousPathException;
import gov.loc.repository.bagit.exceptions.MissingBagitFileException;
import gov.loc.repository.bagit.exceptions.MissingPayloadDirectoryException;
import gov.loc.repository.bagit.exceptions.MissingPayloadManifestException;
import gov.loc.repository.bagit.exceptions.UnparsableVersionException;
import gov.loc.repository.bagit.exceptions.UnsupportedAlgorithmException;
import gov.loc.repository.bagit.exceptions.VerificationException;
import gov.loc.repository.bagit.reader.BagReader;
import gov.loc.repository.bagit.verify.BagVerifier;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.security.KeyManagementException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.xml.bind.DatatypeConverter;
import javax.xml.bind.JAXBException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.FileFileFilter;
import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;


/**
 * Class providing higher-level abstraction helpers based on Invenio API calls
 * @author Herbert Lange <lange@ids-mannheim.de>
 */
public class InvenioAPITools {
    
    // Mutex to only allow one operation at a time
    private static final ReentrantLock mutex = new ReentrantLock();
    
    // Information how to find the metadata file if no record map exists
    static final String METADATA_DIR = "data/Metadata";
    static final String METADATA_FILE = "metadata.cmdi";
    static final String RECORD_MAP_FILE = "recordmap.xml";
    //static final String RECORD_MAP_FILE = "recordmap.json";
    // Path separator to be used instead of /
    static final String SEPARATOR = "-0-0-";
    ControlledVocabulary.LanguageIdFactory languageIdFactory;
    API api;
    String url;
    
    private static final Logger LOG = Logger.getLogger(InvenioAPITools.class.getName());
    
    
    //------------------------------------------------------------------------//
    //                                                                        //
    // Public API                                                             //
    //                                                                        //
    //------------------------------------------------------------------------//
    
    /**
     * Default constructor
     * @param api The API object to be used
     * @throws java.io.IOException
     */
    public InvenioAPITools(API api) throws IOException {
        this.languageIdFactory = new ControlledVocabulary.LanguageIdFactory();
        this.api = api;
        url = api.protocol + "://" + api.host + "/records/";
    }
    
    /**
     * Adds files from a path to Invenio.This will result in one or several
     * new records
     * @param path the path to the files to be added
     * @param filesArePublic flag if files should be public if no mapping is present
     * @param report the report to keep track of detailed information about the process
     * @return the id of the main record if the operation was successful
     * @throws javax.xml.bind.JAXBException
     * @throws java.io.IOException
     * @throws java.net.URISyntaxException
     * @throws java.security.NoSuchAlgorithmException
     * @throws java.security.KeyManagementException
     * @throws java.lang.InterruptedException
     * @throws org.jdom2.JDOMException
     * @throws java.lang.CloneNotSupportedException
     */
    public Optional<String> createObject(Path path, boolean filesArePublic, Report report) throws JAXBException, IOException, URISyntaxException, NoSuchAlgorithmException, KeyManagementException, InterruptedException, KeyManagementException, JDOMException, CloneNotSupportedException {
        LOG.info("Validate data before ingest");
        // Get the mapping from files to Invenio records
        MapRootRecord mapping = getMapping(path, filesArePublic);
        // Validate the mapping
        boolean validBag = validateBag(path, report);
        boolean validMapping = validateMapping(path, mapping, report);
        if (validBag && validMapping) {
            try {
                // Read the bag
                Bag bag = new BagReader().read(path);
                // Try to get the lock with a timeout
                if (mutex.tryLock(10, TimeUnit.MINUTES)) {
                    // Upload the file according to the mapping
                    LOG.info("Upload records");
                    String id = mappingToRecords(path, mapping, report);
                    // Double check if the upload was completely successful
                    LOG.info("Validate uploaded data");
                    if (validateDraftRecords(id, path, bag, report)) {
                        // Publish all drafts
                        LOG.info("Publish records");
                        publishDraftRecords(report);
                        // Release the mutex again
                        mutex.unlock();
                        // Return the first id which is the one of the main record
                        return Optional.of(id);
                    }
                }
                else {
                    report.addCritical("InvenioAPI", "Failed to get lock");
                }
            }
            // Catch all excepions to report them before role-back
            catch (Exception e) {
                report.addException("InvenioAPI", e, "Exception while creating object");
            }
            // If we are here something went wrong and we have to revert to the initial state
            LOG.severe("Rollback");
            deleteDraftRecords();
        }
        else{
            if (!validBag)
                report.addCritical("InvenioAPI", "Validation of BagIt failed");
            if (!validMapping)
                report.addCritical("InvenioAPI", "Validation of mapping failed");
        }
        // No success, so no new id
        return Optional.empty();
    }
    
    //------------------------------------------------------------------------//
    //                                                                        //
    // Private helper functions                                               //
    //                                                                        //
    //------------------------------------------------------------------------//
    
    /**
     * Gets a mapping from a path, either by reading the mapping specification or
     * by creating one ad-hoc
     * @param path the input path
     * @param filesArePublic flags if files should be public when creating a new mapping. Ignored when a mapping is present
     * @return the mapping
     */
    private MapRootRecord getMapping(Path path, boolean filesArePublic) throws JAXBException, IOException {
        MapRootRecord mapping;
        File recordMapFile = Path.of(path.toString(),RECORD_MAP_FILE).toFile();
        if (recordMapFile.exists()) {
            // JAXBContext context = JAXBContext.newInstance(MapRootRecord.class);
            // mapping = (MapRootRecord) context.createUnmarshaller().unmarshal(recordMapFile);
            XmlMapper xm = new XmlMapper();
            mapping = xm.readValue(recordMapFile, MapRootRecord.class);
        }
        else {
            File metadataFile = Path.of(path.toString(),METADATA_DIR, METADATA_FILE).toFile();
            mapping = new MapRootRecord();
            // Initialize metadata
            mapping.setMetadata(metadataFile.toString());
            // Initialize files
            List<MapFile> fileList = new ArrayList<>();
            for (File f : listFilesInPath(path)) { 
                MapFile mf = new MapFile(); 
                mf.setPublic(filesArePublic);
                mf.setName(f.toString());
                fileList.add(mf);
            }
            mapping.setFiles(fileList);
            // Initialize records
            mapping.setRecords(new ArrayList<>());
            
        }
        return mapping;
    }
    
    /**
     * Validates the mapping to make sure that all files are covered and all mapped files exist
     * @param path the input path
     * @param mapping the mapping to records
     * @param report the report to keep track of detailed information about the process
     * @return if the path matches the mapping
     * @throws IOException
     */
    private boolean validateMapping(Path path, MapRootRecord mapping, Report report) throws IOException {
        // Get a list of all files from the data directory of path
        Set<File> filesPresent = listFilesInPath(Path.of(path.toString(),"data"));
        // Get a list of all files in the map
        Set<File> filesExpected = listMapRecordFiles(path, mapping);
        boolean allFilesPresent = filesExpected.containsAll(filesPresent);
        boolean allFileExpected = filesPresent.containsAll(filesExpected);
        if (allFilesPresent && allFileExpected) {
            return true;
        }
        else {
            // Check if all present files are expected
            if (!filesExpected.containsAll(filesPresent)) {
                report.addCritical("InvenioAPI", "Unexpected files in path: ", filesPresent.stream().filter((f) -> !filesExpected.contains(f)).map(File::toString).collect(Collectors.joining(", ")));
            }
            // Check if all expected files are present
            if (!filesPresent.containsAll(filesExpected)) {
                report.addCritical("InvenioAPI", "Expected files missing in path: ", filesExpected.stream().filter((f) -> !filesPresent.contains(f)).map(File::toString).collect(Collectors.joining(", ")));
            }
            return false;
        }
    }
    
    /**
     * Recursively list all files in a given path
     * @param path the input path
     * @return the set of all filenames
     */
    private Set<File> listFilesInPath(Path path) {
        return new HashSet<>(FileUtils.listFiles(path.toFile(), FileFileFilter.FILE, DirectoryFileFilter.DIRECTORY));
    }
    
    /**
     * List all files in all map records
     * @param path the root path of the files
     * @param record the root record
     * @return the set of all files below this record as strings
     * @throws IOException
     */
    private Set<File> listMapRecordFiles(Path path, MapRecord record) throws IOException{
        Set<File> files = new HashSet<>();
        // Add metadata file if present
        if (record.getMetadata().isPresent()) {
            files.add(Path.of(path.toString(),record.getMetadata().get()).toFile().getAbsoluteFile().getCanonicalFile());
        }
        // Add all files of this record as Strings
        if (record.getFiles() != null) {
            for (MapFile mf: record.getFiles()) {
                files.add(Path.of(path.toString(),mf.getName()).toFile().getAbsoluteFile().getCanonicalFile());
            }
        }
        // Go through all child records and add their files
        if (record.getRecords() != null) {
            for (MapRecord rr : record.getRecords()) {
                files.addAll(listMapRecordFiles(path, rr));
            }
        }
        return files;
    }
    
    /**
     * Validates the input as a BagIt container
     * @param path The path to the input
     * @param report the report to keep track of detailed information about the process
     * @return if it is a valid BagIt
     */
    private boolean validateBag(Path path, Report report) {
        BagVerifier verifier = new BagVerifier();
        try {
            Bag bag = new BagReader().read(path);
            for (FetchItem item : bag.getItemsToFetch()) {
                // Download file
                item.getUrl().openStream().transferTo(new FileOutputStream(item.getPath().toFile()));
                // Create blank file
                // item.getPath().toFile().createNewFile();
            }
            // Read the BagIt and validates it without ignoring hidden files (ignoreHidden is false)
            verifier.isValid(bag,false);
            // If validation completes without an exception we succeed
            return true;
        }
        // If we encounter an exception the validation has failed
        catch (CorruptChecksumException | FileNotInPayloadDirectoryException | InvalidBagitFileFormatException | MaliciousPathException | MissingBagitFileException | MissingPayloadDirectoryException | MissingPayloadManifestException | UnparsableVersionException | UnsupportedAlgorithmException | VerificationException | IOException | InterruptedException e) {
            report.addException("InvenioAPI", e, "Exception when checking BagIt " + path);
            return false;
        }
    }
    
    /**
     * Uploads all files given in a map record to Invenio
     * @param mapping the mapping to be uploaded
     * @param report the report to keep track of detailed information about the process
     * @return the id of the root node
     */
    private String mappingToRecords(Path path, MapRootRecord mapping, Report report) throws IOException, InterruptedException, URISyntaxException, NoSuchAlgorithmException, KeyManagementException, JDOMException, CloneNotSupportedException {
        // Get the metadata
        Metadata metadata;
        try {
            metadata = readMetadata(Path.of(path.toString(),mapping.getMetadata().get()).toFile().getCanonicalFile());
        }
        catch (IOException | JDOMException e) {
            report.addException("InvenioAPI", e, "Exception while loading metadata file " + mapping.getMetadata().get());
            throw e;
        }
        // Add empty spare record for preservation management
        // TODO test if that actually works
        MapRecord preservationRecord = new MapRecord();
        preservationRecord.setTitle("Preservation information");
        mapping.getRecords().add(preservationRecord);
        // Continue with uploade
        return uploadRecord(path, mapping, metadata, report);
    }
    
    private String uploadRecord(Path path, MapRecord record, Metadata metadata, Report report) throws IOException, JDOMException, InterruptedException, URISyntaxException, KeyManagementException, NoSuchAlgorithmException, CloneNotSupportedException {
        // Create draft record
        // Set access. Metadata is always public and file access depends
        // If all files are explicitly public or no information is given they are considered public
        Access.AccessType fileAccess;
        if (record.getFiles().stream().allMatch(MapFile::isPublic)) {
            fileAccess = Access.AccessType.Public;
        }
        else {
            fileAccess = Access.AccessType.Restricted;
        }
        Access access = new Access(Access.AccessType.Public, fileAccess);
        // Update metadata if necessary
        Metadata currentMetadata;
        if (!record.getMetadata().isEmpty() && !record.getMetadata().get().isEmpty()) {
            try {
                currentMetadata = readMetadata(Path.of(path.toString(),record.getMetadata().get()).toFile().getCanonicalFile());
            }
            catch (IOException | JDOMException e) {
                report.addException("InvenioAPI", e, "Exception while loading metadata");
                throw e;
            }
        }
        else {
            try {
                currentMetadata = (Metadata) metadata.clone();
            }
            catch (CloneNotSupportedException e) {
                report.addException("InvenioAPI", e, "Exception when cloning metadata");
                throw e;
            }
        }
        if (record.getTitle().isPresent() && !record.getTitle().get().isEmpty()) {
            currentMetadata.setTitle(metadata.getTitle() + ": " + record.getTitle().get());
        }
        // Files are only added if necessary, i.e. if either metadata or files are part of the record
        FilesOptions files = new FilesOptions(!record.getFiles().isEmpty() || record.getMetadata().isPresent());
        DraftRecord draft = new DraftRecord(access, files, currentMetadata);
        Record result;
        try {
            result = api.createDraftRecord(draft);
        }
        catch (IOException | InterruptedException | URISyntaxException | KeyManagementException | NoSuchAlgorithmException e) {
            report.addException("InvenioAPI", e, "Exception when creating new draft record");
            throw e;
        }
        // Upload files
        // Prepare file entries
        ArrayList<Files.FileEntry> entries = new ArrayList<>();
        HashMap<String,File> fileMap = new HashMap<>();
        ArrayList<String> candidates = new ArrayList<>();
        // Potential default preview file
        String defaultPreview = "";
        // Add metadata fie if it exists
        if (record.getMetadata().isPresent()) {
            candidates.add(record.getMetadata().get());
        }
        // Add all other files
        candidates.addAll(record.getFiles().stream().map(MapFile::getName).toList());
        for (String filename : candidates) {
            // Normalize filename and replace path separators
            String updatedName = filename.replaceAll("^./","").replaceAll("/", SEPARATOR);
            // Set default preview if not set yet and file ends in cmdi
            if (defaultPreview.isBlank() && updatedName.endsWith(".cmdi")) {
                defaultPreview = updatedName;
            }
            Files.FileEntry entry = new Files.FileEntry(updatedName);
            entries.add(entry);
            fileMap.put(updatedName, Path.of(path.toString(),filename).toFile().getAbsoluteFile().getCanonicalFile());
            
        }
        api.startDraftFileUpload(result.getId(), entries);
        // For each file
        for (String key : fileMap.keySet()) {
            // Upload file
            LOG.log(Level.INFO, "Uploading {0}", fileMap.get(key));
            api.uploadDraftFile(result.getId(), key, fileMap.get(key));
            api.completeDraftFileUpload(result.getId(), key);
        }
        ArrayList<Metadata.RelatedIdentifier> relatedIdentifiers = new ArrayList<>();
        // Upload child records
        for (MapRecord child : record.getRecords()) {
            String id = uploadRecord(path, child, metadata, report);
            relatedIdentifiers.add(new Metadata.RelatedIdentifier(url + id,
                        new ControlledVocabulary.RelatedRecordIdentifierScheme(ControlledVocabulary.RelatedRecordIdentifierScheme.ERelatedRecordIdentifierScheme.URL),
                        new Metadata.RelatedIdentifier.RelationType(new ControlledVocabulary.RelationTypeId(ControlledVocabulary.RelationTypeId.ERelationTypeId.HasPart),
                                new Metadata.LocalizedStrings().add(new Metadata.Language(languageIdFactory.usingId2("en")), "Has part"))));
            
        }
        // Add references and potentially default preview
        currentMetadata.addRelatedIdentifiers(relatedIdentifiers);
        if (!defaultPreview.isBlank()) {
            draft.getFiles().setDefaultPreview(defaultPreview);
        }
        api.updateDraftRecord(result.getId(), draft);
        return result.getId();
    }
    
    /**
     * Read the Invenio metadata from a CMDI file
     * @param metadataFile the metadata file
     * @return the Invenio metadata
     * @throws IOException
     * @throws JDOMException 
     */
    private Metadata readMetadata(File metadataFile) throws IOException, JDOMException {
        // Read the CMDI file
        Document document = new SAXBuilder().build(metadataFile);
        XmlMagic magic = new XmlMagic(document);
        for (MimeType mt : magic.getMimeTypes()) {
            // Find the CMDI mime type
            if (mt.getSubtype().equals("x-cmdi")) {
                
                switch (mt.getParameters().get("profile")) {
                    // Speech corpus profile
                    case "clarin.eu:cr1:p_1527668176128":
                        return CMDI.readCmdiMetadata(new SpeechCorpusProfileMapper(document));
                    // Text corpus profile
                    case "clarin.eu:cr1:p_1559563375778":
                        return CMDI.readCmdiMetadata(new TextCorpusProfileMapper(document));
                    // OLAC DCMI terms
                    case "clarin.eu:cr1:p_1366895758244":
                        return CMDI.readCmdiMetadata(new OLACDcmiTermsMapper(document));
                    case "clarin.eu:cr1:p_1659015263839":
                        return CMDI.readCmdiMetadata(new CollectionProfileMapper(document));
                    default:
                        throw new IOException("Unsupported CMDI profile " + mt.getParameters().get("profile"));
                }
            }
        }
        throw new IOException("Unrecognized CMDI file or profile");
    }
    
    /**
     * Validates all draft records against the initial bag
     * @param id the record id
     * @param path the root of the file hierarchy
     * @param bag the initial bag
     * @param report the report to keep track of detailed information about the process
     * @return if the draft records match the input data
     */
    private boolean validateDraftRecords(String id, Path path, Bag bag, Report report) throws URISyntaxException, NoSuchAlgorithmException, IOException, JsonProcessingException, KeyManagementException, InterruptedException {
        Files files = api.listDraftFiles(id);
        HashMap<String,String> checksums = new HashMap<>();
        if (files.getEntries() instanceof List) {
            for (Files.FileEntry entry : (List<Files.FileEntry>) files.getEntries()) {
                checksums.put(entry.getKey(), entry.getChecksum());
            }
        }
        else {
            for (String key : ((Map<String,Files.FileEntry>) files.getEntries()).keySet()) {
                Files.FileEntry entry = ((Map<String,Files.FileEntry>) files.getEntries()).get(key);
                checksums.put(entry.getKey(), entry.getChecksum());
            }
        }
        boolean result = true;
        LOG.log(Level.INFO, "Validating record {0}", id);
        // Check checksums for all files
        for (Map.Entry<String, String> entry : checksums.entrySet()) {
            String filename = entry.getKey().replaceAll(SEPARATOR, "/");
            File file = Path.of(path.toString(),filename).toFile().getAbsoluteFile();
            String md5sum = entry.getValue();
            // TODO match the digest algorithm with the one specified in the beginning of the provided checksum
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.reset();
            md.update(new FileInputStream(file).readAllBytes());
            String newSum = "MD5:" + DatatypeConverter.printHexBinary(md.digest());
            result = result && newSum.equalsIgnoreCase(md5sum);
        }
        // Add info about results to the report
        if (result) {
            report.addCorrect("InvenioAPI", "Sucessfuly validated record " + id);
        }
        else {
            report.addCritical("InvenioAPI", "Failed to validate record " + id);
        }
        // Also validate all related records
        DraftRecord record = api.getDraftRecord(id);
        for (Metadata.RelatedIdentifier relatedId : record.getMetadata().getRelatedIdentifiers()) {
            if (relatedId.getRelationType().getId().toString().equalsIgnoreCase("haspart")) {
                String relId = relatedId.getIdentifier().replace(url, "");
                result = result && validateDraftRecords(relId, path, bag, report);
            }
        }
        return result;
    }
    
    private void publishDraftRecords(Report report) throws URISyntaxException, NoSuchAlgorithmException, KeyManagementException, IOException, InterruptedException {
        for (String id : listDraftRecords()) {
            LOG.log(Level.INFO, "Publish record {0}", id);
            report.addCorrect("InvenioAPI", "Published record " + id);
            DraftRecord result = api.publishDraftRecord(id);
            // TODO check result?
        }
        
    }
    
    public void deleteDraftRecords() throws URISyntaxException, NoSuchAlgorithmException, KeyManagementException, IOException, InterruptedException {
        for (String id : listDraftRecords()) {
            LOG.log(Level.INFO, "Deleting {0}", id);
            api.deleteDraftRecord(id);
        }
    }
    
    /**
     * Lists the id of all of the current users draft records
     * @return the list of draft record ids
     */
    public List<String> listDraftRecords() throws IOException, InterruptedException, URISyntaxException, NoSuchAlgorithmException, KeyManagementException {
        // First list records to get the number of all records
        Records records = api.listUserRecords(Optional.empty(),Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
        // Now list all the records
        records = api.listUserRecords(Optional.empty(),Optional.empty(), Optional.of(records.getHits().getTotal()), Optional.empty(), Optional.empty());
        // Get all record ids
        ArrayList<String> ids = new ArrayList<>();
        Records.Hits hits = records.getHits();
        // Only filter unpublished ones
        for (Record hit : hits.getHits()) {
            // Check if it is unpublished -> draft
            if (!hit.isPublished()) {
                ids.add(hit.getId());
            }
        }
        return ids;
    }
}
