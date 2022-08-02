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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class RSSValidatorTest
{
    private RSSValidator validator;

    @BeforeEach
    public void setUp() throws Exception
    {
        this.validator = new RSSValidator();
    }

    @Test
    public void testValidRSS() throws Exception
    {
        this.validator.setDocument(getClass().getResourceAsStream("/rss-valid.xml"));
        this.validator.validate();

        assertTrue(this.validator.getErrors().isEmpty(), this.validator.getErrors().toString());
    }

    @Test
    public void testInvalidRSS() throws Exception
    {
        this.validator.setDocument(getClass().getResourceAsStream("/rss-invalid.xml"));
        this.validator.validate();

        assertEquals(1, this.validator.getErrors().size());
        assertEquals("Invalid XML", this.validator.getErrors().get(0).getMessage());
    }
}
