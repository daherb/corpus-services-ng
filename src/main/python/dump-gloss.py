import os.path
import sys
# import xml.etree.ElementTree as ET
import libxml2

import Report
from CorpusFunction import CorpusFunction


class DumpGloss(CorpusFunction):
    tmpDir = "/tmp/gloss"
    # Data for INEL
    # tierId = "gr" #  "ge"
    # xpath = "//tier[@id=\"%s\"]/event"
    # expectedData = "EXMARaLDATranscriptionData"
    # Data for DoReCo
    tierId = "gl"
    xpath = "//TIER[contains(@TIER_ID,\"%s\")]//ANNOTATION_VALUE"
    expectedData = "ELANData"

    def function(self):
        if self.corpusdata == self.expectedData:
            gloss_text = []
            self.report.append(Report.ReportItem(severity=Report.Severity.NOTE,
                                                 what="Python reading " + self.inputfile))
            # root = ET.parse(self.inputfile)
            doc = libxml2.parseFile(self.inputfile)
            ctx = doc.xpathNewContext();
            self.report.append(Report.ReportItem(severity=Report.Severity.NOTE,
                                                 what="Python running xpath " + (self.xpath % self.tierId)))
            # res = root.findall(self.xpath % self.tierId)
            res = ctx.xpathEval(self.xpath % self.tierId)
            for elem in res:
                # gloss_text.append(elem.text)
                gloss_text.append(elem.getContent())
            fpath, fname = os.path.split(self.inputfile)
            fname, fext = os.path.splitext(fname)
            self.report.append(Report.ReportItem(severity=Report.Severity.NOTE,
                                                 what="Python writing result " + str(gloss_text)))
            with open(os.path.join(self.tmpDir, fname + "_" + self.tierId + ".txt"), "w") as gf:
                gf.write(" ".join(gloss_text))
        else:
            self.report.append(Report.ReportItem(what="Unsupported data type " + self.corpusdata))
        self.write_report()


def main():
    cf = DumpGloss(sys.argv)
    cf.run()


if __name__ == '__main__':
    main()
