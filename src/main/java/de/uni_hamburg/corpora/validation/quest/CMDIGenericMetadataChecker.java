package de.uni_hamburg.corpora.validation.quest;

import de.uni_hamburg.corpora.*;
import de.uni_hamburg.corpora.utilities.quest.XMLTools;
import org.jdom.Attribute;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.Namespace;
import org.jdom.xpath.XPath;

import java.util.*;

/**
 * @author bba1792 Dr. Herbert Lange
 * @version 20210728
 *
 * Checker for the generic metadata within an IMDI corpus
 */
public class CMDIGenericMetadataChecker extends GenericMetadataChecker implements CorpusFunction {

    /**
     * Default constructor not providing fixing options
     */
    public CMDIGenericMetadataChecker(Properties properties) {
        super(properties);
        if (properties != null && !properties.isEmpty() && properties.containsKey("cmdi-criteria-file"))
            setCriteriaFile(properties.getProperty("cmdi-criteria-file"));
        else {
            loadCriteriaResource("cmdi-generic.csv");
        }
    }


    /**
     * Function providing a description of a checker
     * @return the checker description
     */
    @Override
    public String getDescription() {
        return "Checks the generic metadata in a CMDI corpus";
    }

    /**
     * Function to retrieve the collection of all classes the checker is suitable for
     *
     * @return a collection of classes the checker is suitable for
     */
    @Override
    public Collection<Class<? extends CorpusData>> getIsUsableFor() {
        // Valid for CMDI format
        return Collections.singleton(CmdiData.class);
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
        try {
            XPath xpath = XPath.newInstance(locator);
            xpath.addNamespace(Namespace.getNamespace("cmd", "http://www.clarin.eu/cmd/"));
            List<Object> nodes = xpath.selectNodes(((CmdiData) cd).getJdom());
            // Convert nodes to string values
            for (Object o : nodes) {
                // Get the value of the node, either from an element or an attribute
                if (o instanceof Element)
                    values.add(XMLTools.showAllText((Element) o));
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
        }
        catch (JDOMException e) {
            e.printStackTrace();
        }
        return report;
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
        return String.join("/", path);
    }

    @Override
    public Map<String, String> getParameters() {
        Map<String,String> p = super.getParameters();
        p.put("cmdi-criteria-file", "The file for CMDI generic metadata criteria");
        return p;
    }
}
