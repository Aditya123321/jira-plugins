package com.tadigital.jira.reports.report;

public class MinutesDurationFormatterExtended implements DurationFormatterExtended{
     public String format(Long duration) {
         return (duration == null) ? "" : "" + duration.longValue() / 60;
     }

     public String shortFormat(Long duration) {
         return format(duration);
     }
}
