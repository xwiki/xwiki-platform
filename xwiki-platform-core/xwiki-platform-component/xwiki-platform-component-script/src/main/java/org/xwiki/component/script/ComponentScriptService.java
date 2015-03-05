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
package org.xwiki.component.script;

import java.lang.reflect.Type;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.internal.multi.ComponentManagerManager;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.context.Execution;
import org.xwiki.script.service.ScriptService;

/**
 * Provides Component-specific Scripting APIs.
 *
 * @version $Id$
 * @since 4.1M2
 */
@Component
@Named("component")
@Singleton
public class ComponentScriptService implements ScriptService
{
    /**
     * The key under which the last encountered error is stored in the current execution context.
     */
    private static final String ERROR_KEY = "scriptservice.component.error";

    /**
     * The Component Manager that we'll return to the user or use to return component instances to the user. Note that
     * we use a Context Component Manager so that the user gets all components registered in its context.
     */
    @Inject
    @Named("context")
    private Provider<ComponentManager> contextComponentManagerProvider;

    @Inject
    @Named("context/root")
    @Deprecated
    private Provider<ComponentManager> contextrootComponentManagerProvider;

    /**
     * Used to access the component manager corresponding to a specific namespace.
     */
    @Inject
    private ComponentManagerManager componentManagerManager;

    /**
     * Used to check for Programming Rights.
     */
    @Inject
    private DocumentAccessBridge bridge;

    /**
     * Provides access to the current context.
     */
    @Inject
    private Execution execution;

    /**
     * A Component Manager which read in contextual Component Manager and write in root component manager.
     * 
     * @return the Component Manager if the document has Programming Rights or null otherwise
     * @deprecated since 6.4.1, 6.2.6, use {@link #getContextComponentManager()} or
     *             {@link #getContextComponentManager()} instead
     */
    @Deprecated
    public ComponentManager getComponentManager()
    {
        return this.bridge.hasProgrammingRights() ? this.contextrootComponentManagerProvider.get() : null;
    }

    /**
     * @return the contextual Component Manager if the document has Programming Rights or null otherwise
     * @since 6.4.1, 6.2.6
     */
    public ComponentManager getContextComponentManager()
    {
        return this.bridge.hasProgrammingRights() ? this.contextComponentManagerProvider.get() : null;
    }

    /**
     * @return the root Component Manager if the document has Programming Rights or null otherwise
     * @since 6.4.1, 6.2.6
     */
    public ComponentManager getRootComponentManager()
    {
        return getComponentManager(null);
    }

    /**
     * Retrieves the component manager associated with a specific namespace. The namespace is generally of the form
     * <code>prefix:subid</code> where <code>prefix</code> is used to find the proper factory. <code>:</code> can be
     * escaped using <code>\</code> and <code>\</code> need to be escaped as well. There is a namespace for each
     * document, space, wiki and user. E.g. 'wiki:drafts' is the namespace corresponding to the 'drafts' wiki.
     * Namespaces can be nested in which case they inherit from the parent namespace. E.g. the component manager for a
     * specific document has access to the components registered specifically for that document or for any of its
     * namespace ancestors (space, wiki, root). The root (top level) component manager is returned if you pass
     * {@code null}.
     * 
     * @param namespace a namespace or {@code null} for the root {@link ComponentManager}
     * @return the component manager associated with the specified namespace, if any, {@code null otherwise}
     */
    public ComponentManager getComponentManager(String namespace)
    {
        return this.bridge.hasProgrammingRights() ? this.componentManagerManager.getComponentManager(namespace, false)
            : null;
    }

    /**
     * Find a component instance that implements that passed type. If the component has a singleton lifecycle then this
     * method always return the same instance.
     *
     * @param <T> the component role type
     * @param roleType the class (aka role) that the component implements
     * @return the component instance or null if not found
     * @since 4.0RC1 and modified in 6.1M2 to be public (had package visibility)
     */
    public <T> T getInstance(Type roleType)
    {
        T result = null;
        ComponentManager cm = getContextComponentManager();
        if (cm != null) {
            try {
                result = cm.getInstance(roleType);
            } catch (ComponentLookupException e) {
                setError(e);
            }
        }
        return result;
    }

    /**
     * Find a component instance that implements that passed interface class. If the component has a singleton lifecycle
     * then this method always return the same instance.
     *
     * @param <T> the component role type
     * @param roleType the class (aka role) that the component implements
     * @param roleHint the hint that differentiates a component implementation from another one (each component is
     *            registered with a hint; the "default" hint being the default)
     * @return the component instance or null if not found
     * @since 4.0RC1 and modified in 6.1M2 to be public (had package visibility)
     */
    public <T> T getInstance(Type roleType, String roleHint)
    {
        T result = null;
        ComponentManager cm = getContextComponentManager();
        if (cm != null) {
            try {
                result = cm.getInstance(roleType, roleHint);
            } catch (ComponentLookupException e) {
                setError(e);
            }
        }
        return result;
    }

    /**
     * Get the error generated while performing the previously called action.
     *
     * @return the last exception or {@code null} if no exception was thrown
     * @since 6.1M2
     */
    public Exception getLastError()
    {
        return (Exception) this.execution.getContext().getProperty(ERROR_KEY);
    }

    /**
     * Store a caught exception in the context, so that it can be later retrieved using {@link #getLastError()}.
     *
     * @param exception the exception to store, can be {@code null} to clear the previously stored exception
     * @see #getLastError()
     * @since 6.1M2
     */
    private void setError(Exception exception)
    {
        this.execution.getContext().setProperty(ERROR_KEY, exception);
    }

}
