package de.uni_hamburg.corpora;

import de.uni_hamburg.corpora.utilities.PrettyPrinter;
import de.uni_hamburg.corpora.utilities.TypeConverter;

import java.io.*;

import static java.lang.System.out;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpressionException;

import org.apache.commons.io.IOUtils;
import org.exmaralda.partitureditor.jexmaralda.JexmaraldaException;
import org.jdom.Document;
import org.jdom.JDOMException;
import org.jdom.output.XMLOutputter;
import org.reflections.Reflections;
import org.xml.sax.SAXException;

/**
 * Still to do
 *
 * @author fsnv625
 */
public class CorpusIO {
    Collection<CorpusData> cdc = new HashSet<>();
    Collection<URL> alldata = new ArrayList<>();
    Collection<Class<? extends CorpusData>> allCorpusDataTypes = new ArrayList<>();

    public CorpusIO() {
        // Use reflections to get all corpus data classes
        Reflections reflections = new Reflections("de.uni_hamburg.corpora");
        // Get all classes derived from CorpusData and add to list, using stream api
        allCorpusDataTypes.addAll(reflections.getSubTypesOf(CorpusData.class).stream()
                // But only select public classes that are not abstract
                .filter((cd) -> Modifier.isPublic(cd.getModifiers()) && !Modifier.isAbstract(cd.getModifiers()))
                // And convert the stream back to a set
                .collect(Collectors.toSet()));
    }

    public String CorpusData2String(CorpusData cd) throws TransformerException, ParserConfigurationException, SAXException, IOException, XPathExpressionException {
        return cd.toSaveableString();
    }

    public void write(CorpusData cd, URL url) throws TransformerException, ParserConfigurationException, SAXException, IOException, XPathExpressionException {
        write(cd.toSaveableString(), url);
    }

    //TODO
    public void write(String s, URL url) throws IOException {
        //If URL is on fileserver only...
        System.out.println("started writing document...");
        outappend("============================\n");
        FileOutputStream fos = new FileOutputStream(url.getFile());
        fos.write(s.getBytes(StandardCharsets.UTF_8));
        fos.close();
        System.out.println("Document written...");
    }

    public void write(Document doc, URL url) throws IOException, TransformerException, ParserConfigurationException, SAXException, XPathExpressionException {
        XMLOutputter xmOut = new XMLOutputter();
        String unformattedCorpusData = xmOut.outputString(doc);
        PrettyPrinter pp = new PrettyPrinter();
        String prettyCorpusData = pp.indent(unformattedCorpusData, "event");
        write(prettyCorpusData, url);
    }

    public void write(org.w3c.dom.Document doc, URL url) throws IOException, TransformerException, ParserConfigurationException, SAXException, XPathExpressionException {
        String unformattedCorpusData = TypeConverter.W3cDocument2String(doc);
        PrettyPrinter pp = new PrettyPrinter();
        String prettyCorpusData = pp.indent(unformattedCorpusData, "event");
        write(prettyCorpusData, url);
    }

    public void outappend(String a) {
        Calendar cal = Calendar.getInstance(TimeZone.getDefault());
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
        String time = sdf.format(cal.getTime());
        out.append("[" + time + "] ");
        out.append(a);
    }

    public void write(Collection<CorpusData> cdc, URL url) {
        //TODO
    }

