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

import java.util.ArrayList;
import java.util.List;

import org.restlet.ext.jaxrs.InstantiateException;
import org.restlet.ext.jaxrs.ObjectFactory;
import org.xwiki.component.descriptor.ComponentDescriptor;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;

/**
 * This class is used to provide Restlet/JAX-RS a way to instantiate components. Instances requested through this
 * factory are created using the XWiki component manager. A special list stored in the execution context is used in
 * order to keep track of all the allocated components that have a "per-lookup" policy. This is needed in order to
 * ensure proper release of those instances at the end of the request and to avoid memory leaks.
 * 
 * @version $Id$
 */
public class ComponentsObjectFactory implements ObjectFactory
{
    /**
     * The component manager used to lookup and instantiate components.
     */
    private ComponentManager componentManager;

    /**
     * Constructor
     * 
     * @param componentManager The component manager to be used.
     */
    public ComponentsObjectFactory(ComponentManager componentManager)
    {
        this.componentManager = componentManager;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T getInstance(Class<T> clazz) throws InstantiateException
    {
        try {
            // Use the component manager to lookup the class. This ensure that injections are properly executed.
            XWikiRestComponent component = componentManager.getInstance(XWikiRestComponent.class, clazz.getName());

            // JAX-RS resources and providers must be declared as components whose hint is the FQN of the class
            // implementing it. This is needed because of they are looked up using the FQN as the hint.
            ComponentDescriptor<XWikiRestComponent> componentDescriptor =
                componentManager.getComponentDescriptor(XWikiRestComponent.class, clazz.getName());

            // Retrieve the list of releasable components from the execution context. This is used to store component
            // instances that need to be released at the end of the request.
            ExecutionContext executionContext = componentManager.<Execution> getInstance(Execution.class).getContext();
            List<XWikiRestComponent> releasableComponentReferences =
                (List<XWikiRestComponent>) executionContext.getProperty(Constants.RELEASABLE_COMPONENT_REFERENCES);
            if (releasableComponentReferences == null) {
                releasableComponentReferences = new ArrayList<XWikiRestComponent>();
                executionContext.setProperty(Constants.RELEASABLE_COMPONENT_REFERENCES, releasableComponentReferences);
            }

            // Only add the components that have a per-lookup instantiation strategy.
            if (componentDescriptor.getInstantiationStrategy() == ComponentInstantiationStrategy.PER_LOOKUP) {
                releasableComponentReferences.add(component);
            }

            // Return the instantiated component. This cast should never fail if the programmer has correctly set the
            // component hint to the actual fully qualified name of the Java class.
            return (T) component;
        } catch (ComponentLookupException e) {
            throw new InstantiateException(e);
        }
    }
}
