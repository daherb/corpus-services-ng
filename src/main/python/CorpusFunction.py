import json


def usage(cmd):
    print("%s corpus-data input-file report-file:" % cmd)
    print("\tcorpus-data: Format of the input-file")
    print("\tinput-file: Input file")
    print("\toutput-file: Output file")
    print("\treport-file: Report file")


class CorpusFunction(object):
    """Simple class for a python corpus function"""

    def __init__(self, args):
        if len(args) != 5 or "help" in args or "--help" in args:
            usage(args[0])
            exit(-1)
        else:
            self.command = args[0]
            self.corpusdata = args[1]
            self.inputfile = args[2]
            self.outputfile = args[3]
            self.reportfile = args[4]
            self.report = []

    def run(self):
        """The method to run the corpus function"""
        print("Run")
        self.function()
        # Close the report
        self.write_report()

    def write_report(self):
        """Method to write the final report"""
        print("writeReport " + self.reportfile)
        with open(self.reportfile, 'w') as f:
            json.dump(self.report, f)
