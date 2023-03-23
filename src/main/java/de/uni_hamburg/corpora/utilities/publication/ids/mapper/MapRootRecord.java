
package de.uni_hamburg.corpora.utilities.publication.ids.mapper;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;
import java.util.List;
import java.util.Optional;

@JsonIgnoreProperties({"noNamespaceSchemaLocation"})
@JacksonXmlRootElement(localName = "rootRecord")
public class MapRootRecord extends MapRecord {

    @Override
    public String toString() {
        return "MapRootRecord{" + "metadata=" + metadata + ", title=" + title + ", files=" + files + ", records=" + records + '}';
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

    public void setRecords(List<MapRecord> _records) {
        records = _records;
    }

}
