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
package org.xwiki.wysiwyg.server.internal.cleaner;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import junit.framework.TestSuite;

/**
 * A suite of {@link HTMLCleanerTestCase}.
 * 
 * @version $Id$
 */
public class HTMLCleanerTestSuite extends TestSuite
{
    /**
     * The line that separates the HTML input from the expected clean HTML.
     */
    private static final String INPUT_EXPECTED_SEPARATOR = "---";

    /**
     * The string that prefixes comment lines.
     */
    private static final String COMMENT_LINE_PREFIX = "#";

    /**
     * The string used to escape the new line character. This is very useful when you want to split the HTML input or
     * the expected clean HTML on multiple lines without inserting new line characters in the content.
     */
    private static final String NEW_LINE_ESCAPE = "\\";

    /**
     * Creates a new test suite with the given name.
     * 
     * @param suiteName the name of the test suite
     */
    public HTMLCleanerTestSuite(String suiteName)
    {
        super(suiteName);
    }

    /**
     * Adds the tests listed in the specified resource file.
     * 
     * @param testResourceName the name of a resource file that lists the tests to be run
     * @throws IOException if reading the test resource fails
     */
    public void addTestsFromResource(String testResourceName) throws IOException
    {
        for (String testName : getTestNames(getClass().getResourceAsStream(testResourceName))) {
            addTest(createTestFromResource(testName));
        }
    }

    /**
     * Reads the list of test names from the given input stream.
     * 
     * @param source where to read the test names from
     * @return a list of test names
     * @throws IOException if reading the test names fails
     */
    private List<String> getTestNames(InputStream source) throws IOException
    {
        BufferedReader reader = new BufferedReader(new InputStreamReader(source));
        List<String> testNames = new ArrayList<String>();
        try {
            String line = reader.readLine();
            while (line != null) {
                if (!line.startsWith(COMMENT_LINE_PREFIX)) {
                    testNames.add(line.trim());
                }
                line = reader.readLine();
            }
        } finally {
            reader.close();
        }
        return testNames;
    }

    /**
     * Creates a new test by reading the resource file with the specified name.
     * 
     * @param testResourceName the name of a resource file that contains the HTML input and the expected clean HTML
     * @return a new {@link HTMLCleanerTestCase}
     * @throws IOException if reading the resource files fails
     */
    private HTMLCleanerTestCase createTestFromResource(String testResourceName) throws IOException
    {
        InputStream source = getClass().getResourceAsStream('/' + testResourceName + ".test");
        BufferedReader reader = new BufferedReader(new InputStreamReader(source));
        try {
            StringBuilder input = new StringBuilder();
            String line = reader.readLine();
            while (line != null && !line.equals(INPUT_EXPECTED_SEPARATOR)) {
                appendLine(input, line);
                line = reader.readLine();
            }
            StringBuilder expected = new StringBuilder();
            // Skip the line that separates the input from the expected HTML.
            line = reader.readLine();
            while (line != null) {
                appendLine(expected, line);
                line = reader.readLine();
            }
            return new HTMLCleanerTestCase(testResourceName, input.toString(), expected.toString());
        } finally {
            reader.close();
        }
    }

    /**
     * Skips the line if it's a comment and removes the new line character if it is escaped.
     * 
     * @param output where to append the given line
     * @param line the line of text to be appended
     */
    private void appendLine(StringBuilder output, String line)
    {
        if (line.startsWith(COMMENT_LINE_PREFIX)) {
            return;
        } else if (line.endsWith(NEW_LINE_ESCAPE)) {
            output.append(line, 0, line.length() - NEW_LINE_ESCAPE.length());
        } else {
            output.append(line).append('\n');
        }
    }
}
