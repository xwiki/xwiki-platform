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

/**
 * Represents a validation line in the {@code pom.xml}.
 * See
 *
 * @version $Id$
 * @since 11.4RC1
 */
class ValidationLine
{
    private String testName;

    private String line;

    /**
     * @param line the line content without any test name scope
     */
    ValidationLine(String line)
    {
        this.line = line;
    }

    /**
     * @param testName the test for which this line must match
     * @param line the line content without any test name scope
     */
    ValidationLine(String testName, String line)
    {
        this(line);
        this.testName = testName;
    }

    public String getTestName()
    {
        return this.testName;
    }

    public String getLine()
    {
        return this.line;
    }
}
