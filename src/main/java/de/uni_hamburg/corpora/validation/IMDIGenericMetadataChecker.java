package de.uni_hamburg.corpora.validation;


//import com.fasterxml.jackson.annotation.JsonAutoDetect;
//import com.fasterxml.jackson.annotation.PropertyAccessor;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.fasterxml.jackson.databind.SerializationFeature;
import com.opencsv.bean.*;
import de.uni_hamburg.corpora.*;
import org.exmaralda.partitureditor.fsm.FSMException;
import org.exmaralda.partitureditor.jexmaralda.JexmaraldaException;
import org.jdom.Attribute;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.jdom.xpath.XPath;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.*;
import java.io.*;
import java.net.*;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
//import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author bba1792 Dr. Herbert Lange
 * @version 20210727
 *
 * Checker for the generic metadata within an IMDI corpus
 */
public class IMDIGenericMetadataChecker extends Checker implements CorpusFunction {

    // The local logger that can be used for debugging
    private final Logger logger = Logger.getLogger(this.getClass().toString());

    // Flag to see if the checker isproperly set up
    private boolean setUp = false;

    // Data structure representing all metadata criteria
    private List<GenericMetadataCriterion> criteria;

    /**
     * Default constructor without parameter, not providing fixing options
     */
    public IMDIGenericMetadataChecker() {
        this(false);
    }

    /**
     * Default constructor with optional fixing option
     * @param hasfixingoption the fixing option
     */
    public IMDIGenericMetadataChecker(boolean hasfixingoption) {
        super(hasfixingoption);
    }

    /**
     * Function providing a description of a checker
     * @return the checker description
     */
    @Override
    public String getDescription() {
        return "Checks the generic metadata in an IMDI corpus";
    }

