/*
* Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
* Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
*/
package de.idsmannheim.lza.utilities.publication;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import de.idsmannheim.lza.datacitejavaapi.DataciteAPI;
import de.idsmannheim.lza.datacitejavaapi.DataciteAPITools;
import de.idsmannheim.lza.inveniojavaapi.InvenioAPI;
import de.idsmannheim.lza.inveniojavaapi.Access;
import de.idsmannheim.lza.inveniojavaapi.CMDI;
import de.idsmannheim.lza.inveniojavaapi.ControlledVocabulary;
import de.idsmannheim.lza.inveniojavaapi.DraftRecord;
import de.idsmannheim.lza.inveniojavaapi.Files;
import de.idsmannheim.lza.inveniojavaapi.FilesOptions;
import de.idsmannheim.lza.inveniojavaapi.InvenioAPITools;
import de.idsmannheim.lza.inveniojavaapi.Metadata;
import de.idsmannheim.lza.inveniojavaapi.Record;
import de.uni_hamburg.corpora.Corpus;
import de.uni_hamburg.corpora.Report;
import de.idsmannheim.lza.utilities.publication.mapper.MapFile;
import de.idsmannheim.lza.utilities.publication.mapper.MapRecord;
import de.idsmannheim.lza.utilities.publication.mapper.MapRootRecord;
import de.idsmannheim.lza.validation.CheckBag;
import gov.loc.repository.bagit.domain.Bag;
import gov.loc.repository.bagit.reader.BagReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.security.KeyManagementException;
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
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpressionException;
import net.sf.saxon.s9api.SaxonApiException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.FileFileFilter;
import org.datacite.ApiException;
import org.datacite.api.model.Doi;
import org.exmaralda.partitureditor.fsm.FSMException;
import org.exmaralda.partitureditor.jexmaralda.JexmaraldaException;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.xml.sax.SAXException;


/**
 * Class providing higher-level abstraction helpers based on Invenio API calls
 * @author Herbert Lange <lange@ids-mannheim.de>
 */
public class InvenioTools {
    
    // Mutex to only allow one operation at a time
    private static final ReentrantLock mutex = new ReentrantLock();
    
    // Information how to find the metadata file if no record map exists
    static final String METADATA_DIR = "data/Metadata";
    static final String METADATA_FILE = "metadata.cmdi";
    static final String RECORD_MAP_FILE = "recordmap.xml";
    //static final String RECORD_MAP_FILE = "recordmap.json";
    // Path separator to be used instead of / which causes problems in URLs
    static final String SEPARATOR = ">";
    // Regex to check if a PID url is just a placeholder
    static final String PID_PLACEHOLDER = ".*NOTYET.*";
    ControlledVocabulary.LanguageIdFactory languageIdFactory;
    InvenioAPI api;
    String url;
    de.idsmannheim.lza.inveniojavaapi.InvenioAPITools tools;
    private static final Logger LOG = Logger.getLogger(InvenioTools.class.getName());

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
    public InvenioTools(InvenioAPI api) throws IOException {
        this.languageIdFactory = new ControlledVocabulary.LanguageIdFactory();
        this.api = api;
        this.tools = new InvenioAPITools(api);
        url = api.protocol + "://" + api.host + "/records/";
    }
    
