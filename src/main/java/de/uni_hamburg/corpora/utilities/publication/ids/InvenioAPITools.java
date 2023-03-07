/*
* Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
* Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
*/
package de.uni_hamburg.corpora.utilities.publication.ids;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonRootName;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import de.idsmannheim.lza.inveniojavaapi.API;
import de.idsmannheim.lza.inveniojavaapi.Access;
import de.idsmannheim.lza.inveniojavaapi.CMDI;
import de.idsmannheim.lza.inveniojavaapi.ControlledVocabulary;
import de.idsmannheim.lza.inveniojavaapi.DraftRecord;
import de.idsmannheim.lza.inveniojavaapi.Record;
import de.idsmannheim.lza.inveniojavaapi.Files;
import de.idsmannheim.lza.inveniojavaapi.FilesOptions;
import de.idsmannheim.lza.inveniojavaapi.Metadata;
import de.idsmannheim.lza.inveniojavaapi.Records;
import de.idsmannheim.lza.inveniojavaapi.cmdi.*;
import de.idsmannheim.lza.xmlmagic.MimeType;
import de.idsmannheim.lza.xmlmagic.XmlMagic;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.DirectoryFileFilter;
import org.apache.commons.io.filefilter.NameFileFilter;
import org.apache.commons.io.filefilter.NotFileFilter;
import org.jdom2.Document;
import org.jdom2.JDOMException;
import org.jdom2.input.SAXBuilder;

/**
 * Class providing higher-level abstraction helpers based on Invenio API calls
 * @author Herbert Lange <lange@ids-mannheim.de>
 */
public class InvenioAPITools {
    // Information how to find the metadata file
    static final String METADATA_DIR = "metadata";
    static final String METADATA_FILE = "metadata.cmdi";
    //static final String RECORD_MAP_FILE = "recordmap.xml";
    static final String RECORD_MAP_FILE = "recordmap.json";
    // Path separator to be used instead of /
    static final String SEPARATOR = "-0-0-";
    ControlledVocabulary.LanguageIdFactory languageIdFactory;
    
    /**
     * Class representing the SIP in Invenio based on the parent
     * identifiers and the list of child identifiers 
     * (i.e. the file records)
     */
    public static class InvenioSIP {
        String id;
        ArrayList<String> children = new ArrayList<>();

        public InvenioSIP(String id) {
            this.id = id;
        }

        public InvenioSIP addChildren(ArrayList<String> children) {
            this.children.addAll(children);
            return this;
        }

        @Override
        public int hashCode() {
            int hash = 3;
            hash = 71 * hash + Objects.hashCode(this.id);
            hash = 71 * hash + Objects.hashCode(this.children);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final InvenioSIP other = (InvenioSIP) obj;
            if (!Objects.equals(this.id, other.id)) {
                return false;
            }
            return Objects.equals(this.children, other.children);
        }

        @Override
        public String toString() {
            return "InvenioSIP{" + "id=" + id + ", children=" + children + '}';
        }
        
        
    }
    
    /**
     * Class for representing the mapping from files to Invenio records
     */
    @JsonAutoDetect(creatorVisibility = JsonAutoDetect.Visibility.ANY)
    @JsonRootName("object")
    static class DigiObject {
        static class DigiRecord {
            String name;
            List<String> files;
            
            @JsonCreator
            public DigiRecord(@JsonProperty("name") String name, @JsonProperty("file") List<String> files) {
                this.name = name;
                this.files = files;
            }
            
            @Override
            public String toString() {
                return "DigiRecord{" + "name=" + name + ", files=" + files + '}';
            }
        }
        String title;
        List<DigiRecord> records;
        @JsonCreator
        public DigiObject(@JsonProperty("title") String title, @JsonProperty("record") List<DigiRecord> records) {
            this.title = title;
            this.records = records;
        }

        @Override
        public String toString() {
            return "DigiObject{" + "title=" + title + ", records=" + records + '}';
        }
        
        

    }
    
    API api;
    String url;
    
