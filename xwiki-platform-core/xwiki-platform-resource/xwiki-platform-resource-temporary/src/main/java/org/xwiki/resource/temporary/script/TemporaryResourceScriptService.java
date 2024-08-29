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
package org.xwiki.resource.temporary.script;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;
import org.xwiki.resource.ResourceReference;
import org.xwiki.resource.ResourceReferenceSerializer;
import org.xwiki.resource.script.ResourceScriptService;
import org.xwiki.resource.temporary.TemporaryResourceReference;
import org.xwiki.resource.temporary.TemporaryResourceStore;
import org.xwiki.script.service.ScriptService;
import org.xwiki.url.ExtendedURL;

/**
 * Exposes the Temporary Resource API to server-side scripts.
 * 
 * @version $Id$
 * @since 14.4.2
 * @since 14.5
 */
@Component
@Named(TemporaryResourceScriptService.ROLE_HINT)
@Singleton
public class TemporaryResourceScriptService implements ScriptService
{
    /**
     * The role hint of this component.
     */
    public static final String ROLE_HINT = ResourceScriptService.ROLE_HINT + ".temporary";

    /**
     * The key under which the last encountered error is stored in the current execution context.
     */
    private static final String TEMP_RESOURCE_ERROR_KEY = String.format("scriptservice.%s.error", ROLE_HINT);

    /**
     * Used to obtain the URL that corresponds to a temporary resource.
     */
    @Inject
    private ResourceReferenceSerializer<ResourceReference, ExtendedURL> resourceReferenceSerializer;
    
    @Inject
    private TemporaryResourceStore temporaryResourceStore;

    /**
     * Provides access to the current context, used to store and retrieve caught exceptions.
     */
    @Inject
    private Execution execution;

    /**
     * @param tempResourceReference a temporary resource reference
     * @return the URL corresponding to the specified temporary resource
     */
    public String getURL(TemporaryResourceReference tempResourceReference)
    {
        setError(null);

        try {
            return this.resourceReferenceSerializer.serialize(tempResourceReference).serialize();
        } catch (Exception e) {
            setError(e);
            return null;
        }
    }

    /**
     * Check if a temporary resource exists.
     * 
     * @param tempResourceReference a temporary resource reference
     * @return {@code true} if the specified temporary resource exists, {@code false} otherwise
     */
    public boolean exists(TemporaryResourceReference tempResourceReference)
    {
        try {
            return this.temporaryResourceStore.getTemporaryFile(tempResourceReference).exists();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Get the error generated while performing the previously called action.
     *
     * @return an eventual exception or {@code null} if no exception was thrown
     */
    public Exception getLastError()
    {
        return (Exception) this.execution.getContext().getProperty(TEMP_RESOURCE_ERROR_KEY);
    }

    /**
     * Store a caught exception in the context, so that it can be later retrieved using {@link #getLastError()}.
     *
     * @param e the exception to store, can be {@code null} to clear the previously stored exception
     * @see #getLastError()
     */
    private void setError(Exception e)
    {
        this.execution.getContext().setProperty(TEMP_RESOURCE_ERROR_KEY, e);
    }
}
