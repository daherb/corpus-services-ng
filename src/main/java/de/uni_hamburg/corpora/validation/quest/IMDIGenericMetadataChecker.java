package de.uni_hamburg.corpora.validation.quest;

import de.uni_hamburg.corpora.*;
import de.uni_hamburg.corpora.utilities.quest.XMLTools;
import org.jdom2.Attribute;
import org.jdom2.Element;
import org.jdom2.Namespace;
import org.jdom2.filter.Filters;
import org.jdom2.xpath.XPathBuilder;
import org.jdom2.xpath.jaxen.JaxenXPathFactory;

import java.io.FileNotFoundException;
import java.net.URL;
import java.util.*;

/**
 * Checker for the generic metadata within an IMDI corpus
 *
 * Last updated
 * @author Herbert Lange
 * @version 20240322
 */
public class IMDIGenericMetadataChecker extends GenericMetadataChecker implements CorpusFunction {

    /**
     * Default constructor not providing fixing options
     */
    public IMDIGenericMetadataChecker(Properties properties) throws FileNotFoundException {
        super(properties);
        if (properties != null && !properties.isEmpty() && properties.containsKey("imdi-criteria-file"))
            setCriteriaFile(properties.getProperty("imdi-criteria-file"));
        else {
            loadCriteriaResource("imdi-generic.csv");
        }
    }

    /**
     * Skip files in corpus structure
     */
    @Override
    protected boolean shouldBeChecked(URL filename) {
        return  !props.getProperty("skip-corpus-structure").equalsIgnoreCase("true") ||
                !filename.toString().toLowerCase().matches(".*corpus\\s*structure.*");
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
     * Function to retrieve the collection of all classes the checker is suitable for
     *
     * @return a collection of classes the checker is suitable for
     */
    @Override
    public Collection<Class<? extends CorpusData>> getIsUsableFor() {
        // Valid for IMDI format
        return Collections.singleton(IMDIData.class);
    }

    /**
     * Function to get a collection of values based on a locator
     *
     * @param locator the locator for the values
     * @return the values determined by the locator
     */
    @Override
    protected Report getValuesForLocator(CorpusData cd, String locator, Collection<String> values) {
        Report report = new Report();
        // Workaround for default namespace "" kind of following
        // http://www.edankert.com/defaultnamespaces.html
        XPathBuilder<?> xpb = new XPathBuilder<>(locator, Filters.attribute().or(Filters.fboolean()));
        xpb.setNamespace(Namespace.getNamespace("imdi", "http://www.mpi.nl/IMDI/Schema/IMDI"));
        List<? extends Object> nodes =
                xpb.compileWith(new JaxenXPathFactory()).evaluate(((IMDIData) cd).getJdom());
        // Convert nodes to string values
        for (Object o : nodes) {
            // Get the value of the node, either from an element or an attribute
            if (o instanceof Element) {
                values.add(XMLTools.showAllText((Element) o));
            }
            else if (o instanceof Attribute)
                values.add(((Attribute) o).getValue());
                // Result of a XPath predicate -> only keep if the result is true
                // This allows e.g. predicates where the counts of elements can be compared
            else if (o instanceof Boolean) {
                if ((Boolean) o)
                    values.add(((Boolean) o).toString());
            }
            else {
                // Error if it is neither element nor attribute
                report.addCritical(getFunction(), cd, "Unexpected object type: " + o.getClass().getName());
                break;
            }
        }
        return report ;

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

    @Override
    public Map<String, String> getParameters() {
        Map<String,String> p = super.getParameters();
        p.put("imdi-criteria-file", "The file for IMDI generic metadata criteria");
        p.put("skip-corpus-structure", "Flag determining if all files in corpus structure folders should be skipped");
        return p;
    }

}
