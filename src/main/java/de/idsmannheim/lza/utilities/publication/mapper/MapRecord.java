
package de.idsmannheim.lza.utilities.publication.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

@JacksonXmlRootElement(localName = "record")
public class MapRecord {

    @JacksonXmlProperty(localName = "metadata")
    protected Optional<String> metadata = Optional.empty();
    @JacksonXmlElementWrapper(localName = "records")
    @JacksonXmlProperty(localName = "record")
    List<MapRecord> records = new ArrayList<>();
    @JacksonXmlElementWrapper(localName = "files")
    @JacksonXmlProperty(localName = "file")
    List<MapFile> files = new ArrayList<>();
    @JacksonXmlProperty(localName = "title", isAttribute = true)
    Optional<String> title = Optional.empty();

    @Override
    public String toString() {
        return "MapRecord{" + "metadata=" + metadata + ", records=" + records + ", files=" + files + ", title=" + title + '}';
    }

    public void setMetadata(String metadata) {
        this.metadata = Optional.of(metadata);
    }

    public void setTitle(String title) {
        this.title = Optional.of(title);
    }

    public void setFiles(List<MapFile> files) {
        this.files = files;
    }
    
    public Optional<String> getMetadata() {
        return metadata;
    }
    
    public List<MapRecord> getRecords() {
        return records;
    }

    public List<MapFile> getFiles() {
        return files;
    }

    public Optional<String> getTitle() {
        return title;
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        ObjectMapper om = new ObjectMapper();
        om.findAndRegisterModules();
        try {
            return om.readValue(om.writeValueAsString(this), MapRecord.class);
        } catch (JsonProcessingException ex) {
            throw new CloneNotSupportedException(ex.toString());
        }
    }

    /***
     * Modifies a record map to store private files in separate records from 
     * the public ones
     * @param map The original record map
     * @return A modified copy of the original map or null if anything went wrong
     */
    public static MapRecord separatePrivateFiles(MapRecord map) {
        MapRecord newMap;
        if (map instanceof MapRootRecord) {
            newMap = new MapRootRecord();
        }
        else {
            newMap = new MapRecord();
        }
        // Set the metadata in newMap if it is present in map
        map.getMetadata().ifPresent(newMap::setMetadata);
        // Same with title
        map.getTitle().ifPresent(newMap::setTitle);
        // Split file list into private and public files
        ArrayList<MapFile> privateFiles = new ArrayList<>(map.getFiles().stream()
                .filter(Predicate.not(MapFile::isPublic)).toList());
        ArrayList<MapFile> publicFiles = new ArrayList<>(map.getFiles().stream()
                .filter(MapFile::isPublic).toList());
        // If we have private files we create a new record for them
        if (!privateFiles.isEmpty()) {
            MapRecord privateRecord = new MapRecord();
            map.getTitle().ifPresent((t) -> privateRecord.setTitle(t + " - Private files"));
            privateRecord.setFiles(privateFiles);
            // Copy metadata from public record
            map.getMetadata().ifPresent(privateRecord::setMetadata);
            newMap.getRecords().add(privateRecord);
            newMap.setFiles(publicFiles);
        }
        // Otherwise just add all the files
        else {
            newMap.setFiles(map.getFiles());
        }
        newMap.getRecords().addAll(map.getRecords().stream()
                .map(MapRecord::separatePrivateFiles).toList());
        return newMap;
    }
    
}
