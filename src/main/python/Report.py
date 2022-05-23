from enum import Enum
import json


class Severity(str, Enum):
    CRITICAL = "CRITICAL",
    WARNING = "WARNING",
    NOTE = "NOTE",
    MISSING = "MISSING",
    CORRECT = "CORRECT",
    IFIXEDITFORYOU = "IFIXEDITFORYOU",
    UNKNOWN = "UNKNOWN"

    def __str__(self):
        if self == Severity.CRITICAL:
            return "CRITICAL"
        elif self == Severity.WARNING:
            return "WARNING"
        elif self == Severity.NOTE:
            return "NOTE"
        elif self == Severity.MISSING:
            return "MISSING"
        elif self == Severity.CORRECT:
            return "CORRECT"
        elif self == Severity.IFIXEDITFORYOU:
            return "IFIXEDITFORYOU"
        else:
            return "UNKNOWN"

    def toJson(self):
        return self.__str__()


class ReportItem(dict):
    """Class for report items"""

    def __init__(self,
                 severity=Severity.CRITICAL,
                 what="Totally unexpected error",
                 howto="No known fix",
                 e=None,
                 filename=None,
                 lines=None,
                 columns=None,
                 function="Unknown function"
                 ):
        """Constructor for ReportItem

        Keyword arguments:
        severity -- Severity of the item (see Severity)
        what -- The message for the item
        howto -- Message how to fix issue
        e -- Exception encountered
        filename -- File processed while error occurred
        lines -- The lines affected by the problem
        columns -- The columns affected by the problem
        function -- The checker function
        """
        dict.__init__(self,
                      severity=severity,
                      what=what,
                      howto=howto,
                      e=e,
                      filename=filename,
                      lines=lines,
                      columns=columns,
                      function=function
                      )

        def __str__(self):
            return json.dumps(self)
