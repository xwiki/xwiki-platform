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
package com.xpn.xwiki.test;

import com.xpn.xwiki.CoreConfiguration;
import org.xwiki.component.descriptor.ComponentDescriptor;
import org.xwiki.component.descriptor.DefaultComponentDescriptor;

public class TestCoreConfiguration implements CoreConfiguration
{
    /**
     * Create and return a descriptor for this component.
     * 
     * @return the descriptor of the component.
     */
    public static ComponentDescriptor<CoreConfiguration> getComponentDescriptor()
    {
        DefaultComponentDescriptor<CoreConfiguration> componentDescriptor = 
            new DefaultComponentDescriptor<CoreConfiguration>();

        componentDescriptor.setRole(CoreConfiguration.class);
        componentDescriptor.setImplementation(TestCoreConfiguration.class);

        return componentDescriptor;
    }

    public String getDefaultDocumentSyntax()
    {
        return "xwiki/1.0";
    }
}
