/**
 * @file StatisticsReport.java
 *
 * Auxiliary data structure for user friendly validation reports. Bit like a
 * logger.
 *
 * @author Tommi A Pirinen <tommi.antero.pirinen@uni-hamburg.de>
 * @author HZSK
 */
package de.uni_hamburg.corpora;

import de.uni_hamburg.corpora.ReportItem.Severity;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Date;
import org.jdom.JDOMException;

/**
 * Statistics report is a container class to facilitate building reports for
 * different validators and other file processors. The statistics consist of
 * "messages" that are singular events of success, failure or other notes,
 * categorised in named buckets. It's quite generic, the main point is to create
 * reports like:
 *
 * <pre>
 *   File "xyz.xml" has:
 *      1567 of things and stuff: 95 % done correctly,
 *      1 % missing, and 4 % with errors (see details here: ___)
 *      12400 of annotations: 100 % done correctly, 7 % with warnings.
 * </pre>
 */
public class Report {

    //Anne: Is this the ErrorList in the UML? If so, should we rename here or use StatisticsReport in UML? Or maybe best: ErrorReport?
    //But what would be the Items in here? ReportItems? Errors? StatisticsStuff?
    /**
     * Special statistics counter for higher level exceptions. I use this to
     * produce an error count with no possible successes, a bit like root logger
     * in logging
     */
    public final String ROOT_BUCKET = "root";

    /**
     * the data structure holding all statistics.
     */
    private Map<String, Collection<ReportItem>> statistics;

    /**
     * convenience function to create new statistic set if missing or get old.
     */
    private Collection<ReportItem> getOrCreateStatistic(String statId) {
        if (!statistics.containsKey(statId)) {
            statistics.put(statId, new ArrayList<ReportItem>());
        }
        return statistics.get(statId);
    }

    /**
     * Create empty report.
     */
    public Report() {
        statistics = new HashMap<String, Collection<ReportItem>>();
    }

    /**
     * Merge two error reports. Efficiently adds statistics from other report to
     * this one.
     */
    public void merge(Report sr) {
        for (Map.Entry<String, Collection<ReportItem>> kv
                : sr.statistics.entrySet()) {
            if (statistics.containsKey(kv.getKey())) {
                Collection<ReportItem> c
                        = statistics.get(kv.getKey());
                c.addAll(kv.getValue());
                statistics.put(kv.getKey(), c);
            } else {
                statistics.put(kv.getKey(), kv.getValue());
            }
        }
    }

    /**
     * Add a complete ReportItem in the root log. Not sure if this was meant to
     * work like this, I thought it may be needed to add ReportItems generated
     * by corpusFunctions?
     *
     * @sa addCritical(String, String)
     */
    public void addReportItem(String statId, ReportItem reportItem) {
        Collection<ReportItem> stat = getOrCreateStatistic(statId);
        stat.add(reportItem);
    }

    /**
     * Add a critical error in the root log.
     *
     * @sa addCritical(String, String)
     */
    public void addCritical(String description) {
        addCritical(ROOT_BUCKET, description);
    }

    /**
     * Add a critical error in named statistics bucket.
     */
    public void addCritical(String statId, String description) {
        Collection<ReportItem> stat = getOrCreateStatistic(statId);
        stat.add(new ReportItem(ReportItem.Severity.CRITICAL,
                description));
    }

    /**
     * Add a critical error in named statistics bucket.
     *
     * @todo extrablah
     */
    public void addCritical(String statId, String description, String extraBlah) {
        addCritical(statId, description + extraBlah);
    }

    /**
     * Add note for correctly formatted data in named statistics bucket.
     */
    public void addCritical(String statId, Throwable e, String description, String extrablah) {
        addCritical(statId, description + "::" + extrablah
                + "..." + e.getStackTrace()[0]);
    }

    /**
     * Add a critical error in named statistics bucket.
     *
     * @todo extrablah
     */
    public void addCritical(String statId, Throwable e, String description) {
        addCritical(statId, description + e.getStackTrace()[0]);
    }

