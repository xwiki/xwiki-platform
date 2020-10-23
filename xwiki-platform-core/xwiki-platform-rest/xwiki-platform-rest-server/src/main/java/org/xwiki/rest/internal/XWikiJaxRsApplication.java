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
package org.xwiki.rest.internal;

import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.ws.rs.core.Application;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.descriptor.ComponentDescriptor;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.rest.XWikiRestComponent;

/**
 * <p>
 * This class is used to configure JAX-RS resources and providers. They are dynamically discovered using the component
 * manager.
 * </p>
 * <p>
 * JAX-RS resources and providers must be declared as components whose hint is the FQN of the class implementing it.
 * This is needed because of how the Restlet object factory works. When Restlet requests an object it passes to the
 * factory the FQN name of the class to be instantiated. We use this FQN to lookup the component among the ones that are
 * implementing the XWikiRestComponent (marker) interface.
 * </p>
 * 
 * @version $Id$
 */
@Component(roles = Application.class)
@Singleton
public class XWikiJaxRsApplication extends Application
{
    @Inject
    @Named("context")
    private Provider<ComponentManager> componentManagerProvider;

    @Override
    public Set<Class<?>> getClasses()
    {
        Set<Class<?>> jaxRsClasses = new HashSet<>();

        /* Look up all the component that are marked as XWikiRestComponent. */
        List<ComponentDescriptor<XWikiRestComponent>> cds =
            this.componentManagerProvider.get().getComponentDescriptorList((Type) XWikiRestComponent.class);

        for (ComponentDescriptor<XWikiRestComponent> cd : cds) {
            jaxRsClasses.add(cd.getImplementation());
        }

        return jaxRsClasses;
    }
}
