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
package org.xwiki.model.reference;

import org.junit.jupiter.api.Test;
import org.xwiki.model.EntityType;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for {@link PageAttachmentReference}.
 *
 * @version $Id$
 */
public class PageAttachmentReferenceTest
{
    @Test
    public void testInvalidType()
    {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
            () -> new PageAttachmentReference(new EntityReference("filename", EntityType.PAGE)));

        assertEquals("Invalid type [PAGE] for an attachment reference", e.getMessage());
    }

    @Test
    public void testInvalidNullParent()
    {
        IllegalArgumentException e =
            assertThrows(IllegalArgumentException.class, () -> new PageAttachmentReference("filename", null));

        assertEquals("Invalid parent reference [null] in an attachment reference", e.getMessage());
    }

    @Test
    public void testInvalidParentType()
    {
        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> new PageAttachmentReference(
            new EntityReference("filename", EntityType.PAGE_ATTACHMENT, new WikiReference("wiki"))));

        assertEquals("Invalid parent reference [Wiki wiki] in an attachment reference", e.getMessage());
    }

    @Test
    public void testReplaceParent()
    {
        PageAttachmentReference reference =
            new PageAttachmentReference("file", new PageReference("wiki", "space", "page"))
                .replaceParent(new PageReference("wiki2", "space2", "page2"));

        assertEquals(new PageAttachmentReference("file", new PageReference("wiki2", "space2", "page2")), reference);

        assertSame(reference, reference.replaceParent(reference.getParent()));
    }
}
