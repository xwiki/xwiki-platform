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
package org.xwiki.store.filesystem.internal;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.xwiki.cache.Cache;
import org.xwiki.model.reference.DocumentReference;

import com.xpn.xwiki.doc.XWikiAttachment;

/**
 * Contains all information for a user editing session when performing temporary attachment uploads.
 * This class manipulates a map of {@link TemporaryAttachmentDocumentSession} since a user might edit several document
 * at once.
 *
 * @see DefaultTemporaryAttachmentManager
 * @see TemporaryAttachmentDocumentSession
 * @version $Id$
 * @since 14.3RC1
 */
public class TemporaryAttachmentSession
{
    private final String sessionId;

    private final Map<DocumentReference, TemporaryAttachmentDocumentSession> temporaryAttachmentDocumentSessionMap;

    /**
     * Default constructor.
     *
     * @param sessionId the identifier of the session.
     */
    public TemporaryAttachmentSession(String sessionId)
    {
        this.sessionId = sessionId;
        this.temporaryAttachmentDocumentSessionMap = new ConcurrentHashMap<>();
    }

    /**
     * Accessor of the temporary attachment document session map for testing purpose.
     *
     * @return the temporary attachment document session map.
     */
    protected Map<DocumentReference, TemporaryAttachmentDocumentSession> getTemporaryAttachmentDocumentSessionMap()
    {
        return temporaryAttachmentDocumentSessionMap;
    }

    /**
     * Check if there is already a cache created for the given document reference.
     *
     * @param documentReference the reference for which to cache temporary attachment.
     * @return {@code true} if a cache already exists.
     */
    public boolean hasOpenEditionSession(DocumentReference documentReference)
    {
        return this.temporaryAttachmentDocumentSessionMap.containsKey(documentReference);
    }

    /**
     * Add a new cache for the given document reference.
     * This method also creates the appropriate listener for the cache to handle set of file names.
     *
     * @param documentReference the reference of the document for which the cache is created.
     * @param cache a newly created cache to be used with the given document reference.
     */
    public void startEditionSession(DocumentReference documentReference, Cache<XWikiAttachment> cache)
    {
        TemporaryAttachmentDocumentSession temporaryAttachmentDocumentSession =
            new TemporaryAttachmentDocumentSession(this.sessionId, documentReference, cache);
        this.temporaryAttachmentDocumentSessionMap.put(documentReference, temporaryAttachmentDocumentSession);
    }

    /**
     * Dispose all caches created for this session.
     */
    public void dispose()
    {
        this.temporaryAttachmentDocumentSessionMap.values().forEach(TemporaryAttachmentDocumentSession::dispose);
    }

    /**
     * Retrieve the cache created for the given document reference.
     *
     * @param documentReference the reference for which to retrieve a cache.
     * @return the cache associated to the given reference, or {@code null} if no cache exists for the given reference
     * @see #hasOpenEditionSession(DocumentReference)
     */
    public Cache<XWikiAttachment> getCache(DocumentReference documentReference)
    {
        if (this.hasOpenEditionSession(documentReference)) {
            return this.temporaryAttachmentDocumentSessionMap.get(documentReference).getAttachmentCache();
        } else {
            return null;
        }
    }

    /**
     * Retrieve the set of filenames of uploaded attachments for the given document reference.
     *
     * @param documentReference the reference for which to retrieve the set of filenames.
     * @return the set of filenames of uploaded attachments, or {@link Collections#emptySet()} if there's no session
     *          opened for the given reference.
     * @see #hasOpenEditionSession(DocumentReference)
     */
    public Set<String> getFilenames(DocumentReference documentReference)
    {
        if (this.hasOpenEditionSession(documentReference)) {
            return this.temporaryAttachmentDocumentSessionMap.get(documentReference).getFilenames();
        } else {
            return Collections.emptySet();
        }
    }

    /**
     * @return the session identifier.
     */
    public String getSessionId()
    {
        return sessionId;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) {
            return true;
        }

        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        TemporaryAttachmentSession that = (TemporaryAttachmentSession) o;

        return new EqualsBuilder().append(sessionId, that.sessionId).isEquals();
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder(17, 63).append(sessionId).toHashCode();
    }
}
