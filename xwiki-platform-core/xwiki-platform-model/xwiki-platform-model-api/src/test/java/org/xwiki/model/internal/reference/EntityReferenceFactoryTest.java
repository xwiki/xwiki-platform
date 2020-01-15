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
package org.xwiki.model.internal.reference;

import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.PageReference;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Validate {@link EntityReferenceFactory}.
 * 
 * @version $Id$
 */
@ComponentTest
public class EntityReferenceFactoryTest
{
    @InjectMockComponents
    private EntityReferenceFactory factory;

    @Test
    public void getReferenceWhenNull()
    {
        assertNull(this.factory.getReference(null));
    }

    @Test
    public void getReference()
    {
        PageReference page = new PageReference("wiki", "parent", "page");
        PageReference pageClone = new PageReference("wiki", "parent", "page");

        assertSame(page, this.factory.getReference(page));
        assertSame(page, this.factory.getReference(pageClone));

        // Make sure the parent is cached too
        assertSame(page.getParent(), this.factory.getReference(pageClone.getParent()));

        PageReference page2 = new PageReference("wiki", "parent", "page2");

        PageReference page2Cached = this.factory.getReference(page2);
        assertEquals(page2, page2Cached);
        assertNotSame(page, page2Cached);
        assertSame(page.getParent(), page2Cached.getParent());
    }

    @Test
    public void getReferenceWhenExistingIsLowerType()
    {
        PageReference page = new PageReference("wiki", "parent", "page");
        EntityReference entity = new EntityReference(page);

        assertSame(entity, this.factory.getReference(entity));
        assertSame(page, this.factory.getReference(page));
        assertEquals(entity, this.factory.getReference(entity));
        assertNotSame(entity, this.factory.getReference(entity));
    }
}
