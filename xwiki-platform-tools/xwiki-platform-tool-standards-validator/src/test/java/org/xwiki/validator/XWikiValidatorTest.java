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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test class for {@link org.xwiki.validator.XWikiValidator}.
 *
 * @version $Id$
 * @since 6.4M3
 */
class XWikiValidatorTest
{
    private XWikiValidator validator;

    @BeforeEach
    void beforeEach()
    {
        this.validator = new XWikiValidator();
    }

    private void validate(InputStream document)
    {
        this.validator.setDocument(document);
        this.validator.validate();
    }

    // Tests

    @Test
    void testValid()
    {
        validate(getClass().getResourceAsStream("/xwiki-valid.html"));
        List<ValidationError> errors = this.validator.getErrors();
        assertTrue(errors.isEmpty());
    }

    @Test
    void testInvalid()
    {
        validate(getClass().getResourceAsStream("/xwiki-invalid.html"));
        List<ValidationError> errors = this.validator.validate();
        assertEquals(errors, this.validator.getErrors());
        assertEquals(3, errors.size());
        assertEquals("Found rendering error", errors.get(0).getMessage());
        assertEquals(ValidationError.Type.ERROR, errors.get(0).getType());
        assertEquals("ERROR: Found rendering error", errors.get(0).toString());
        assertEquals("A h1 heading with empty id (\"H\") has been found", errors.get(1).getMessage());
        assertEquals("A h2 heading with empty id (\"H\") has been found", errors.get(2).getMessage());
        this.validator.clear();
        assertTrue(this.validator.getErrors().isEmpty());
    }

    @Test
    void validateURLsWithSeveralQueryStringDelimiters()
    {
        validate(new ByteArrayInputStream(("<html><body><a href=\"/xwiki/bin/view/A/B?x=1?y=2\">link</a>"
            + "<img src=\"/image.png?a=1?b=2\" alt=\"\"/></body></html>").getBytes(StandardCharsets.UTF_8)));
        List<ValidationError> errors = this.validator.getErrors();
        assertEquals(2, errors.size());
        assertEquals("A link contain two query parameter delimiter '?' (url: /xwiki/bin/view/A/B?x=1?y=2)",
            errors.get(0).getMessage());
        assertEquals("A resource img contain two query parameter delimiter '?' (url: /image.png?a=1?b=2)",
            errors.get(1).getMessage());
    }

    @Test
    void validateWithoutDocument()
    {
        validate(null);
        assertTrue(this.validator.getErrors().isEmpty());
    }
}
