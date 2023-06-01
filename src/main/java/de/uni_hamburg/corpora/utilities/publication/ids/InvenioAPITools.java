/*
* Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
* Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
*/
package de.uni_hamburg.corpora.utilities.publication.ids;

import com.fasterxml.jackson.core.JsonProcessingException;
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
import de.idsmannheim.lza.inveniojavaapi.cmdi.CmdiProfileMapping;
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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.security.KeyManagementException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
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
     * Adds files from a path to a new Invenio object.This will result in one or several
 new records
     * @param path the path to the files to be added
     * @param filesArePublic flag if files should be public if no specific information is present
     * @param update flag if existing records with the same title should be updated
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
    public Optional<String> createOrUpdateObject(Path path, boolean filesArePublic, boolean update, Report report) throws JAXBException, IOException, URISyntaxException, NoSuchAlgorithmException, KeyManagementException, InterruptedException, KeyManagementException, JDOMException, CloneNotSupportedException {
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
                    RecordId id = mappingToRecords(path, mapping, update, report);
                    // Double check if the upload was completely successful
                    LOG.info("Validate uploaded data");
                    if (validateRecords(id, path, bag, report)) {
                        // Publish all drafts
                        LOG.info("Publish records");
                        publishDraftRecords(report);
                        // Release the mutex again
                        mutex.unlock();
                        // Return the first id which is the one of the main record
                        return Optional.of(id.getId());
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
    
    /**
     * Delete all unpublished draft records
     * @throws URISyntaxException
     * @throws NoSuchAlgorithmException
     * @throws KeyManagementException
     * @throws IOException
     * @throws InterruptedException 
     */
    public void deleteDraftRecords() throws URISyntaxException, NoSuchAlgorithmException, KeyManagementException, IOException, InterruptedException {
        for (String id : listDraftRecords()) {
            LOG.log(Level.INFO, "Deleting {0}", id);
            api.deleteDraftRecord(id);
        }
    }
    
    /** *  Download an Invenio object. This can consist of one or several records
     * 
     * @param recordId The record id of the root record
     * @param outputPath the path where the files will be stored
     * @param report report for logging
     * @throws java.net.URISyntaxException
     * @throws java.io.IOException
     * @throws java.security.NoSuchAlgorithmException
     * @throws java.security.KeyManagementException
     * @throws com.fasterxml.jackson.core.JsonProcessingException
     * @throws java.lang.InterruptedException
     * @throws java.io.UnsupportedEncodingException
     */
    public void downloadObject(String recordId, Path outputPath, Report report) throws URISyntaxException, NoSuchAlgorithmException, NoSuchAlgorithmException, KeyManagementException, JsonProcessingException, IOException, InterruptedException, UnsupportedEncodingException {
        // First download all files
        Files files = api.listRecordFiles(recordId);
        // File list can either be a map or a simple list
        if (files.getEntries() instanceof HashMap) {
            HashMap<String, Files.FileEntry> fileMap = (HashMap<String, Files.FileEntry>) files.getEntries();
            for (String key : fileMap.keySet()) {
                downloadFile(recordId, fileMap.get(key), outputPath, report);
            }
        }
        else {
            ArrayList<Files.FileEntry> fileList = (ArrayList<Files.FileEntry>) files.getEntries();
            for (Files.FileEntry entry : fileList) {
                downloadFile(recordId, entry, outputPath, report);
            }
        }
        Record root = api.getRecord(recordId);
        List<Metadata.RelatedIdentifier> references = root.getMetadata().getRelatedIdentifiers();
        for (Metadata.RelatedIdentifier reference : references) {
            if (reference.getRelationType().getId().equals(new ControlledVocabulary.RelationTypeId(ControlledVocabulary.RelationTypeId.ERelationTypeId.HasPart))) {
                String id = reference.getIdentifier().replace(url, "");
                downloadObject(id, outputPath, report);
            }
        }
    }
    
    /**
     * Gets the record id for the record matching the title
     * @param recordTitle the record title
     * @return the record id
     * @throws java.io.IOException
     * @throws java.lang.InterruptedException
     * @throws java.net.URISyntaxException
     * @throws java.security.NoSuchAlgorithmException
     * @throws java.security.KeyManagementException
     */
    public String getRecordIdForTitle(String recordTitle) throws IOException, InterruptedException, URISyntaxException, NoSuchAlgorithmException, KeyManagementException {
        Records matches = api.listUserRecords(Optional.of("title:" + recordTitle), Optional.empty(), Optional.of(1), Optional.empty(), Optional.of(false));
        if (matches.getHits().getHits().size() == 1) {
            return matches.getHits().getHits().get(0).getId();
        }
        else {
            throw new IllegalArgumentException("Title matches no record: " + recordTitle);
        }
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
     * @param update flag if existing records should be updated
     * @param report the report to keep track of detailed information about the process

     * @return the id of the root node
     */
    private RecordId mappingToRecords(Path path, MapRootRecord mapping, boolean update, Report report) throws IOException, InterruptedException, URISyntaxException, NoSuchAlgorithmException, KeyManagementException, JDOMException, CloneNotSupportedException {
        // Get the metadata
        Metadata metadata;
        CmdiProfileMapping metadataMapping;
        try {
            metadataMapping = createCmdiProfileMapping(Path.of(path.toString(),mapping.getMetadata().get()).toFile().getCanonicalFile());
            metadata = readMetadata(metadataMapping);
            
        }
        catch (IOException | JDOMException e) {
            report.addException("InvenioAPI", e, "Exception while loading metadata file " + mapping.getMetadata().get());
            throw e;
        }
        // Add empty spare record for preservation management
        MapRecord preservationRecord = new MapRecord();
        String title = metadata.getTitle();
        Metadata preservationMetadata = new Metadata(new Metadata.ResourceType( new ControlledVocabulary.ResourceType(ControlledVocabulary.ResourceType.EResourceType.Other)), 
                new ArrayList<>(List.of(new Metadata.Creator(new Metadata.PersonOrOrg("Leibniz-Institut für Deutsche Sprache (IDS)")))), 
                title + ": Preservation information", 
                Metadata.ExtendedDateTimeFormat0.parseDateToExtended(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX").format(new Date()))
        );
        
        preservationMetadata.setDescription("Record for storing preservation information for " + title);
        RecordId preservationId = uploadRecord(path, preservationRecord, preservationMetadata, update, report);
        // Continue with uploade
        RecordId rootId = uploadRecord(path, mapping, metadata, update, report);
        // Fix links between root record and preservation record
        if (preservationId.isDraft() && rootId.isDraft()) {
            DraftRecord preservationDraft = api.getDraftRecord(preservationId.getId());
            DraftRecord rootDraft = api.getDraftRecord(rootId.getId());
            preservationDraft.getMetadata().addRelatedIdentifiers(
                    new ArrayList<>(List.of(
                            new Metadata.RelatedIdentifier(url + rootId,
                                    new ControlledVocabulary.RelatedRecordIdentifierScheme(ControlledVocabulary.RelatedRecordIdentifierScheme.ERelatedRecordIdentifierScheme.URL),
                                    new Metadata.RelatedIdentifier.RelationType(new ControlledVocabulary.RelationTypeId(ControlledVocabulary.RelationTypeId.ERelationTypeId.Describes),
                                            new Metadata.LocalizedStrings().add(new Metadata.Language(languageIdFactory.usingId2("en")), "Describes")))
                    )));
            rootDraft.getMetadata().addRelatedIdentifiers(
                    new ArrayList<>(List.of(
                            new Metadata.RelatedIdentifier(url + preservationId,
                                    new ControlledVocabulary.RelatedRecordIdentifierScheme(ControlledVocabulary.RelatedRecordIdentifierScheme.ERelatedRecordIdentifierScheme.URL),
                                    new Metadata.RelatedIdentifier.RelationType(new ControlledVocabulary.RelationTypeId(ControlledVocabulary.RelationTypeId.ERelationTypeId.IsDescribedBy),
                                            new Metadata.LocalizedStrings().add(new Metadata.Language(languageIdFactory.usingId2("en")), "Is described by")))
                    )));
            api.updateDraftRecord(preservationId.getId(), preservationDraft);
            api.updateDraftRecord(rootId.getId(), rootDraft);
        }
        return RecordId.newDraft(rootId.getId());
    }
    
    /**
     * Uploads a record
     * @param path the file path
     * @param record the record map
     * @param metadata the metadata
     * @param update flag if an existing record should be updated
     * @param report report to keep track of the process
     * @return the id of the resulting record
     * @throws IOException
     * @throws JDOMException
     * @throws InterruptedException
     * @throws URISyntaxException
     * @throws KeyManagementException
     * @throws NoSuchAlgorithmException
     * @throws CloneNotSupportedException 
     */
    private RecordId uploadRecord(Path path, MapRecord record, Metadata metadata, boolean update, Report report) throws IOException, JDOMException, InterruptedException, URISyntaxException, KeyManagementException, NoSuchAlgorithmException, CloneNotSupportedException {
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
        CmdiProfileMapping currentMetadataMapping = null;
        if (!record.getMetadata().isEmpty() && !record.getMetadata().get().isEmpty()) {
            try {
                currentMetadataMapping = createCmdiProfileMapping(Path.of(path.toString(),record.getMetadata().get()).toFile().getCanonicalFile());
                currentMetadata = readMetadata(currentMetadataMapping);
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
        // Check if title is already used
        Optional<String> potentiallyExistingRecordId = findRecordByTitle(currentMetadata.getTitle());
        if (potentiallyExistingRecordId.isPresent()) {
            if (update) {
                // Todo
                LOG.info("Update record " + potentiallyExistingRecordId.get());
                boolean changed = false;
                HashMap<String,String> checksums = getFileChecksums(api.listRecordFiles(potentiallyExistingRecordId.get()));
                // Add all files from recordmap to potentially new files
                Set<String> newFiles = new HashSet<>();
                // Add metadata if available
                record.getMetadata().ifPresent(newFiles::add);
                // Add all other files
                newFiles.addAll(record.getFiles().stream().map((mf) -> mf.getName()).collect(Collectors.toSet()));
                Set<String> deletedFiles = new HashSet<>();
                Set<String> updatedFiles = new HashSet<>();
                // Check if the record has changed. Only update if necessary. Alternatively we could always create a new version
                for (String filename : checksums.keySet()) {
                    if (newFiles.contains(filename)) {
                        // If the checksum does not match we know that the record
                        // has been changed
                        if (!validateChecksum(Path.of(path.toString(),filename).toFile().getAbsoluteFile(), checksums.get(filename))) {
                            changed = true;
                            updatedFiles.add(filename);
                            
                        }
                        newFiles.remove(filename);
                    }
                    // If the file is not present it must have been deleted
                    else {
                        deletedFiles.add(filename);
                        // This of course means that the record has been changed
                        changed = true;
                    }
                }
                // If we have new or deleted files we also know that the record has been changed
                changed = changed || !newFiles.isEmpty() || !deletedFiles.isEmpty();
                // Create updated record if the record has been changed
                if (changed) {
                    LOG.log(Level.INFO, "New files:\n{0} Updated files:\n{1} Removed files:\n{2}", 
                            new Object[]{newFiles.stream().collect(Collectors.joining(", ")), 
                                updatedFiles.stream().collect(Collectors.joining(", ")), 
                                deletedFiles.stream().collect(Collectors.joining(", "))});
                    // If update needed first create the new draft with all previous files
                    // First create a new version as a draft
                    DraftRecord newDraft = api.createNewVersion(potentiallyExistingRecordId.get());
                    String newDraftId = newDraft.getId().get();
                    // Import previous files if possible
                    api.draftImportFiles(newDraftId);
                    // Remove files that are missing or updated in the new version
                    for (String filename : Stream.concat(deletedFiles.stream(), updatedFiles.stream()).toList()) {
                        String fileKey = filename.replaceAll("/", SEPARATOR);
                        api.deleteDraftFile(newDraftId, fileKey);
                    }
                    // (re-)upload new version if the file has been changed or added
                    for (String filename : Stream.concat(newFiles.stream(), updatedFiles.stream()).toList()) {
                        String fileKey = filename.replaceAll("/", SEPARATOR);
                        api.startDraftFileUpload(newDraftId, new ArrayList<>(List.of(new Files.FileEntry(fileKey))));
                        api.uploadDraftFile(newDraftId, fileKey, Path.of(path.toString(),filename).toFile());
                        api.completeDraftFileUpload(newDraftId, fileKey);
                    }
                    // Update publication date
                    newDraft = api.getDraftRecord(newDraftId);
                    newDraft.getMetadata().setPublicationDate(
                            new Metadata.ExtendedDateTimeFormat0(String.valueOf(Calendar.getInstance().get(Calendar.YEAR)))
                            .addStartMonth(String.format("%02d", Calendar.getInstance().get(Calendar.MONTH)+1))
                            .addStartDay(String.format("%02d", Calendar.getInstance().get(Calendar.DAY_OF_MONTH))));
                    api.updateDraftRecord(newDraftId, newDraft);
                    return RecordId.newDraft(newDraftId);
                }
                // Otherwise just return the existing id
                else {
                    report.addCorrect("InvenioAPI", "Object " + potentiallyExistingRecordId.get() + " already up-to-date");
                    LOG.log(Level.INFO, "Object {0} already up-to-date", potentiallyExistingRecordId.get());
                    return RecordId.newRecord(potentiallyExistingRecordId.get());
                }
            }
            else {
                LOG.log(Level.SEVERE, "Record with title already exist: {0}", currentMetadata.getTitle());
                throw new IllegalArgumentException("Record with title already exist: " + currentMetadata.getTitle());
            }
        }
        else {
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
                RecordId id = uploadRecord(path, child, metadata, update, report);
                relatedIdentifiers.add(new Metadata.RelatedIdentifier(url + id.getId(),
                        new ControlledVocabulary.RelatedRecordIdentifierScheme(ControlledVocabulary.RelatedRecordIdentifierScheme.ERelatedRecordIdentifierScheme.URL),
                        new Metadata.RelatedIdentifier.RelationType(new ControlledVocabulary.RelationTypeId(ControlledVocabulary.RelationTypeId.ERelationTypeId.HasPart),
                                new Metadata.LocalizedStrings().add(new Metadata.Language(languageIdFactory.usingId2("en")), "Has part"))));
                
            }
            // Add references and potentially default preview
            currentMetadata.addRelatedIdentifiers(relatedIdentifiers);
            if (!defaultPreview.isBlank()) {
                draft.getFiles().setDefaultPreview(defaultPreview);
            }
            // Add external pids as additional identifiers
            List<Metadata.AlternateIdentifier> pids = new ArrayList<>();
            if (currentMetadataMapping != null && currentMetadataMapping.getSelfLink().isPresent()) {
                String selfLink = currentMetadataMapping.getSelfLink().get();
                if (selfLink.contains("handle.net")) {
                    Matcher m = Pattern.compile("https://hdl.handle.net/(.*)").matcher(selfLink);
                    if (m.matches()) {
                        String pid = m.group(1);
                        pids.add(new Metadata.AlternateIdentifier(pid, 
                                new ControlledVocabulary.RecordIdentifierScheme(ControlledVocabulary.RecordIdentifierScheme.ERecordItentifierScheme.Handle)));
                    }
                    else {
                        throw new IllegalArgumentException("Unable to extract pid from " + selfLink);
                    }
                }
                else if (selfLink.contains("hdl:")) {
                    Matcher m = Pattern.compile("hdl:/(.*)").matcher(selfLink);
                    if (m.matches()) {
                        String pid = m.group(1);
                        pids.add(new Metadata.AlternateIdentifier(pid, 
                                new ControlledVocabulary.RecordIdentifierScheme(ControlledVocabulary.RecordIdentifierScheme.ERecordItentifierScheme.Handle)));
                    }
                    else {
                        throw new IllegalArgumentException("Unable to extract pid from " + selfLink);
                    }
                }
                else if (selfLink.contains("ark:")) {
                    Matcher m = Pattern.compile(".*ark:/?(.*)").matcher(selfLink);
                    if (m.matches()) {
                        String pid = m.group(1);
                        pids.add(new Metadata.AlternateIdentifier(pid,
                            new ControlledVocabulary.RecordIdentifierScheme(ControlledVocabulary.RecordIdentifierScheme.ERecordItentifierScheme.ARK)));
                    }
                    else {
                        throw new IllegalArgumentException("Unable to extract pid from " + selfLink);
                    }
                }
                currentMetadata.addAlternativeIdentifiers(pids);
                
            }
            api.updateDraftRecord(result.getId(), draft);
            return RecordId.newDraft(result.getId());
        }
    }
    
    /**
     * Create a CMDI metadata mapping suitable for the metadata file
     * @param metadataFile the file to be mapped
     * @return the CMDI metadata mapping
     * @throws JDOMException
     * @throws IOException 
     */
    private CmdiProfileMapping createCmdiProfileMapping(File metadataFile) throws JDOMException, IOException {
        // Read the CMDI file
        Document document = new SAXBuilder().build(metadataFile);
        XmlMagic magic = new XmlMagic(document);
        for (MimeType mt : magic.getMimeTypes()) {
            // Find the CMDI mime type
            if (mt.getSubtype().equals("x-cmdi")) {
                
                switch (mt.getParameters().get("profile")) {
                    // Speech corpus profile
                    case "clarin.eu:cr1:p_1527668176128":
                        return new SpeechCorpusProfileMapper(document);
                    // Text corpus profile
                    case "clarin.eu:cr1:p_1559563375778":
                        return new TextCorpusProfileMapper(document);
                    // OLAC DCMI terms
                    case "clarin.eu:cr1:p_1366895758244":
                        return new OLACDcmiTermsMapper(document);
                    case "clarin.eu:cr1:p_1659015263839":
                        return new CollectionProfileMapper(document);
                    default:
                        throw new IOException("Unsupported CMDI profile " + mt.getParameters().get("profile"));
                }
            }
        }
        throw new IOException("Unrecognized CMDI file or profile");
    }
    /**
     * Read the Invenio metadata from a CMDI file
     * @param metadataMapping the metadata file
     * @return the Invenio metadata
     * @throws IOException
     * @throws JDOMException 
     */
    private Metadata readMetadata(CmdiProfileMapping metadataMapping) throws IOException, JDOMException {
        // Read the CMDI file
        return CMDI.readCmdiMetadata(metadataMapping);
    }
    
    /**
     * Validates all draft records against the initial bag
     * @param id the record id
     * @param path the root of the file hierarchy
     * @param bag the initial bag
     * @param report the report to keep track of detailed information about the process
     * @return if the draft records match the input data
     */
    private boolean validateRecords(RecordId id, Path path, Bag bag, Report report) throws URISyntaxException, NoSuchAlgorithmException, IOException, JsonProcessingException, KeyManagementException, InterruptedException {
        // if id is null it means non-existent record and we know that something went wrong
        if (id == null) {
            return false;
        }
        Files files;
        if (id.isDraft()) {
            files = api.listDraftFiles(id.getId());
        }
        else {
            files = api.listRecordFiles(id.getId());
        }
        HashMap<String,String> checksums = getFileChecksums(files);
        boolean result = true;
        LOG.log(Level.INFO, "Validating record {0}", id);
        // Check checksums for all files
        for (Map.Entry<String, String> entry : checksums.entrySet()) {
            String filename = entry.getKey();
            File file = Path.of(path.toString(),filename).toFile().getAbsoluteFile();
            String checksum = entry.getValue();
            
            result = result && validateChecksum(file, checksum); //newSum.equalsIgnoreCase(checksum);
        }
        // Add info about results to the report
        if (result) {
            report.addCorrect("InvenioAPI", "Sucessfuly validated record " + id);
        }
        else {
            report.addCritical("InvenioAPI", "Failed to validate record " + id);
        }
        // Also validate all related records
        List<Metadata.RelatedIdentifier> idList;
        if (id.isDraft()) {
            idList = api.getDraftRecord(id.getId()).getMetadata().getRelatedIdentifiers();
        }
        else {
            idList = api.getRecord(url).getMetadata().getRelatedIdentifiers();
        }
        for (Metadata.RelatedIdentifier relatedId : idList) {
            if (relatedId.getRelationType().getId().toString().equalsIgnoreCase("haspart")) {
                String relId = relatedId.getIdentifier().replace(url, "");
                result = result && validateRecords(new RecordId(isDraft(relId),relId), path, bag, report);
            }
        }
        return result;
    }
    
    /**
     * Publish all unpublished draft records
     * @param report the corpus service report
     * @throws URISyntaxException
     * @throws NoSuchAlgorithmException
     * @throws KeyManagementException
     * @throws IOException
     * @throws InterruptedException 
     */
    private void publishDraftRecords(Report report) throws URISyntaxException, NoSuchAlgorithmException, KeyManagementException, IOException, InterruptedException {
        ArrayList<String> failed = new ArrayList<>();
        for (String id : listDraftRecords()) {
            DraftRecord result = api.publishDraftRecord(id);
            if (result.getIsPublished().orElse(Boolean.FALSE)) {
                report.addCorrect("InvenioAPI", "Published record " + id);
                LOG.log(Level.INFO, "Publish record {0}", id);
            }
            else {
                LOG.log(Level.SEVERE, "Failed to publish {0}", id);
                failed.add(id);
            }
        }
        if (!failed.isEmpty()) {
            report.addCritical("Failed to publish records " + failed.toString());
        }
    }
    
    /**
     * Lists the id of all of the current users draft records
     * @return the list of draft record ids
     * @throws java.io.IOException
     * @throws java.lang.InterruptedException
     * @throws java.net.URISyntaxException
     * @throws java.security.KeyManagementException
     * @throws java.security.NoSuchAlgorithmException
     */
    private List<String> listDraftRecords() throws IOException, InterruptedException, URISyntaxException, NoSuchAlgorithmException, KeyManagementException {
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

    /**
     * Download one specific file from a record to an output path
     * 
     * @param recordId the record id
     * @param fileEntry the file entry
     * @param outputPath the output path
     */
    private void downloadFile(String recordId, Files.FileEntry fileEntry, Path outputPath, Report report) {
        try {
        String fileName = fileEntry.getKey();
        File outputFile = Path.of(outputPath.toString(), fileName.replaceAll(SEPARATOR, "/")).toFile();
        // Create directories if necessary
        outputFile.getParentFile().mkdirs();
        // Download file
        api.getRecordFileContent(recordId, fileName).transferTo(new FileOutputStream(outputFile));
        if (validateChecksum(outputFile, fileEntry.getChecksum())) {
            report.addCorrect("InvenioAPI", "Downloaded file " + outputFile);
        }
        else {
            report.addCritical("InvenioAPI", "Failed to download " + outputFile);
        }
        }
        catch (Exception e) {
            report.addException("InvenioAPI", e, "Exception when downloading file " + fileEntry.getKey());
        }
    }

    /**
     * Computes the checksum for a file and compares it to a given checksum
     * @param file the file
     * @param checksum the checksum of the form `algorithm:checksum`
     * @return if the checksums match
     * @throws NoSuchAlgorithmException
     * @throws FileNotFoundException
     * @throws IOException 
     */
    private boolean validateChecksum(File file, String checksum) throws NoSuchAlgorithmException, FileNotFoundException, IOException {
        String[] parts = checksum.split(":");
        // Match the digest algorithm with the one specified in the beginning of the provided checksum
        MessageDigest md = MessageDigest.getInstance(parts[0]);
        md.reset();
        md.update(new FileInputStream(file).readAllBytes());
        String newSum = parts[0]+":" + DatatypeConverter.printHexBinary(md.digest());
        return newSum.equalsIgnoreCase(checksum);
    }

    /**
     * Finds a unique record with a given title
     * @param title the record title
     * @return the record id if it exists
     * @throws IOException
     * @throws InterruptedException
     * @throws URISyntaxException
     * @throws NoSuchAlgorithmException
     * @throws KeyManagementException
     */
    private Optional<String> findRecordByTitle(String title) throws IOException, InterruptedException, URISyntaxException, NoSuchAlgorithmException, KeyManagementException {
        Records matches = api.listUserRecords(Optional.of("metadata.title:\"" +title+ "\""), Optional.empty(), Optional.of(1), Optional.empty(), Optional.empty());
        if (matches.getHits().getHits().size() == 1 && matches.getHits().getHits().get(0).getMetadata().getTitle().equals(title)) {
            return Optional.of(matches.getHits().getHits().get(0).getId());
        }
        else if (matches.getHits().getHits().isEmpty() || !matches.getHits().getHits().get(0).getMetadata().getTitle().equals(title)) {
            return Optional.empty();
        }
        else {
            throw new IllegalArgumentException("More than one record found matching title");
        }
    }

    /**
     * Creates a map from filename to checksum given a record or draft file list
     * @param files the file list
     * @return the map from filename to checksum
     */
    private HashMap<String, String> getFileChecksums(Files files) {
        HashMap<String, String> checksums = new HashMap<>();
        if (files.getEntries() instanceof List) {
            for (Files.FileEntry entry : (List<Files.FileEntry>) files.getEntries()) {
                checksums.put(entry.getKey().replaceAll(SEPARATOR, "/"), entry.getChecksum());
            }
        }
        else {
            for (String key : ((Map<String,Files.FileEntry>) files.getEntries()).keySet()) {
                Files.FileEntry entry = ((Map<String,Files.FileEntry>) files.getEntries()).get(key);
                checksums.put(entry.getKey().replaceAll(SEPARATOR, "/"), entry.getChecksum());
            }
        }
        return checksums;
    }

    /***
     * Helper to check if an id is a draft
     * @param id the id of the record
     * @return if it is a draft
     * @throws IOException
     * @throws InterruptedException
     * @throws URISyntaxException
     * @throws NoSuchAlgorithmException
     * @throws KeyManagementException 
     */
    private boolean isDraft(String id) throws IOException, InterruptedException, URISyntaxException, NoSuchAlgorithmException, KeyManagementException {
        return listDraftRecords().contains(id);
    }

    /**
     * Class keeping track if the id is a draft or an already published record
     */
    private static class RecordId {
        // Flag if the ID is for a record
        boolean draft;
        // The ID itself
        private String id;

        /***
         * Private constructor, use static method instead
         * @param draft
         * @param id 
         */ 
        private RecordId(boolean draft, String id) {
            this.draft = draft;
            this.id = id;
        }
        
        /***
         * Create a new draft record id
         * @param id the new id as string
         * @return the id object
         */
        public static RecordId newDraft(String id) {
            return new RecordId(true, id);
        }
        
        /***
         * Create a new record id
         * @param id the new id as string
         * @return  the id object
         */
        public static RecordId newRecord(String id) {
            return new RecordId(false, id);
        }

        public boolean isDraft() {
            return draft;
        }

        public String getId() {
            return id;
        }

        @Override
        public String toString() {
            return "RecordId{" + "draft=" + draft + ", id=" + id + '}';
        }
    }
}
