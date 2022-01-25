package de.uni_hamburg.corpora.utilities.quest;

import org.jdom.Element;

import java.util.stream.Collectors;

/**
 * @author bba1792, Dr. Herbert Lange
 * @version 20220120
 */
public class XMLTools {
    /**
     * Returns all text contained in a DOM subtree given by the root element
     * @param e the root of the subtree
     * @return all text contained in the subtree
     */
    public static String showAllText(Element e) {
        String result = e.getText() + e.getChildren().stream().map((c) ->
                showAllText((Element) c)).collect(Collectors.joining(" "));
        return result.trim();
    }
}