    /**
     * Checker function for a single document in a corpus
     * @param cd the corpus file
     * @param fix the fixing option
     * @return a detailed report of the checker
     * @throws NoSuchAlgorithmException inherited
     * @throws ClassNotFoundException inherited
     * @throws FSMException inherited
     * @throws URISyntaxException inherited
     * @throws SAXException inherited
     * @throws IOException inherited
     * @throws ParserConfigurationException inherited
     * @throws JexmaraldaException inherited
     * @throws TransformerException inherited
     * @throws XPathExpressionException inherited
     * @throws JDOMException inherited
     */
    @Override
    public Report function(CorpusData cd, Boolean fix) throws NoSuchAlgorithmException, ClassNotFoundException, FSMException, URISyntaxException, SAXException, IOException, ParserConfigurationException, JexmaraldaException, TransformerException, XPathExpressionException, JDOMException {
        // The corpus is IMDI data
        IMDIData imdi = (IMDIData) cd;
        Report report = new Report();
        // Only work if properly set up
        if (setUp) {
            for (GenericMetadataCriterion c : criteria) {
                // Collect all nodes that match either one of the XPath expressions
                List nodes = new ArrayList();
                for (String x : c.locator) {
                    // Ignore any property without locator
                    if (!x.equals("N/A")) {
                        //report.addNote(function, cd, "Element " + c.name + " not applicable");
                        String qx;
                        if (x.startsWith("/"))
                            qx = x;
                        else
                            // Workaround for incomplete XPath expressions
                            qx = "//" + x;
                        // Workaround for default namespace "" kind of following
                        // http://www.edankert.com/defaultnamespaces.html
                        XPath xpath = XPath.newInstance(qx.replaceAll("/([a-zA-Z])", "/imdi:$1"));
                        xpath.addNamespace(Namespace.getNamespace("imdi", "http://www.mpi.nl/IMDI/Schema/IMDI"));
                        nodes.addAll(xpath.selectNodes(imdi.getJdom()));
                    }
                }
                // Check if we have at least lower bound elements (if lower bounds are defined, i.e. not N/A)
                if (c.bounds.lower != GenericMetadataCriterion.Bounds.EBounds.NA && !c.locator.contains("N/A") &&
                        GenericMetadataCriterion.compareToBounds(nodes.size(), c.bounds.lower) < 0)
                    report.addCritical(function, cd, "Less than " + GenericMetadataCriterion
                            .Bounds.toString(c.bounds.lower) + " occurrences of " + c.name + " found: " + nodes.size());
                // Check if we have at most upper bound elements (if upper bounds are defined, i.e. not N/A)
                if (c.bounds.upper != GenericMetadataCriterion.Bounds.EBounds.NA && !c.locator.contains("N/A") &&
                        GenericMetadataCriterion.compareToBounds(nodes.size(), c.bounds.upper) > 0)
                    report.addCritical(function, cd, "More than " + GenericMetadataCriterion
                            .Bounds.toString(c.bounds.upper) + " occurrences of " + c.name + " found: " + nodes.size());
                // Now check all the results
                for (Object o : nodes) {
                    // Get the value of the node, either from an element or an attribute
                    String value;
                    if (o instanceof Element)
                        value = ((Element) o).getValue();
                    else if (o instanceof Attribute)
                        value = ((Attribute) o).getValue();
                    else {
                        // Error if it is neither element nor attribute
                        report.addCritical(function, cd, "Unexpected object type: " + o.getClass().getName());
                        break;
                    }
                    // DEBUG check the path for a property
//                    if (c.name.equalsIgnoreCase("PublicationYear")) {
//                        String path = getPathForElement((Element) o);
//                        logger.log(Level.INFO, "PublicationYear: " + path);
//                    }
                    // Check if we can parse the value as one of the valid types
                    boolean parsable = false;
                    for (Optional<String> t : c.type) {
                        // No type, we don't have to parse it
                        if (!t.isPresent())
                            parsable = true;
                        else {
                            // String is always valid
                            if (t.get().equalsIgnoreCase("string")) {
                                parsable = true;
                            }
                            // Testing an URI/URL
                            else if (t.get().equalsIgnoreCase("uri")) {
                                try {
                                    // Just check if it is a uri
                                    new URI(value);
                                    parsable = true;
                                } catch (URISyntaxException e) {
                                    report.addCritical(function, cd, "Invalid URI syntax " + e);
                                    break ;
                                }
                                // Also try it as a URL
                                URL url = null;
                                try {
                                    // Handle URIs can start with hdl: and can be resolved using a proxy
                                    if (value.startsWith("hdl:")) {
                                        url = new URL(value.replace("hdl:", "https://hdl.handle.net/"));

                                    }
                                    // DOI URIs can start with hdl: and can be resolved using a proxy
                                    else if (value.startsWith("doi:")) {
                                        url = new URL(value.replace("doi:", "https://doi.org/"));
                                    }
                                    // HTTP URIs are URLs
                                    else if (value.startsWith("http")) {
                                        url = new URL(value);
                                    }
                                } catch (MalformedURLException e) {
                                    report.addWarning(function, cd, "Malformed URL " + e);
                                    break ;
                                }
                                // If we succeed in creating a URL object we can try to connect
                                /*if (url != null) {
                                    try {
                                        TimeUnit.MILLISECONDS.sleep(500);
                                        HttpURLConnection con = ((HttpURLConnection) url.openConnection());
                                        con.setRequestMethod("GET");
                                        int code = con.getResponseCode();
                                        if (code != 200)
                                            report.addWarning(function, cd, "Error connecting to url " + url + ": " + code);
                                    } catch (IOException | InterruptedException e) {
                                        report.addWarning(function, cd, "Exception when connecting to url " + e);
                                    }

                                }*/

                            }
                            // Check date
                            else if (t.get().equalsIgnoreCase("date")) {
                                // Try various date formats
                                try {
                                    new SimpleDateFormat("yyyy-MM-dd").parse(value);
                                    parsable = true;
                                } catch (ParseException e) {
                                    parsable = false;
                                }
                                if (!parsable)
                                    try {
                                        new SimpleDateFormat("yyyy-MM").parse(value);
                                        parsable = true;
                                    } catch (ParseException e) {
                                        parsable = false;
                                    }
                                if (!parsable)
                                    try {
                                        new SimpleDateFormat("yyyy").parse(value);
                                        parsable = true;
                                    } catch (ParseException e) {
                                        parsable = false;
                                    }
                            }
                        }
                    }
                    // Error if everything failed
                    if (!parsable)
                        report.addCritical(function, cd, "No valid type given for value " + value +
                                " (Attempted " + c.type + ")");
                }
            }
        } else
            report.addCritical(function, cd, "No criteria file loaded");
        return report;
    }

