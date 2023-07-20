
package de.uni_hamburg.corpora.utilities.publication.ids.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

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

}
