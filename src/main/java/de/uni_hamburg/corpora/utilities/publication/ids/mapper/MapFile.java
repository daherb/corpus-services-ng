
package de.uni_hamburg.corpora.utilities.publication.ids.mapper;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlText;

public class MapFile {

    @JacksonXmlText
    protected String name;
    @JacksonXmlProperty(localName = "public", isAttribute = true)
    protected Boolean _public;

    @Override
    public String toString() {
        return "MapFile{" + "name=" + name + ", _public=" + _public + '}';
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPublic(Boolean _public) {
        this._public = _public;
    }

    public String getName() {
        return name;
    }

    public Boolean isPublic() {
        if (_public == null) {
            _public = false;
        }
        return _public;
    }
    
    
}
