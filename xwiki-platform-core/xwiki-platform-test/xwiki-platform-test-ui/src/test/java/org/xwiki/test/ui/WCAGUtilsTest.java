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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.io.TempDir;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link WCAGUtils}.
 *
 * @version $Id$
 */
class WCAGUtilsTest
{
    private static final String MAVEN_BUILD_DIR = "maven.build.dir";

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.INFO);

    @TempDir
    private File buildDirectory;

    private String previousBuildDirectory;

    @BeforeEach
    void beforeEach()
    {
        this.previousBuildDirectory = System.setProperty(MAVEN_BUILD_DIR, this.buildDirectory.getAbsolutePath());
    }

    @AfterEach
    void afterEach()
    {
        if (this.previousBuildDirectory == null) {
            System.clearProperty(MAVEN_BUILD_DIR);
        } else {
            System.setProperty(MAVEN_BUILD_DIR, this.previousBuildDirectory);
        }
    }

    @Test
    void writeWCAGResultsOfSeveralTestClassesWithTheSameConfiguration() throws Exception
    {
        WCAGUtils firstTestClassUtils = new WCAGUtils();
        firstTestClassUtils.setupWCAGValidation(true, "org.xwiki.test.FirstIT", false);
        firstTestClassUtils.writeWCAGResults();

        // The second test class writes its reports in the same directory, without overwriting the reports of the
        // first one.
        WCAGUtils secondTestClassUtils = new WCAGUtils();
        secondTestClassUtils.setupWCAGValidation(true, "org.xwiki.test.SecondIT", false);
        secondTestClassUtils.writeWCAGResults();

        File wcagDirectory = new File(this.buildDirectory, "wcag-reports");
        assertTrue(new File(wcagDirectory, "wcagOverview-FirstIT.txt").isFile());
        assertTrue(new File(wcagDirectory, "wcagOverview-SecondIT.txt").isFile());

        assertEquals(4, this.logCapture.size());
        assertEquals("WCAG validation is enabled.", this.logCapture.getMessage(0));
        assertEquals("Time spent on WCAG validation for [org.xwiki.test.FirstIT]: [0.0] (in s)",
            this.logCapture.getMessage(1));
        assertEquals("WCAG validation is enabled.", this.logCapture.getMessage(2));
        assertEquals("Time spent on WCAG validation for [org.xwiki.test.SecondIT]: [0.0] (in s)",
            this.logCapture.getMessage(3));
    }
}
