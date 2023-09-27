package de.uni_hamburg.corpora;

import de.uni_hamburg.corpora.data.CorpusData;

/***
 * Class representing the result of a corpus function, i.e. the report and the resulting corpus data.
 * Can be used to build pipelines of corpus functions
 *
 *  @author Herbert Lange
 *  @version 20230105
 */
public class Result {
    Report report ;
    CorpusData corpusData;

    public Result(Report report, CorpusData cd) {
        this.report = report;
        this.corpusData = cd;
    }

    public Report getReport() {
        return report;
    }

    public CorpusData getCorpusData() {
        return corpusData;
    }

    public Result apply(CorpusFunction corpusFunction) {
        if (corpusFunction.getIsUsableFor().contains(this.corpusData.getClass())) {
            Result result = corpusFunction.execute(this.corpusData);
            this.report.merge(result.getReport());
            return new Result(this.report,result.getCorpusData());
        }
        else {
            return this;
        }
    }
}
