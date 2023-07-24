package de.uni_hamburg.corpora.data.content;

import de.uni_hamburg.corpora.XMLData;
import de.uni_hamburg.corpora.data.ContentData;
import de.uni_hamburg.corpora.data.UnspecifiedXMLData;

import java.net.URL;
import java.util.Collection;
import java.util.Collections;

/**
 * FolkerData is just a different mame for UnspecifiedXMLData
 *
 * @author bba1792 Dr. Herbert Lange
 * @version 20230105
 */
public class FolkerData extends UnspecifiedXMLData implements XMLData, ContentData {

    public FolkerData(URL url) { super(url); }

    public FolkerData() { super(); }

    @Override
    public Collection<String> getFileExtensions() {
        return Collections.singleton("fln");
    }

    @Override
    public Object clone() {
        return new FolkerData(this.getURL());
    }
}
