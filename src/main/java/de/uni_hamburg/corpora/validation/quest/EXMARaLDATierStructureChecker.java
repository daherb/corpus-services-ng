package de.uni_hamburg.corpora.validation.quest;

import com.google.common.collect.Lists;
import de.uni_hamburg.corpora.*;
import org.jdom.Attribute;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.xpath.XPath;

import java.util.*;

/**
 * Tier structure checker for EXMARaLDA corpora
 *
 * @author bba1792, Dr. Herbert Lange
 * @version 20220211
 */
public class EXMARaLDATierStructureChecker extends TierStructureChecker {

    public EXMARaLDATierStructureChecker(Properties properties) {
        super(properties);
    }

    @Override
    public Collection<Class<? extends CorpusData>> getIsUsableFor() {
        return Collections.singleton(EXMARaLDACorpusData.class);
    }

    @Override
    Set<Map<String, String>> getTierStructure(Report report, CorpusData cd) {
        Set<Map<String,String>> tiers = new HashSet<>();
        if (cd instanceof EXMARaLDACorpusData) {
            try {
                List<Element> tierElements =
                        Lists.newArrayList(XPath.newInstance("//tier").selectNodes(((EXMARaLDACorpusData) cd).getJdom()));
                for (Element e : tierElements) {
                    Map<String,String> tierAttribs = new HashMap<>();
                    {
                        Attribute id = e.getAttribute("id");
                        if (id != null)
                            tierAttribs.put("id", id.getValue());
                    }
                    {
                        Attribute name = e.getAttribute("name");
                        if (name != null)
                            tierAttribs.put("name", name.getValue());
                    }
                    {
                        Attribute category = e.getAttribute("category");
                        if (category != null)
                            tierAttribs.put("category", category.getValue());
                    }
                    {
                        Attribute type = e.getAttribute("type");
                        if (type != null)
                            tierAttribs.put("type", type.getValue());
                    }
//                    {
//                    Attribute speaker = e.getAttribute("speaker");
//                    if (speaker != null)
//                        tierAttribs.put("speaker", speaker.getValue());
//                    }
//                    {
//                        Attribute parent = e.getAttribute("PARENT_REF");
//                        if (parent != null)
//                            tierAttribs.put("parent", parent.getValue());
//                    }
                    tiers.add(tierAttribs);
                }
            } catch (JDOMException e) {
                report.addCritical(getFunction(),ReportItem.newParamMap(
                        new String[]{"function","description","exception"},
                        new Object[]{getFunction(),"Exception when extracting tiers from " + cd.getFilename(), e}
                ));
            }
        }
        else
            report.addCritical(getFunction(), ReportItem.newParamMap(
                    new String[]{"function", "description"},
                    new Object[]{getFunction(),"Not an EXMARaLDA EXB file: " + cd.getFilename()}));
        return tiers;
    }
}
