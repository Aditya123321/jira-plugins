package com.atlassian.plugins.tutorial.jira.reports;

import com.atlassian.jira.util.I18nHelper;
import com.atlassian.jira.util.JiraDurationUtils;
import com.atlassian.plugin.spring.scanner.annotation.component.Scanned;
import com.atlassian.plugin.spring.scanner.annotation.imports.JiraImport;


public class DurationFormatterExtendedImpl implements DurationFormatterExtended{
    @JiraImport
    private final JiraDurationUtils jiraDurationUtils;
    @JiraImport
    private final I18nHelper i18nHelper;

    DurationFormatterExtendedImpl(final I18nHelper i18nHelper, final JiraDurationUtils jiraDurationUtils) {
        this.i18nHelper = i18nHelper;
        this.jiraDurationUtils = jiraDurationUtils;
    }

    /**
     * Formats the duration. If duration is null, returns a dash..
     *
     * @param duration duration
     * @return formatted duration String or i18ned "unknown" if null.
     */
    public String format(Long duration) {
        if (duration == null) {
            return "-";
        }
        duration = Math.abs(duration.longValue());
        return jiraDurationUtils.getFormattedDuration(duration, i18nHelper.getLocale());
    }

    public String shortFormat(Long duration) {
        if (duration == null) {
            return "-";
        }
        duration = Math.abs(duration.longValue());
        return jiraDurationUtils.getShortFormattedDuration(duration);
    }
}
