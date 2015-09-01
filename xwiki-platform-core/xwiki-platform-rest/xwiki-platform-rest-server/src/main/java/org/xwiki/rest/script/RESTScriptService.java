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
package org.xwiki.rest.script;

import java.net.URL;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.rest.XWikiRestException;
import org.xwiki.rest.url.RestURLGenerator;
import org.xwiki.script.service.ScriptService;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

/**
 * Generate REST URLs for different types of resources.
 * 
 * @version $Id$
 * @since 7.2M1
 */
@Component
@Named("rest")
@Singleton
public class RESTScriptService implements ScriptService
{
    /**
     * The key under which the last encountered error is stored in the current execution context.
     */
    private static final String ERROR_KEY = "scriptservice.rest.error";

    @Inject
    private RestURLGenerator restURLGenerator;

    @Inject
    private Execution execution;

    @Inject
    private WikiDescriptorManager wikis;

    /**
     * Create an REST URL representing the passed entity.
     * <p>
     * The URL path will be returned unless the reference wiki is different from the current wiki.
     * 
     * @param reference an entity reference
     * @return the REST URL corresponding to the referenced entity, or null if an error occurs
     * @since 7.2M3
     */
    public String url(EntityReference reference)
    {
        return url(reference, false);
    }

    /**
     * Create an REST URL representing the passed entity.
     * <p>
     * If <code>external</code> is false the URL path will be returned unless the reference wiki is different from the
     * current wiki.
     * 
     * @param reference an entity reference
     * @param external indicate if the returned URL should be absolute (to be used by an external service usually)
     * @return the REST URL corresponding to the referenced entity, or null if an error occurs
     * @since 7.2M3
     */
    public String url(EntityReference reference, boolean external)
    {
        EntityReference wikiReference = reference.extractReference(EntityType.WIKI);

        try {
            URL externalURL = this.restURLGenerator.getURL(reference);

            // If forced or the reference wiki is different from the current wiki return external form URL
            if (external || (wikiReference != null && !wikiReference.getName().equals(this.wikis.getCurrentWikiId()))) {
                return externalURL.toExternalForm();
            }

            return externalURL.getPath();
        } catch (XWikiRestException e) {
            setLastError(e);
            return null;
        }
    }

    /**
     * Get the error generated while performing the previously called action.
     * 
     * @return an eventual exception or {@code null} if no exception was thrown
     */
    public XWikiRestException getLastError()
    {
        return (XWikiRestException) this.execution.getContext().getProperty(ERROR_KEY);
    }

    /**
     * Store a caught exception in the context, so that it can be later retrieved using {@link #getLastError()}.
     *
     * @param e the exception to store, can be {@code null} to clear the previously stored exception
     * @see #getLastError()
     */
    private void setLastError(XWikiRestException e)
    {
        this.execution.getContext().setProperty(ERROR_KEY, e);
    }
}
