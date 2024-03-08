/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.xwiki.test.ui;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Helper methods for validating WCAG, not related to a specific Page Object. Also made available to tests classes.
 *
 * @version $Id$
 * @since 15.2RC1
 */
public class WCAGUtils
{
    private static final Logger LOGGER = LoggerFactory.getLogger(WCAGUtils.class);

    private static final String FILENAME_OVERVIEW = "wcagOverview.txt";
    private static final String FILENAME_INCOMPLETE = "wcagIncompletes.txt";
    private static final String FILENAME_WARNING = "wcagWarnings.txt";
    private static final String FILENAME_FAILING = "wcagFails.txt";

    /**
     * Cached accessibility results.
     */
    private WCAGContext wcagContext = new WCAGContext();

    /**
     * @param wcag true if WCAG tests must be executed, false otherwise
     * @param testClassName the PO class name to use for logging and reporting
     * @param stopOnError {@code false} if WCAG validation should ignore errors, {@code true} otherwise.
     */
    public void setupWCAGValidation(boolean wcag, String testClassName, boolean stopOnError)
    {
        this.wcagContext.setWCAGEnabled(wcag);
        this.wcagContext.setWCAGStopOnError(stopOnError);
        if (wcag) {
            LOGGER.info("WCAG validation is enabled.");
            wcagContext.setTestClassName(testClassName);
        }
    }

    /**
     * Change the current WCAG test method for reporting.
     *
     * @param testMethodName the test method name to update
     */
    public void changeWCAGTestMethod(String testMethodName)
    {
        if (this.wcagContext.isWCAGEnabled()) {
            this.wcagContext.setTestMethodName(testMethodName);
        }
    }

    /**
     * Get the WCAG context for the test suite.
     *
     * @return the WCAG Context
     */
    public WCAGContext getWCAGContext()
    {
        return this.wcagContext;
    }

    /**
     * Ends the validation process for WCAG.
     */
    public void endWCAGValidation() throws IOException
    {
        if (this.wcagContext.isWCAGEnabled()) {
            writeWCAGResults();
            assertWCAGResults();
        }
    }

    /**
     * Writes the WCAG validation results into report files. There are four files:
     * * wcagOverview : overview of the results, contains stats and will always be generated.
     * * wcagIncomplete : list of the results that are Incomplete and need further manual inspection.
     * * wcagWarnings : list of the results that are violations, not failing the build.
     * * wcagFails : list of the results that are violations, failing the build.
     * @throws IOException if the directory creation fails
     */
    public void writeWCAGResults() throws IOException
    {
        float totalTime = (float) this.wcagContext.getWCAGTime() / 1000;
        LOGGER.info("Time spent on WCAG validation for [{}]: [{}] (in s)",
            getWCAGContext().getTestClassName(), totalTime);

        File wcagDir = new File(getWCAGReportPathOnHost());
        Files.createDirectory(wcagDir.toPath());
        File overviewFile = new File(wcagDir, WCAGUtils.FILENAME_OVERVIEW);
        WCAGContext.writeWCAGReportToFile(overviewFile, this.wcagContext.buildOverview());

        if (this.wcagContext.hasWCAGIncomplete()) {
            LOGGER.error(
                "There are [{}] incomplete accessibility checks in the test suite. See [{}/{}] for more details.",
                this.wcagContext.getWCAGIncompleteCount(), getWCAGReportPathOnHost(), WCAGUtils.FILENAME_INCOMPLETE);
            File incompleteFile = new File(wcagDir, WCAGUtils.FILENAME_INCOMPLETE);
            WCAGContext.writeWCAGReportToFile(incompleteFile, this.wcagContext.buildIncompleteReport());
        }
        if (this.wcagContext.hasWCAGWarnings()) {
            LOGGER.warn("There are [{}] accessibility warnings in the test suite. See [{}/{}] for more details.",
                this.wcagContext.getWCAGWarnCount(), getWCAGReportPathOnHost(), WCAGUtils.FILENAME_WARNING);
            logViolationCount(false);
            File warningsFile = new File(wcagDir, WCAGUtils.FILENAME_WARNING);
            WCAGContext.writeWCAGReportToFile(warningsFile, this.wcagContext.buildWarningsReport());
        }
        if (this.wcagContext.hasWCAGFails()) {
            LOGGER.error("There are [{}] accessibility fails in the test suite. See [{}/{}] for more details.",
                this.wcagContext.getWCAGFailCount(), getWCAGReportPathOnHost(), WCAGUtils.FILENAME_FAILING);
            logViolationCount(true);
            File failsFile = new File(wcagDir, WCAGUtils.FILENAME_FAILING);
            WCAGContext.writeWCAGReportToFile(failsFile, this.wcagContext.buildFailsReport());
        }
    }

    /**
     * Logs the number of violations at the level of the test suite.
     */
    private void logViolationCount(boolean isFailingViolations)
    {
        Map<String, Integer> violationCounts = wcagContext.getViolationCountPerRule();
        for (String ruleID : violationCounts.keySet()) {
            if (isFailingViolations
                && wcagContext.isFailing(ruleID)) {
                LOGGER.error("    [{}] : [{}] failures", ruleID, violationCounts.get(ruleID));
            } else if (!isFailingViolations
                && !wcagContext.isFailing(ruleID)) {
                LOGGER.warn("    [{}] : [{}] warnings", ruleID, violationCounts.get(ruleID));
            }
        }
    }

    /**
     * Assert that the results of the different WCAG failing checks are all empty.
     */
    public void assertWCAGResults()
    {
        assertFalse(this.wcagContext.hasWCAGFails(), this.wcagContext.buildFailsReport());
    }

    /**
     * @return the path where the WCAG reports are stored by Maven after validation, on the host.
     */
    private String getWCAGReportPathOnHost()
    {
        String testClassesDirectory;
        String mavenBuildDir = System.getProperty("maven.build.dir");
        if (mavenBuildDir == null) {
            testClassesDirectory = "target/wcag-reports";
        } else {
            testClassesDirectory = String.format("%s/wcag-reports", mavenBuildDir);
        }
        return testClassesDirectory;
    }
}
