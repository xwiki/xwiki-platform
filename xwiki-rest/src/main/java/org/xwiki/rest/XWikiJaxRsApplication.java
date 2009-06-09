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
package org.xwiki.rest;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;

import javax.ws.rs.core.Application;

import org.restlet.Context;
import org.xwiki.component.descriptor.ComponentDescriptor;
import org.xwiki.component.manager.ComponentManager;

/**
 * This class is used to configure JAX-RS resources and providers. They are dynamically discovered using the component
 * manager.
 * 
 * @version $Id$
 */
public class XWikiJaxRsApplication extends Application
{
    private Set<Class< ? >> jaxRsClasses;

    public XWikiJaxRsApplication(Context context)
    {
        this.jaxRsClasses = new HashSet<Class< ? >>();

        ComponentManager componentManager =
            (ComponentManager) context.getAttributes().get(Constants.XWIKI_COMPONENT_MANAGER);
        List<ComponentDescriptor<XWikiRestComponent>> cds = 
            componentManager.getComponentDescriptorList(XWikiRestComponent.class);

        for (ComponentDescriptor<XWikiRestComponent> cd : cds) {
            this.jaxRsClasses.add(cd.getImplementation());
            context.getLogger().log(Level.FINE, String.format("%s registered.", cd.getImplementation().getName()));
        }

        context.getLogger().log(Level.INFO, "RESTful API subsystem initialized.");
    }

    @Override
    public Set<Class< ? >> getClasses()
    {
        return jaxRsClasses;
    }

}