    /**
     * Checker function for a corpus
     * @param c the corpus
     * @param fix the fixing parameter
     * @return detailed report of the checker
     * @throws NoSuchAlgorithmException inherited
     * @throws ClassNotFoundException inherited
     * @throws FSMException inherited
     * @throws URISyntaxException inherited
     * @throws SAXException inherited
     * @throws IOException inherited
     * @throws ParserConfigurationException inherited
     * @throws JexmaraldaException inherited
     * @throws TransformerException inherited
     * @throws XPathExpressionException inherited
     * @throws JDOMException inherited
     */
    @Override
    public Report function(Corpus c, Boolean fix) throws NoSuchAlgorithmException, ClassNotFoundException, FSMException, URISyntaxException, SAXException, IOException, ParserConfigurationException, JexmaraldaException, TransformerException, XPathExpressionException, JDOMException {
        Report report = new Report();
        if (setUp) {
            // Apply function for each supported file
            Collection usable = this.getIsUsableFor();
            for (CorpusData cdata : c.getCorpusData()) {
                if (usable.contains(cdata.getClass())) {
                    report.merge(function(cdata, fix));
                }
            }
        } else
            report.addCritical(function, "No criteria file loaded");
        return report;
    }

    /**
     * Function to retrieve the collection of all classes the checker is suitable for
     *
     * @return a collection of classes the checker is suitable for
     * @throws ClassNotFoundException inherited
     */
    @Override
    public Collection<Class<? extends CorpusData>> getIsUsableFor() throws ClassNotFoundException {
        // Valid for IMDI format
        return Collections.singleton(IMDIData.class);
    }

    /**
     * Function to load a criteria file and set up the checker
     * @param filename
     */
    public void setCriteriaFile(String filename) {
        try {
            // DEBUG
            //logger.log(Level.INFO, "Filename: " + filename);
            //logger.log(Level.INFO, "Reading CSV " + filename);
            // Read CSV file
            criteria = new CsvToBeanBuilder<GenericMetadataCriterion>(new FileReader(filename))
                    .withType(GenericMetadataCriterion.class)
                    .withSkipLines(1) // skip header
                    .build()
                    .parse();
            // DEBUG write criteria to log
            /*for (GenericMetadataCriterion c : criteria) {
                logger.log(Level.INFO,"Name: " + c.name + ", Bounds: " + c.bounds.lower + " - " + c.bounds.upper +
                        ", Type: " + c.type + ", XPath: " + c.locator);
            }*/
            // DEBUG Write criteria to json file
            /*ObjectMapper mapper = new ObjectMapper();
            // Allows serialization even when getters are missing
            mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY);
            mapper.configure(SerializationFeature.INDENT_OUTPUT, true);
            try {
                FileWriter fw = new FileWriter("/tmp/imdi.json");
                fw.write(mapper.writeValueAsString(criteria));
                fw.close();
            } catch (Exception e) {
                e.printStackTrace();
            }*/
            // Checker is properly set up
            setUp = true;
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Encountered exception when loading criteria ", e);
        }
    }

    /**
     * Function to get the path for an element within an XML document
     * @param e the element
     * @return the path as a string
     */
    public static String getPathForElement(Element e) {
        LinkedList<String> path = new LinkedList<>();
        Element cur = e;
        while (!cur.equals(cur.getDocument().getRootElement())) {
            path.addFirst(cur.getName());
            cur = cur.getParentElement();
        }
        Optional<String> optPath = path.stream().reduce((s1, s2) -> s1 + "/" + s2);
        return optPath.orElse("") ;
    }
}
