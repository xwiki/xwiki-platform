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

import java.util.List;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for {@link StackTraceLogParser}.
 *
 * @version $Id$
 * @since 11.4RC1
 */
public class StackTraceLogParserTest
{
    @Test
    public void parseLogWithStackTrace() throws Exception
    {
        String log = IOUtils.toString(this.getClass().getClassLoader().getResourceAsStream("stacktrace.txt"), "UTF-8");
        StackTraceLogParser parser = new StackTraceLogParser();
        List<String> results = parser.parse(log);
        assertEquals(47, results.size());
        assertEquals("2019-05-06 20:02:37,331 [Exec Stream Pumper] - 2019-05-06 20:02:37,330 "
                + "[http://localhost:8080/xwiki/webjars/wiki%3Axwiki/jstree/3.3.7/jstree.min.js] "
                    + "ERROR ebJarsResourceReferenceHandler - Failed to read resource [jstree/3.3.7/jstree.min.js]\n"
            + "org.xwiki.resource.ResourceReferenceHandlerException: "
                + "Failed to read resource [jstree/3.3.7/jstree.min.js]\n"
            + "Caused by: org.eclipse.jetty.io.EofException: null\n"
            + "Caused by: java.io.IOException: Broken pipe", results.get(45));
    }
}
