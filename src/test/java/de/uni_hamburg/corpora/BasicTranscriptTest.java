package de.uni_hamburg.corpora;

/**
 * @file BasicTranscriptionData.java
 *
 * Connects BasicTranscription from Exmaralda to HZSK corpus services.
 *
 * @author Tommi A Pirinen <tommi.antero.pirinen@uni-hamburg.de>
 * @author HZSK
 */



import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;

import java.io.UnsupportedEncodingException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.File;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.net.URL;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public class BasicTranscriptTest {


    @Test
    public void readWriteBT() {
        try {
            String exbFilename = "src/test/java/de/uni_hamburg/corpora/resources/example/HardTalk.exb";
            String newExbFilename = "src/test/java/de/uni_hamburg/corpora/resources/example/outxample.exb";
            String exbString = new
                String(Files.readAllBytes(Paths.get(exbFilename)), "UTF-8");
            File exbFile = new File(exbFilename);
            URL url = exbFile.toURI().toURL();
            EXMARaLDACorpusData btd = new EXMARaLDACorpusData(url);
            //btd.loadFile(exbFile);
            String unprettyXML = btd.toUnformattedString();
            assertNotNull(unprettyXML);
            // could be assertThat()
            assertTrue(unprettyXML.equals(exbString));
            PrintWriter exbOut = new PrintWriter("src/test/java/de/uni_hamburg/corpora/resources/example/outxample.exb");
            exbOut.print(unprettyXML);
            exbOut.close();
            File newExbFile = new File(newExbFilename);
            //remove the created file after the tests
            newExbFile.delete();
        } catch (UnsupportedEncodingException uee) {
            uee.printStackTrace();
            fail("Unexpected exception " + uee);
        } catch (FileNotFoundException fnfe) {
            fnfe.printStackTrace();
            fail("Unexpected exception " + fnfe);
        } catch (IOException ioe) {
            ioe.printStackTrace();
            fail("Unexpected exception " + ioe);
        }
    }
}
