package de.uni_hamburg.corpora.utilities.quest;

import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDMetadata;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.graphics.color.PDOutputIntent;
import org.apache.pdfbox.preflight.Format;
import org.apache.pdfbox.preflight.PreflightDocument;
import org.apache.pdfbox.preflight.parser.*;
import org.apache.xmpbox.XMPMetadata;
import org.apache.xmpbox.schema.DublinCoreSchema;
import org.apache.xmpbox.schema.PDFAIdentificationSchema;
import org.apache.xmpbox.type.BadFieldValueException;
import org.apache.xmpbox.xml.XmpSerializer;

import javax.xml.transform.TransformerException;

/**
 *
 * Some basic PDF handling functions
 *
 * References
 * @url https://pdfbox.apache.org/1.8/cookbook/pdfavalidation.html
 * @url https://pdfbox.apache.org/1.8/cookbook/pdfacreation.html
 *
 * Last updated
 * @author Herbert Lange
 * @version 20240517
 */
public class PdfTool {

    public static boolean isPDFA(URL file) {
        try (PDDocument document = new PreflightParser(new File(file.toURI())).parse()) {
            PreflightDocument pd = new PreflightDocument(document.getDocument(), Format.PDF_A1A);
            return pd.validate().isValid();
        } catch (IOException | URISyntaxException e) {
            return false;
        }
    }

    public static PDDocument toPDFA(PDDocument src, File destFile) throws IOException, URISyntaxException {
        try (PDDocument doc = new PDDocument(src.getDocument())) {
            PDPage page = new PDPage();
            doc.addPage(page);

            // Load font to be embedded
            InputStream fontStream = PdfTool.class
                    .getResourceAsStream("/org/apache/pdfbox/resources/ttf/LiberationSans-Regular.ttf");

            PDFont font = PDType0Font.load(doc, fontStream);

            // A PDF/A file needs to have the font embedded if the font is used for text rendering
            // in rendering modes other than text rendering mode 3.
            //
            // This requirement includes the PDF standard fonts, so don't use their static PDFType1Font classes such as
            // PDFType1Font.HELVETICA.
            //
            // As there are many different font licenses it is up to the developer to check if the license terms for the
            // font loaded allows embedding in the PDF.
            //
            if (!font.isEmbedded()) {
                throw new IllegalStateException("PDF/A compliance requires that all fonts used for"
                        + " text rendering in rendering modes other than rendering mode 3 are embedded.");
            }

            // add XMP metadata
            XMPMetadata xmp = XMPMetadata.createXMPMetadata();

            try {
                DublinCoreSchema dc = xmp.createAndAddDublinCoreSchema();
                dc.setTitle(src.getDocumentInformation().getTitle());
                PDFAIdentificationSchema id = xmp.createAndAddPDFAIdentificationSchema();
                id.setPart(1);
                id.setConformance("B");

                XmpSerializer serializer = new XmpSerializer();
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                serializer.serialize(xmp, baos, true);

                PDMetadata metadata = new PDMetadata(doc);
                metadata.importXMPMetadata(baos.toByteArray());
                doc.getDocumentCatalog().setMetadata(metadata);
            } catch (BadFieldValueException e) {
                // won't happen here, as the provided value is valid
                throw new IllegalArgumentException(e);
            } catch (TransformerException e) {
                e.printStackTrace();
            }

            // sRGB output intent
            InputStream colorProfile = PdfTool.class.getResourceAsStream(
//                    "/org/apache/pdfbox/resources/pdfa/sRGB.icc");
//                    "/org/apache/pdfbox/resources/icc/ISOcoated_v2_300_bas.icc");
                    "/sRGB2014.icc");
            PDOutputIntent intent = new PDOutputIntent(doc, colorProfile);
            intent.setInfo("sRGB IEC61966-2.1");
            intent.setOutputCondition("sRGB IEC61966-2.1");
            intent.setOutputConditionIdentifier("sRGB IEC61966-2.1");
            intent.setRegistryName("http://www.color.org");
            doc.getDocumentCatalog().addOutputIntent(intent);
            doc.save(destFile);
            return doc;
        }
    }

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Infile and outfile required");
        }
        else {
            try {
                URL inFile = new File(args[0]).toURI().toURL();
                System.out.println("URL: " + inFile);
                if (PdfTool.isPDFA(inFile)) {
                    System.out.println("Already a PDF/A");
                } else {
                    System.out.println("Converting to PDF/A");
                    PDDocument dest = PdfTool.toPDFA(Loader.loadPDF(new File(args[0])),new File(args[1]));
                }
            } catch (IOException | URISyntaxException e) {
                e.printStackTrace();
            }
        }
    }
}
