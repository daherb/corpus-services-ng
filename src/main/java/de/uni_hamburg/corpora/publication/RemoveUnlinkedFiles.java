/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uni_hamburg.corpora.publication;

import de.uni_hamburg.corpora.Corpus;
import de.uni_hamburg.corpora.CorpusData;
import de.uni_hamburg.corpora.CorpusFunction;
import de.uni_hamburg.corpora.Report;
import de.uni_hamburg.corpora.utilities.TypeConverter;
import java.io.File;
import java.io.IOException;
import java.util.*;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.xpath.XPathExpressionException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * This class removes all files from a corpus directory which are not linked in
 * the coma file
 *
 * @author sesv009
 */
public class RemoveUnlinkedFiles extends Publisher implements CorpusFunction {

    List<String> fileList;
    CorpusData comadata;
    String baseDirectory;
    final List<String> filenamewhitelist;

    public RemoveUnlinkedFiles(Properties properties) {
        super(properties);
        fileList = new ArrayList<String>();
        //these are the files we don't want to remove even if they are not in Coma
        filenamewhitelist = new ArrayList<String>();
        filenamewhitelist.add("_score.html");
        filenamewhitelist.add("_list.html");
    }

    /**
     * Remove unlinked files
     *
     * @param comadata CorpusData object
     */
    public Report removeFiles(CorpusData comadata) {

        Report stats = new Report();

        stats.addFix(function, comadata, "File:" + baseDirectory);

        // iterate through all files in the coma directory and its subdirectories
        walk(baseDirectory, stats);
        System.out.println("Done");

        return stats;
    }

    /**
     * Iterate through specific elements in a coma file and get all file
     * references from there and save them to fileList
     *
     * @param cd CorpusData object
     */
    public Report generateFileList(CorpusData cd) {

        Report stats = new Report();

        try {

            //firstly add path of coma file itself because it is not linked in itself
            fileList.add(cd.getFilename());

            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(TypeConverter.String2InputStream(cd.toSaveableString())); // get the file as a document

            //list of names of elements that provide a reference to a corpus file
            List<String> elementNames = Arrays.asList("relPath", "NSLink");

            //iterate through elements and save file paths to fileList
            for (int i = 0; i < elementNames.size(); i++) {
                NodeList elements = doc.getElementsByTagName(elementNames.get(i));
                for (int j = 0; j < elements.getLength(); j++) {
                    Element e = (Element) elements.item(j);
                    String c = e.getTextContent();
                    c = c.replace('/', File.separatorChar).replace('\\', File.separatorChar);
                    //c = c.substring(c.lastIndexOf(baseDirectory.replace('/', File.separatorChar).replace('\\', File.separatorChar)) + 1);
                    fileList.add(c);
                }
            }

        } catch (ParserConfigurationException ex) {
            stats.addException(function, ex, cd, "Unknown ParserConfigurationException.");
        } catch (TransformerException ex) {
            stats.addException(function, ex, cd, "Unknown TransformerException.");
        } catch (SAXException ex) {
            stats.addException(function, ex, cd, "Unknown SAXException.");
        } catch (IOException ex) {
            stats.addException(function, ex, cd, "Unknown IOException.");
        } catch (XPathExpressionException ex) {
            stats.addException(function, ex, cd, "Unknown XPathExpressionException.");
        }

        return stats;
    }

    public void walk(String path, Report stats) {

        File dir = new File(path);
        File[] foundFiles = dir.listFiles();

        for (File file : foundFiles) {
            if (file.isDirectory() && (!file.getName().startsWith("."))) {
                //disregard directories starting with "." and go further
                walk(file.getAbsolutePath(), stats);
            } else if (!file.getName().startsWith(".")) {

                // see if this file is in the file list from Coma
                // if it is not, then remove it from disk
                String name = file.getAbsolutePath().replace('/', File.separatorChar).replace('\\', File.separatorChar);

                //iterate through files linked in Coma and see if the current one is there
                Boolean keepFile = false;
                for (int i = 0; i < fileList.size(); i++) {
                    String linkedFile = fileList.get(i);
                    if (name.endsWith(linkedFile) || name.endsWith(filenamewhitelist.get(0)) || name.endsWith(filenamewhitelist.get(1))) {
                        keepFile = true;
                    }
                }

                if (keepFile) {
                    stats.addNote(function, comadata, "Keeping: " + name + " (found in Coma and file system).");
                } else {
                    File FileToRemove = new File(name);
                    if (FileToRemove.delete()) {
                        stats.addNote(function, comadata, "Removed: " + name + " (not found in Coma).");
                    } else {
                        stats.addWarning(function, comadata, "Removal unsuccessful: " + name + " (not found in Coma).");
                    }
                }
            }
        }

    }

    @Override
    public Report function(CorpusData cd) {

        //path of coma file
        comadata = cd;
        baseDirectory = comadata.getParentURL().getPath();

        Report stats = new Report();
        stats = generateFileList(cd);
        stats.merge(removeFiles(cd));
        return stats;
    }

    @Override
    public Report function(Corpus c) {

        //path of coma file
        comadata = c.getComaData();
        baseDirectory = comadata.getParentURL().getPath();

        Report stats = new Report();
        stats = generateFileList(cd);
        stats.merge(removeFiles(cd));
        return stats;
    }

    @Override
    public Collection<Class<? extends CorpusData>> getIsUsableFor() {
        try {
            Class cl = Class.forName("de.uni_hamburg.corpora.ComaData");
            IsUsableFor.add(cl);
        } catch (ClassNotFoundException ex) {
            report.addException(ex, "Usable class not found.");
        }
        return IsUsableFor;
    }

    @Override
    public String getDescription() {
        String description = "This class takes a coma file and removes all files from the directory/subdirectories "
                + "which are not linked somewhere in the coma file. ";
        return description;
    }

}
