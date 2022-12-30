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
package org.xwiki.rendering.async.internal;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.xwiki.model.reference.DocumentReference;

/**
 * Configuration to pass to {@link AsyncRendererExecutor}.
 * 
 * @version $Id$
 * @since 10.11.1
 * @since 11.0rc1
 */
public class AsyncRendererConfiguration
{
    protected Set<String> contextEntries;

    protected DocumentReference secureAuthorReference;

    protected DocumentReference secureDocumentReference;

    protected boolean secureSet;

    protected boolean placeHolderForced;

    /**
     * @return the list of context entries to take remember for the execution
     */
    public Set<String> getContextEntries()
    {
        return this.contextEntries;
    }

    /**
     * @param contextEntries the context entries needed to execute the content
     */
    public void setContextEntries(Set<String> contextEntries)
    {
        this.contextEntries = contextEntries;
    }

    /**
     * @param contextEntries the context entries needed to execute the content
     * @since 15.0RC1
     * @since 14.10.3
     * @since 13.10.11
     * @since 14.4.8
     */
    public void addContextEntries(Set<String> contextEntries)
    {
        if (CollectionUtils.isNotEmpty(contextEntries)) {
            if (!(this.contextEntries instanceof LinkedHashSet)) {
                this.contextEntries =
                    this.contextEntries == null ? new LinkedHashSet<>() : new LinkedHashSet<>(this.contextEntries);
            }

            this.contextEntries.addAll(contextEntries);
        }
    }

    /**
     * @param contextEntries the context entries needed to execute the content
     * @since 15.0RC1
     * @since 14.10.3
     * @since 13.10.11
     * @since 14.4.8
     */
    public void addContextEntries(String... contextEntries)
    {
        if (ArrayUtils.isNotEmpty(contextEntries)) {
            if (!(this.contextEntries instanceof LinkedHashSet)) {
                this.contextEntries =
                    this.contextEntries == null ? new LinkedHashSet<>() : new LinkedHashSet<>(this.contextEntries);
            }

            Collections.addAll(this.contextEntries, contextEntries);
        }
    }

    /**
     * @return true if the reference of the author have been set
     */
    public boolean isSecureReferenceSet()
    {
        return this.secureSet;
    }

    /**
     * @return the reference of the author of the code
     */
    public DocumentReference getSecureAuthorReference()
    {
        return this.secureAuthorReference;
    }

    /**
     * @return the reference of the document containing the executed block
     */
    public DocumentReference getSecureDocumentReference()
    {
        return this.secureDocumentReference;
    }

    /**
     * @param documentReference the reference of the document containing the executed block
     * @param authorReference the reference of the author of the code
     */
    public void setSecureReference(DocumentReference documentReference, DocumentReference authorReference)
    {
        this.secureDocumentReference = documentReference;
        this.secureAuthorReference = authorReference;
        this.secureSet = true;
    }

    /**
     * @return {@code true} if the renderer should display a placeholder even if the data is already available.
     * @see #setPlaceHolderForced(boolean).
     * @since 12.5RC1
     */
    public boolean isPlaceHolderForced()
    {
        return placeHolderForced;
    }

    /**
     * Set to {@code true} to force the renderer to return an async placeholder even if the data is available. This
     * allows to use easily the Async rendering framework with AJAX requests: we can force the async renderers in an
     * AJAX request to return always placeholders, so they are resolved once added in the current DOM by using
     * Javascript.
     *
     * @param placeHolderForced {@code true} to force using a placeholder.
     * @since 12.5RC1
     */
    public void setPlaceHolderForced(boolean placeHolderForced)
    {
        this.placeHolderForced = placeHolderForced;
    }
}