    private static final Logger LOG = Logger.getLogger(InvenioAPITools.class.getName());
    
    
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
     * Upload a sip from a local path as a new draft
     * @param sipPath the path to the SIP
     * @param publicFiles flag if files are public
     * @return the id of the new draft record
     * @throws IOException
     * @throws JDOMException
     * @throws URISyntaxException
     * @throws NoSuchAlgorithmException
     * @throws KeyManagementException
     * @throws InterruptedException
     * @throws FileNotFoundException
     * @throws JsonProcessingException
     * @throws UnsupportedEncodingException
     * @throws java.lang.CloneNotSupportedException
     */
    public InvenioSIP uploadDraftSip(Path sipPath, boolean publicFiles) throws IOException, JDOMException, URISyntaxException, NoSuchAlgorithmException, KeyManagementException, InterruptedException, FileNotFoundException, JsonProcessingException, UnsupportedEncodingException, CloneNotSupportedException {
        ObjectMapper om = new ObjectMapper();
        om.findAndRegisterModules();
        om.enable(SerializationFeature.INDENT_OUTPUT);
        // Read metadata from CMDI file
        Metadata metadata = readSipMetadata(sipPath);
        // File access to metadata file is currently always possible
        Access.AccessType fileAccess = Access.AccessType.Public;
        DraftRecord draftRecord = new DraftRecord(
                new Access(Access.AccessType.Public, fileAccess),
                new FilesOptions(true),
                metadata);
        Record created = api.createDraftRecord(draftRecord);
        // Upload CMDI file
        api.startDraftFileUpload(created.getId(), new ArrayList<>(Collections.singletonList(new Files.FileEntry(METADATA_FILE))));
        api.uploadDraftFile(created.getId(), METADATA_FILE, Path.of(sipPath.toString(), METADATA_DIR, METADATA_FILE).toFile());
        api.completeDraftFileUpload(created.getId(), METADATA_FILE);
        // Upload files and keep track of IDS 
        InvenioSIP sip = new InvenioSIP(created.getId());
        // Keep track of file references
        ArrayList<Metadata.RelatedIdentifier> fileIds = new ArrayList<>();
        File recordMapFile = Path.of(sipPath.toString(), METADATA_DIR, RECORD_MAP_FILE).toFile();
        if (recordMapFile.exists()) {
            ObjectMapper mapper = new ObjectMapper();
            DigiObject map = mapper
                    .configure(DeserializationFeature.UNWRAP_ROOT_VALUE, true)
                    .readValue(recordMapFile, DigiObject.class);
        }
        else {
            ArrayList<String> children = uploadSipFiles(created.getId(), (Metadata) metadata.clone(), sipPath, publicFiles);
            sip.addChildren(children);
            // Add file references
            for (String id : children) {
                fileIds.add(new Metadata.RelatedIdentifier(url + id,
                        new ControlledVocabulary.RelatedRecordIdentifierScheme(ControlledVocabulary.RelatedRecordIdentifierScheme.ERelatedRecordIdentifierScheme.URL),
                        new Metadata.RelatedIdentifier.RelationType(new ControlledVocabulary.RelationTypeId(ControlledVocabulary.RelationTypeId.ERelationTypeId.HasPart),
                                new Metadata.LocalizedStrings().add(new Metadata.Language(languageIdFactory.usingId2("en")), "Has part"))));
            }
        }
        metadata.addRelatedIdentifiers(fileIds);
        draftRecord = new DraftRecord(
                new Access(Access.AccessType.Public, fileAccess),
                new FilesOptions(true),
                metadata);
        api.updateDraftRecord(created.getId(), draftRecord);
        api.publishDraftRecord(created.getId());
        return sip;
    }
    
