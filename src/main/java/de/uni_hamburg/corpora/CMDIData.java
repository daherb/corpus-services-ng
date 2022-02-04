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
public class CMDIData extends UnspecifiedXMLData implements XMLData, CorpusData {

    public CMDIData(URL url) { super(url); }

    public CMDIData() { super(); }

    @Override
    public Collection<String> getFileExtensions() {
        return Collections.singleton("cmdi");
    }

    @Override
    public Object clone() {
        return new CMDIData(this.url);
    }

    @Override
    public Location getLocation(String token) {
        return new Location("undefined","");
    }
}
