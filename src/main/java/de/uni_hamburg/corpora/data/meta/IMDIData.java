package de.uni_hamburg.corpora.data.meta;

import de.uni_hamburg.corpora.XMLData;
import de.uni_hamburg.corpora.data.Metadata;
import de.uni_hamburg.corpora.data.UnspecifiedXMLData;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collection;
import java.util.Collections;

/**
 * IMDIData is just a different mame for UnspecifiedXMLData
 *
 * @author bba1792 Dr. Herbert Lange
 * @version 20210720
 */
public class IMDIData extends UnspecifiedXMLData implements XMLData, Metadata {

    public IMDIData(URL url) { super(url); }

    public IMDIData() { super(); }

    @Override
    public Collection<String> getFileExtensions() {
        return Collections.singleton("imdi");
    }

    @Override
    public Object clone() {
        return new IMDIData(this.getURL());
    }

    @Override
    public Collection<URL> getReferencedCorpusDataURLs() throws MalformedURLException, URISyntaxException {
        // TODO
        return null;
    }
}
