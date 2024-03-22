/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

 /*
 * ApplicationFrame.java
 *
 * Created on 21.09.2010, 10:53:59
 */
package de.uni_hamburg.corpora.swing;

import de.uni_hamburg.corpora.CorpusData;
import de.uni_hamburg.corpora.CorpusFunction;
import de.uni_hamburg.corpora.CorpusIO;
import de.uni_hamburg.corpora.CorpusMagician;
import static de.uni_hamburg.corpora.CorpusMagician.corpusFunctionStrings2Classes;
import de.uni_hamburg.corpora.Report;
import de.uni_hamburg.corpora.ReportItem;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Desktop;
import java.io.File;
import java.io.FileFilter;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.border.LineBorder;
import javax.swing.text.BadLocationException;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import org.exmaralda.folker.utilities.HTMLDisplayDialog;
import org.exmaralda.partitureditor.jexmaralda.JexmaraldaException;
//import org.exmaralda.common.jdomutilities.IOUtilities;
//import org.exmaralda.folker.utilities.HTMLDisplayDialog;
//import org.exmaralda.partitureditor.fsm.FSMException;
//import org.exmaralda.partitureditor.jexmaralda.BasicTranscription;
//import org.exmaralda.partitureditor.jexmaralda.convert.CHATConverter;
//import org.exmaralda.partitureditor.jexmaralda.convert.ELANConverter;
//import org.exmaralda.partitureditor.jexmaralda.convert.StylesheetFactory;
//import org.exmaralda.partitureditor.jexmaralda.convert.TEIConverter;
//import org.exmaralda.partitureditor.jexmaralda.convert.TranscriberConverter;
//import org.exmaralda.partitureditor.partiture.BrowserLauncher;
import org.jdom2.Document;
import org.jdom2.output.Format;
import org.jdom2.output.XMLOutputter;
import org.xml.sax.SAXException;

/**
 *
 * @author thomas
 */
public class ApplicationFrame extends javax.swing.JFrame {

    // TEST: EXMARaLDA_FRESH
    DropPanel dropPanel = new DropPanel();
    FileDrop fileDrop;
    DefaultListModel listModel = new DefaultListModel();
    int done = 0;
    int all = 0;
    LineBorder dragBorder = new LineBorder(Color.BLUE, 3, true);

    ImageIcon inactiveIcon;
    ImageIcon activeIcon;

    CorpusIO cio = new CorpusIO();
    CorpusMagician corpuma = new CorpusMagician();
    Collection<CorpusData> allFiles = new ArrayList();
    Report report = new Report();

    /**
     * Creates new form ApplicationFrame
     */
    public ApplicationFrame() {
        initComponents();
        inactiveIcon = new javax.swing.ImageIcon(getClass().getResource("/images/droptarget.png"));
        activeIcon = new javax.swing.ImageIcon(getClass().getResource("/images/droptarget_active.png"));
        mainPanel.add(dropPanel, java.awt.BorderLayout.NORTH);
        teiFilesList.setModel(listModel);
        pack();
        fileDrop = new FileDrop(dropPanel,
                dragBorder,
                new FileDrop.Listener() {
            @Override
            public void filesDropped(java.io.File[] files) {
                // handle file drop
                handleFileDrop(files);
            }   // end filesDropped
        }); // end FileDrop.Listener
    }

    void message(final String s) {
        int index = 0;
        while (index < s.length()) {
            String schnippel = s.substring(index, Math.min(index + 75, s.length()));
            messagesTextArea.append(schnippel + "\n");
            index += 75;
        }
        messagesTextArea.setCaretPosition(messagesTextArea.getText().length() - 1);
    }