    /*
     * The following methods need to be in the Iterators for Coma and CMDI that don't exist yet
     *

     public abstract Collection getAllTranscripts();

     public abstract Collection getAllAudioFiles();

     public abstract Collection getAllVideoFiles();

     public abstract String getAudioLinkForTranscript();

     public abstract String getVideoLinkForTranscript();

     */
    //read a single file as a corpus data object from an url
    //only read it if it is needed
    public CorpusData readFileURL(URL url, Collection<Class<? extends CorpusData>> clcds) throws SAXException, JexmaraldaException, ClassNotFoundException, UnsupportedEncodingException {
        if (new File(URLDecoder.decode(url.getFile(),"UTF-8")).isFile()) {
            if (url.getPath().toLowerCase().contains("corpusservices_errors")) {
                // Ignore
            }
            if (url.getPath().toLowerCase().endsWith("xml") && url.getPath().toLowerCase().contains("annotation") && clcds.contains(AnnotationSpecification.class)) {
                return new AnnotationSpecification(url);
            }
            // An xml file is a CMDI file if it is:
            // - an xml file
            // - in a path containing cmdi
            // - we expect CMDI files in one of the checkers
            else if (url.getPath().toLowerCase().endsWith("xml")
                    && url.getPath().toLowerCase().contains("cmdi") && clcds.contains(CmdiData.class)) {
                return new CmdiData(url);
            } else {
                // TODO this method does not work properly e.g. for the suffix xml. Curent workaround: treat xml separately
                if (url.getPath().toLowerCase().endsWith(".xml")) {
                    out.println("Read " + url);
                    return new UnspecifiedXMLData(url);
                }
                for (Class<? extends CorpusData> c : clcds) {
                    try {
                        CorpusData cd = c.getDeclaredConstructor(URL.class).newInstance(url);
                        for (String e : cd.getFileExtensions()) {
                            // Check extension including the dot
                            if (url.getPath().toLowerCase().endsWith("." + e)) {
                                out.println("Read " + url);
                                return cd;
                            }
                        }
                    } catch (NoSuchMethodException | InvocationTargetException | InstantiationException | IllegalAccessException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        // No suitable format found
        System.out.println(url + " will not be read");
        return null;
    }

    /**
     * Generates the list of all known file extensions for corpus data objects
     * @author bba1792, Dr. Herbert lange
     * @version 20210924
     * @return the list of all known file extensions for corpus data objects
     */
    private Collection<String> getAllExtensions() {
        Set<String> allExts = new HashSet<>();
        for (Class c : allCorpusDataTypes) {
            try {
                // Create an actual object from the class using reflections to access the constructor
                CorpusData cd = (CorpusData) c.getDeclaredConstructor().newInstance();
                // Add all extensions. Also include the dot
                allExts.addAll(cd.getFileExtensions().stream().map((e) -> "." + e).collect(Collectors.toList()));
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException | InstantiationException e) {
                // Basically do nothing in case of an exception
                allExts.addAll(Collections.EMPTY_SET);
            }
        }
        return allExts ;
    }

    //read a single file as a corpus data object from an url
    public CorpusData readFileURL(URL url) throws SAXException, JexmaraldaException, ClassNotFoundException, UnsupportedEncodingException {
        return readFileURL(url, allCorpusDataTypes);
    }

    //read all the files as corpus data objects from a directory url
    public Collection<CorpusData> read(URL url, Report report) throws URISyntaxException, IOException, SAXException,
            JexmaraldaException,
            ClassNotFoundException {
        return read(url,allCorpusDataTypes, report);
    }

    //read only the files as corpus data objects from a directory url that are specified in the Collection
    public Collection<CorpusData> read(URL url, Collection<Class<? extends CorpusData>> chosencdc, Report report) throws URISyntaxException,
            IOException, SAXException, JexmaraldaException, ClassNotFoundException {
        //To do
        alldata = URLtoList(url, report);
        for (URL readurl : alldata) {
            CorpusData cdread = readFileURL(readurl,chosencdc);
            if (cdread != null) {
                // cdc is a set so we don't have to check if the file is already in there
                cdc.add(cdread);
            }
        }
        return cdc;
    }

    public String readInternalResourceAsString(String path2resource) throws JDOMException, IOException {
        String xslstring = TypeConverter.InputStream2String(getClass().getResourceAsStream(path2resource));
        System.out.println(path2resource);
        if (xslstring == null) {
            throw new IOException("Stylesheet not found!");
        }
        return xslstring;
    }

    public String readExternalResourceAsString(String path2resource) throws JDOMException, IOException, URISyntaxException {
        String xslstring = new String(Files.readAllBytes(Paths.get(new URL(path2resource).toURI())));
        System.out.println(path2resource);
        return xslstring;
    }

    public Collection<URL> URLtoList(URL url, Report report) throws URISyntaxException, IOException {
        if (isLocalFile(url)) {
            //if the url points to a directory
            if (isDirectory(url)) {
                //we need to iterate    
                //and add everything to the list
                Path path = Paths.get(url.toURI());
                for (URL urlread : listFiles(path, report)) {
                    if (!isDirectory(urlread)) {
                        alldata.add(urlread);
                    }
                }
                return alldata;
            } //if the url points to a file
            else {
                //we need to add just this file
                alldata.add(url);
                return alldata;
            }
        } else {
            //it's a datastream in the repo
            //TODO later          
            return null;
        }
    }

    /**
     * Whether the URL is a file in the local file system.
     */
    public static boolean isLocalFile(java.net.URL url) {
        String scheme = url.getProtocol();
        return "file".equalsIgnoreCase(scheme) && !hasHost(url);
    }

    /**
     * Whether the URL is a directory in the local file system.
     */
    public static boolean isDirectory(java.net.URL url) throws URISyntaxException {
        //return new File(url.toURI()).isDirectory();
        return Files.isDirectory(Paths.get(url.toURI()));
    }

    public static boolean hasHost(java.net.URL url) {
        String host = url.getHost();
        return host != null && !"".equals(host);
    }

    public void writePrettyPrinted(CorpusData cd, URL url) throws TransformerException, ParserConfigurationException, SAXException, IOException, XPathExpressionException {
        write(cd.toSaveableString(), url);
    }

    public void copyInternalBinaryFile(String internalPath, URL url) throws IOException {
        InputStream in = getClass().getResourceAsStream(internalPath);
        OutputStream out = new FileOutputStream(url.getFile());
        if (in != null)
            IOUtils.copy(in, out);
        //else
        // TODO show some error
    }

    Collection<URL> listFiles(Path path, Report report) throws IOException {
        Collection<URL> recursed = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(path)) {
            for (Path entry : stream) {
                try {
                    // If we are in a directory we call ourself recursively
                    if (Files.isDirectory(entry)) {
                        recursed.addAll(listFiles(entry, report));
                    }
                    // Othwereise we check the file extension if we know it
                    // First getting the file name
                    String sentry = entry.getFileName().toString().toLowerCase();
                    // Getting all known extensions
                    Collection<String> allExts = getAllExtensions();
                    // Check if eny of these extensions happens to be the final part of sentry
                    // if yes we add the file to the list
                    if (allExts.stream().map(sentry::endsWith).reduce(Boolean::logicalOr).orElse(false)) {
                        recursed.add(entry.toUri().toURL());
                    }
                }
                catch (IOException e) {
                    report.addException("CorpusIO", e, "Exception when reading file " + entry);
                }
            }
        }
        return recursed;
    }
}