    /**
     * Add a critical error in named statistics bucket. with CorpusData object
     */
    public void addCritical(String statId, CorpusData cd, String description) {
        Collection<ReportItem> stat = getOrCreateStatistic(statId);
        stat.add(new ReportItem(ReportItem.Severity.CRITICAL,
                cd.getURL().toString(), description, statId));
    }

    /**
     * Add a critical error in named statistics bucket. with CorpusData object
     */
    public void addFix(String statId, CorpusData cd, String description) {
        Collection<ReportItem> stat = getOrCreateStatistic(statId);
        stat.add(new ReportItem(ReportItem.Severity.IFIXEDITFORYOU,
                cd.getURL().toString(), description, statId));
    }

    /**
     * Add a non-critical error in named statistics bucket.
     */
    public void addWarning(String statId, String description) {
        Collection<ReportItem> stat = getOrCreateStatistic(statId);
        stat.add(new ReportItem(ReportItem.Severity.WARNING,
                description));
    }

    /**
     * Add a non-critical error in named statistics bucket.
     *
     * @todo extrablah
     */
    public void addWarning(String statId, String description, String extraBlah) {
        addWarning(statId, description + extraBlah);
    }

    /**
     * Add note for correctly formatted data in named statistics bucket.
     */
    public void addWarning(String statId, Throwable e, String description, String extrablah) {
        addWarning(statId, description + "::" + extrablah
                + "..." + e.getStackTrace()[0]);
    }

    /**
     * Add a critical error in named statistics bucket. with CorpusData object
     */
    public void addWarning(String statId, CorpusData cd, String description) {
        Collection<ReportItem> stat = getOrCreateStatistic(statId);
        stat.add(new ReportItem(ReportItem.Severity.WARNING,
                cd.getURL().toString(), description, statId));
    }

    /**
     * Add error about missing data in named statistics bucket.
     */
    public void addMissing(String statId, String description) {
        Collection<ReportItem> stat = getOrCreateStatistic(statId);
        stat.add(new ReportItem(ReportItem.Severity.MISSING,
                description));
    }

    /**
     * Add a critical error in named statistics bucket. with CorpusData object
     */
    public void addMissing(String statId, CorpusData cd, String description) {
        Collection<ReportItem> stat = getOrCreateStatistic(statId);
        stat.add(new ReportItem(ReportItem.Severity.MISSING,
                cd.getURL().toString(), description, statId));
    }

    /**
     * Add note for correctly formatted data in named statistics bucket.
     */
    public void addCorrect(String statId, String description) {
        Collection<ReportItem> stat = getOrCreateStatistic(statId);
        stat.add(new ReportItem(ReportItem.Severity.CORRECT,
                description));
    }

    /**
     * Add note for correctly formatted data in named statistics bucket with
     * filenamre
     */
    public void addCorrect(String statId, String filename, String description) {
        Collection<ReportItem> stat = getOrCreateStatistic(statId);
        stat.add(new ReportItem(ReportItem.Severity.CORRECT, filename,
                description, statId));
    }

    /**
     * Add a correct note in named statistics bucket. with CorpusData object
     */
    public void addCorrect(String statId, CorpusData cd, String description) {
        Collection<ReportItem> stat = getOrCreateStatistic(statId);
        stat.add(new ReportItem(ReportItem.Severity.CORRECT,
                cd.getURL().toString(), description, statId));
    }

    /**
     * Add note for correctly formatted data in named statistics bucket.
     */
    public void addNote(String statId, String description) {
        Collection<ReportItem> stat = getOrCreateStatistic(statId);
        stat.add(new ReportItem(ReportItem.Severity.NOTE,
                description));
    }

    /**
     * Add note for correctly formatted data in named statistics bucket.
     */
    public void addNote(String statId, Throwable e, String description) {
        addNote(statId, description + "..." + e.getStackTrace()[0]);
    }

    /**
     * Add note for correctly formatted data in named statistics bucket.
     */
    public void addNote(String statId, Throwable e, String description, String extrablah) {
        addNote(statId, description + "::" + extrablah
                + "..." + e.getStackTrace()[0]);
    }

    /**
     * Add note for correctly formatted data in named statistics bucket.
     */
    public void addNote(String statId, String description, String extraBlah) {
        addNote(statId, description + "::" + extraBlah);
    }

