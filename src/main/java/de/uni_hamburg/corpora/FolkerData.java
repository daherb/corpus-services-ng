package de.uni_hamburg.corpora;

import java.net.URL;
import java.util.Collection;
import java.util.Collections;

/**
 * FolkerData is just a different mame for UnspecifiedXMLData
 *
 * @author bba1792 Dr. Herbert Lange
 * @version 20211108
 */
public class FolkerData extends UnspecifiedXMLData implements XMLData, CorpusData {

    public FolkerData(URL url) { super(url); }

    public FolkerData() { super(); }

    @Override
    public Collection<String> getFileExtensions() {
        return Collections.singleton("fln");
    }

    @Override
    public Object clone() {
        return new FolkerData(this.url);
    }
}
