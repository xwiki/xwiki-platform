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

import java.util.Set;

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

    /**
     * @return the list of context entries to take remember for the execution
     */
    public Set<String> getContextEntries()
    {
        return contextEntries;
    }

    /**
     * @param contextEntries the list of context entries to take remember for the execution
     */
    public void setContextEntries(Set<String> contextEntries)
    {
        this.contextEntries = contextEntries;
    }

    /**
     * @return true if the reference of the author have been set
     */
    public boolean isSecureReferenceSet()
    {
        return this.secureDocumentReference != null;
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
    }
}
