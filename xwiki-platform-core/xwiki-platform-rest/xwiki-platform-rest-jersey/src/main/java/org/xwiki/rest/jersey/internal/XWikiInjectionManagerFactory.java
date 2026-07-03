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
package org.xwiki.rest.jersey.internal;

import javax.annotation.Priority;

import org.glassfish.jersey.inject.hk2.Hk2InjectionManagerFactory;
import org.glassfish.jersey.internal.inject.InjectionManager;
import org.glassfish.jersey.internal.inject.InjectionManagerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;

/**
 * Replace the default {@link InjectionManagerFactory} by one able to create XWiki components.
 * 
 * @version $Id$
 * @since 16.2.0RC1
 */
@Priority(100)
public class XWikiInjectionManagerFactory implements InjectionManagerFactory
{
    protected static final Logger LOGGER = LoggerFactory.getLogger(XWikiInjectionManagerFactory.class);

    @Override
    public InjectionManager create(Object parent)
    {
        InjectionManager injectionManager;
        if (parent instanceof ComponentManager componentManager) {
            try {
                injectionManager = componentManager.getInstance(XWikiInjectionManager.class);
            } catch (ComponentLookupException e) {
                throw new RuntimeException("Failed to lookup XWikiInjectionManager", e);
            }
        } else {
            // Not XWiki use case
            injectionManager = new Hk2InjectionManagerFactory().create(parent);
        }

        return injectionManager;
    }
}
