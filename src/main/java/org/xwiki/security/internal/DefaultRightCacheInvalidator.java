/*
 * Copyright 2010 Andreas Jonsson
 * 
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
 *
 */
package org.xwiki.security.internal;

import org.xwiki.security.RightCache;
import org.xwiki.security.RightCacheKey;
import org.xwiki.security.RightServiceException;

import org.xwiki.model.reference.DocumentReference;

import org.xwiki.observation.EventListener;
import org.xwiki.observation.event.Event;
import org.xwiki.observation.event.DocumentDeleteEvent;
import org.xwiki.observation.event.DocumentUpdateEvent;
import org.xwiki.observation.event.DocumentSaveEvent;

import java.util.List;
import java.util.Arrays;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * The instance of this class monitors updates and invalidates right
 * cache entries whenever necessary.
 * @version $Id: $
 */
@Component
public class DefaultRightCacheInvalidator implements RightCacheInvalidator, EventListener
{
    /**
     * The logging tool.
     */
    private static final Log LOG = LogFactory.getLog(DefaultRightLoader.class);

    /** The right cache. */
    @Requirement private RightCache rightCache;

    /**
     * We use a fair read-write lock to suspend the delivery of
     * document update events while there are loads in progress.
     */
    private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock(true);

    @Override
    public void suspend()
    {
        readWriteLock.readLock().lock();
    }

    @Override
    public void resume()
    {
        readWriteLock.readLock().unlock();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.observation.EventListener#getName()
     */
    public String getName()
    {
        return getClass().getName();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.observation.EventListener#getEvents()
     */
    public List<Event> getEvents()
    {
        Event[] events = {
            new DocumentUpdateEvent(),
            new DocumentDeleteEvent(),
            new DocumentSaveEvent()
        };
        return Arrays.asList(events);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.observation.EventListener#onEvent(org.xwiki.observation.event.Event, java.lang.Object,
     *      java.lang.Object)
     */
    public void onEvent(Event event, Object source, Object data)
    {
        DocumentReference ref = XWikiUtils.getDocumentReference(source);
        readWriteLock.writeLock().lock();
        try {
            deliverUpdateEvent(ref);
            if (XWikiUtils.isGroupDocument(source)) {
                XWikiUtils.invalidateGroupMembers(ref, rightCache);
            }
        } catch (RightServiceException e) {
            LOG.error("Failed to invalidate group members on the document: " + ref, e);
        } finally {
            readWriteLock.writeLock().unlock();
        }
    }

    /**
     * Describe <code>deliverUpdateEvent</code> method here.
     *
     * @param ref Reference to the document that should be
     * invalidated.
     */
    private void deliverUpdateEvent(DocumentReference ref)
    {
        if (ref.getName().equals(XWikiUtils.WIKI_DOC)
            && ref.getParent().getName().equals(XWikiUtils.WIKI_SPACE)) {
            RightCacheKey wikiKey = rightCache.getRightCacheKey(ref.getWikiReference());
            rightCache.remove(wikiKey);
        } else if (ref.getName().equals(XWikiUtils.SPACE_DOC)) {
            RightCacheKey spaceKey = rightCache.getRightCacheKey(ref.getParent());
            rightCache.remove(spaceKey);
        } else {
            RightCacheKey key = rightCache.getRightCacheKey(ref);
            rightCache.remove(key);
        }
    }
}