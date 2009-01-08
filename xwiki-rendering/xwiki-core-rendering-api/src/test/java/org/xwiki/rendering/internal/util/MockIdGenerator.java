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
package org.xwiki.rendering.internal.util;

import org.xwiki.component.descriptor.ComponentDescriptor;
import org.xwiki.component.descriptor.DefaultComponentDescriptor;
import org.xwiki.rendering.util.IdGenerator;

/**
 * Default implementation of {@link org.xwiki.rendering.util.IdGenerator}.
 * 
 * @version $Id: $
 */
public class MockIdGenerator implements IdGenerator
{
    /**
     * Create and return a descriptor for this component.
     * 
     * @return the descriptor of the component.
     */
    public static ComponentDescriptor getComponentDescriptor()
    {
        DefaultComponentDescriptor componentDescriptor = new DefaultComponentDescriptor();

        componentDescriptor.setRole(IdGenerator.ROLE);
        componentDescriptor.setRoleHint("default");
        componentDescriptor.setImplementation(MockIdGenerator.class.getName());

        return componentDescriptor;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.util.IdGenerator#generateRandomUniqueId()
     */
    public String generateRandomUniqueId()
    {
        return "testid";
    }
}
