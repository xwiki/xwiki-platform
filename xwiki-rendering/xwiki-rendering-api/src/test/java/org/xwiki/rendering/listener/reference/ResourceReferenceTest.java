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
package org.xwiki.rendering.listener.reference;

import org.junit.*;

/**
 * Unit tests for {@link ResourceReference}.
 *
 * @version $Id$
 * @since 3.0M2
 */
public class ResourceReferenceTest
{
    @Test
    public void testEquals()
    {
        ResourceReference reference1 = new ResourceReference("reference", ResourceType.DOCUMENT);
        ResourceReference reference2 = new ResourceReference("reference", ResourceType.DOCUMENT);
        Assert.assertEquals(reference1, reference2);

        reference2.addBaseReference("base");
        Assert.assertFalse(reference1.equals(reference2));

        reference1.addBaseReference("base");
        Assert.assertEquals(reference1, reference2);
    }

    @Test
    public void testHashCode()
    {
        ResourceReference reference1 = new ResourceReference("reference", ResourceType.DOCUMENT);
        ResourceReference reference2 = new ResourceReference("reference", ResourceType.DOCUMENT);
        Assert.assertEquals(reference1.hashCode(), reference2.hashCode());

        reference1.addBaseReference("base");
        reference2.addBaseReference("base");
        Assert.assertEquals(reference1.hashCode(), reference2.hashCode());
    }
}