    /**
     * Add a correct note in named statistics bucket. with CorpusData object
     */
    public void addNote(String statId, CorpusData cd, String description) {
        Collection<ReportItem> stat = getOrCreateStatistic(statId);
        stat.add(new ReportItem(ReportItem.Severity.NOTE,
                cd.getURL().toString(), description, statId));
    }

    /**
     * Add error with throwable to root log.
     *
     * @sa addException(String, Throwable, String)
     */
    public void addException(Throwable e, String description) {
        addException(ROOT_BUCKET, e, description);
    }

    /**
     * Add error with throwable in statistics bucket. The exception provides
     * extra information about the error, ideally e.g. when parsing a file if
     * error comes in form of exception is a good idea to re-use the throwable
     * in statistics.
     */
    public void addException(String statId, Throwable e, String description) {
        Collection<ReportItem> stat = getOrCreateStatistic(statId);
        stat.add(new ReportItem(ReportItem.Severity.CRITICAL,
                e, description));
    }

    /**
     * Add error with throwable in statistics bucket. The exception provides
     */
    public void addException(String statId, Throwable e, String description,
            String extrablah) {
        addException(statId, e, description + "\n\t" + extrablah);
    }

    /**
     * Add a exception in named statistics bucket. with CorpusData object
     */
    public void addException(String statId, Throwable e, CorpusData cd, String description) {
        Collection<ReportItem> stat = getOrCreateStatistic(statId);
        stat.add(new ReportItem(ReportItem.Severity.CRITICAL,
                e, cd.getURL().toString(), description));
    }

    /**
     * Generate a one-line text-only message summarising the named bucket.
     */
    public String getSummaryLine(String statId) {
        int good = 0;
        int severe = 0;
        int badish = 0;
        int unk = 0;
        Collection<ReportItem> stats = statistics.get(statId);
        for (ReportItem s : stats) {
            if (s.isSevere()) {
                severe += 1;
            } else if (s.isBad()) {
                badish += 1;
            } else if (s.isGood()) {
                good += 1;
            } else {
                unk += 1;
            }
        }
        int totes = good + severe + badish + unk;
        return MessageFormat.format("  {0}: {1} %: {2} OK, {3} bad, "
                + "{4} warnings and {5} unknown. "
                + "= {6} items.\n", statId, 100 * good / totes,
                good, severe, badish, unk, totes);
    }

    /**
     * Generate a one-line text-only message summarising the named bucket.
     */
    public String getAllAsSummaryLine() {
        int good = 0;
        int severe = 0;
        int badish = 0;
        int unk = 0;
        Collection<ReportItem> stats = new ArrayList<ReportItem>();
        for (String statId : statistics.keySet()) {
            //System.out.println("key : " + statId);
            //System.out.println("value : " + statistics.get(statId));
            stats.addAll(statistics.get(statId));
        }
        for (ReportItem s : stats) {
            if (s.isSevere()) {
                severe += 1;
            } else if (s.isBad()) {
                badish += 1;
            } else if (s.isGood()) {
                good += 1;
            } else {
                unk += 1;
            }
        }
        int totes = good + severe + badish + unk;
        if (totes > 0) {
            return MessageFormat.format("  {0}: {1} %: {2} OK, {3} bad, "
                    + "{4} warnings and {5} unknown. "
                    + "= {6} items.\n", "Total", 100 * good / totes,
                    good, severe, badish, unk, totes);
        } else {
            return "no elements present.";
        }
    }

    /**
     * Generate summaries for all buckets.
     */
    public String getSummaryLines() {
        String rv = "";
        for (Map.Entry<String, Collection<ReportItem>> kv
                : statistics.entrySet()) {
            rv += getSummaryLine(kv.getKey());
        }
        //add summary of all buckets in one line
        rv += getAllAsSummaryLine();
        return rv;
    }

