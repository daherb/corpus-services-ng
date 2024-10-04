package de.uni_hamburg.corpora;

import com.helger.collection.pair.Pair;
import org.apache.commons.io.FilenameUtils;
import org.jdom2.JDOMException;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpressionException;
import java.io.*;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

/***
 * @author bba1702 Dr. Herbert Lange
 * @version 20210622
 *
 * Representation of Metadata of a CHILDES/TalkBank corpus
 */
public class ChildesMetadata implements Metadata, CorpusData {

    // url of the metadata file
    private URL url ;
    // Either the containing folder for a file or the parent directory for a folder
    private URL parentUrl ;
    // filename from the url
    private String fileName ;
    // baseName is filename without extension
    private String baseName ;
    private String unformatedString ;

    // Use hash set because we cannot assume that keys are unique
    private final HashSet<Pair<String,String>> metadata = new HashSet<>();
    // Separate set for efficient lookup of keys
    private final HashSet<String> metadataKeys = new HashSet<>();

    public HashSet<Pair<String, String>> getMetadata() {
        return metadata;
    }

    public HashSet<String> getMetadataKeys() {
        return metadataKeys;
    }

    public ChildesMetadata() {
        super();
    }

    /**
     * Instantiates a new Childes metadata.
     *
     * @param url the url of the corpus file
     */
    public ChildesMetadata(URL url) {
        super();
        this.url = url ;

        try {
            // Copied from BasicTranscriptionData...
            // Get the url of the parent directory
            URI uri = url.toURI();
            // Either strip file name for file or get parent directory for folders
            URI parentURI = uri.getPath().endsWith("/") ? uri.resolve("..") : uri.resolve(".");
            parentUrl = parentURI.toURL();
            // ...up to here
            fileName = new File(uri).getName();
            baseName = FilenameUtils.getBaseName(fileName);
            BufferedReader input = new BufferedReader(new FileReader(new File(uri)));
            // read file line by line
            for (Object l : input.lines().toArray()) {
                String line = l.toString();
                // Split lines into keys and values and store the results in a hash map
                String[] pair = line.split(":\\s+");
                if (pair.length == 2) {
                    metadata.add(new Pair<>(pair[0],pair[1]));
                }
                metadataKeys.add(pair[0]);
            }
        }
        catch (Exception ex) {
            Logger.getLogger(ChildesMetadata.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public URL getURL() {
        return url;
    }

    @Override
    public URL getParentURL() {
        return parentUrl;
    }

    @Override
    public String getFilename() {
        return fileName;
    }

    @Override
    public String getFilenameWithoutFileEnding() {
        return baseName;
    }

    @Override
    public String toSaveableString() throws TransformerException, ParserConfigurationException, SAXException, IOException, JDOMException {
        return unformatedString;
    }

    @Override
    public String toUnformattedString() {
        return unformatedString;
    }

    @Override
    public void updateUnformattedString(String newUnformattedString) {
        this.unformatedString = newUnformattedString ;
    }

    @Override
    public Collection<String> getFileExtensions() {
        return Collections.singleton("cdc");
    }

    @Override
    public Collection<URL> getReferencedCorpusDataURLs() throws MalformedURLException, URISyntaxException {
        // Return empty list assuming that there are no corpus urls referenced in CHILDES meta-data
        return new ArrayList<>();
    }

    @Override
    public Object clone() {
        return new ChildesMetadata(this.url);
    }

    @Override
    public Location getLocation(String token) {
        return new Location("undefined","");
    }
}