    /**
     * Adds files from a path to a new Invenio object.This will result in one or several
     * new records
     * @param c the Corpus object to be added
     * @param datacite the optional DataciteAPI object for handling DOI minting
     * @param datacitePrefix the optional Datacite prefix for DOI minting
     * @param filesArePublic flag if files should be public if no specific information is present
     * @param separatePrivateRecords flag if private files should be stored in a separate record
     * @param update flag if existing records with the same title should be updated
     * @param publishRecords Flag if Invenio records should be published. This should usually be true except for testsing
     * @param publishDois flag if DOIs should be published, i.e. set to registered
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
    public Optional<String> createOrUpdateObject(Corpus c, Optional<DataciteAPI> datacite, Optional<String> datacitePrefix, boolean filesArePublic, boolean separatePrivateRecords, boolean update, boolean publishRecords, boolean publishDois, Report report) throws JAXBException, IOException, URISyntaxException, NoSuchAlgorithmException, KeyManagementException, InterruptedException, KeyManagementException, JDOMException, CloneNotSupportedException {
        Path path = Path.of(c.getBaseDirectory().toURI());
        LOG.info("Validate data before ingest");
        // Get the mapping from files to Invenio records
        MapRootRecord mapping = getMapping(path, filesArePublic, separatePrivateRecords);
        // Validate the mapping
        boolean validBag = false;
        try {
            validBag = validateBag(c, report);
        }
        catch (Exception e) {
            report.addException("InvenioAPI", e, "Unexpected exception when validating bag");
        }
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
                        // Mint DOIs and update CMDIs
                        if (datacite.isPresent() && datacitePrefix.isPresent()) {
                            mintDois(datacite.get(), datacitePrefix.get(), report);
                            updateCmdis(report);
                        }
                        // Publish all drafts
                        LOG.info("Publish records");
                        // Publish all records that have been changed
                        if (publishRecords) {
                            publishRecords(tools.listEditedRecords(),report);
                        }
                        LOG.info("Publish DOIs");
                        if (datacite.isPresent() && datacitePrefix.isPresent() && publishDois) {
                            publishDois(datacite.get(), datacitePrefix.get(), report);
                        }
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
                LOG.info(e.getMessage());
                e.printStackTrace();
            }
            // If we are here something went wrong and we have to revert to the initial state
            LOG.severe("Rollback");
            rollback(datacite, datacitePrefix);
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
        boolean success = true;
        if (files.getEntries() instanceof HashMap) {
            HashMap<String, Files.FileEntry> fileMap = (HashMap<String, Files.FileEntry>) files.getEntries();
            for (String key : fileMap.keySet()) {
                success |= tools.downloadFile(recordId, fileMap.get(key), outputPath, SEPARATOR);
            }
        }
        else {
            ArrayList<Files.FileEntry> fileList = (ArrayList<Files.FileEntry>) files.getEntries();
            for (Files.FileEntry entry : fileList) {
                success |= tools.downloadFile(recordId, entry, outputPath, SEPARATOR);
            }
        }
        if (!success) {
            report.addCritical("Error when downloading files");
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
    

    
    //------------------------------------------------------------------------//
    //                                                                        //
    // Private helper functions                                               //
    //                                                                        //
    //------------------------------------------------------------------------//
    
    /**
     * Gets a mapping from a path, either by reading the mapping specification or
     * by creating one ad-hoc
     * @param path the input path
     * @param filesArePublic flag if files should be public by default when creating a new mapping. Ignored when a explicitly stated
     * @param separatePrivateRecords flag if private files should be stored in a separate record
     * @return the mapping
     */
    private MapRootRecord getMapping(Path path, boolean filesArePublic, boolean separatePrivateRecords) throws JAXBException, IOException {
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
        if (separatePrivateRecords) {
            mapping = (MapRootRecord) MapRecord.separatePrivateFiles(mapping);
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
     * @param c The Corpus object containing the SIP
     * @param report the report to keep track of detailed information about the process
     * @return if it is a valid BagIt
     */
    private boolean validateBag(Corpus c, Report report) throws MalformedURLException, JexmaraldaException, URISyntaxException, IOException, ClassNotFoundException, SAXException, NoSuchAlgorithmException, FSMException, ParserConfigurationException, TransformerException, XPathExpressionException, org.jdom.JDOMException {
        Report r = new Report();
        // Create new properties to pass the parameter
        Properties props = new Properties();
        props.setProperty("fetch-files", "true");
        // Create checker
        CheckBag bagChecker = new CheckBag(props);
        // Run the checker and merge reports
        r.merge(bagChecker.function(c, Boolean.FALSE));
        report.merge(r);
        // If we encountered no problems then the Bag is valid
        return r.getErrorStatistics().isEmpty();
    }
    
    /**
     * Uploads all files given in a map record to Invenio
     * @param mapping the mapping to be uploaded
     * @param update flag if existing records should be updated
     * @param report the report to keep track of detailed information about the process

     * @return the id of the root node
     */
    private RecordId mappingToRecords(Path path, MapRootRecord mapping, boolean update, Report report) throws IOException, InterruptedException, URISyntaxException, NoSuchAlgorithmException, KeyManagementException, JDOMException, CloneNotSupportedException, IllegalArgumentException, SaxonApiException {
        // Get the metadata
        Metadata metadata;
        try {
            metadata = tools.readMetadata(Path.of(path.toString(),mapping.getMetadata().get()).toFile().getCanonicalFile());
            sanitizeAlternateIdentifiers(metadata);
            
        }
        catch (IOException | JDOMException e) {
            report.addException("InvenioAPI", e, "Exception while loading metadata file " + mapping.getMetadata().get());
            throw e;
        }
        // Start with the root
        RecordId rootId = uploadRecord(path, mapping, metadata, Optional.empty(), update, report);
        // Add empty spare record for preservation management
        MapRecord preservationRecord = new MapRecord();
        String title = metadata.getTitle();
        Metadata preservationMetadata = new Metadata(new Metadata.ResourceType( new ControlledVocabulary.ResourceType(ControlledVocabulary.ResourceType.EResourceType.Other)), 
                new ArrayList<>(List.of(new Metadata.Creator(new Metadata.PersonOrOrg("Leibniz-Institut für Deutsche Sprache (IDS)")))), 
                title + ": Preservation information", 
                Metadata.ExtendedDateTimeFormat0.parseDateToExtended(new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX").format(new Date()))
        );
        
        preservationMetadata.setDescription("Record for storing preservation information for " + title);
        Optional<String> potentiallyExistingPreservationRecord = tools.findRecordByTitle(preservationMetadata.getTitle());
        RecordId preservationId;
        DraftRecord preservationDraft;
        if (potentiallyExistingPreservationRecord.isEmpty()) {
            preservationId = uploadRecord(path, preservationRecord, preservationMetadata, Optional.empty(), update, report);
            preservationDraft = api.getDraftRecord(preservationId.getId());
        }
        else {
            preservationDraft = api.createDraftFromPublished(potentiallyExistingPreservationRecord.get());
            preservationId = new RecordId(true, preservationDraft.getId().get());
        }
        
        // Continue with upload
        // Fix links between root record and preservation record
        if (rootId.isDraft()) {
            
            DraftRecord rootDraft = api.getDraftRecord(rootId.getId());
            preservationDraft.getMetadata().addRelatedIdentifiers(
                    new ArrayList<>(List.of(
                            new Metadata.RelatedIdentifier(url + rootId.getId(),
                                    new ControlledVocabulary.RelatedRecordIdentifierScheme(ControlledVocabulary.RelatedRecordIdentifierScheme.ERelatedRecordIdentifierScheme.URL),
                                    new Metadata.RelatedIdentifier.RelationType(new ControlledVocabulary.RelationTypeId(ControlledVocabulary.RelationTypeId.ERelationTypeId.Describes),
                                            new Metadata.LocalizedStrings().add(new Metadata.Language(languageIdFactory.usingId2("en")), "Describes")))
                    )));
            rootDraft.getMetadata().addRelatedIdentifiers(
                    new ArrayList<>(List.of(
                            new Metadata.RelatedIdentifier(url + preservationId.getId(),
                                    new ControlledVocabulary.RelatedRecordIdentifierScheme(ControlledVocabulary.RelatedRecordIdentifierScheme.ERelatedRecordIdentifierScheme.URL),
                                    new Metadata.RelatedIdentifier.RelationType(new ControlledVocabulary.RelationTypeId(ControlledVocabulary.RelationTypeId.ERelationTypeId.IsDescribedBy),
                                            new Metadata.LocalizedStrings().add(new Metadata.Language(languageIdFactory.usingId2("en")), "Is described by")))
                    )));
            api.updateDraftRecord(preservationId.getId(), preservationDraft);
            api.updateDraftRecord(rootId.getId(), rootDraft);
//            api.publishDraftRecord(preservationId.getId());
        }
        return RecordId.newDraft(rootId.getId());
    }
    
    /**
     * Uploads a record
     * @param path the file path
     * @param record the record map
     * @param metadata the metadata
     * @param parentId the optional id of a parent record
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
    private RecordId uploadRecord(Path path, MapRecord record, Metadata metadata, Optional<RecordId> parentId, boolean update, Report report) throws IOException, JDOMException, InterruptedException, URISyntaxException, KeyManagementException, NoSuchAlgorithmException, CloneNotSupportedException, IllegalArgumentException, SaxonApiException {
        // Create draft record
        DraftRecord draft;
        String draftId;
        // Set access. Metadata is always public and file access depends:
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
        // Get the metadata file from the record mapping
        if (!record.getMetadata().isEmpty() && !record.getMetadata().get().isEmpty()) {
            try {
                currentMetadata = tools.readMetadata(Path.of(path.toString(),record.getMetadata().get()).toFile().getCanonicalFile());
                sanitizeAlternateIdentifiers(currentMetadata);
            }
            catch (IOException | IllegalArgumentException | SaxonApiException e) {
                report.addException("InvenioAPI", e, "Exception while loading metadata");
                throw e;
            }
        }
        // Otherwise clone the current metadata
        else {
            try {
                currentMetadata = (Metadata) metadata.clone();
            }
            catch (CloneNotSupportedException e) {
                report.addException("InvenioAPI", e, "Exception when cloning metadata");
                throw e;
            }
        }
        // Update the record title if one is given in the record map
        if (record.getTitle().isPresent() && !record.getTitle().get().isEmpty()) {
            currentMetadata.setTitle(metadata.getTitle() + ": " + record.getTitle().get());
        }
        // Check if title is already used
        Optional<String> potentiallyExistingRecordId = tools.findRecordByTitle(currentMetadata.getTitle());
        if (potentiallyExistingRecordId.isPresent()) {
            // The title already exists and we want to update records
            if (update) {
                LOG.log(Level.INFO, "Update record {0}", potentiallyExistingRecordId.get());
                // Check if the record has changed, i.e. if either new files are
                // added or we have files where the checksum has changed
                boolean changed = false;
                HashMap<String,String> checksums = getFileChecksums(api.listRecordFiles(potentiallyExistingRecordId.get()));
                // Add all files from recordmap to potentially new files
                Set<String> newFiles = new HashSet<>();
                // Add metadata file to file listif available
                record.getMetadata().ifPresent(newFiles::add);
                // Add all other files
                newFiles.addAll(record.getFiles().stream().map((mf) -> mf.getName()).collect(Collectors.toSet()));
                // Keep track of updated or deleted files
                Set<String> deletedFiles = new HashSet<>();
                Set<String> updatedFiles = new HashSet<>();
                // Check if the record has changed. Only update if necessary.
                // Alternatively we could always create a new version but we
                // want to keep the number of versions down
                for (String filename : checksums.keySet()) {
                    if (newFiles.contains(filename)) {
                        // If the checksum does not match we know that the record
                        // has been changed
                        if (!tools.validateChecksum(Path.of(path.toString(),filename).toFile().getAbsoluteFile(), checksums.get(filename))) {
                            changed = true;
                            updatedFiles.add(filename);
                            
                        }
                        // But it is not a new file
                        newFiles.remove(filename);
                    }
                    // If the file is not present in the new file list it must
                    // have been deleted
                    else {
                        deletedFiles.add(filename);
                        // This of course means that the record has been changed
                        changed = true;
                    }
                }
                // If we have new  files we also know that the record has been changed
                changed = changed || !newFiles.isEmpty();
                // Create updated record if the record has been changed
                if (changed) {
                    LOG.log(Level.INFO, "New files:\n{0} Updated files:\n{1} Removed files:\n{2}", 
                            new Object[]{newFiles.stream().collect(Collectors.joining(", ")), 
                                updatedFiles.stream().collect(Collectors.joining(", ")), 
                                deletedFiles.stream().collect(Collectors.joining(", "))});
                    // If update needed first create the new draft with all previous files
                    // First create a new version as a draft
                    draft = api.createNewVersion(potentiallyExistingRecordId.get());
                    // This just copies the previous default previewer but replaces
                    // it with a new metadata file if the filename is different
                    String defaultPreview = draft.getFiles().getDefaultPreview();
                    if (record.getMetadata().isPresent() && defaultPreview != null && !defaultPreview.equals(tools.normalizeFilename(record.getMetadata().get(),SEPARATOR))) {
                        defaultPreview = tools.normalizeFilename(record.getMetadata().get(),SEPARATOR);
                    }
                    draftId = draft.getId().get();
                    // Import previous files if possible
                    api.draftImportFiles(draftId);
                    // Remove files that are missing or updated in the new version
                    for (String filename : Stream.concat(deletedFiles.stream(), updatedFiles.stream()).toList()) {
                        String fileKey = tools.normalizeFilename(filename, SEPARATOR);
                        api.deleteDraftFile(draftId, fileKey);
                    }
                    // (re-)upload new version if the file has been changed or added
                    tools.uploadDraftFiles(draftId, path, new ArrayList<>(Stream.concat(newFiles.stream(), updatedFiles.stream()).toList()),SEPARATOR);
                    draft.getFiles().setDefaultPreview(defaultPreview);
                    // Update publication date
                    draft.getMetadata().setPublicationDate(
                            new Metadata.ExtendedDateTimeFormat0(String.valueOf(Calendar.getInstance().get(Calendar.YEAR)))
                            .addStartMonth(String.format("%02d", Calendar.getInstance().get(Calendar.MONTH)+1))
                            .addStartDay(String.format("%02d", Calendar.getInstance().get(Calendar.DAY_OF_MONTH))));
                }
                // Otherwise just create a new draft
                else {
                    report.addCorrect("InvenioAPI", "Object " + potentiallyExistingRecordId.get() + " already up-to-date");
                    LOG.log(Level.INFO, "Object {0} already up-to-date", potentiallyExistingRecordId.get());
                    // return RecordId.newRecord(potentiallyExistingRecordId.get());
                    LOG.log(Level.INFO, "Create new draft from {0}", potentiallyExistingRecordId.get());
                    draft = api.createDraftFromPublished(potentiallyExistingRecordId.get());
                    if (parentId.isPresent()) {
                        draft.getMetadata().addRelatedIdentifiers(List.of(
                                new Metadata.RelatedIdentifier(url + parentId.get().getId(),
                                        new ControlledVocabulary.RelatedRecordIdentifierScheme(ControlledVocabulary.RelatedRecordIdentifierScheme.ERelatedRecordIdentifierScheme.URL),
                                        new Metadata.RelatedIdentifier.RelationType(new ControlledVocabulary.RelationTypeId(ControlledVocabulary.RelationTypeId.ERelationTypeId.IsPartOf),
                                                new Metadata.LocalizedStrings().add(new Metadata.Language(languageIdFactory.usingId2("en")), "Is part of")))
                        ));
                    }
                    draftId = draft.getId().get();
                    api.updateDraftRecord(draftId, draft);
//                    String newId = api.publishDraftRecord(draftId).getId().get();
//                    return new RecordId(false, newId);
                    return new RecordId(true, draftId);
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
            draft = new DraftRecord(access, files, currentMetadata);
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
            ArrayList<String> fileNames = new ArrayList<>();
            // Potential default preview file
            String defaultPreview = "";
            String metadataFile = "";
            // Add metadata file if it exists
            if (record.getMetadata().isPresent()) {
                metadataFile = record.getMetadata().get();
                defaultPreview = tools.normalizeFilename(metadataFile,SEPARATOR);
                // Skip CMDI file in record for private files
                if (!record.getTitle().orElse("").endsWith(" - Private files")) {
                    fileNames.add(metadataFile);
                }
            }
            // Add all other public files
            // fileNames.addAll(record.getFiles().stream().filter(MapFile::isPublic).map(MapFile::getName).toList());
            // Add all other files
            fileNames.addAll(record.getFiles().stream().map(MapFile::getName).toList());
            // Upload all public files to the draft
            tools.uploadDraftFiles(result.getId(),path,fileNames,SEPARATOR);
            // Potentially add default preview
            if (!defaultPreview.isBlank()) {
                draft.getFiles().setDefaultPreview(defaultPreview);
            }
            // Add external pids as additional identifiers
            List<Metadata.AlternateIdentifier> pids = new ArrayList<>();
            // First check if we actually have a metadata file
            if (!metadataFile.isBlank()) {
                // Try to get self link
                Optional<String> selfLink = CMDI.getSelfLink(CMDI.readCmdiFile(Path.of(path.toString(),metadataFile).toFile()));
                // Check if the self link exist and if it contains a certain string marking it as a placeholder
                if (selfLink.isPresent() && !selfLink.get().matches(PID_PLACEHOLDER)) {
                    if (selfLink.get().contains("handle.net")) {
                        Matcher m = Pattern.compile("https://hdl.handle.net/(.*)").matcher(selfLink.get());
                        if (m.matches()) {
                            String pid = m.group(1);
                            pids.add(new Metadata.AlternateIdentifier(pid,
                                    new ControlledVocabulary.RecordIdentifierScheme(ControlledVocabulary.RecordIdentifierScheme.ERecordItentifierScheme.Handle)));
                        }
                        else {
                            throw new IllegalArgumentException("Unable to extract pid from " + selfLink);
                        }
                    }
                    else if (selfLink.get().contains("hdl:")) {
                        Matcher m = Pattern.compile("hdl:/(.*)").matcher(selfLink.get());
                        if (m.matches()) {
                            String pid = m.group(1);
                            pids.add(new Metadata.AlternateIdentifier(pid,
                                    new ControlledVocabulary.RecordIdentifierScheme(ControlledVocabulary.RecordIdentifierScheme.ERecordItentifierScheme.Handle)));
                        }
                        else {
                            throw new IllegalArgumentException("Unable to extract pid from " + selfLink);
                        }
                    }
                    else if (selfLink.get().contains("ark:")) {
                        Matcher m = Pattern.compile(".*ark:/?(.*)").matcher(selfLink.get());
                        if (m.matches()) {
                            String pid = m.group(1);
                            pids.add(new Metadata.AlternateIdentifier(pid,
                                    new ControlledVocabulary.RecordIdentifierScheme(ControlledVocabulary.RecordIdentifierScheme.ERecordItentifierScheme.ARK)));
                        }
                        else {
                            throw new IllegalArgumentException("Unable to extract pid from " + selfLink);
                        }
                    }
                currentMetadata.addAlternateIdentifiers(pids);
                }
            }
            draftId = result.getId();
        }
        // Upload child records and add links between the records
        ArrayList<Metadata.RelatedIdentifier> relatedIdentifiers = new ArrayList<>();
        
        for (MapRecord child : record.getRecords()) {
            RecordId id = uploadRecord(path, child, metadata, Optional.of(RecordId.newDraft(draftId)), update, report);
            relatedIdentifiers.add(new Metadata.RelatedIdentifier(url + id.getId(),
                    new ControlledVocabulary.RelatedRecordIdentifierScheme(ControlledVocabulary.RelatedRecordIdentifierScheme.ERelatedRecordIdentifierScheme.URL),
                    new Metadata.RelatedIdentifier.RelationType(new ControlledVocabulary.RelationTypeId(ControlledVocabulary.RelationTypeId.ERelationTypeId.HasPart),
                            new Metadata.LocalizedStrings().add(new Metadata.Language(languageIdFactory.usingId2("en")), "Has part"))));
            
        }
        if (parentId.isPresent()) {
            relatedIdentifiers.add(new Metadata.RelatedIdentifier(url + parentId.get().getId(),
                    new ControlledVocabulary.RelatedRecordIdentifierScheme(ControlledVocabulary.RelatedRecordIdentifierScheme.ERelatedRecordIdentifierScheme.URL),
                    new Metadata.RelatedIdentifier.RelationType(new ControlledVocabulary.RelationTypeId(ControlledVocabulary.RelationTypeId.ERelationTypeId.IsPartOf),
                            new Metadata.LocalizedStrings().add(new Metadata.Language(languageIdFactory.usingId2("en")), "Is part of"))));
        }
        draft.getMetadata().setRelatedIdentifiers(relatedIdentifiers);
        api.updateDraftRecord(draftId, draft);
        return RecordId.newDraft(draftId);
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
        boolean result = true;
        LOG.log(Level.INFO, "Validating record {0}", id);
//        Files files = new Files(false);
        if (id.isDraft()) {
            Files files = api.listDraftFiles(id.getId());
            HashMap<String,String> checksums = getFileChecksums(files);
            // Check checksums for all files
            for (Map.Entry<String, String> entry : checksums.entrySet()) {
                String filename = entry.getKey();
                File file = Path.of(path.toString(),filename).toFile().getAbsoluteFile();
                String checksum = entry.getValue();
                
                result = result && tools.validateChecksum(file, checksum);
            }
        }
        LOG.info("done");
        /*else {
            files = api.listRecordFiles(id.getId());
        }*/
//        HashMap<String,String> checksums = getFileChecksums(files);
//        // Check checksums for all files
//        for (Map.Entry<String, String> entry : checksums.entrySet()) {
//            String filename = entry.getKey();
//            File file = Path.of(path.toString(),filename).toFile().getAbsoluteFile();
//            String checksum = entry.getValue();
//            
//            result = result && validateChecksum(file, checksum);
//        }
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
            idList = api.getRecord(id.getId()).getMetadata().getRelatedIdentifiers();
        }
        for (Metadata.RelatedIdentifier relatedId : idList) {
            if (relatedId.getRelationType().getId().toString().equalsIgnoreCase("haspart")) {
                String relId = relatedId.getIdentifier().replace(url, "");
                result = result && validateRecords(new RecordId(tools.isDraft(relId),relId), path, bag, report);
            }
        }
        return result;
    }
    
    /**
     * Publish all given records
     * @param recordIds List of records to be published
     * @param report the corpus service report
     * @throws URISyntaxException
     * @throws NoSuchAlgorithmException
     * @throws KeyManagementException
     * @throws IOException
     * @throws InterruptedException 
     */
    public void publishRecords(List<String> recordIds, Report report) throws IOException, InterruptedException, URISyntaxException, NoSuchAlgorithmException, KeyManagementException {
        ArrayList<String> failed = new ArrayList<>();
        LOG.log(Level.INFO, "Records to be published: {0}", recordIds);
        for (String id : recordIds) {
            try {
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
            catch (IOException | InterruptedException | URISyntaxException | KeyManagementException | NoSuchAlgorithmException e) {
                LOG.log(Level.SEVERE, "Exception when publishing {0}", id);
                failed.add(id);
            }
        }
        if (!failed.isEmpty()) {
            report.addCritical("Failed to publish records " + failed.toString());
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
     * Mints DOIs for all draft records containing CMDIs
     * @param report to keep track of the process
     */
    private void mintDois(DataciteAPI datacite, String prefix, Report report) throws IOException, InterruptedException, URISyntaxException, NoSuchAlgorithmException, KeyManagementException, ApiException {
        List<String> draftRecords = tools.listDraftRecords();
        for (String id : draftRecords) {
            // Get draft record
            DraftRecord draftRecord = api.getDraftRecord(id);
            // Check if the record contains CMDI
            boolean hasCmdi = draftRecord.getFiles().getDefaultPreview() != null && 
                    draftRecord.getFiles().getDefaultPreview().toLowerCase().endsWith(".cmdi");
            // Get alternative identifiers for the record
            List<Metadata.AlternateIdentifier> alternateIdentifiers = draftRecord.getMetadata().getAlternateIdentifiers();
            if (hasCmdi) {
                // Mint DOI
                Doi doi = datacite.createDraftDOI(prefix, Optional.empty());
                // Start updating draft record
                // Check if we actually have a doi
                if (doi != null && doi.getData() != null && doi.getData().getAttributes() != null && doi.getData().getAttributes().getSuffix() != null) {
                    String doiId = doi.getData().getId();
                    String doiSuffix = doi.getData().getAttributes().getSuffix();
                    LOG.info("Minted doi " + doiSuffix);
                    // Update DOI identifier in Invenio metadata
                    for (Metadata.AlternateIdentifier identifier : alternateIdentifiers) {
                        if (identifier.getScheme().equals(new ControlledVocabulary.RecordIdentifierScheme(ControlledVocabulary.RecordIdentifierScheme.ERecordItentifierScheme.DOI)) &&
                                identifier.getIdentifier().matches(PID_PLACEHOLDER)) {
                            identifier.setIdentifier(doi.getData().getId());
                        }
                    }
//                draftRecord.getMetadata().setAlternateIdentifiers(alternateIdentifiers);
                    // Update DOI metadata
                    DataciteAPITools dataciteTools = new DataciteAPITools(datacite);
                    Doi dataciteMetadata = dataciteTools.convertInvenioMetadata(draftRecord.getMetadata());
                    dataciteMetadata.getData().getAttributes().setUrl(draftRecord.getLinks().get("record_html"));
                    datacite.updateDOI(doiId, dataciteMetadata);
                }
            }
            else {
                // Cleanup DOI placeholder
                alternateIdentifiers.removeAll(alternateIdentifiers.stream().filter((i) -> 
                        i.getScheme().equals(new ControlledVocabulary.RecordIdentifierScheme(ControlledVocabulary.RecordIdentifierScheme.ERecordItentifierScheme.DOI)) &&
                                i.getIdentifier().matches(PID_PLACEHOLDER)).toList());
            }
            // Upload updated Invenio record
            api.updateDraftRecord(id, draftRecord);
        }
    }

    /***
     * Update all CMDI files found in draft records fixing self and resource links
     * @param report to keep track of the process
     */
    private void updateCmdis(Report report) throws IOException, InterruptedException, URISyntaxException, NoSuchAlgorithmException, KeyManagementException, JDOMException {
        List<String> draftRecords = tools.listDraftRecords();
        for (String id : draftRecords) {
            // Get draft record
            DraftRecord draftRecord = api.getDraftRecord(id);
            // Get alternative identifiers for the record
            List<Metadata.AlternateIdentifier> alternateIdentifiers = draftRecord.getMetadata().getAlternateIdentifiers();
            // Check if the record contains CMDI
            boolean hasCmdi = draftRecord.getFiles().getDefaultPreview() != null && 
                    draftRecord.getFiles().getDefaultPreview().toLowerCase().endsWith(".cmdi");
            if (hasCmdi) {
                LOG.info("Updating CMDI for " + id);
                Document cmdi = new SAXBuilder().build(
                        api.getDraftFileContent(id, draftRecord.getFiles().getDefaultPreview()));
                // TODO
                Element root = cmdi.getRootElement();
                // Set the self link in the header to the first DOI found in alternate identifiers 
                // if it exists
                Element selfLink = root.getChild("Header",root.getNamespace()).getChild("MdSelfLink",root.getNamespace());
                alternateIdentifiers.stream().filter((i) -> i.getScheme().equals(new ControlledVocabulary.RecordIdentifierScheme(ControlledVocabulary.RecordIdentifierScheme.ERecordItentifierScheme.DOI)))
                        .findFirst()
                        // Add DOI url if missing
                        .ifPresent((Metadata.AlternateIdentifier i) -> {
                            if (i.getIdentifier().startsWith("http")) {
                                selfLink.setText(i.getIdentifier());
                            }
                            else {
                                selfLink.setText("https://doi.org/" + i.getIdentifier());
                            }
                });
                // Update resource proxy list
                Element resourceProxyList = root.getChild("Resources", root.getNamespace()).getChild("ResourceProxyList", root.getNamespace());
                // First remove all items
                resourceProxyList.removeChildren("ResourceProxy", root.getNamespace());
                // Add landing page
                Element landingPage = new Element("ResourceProxy", root.getNamespace())
                        .setAttribute("id","landingPage-"+ id)
                        .addContent(new Element("ResourceType", root.getNamespace())
                                .setAttribute("mimetype", "text/html")
                                .setText("LandingPage")
                        )
                        .addContent(new Element("ResourceRef", root.getNamespace())
                                .setText(draftRecord.getLinks().get("record_html"))
                        );
                resourceProxyList.addContent(landingPage);
                // Add metadata file
                Element metadataFile = new Element("ResourceProxy", root.getNamespace())
                        .setAttribute("id","metadataFile-"+id)
                        .addContent(new Element("ResourceType", root.getNamespace())
                                .setAttribute("mimetype", "application/x-cmdi+xml")
                                .setText("Metadata")
                        )
                        .addContent(new Element("ResourceRef", root.getNamespace())
                                .setText(
                                        url + id + "/files/" + draftRecord.getFiles().getDefaultPreview() + "?download=1"
                                        // api.protocol+ "://" + api.host + "/oai2d/verb=GetRecord&identifier=oai:ids-repos2.ids-mannheim.de:" + id
                                )
                        );
                resourceProxyList.addContent(metadataFile);
                // Add all related resources
                for (Metadata.RelatedIdentifier relatedIdentifier : draftRecord.getMetadata().getRelatedIdentifiers()) {
                    if (relatedIdentifier.getRelationType().getId()
                            .equals(new ControlledVocabulary.RelationTypeId(ControlledVocabulary.RelationTypeId.ERelationTypeId.HasPart))) {
                        String relatedId = relatedIdentifier.getIdentifier().replace(url, "");
                        DraftRecord relatedRecord = api.getDraftRecord(relatedId);
                        resourceProxyList.addContent(new Element("ResourceProxy")
                                .setAttribute("id", "resource-" + relatedRecord.getId().orElse(""))
                                .addContent(new Element("ResourceType", root.getNamespace())
                                        .setAttribute("mimetype", "application/xml")
                                        .setText("Resource")
                                )
                                // Set the reference to the first doi listed as alternate identifier
                                .addContent(new Element("ResourceRef", root.getNamespace())
                                        .setText(
                                                relatedRecord.getMetadata().getAlternateIdentifiers().stream()
                                                        .filter((i) -> i.getScheme().equals(new ControlledVocabulary.RecordIdentifierScheme(ControlledVocabulary.RecordIdentifierScheme.ERecordItentifierScheme.DOI)))
                                                        .findFirst()
                                                        // Add DOI url if missing
                                                        .map((Metadata.AlternateIdentifier i) -> {
                                                            if (i.getIdentifier().startsWith("http")) {
                                                                return i.getIdentifier();
                                                            }
                                                            else {
                                                                return "https://doi.org/" + i.getIdentifier();
                                                            }
                        })
                                                        .orElse("")
                                        )
                                )
                        );
                    }
                }
                // Write Cmdi to temp file and upload to invenio
                File tmpCmdi = File.createTempFile("cmdi", ".cmdi");
                
                new XMLOutputter(Format.getPrettyFormat()).output(cmdi, new FileOutputStream(tmpCmdi));
                api.deleteDraftFile(id, draftRecord.getFiles().getDefaultPreview());
                api.startDraftFileUpload(id, new ArrayList<>(List.of(new Files.FileEntry(draftRecord.getFiles().getDefaultPreview()))));
                api.uploadDraftFile(id, draftRecord.getFiles().getDefaultPreview(), tmpCmdi.toURI());
                api.completeDraftFileUpload(id, draftRecord.getFiles().getDefaultPreview());
                tmpCmdi.delete();
            }
        }
    }

    /***
     * Publish all draft DOIs
     * @param datacite the Datacite API to be used
     * @param prefix the DOI prefix for the DOIs
     * @param report to keep track of the process
     * @throws ApiException
     */
    private void publishDois(DataciteAPI datacite, String prefix, Report report) throws ApiException {
        DataciteAPITools dataciteTools = new DataciteAPITools(datacite);
        dataciteTools.publishAllDraftDois(prefix);
        throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    }

    /***
     * Modifies alternate identifiers for placeholders to be valid DOIs 
     * @param metadata 
     */
    private void sanitizeAlternateIdentifiers(Metadata metadata) {
        metadata.setAlternateIdentifiers(
                metadata.getAlternateIdentifiers().stream().map((i) -> {
                    if (i.getScheme().getScheme().equals(ControlledVocabulary.RecordIdentifierScheme.ERecordItentifierScheme.DOI) &&
                            i.getIdentifier().matches(PID_PLACEHOLDER)) {
                        return new Metadata.AlternateIdentifier("10.0/NOTYET", i.getScheme());
                    }
                    else {
                        return i;
                    }
                }).toList());
    }

    private void rollback(Optional<DataciteAPI> datacite, Optional<String> datacitePrefix) throws URISyntaxException, NoSuchAlgorithmException, NoSuchAlgorithmException, KeyManagementException, IOException, InterruptedException {
        tools.deleteDraftRecords();
        // Potentially delete draft DOIs
        datacite.ifPresent((dc) -> datacitePrefix.ifPresent((prefix) -> {
            try {
                new DataciteAPITools(dc).deleteAllDraftDOIs(prefix);
            } catch (ApiException ex) {
                Logger.getLogger(InvenioTools.class.getName()).log(Level.SEVERE, null, ex);
            }
        }));
    }

    
    /**
     * Class keeping track if the id is a draft or an already published record
     */
    private static class RecordId {
        // Flag if the ID is for a draft record
        boolean draft;
        // The ID itself
        private final String id;

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