    public ArrayList<String> uploadSipFiles(String parentId, Metadata metadata, Path sipPath, boolean publicFile) throws UnsupportedEncodingException, URISyntaxException, NoSuchAlgorithmException, KeyManagementException, IOException, InterruptedException {
        // TODO
        ArrayList<String> fileIds = new ArrayList<>();
        // Upload all files except for the metadata file as separate records
        for (File f : FileUtils.listFiles(sipPath.toFile(), new NotFileFilter(new NameFileFilter(METADATA_FILE)), DirectoryFileFilter.DIRECTORY)) {
            fileIds.add(uploadSipFile(parentId, metadata, sipPath, f, publicFile));
        }
//        ArrayList<Files.FileEntry> entries = new ArrayList();
//        for (String fn :sipFiles.keySet()) {
//            // entries.add(new Files.FileEntry(URLEncoder.encode(fn, StandardCharsets.UTF_8.toString())));
//            entries.add(new Files.FileEntry(fn));
//        }
//        Files files = api.startDraftFileUpload(id, entries);
//        for (HashMap.Entry<String, File> sipFile : sipFiles.entrySet()) {
//            Files.FileEntry entry = api.uploadDraftFile(id, sipFile.getKey(), sipFile.getValue());
//            Files.FileEntry completed = api.completeDraftFileUpload(id, sipFile.getKey());
//        }
        return fileIds;
    }
    
    public String uploadSipFile(String parentId, Metadata metadata, Path sipPath, File file, boolean publicFile) throws URISyntaxException, NoSuchAlgorithmException, KeyManagementException, FileNotFoundException, IOException, JsonProcessingException, InterruptedException, UnsupportedEncodingException {
        // Check if files should be public
        Access.AccessType fileAccess;
        if (publicFile)
            fileAccess = Access.AccessType.Public;
        else
            fileAccess = Access.AccessType.Restricted;
        // Update title and add reference to parent
        Metadata fileMetadata = new Metadata(
                new Metadata.ResourceType(new ControlledVocabulary.ResourceType(ControlledVocabulary.ResourceType.EResourceType.Other)),
                metadata.getCreators(),
                file.toString().replace(sipPath.toString(), ""),
                metadata.getPublicationDate()
        );
        fileMetadata.addRelatedIdentifiers(Collections.singletonList(
                new Metadata.RelatedIdentifier(url + parentId,
                        new ControlledVocabulary.RelatedRecordIdentifierScheme(ControlledVocabulary.RelatedRecordIdentifierScheme.ERelatedRecordIdentifierScheme.URL),
                        new Metadata.RelatedIdentifier.RelationType(new ControlledVocabulary.RelationTypeId(ControlledVocabulary.RelationTypeId.ERelationTypeId.IsPartOf),
                                new Metadata.LocalizedStrings().add(new Metadata.Language(languageIdFactory.usingId2("en")), "Is part of"))))
        );
        DraftRecord draftRecord = new DraftRecord(
                new Access(Access.AccessType.Public, fileAccess),
                new FilesOptions(true),
                fileMetadata);
        // Create draft for file
        Record created = api.createDraftRecord(draftRecord);
        // Prepare and upload file
        LOG.log(Level.INFO, "Upload file: {0}", file.toString());
        api.startDraftFileUpload(created.getId(),new ArrayList<>(Collections.singletonList(new Files.FileEntry(file.getName()))));
        api.uploadDraftFile(created.getId(), file.getName(), file);
        api.completeDraftFileUpload(created.getId(), file.getName());
        api.publishDraftRecord(created.getId());
        return created.getId();
    }
    
    /**
     * Uploads all sips (i.e.subfolders) in a certain path
     * @param sipPath The path to the folder containing the sips
     * @param publicFiles Flag if files will be accessible publicly
     * @throws java.io.IOException
     * @throws org.jdom2.JDOMException
     * @throws java.net.URISyntaxException
     * @throws java.security.NoSuchAlgorithmException
     * @throws java.io.UnsupportedEncodingException
     * @throws java.lang.InterruptedException
     * @throws java.io.FileNotFoundException
     * @throws com.fasterxml.jackson.core.JsonProcessingException
     * @throws java.security.KeyManagementException
     * @throws java.lang.CloneNotSupportedException
     */
    public void uploadDraftSips(Path sipPath, boolean publicFiles) throws IOException, JDOMException, URISyntaxException, NoSuchAlgorithmException, KeyManagementException, InterruptedException, FileNotFoundException, JsonProcessingException, UnsupportedEncodingException, CloneNotSupportedException {
        for (File sip : sipPath.toFile().listFiles()) {
            if (sip.isDirectory()) {
                LOG.log(Level.INFO, "Uploading {0}", sip.toString());
                uploadDraftSip(sip.toPath(), publicFiles);
            }
        }
    }
    
