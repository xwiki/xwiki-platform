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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for {@link org.xwiki.validator.HTML5Validator}.
 *
 * @version $Id$
 * @since 6.0RC1
 */
class HTML5ValidatorTest
{
    private HTML5Validator validator;

    @BeforeEach
    void setUp()
    {
        this.validator = new HTML5Validator();
    }

    @Test
    void valid()
    {
        this.validator.setDocument(getClass().getResourceAsStream("/html5-valid.html"));
        List<ValidationError> errors = this.validator.validate();
        assertTrue(errors.isEmpty());
    }

    @Test
    void invalid()
    {
        this.validator.setDocument(getClass().getResourceAsStream("/html5-invalid.html"));
        List<ValidationError> errors = this.validator.validate();
        assertEquals(errors, this.validator.getErrors());
        assertEquals(6, errors.size());
        this.validator.clear();
        assertTrue(this.validator.getErrors().isEmpty());
    }

    @Test
    void whenException()
    {
        this.validator.setDocument(null);
        assertThrows(RuntimeException.class, () -> this.validator.validate());
    }

    @Test
    void name()
    {
        assertEquals("HTML5", this.validator.getName());
    }
}
