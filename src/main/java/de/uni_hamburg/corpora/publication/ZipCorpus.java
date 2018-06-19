/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package de.uni_hamburg.corpora.publication;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
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
public class ZipCorpus {

    List<String> fileList;
    //folder
    private static String SOURCE_FOLDER = "E:\\Anne\\NganasanCorpus";
    //get name of folder
    private static String SOURCE_FOLDER_NAME = SOURCE_FOLDER.substring(SOURCE_FOLDER.lastIndexOf(File.separator) + 1);
    //get path of zip file corpus/resources/corpus.zip
    private static String OUTPUT_ZIP_FILE;
    private static Boolean AUDIO = false;

    ZipCorpus() {
        fileList = new ArrayList<String>();
    }

    public static void main(String[] args) {
        ZipCorpus appZip = new ZipCorpus();
        System.out.println(SOURCE_FOLDER);
        appZip.generateFileList(new File(SOURCE_FOLDER));
        appZip.zipIt(OUTPUT_ZIP_FILE, AUDIO);
    }

    /**
     * Zip it
     *
     * @param zipFile output ZIP file location
     */
    public void zipIt(String zipFile, Boolean AUDIO) {
        if (AUDIO) {
            zipFile = SOURCE_FOLDER + File.separator + "resources" + File.separator + SOURCE_FOLDER_NAME + "WithAudio.zip";
        } else {
            zipFile = SOURCE_FOLDER + File.separator + "resources" + File.separator + SOURCE_FOLDER_NAME + "NoAudio.zip";
        }
        byte[] buffer = new byte[1024];

        try {

            FileOutputStream fos = new FileOutputStream(zipFile);
            ZipOutputStream zos = new ZipOutputStream(fos);

            System.out.println("Output to Zip : " + zipFile);

            for (String file : this.fileList) {

                System.out.println("File Added : " + file);
                ZipEntry ze = new ZipEntry(file);
                zos.putNextEntry(ze);

                FileInputStream in
                        = new FileInputStream(SOURCE_FOLDER + File.separator + file);

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
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    /**
     * Traverse a directory and get all files, and add the file into fileList
     *
     * @param node file or directory
     */
    public void generateFileList(File node) {

        //add file only
        if (node.isFile()) {
            if (AUDIO) {
                if (node.getName().endsWith(".exb") || node.getName().endsWith(".exs") || node.getName().endsWith(".coma") || node.getName().endsWith(".pdf") || node.getName().endsWith(".mp3")) {
                    fileList.add(generateZipEntry(node.getAbsoluteFile().toString()));
                }
            } else {
                if (node.getName().endsWith(".exb") || node.getName().endsWith(".exs") || node.getName().endsWith(".coma") || node.getName().endsWith(".pdf")) {
                    fileList.add(generateZipEntry(node.getAbsoluteFile().toString()));
                }
            }
        }
        if (node.isDirectory()) {
            String[] subNote = node.list();
            for (String filename : subNote) {
                generateFileList(new File(node, filename));
            }
        }

    }

    /**
     * Format the file path for zip
     *
     * @param file file path
     * @return Formatted file path
     */
    private String generateZipEntry(String file) {
        return file.substring(SOURCE_FOLDER.length() + 1, file.length());
    }
}