    /**
     * Downloads a draft SIP to a destination
     * @param id The Invenio id
     * @param destinationPath the path where the sip will be stored
     * @throws java.io.IOException
     * @throws org.jdom2.JDOMException
     * @throws java.net.URISyntaxException
     * @throws java.security.NoSuchAlgorithmException
     * @throws java.io.UnsupportedEncodingException
     * @throws java.lang.InterruptedException
     * @throws java.io.FileNotFoundException
     * @throws com.fasterxml.jackson.core.JsonProcessingException
     * @throws java.security.KeyManagementException
     */
    public void downloadDraftSip(String id, Path destinationPath) throws IOException, JDOMException, URISyntaxException, NoSuchAlgorithmException, KeyManagementException, InterruptedException, FileNotFoundException, JsonProcessingException, UnsupportedEncodingException {
        ArrayList<String> fileNames = listSipFilenames(id);
        for (String name : fileNames) {
            downloadSipFile(id, name, destinationPath);
        }
    }
    
    /**
     * Helper to download a sip file
     * @param id the draft id
     * @param name the file name
     * @param destination the download destination
     * @throws UnsupportedEncodingException
     * @throws IOException
     * @throws URISyntaxException
     * @throws NoSuchAlgorithmException
     * @throws KeyManagementException
     * @throws JsonProcessingException
     * @throws InterruptedException
     */
    private void downloadSipFile(String id, String name, Path destination) throws UnsupportedEncodingException, IOException, URISyntaxException, NoSuchAlgorithmException, KeyManagementException, JsonProcessingException, InterruptedException {
        // String decodedName = URLDecoder.decode(name, StandardCharsets.UTF_8.toString());
        String decodedName = name;
        File file = Path.of(Path.of(destination.toString(),id).toString(),
                decodedName.split(SEPARATOR)).toFile();
        FileUtils.forceMkdirParent(file);
        LOG.log(Level.INFO, "Downloading file {0}", file);
        api.getDraftFileContent(id,decodedName).transferTo(new FileOutputStream(file));
    }
    
    /**
     * Deletes a draft sip and all associated files
     * @param id the draft id
     * @throws java.io.IOException
     * @throws org.jdom2.JDOMException
     * @throws java.net.URISyntaxException
     * @throws java.security.NoSuchAlgorithmException
     * @throws java.io.UnsupportedEncodingException
     * @throws java.lang.InterruptedException
     * @throws java.io.FileNotFoundException
     * @throws com.fasterxml.jackson.core.JsonProcessingException
     * @throws java.security.KeyManagementException
     */
    public void deleteDraftSip(String id) throws IOException, JDOMException, URISyntaxException, NoSuchAlgorithmException, KeyManagementException, InterruptedException, FileNotFoundException, JsonProcessingException, UnsupportedEncodingException {
        // List and delete all draft files
//        ArrayList<String> fileNames = listSipFilenames(id);
//        for (String name : fileNames) {
//            api.deleteDraftFile(id, name);
//        }
        api.deleteDraftRecord(id);
    }
    
    /**
     * Publish a draft SIP
     * @param id the draft id
     * @throws java.io.IOException
     * @throws org.jdom2.JDOMException
     * @throws java.net.URISyntaxException
     * @throws java.security.NoSuchAlgorithmException
     * @throws java.io.UnsupportedEncodingException
     * @throws java.lang.InterruptedException
     * @throws java.io.FileNotFoundException
     * @throws com.fasterxml.jackson.core.JsonProcessingException
     * @throws java.security.KeyManagementException
     */
    public void publishDraftSip(String id) throws IOException, JDOMException, URISyntaxException, NoSuchAlgorithmException, KeyManagementException, InterruptedException, FileNotFoundException, JsonProcessingException, UnsupportedEncodingException {
        api.publishDraftRecord(id);
    }
    