    /**
     * Generate error report for given bucket. Includes only severe errors and
     * problems in detail.
     */
    public String getErrorReport(String statId) {
        Collection<ReportItem> stats = statistics.get(statId);
        String rv = MessageFormat.format("{0}:\n", statId);
        int suppressed = 0;
        for (ReportItem s : stats) {
            if (s.isSevere()) {
                rv += s.getSummary() + "\n";
            } else {
                suppressed += 1;
            }
        }
        if (suppressed != 0) {
            rv += MessageFormat.format("{0} warnings and notes hidden\n",
                    suppressed);
        }
        return rv;
    }

    /**
     * Generate error report for given bucket. Includes only severe errors and
     * problems in detail.
     */
    public String getWarningReport(String statId) {
        Collection<ReportItem> stats = statistics.get(statId);
        String rv = MessageFormat.format("{0}:\n", statId);
        int suppressed = 0;
        for (ReportItem s : stats) {
            if (s.isBad()) {
                rv += s.getSummary() + "\n";
            } else {
                suppressed += 1;
            }
        }
        if (suppressed != 0) {
            rv += MessageFormat.format("{0} notes hidden\n",
                    suppressed);
        }
        return rv;
    }

    /**
     * Generate error reports for all buckets.
     */
    public String getErrorReports() {
        String rv = "Errors:\n";
        for (Map.Entry<String, Collection<ReportItem>> kv
                : statistics.entrySet()) {
            rv += getErrorReport(kv.getKey());
        }
        return rv;
    }

    /**
     * Generate error reports for all buckets.
     */
    public String getWarningReports() {
        String rv = "Warnings:\n";
        for (Map.Entry<String, Collection<ReportItem>> kv
                : statistics.entrySet()) {
            rv += getWarningReport(kv.getKey());
        }
        return rv;
    }

    /**
     * Generate verbose report for given bucket.
     */
    public String getFullReport(String statId) {
        Collection<ReportItem> stats = statistics.get(statId);
        String rv = MessageFormat.format("{0}:\n", statId);
        for (ReportItem s : stats) {
            if (s.isGood()) {
                rv += s.toString() + "\n";
            }
        }
        for (ReportItem s : stats) {
            if (s.isBad()) {
                rv += s.toString() + "\n";
            }
        }
        return rv;
    }

    /**
     * Generate verbose reports for all buckets.
     */
    public String getFullReports() {
        String rv = "All reports\n";
        for (Map.Entry<String, Collection<ReportItem>> kv
                : statistics.entrySet()) {
            rv += getFullReport(kv.getKey());
        }
        return rv;
    }

    /**
     * Get single collection of statistics.
     */
    public Collection<ReportItem> getRawStatistics() {
        Collection<ReportItem> allStats = new ArrayList<ReportItem>();
        for (Map.Entry<String, Collection<ReportItem>> kv
                : statistics.entrySet()) {
            allStats.addAll(kv.getValue());
        }
        return allStats;
    }

    /**
     * Get single collection of only error statistics.
     */
    public Collection<ReportItem> getErrorStatistics() {
        Collection<ReportItem> errorStats = new ArrayList<ReportItem>();
        Collection<ReportItem> onlyerrorStats = new ArrayList<ReportItem>();
        for (Map.Entry<String, Collection<ReportItem>> kv
                : statistics.entrySet()) {
            errorStats.addAll(kv.getValue());
        }
        for (ReportItem ri : errorStats) {
            if (ri.getSeverity().equals(Severity.CRITICAL) || ri.getSeverity().equals(Severity.WARNING) || ri.getSeverity().equals(Severity.MISSING)) {
                //now make the Location relative to the base dir
                ri.getLocation();
                onlyerrorStats.add(ri);
            }

        }
        return onlyerrorStats;
    }

    /**
     * Generate summaries for all buckets.
     */
    public String getFixJson(Corpus corpus) throws JDOMException {
        String rv = "";
        for (Map.Entry<String, Collection<ReportItem>> kfj
                : statistics.entrySet()) {
            rv += getFixLine(kfj.getKey(), corpus);
        }
        rv = rv + "\n";
        return rv;
    }

    /**
     * Generate summaries for all buckets.
     */
    public String getFixJson() {
        String rv = "";
        for (Map.Entry<String, Collection<ReportItem>> kfj
                : statistics.entrySet()) {
            rv += getFixLine(kfj.getKey());
        }
        rv = rv + "\n";
        return rv;
    }

