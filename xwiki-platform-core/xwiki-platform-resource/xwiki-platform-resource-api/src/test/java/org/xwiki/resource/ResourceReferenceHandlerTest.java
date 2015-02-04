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
package org.xwiki.resource;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link ResourceReferenceHandler}.
 *
 * @version $Id$
 * @since 6.1M2
 */
public class ResourceReferenceHandlerTest
{
    private class TestableResourceReferenceHandler extends AbstractResourceReferenceHandler<ResourceType>
    {
        public TestableResourceReferenceHandler(int priority)
        {
            setPriority(priority);
        }

        @Override
        public List<ResourceType> getSupportedResourceReferences()
        {
            return Arrays.asList(new ResourceType("test"));
        }

        @Override
        public void handle(ResourceReference reference, ResourceReferenceHandlerChain chain) throws
            ResourceReferenceHandlerException
        {
        }
    };

    @Test
    public void priority()
    {
        ResourceReferenceHandler handler1 = new TestableResourceReferenceHandler(500);
        assertEquals(500, handler1.getPriority());
        assertTrue(handler1.getSupportedResourceReferences().contains(new ResourceType("test")));

        ResourceReferenceHandler handler2 = new TestableResourceReferenceHandler(200);
        assertEquals(300, handler1.compareTo(handler2));
    }
}
