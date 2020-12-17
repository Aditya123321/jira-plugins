package com.atlassian.plugins.tutorial.jira.reports;

 class MinutesDurationFormatterExtended implements DurationFormatterExtended{
     public String format(Long duration) {
         return (duration == null) ? "" : "" + duration.longValue() / 60;
     }

     public String shortFormat(Long duration) {
         return format(duration);
     }
}
