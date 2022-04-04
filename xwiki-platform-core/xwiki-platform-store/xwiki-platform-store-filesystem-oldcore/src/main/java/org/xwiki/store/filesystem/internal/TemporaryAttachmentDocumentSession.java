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

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.xwiki.cache.Cache;
import org.xwiki.cache.event.CacheEntryEvent;
import org.xwiki.cache.event.CacheEntryListener;
import org.xwiki.model.reference.DocumentReference;

import com.xpn.xwiki.doc.XWikiAttachment;

/**
 * This class contains the attachment cache for a specific user edition session on a given document.
 *
 * @version $Id$
 * @since 14.3RC1
 */
public class TemporaryAttachmentDocumentSession
{
    private final String sessionId;

    private final DocumentReference documentReference;

    private final Cache<XWikiAttachment> attachmentCache;

    private final Set<String> filenames;

    /**
     * Default constructor, which is also responsible for initializing the cache listener for keeping the set of
     * document filenames synchronized.
     *
     * @param sessionId the identifier of the user session
     * @param documentReference the reference of the document being edited
     * @param attachmentCache the actual new cache that will be used for this edition session
     */
    public TemporaryAttachmentDocumentSession(String sessionId, DocumentReference documentReference,
        Cache<XWikiAttachment> attachmentCache)
    {
        this.sessionId = sessionId;
        this.documentReference = documentReference;
        this.attachmentCache = attachmentCache;
        this.filenames = ConcurrentHashMap.newKeySet();
        this.initCacheListener();
    }

    /**
     * Initialize the cache entry listener responsible of keeping the list of filenames synchronized.
     */
    private void initCacheListener()
    {
        this.attachmentCache.addCacheEntryListener(new CacheEntryListener<XWikiAttachment>()
        {
            @Override
            public void cacheEntryAdded(CacheEntryEvent<XWikiAttachment> event)
            {
                TemporaryAttachmentDocumentSession.this.filenames.add(event.getEntry().getKey());
            }

            @Override
            public void cacheEntryRemoved(CacheEntryEvent<XWikiAttachment> event)
            {
                TemporaryAttachmentDocumentSession.this.filenames.remove(event.getEntry().getKey());
            }

            @Override
            public void cacheEntryModified(CacheEntryEvent<XWikiAttachment> event)
            {
                // Nothing to do.
            }
        });
    }

    /**
     * @return the session identifier of the current user session.
     */
    public String getSessionId()
    {
        return sessionId;
    }

    /**
     * @return the attachment cache.
     */
    public Cache<XWikiAttachment> getAttachmentCache()
    {
        return attachmentCache;
    }

    /**
     * @return the set of filenames of cached attachments.
     */
    public Set<String> getFilenames()
    {
        return new HashSet<>(filenames);
    }

    /**
     * @return the document reference of the editing session.
     */
    public DocumentReference getDocumentReference()
    {
        return documentReference;
    }

    /**
     * Close the cache and clear the set of filenames.
     */
    public void dispose()
    {
        this.attachmentCache.dispose();
        this.filenames.clear();
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

        TemporaryAttachmentDocumentSession that = (TemporaryAttachmentDocumentSession) o;

        return new EqualsBuilder()
            .append(sessionId, that.sessionId)
            .append(documentReference, that.documentReference)
            .isEquals();
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder(65, 37)
            .append(sessionId)
            .append(documentReference)
            .toHashCode();
    }
}
