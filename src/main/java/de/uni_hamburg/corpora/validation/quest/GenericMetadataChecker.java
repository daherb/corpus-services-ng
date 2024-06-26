package de.uni_hamburg.corpora.validation.quest;

import com.opencsv.bean.CsvToBeanBuilder;
import de.uni_hamburg.corpora.*;
import de.uni_hamburg.corpora.validation.Checker;
import org.exmaralda.partitureditor.fsm.FSMException;
import org.exmaralda.partitureditor.jexmaralda.JexmaraldaException;
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
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Abstract Checker for the generic metadata
 *
 * Last updated
 * @author Herbert Lange
 * @version 20240405
 */
abstract public class GenericMetadataChecker extends Checker implements CorpusFunction {

    // Regex to match for an empty string
    private final String emptyString = "[\\s\\n]+";

    // The local logger that can be used for debugging
    final Logger logger = Logger.getLogger(this.getClass().toString());

    // Flag to see if the checker is properly set up
    boolean setUp = false;

    // Flag to see if we should show full summary
    boolean showFullSummary = false;

    // Data structure representing all metadata criteria
    List<GenericMetadataCriterion> criteria;

    // Set of criteria names to be ignored, can be set via a parameter
    Set<String> ignoredCriteria = new HashSet<>();

    // Set of criteria names to be included in the summary
    Set<String> summaryCriteria = new HashSet<>();

    // Data structure to keep track of all the values in a corpus, mapping from criterion to value to count
    HashMap<String,HashMap<String,Integer>> allValues = new HashMap<>();

    // Data structure to keep track of errors in a corpus, mapping from criterion to count
    HashMap<String,Integer> errorCount = new HashMap<>();

