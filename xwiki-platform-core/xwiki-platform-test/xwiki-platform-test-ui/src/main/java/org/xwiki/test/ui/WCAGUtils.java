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

    private static final String FILENAME_OVERVIEW = "wcagOverview";
    private static final String FILENAME_INCOMPLETE = "wcagIncompletes";
    private static final String FILENAME_WARNING = "wcagWarnings";
    private static final String FILENAME_FAILING = "wcagFails";

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
     * Writes the WCAG validation results into report files, suffixed with the simple name of the test class, so that
     * the reports of several test classes executed with the same configuration don't overwrite each other. There are
     * four files:
     * * wcagOverview-[TestClass].txt : overview of the results, contains stats and will always be generated.
     * * wcagIncompletes-[TestClass].txt : list of the results that are Incomplete and need further manual inspection.
     * * wcagWarnings-[TestClass].txt : list of the results that are violations, not failing the build.
     * * wcagFails-[TestClass].txt : list of the results that are violations, failing the build.
     * @throws IOException if the directory creation fails
     */
    public void writeWCAGResults() throws IOException
    {
        float totalTime = (float) this.wcagContext.getWCAGTime() / 1000;
        LOGGER.info("Time spent on WCAG validation for [{}]: [{}] (in s)",
            getWCAGContext().getTestClassName(), totalTime);

        // The directory already exists when another test class has been executed before with the same configuration.
        File wcagDir = new File(getWCAGReportPathOnHost());
        Files.createDirectories(wcagDir.toPath());
        WCAGContext.writeWCAGReportToFile(getReportFile(wcagDir, FILENAME_OVERVIEW),
            this.wcagContext.buildOverview());

        if (this.wcagContext.hasWCAGIncomplete()) {
            File incompleteFile = getReportFile(wcagDir, FILENAME_INCOMPLETE);
            LOGGER.info("There are [{}] incomplete accessibility checks in the test suite. See [{}] for more details.",
                this.wcagContext.getWCAGIncompleteCount(), incompleteFile);
            WCAGContext.writeWCAGReportToFile(incompleteFile, this.wcagContext.buildIncompleteReport());
        }
        if (this.wcagContext.hasWCAGWarnings()) {
            File warningsFile = getReportFile(wcagDir, FILENAME_WARNING);
            LOGGER.warn("There are [{}] accessibility warnings in the test suite. See [{}] for more details.",
                this.wcagContext.getWCAGWarnCount(), warningsFile);
            logViolationCount(false);
            WCAGContext.writeWCAGReportToFile(warningsFile, this.wcagContext.buildWarningsReport());
        }
        if (this.wcagContext.hasWCAGFails()) {
            File failsFile = getReportFile(wcagDir, FILENAME_FAILING);
            LOGGER.error("There are [{}] accessibility fails in the test suite. See [{}] for more details.",
                this.wcagContext.getWCAGFailCount(), failsFile);
            logViolationCount(true);
            WCAGContext.writeWCAGReportToFile(failsFile, this.wcagContext.buildFailsReport());
        }
    }

    private File getReportFile(File wcagDir, String reportName)
    {
        String testClassName = this.wcagContext.getTestClassName();
        String testClassSimpleName = testClassName.substring(testClassName.lastIndexOf('.') + 1);
        return new File(wcagDir, String.format("%s-%s.txt", reportName, testClassSimpleName));
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