    /**
     * Delete all draft sips
     * @throws java.io.IOException
     * @throws org.jdom2.JDOMException
     * @throws java.net.URISyntaxException
     * @throws java.security.NoSuchAlgorithmException
     * @throws java.io.UnsupportedEncodingException
     * @throws java.lang.InterruptedException
     * @throws java.io.FileNotFoundException
     * @throws com.fasterxml.jackson.core.JsonProcessingException
     * @throws java.security.KeyManagementException
     */
    public void deleteAllDrafts() throws IOException, JDOMException, URISyntaxException, NoSuchAlgorithmException, KeyManagementException, InterruptedException, FileNotFoundException, JsonProcessingException, UnsupportedEncodingException {
        Records userRecords = api.listUserRecords(); // api.searchRecords(Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty(), Optional.empty());
        for (Record r : userRecords.getHits().getHits()) {
            if (!r.isPublished()) { // Tried to use isDraft but doesn't seem to be working
                LOG.log(Level.INFO, "Deleting {0} - {1} - {2}", new Object[]{r.getId(), r.getMetadata().getTitle(), r.isDraft()});
                api.deleteDraftRecord(r.getId());
            }
            
        }
    }
    
    /**
     * Updates a published sip by creating a new draft and uploading the new data
     * 
     * @param id the id of the record to be updated
     * @param sipPath the path to the new files
     * @return the id of the new draft record
     * @throws java.net.URISyntaxException
     * @throws java.security.NoSuchAlgorithmException
     * @throws java.security.KeyManagementException
     * @throws java.lang.InterruptedException
     * @throws java.io.IOException
     * @throws org.jdom2.JDOMException
     */
    // TODO fix
//    public String updateSip(String id, Path sipPath) throws URISyntaxException, NoSuchAlgorithmException, KeyManagementException, KeyManagementException, IOException, InterruptedException, JDOMException {
//        DraftRecord draft = api.createNewVersion(id);
//        if (draft.getId().isPresent()) {
//            draft.setMetadata(readSipMetadata(sipPath));
//            api.updateDraftRecord(draft.getId().get(), draft);
//            uploadSipFiles(draft.getId().get(), sipPath);
//            return draft.getId().get();
//        }
//        else {
//            throw new IOException("Error creating new draft");
//        }
//    }

    /**
     * Read the sip metadata from the CMDI file in the sip
     * @param sipPath path to the sip
     * @return the Invenio metadata
     * @throws IOException
     * @throws JDOMException 
     */
    public Metadata readSipMetadata(Path sipPath) throws IOException, JDOMException {
//        // Look for all CMDI files in the metadata subfolder
//        ArrayList<File> cmdiFiles = new ArrayList<>(
//                Arrays.asList(Path.of(sipPath.toString(), "metadata").toFile()
//                        .listFiles()).stream()
//                        .filter((f) -> f.toString().toLowerCase().endsWith(".cmdi"))
//                        .collect(Collectors.toList()));
//        if (cmdiFiles.size() != 1) {
//            throw new IOException("Wrong number of cmid files. Should be 1 but was " + String.valueOf(cmdiFiles.size()));
//        }
//        Document document = new SAXBuilder().build(cmdiFiles.get(0));
        // Read the CMDI file metadata/metadata.cmdi
        Document document = new SAXBuilder().build(Path.of(sipPath.toString(), "metadata","metadata.cmdi").toFile());
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
                    default:
                        throw new IOException("Unsupported CMDI profile");
                }
            }
        }
        throw new IOException("Unrecognized CMDI file or profile");
    }

    /**
     * List all files associated with a SIP record
     * @param id the id of the record of which the files should be listed
     * @return  the list of filenames included in the record
     */
    private ArrayList<String> listSipFilenames(String id) throws NoSuchAlgorithmException, KeyManagementException, IOException, JsonProcessingException, InterruptedException, URISyntaxException {
        Files files = api.listDraftFiles(id);
        ArrayList<String> fileNames = new ArrayList<>();
        if (files.getEntries().getClass().equals(ArrayList.class)) {
            for (Files.FileEntry fe : (ArrayList<Files.FileEntry>) files.getEntries()) {
                fileNames.add(fe.getKey());
            }
        }
        else {
            for (String key : ((HashMap<String,Files.FileEntry>) files.getEntries()).keySet()) {
                fileNames.add(key);
            }
        }
        return fileNames;
    }
}
