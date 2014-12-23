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
package org.xwiki.validator;

import java.util.List;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Test class for {@link org.xwiki.validator.XWikiValidator}.
 *
 * @since 6.4M3
 * @version $Id$
 */
public class XWikiValidatorTest
{
    @Test
    public void testValid() throws Exception
    {
        XWikiValidator validator = new XWikiValidator();
        validator.setDocument(getClass().getResourceAsStream("/xwiki-valid.html"));
        List<ValidationError> errors = validator.validate();
        assertTrue(errors.isEmpty());
    }

    @Test
    public void testInvalid() throws Exception
    {
        XWikiValidator validator = new XWikiValidator();
        validator.setDocument(getClass().getResourceAsStream("/xwiki-invalid.html"));
        List<ValidationError> errors = validator.validate();
        assertEquals(errors, validator.getErrors());
        assertEquals(1, errors.size());
        assertEquals("Found rendering error", errors.get(0).getMessage());
        validator.clear();
        assertTrue(validator.getErrors().isEmpty());
    }
}
