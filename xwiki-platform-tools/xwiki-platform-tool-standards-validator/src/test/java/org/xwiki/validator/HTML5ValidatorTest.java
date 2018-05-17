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

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for {@link org.xwiki.validator.HTML5Validator}.
 *
 * @since 6.0RC1
 * @version $Id$
 */
public class HTML5ValidatorTest
{
    private HTML5Validator validator;

    @Before
    public void setUp()
    {
        validator = new HTML5Validator();
    }

    @Test
    public void testValid()
    {
        validator.setDocument(getClass().getResourceAsStream("/html5-valid.html"));
        List<ValidationError> errors = validator.validate();
        assertTrue(errors.isEmpty());
    }

    @Test
    public void testInvalid()
    {
        validator.setDocument(getClass().getResourceAsStream("/html5-invalid.html"));
        List<ValidationError> errors = validator.validate();
        assertEquals(errors, validator.getErrors());
        assertEquals(5, errors.size());
        validator.clear();
        assertTrue(validator.getErrors().isEmpty());
    }

    @Test
    public void testWhenException()
    {
        validator.setDocument(null);
        boolean exceptionCaught = false;
        try {
            validator.validate();
        } catch(RuntimeException e) {
            exceptionCaught = true;
        }
        assertTrue(exceptionCaught);
    }

    @Test
    public void testName()
    {
        assertEquals("HTML5", validator.getName());
    }
}
