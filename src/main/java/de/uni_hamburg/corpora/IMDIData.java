package de.uni_hamburg.corpora;

import java.net.URL;
import java.util.Collection;
import java.util.Collections;

/**
 * IMDIData is just a different mame for UnspecifiedXMLData
 *
 * @author bba1792 Dr. Herbert Lange
 * @version 20210720
 */
public class IMDIData extends UnspecifiedXMLData implements XMLData, CorpusData {

    public IMDIData(URL url) { super(url); }

    public IMDIData() { super(); }

    @Override
    public Collection<String> getFileExtensions() {
        return Collections.singleton("imdi");
    }

    @Override
    public Object clone() {
        return new IMDIData(this.url);
    }
}
