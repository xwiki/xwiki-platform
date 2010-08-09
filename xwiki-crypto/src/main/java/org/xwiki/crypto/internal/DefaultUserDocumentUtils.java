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
package org.xwiki.crypto.internal;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;


/**
 * Default implementation of {@link UserDocumentUtils}.
 * 
 * @version $Id$
 * @since 2.5
 */
@Component
public class DefaultUserDocumentUtils implements UserDocumentUtils
{
    /** DocumentAccessBridge for getting the current user's document and URL. */
    @Requirement
    private DocumentAccessBridge bridge;

    /** Resolver which can make a DocumentReference out of a String. */
    @Requirement(role = String.class)
    private DocumentReferenceResolver<String> resolver;

    /** Serializer to turn a document reference into a String which can be put in a certificate. */
    @Requirement(role = String.class)
    private EntityReferenceSerializer<String> serializer;

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.crypto.internal.UserDocumentUtils#getCurrentUser()
     */
    public String getCurrentUser()
    {
        String localName = this.bridge.getCurrentUser();
        DocumentReference dr = this.resolver.resolve(localName);
        return this.serializer.serialize(dr);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.crypto.internal.UserDocumentUtils#getUserDocURL(java.lang.String)
     */
    public String getUserDocURL(final String userDocName)
    {
        DocumentReference dr = this.resolver.resolve(userDocName);
        return this.bridge.getDocumentURL(dr, "view", "", "", true);
    }
}
