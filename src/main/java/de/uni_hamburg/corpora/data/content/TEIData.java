package de.uni_hamburg.corpora.data.content;

import de.uni_hamburg.corpora.XMLData;
import de.uni_hamburg.corpora.data.ContentData;
import de.uni_hamburg.corpora.data.UnspecifiedXMLData;

import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

/**
 * TEIData is just a different mame for UnspecifiedXMLData
 *
 * @author bba1792 Dr. Herbert Lange
 * @version 20230105
 */
public class TEIData extends UnspecifiedXMLData implements XMLData, ContentData {

    public TEIData(URL url) { super(url); }

    public TEIData() { super(); }

    @Override
    public Collection<String> getFileExtensions() {
        return Arrays.asList(new String[]{"xml","okk"});
    }

    @Override
    public Object clone() {
        return new TEIData(this.getURL());
    }
}
