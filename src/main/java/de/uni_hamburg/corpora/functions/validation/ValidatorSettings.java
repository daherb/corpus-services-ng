/**
 * @file CorpusFileCheckCommandLine.java
 *
 * A command-line interface for checking corpus files.
 *
 * @author Tommi A Pirinen <tommi.antero.pirinen@uni-hamburg.de>
 * @author HZSK
 */

package de.uni_hamburg.corpora.validation;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.ParseException;
import org.ini4j.Ini;


/**
 * A command-line interface for error-checking corpora files.
 */
public class ValidatorSettings {

    boolean verbose = false;
    boolean replace = false;
    File iniFile;
    Ini optionsIni;
    File baseDirectory;
    File dataDirectory;
    List<File> inputFiles;
    File outputFile;

    String name;
    String header;
    String footer;

    public ValidatorSettings() {
        inputFiles = new ArrayList<File>();
        this.name = "UnnamedValidator";
        this.header = "This validator is so unfinished it doesn't have a header!";
        this.footer = "This validator is so unfinished it doesn't have a footer!";
    }

    public ValidatorSettings(String name, String header, String footer) {
        inputFiles = new ArrayList<File>();
        this.name = name;
        this.header = header;
        this.footer = footer;
    }

    /* getters and setters */

    public boolean isVerbose() {
        return verbose;
    }

    public Collection<File> getInputFiles() {
        return inputFiles;
    }

    public File getOutputFile() {
        return outputFile;
    }

    public File getBaseDirectory() {
        return baseDirectory;
    }

    public File getDataDirectory() {
        return dataDirectory;
    }

    /** extend options from ini file. */
    public void amendOptions(Ini options) throws IOException {
        for (String sectionName : options.keySet()) {
            if (sectionName.equals("hzsk-validator")) {
                Ini.Section commons = options.get(sectionName);
                if (commons.containsKey("verbose")) {
                    verbose = true;
                }
                if (commons.containsKey("basedir")) {
                    baseDirectory = new File(commons.get("basedir"));
                }
                if (commons.containsKey("input")) {
                    inputFiles.add(new File(commons.get("input")));
                }
                if (commons.containsKey("output")) {
                    outputFile = new File(commons.get("output"));
                }
            } else {
                Ini.Section section = options.get(sectionName);
                Map<String, String> optionSet = new HashMap<String, String>();
                for (String keyName : section.keySet()) {
                    if (verbose) {
                        System.out.print(keyName + " = " +
                                section.get(keyName));
                    }
                }
            }
        }
    }


    /**
     * Command-line interface handling. Uses apache commons cli to parse
     * arguments.
     *
     * @param  args the command-line parameters
     */
    public CommandLine handleCommandLine(String[] args, List<Option> extraOptions) {
        CommandLine cmd;
        Options parameters = new Options();
        HelpFormatter formatter = new HelpFormatter();
        // parse
        try {
            parameters.addOption("h", "help", false,
                "print this help screen");
            parameters.addOption("v", "verbose", false,
                "print verbosely while processing");
            parameters.addOption("R", "recursive", false,
                    "check recursively for references found in a file");
            parameters.addOption("o", "output", true,
                    "fix problems where possible, write output to given file");
            parameters.addOption("c", "configuration", true,
                    "read configuration from ini file");
            parameters.addOption("i", "input", true,
                    "input file to validate");
            parameters.addOption("b", "base-dir", true,
                    "base directory for solving relative file references etc.");
            parameters.addOption("d", "data-dir", true,
                    "data directory for solving relative file references etc.");
            parameters.addOption("X", "in-place", false,
                    "fix problems in place, replacing original file");
            for (Option option : extraOptions) {
                parameters.addOption(option);
            }
            CommandLineParser parser = new DefaultParser();
            cmd = parser.parse(parameters, args);
        } catch (ParseException pe) {
            formatter.printHelp(name, header, parameters, footer, true);
            return null;
        }
        // check obligatories and conflicts
        if (!cmd.hasOption("input") && !cmd.hasOption("configuration") &&
                (cmd.getArgs().length < 1)) {
            System.err.println("No inputs given and no configuration found");
            formatter.printHelp(name, header, parameters, footer, true);
            return null;
        } else if (cmd.hasOption("input") && cmd.hasOption("configuration")) {
            System.err.println("Input and configuration are mutually " +
                    "exclusive");
            formatter.printHelp(name, header, parameters, footer, true);
            return null;
        } else if (cmd.hasOption("input") && (cmd.getArgs().length > 0)) {
            System.err.println("Unrecognised parameters with --input: " +
                cmd.getArgs());
            formatter.printHelp(name, header, parameters, footer, true);
            return null;
        } else if (cmd.hasOption("configuration") &&
                (cmd.getArgs().length > 0)) {
            System.err.println("Unrecognised parameters with --configuration: " +
                cmd.getArgs());
            formatter.printHelp(name, header, parameters, footer, true);
            return null;
        }
        // apply important / global parameters first
        if (cmd.hasOption("help")) {
            formatter.printHelp(name, header, parameters, footer, true);
            return null;
        }
        // create options stuff based on input stuff
        verbose = false;
        if (cmd.hasOption("verbose")) {
            System.out.println("Printing long report");
            verbose = true;
        }
        // set base dir
        if (cmd.hasOption("base-dir")) {
            baseDirectory = new File(cmd.getOptionValue("base-dir"));
        } else if (cmd.hasOption("configuration")) {
            iniFile = new File(cmd.getOptionValue("configuration"));
            if (iniFile.isFile()) {
                baseDirectory = iniFile.getParentFile();
            }
        }
        if (cmd.hasOption("data-dir")) {
            dataDirectory = new File(cmd.getOptionValue("data-dir"));
        }
        if (cmd.hasOption("configuration")) {
            if (verbose) {
                System.out.println("Reading configuration from " +
                        cmd.getOptionValue("configuration"));
            }
            try {
                optionsIni =
                    new Ini(new File(cmd.getOptionValue("configuration")));
                amendOptions(optionsIni);
            } catch (IOException ioe) {
                System.err.println("Configuration file not found: " +
                        cmd.getOptionValue("configuration") + " (or one of " +
                        "the referred files...)\n" +
                        ioe);
            }
        }
        // parse rest command lines any ways
        if (replace) {
            if (verbose) {
                System.out.println("Making corrections in place");
            }
        } else if (cmd.hasOption("output")) {
            if (verbose) {
                System.out.println("Making corrections to " +
                        cmd.getOptionValue("output"));
            }
            outputFile = new File(cmd.getOptionValue("output"));
        }
        if (cmd.hasOption("input")) {
            if (verbose) {
                System.out.println("Adding input: " +
                        cmd.getOptionValue("input"));
            }
            inputFiles.add(new File(cmd.getOptionValue("input")));
        }
        for (String s : cmd.getArgs()) {
            if (verbose) {
                System.out.println("Adding input: " + s);
            }
            inputFiles.add(new File(s));
        }
        return cmd;
    }


}
