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
package org.xwiki.rendering.internal;

import org.xwiki.bridge.DocumentName;
import org.xwiki.bridge.DocumentNameSerializer;
import org.xwiki.component.descriptor.ComponentDescriptor;
import org.xwiki.component.descriptor.DefaultComponentDescriptor;

/**
 * {@link DocumentNameSerializer} mock implementation used for tests.
 * 
 * @version $Id$
 * @since 2.0M1
 */
public class MockDocumentNameSerializer implements DocumentNameSerializer
{
    /**
     * Create and return a descriptor for this component.
     * 
     * @return the descriptor of the component.
     */
    public static ComponentDescriptor<DocumentNameSerializer> getComponentDescriptor()
    {
        DefaultComponentDescriptor<DocumentNameSerializer> descriptor =
            new DefaultComponentDescriptor<DocumentNameSerializer>();
        descriptor.setRole(DocumentNameSerializer.class);
        descriptor.setRoleHint("default");
        descriptor.setImplementation(MockDocumentNameSerializer.class);
        return descriptor;
    }

    /**
     * {@inheritDoc}
     */
    public String serialize(DocumentName name)
    {
        return "Test.Test";
    }
}
