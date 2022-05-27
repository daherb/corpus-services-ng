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
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * This class zips all the needed files of the corpus into a zip folder
 *
 * It only takes exb, exs, coma, pdf and optionally mp3, and the folder
 * structure
 *
 * http://www.mkyong.com/java/how-to-compress-files-in-zip-format/
 *
 * @author fsnv625
 */
public class ZipCorpus extends Publisher implements CorpusFunction {

    List<String> fileList;
    //folder
    String SOURCE_FOLDER = "";
    //get path of zip file corpus/resources/corpus.zip
    String OUTPUT_ZIP_FILE = "";
    String AUDIO = "";
    CorpusData comadata;

    public ZipCorpus(Properties properties) {
        super(properties);
        fileList = new ArrayList<String>();
    }

    /*
    public static void main(String[] args) {
        ZipCorpus appZip = new ZipCorpus();
        System.out.println(SOURCE_FOLDER);
        appZip.generateFileList(new File(SOURCE_FOLDER));
        appZip.zipIt(OUTPUT_ZIP_FILE, AUDIO);
    }
     */
    /**
     * Zip it
     *
     * @param zipFile output ZIP file location
     */
    public Report zipIt(CorpusData comadata, String zipFile, String AUDIO) {
        Report stats = new Report();
        //get name of folder
        if (zipFile.equals("")) {
            String SOURCE_FOLDER_NAME = comadata.getFilenameWithoutFileEnding();
            if (AUDIO.equals("all")) {
                zipFile = SOURCE_FOLDER + "resources" + File.separator + SOURCE_FOLDER_NAME + ".zip";
            } else if (AUDIO.equals("mp3")) {
                zipFile = SOURCE_FOLDER + "resources" + File.separator + SOURCE_FOLDER_NAME + "-mp3only.zip";
            } else {
                zipFile = SOURCE_FOLDER + "resources" + File.separator + SOURCE_FOLDER_NAME + "-noaudio.zip";
            }
        }
        byte[] buffer = new byte[1024];

        try {

            FileOutputStream fos = new FileOutputStream(zipFile);
            ZipOutputStream zos = new ZipOutputStream(fos);

            System.out.println("Output to Zip : " + zipFile);

            for (String file : this.fileList) {

                System.out.println("File Added : " + file);
                ZipEntry ze = new ZipEntry(file.substring(SOURCE_FOLDER.length() - 1, file.length()));
                zos.putNextEntry(ze);

                FileInputStream in
                        = new FileInputStream(file);

                int len;
                while ((len = in.read(buffer)) > 0) {
                    zos.write(buffer, 0, len);
                }

                in.close();
            }

            zos.closeEntry();
            //remember close it
            zos.close();
            System.out.println("Done");
            stats.addCorrect(function, comadata, "Successfully created zip file at " + zipFile);
        } catch (IOException ex) {
            stats.addException(function, ex, comadata, "Unknown IO exception");
        }
        return stats;
    }

    /**
     * Traverse a directory and get all files, and add the file into fileList
     *
     * @param node file or directory
     */
    public Report generateFileList(File node) {
        Report stats = new Report();
        //add file only
        if (node.isFile()) {
            if (AUDIO.equals("all")) {
                if (node.getName().endsWith(".exb") || node.getName().endsWith(".exs") || node.getName().endsWith(".coma") || node.getName().endsWith(".pdf") || node.getName().endsWith(".wav") || node.getName().endsWith("tei.xml") || node.getName().endsWith(".mp3")) {
                    System.out.println(node.getName());
                    fileList.add(generateZipEntry(node.getAbsoluteFile().toString()));
                    stats.addCorrect(function, comadata, node.getAbsoluteFile().toString() + " added to filelist");
                }
            } else if (AUDIO.equals("mp3")) {
                if (node.getName().endsWith(".exb") || node.getName().endsWith(".exs") || node.getName().endsWith(".coma") || node.getName().endsWith(".pdf") || node.getName().endsWith(".mp3") || node.getName().endsWith("tei.xml")) {
                    System.out.println(node.getName());
                    fileList.add(generateZipEntry(node.getAbsoluteFile().toString()));
                    stats.addCorrect(function, comadata, node.getAbsoluteFile().toString() + " added to filelist");
                }
            } else {
                if (node.getName().endsWith(".exb") || node.getName().endsWith(".exs") || node.getName().endsWith(".coma") || node.getName().endsWith(".pdf") || node.getName().endsWith("tei.xml")) {
                    System.out.println(node.getName());
                    fileList.add(generateZipEntry(node.getAbsoluteFile().toString()));
                    stats.addCorrect(function, comadata, node.getAbsoluteFile().toString() + " added to filelist");
               }
            }
        }
        if (node.isDirectory()) {
            String[] subNote = node.list();
            for (String filename : subNote) {
                System.out.println(node.getName());
                generateFileList(new File(node, filename));
            }
        }
        return stats;
    }

    /**
     * Format the file path for zip
     *
     * @param file file path
     * @return Formatted file path
     */
    private String generateZipEntry(String file) {
        //return file.substring(SOURCE_FOLDER.length() + 1, file.length());
        return file;
    }

    @Override
    public Report function(CorpusData cd) {
        Report stats = new Report();
        comadata = cd;
        if (SOURCE_FOLDER.equals("")){
            SOURCE_FOLDER = cd.getParentURL().getPath();
        }
        stats = generateFileList(new File(SOURCE_FOLDER));
        stats.merge(zipIt(cd, OUTPUT_ZIP_FILE, AUDIO));
        return stats;
    }

    @Override
    public Report function(Corpus c) {
        Report stats = new Report();
        comadata = c.getComaData();
        SOURCE_FOLDER = cd.getParentURL().getPath();
        stats = generateFileList(new File(SOURCE_FOLDER));
        stats.merge(zipIt(cd, OUTPUT_ZIP_FILE, AUDIO));
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

    public void setSourceFolder(String s) {
        SOURCE_FOLDER = s;
    }

    public void setOutputFile(String s) {
        OUTPUT_ZIP_FILE = s;
    }

    public void setWithAudio(String s) {
        report = new Report();
        if (s.equalsIgnoreCase("mp3")) {
            AUDIO = "mp3";
        } else if (s.equalsIgnoreCase("all")) {
            AUDIO = "all";
        } else {
            report.addCritical(function, cd, "Parameter audio not recognized: " + s);
        }
    }

    @Override
    public String getDescription() {
        String description = "This class takes a coma file and creates a zip file containing all important "
                + "corpus file in the resources folder. It only takes exb, exs, coma, pdf, ISO/TEI and optionally mp3 and/or wav, "
                + "and the folder structure. ";
        return description;
    }

}
