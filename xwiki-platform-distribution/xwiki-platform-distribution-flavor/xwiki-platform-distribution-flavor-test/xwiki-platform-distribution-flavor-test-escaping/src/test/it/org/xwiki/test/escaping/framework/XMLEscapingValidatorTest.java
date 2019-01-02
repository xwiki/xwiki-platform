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
package org.xwiki.test.escaping.framework;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit test for {@link XMLEscapingValidator}.
 *
 * @version $Id$
 * @since 10.6RC1
 */
public class XMLEscapingValidatorTest
{
    @Test
    public void checkStringDelimitersWhenSingleQuoteInsideAttributeValue()
    {
        XMLEscapingValidator validator = new XMLEscapingValidator();

        // Test attributes

        // Escaped single quote & escaped double quote inside double quotes: that's ok
        validator.checkStringDelimiters("value=\"aaa&quot;bbb&apos;ccc&gt;ddd&lt;eee\"", 1);
        assertEquals(0, validator.getErrors().size());

        // Escaped single quote & escaped double quote inside single quotes: that's ok
        validator.checkStringDelimiters("value='aaa&quot;bbb&apos;ccc&gt;ddd&lt;eee'", 1);
        assertEquals(0, validator.getErrors().size());

        // Not escaped single quote inside single quotes: that's not ok!
        validator.checkStringDelimiters("value='aaa&quot;bbb'ccc&gt;ddd&lt;eee'", 1);
        assertEquals(1, validator.getErrors().size());
        validator.clear();

        // Not escaped double quote inside double quotes: that's not ok!
        validator.checkStringDelimiters("value=\"aaa\"bbb'ccc&gt;ddd&lt;eee\"", 1);
        assertEquals(1, validator.getErrors().size());
        validator.clear();

        // Not escaped single quote inside single quotes: that's not ok!
        validator.checkStringDelimiters("value='aaa\"bbb'ccc&gt;ddd&lt;eee'", 1);
        assertEquals(1, validator.getErrors().size());
        validator.clear();

        // Not escaped single quote inside double quotes: that's ok
        validator.checkStringDelimiters("value=\"aaa&quot;bbb'ccc&gt;ddd&lt;eee\"", 1);
        assertEquals(0, validator.getErrors().size());

        // Not escaped double quote inside single quotes: that's ok
        validator.checkStringDelimiters("value='aaa\"bbb&apos;ccc&gt;ddd&lt;eee'", 1);
        assertEquals(0, validator.getErrors().size());

        // Content

        // Escaped quotes in content: that's ok
        validator.checkStringDelimiters("<p>aaa&quot;bbb&apos;ccc&gt;ddd&lt;eee</p>", 1);
        assertEquals(0, validator.getErrors().size());

        // Not escaped quotes in content: that's ok
        validator.checkStringDelimiters("<p>aaa\"bbb'ccc&gt;ddd&lt;eee</p>", 1);
        assertEquals(0, validator.getErrors().size());
    }
}
