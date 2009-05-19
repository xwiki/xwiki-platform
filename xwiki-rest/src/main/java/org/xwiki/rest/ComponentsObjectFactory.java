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
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentDescriptor;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;

/**
 * This class is used to provide Restlet/JAX-RS a way to instantiate components. Instances requested through this
 * factory are created using the XWiki component manager. A special list stored in the current Restlet context is used
 * in order to keep track of all the allocated components that have a "per-lookup" policy. This is needed in order to
 * ensure proper release of those instances and to avoid memory leaks.
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

    /*
     * (non-Javadoc)
     * @see org.restlet.ext.jaxrs.ObjectFactory#getInstance(java.lang.Class)
     */
    public <T> T getInstance(Class<T> clazz) throws InstantiateException
    {
        try {
            /* Use the component manager to lookup the class. This ensure that injections are properly executed */
            XWikiRestComponent component =
                (XWikiRestComponent) componentManager.lookup(XWikiRestComponent.class, clazz.getName());
            
            ComponentDescriptor componentDescriptor = componentManager.getComponentDescriptor(XWikiRestComponent.class, clazz.getName());            
            
            /*
             * Retrieve the releasable component list from the context. This is used to store component instances that
             * need to be released later.
             */
            List<XWikiRestComponent> releasableComponentReferences =
                (List<XWikiRestComponent>) org.restlet.Context.getCurrent().getAttributes().get(
                    Constants.RELEASABLE_COMPONENT_REFERENCES);
            if (releasableComponentReferences == null) {
                releasableComponentReferences = new ArrayList<XWikiRestComponent>();
                org.restlet.Context.getCurrent().getAttributes().put(Constants.RELEASABLE_COMPONENT_REFERENCES,
                    releasableComponentReferences);
            }

            /* Only add components that have a per-lookup instantiation stategy. */
            if(componentDescriptor.getInstantiationStrategy().equals(ComponentInstantiationStrategy.PER_LOOKUP)) {                
                releasableComponentReferences.add(component);
            }
                      
            /*
             * Return the instantiated component. This cast should never fail is the programmer has correctly set the
             * component hint to the actualy fully qualified name of the Java class.
             */
            return (T) component;
        } catch (ComponentLookupException e) {
            throw new InstantiateException(e);
        }
    }

}