    /**
     * Generate a one-line text-only message summarising the named bucket.
     */
    public String getFixLine(String statId, Corpus corpus) throws JDOMException {
        Collection<ReportItem> stats = statistics.get(statId);
        int fix = 0;
        int good = 0;
        int severe = 0;
        int badish = 0;
        int unk = 0;
        String line = "";
        for (ReportItem s : stats) {
            if (s.isFix()) {
                fix += 1;
            } else if (s.isSevere()) {
                severe += 1;
            } else if (s.isBad()) {
                badish += 1;
            } else if (s.isGood()) {
                good += 1;
            } else {
                unk += 1;
            }
        }
        //"2020-02-17T11:41:00Z"
        //now add the T that is needed for Kibana between date and time
        String patternDate = "yyyy-MM-dd";
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(patternDate);
        String date = simpleDateFormat.format(new Date());
        String patternTime = "hh:mm:ssZ";
        SimpleDateFormat simpleTimeFormat = new SimpleDateFormat(patternTime);
        String time = simpleTimeFormat.format(new Date());
        String dateTime = date + "T" + time;
        //System.out.println(dateTime);
        String corpusname = corpus.getCorpusName();
        //now we also need 
        /*
                number of words whole corpus
                number of sentences whole corpus
                number of transcriptions
                number of speakers whole corpus
                number of communications whole corpus
         */
        String corpuswords = corpus.getCorpusWords();
        String corpussents = corpus.getCorpusSentenceNumber();
        String corpustrans = corpus.getCorpusTranscriptionNumber();
        String corpusspeaks = corpus.getCorpusSpeakerNumber();
        String corpuscomms = corpus.getCorpusCommunicationNumber();
        //"corpus-words":1234,"corpus-sentences":2345,"corpus-transcriptions":12,"corpus-speakers":34,"corpus-transcriptions":12
        line = "{ \"index\": { \"_index\": \"inel-curation\", \"_type\": \"corpus-service-report\" }}\n{ \"doc\": { \"corpus\": \""
                + corpusname + "\", \"name\": \"" + statId + "\", \"method\": \"fix\", \"date\": \""
                + dateTime + "\", \"ok\": " + good + ", \"bad\": " + severe + ", \"fixed\": "
                + fix + ", \"corpus-words\": " + corpuswords + ", \"corpus-sentences\": " + corpussents + ", \"corpus-transcriptions\": " + corpustrans
                + ", \"corpus-speaker\": " + corpusspeaks + ", \"corpus-communications\": " + corpuscomms + " }}\n";
        return line;
    }

    /**
     * Generate a one-line text-only message summarising the named bucket.
     */
    public String getFixLine(String statId) {
        Collection<ReportItem> stats = statistics.get(statId);
        int fix = 0;
        int good = 0;
        int severe = 0;
        int badish = 0;
        int unk = 0;
        String line = "";
        for (ReportItem s : stats) {
            if (s.isFix()) {
                fix += 1;
            } else if (s.isSevere()) {
                severe += 1;
            } else if (s.isBad()) {
                badish += 1;
            } else if (s.isGood()) {
                good += 1;
            } else {
                unk += 1;
            }
        }
        //"2020-02-17T11:41:00Z"
        //now add the T that is needed for Kibana between date and time
        String patternDate = "yyyy-MM-dd";
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(patternDate);
        String date = simpleDateFormat.format(new Date());
        String patternTime = "hh:mm:ssZ";
        SimpleDateFormat simpleTimeFormat = new SimpleDateFormat(patternTime);
        String time = simpleTimeFormat.format(new Date());
        String dateTime = date + "T" + time;
        //System.out.println(dateTime);
        line = "{ \"index\": { \"_index\": \"inel-curation\", \"_type\": \"corpus-service-report\" }}\n{\"doc\": { \"name\": \"" + statId + "\", \"method\": \"fix\", \"date\": \"" + dateTime + "\", \"ok\": " + good + ", \"bad\": " + severe + ", \"fixed\": " + fix + " }}\n";
        return line;
    }

}
