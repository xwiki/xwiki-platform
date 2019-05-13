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
package org.xwiki.test.integration.junit5;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Allow registering excludes and expected failling log content for tests.
 *
 * @version $Id$
 * @since 11.4RC1
 */
public class LogCaptureConfiguration
{
    private List<String> excludedLines = new ArrayList<>();

    private List<String> expectedLines = new ArrayList<>();

    /**
     * @param excludeLines the lines to exclude from failing the test
     */
    public void registerExcludes(String... excludeLines)
    {
        this.excludedLines.addAll(Arrays.asList(excludeLines));
    }

    /**
     * @param expectedLines the failing lines to expect
     */
    public void registerExpected(String... expectedLines)
    {
        this.excludedLines.addAll(Arrays.asList(expectedLines));
    }

    /**
     * @return the list of excluded lines (i.e. lines that should fail the test but that are excluded for now till the
     *         test or code is fixed)
     */
    public List<String> getExcludedLines()
    {
        return this.excludedLines;
    }

    /**
     * @return the list of expected lines (i.e. lines for which it's normal that they don't fail the test)
     */
    public List<String> getExpectedLines()
    {
        return this.expectedLines;
    }
}
