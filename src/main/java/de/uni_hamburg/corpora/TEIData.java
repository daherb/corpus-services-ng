package de.uni_hamburg.corpora;

import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

/**
 * TEIData is just a different mame for UnspecifiedXMLData
 *
 * @author bba1792 Dr. Herbert Lange
 * @version 20211007
 */
public class TEIData extends UnspecifiedXMLData implements XMLData, CorpusData {

    public TEIData(URL url) { super(url); }

    public TEIData() { super(); }

    @Override
    public Collection<String> getFileExtensions() {
        return Arrays.asList(new String[]{"xml","okk"});
    }

    @Override
    public Object clone() {
        return new TEIData(this.url);
    }
}