    /**
     * Default constructor without parameter, not providing fixing options
     */
    public GenericMetadataChecker(Properties properties) {
        super(false, properties);
        // If parameters contain list of criteria, split the values on commas and add as lower-case
        if (properties.containsKey("ignore-criteria")) {
            ignoredCriteria.addAll(Stream.of(properties.getProperty("ignore-criteria").split(","))
                    .map(String::toLowerCase).toList());
        }
        if (properties.containsKey("metadata-summary") && !properties.getProperty("metadata-summary").equalsIgnoreCase(
                "true"))
            summaryCriteria.addAll(
                    Stream.of(props.getProperty("metadata-summary").split(","))
                            .map(String::toLowerCase).collect(Collectors.toSet()));
        if (properties.containsKey("full-summary") && properties.getProperty("full-summary").equalsIgnoreCase(
                "true"))
            showFullSummary = true;
        	// TODO actually implement full summary
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
        Report report = new Report();
        // Used to match empty optionals
        final Optional<String> emptyStr = Optional.empty();
        // Only work if properly set up
        if (setUp && shouldBeChecked(cd.getURL())) {
            for (GenericMetadataCriterion c : criteria) {
                if (!ignoredCriteria.contains(c.name.toLowerCase())) {
                    // Collect all values of properties that match one of the locators
                    ArrayList<String> values = new ArrayList<>();
                    for (String locator : c.locator) {
                        // Ignore any property without locator
                        if (!locator.equals("N/A")) {
                            // get the values based on the locator
                            report.merge(getValuesForLocator(cd, locator, values));
                        }
                    }
                    // Check if we have at least lower bound elements (if lower bounds are defined, i.e. not N/A)
                    if (c.bounds.lower != GenericMetadataCriterion.Bounds.EBounds.NA && !c.locator.contains("N/A") &&
                            GenericMetadataCriterion.compareToBounds(values.size(), c.bounds.lower) < 0) {
                        report.addCritical(getFunction(), cd, "Less than " + GenericMetadataCriterion
                                .Bounds.toString(c.bounds.lower) + " occurrences of " + c.name + " found: " + values.size());
                        errorCount.compute(c.name,(k,v) -> (v == null) ? 1 : v + 1);
                    }
                    // Check if we have at most upper bound elements (if upper bounds are defined, i.e. not N/A)
                    else if (c.bounds.upper != GenericMetadataCriterion.Bounds.EBounds.NA && !c.locator.contains("N/A") &&
                            GenericMetadataCriterion.compareToBounds(values.size(), c.bounds.upper) > 0) {
                        report.addCritical(getFunction(), cd, "More than " + GenericMetadataCriterion
                                .Bounds.toString(c.bounds.upper) + " occurrences of " + c.name + " found: " + values.size());
                    errorCount.compute(c.name,(k,v) -> (v == null) ? 1 : v + 1);
                    }
                    // Store all values that have a reasonable type for potential statistics
                    // (Ab)use sets to remove duplicates
                    if (!c.type.contains(emptyStr)) {
                        if (!allValues.containsKey(c.name))
                            allValues.put(c.name,new HashMap<>());
                        for (String val : values)
                            if (val.isEmpty() || val.matches(emptyString))
                                allValues.get(c.name).compute("#EMPTY#", (k,v) -> (v == null) ? 1 : v + 1);
                            else
                                allValues.get(c.name).compute(val, (k,v) -> (v == null) ? 1 : v + 1);
                    }
                    // Now check all the results but ignore problems with optional fields
                    if (!c.bounds.lower.equals(GenericMetadataCriterion.Bounds.EBounds.B0) &&
                            values.stream().map((v) -> v.isEmpty() || v.matches(emptyString))
                                    .reduce(Boolean::logicalAnd).orElse(false) &&
                                c.type.stream().map(Optional::isPresent).reduce(Boolean::logicalOr).orElse(false)) {
                            report.addCritical(getFunction(),cd,
                                    "Only empty values encountered for criterion " + c.name);
                        }
                    for (String value : values) {
                        // Get the value of the node, either from an element or an attribute
                        // Check if we can parse the value as one of the valid types
                        boolean parsable = false;
                        for (Optional<String> t : c.type) {
                            // No type, we don't have to parse it
                            if (t.isEmpty())
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
//                                    // We only want to create a log item if there is no fallback to string
//                                    if (!c.type.stream().map((o) -> o.orElse("").toLowerCase(Locale.ROOT))
//                                            .collect(Collectors.toSet()).contains("string"))
//                                        report.addWarning(getFunction(), cd, c.name + ": Invalid URI syntax " + e);
//                                    // But we skip the rest of this iteration
//                                    continue ;
                                        parsable = false;
                                    }
                                    // Also try it as a URL
                                    try {
                                        // Handle URIs can start with hdl: and can be resolved using a proxy
                                        if (value.startsWith("hdl:")) {
                                            new URL(value.replace("hdl:", "https://hdl.handle.net/"));
                                        }
                                        // DOI URIs can start with hdl: and can be resolved using a proxy
                                        else if (value.startsWith("doi:")) {
                                            new URL(value.replace("doi:", "https://doi.org/"));
                                        }
                                        // HTTP URIs are URLs
                                        else if (value.startsWith("http")) {
                                            new URL(value);
                                        }
                                    } catch (MalformedURLException e) {
//                                    // We only want to create a log item if there is no fallback to string
//                                    if (!c.type.stream().map((o) -> o.orElse("").toLowerCase(Locale.ROOT))
//                                            .collect(Collectors.toSet()).contains("string"))
//                                        report.addWarning(getFunction(), cd, c.name + ": Malformed URL " + e);
//                                    // But we skip the rest of this iteration
//                                    continue ;
                                        parsable = false;
                                    }
                                    // If we succeed in creating a URL object we can try to connect
                                    /*if (url != null) {
                                    try {
                                        TimeUnit.MILLISECONDS.sleep(500);
                                        HttpURLConnection con = ((HttpURLConnection) url.openConnection());
                                        con.setRequestMethod("GET");
                                        int code = con.getResponseCode();
                                        if (code != 200)
                                            report.addWarning(getFunction(), cd, "Error connecting to url " + url + ": " + code);
                                    } catch (IOException | InterruptedException e) {
                                        report.addWarning(getFunction(), cd, "Exception when connecting to url " + e);
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
                                        } catch (ParseException ignored) {
                                        }
                                    if (!parsable)
                                        try {
                                            new SimpleDateFormat("yyyy").parse(value);
                                            parsable = true;
                                        } catch (ParseException ignored) {
                                        }
                                }
                            }
                        }
                        // Error if everything failed
                        if (!parsable) {
                            // Get all the types we attempted, if they don't exist replace them by n/a and join them
                            // using commas
                            String attemptedTypes =
                                    String.join(",",
                                            c.type.stream().map((s) -> s.orElse("n/a")).collect(Collectors.toSet()));
                            // Create error message for problematic value. Only warn if it is unknown but create error if unspecified
                            if (value.equals("Unknown"))
                                report.addWarning(getFunction(), cd, c.name + ": Unexpected value " + value +
                                        ". Expected type: " + attemptedTypes);
                            else
                                report.addWarning(getFunction(), cd, c.name + ": Unexpected value " + value +
                                        ". Expected type: " + attemptedTypes);
                        }
                    }
                }
            }
        }
        else if (!setUp)
            report.addCritical(getFunction(), cd, "No criteria file loaded");
        return report;
    }

    /**
     * Determines if a file should be checked depending on the filename
     * @param filename the filename
     * @return if the file should be checked
     */
    protected boolean shouldBeChecked(URL filename) {
        return true;
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
        boolean checked = false;
        Collection<Class<? extends CorpusData>> usable = this.getIsUsableFor();
        if (setUp) {
            // Apply function for each supported file
            for (CorpusData cdata : c.getCorpusData()) {
                if (usable.contains(cdata.getClass())) {
                    report.merge(function(cdata, fix));
                    checked = true;
                }
            }
            if (!checked) {
                report.addWarning(getFunction(),ReportItem.newParamMap(
                        new ReportItem.Field[]{ReportItem.Field.Function, ReportItem.Field.Description},
                        new Object[]{getFunction(), "No metadata files found matching supported formats: " +
                        usable.stream().map(Class::getSimpleName)
                                .collect(Collectors.joining(","))
                        }
                ));
            }
            // Add statistics of the parameter is set
            if (props.containsKey("metadata-summary") && !props.getProperty("metadata-summary")
                    .equalsIgnoreCase("false")) {
                StringBuilder stats = new StringBuilder();
                for (String critName : allValues.keySet()) {
                    if (summaryCriteria.contains(critName.toLowerCase()) || summaryCriteria.isEmpty()){
                        // Copy the map for the current criterion
                        HashMap<String,Integer> vals = new HashMap<>(allValues.get(critName));
                        // Remove the placeholder for empty values
                        vals.remove("#EMPTY#");
                        stats.append("\n");
                        stats.append(critName);
                        stats.append(" (");
                        stats.append(errorCount.getOrDefault(critName,0));
                        stats.append(" errors, ");
                        stats.append(vals.size());
                        stats.append(" distinct and ");
                        // Get the count of empty values
                        stats.append(
                                allValues.get(critName).getOrDefault("#EMPTY#",0));
                        stats.append(" empty values)");
                        stats.append(":\n");
                        // Sort values with most frequent first
                        ArrayList<String> valsList = new ArrayList<>(vals.keySet());
                        valsList.sort(Comparator.comparingInt(vals::get).reversed());
                        for (String val : valsList) {
                            stats.append(" - ");
                            stats.append(val);
                            stats.append(" (");
                            stats.append(vals.get(val));
                            stats.append(")\n");

                        }
                    }
                }
                report.addNote(getFunction(), getFunction() + " summary\n" +
                        c.getCorpusData().stream().map(CorpusData::getURL).filter(this::shouldBeChecked).count() +
                        " files checked\n" + stats);
            }
        } else
            report.addCritical(getFunction(), "No criteria file loaded");
        return report;
    }

    /**
     * Function to get a collection of values based on a locator
     *
     * @param locator the locator for the values
     * @return the values determiend by the locator
     */
    protected abstract Report getValuesForLocator(CorpusData cd, String locator, Collection<String> values);

    /**
     * Function to load a criteria file and set up the checker
     * @param filename the criteria as a csv file to be loaded
     * @return if loading succeeded
     */
    public boolean loadCriteriaFile(String filename) {
        try {
            // Read CSV file
            criteria = new CsvToBeanBuilder<GenericMetadataCriterion>(new FileReader(filename))
                    .withType(GenericMetadataCriterion.class)
                    .withSkipLines(1) // skip header
                    .build()
                    .parse();
            return true;
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Encountered exception when loading criteria ", e);
            return false;
        }
    }

    /**
     * Loads the criteria as a resource
     * @param name the name of the resource
     * @return if loading succeeded
     */
    public boolean loadCriteriaResource(String name) throws FileNotFoundException {
            InputStream resourceStream = this.getClass().getClassLoader().getResourceAsStream("metadata/"+name);
            if (resourceStream != null) {
                // Read CSV file
                criteria = new CsvToBeanBuilder<GenericMetadataCriterion>(new InputStreamReader(resourceStream))
                        .withType(GenericMetadataCriterion.class)
                        .withSkipLines(1) // skip header
                        .build()
                        .parse();
                return true;
            }
            else {
                logger.log(Level.SEVERE,"Missing file in resources: ",name);
                return false;
            }
    }

    /**
     * Loads the criteria either as a resource or as a file. Tries resource first and if that fails tries file instead
     * @param fileOrResource the name of the file or resource
     * @return if loading succeeded
     */
    public boolean setCriteria(String fileOrResource) {
    	try {
    		return loadCriteriaResource(fileOrResource) || loadCriteriaFile(fileOrResource);
    	}
    	catch (IOException e) {
    		return false;
    	}
    }
    
    @Override
    public Map<String, String> getParameters() {
        Map<String, String> params = super.getParameters();
        params.put("ignore-criteria", "Comma-separated list of criteria to be ignored by the checker");
        params.put("metadata-summary","Flag determining if a summary should be generated. Alternatively, " +
                "comma-separated list of fields to be included in the summary");
        params.put("full-summary", "Flag determining if the full summary, i.e. the list of all distinct values" +
                "should be included");
        return params;
    }
}
