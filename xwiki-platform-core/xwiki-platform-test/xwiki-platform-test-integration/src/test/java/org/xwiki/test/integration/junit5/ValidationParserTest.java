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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Unit tests for {@link ValidationParser}.
 *
 * @version $Id$
 * @since 11.4RC1
 */
public class ValidationParserTest
{
    @Test
    public void parse()
    {
        ValidationParser parser = new ValidationParser();
        List<ValidationLine> results = parser.parse(""
            + "\"meta.js?cache-version=\",\n"
            + "\"require.min.js?r=1, line 7: Error: Script error for \\\"selectize\\\", needed by: xwiki\",\n"
            + "\"{{.*my.*test}}meta.js?cache-version=\",\n"
            + "\"{{invalidtestname\"\n");
        assertEquals(4, results.size());
        assertNull(results.get(0).getTestName());
        assertEquals("meta.js?cache-version=", results.get(0).getLine());
        assertNull(results.get(1).getTestName());
        assertEquals("require.min.js?r=1, line 7: Error: Script error for \"selectize\", needed by: xwiki",
            results.get(1).getLine());
        assertEquals(".*my.*test", results.get(2).getTestName());
        assertEquals("meta.js?cache-version=", results.get(2).getLine());
        assertNull(results.get(3).getTestName());
        assertEquals("{{invalidtestname", results.get(3).getLine());
    }
}
