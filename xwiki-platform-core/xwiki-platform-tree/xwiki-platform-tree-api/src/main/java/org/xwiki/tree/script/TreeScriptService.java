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
package org.xwiki.tree.script;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.script.service.ScriptService;
import org.xwiki.stability.Unstable;
import org.xwiki.tree.Tree;

/**
 * Exposes the tree API in server-side scripts.
 * 
 * @version $Id$
 * @since 8.3M2, 7.4.5
 */
@Component
@Named("tree")
@Singleton
@Unstable
public class TreeScriptService implements ScriptService
{
    @Inject
    private Logger logger;

    @Inject
    @Named("context")
    private Provider<ComponentManager> contextComponentManagerProvider;

    /**
     * @param roleHint the {@link Tree} role hint
     * @return the {@link Tree} component implementation with the specified hint
     */
    public Tree get(String roleHint)
    {
        ComponentManager contextComponentManager = this.contextComponentManagerProvider.get();
        if (contextComponentManager.hasComponent(Tree.class, roleHint)) {
            try {
                return contextComponentManager.getInstance(Tree.class, roleHint);
            } catch (ComponentLookupException e) {
                this.logger.warn("Failed to load the specified tree component. Root cause is [{}]",
                    ExceptionUtils.getRootCauseMessage(e));
            }
        }
        return null;
    }
}
