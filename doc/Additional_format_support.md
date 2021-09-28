# List of formats currently supported in Corpus Services (class - .extension)

- ComaData - .coma
- EXMARaLDAData - .exb
- SegmentedEXMARaLDATranscription - .exs
- AnnotationSpecification - .xml (the filename should contain "Annotation")
- CmdiData - .xml (the filename should contain "cmdi")
- UnspecifiedXmlData - .xml
- ELANData - .eaf
- FlextextData - .flextext

# How to add a new (XML-based) format to corpus services

Note: At the moment there is no support for binary files such as audio- or video recordings. The following guide primarily focuses on XML-based formats, so bear in mind that the steps which need to be taken to handle a binary should be somewhat different.

## 1. Create a new class with a name matching to the format (e.g. `NewFormatData` in the main package)

- Implement relevant interfaces (so far we have CorpusData for all corpus files, Metadata for coma files, XMLData for XML-based formats and CorpusData for non-coma XML- based formats which does nothing at the moment but gets implemented anyway)

- Override abstract methods inherited from the interfaces implemented in the previous step

- Create a constructor allowing the class to read and parse files. Here's an example of such a constructor for the BasicTranscription class:

<pre><code class="java">
public BasicTranscriptionData(URL url) {
    try {
        this.url = url;
        SAXBuilder builder = new SAXBuilder();
        jdom = builder.build(url);
        File f = new File(url.toURI());
        loadFile(f);
        originalstring = new String(Files.readAllBytes(Paths.get(url.toURI())), "UTF-8");
        URI uri = url.toURI();
        URI parentURI = uri.getPath().endsWith("/") ? uri.resolve("..") : uri.resolve(".");
        parenturl = parentURI.toURL();
        filename = FilenameUtils.getName(url.getPath());
        filenamewithoutending = FilenameUtils.getBaseName(url.getPath());
    } catch (JDOMException ex) {
        Logger.getLogger(UnspecifiedXMLData.class.getName()).log(Level.SEVERE, null, ex);
    } catch (IOException ex) {
        Logger.getLogger(UnspecifiedXMLData.class.getName()).log(Level.SEVERE, null, ex);
    } catch (URISyntaxException ex) {
        Logger.getLogger(BasicTranscriptionData.class.getName()).log(Level.SEVERE, null, ex);
    } catch (SAXException ex) {
        Logger.getLogger(BasicTranscriptionData.class.getName()).log(Level.SEVERE, null, ex);
    } catch (JexmaraldaException ex) {
        Logger.getLogger(BasicTranscriptionData.class.getName()).log(Level.SEVERE, null, ex);
    }
}
</code></pre> 

- The class also needs to implement the `Collection<String> getFileExtensions();` method that returns a list of
  valid file extensions. This function is used both in `readFileURL` and `listFiles` in `CorpusIO`.
  
## 2. Optional: Make changes to CorpusIO.java

- Add your class in the method `public CorpusData readFileURL(URL url, Collection<Class<? extends CorpusData>> clcds)`.
  This is only necessary if your file suffix is not unique and the file type also depends e.g. on the file path

## 3. Make changes to Corpus.java

- Make a collection for your class

<pre><code class="java">
Collection<NewFormatData> newformatdata = new ArrayList();
</code></pre>

- Add files matching your class to the collection in a loop under `public Corpus(ComaData coma, Collection<Class<? extends CorpusData>> clcds)`

<pre><code class="java">
newformatdata.add((NewFormatData) cd);
</code></pre>

- Add get() and set() methods

<pre><code class="java">
public Collection<NewFormatData> getNewFormatData() {
    return newformatdata;
}
</code></pre>
<pre><code class="java">
public void setNewForamtData(Collection<NewFormatData> newformatdata) {
    this.newformatdata = mewformatdata;
}
</code></pre>