    void updateProgress(String s) {
        done++;
        progressBar.setValue((int) Math.round(((double) done / all) * 100));
        progressBar.setString(s);
        if (done == all) {
            progressBar.setString("Done.");
            dropPanel.setIcon(inactiveIcon);
            message("***** DONE *****");
        }
        if ((teiFilesList.getModel().getSize() > 0) || (teiFilesList.isVisible())) {
            final int lastIndex = teiFilesList.getModel().getSize() - 1;
            try {
                SwingUtilities.invokeAndWait(new Runnable() {
                    public void run() {
                        teiFilesList.getSelectionModel().setSelectionInterval(lastIndex, lastIndex);
                    }
                });
                teiFilesList.scrollRectToVisible(teiFilesList.getCellBounds(lastIndex, lastIndex));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    void handleFileDrop(final File[] files) {
        for (File f : files) {
            try {
                URL url = f.toURI().toURL();
                if (f.isDirectory()) {
                    message("[Directory " + f.getName() + "]");
                    System.out.println("[Directory " + f.getName() + "]");
                    //need to use CorpusIO read(URL) method here
                    //that gives back a Colelction of CorpusData Objects
                    ArrayList<CorpusData> allcd = (ArrayList<CorpusData>) cio.read(url, report);
                    for (CorpusData cd: allcd){
                        message(cd.getFilename() + " added to list.");    
                    }                 
                } else {
                    CorpusData cd = cio.readFileURL(url);
                    if (cd != null) {
                        allFiles.add(cd);
                        message(cd.getFilename() + " added to list.");
                    } else {
                        message(f.getName() + " not added to list (data suffix not recognized).");
                    }
                }
            } catch (MalformedURLException ex) {
                message(f.getName() + " not added to list (file could not be read).");
            } catch (URISyntaxException ex) {
                message(f.getName() + " not added to list (file could not be read).");
            } catch (IOException ex) {
                Logger.getLogger(ApplicationFrame.class.getName()).log(Level.SEVERE, null, ex);
            } catch (SAXException ex) {
                Logger.getLogger(ApplicationFrame.class.getName()).log(Level.SEVERE, null, ex);
            } catch (JexmaraldaException ex) {
                Logger.getLogger(ApplicationFrame.class.getName()).log(Level.SEVERE, null, ex);
            } catch (ClassNotFoundException ex) {
                Logger.getLogger(ApplicationFrame.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        done = 0;
        all = allFiles.size();
        dropPanel.setIcon(activeIcon);

        for (final CorpusData cd : allFiles) {
            Thread t = new Thread() {

                @Override
                public void run() {
                    try {
                        //now we need to run the correct functions on the correct data
                        //where do we display which functions are possible on the data?
                        // Determine input type

//                            ex.printStackTrace();
//                            message("["+ f.getName() + "] " + ex.getLocalizedMessage());
//                            updateProgress(f.getName());
//                            return;

                        // Determine errorlist saveing location
                        URL reportlocation;
                        if (sameDirectory.isSelected()){
                            reportlocation = cd.getParentURL();
                        } else {
                            reportlocation = Paths.get(otherDirectoryTextField.getText()).toUri().toURL();
                        }
                        URI uri = reportlocation.toURI();
                        URI parentURI = uri.getPath().endsWith("/") ? uri.resolve("..") : uri.resolve(".");
                        String errorlistlocstring = Paths.get(parentURI).toString() + File.separator + "report_output.html";
                        URL errorlistlocation = Paths.get(errorlistlocstring).toUri().toURL();

// run corpusfunctions
//find out which function to run
                        List<String> s = Arrays.asList(String.valueOf(parseMethodComboBox.getSelectedItem()));
                        corpuma.setChosencorpusfunctions(s);
                        for (String str : s) {
                            message("Added function " + str + " to list");
                        }

                        Collection<CorpusFunction> cfs = corpusFunctionStrings2Classes(corpuma.getChosencorpusfunctions());
                        for (CorpusFunction cf : cfs) {
                            //make sure to run it only on the data the check is allowed for
                            report.merge(corpuma.runCorpusFunction(cd, cf));
                        }
//TO DO
                        URL basedirectory = cd.getParentURL();
                        String reportOutput = ReportItem.generateDataTableHTML(report.getRawStatistics(), report.getSummaryLines());
                        String absoluteReport = reportOutput.replaceAll(basedirectory.toString(), "");
                        cio.write(absoluteReport, errorlistlocation);
                        message("Wrote ErrorList at " + errorlistlocation);
                        listModel.addElement(new File(cd.getFilename()));
                        updateProgress(cd.getFilename());
                    } catch (MalformedURLException ex) {
                        message("Couldn't write error list - location is incorrect");
                    } catch (IOException ex) {
                        message("Couldn't write error list - location is incorrect");
                    } catch (URISyntaxException ex) {
                        message("Couldn't write error list - location is incorrect");
                    }
                }

            };
            t.start();
        }
    }

    void displayHelp() {
        try {
            //TO DO better website
            Desktop.getDesktop().browse(new URI("https://lab.multilingua.uni-hamburg.de/redmine/projects/redmine/wiki/How_to_use_the_Corpus_Validator"));
        } catch (IOException ex) {
            ex.printStackTrace();
        } catch (URISyntaxException ex) {
            Logger.getLogger(ApplicationFrame.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">                                                                          
    private void initComponents() {

        buttonGroup1 = new javax.swing.ButtonGroup();
        sidePanel = new javax.swing.JPanel();
        corpusServices = new javax.swing.JPanel();
        jLabel3 = new javax.swing.JLabel();
        settingsPanel = new javax.swing.JPanel();
        corpusFunction = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        parseMethodComboBox = new javax.swing.JComboBox();
        outputPanel = new javax.swing.JPanel();
        output = new javax.swing.JLabel();
        sameDirectory = new javax.swing.JRadioButton();
        otherDirectory = new javax.swing.JRadioButton();
        otherDirectoryTextField = new javax.swing.JTextField();
        browseButton = new javax.swing.JButton();
        mainPanel = new javax.swing.JPanel();
        jPanel1 = new javax.swing.JPanel();
        messageAndProgressPanel = new javax.swing.JPanel();
        messageScrollPane = new javax.swing.JScrollPane();
        messagesTextArea = new javax.swing.JTextArea();
        listScrollPane = new javax.swing.JScrollPane();
        teiFilesList = new javax.swing.JList();
        operationsPanel = new javax.swing.JPanel();
        showXMLButton = new javax.swing.JButton();
        showHTMLButton = new javax.swing.JButton();
        lowerPanel = new javax.swing.JPanel();
        jPanel7 = new javax.swing.JPanel();
        progressBar = new javax.swing.JProgressBar();
        dropletToggleButton = new javax.swing.JToggleButton();
        helpButton = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setTitle("Corpus Services");
        getContentPane().setBackground(new java.awt.Color(15, 155, 155));

        sidePanel.setBackground(new java.awt.Color(15, 155, 155));
        sidePanel.setBorder(null);
        sidePanel.setPreferredSize(new java.awt.Dimension(150, 70));

        corpusServices.setBackground(new java.awt.Color(15, 155, 155));

        jLabel3.setBackground(new java.awt.Color(15, 155, 155));
        jLabel3.setForeground(new java.awt.Color(255, 255, 255));
        jLabel3.setFont(new java.awt.Font("Segoe UI", 1, 18)); // NOI18N
        jLabel3.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/icons/chart-bars.png")));
        jLabel3.setText("corpus services");

        javax.swing.GroupLayout corpusServicesLayout = new javax.swing.GroupLayout(corpusServices);
        corpusServices.setLayout(corpusServicesLayout);
        corpusServicesLayout.setHorizontalGroup(
                corpusServicesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(corpusServicesLayout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(jLabel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addContainerGap())
        );
        corpusServicesLayout.setVerticalGroup(
                corpusServicesLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(corpusServicesLayout.createSequentialGroup()
                                .addGap(25, 25, 25)
                                .addComponent(jLabel3, javax.swing.GroupLayout.PREFERRED_SIZE, 46, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        settingsPanel.setBackground(new java.awt.Color(15, 155, 155));
        settingsPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(javax.swing.BorderFactory.createCompoundBorder(), "Settings", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Segoe UI", 1, 14), new java.awt.Color(255, 255, 255))); // NOI18N
        settingsPanel.setForeground(new java.awt.Color(255, 255, 255));
        settingsPanel.setToolTipText("");
        settingsPanel.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
        settingsPanel.setMaximumSize(new java.awt.Dimension(150, 300));
        settingsPanel.setMinimumSize(new java.awt.Dimension(120, 200));
        settingsPanel.setPreferredSize(new java.awt.Dimension(120, 200));

        corpusFunction.setBackground(new java.awt.Color(15, 155, 155));
        corpusFunction.setAlignmentX(0.0F);
        corpusFunction.setPreferredSize(new java.awt.Dimension(178, 20));

        jLabel1.setBackground(new java.awt.Color(15, 155, 155));
        jLabel1.setFont(new java.awt.Font("Segoe UI", 0, 12)); // NOI18N
        jLabel1.setForeground(new java.awt.Color(255, 255, 255));
        jLabel1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/icons/magic-wand.png")));
        jLabel1.setText("Corpus function(s)");
        jLabel1.setToolTipText("Choose the corpus function(s) you want to use.");
        jLabel1.setMaximumSize(new java.awt.Dimension(150, 20));
        jLabel1.setMinimumSize(new java.awt.Dimension(150, 20));
        jLabel1.setPreferredSize(new java.awt.Dimension(150, 20));

        parseMethodComboBox.setBackground(new java.awt.Color(15, 155, 155));
        parseMethodComboBox.setFont(new java.awt.Font("Segoe UI", 0, 10)); // NOI18N
        parseMethodComboBox.setForeground(new java.awt.Color(15, 155, 155));
        //To Do the available corpus functions
        ArrayList<String> functions = (ArrayList<String>) corpuma.getAllExistingCFs();
        parseMethodComboBox.setModel(new javax.swing.DefaultComboBoxModel(functions.toArray()));
        parseMethodComboBox.setMaximumSize(new java.awt.Dimension(150, 24));
        parseMethodComboBox.setMinimumSize(new java.awt.Dimension(150, 24));
        parseMethodComboBox.setOpaque(true);
        parseMethodComboBox.setPreferredSize(new java.awt.Dimension(150, 24));
        parseMethodComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                parseMethodComboBoxActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout corpusFunctionLayout = new javax.swing.GroupLayout(corpusFunction);
        corpusFunction.setLayout(corpusFunctionLayout);
        corpusFunctionLayout.setHorizontalGroup(
                corpusFunctionLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(corpusFunctionLayout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(corpusFunctionLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(parseMethodComboBox, 0, 200, Short.MAX_VALUE))
                                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        corpusFunctionLayout.setVerticalGroup(
                corpusFunctionLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(corpusFunctionLayout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(parseMethodComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addContainerGap(43, Short.MAX_VALUE))
        );

        outputPanel.setBackground(new java.awt.Color(15, 155, 155));

        output.setBackground(new java.awt.Color(15, 155, 155));
        output.setFont(new java.awt.Font("Segoe UI", 0, 12)); // NOI18N
        output.setForeground(new java.awt.Color(255, 255, 255));
        output.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/icons/enter.png")));
        output.setText("Write error report to");
        output.setMaximumSize(new java.awt.Dimension(150, 20));
        output.setMinimumSize(new java.awt.Dimension(150, 20));
        output.setPreferredSize(new java.awt.Dimension(150, 20));

        sameDirectory.setBackground(new java.awt.Color(15, 155, 155));
        buttonGroup1.add(sameDirectory);
        sameDirectory.setFont(new java.awt.Font("Segoe UI", 0, 12)); // NOI18N
        sameDirectory.setForeground(new java.awt.Color(255, 255, 255));
        sameDirectory.setSelected(true);
        sameDirectory.setText("the same directory");
        sameDirectory.setOpaque(true);
        sameDirectory.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                sameDirectoryActionPerformed(evt);
            }
        });

        otherDirectory.setBackground(new java.awt.Color(15, 155, 155));
        buttonGroup1.add(otherDirectory);
        otherDirectory.setFont(new java.awt.Font("Segoe UI", 0, 12)); // NOI18N
        otherDirectory.setForeground(new java.awt.Color(255, 255, 255));
        otherDirectory.setText("a separate directory: ");
        otherDirectory.setOpaque(true);
        otherDirectory.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                otherDirectoryActionPerformed(evt);
            }
        });

        otherDirectoryTextField.setBackground(new java.awt.Color(15, 155, 155));
        otherDirectoryTextField.setFont(new java.awt.Font("Segoe UI", 0, 12)); // NOI18N
        otherDirectoryTextField.setAlignmentX(1.0F);
        otherDirectoryTextField.setMaximumSize(new java.awt.Dimension(300, 20));
        otherDirectoryTextField.setMinimumSize(new java.awt.Dimension(200, 20));
        otherDirectoryTextField.setOpaque(true);
        otherDirectoryTextField.setPreferredSize(new java.awt.Dimension(250, 20));

        browseButton.setBackground(new java.awt.Color(15, 155, 155));
        browseButton.setFont(new java.awt.Font("Segoe UI", 0, 12)); // NOI18N
        browseButton.setForeground(new java.awt.Color(15, 155, 155));
        browseButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/icons/file-empty.png")));
        browseButton.setText("Browse...");
        browseButton.setAlignmentX(0.5F);
        browseButton.setOpaque(true);
        browseButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                browseButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout outputPanelLayout = new javax.swing.GroupLayout(outputPanel);
        outputPanel.setLayout(outputPanelLayout);
        outputPanelLayout.setHorizontalGroup(
                outputPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, outputPanelLayout.createSequentialGroup()
                                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(otherDirectoryTextField, javax.swing.GroupLayout.PREFERRED_SIZE, 153, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(68, 68, 68))
                        .addGroup(outputPanelLayout.createSequentialGroup()
                                .addContainerGap()
                                .addGroup(outputPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addGroup(outputPanelLayout.createSequentialGroup()
                                                .addComponent(output, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                .addGap(36, 36, 36))
                                        .addGroup(outputPanelLayout.createSequentialGroup()
                                                .addGroup(outputPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                        .addComponent(sameDirectory, javax.swing.GroupLayout.PREFERRED_SIZE, 128, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                        .addComponent(otherDirectory)
                                                        .addComponent(browseButton))
                                                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))))
        );
        outputPanelLayout.setVerticalGroup(
                outputPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(outputPanelLayout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(output, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(sameDirectory)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(otherDirectory)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(otherDirectoryTextField, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                                .addComponent(browseButton)
                                .addGap(25, 25, 25))
        );

        javax.swing.GroupLayout settingsPanelLayout = new javax.swing.GroupLayout(settingsPanel);
        settingsPanel.setLayout(settingsPanelLayout);
        settingsPanelLayout.setHorizontalGroup(
                settingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(settingsPanelLayout.createSequentialGroup()
                                .addGroup(settingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(corpusFunction, 0, 178, Short.MAX_VALUE)
                                        .addComponent(outputPanel, javax.swing.GroupLayout.PREFERRED_SIZE, 165, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addContainerGap(19, Short.MAX_VALUE))
        );
        settingsPanelLayout.setVerticalGroup(
                settingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(settingsPanelLayout.createSequentialGroup()
                                .addGap(14, 14, 14)
                                .addComponent(corpusFunction, javax.swing.GroupLayout.PREFERRED_SIZE, 99, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(outputPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(86, 86, 86))
        );

        javax.swing.GroupLayout sidePanelLayout = new javax.swing.GroupLayout(sidePanel);
        sidePanel.setLayout(sidePanelLayout);
        sidePanelLayout.setHorizontalGroup(
                sidePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(sidePanelLayout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(corpusServices, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addComponent(settingsPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 192, Short.MAX_VALUE)
        );
        sidePanelLayout.setVerticalGroup(
                sidePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, sidePanelLayout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(corpusServices, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(settingsPanel, javax.swing.GroupLayout.PREFERRED_SIZE, 295, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addContainerGap())
        );

        mainPanel.setBackground(new java.awt.Color(15, 155, 155));
        mainPanel.setLayout(new java.awt.BorderLayout());

        jPanel1.setBackground(new java.awt.Color(15, 155, 155));

        messageAndProgressPanel.setBackground(new java.awt.Color(15, 155, 155));
        messageAndProgressPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(javax.swing.BorderFactory.createCompoundBorder(), "   Messages", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Segoe UI", 1, 14), new java.awt.Color(255, 255, 255))); // NOI18N
        messageAndProgressPanel.setForeground(new java.awt.Color(255, 255, 255));
        messageAndProgressPanel.setToolTipText("");
        messageAndProgressPanel.setFont(new java.awt.Font("Arial", 0, 12)); // NOI18N

        messageScrollPane.setBackground(new java.awt.Color(15, 155, 155));
        messageScrollPane.setBorder(null);
        messageScrollPane.setViewportBorder(null);
        messageScrollPane.setPreferredSize(new java.awt.Dimension(400, 120));
        
        messagesTextArea.setBackground(new java.awt.Color(172, 221, 221));
        messagesTextArea.setColumns(20);
        messagesTextArea.setFont(new java.awt.Font("Monospaced", 0, 12)); // NOI18N
        messagesTextArea.setForeground(new java.awt.Color(255, 255, 255));
        messagesTextArea.setRows(5);
        messagesTextArea.setBorder(null);
        messagesTextArea.setPreferredSize(new java.awt.Dimension(140, 75));
        messagesTextArea.setRequestFocusEnabled(false);
        messagesTextArea.setLineWrap(true);
        messageScrollPane.setViewportView(messagesTextArea);
	messageAndProgressPanel.add(messageScrollPane, java.awt.BorderLayout.CENTER);
        javax.swing.GroupLayout messageAndProgressPanelLayout = new javax.swing.GroupLayout(messageAndProgressPanel);
        messageAndProgressPanel.setLayout(messageAndProgressPanelLayout);
        messageAndProgressPanelLayout.setHorizontalGroup(
                messageAndProgressPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(messageAndProgressPanelLayout.createSequentialGroup()
                                .addContainerGap()
                                .addComponent(messageScrollPane, javax.swing.GroupLayout.PREFERRED_SIZE, 299, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        messageAndProgressPanelLayout.setVerticalGroup(
                messageAndProgressPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                        .addComponent(messageScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        listScrollPane.setBackground(new java.awt.Color(15, 155, 155));
        listScrollPane.setBorder(javax.swing.BorderFactory.createTitledBorder(javax.swing.BorderFactory.createCompoundBorder(), "Processed files", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Segoe UI", 1, 14), new java.awt.Color(255, 255, 255))); // NOI18N
        listScrollPane.setForeground(new java.awt.Color(255, 255, 255));

        teiFilesList.setBackground(new java.awt.Color(172, 221, 221));
        teiFilesList.setForeground(new java.awt.Color(255, 255, 255));
        teiFilesList.addListSelectionListener(new javax.swing.event.ListSelectionListener() {
            public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
                teiFilesListValueChanged(evt);
            }
        });
        listScrollPane.setViewportView(teiFilesList);

        operationsPanel.setBackground(new java.awt.Color(15, 155, 155));
        operationsPanel.setBorder(javax.swing.BorderFactory.createTitledBorder(javax.swing.BorderFactory.createCompoundBorder(), "Operations", javax.swing.border.TitledBorder.DEFAULT_JUSTIFICATION, javax.swing.border.TitledBorder.DEFAULT_POSITION, new java.awt.Font("Segoe UI", 1, 14), new java.awt.Color(255, 255, 255))); // NOI18N
        operationsPanel.setForeground(new java.awt.Color(255, 255, 255));
        operationsPanel.setMinimumSize(new java.awt.Dimension(70, 96));
        operationsPanel.setPreferredSize(new java.awt.Dimension(70, 96));

        showXMLButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/exmaralda/tei/swing/xmldoc.gif"))); // NOI18N
        showXMLButton.setToolTipText("Show XML");
        showXMLButton.setEnabled(false);
        showXMLButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        showXMLButton.setMaximumSize(new java.awt.Dimension(60, 60));
        showXMLButton.setMinimumSize(new java.awt.Dimension(60, 60));
        showXMLButton.setPreferredSize(new java.awt.Dimension(60, 60));
        showXMLButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        showXMLButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                showXMLButtonActionPerformed(evt);
            }
        });
        operationsPanel.add(showXMLButton);

        showHTMLButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/org/exmaralda/folker/tangoicons/tango-icon-theme-0.8.1/32x32/mimetypes/text-html.png"))); // NOI18N
        showHTMLButton.setToolTipText("Show HTML in Browser");
        showHTMLButton.setEnabled(false);
        showHTMLButton.setHorizontalTextPosition(javax.swing.SwingConstants.CENTER);
        showHTMLButton.setMaximumSize(new java.awt.Dimension(60, 60));
        showHTMLButton.setMinimumSize(new java.awt.Dimension(60, 60));
        showHTMLButton.setPreferredSize(new java.awt.Dimension(60, 60));
        showHTMLButton.setVerticalTextPosition(javax.swing.SwingConstants.BOTTOM);
        showHTMLButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                showHTMLButtonActionPerformed(evt);
            }
        });
        operationsPanel.add(showHTMLButton);

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
                jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                                .addGap(5, 5, 5)
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                                                .addComponent(messageAndProgressPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(listScrollPane, javax.swing.GroupLayout.PREFERRED_SIZE, 316, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addGap(8, 8, 8))
                                        .addGroup(jPanel1Layout.createSequentialGroup()
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                .addComponent(operationsPanel, javax.swing.GroupLayout.PREFERRED_SIZE, 643, javax.swing.GroupLayout.PREFERRED_SIZE)))
                                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
                jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(jPanel1Layout.createSequentialGroup()
                                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                        .addComponent(messageAndProgressPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                        .addComponent(listScrollPane, javax.swing.GroupLayout.PREFERRED_SIZE, 232, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(operationsPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addGap(0, 55, Short.MAX_VALUE))
        );

        mainPanel.add(jPanel1, java.awt.BorderLayout.CENTER);

        lowerPanel.setBackground(new java.awt.Color(15, 155, 155));
        lowerPanel.setLayout(new java.awt.BorderLayout());

        jPanel7.setBackground(new java.awt.Color(15, 155, 155));
        jPanel7.setLayout(new java.awt.BorderLayout());

        progressBar.setBackground(new java.awt.Color(15, 155, 155));
        progressBar.setFont(new java.awt.Font("Segoe UI", 0, 12)); // NOI18N
        progressBar.setForeground(new java.awt.Color(255, 255, 255));
        progressBar.setMaximumSize(new java.awt.Dimension(300, 19));
        progressBar.setString("Waiting for input...");
        progressBar.setStringPainted(true);
        jPanel7.add(progressBar, java.awt.BorderLayout.CENTER);

        dropletToggleButton.setBackground(new java.awt.Color(15, 155, 155));
        dropletToggleButton.setFont(new java.awt.Font("Segoe UI", 0, 12)); // NOI18N
        dropletToggleButton.setForeground(new java.awt.Color(15, 155, 155));
        dropletToggleButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/icons/chevron-left-circle.png")));
        dropletToggleButton.setText("Reduce to droplet");
        dropletToggleButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                dropletToggleButtonActionPerformed(evt);
            }
        });
        jPanel7.add(dropletToggleButton, java.awt.BorderLayout.EAST);

        helpButton.setBackground(new java.awt.Color(15, 155, 155));
        helpButton.setFont(new java.awt.Font("Segoe UI", 0, 12)); // NOI18N
        helpButton.setForeground(new java.awt.Color(15, 155, 155));
        helpButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/images/icons/question-circle.png")));
        helpButton.setText("Help");
        helpButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                helpButtonActionPerformed(evt);
            }
        });
        jPanel7.add(helpButton, java.awt.BorderLayout.WEST);

        lowerPanel.add(jPanel7, java.awt.BorderLayout.SOUTH);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                        .addComponent(lowerPanel, javax.swing.GroupLayout.PREFERRED_SIZE, 851, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addGroup(layout.createSequentialGroup()
                                                .addComponent(sidePanel, javax.swing.GroupLayout.PREFERRED_SIZE, 192, javax.swing.GroupLayout.PREFERRED_SIZE)
                                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                                .addComponent(mainPanel, javax.swing.GroupLayout.PREFERRED_SIZE, 653, javax.swing.GroupLayout.PREFERRED_SIZE)))
                                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
                layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                        .addGroup(layout.createSequentialGroup()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                        .addComponent(mainPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                        .addComponent(sidePanel, javax.swing.GroupLayout.PREFERRED_SIZE, 390, javax.swing.GroupLayout.PREFERRED_SIZE))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                .addComponent(lowerPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        pack();
    }// </editor-fold>                                            

    private void dropletToggleButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_dropletToggleButtonActionPerformed
        boolean hide = !(dropletToggleButton.isSelected());
        if (!hide) {
            dropletToggleButton.setText("Expand");
        } else {
            dropletToggleButton.setText("Reduce to droplet");
        }
        messageAndProgressPanel.setVisible(hide);
        settingsPanel.setVisible(hide);
        operationsPanel.setVisible(hide);
        listScrollPane.setVisible(hide);
        pack();
    }//GEN-LAST:event_dropletToggleButtonActionPerformed

    private void helpButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_helpButtonActionPerformed
        this.displayHelp();
    }//GEN-LAST:event_helpButtonActionPerformed

    private void browseButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_browseButtonActionPerformed
        JFileChooser jfc = new JFileChooser();
        jfc.setDialogTitle("Choose directory");
        jfc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        int ret = jfc.showOpenDialog(this);
        if (ret == JFileChooser.APPROVE_OPTION) {
            otherDirectoryTextField.setText(jfc.getSelectedFile().getAbsolutePath());
            otherDirectory.setSelected(true);
        }
    }//GEN-LAST:event_browseButtonActionPerformed

    private void otherDirectoryActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_otherDirectoryActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_otherDirectoryActionPerformed

    private void sameDirectoryActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_sameDirectoryActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_sameDirectoryActionPerformed

    private void parseMethodComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_parseMethodComboBoxActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_parseMethodComboBoxActionPerformed

    private void showHTMLButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_showHTMLButtonActionPerformed
        try {
            File f = (File) (teiFilesList.getSelectedValue());
            File tempHTML = File.createTempFile("teidrophtml", ".html");
            tempHTML.deleteOnExit();
            //String htmlString = new StylesheetFactory(true).applyInternalStylesheetToExternalXMLFile("/org/exmaralda/tei/xml/tei2html.xsl", f.getAbsolutePath());
            FileOutputStream fos = new FileOutputStream(tempHTML);
            //fos.write(htmlString.getBytes("UTF-8"));
            fos.close();
            System.out.println("document written.");
            //BrowserLauncher.openURL(tempHTML.toURI().toURL().toString());
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(rootPane, ex.getLocalizedMessage());
        }
    }//GEN-LAST:event_showHTMLButtonActionPerformed

    private void showXMLButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_showXMLButtonActionPerformed
        File f = (File) (teiFilesList.getSelectedValue());
        Document teidoc;
        try {
            //teidoc = IOUtilities.readDocumentFromLocalFile(f.getAbsolutePath());
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(rootPane, ex.getLocalizedMessage());
            return;
        }
        Format format = Format.getPrettyFormat();
        XMLOutputter outputter = new XMLOutputter(format);
        // Get the text pane's document
        JTextPane textPane = new JTextPane();
        StyledDocument doc = (StyledDocument) textPane.getDocument();
        // Create a style object and then set the style attributes
        Style style = doc.addStyle("StyleName", null);
        // Font family
        StyleConstants.setFontFamily(style, "Courier");
        // Font size
        StyleConstants.setFontSize(style, 12);
        // Foreground color
        StyleConstants.setForeground(style, Color.black);
        /* try {
            doc.insertString(doc.getLength(), outputter.outputString(teidoc), style);
        } catch (BadLocationException ex) {
            ex.printStackTrace();
        } */
        JDialog dialog = new JDialog(this, true);
        dialog.setTitle(f.getName());
        dialog.getContentPane().setLayout(new BorderLayout());
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
        dialog.getContentPane().add(new JScrollPane(textPane), BorderLayout.CENTER);
        dialog.pack();
        dialog.setLocationRelativeTo(this);
        textPane.setCaretPosition(0);
        dialog.setVisible(true);
    }//GEN-LAST:event_showXMLButtonActionPerformed

    private void teiFilesListValueChanged(javax.swing.event.ListSelectionEvent evt) {//GEN-FIRST:event_teiFilesListValueChanged
        enableOperations(teiFilesList.getSelectedIndex() >= 0);
    }//GEN-LAST:event_teiFilesListValueChanged

    void enableOperations(boolean enable) {
        showXMLButton.setEnabled(enable);
        showHTMLButton.setEnabled(enable);
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        try {
            System.out.println("Setting system L&F : " + javax.swing.UIManager.getSystemLookAndFeelClassName());
            javax.swing.UIManager.setLookAndFeel(javax.swing.UIManager.getSystemLookAndFeelClassName());
        } catch (Exception e) {
            e.printStackTrace();
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                new ApplicationFrame().setVisible(true);
            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton browseButton;
    private javax.swing.ButtonGroup buttonGroup1;
    private javax.swing.JPanel corpusFunction;
    private javax.swing.JPanel corpusServices;
    private javax.swing.JToggleButton dropletToggleButton;
    private javax.swing.JButton helpButton;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel7;
    private javax.swing.JScrollPane listScrollPane;
    private javax.swing.JPanel lowerPanel;
    private javax.swing.JPanel mainPanel;
    private javax.swing.JPanel messageAndProgressPanel;
    private javax.swing.JScrollPane messageScrollPane;
    private javax.swing.JTextArea messagesTextArea;
    private javax.swing.JPanel operationsPanel;
    private javax.swing.JRadioButton otherDirectory;
    private javax.swing.JTextField otherDirectoryTextField;
    private javax.swing.JLabel output;
    private javax.swing.JPanel outputPanel;
    private javax.swing.JComboBox parseMethodComboBox;
    private javax.swing.JProgressBar progressBar;
    private javax.swing.JRadioButton sameDirectory;
    private javax.swing.JPanel settingsPanel;
    private javax.swing.JButton showHTMLButton;
    private javax.swing.JButton showXMLButton;
    private javax.swing.JPanel sidePanel;
    private javax.swing.JList teiFilesList;
    // End of variables declaration//GEN-END:variables

}
