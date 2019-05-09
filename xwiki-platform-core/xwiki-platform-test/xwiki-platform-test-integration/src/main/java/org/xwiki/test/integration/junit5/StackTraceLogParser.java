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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

import org.xwiki.text.StringUtils;

/**
 * Parses XWiki log formats and aggregates stack traces. For example the following:
 * <code><pre>
 * 2019-05-06 20:02:37,331 [Exec Stream Pumper] - 2019-05-06 20:02:37,330
 *   [http://localhost:8080/xwiki/webjars/wiki%3Axwiki/jstree/3.3.7/jstree.min.js]
 *     ERROR ebJarsResourceReferenceHandler - Failed to read resource [jstree/3.3.7/jstree.min.js]
 * 2019-05-06 20:02:37,331 [Exec Stream Pumper] - org.xwiki.resource.ResourceReferenceHandlerException:
 *   Failed to read resource [jstree/3.3.7/jstree.min.js]
 * 2019-05-06 20:02:37,331 [Exec Stream Pumper] -  at
 *   org.xwiki.resource.servlet.AbstractServletResourceReferenceHandler.serveResource(AbstractServlet...
 * ...
 * 2019-05-06 20:02:37,352 [Exec Stream Pumper] - Caused by: org.eclipse.jetty.io.EofException: null
 * ...
 * 2019-05-06 20:02:37,359 [Exec Stream Pumper] -  ... 75 common frames omitted
 * </pre></code>
 *
 * Will return a line of:
 *
 * <code><pre>
 * 2019-05-06 20:02:37,331 [Exec Stream Pumper] - 2019-05-06 20:02:37,330
 *   [http://localhost:8080/xwiki/webjars/wiki%3Axwiki/jstree/3.3.7/jstree.min.js]
 *     ERROR ebJarsResourceReferenceHandler - Failed to read resource [jstree/3.3.7/jstree.min.js]
 * org.xwiki.resource.ResourceReferenceHandlerException: Failed to read resource [jstree/3.3.7/jstree.min.js]
 * Caused by: org.eclipse.jetty.io.EofException: null
 * </pre></code>
 *
 * @version $Id$
 * @since 11.4RC1
 */
public class StackTraceLogParser
{
    /**
     * @param logContent the stack trace to parse
     * @return the lines of the log with stack traces aggregated into a single line containing all the "caused by"
     */
    public List<String> parse(String logContent)
    {
        try {
            return parseInternal(logContent);
        } catch (IOException e) {
            // This shouldn't happen normally.
            throw new RuntimeException(String.format("Failed to reads log content for [%s]", logContent));
        }
    }

    private List<String> parseInternal(String logContent) throws IOException
    {
        List<String> results = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new StringReader(logContent))) {
            // When we notice a stack trace, we keep reading till the end of it and return all the "caused by" parts as
            // a single String, so that they can later be excluded for example.
            //
            // Example of stack trace:
            //
            //  2019-05-06 20:02:37,331 [Exec Stream Pumper] - 2019-05-06 20:02:37,330
            //    [http://localhost:8080/xwiki/webjars/wiki%3Axwiki/jstree/3.3.7/jstree.min.js]
            //      ERROR ebJarsResourceReferenceHandler - Failed to read resource [jstree/3.3.7/jstree.min.js]
            //  2019-05-06 20:02:37,331 [Exec Stream Pumper] - org.xwiki.resource.ResourceReferenceHandlerException:
            //    Failed to read resource [jstree/3.3.7/jstree.min.js]
            //  2019-05-06 20:02:37,331 [Exec Stream Pumper] -  at
            //    org.xwiki.resource.servlet.AbstractServletResourceReferenceHandler.serveResource(AbstractServlet...
            //  ...
            //  2019-05-06 20:02:37,352 [Exec Stream Pumper] - Caused by: org.eclipse.jetty.io.EofException: null
            //  ...
            //  2019-05-06 20:02:37,359 [Exec Stream Pumper] -  ... 75 common frames omitted
            //
            // Here's how we recognize a stack trace:
            // - Store the position of the first "-" character in the current line.
            // - When we find a line that has "-(space)(tab)at(space)" with the "-" at the same position as the
            //   previously recorded one, then consider that the previous previous line is the start of a stack trace.
            // - Keep reading till we have "-(space)(space)at(space)"
            // - When we find a "-(space)Caused by:(space)" pattern, then aggregate the text in the buffer
            // - When we find a "-(space)(tab)...(space)" pattern then keep reading
            StringBuilder buffer = null;
            boolean inStackTrace = false;
            int dashPosition = -1;
            String line;
            while ((line = reader.readLine()) != null) {
                int currentDashPosition = line.indexOf(" - ");
                if (results.size() > 1 && currentDashPosition == dashPosition) {
                    if (isMatchingAtPattern(currentDashPosition, line)) {
                        // If we're already reading a stack trace then discard, otherwise consider that the previously
                        // saved 2 lines at part of the same stack trace and remove them from the array list to store
                        // them in the buffer.
                        if (!inStackTrace) {
                            buffer = constructNewBuffer(currentDashPosition, results);
                            inStackTrace = true;
                        }
                    } else if (isMatchingCausedByPattern(currentDashPosition, line)) {
                        if (inStackTrace) {
                            // Aggregate to buffer so that it can be later asserted.
                            buffer.append('\n').append(line.substring(currentDashPosition + 3));
                        } else {
                            results.add(line);
                        }
                    } else if (isMatchingOmittedFramesPattern(currentDashPosition, line)) {
                        if (!inStackTrace) {
                            results.add(line);
                        }
                    } else {
                        buffer = saveBuffer(buffer, results);
                        inStackTrace = false;
                        results.add(line);
                    }
                } else {
                    buffer = saveBuffer(buffer, results);
                    inStackTrace = false;
                    results.add(line);
                }
                dashPosition = currentDashPosition;
            }
        }
        return results;
    }

    private StringBuilder constructNewBuffer(int currentDashPosition, List<String> results)
    {
        StringBuilder buffer;
        String line2 = results.remove(results.size() - 1);
        String line1 = results.remove(results.size() - 1);
        buffer = new StringBuilder(line1);
        // Strip the prefix from line2 to get a nice string to assert.
        line2 = line2.substring(currentDashPosition + 3);
        buffer.append('\n').append(line2);
        return buffer;
    }

    private StringBuilder saveBuffer(StringBuilder buffer, List<String> results)
    {
        if (!StringUtils.isEmpty(buffer)) {
            results.add(buffer.toString());
        }
        return null;
    }

    private boolean isMatchingAtPattern(int currentDashPosition, String line)
    {
        return isMatchingPattern(" \tat ", currentDashPosition, line);
    }

    private boolean isMatchingCausedByPattern(int currentDashPosition, String line)
    {
        return isMatchingPattern(" Caused by: ", currentDashPosition, line);
    }

    private boolean isMatchingOmittedFramesPattern(int currentDashPosition, String line)
    {
        return isMatchingPattern(" \t... ", currentDashPosition, line);
    }

    private boolean isMatchingPattern(String pattern, int currentDashPosition, String line)
    {
        if (line.length() > currentDashPosition + pattern.length() + 2) {
            String substring = line.substring(currentDashPosition + 2, currentDashPosition + pattern.length() + 2);
            return substring.equals(pattern);
        } else {
            return false;
        }
    }
}
