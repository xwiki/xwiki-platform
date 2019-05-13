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
import java.util.List;

/**
 * Parses "excludes/expected" lines from the {@code pom.xml}. Example syntax:
 * <pre><code>
 *   "meta.js?cache-version=",
 *   "require.min.js?r=1, line 7: Error: Script error for \"selectize\", needed by: xwiki\",
 *   "{{.*MyTest.*testMethod}}meta.js?cache-version=",
 *   "{{invalidtestname"
 * </code></pre>
 * <p>
 * The format of the test pattern corresponds to the JUnit5 {@link org.junit.platform.launcher.TestIdentifier} unique
 * id. For example {@code [engine:junit-jupiter]/[class:org.xwiki.MyTest]/[method:outputToConsole()]}.
 *
 * @version $Id$
 * @since 11.4RC1
 */
public class ValidationParser
{
    /**
     * @param content the line content to parse
     * @return the list of {@link ValidationLine} objects representing the parsed lines
     */
    public List<ValidationLine> parse(String content)
    {
        List<ValidationLine> definitionList = new ArrayList<>();
        boolean inQuotes = false;
        boolean inEscape = false;
        StringBuilder b = new StringBuilder();
        for (char c : content.toCharArray()) {
            switch (c) {
                case ',':
                    if (inQuotes || inEscape) {
                        b.append(c);
                    } else {
                        definitionList.add(toValidationLine(b.toString()));
                        b = new StringBuilder();
                    }
                    inEscape = false;
                    break;
                case '\"':
                    if (!inEscape) {
                        inQuotes = !inQuotes;
                    } else {
                        b.append(c);
                        inEscape = false;
                    }
                    break;
                case '\\':
                    if (inEscape) {
                        b.append(c);
                    }
                    inEscape = !inEscape;
                    break;
                default:
                    //Ignore characters not in quotes. This allows ignoring new lines and spaces between entries.
                    if (inQuotes) {
                        b.append(c);
                    }
                    inEscape = false;
                    break;
            }
        }
        definitionList.add(toValidationLine(b.toString()));
        return definitionList;
    }

    private ValidationLine toValidationLine(String line)
    {
        ValidationLine pattern;
        if (line.startsWith("{{")) {
            int pos = line.indexOf("}}");
            if (pos > -1) {
                pattern = new ValidationLine(line.substring(2, pos), line.substring(pos + 2));
            } else {
                // Invalid, consider there's no test name pattern specified
                pattern = new ValidationLine(line);
            }
        } else {
            pattern = new ValidationLine(line);
        }
        return pattern;
    }
}
