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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;
import javax.servlet.http.Part;

import org.xwiki.cache.Cache;
import org.xwiki.cache.CacheException;
import org.xwiki.cache.CacheManager;
import org.xwiki.cache.config.LRUCacheConfiguration;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLifecycleException;
import org.xwiki.component.phase.Disposable;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.store.TemporaryAttachmentException;
import org.xwiki.store.TemporaryAttachmentManager;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiAttachment;

/**
 * Default implementation of {@link TemporaryAttachmentManager}.
 * Note that this component also implements {@link HttpSessionListener} so that the cache is properly clean up whenever
 * a session is destroyed.
 *
 * @version $Id$
 * @since 14.3RC1
 */
@Component
@Singleton
public class DefaultTemporaryAttachmentManager implements TemporaryAttachmentManager, Initializable, Disposable,
    HttpSessionListener
{
    private Map<String, TemporaryAttachmentSession> temporaryAttachmentSessionMap;

    @Inject
    private CacheManager cacheManager;

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    private EntityReferenceSerializer<String> stringEntityReferenceSerializer;

    @Override
    public void initialize() throws InitializationException
    {
        this.temporaryAttachmentSessionMap = new ConcurrentHashMap<>();
    }

    @Override
    public void dispose() throws ComponentLifecycleException
    {
        this.temporaryAttachmentSessionMap.values().forEach(TemporaryAttachmentSession::dispose);
    }

    @Override
    public void sessionCreated(HttpSessionEvent httpSessionEvent)
    {
        // Nothing should be done here.
    }

    /**
     * Accessor to the map of temporary attachment sessions. Mainly for test purpose.
     *
     * @return the map of temporary attachment session.
     */
    protected Map<String, TemporaryAttachmentSession> getTemporaryAttachmentSessionMap()
    {
        return temporaryAttachmentSessionMap;
    }

    private String getSessionId()
    {
        XWikiContext context = this.contextProvider.get();
        return context.getRequest().getSession().getId();
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent httpSessionEvent)
    {
        String sessionId = httpSessionEvent.getSession().getId();
        if (this.temporaryAttachmentSessionMap.containsKey(sessionId)) {
            this.temporaryAttachmentSessionMap.get(sessionId).dispose();
            this.temporaryAttachmentSessionMap.remove(sessionId);
        }
    }

    private String getCacheKey(String sessionId, DocumentReference documentReference)
    {
        return String.format("%s_%s", sessionId, this.stringEntityReferenceSerializer.serialize(documentReference));
    }

    private Cache<XWikiAttachment> getOrCreateCache(TemporaryAttachmentSession temporaryAttachmentSession,
        DocumentReference documentReference) throws CacheException
    {
        Cache<XWikiAttachment> result;

        if (!temporaryAttachmentSession.hasOpenEditionSession(documentReference)) {
            String key = getCacheKey(temporaryAttachmentSession.getSessionId(), documentReference);
            String configName = String.format("temp.attachment.%s", key);
            // FIXME: the configuration values should be configurable
            result = this.cacheManager.createNewCache(new LRUCacheConfiguration(configName, 10000, 3600));
            temporaryAttachmentSession.startEditionSession(documentReference, result);
        } else {
            result = temporaryAttachmentSession.getCache(documentReference);
        }
        return result;
    }

    private TemporaryAttachmentSession getOrCreateSession()
    {
        TemporaryAttachmentSession temporaryAttachmentSession;
        String sessionId = getSessionId();
        if (this.temporaryAttachmentSessionMap.containsKey(sessionId)) {
            temporaryAttachmentSession = this.temporaryAttachmentSessionMap.get(sessionId);
        } else {
            temporaryAttachmentSession = new TemporaryAttachmentSession(sessionId);
            this.temporaryAttachmentSessionMap.put(sessionId, temporaryAttachmentSession);
        }
        return temporaryAttachmentSession;
    }

    @Override
    public XWikiAttachment uploadAttachment(DocumentReference documentReference, Part part)
        throws TemporaryAttachmentException
    {
        // FIXME: handle upload max size
        XWikiAttachment xWikiAttachment;
        TemporaryAttachmentSession temporaryAttachmentSession = getOrCreateSession();
        try {
            Cache<XWikiAttachment> cache = this.getOrCreateCache(temporaryAttachmentSession, documentReference);
            xWikiAttachment = new XWikiAttachment();
            xWikiAttachment.setFilename(part.getSubmittedFileName());
            xWikiAttachment.setContent(part.getInputStream());
            cache.set(xWikiAttachment.getFilename(), xWikiAttachment);
        } catch (CacheException e) {
            throw new TemporaryAttachmentException("Error while creating dedicated cache for temporary uploads", e);
        } catch (IOException e) {
            throw new TemporaryAttachmentException("Error while reading the content of a request part", e);
        }
        return xWikiAttachment;
    }

    @Override
    public Collection<XWikiAttachment> getUploadedAttachments(DocumentReference documentReference)
    {
        List<XWikiAttachment> result = new ArrayList<>();
        TemporaryAttachmentSession temporaryAttachmentSession = getOrCreateSession();
        if (temporaryAttachmentSession.hasOpenEditionSession(documentReference)) {
            Cache<XWikiAttachment> xWikiAttachmentCache = temporaryAttachmentSession.getCache(documentReference);
            Set<String> filenames = temporaryAttachmentSession.getFilenames(documentReference);
            for (String filename : filenames) {
                result.add(xWikiAttachmentCache.get(filename));
            }
        }
        return result;
    }

    @Override
    public Optional<XWikiAttachment> getUploadedAttachment(DocumentReference documentReference, String filename)
    {
        Optional<XWikiAttachment> result = Optional.empty();
        TemporaryAttachmentSession temporaryAttachmentSession = getOrCreateSession();
        if (temporaryAttachmentSession.hasOpenEditionSession(documentReference)) {
            Cache<XWikiAttachment> xWikiAttachmentCache = temporaryAttachmentSession.getCache(documentReference);
            XWikiAttachment xWikiAttachment = xWikiAttachmentCache.get(filename);
            if (xWikiAttachment != null) {
                result = Optional.of(xWikiAttachment);
            }
        }
        return result;
    }

    @Override
    public boolean removeUploadedAttachment(DocumentReference documentReference, String filename)
    {
        boolean result = false;
        TemporaryAttachmentSession temporaryAttachmentSession = getOrCreateSession();
        if (temporaryAttachmentSession.hasOpenEditionSession(documentReference)) {
            Cache<XWikiAttachment> xWikiAttachmentCache = temporaryAttachmentSession.getCache(documentReference);
            Set<String> filenames = temporaryAttachmentSession.getFilenames(documentReference);
            if (filenames.contains(filename)) {
                xWikiAttachmentCache.remove(filename);
                result = true;
            }
        }
        return result;
    }

    @Override
    public boolean removeUploadedAttachments(DocumentReference documentReference)
    {
        boolean result = false;
        TemporaryAttachmentSession temporaryAttachmentSession = getOrCreateSession();
        if (temporaryAttachmentSession.hasOpenEditionSession(documentReference)) {
            result = true;
            Cache<XWikiAttachment> xWikiAttachmentCache = temporaryAttachmentSession.getCache(documentReference);
            xWikiAttachmentCache.removeAll();
        }
        return result;
    }
}
