/*
* Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
* Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
*/
package de.uni_hamburg.corpora.utilities.publication;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import de.idsmannheim.lza.inveniojavaapi.API;
import de.idsmannheim.lza.inveniojavaapi.Access;
import de.idsmannheim.lza.inveniojavaapi.CMDI;
import de.idsmannheim.lza.inveniojavaapi.DraftRecord;
import de.idsmannheim.lza.inveniojavaapi.Record;
import de.idsmannheim.lza.inveniojavaapi.Files;
import de.idsmannheim.lza.inveniojavaapi.FilesOptions;
import de.idsmannheim.lza.inveniojavaapi.Metadata;
import de.idsmannheim.lza.inveniojavaapi.Records;
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
import java.util.HashMap;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
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
    
    // Path separator to be used instead of /
    public static final String SEPARATOR = "-0-0-";
    API api;
    
    private static final Logger LOG = Logger.getLogger(InvenioAPITools.class.getName());
    
    
    /**
     * Default constructor
     * @param api The API object to be used
     */
    public InvenioAPITools(API api) {
        this.api = api;
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
     */
    public String uploadDraftSip(Path sipPath, boolean publicFiles) throws IOException, JDOMException, URISyntaxException, NoSuchAlgorithmException, KeyManagementException, InterruptedException, FileNotFoundException, JsonProcessingException, UnsupportedEncodingException {
        ObjectMapper om = new ObjectMapper();
        // add a trailing slash if missing
        if (!sipPath.endsWith("/"))
            sipPath = Path.of(sipPath.toString(), "/");
        om.findAndRegisterModules();
        om.enable(SerializationFeature.INDENT_OUTPUT);
        
        Metadata metadata = readSipMetadata(sipPath);
        Access.AccessType fileAccess;
        // Check if files should be public
        if (publicFiles)
            fileAccess = Access.AccessType.Public;
        else
            fileAccess = Access.AccessType.Restricted;
        DraftRecord draftRecord = new DraftRecord(
                new Access(Access.AccessType.Public, fileAccess),
                new FilesOptions(true),
                metadata);
        Record created = api.createDraftRecord(draftRecord);
        uploadSipFiles(created.getId(), sipPath);
        LOG.log(Level.INFO, "New record id: {0}", created.getId());
        return created.getId();
    }
    
    public void uploadSipFiles(String id, Path sipPath) throws UnsupportedEncodingException, URISyntaxException, NoSuchAlgorithmException, KeyManagementException, IOException, InterruptedException {
        HashMap<String,File> sipFiles = new HashMap<>();
        for (File f : FileUtils.listFiles(sipPath.toFile(), FileFileFilter.FILE, DirectoryFileFilter.DIRECTORY)) {
            String fn = f.getAbsolutePath().replace(sipPath.toAbsolutePath().toString(), "").replaceAll("/", SEPARATOR);
            sipFiles.put(fn, f);
        }
        ArrayList<Files.FileEntry> entries = new ArrayList();
        for (String fn :sipFiles.keySet()) {
            // entries.add(new Files.FileEntry(URLEncoder.encode(fn, StandardCharsets.UTF_8.toString())));
            entries.add(new Files.FileEntry(fn));
        }
        Files files = api.startDraftFileUpload(id, entries);
        for (HashMap.Entry<String, File> sipFile : sipFiles.entrySet()) {
            Files.FileEntry entry = api.uploadDraftFile(id, sipFile.getKey(), sipFile.getValue());
            Files.FileEntry completed = api.completeDraftFileUpload(id, sipFile.getKey());
        }
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
     */
    public void uploadDraftSips(Path sipPath, boolean publicFiles) throws IOException, JDOMException, URISyntaxException, NoSuchAlgorithmException, KeyManagementException, InterruptedException, FileNotFoundException, JsonProcessingException, UnsupportedEncodingException {
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
    public String updateSip(String id, Path sipPath) throws URISyntaxException, NoSuchAlgorithmException, KeyManagementException, KeyManagementException, IOException, InterruptedException, JDOMException {
        DraftRecord draft = api.createNewVersion(id);
        if (draft.getId().isPresent()) {
            draft.setMetadata(readSipMetadata(sipPath));
            api.updateDraftRecord(draft.getId().get(), draft);
            uploadSipFiles(draft.getId().get(), sipPath);
            return draft.getId().get();
        }
        else {
            throw new IOException("Error creating new draft");
        }
    }

    /**
     * Read the sip metadata from the CMDI file in the sip
     * @param sipPath path to the sip
     * @return the Invenio metadata
     * @throws IOException
     * @throws JDOMException 
     */
    public Metadata readSipMetadata(Path sipPath) throws IOException, JDOMException {
        // Look for all CMDI files in the metadata subfolder
//        ArrayList<File> cmdiFiles = new ArrayList<>(
//                Arrays.asList(Path.of(sipPath.toString(), "metadata").toFile()
//                        .listFiles()).stream()
//                        .filter((f) -> f.toString().toLowerCase().endsWith(".cmdi"))
//                        .collect(Collectors.toList()));
//        if (cmdiFiles.size() != 1) {
//            throw new IOException("Wrong number of cmid files. Should be 1 but was " + String.valueOf(cmdiFiles.size()));
//        }
//        Document document = new SAXBuilder().build(cmdiFiles.get(0));
        Document document = new SAXBuilder().build(Path.of(sipPath.toString(), "metadata","metadata.cmdi").toFile());
        return CMDI.readCmdiMetadata(document);
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
