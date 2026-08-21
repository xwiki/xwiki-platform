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
package org.xwiki.rendering.transformation;

import org.junit.jupiter.api.Test;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.ObjectPropertyReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for {@link XWikiTransformationContext}.
 *
 * @version $Id$
 */
class XWikiTransformationContextTest
{
    @Test
    void setAbsoluteContentEntityReference()
    {
        XWikiTransformationContext context = new XWikiTransformationContext();
        DocumentReference reference = new DocumentReference("wiki", "Space", "Page");
        context.setContentEntityReference(reference);
        assertSame(reference, context.getContentEntityReference());
    }

    @Test
    void setAbsoluteObjectPropertyContentEntityReference()
    {
        XWikiTransformationContext context = new XWikiTransformationContext();
        ObjectPropertyReference reference = new ObjectPropertyReference("wiki", "Space", "Page", "MyClass", "content");
        context.setContentEntityReference(reference);
        assertSame(reference, context.getContentEntityReference());
    }

    @Test
    void setNullContentEntityReference()
    {
        XWikiTransformationContext context = new XWikiTransformationContext();
        context.setContentEntityReference(null);
        assertNull(context.getContentEntityReference());
    }

    @Test
    void setRelativeContentEntityReference()
    {
        XWikiTransformationContext context = new XWikiTransformationContext();
        EntityReference reference = new EntityReference("Page", EntityType.DOCUMENT,
            new EntityReference("Space", EntityType.SPACE));
        IllegalArgumentException exception =
            assertThrows(IllegalArgumentException.class, () -> context.setContentEntityReference(reference));
        assertEquals("The content entity reference [Document Space.Page] must be absolute (i.e. rooted at a wiki "
            + "reference).", exception.getMessage());
        assertNull(context.getContentEntityReference());
    }
}
