package de.uni_hamburg.corpora.utilities.quest;

import net.sf.jmimemagic.*;

import java.io.File;

/**
 * @author bba1792 Dr. Herbert Lange
 * @version 20211020
 *
 * Checks media files for compatibility with well-known archives
 */
public class MediaFileChecker {

    public static class MediaInformation {
        public MediaInformation(String mime, int sampleRate, int bitRate) {
            this.mime = mime;
            this.sampleRate = sampleRate;
            this.bitRate = bitRate;
        }

        String mime ;
        int sampleRate ;
        int bitRate;
    }

    static MediaInformation checkFile(String fileName) {
        try {
            Magic.initialize();
            MagicMatch match = Magic.getMagicMatch(new File(fileName),false);
            String mime = match.getMimeType();
            System.out.println(mime);
            return new MediaInformation(mime,0,0);
        } catch (MagicParseException | MagicMatchNotFoundException | MagicException e) {
            e.printStackTrace();
        }
        return null;
//        Tika tk = new Tika();
//        String mime = tk.detect(fileName);
//        System.out.println(mime);

    }

    public static void main(String[] args) {
        MediaFileChecker.checkFile(args[0]);
    }
}

