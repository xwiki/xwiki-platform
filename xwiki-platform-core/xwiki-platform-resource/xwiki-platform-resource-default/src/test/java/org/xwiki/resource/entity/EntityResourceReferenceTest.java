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
package org.xwiki.resource.entity;

import java.util.Collections;
import java.util.Locale;

import org.junit.*;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link org.xwiki.resource.entity.EntityResourceReference}.
 *
 * @version $Id$
 * @since 6.1M2
 */
public class EntityResourceReferenceTest
{
    @Test
    public void creation()
    {
        EntityReference reference = new DocumentReference("wiki", "space", "page");
        EntityResourceReference resource = new EntityResourceReference(reference, EntityResourceAction.VIEW);
        assertEquals(EntityResourceAction.VIEW, resource.getAction());
        assertEquals(reference, resource.getEntityReference());
        assertEquals(Collections.EMPTY_MAP, resource.getParameters());
        assertNull(resource.getLocale());

        resource.addParameter("param1", "value1");
        assertEquals("value1", resource.getParameterValue("param1"));

        resource.setLocale(Locale.ROOT);
        assertEquals(Locale.ROOT, resource.getLocale());

        resource.setRevision("1.0");
        assertEquals("1.0", resource.getRevision());
    }
}
