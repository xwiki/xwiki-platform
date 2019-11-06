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
package org.xwiki.test.integration.junit;

import java.util.ArrayList;
import java.util.List;

/**
 * Allow registering excludes and expected failing log content for tests.
 *
 * @version $Id$
 * @since 11.4RC1
 */
public class LogCaptureConfiguration
{
    private List<Line> excludedLines = new ArrayList<>();

    private List<Line> expectedLines = new ArrayList<>();

    /**
     * @param excludeLines the lines to exclude from failing the test
     */
    public void registerExcludes(String... excludeLines)
    {
        for (String excludeLine : excludeLines) {
            this.excludedLines.add(new Line(excludeLine));
        }
    }

    /**
     * @param excludeLineRegexes the lines to exclude from failing the test (defined as regexes)
     */
    public void registerExcludeRegexes(String... excludeLineRegexes)
    {
        for (String line : excludeLineRegexes) {
            this.excludedLines.add(new Line(line, true));
        }
    }

    /**
     * @param expectedLines the failing lines to expect
     */
    public void registerExpected(String... expectedLines)
    {
        for (String line : expectedLines) {
            this.expectedLines.add(new Line(line));
        }
    }

    /**
     * @param expectedLineRegexes the failing lines to expect (defined as regexes)
     */
    public void registerExpectedRegexes(String... expectedLineRegexes)
    {
        for (String line : expectedLineRegexes) {
            this.expectedLines.add(new Line(line, true));
        }
    }

    /**
     * @return the list of excluded lines (i.e. lines that should fail the test but that are excluded for now till the
     * test or code is fixed)
     */
    public List<Line> getExcludedLines()
    {
        return this.excludedLines;
    }

    /**
     * @return the list of expected lines (i.e. lines for which it's normal that they don't fail the test)
     */
    public List<Line> getExpectedLines()
    {
        return this.expectedLines;
    }
}
