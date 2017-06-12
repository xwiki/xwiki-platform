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
package org.xwiki.component.wiki.internal;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.wiki.WikiComponent;
import org.xwiki.component.wiki.WikiComponentException;
import org.xwiki.component.wiki.WikiComponentManager;
import org.xwiki.model.reference.EntityReference;

/**
 * This component is meant to ease the interaction between the two event listeners that are
 * {@link DefaultWikiComponentManagerEventListener} and {@link DefaultWikiObjectComponentManagerEventListener}
 * and the {@link DefaultWikiComponentManager}.
 *
 * @version $Id$
 * @since 9.5RC1
 */
@Component(roles = WikiComponentManagerEventListenerHelper.class)
@Singleton
public class WikiComponentManagerEventListenerHelper
{
    /**
     * The wiki component manager that knows how to register component definition against the underlying CM.
     */
    @Inject
    private WikiComponentManager wikiComponentManager;

    @Inject
    private Logger logger;

    /**
     * Register every given {@link WikiComponent} against the {@link WikiComponentManager}.
     *
     * @param components a list of components that should be registered
     */
    public void registerComponentList(List<WikiComponent> components)
    {
        for (WikiComponent component : components) {
            // Register the component
            try {
                this.wikiComponentManager.registerWikiComponent(component);
            } catch (WikiComponentException e) {
                this.logger.warn("Unable to register component(s) from document [{}]: {}",
                        component.getDocumentReference(), ExceptionUtils.getRootCauseMessage(e));
            }
        }
    }

    /**
     * Helper method to unregister a wiki component.
     *
     * @param entityReference the reference to the entity for which to unregister the held wiki component.
     */
    public void unregisterComponents(EntityReference entityReference)
    {
        try {
            this.wikiComponentManager.unregisterWikiComponents(entityReference);
        } catch (WikiComponentException e) {
            this.logger.warn("Unable to unregister component(s) from the entity [{}]: {}", entityReference,
                    ExceptionUtils.getRootCauseMessage(e));
        }
    }
}
