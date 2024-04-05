package de.uni_hamburg.corpora.utilities.quest;

import org.jdom2.Element;

import java.util.stream.Collectors;

/**
 * Last updated
 * @author Herbert Lange
 * @version 20240322
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
