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
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;
import javax.servlet.http.Part;

import org.apache.commons.lang3.math.NumberUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLifecycleException;
import org.xwiki.component.phase.Disposable;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.store.TemporaryAttachmentException;
import org.xwiki.store.TemporaryAttachmentSessionsManager;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.plugin.fileupload.FileUploadPlugin;

/**
 * Default implementation of {@link TemporaryAttachmentSessionsManager}.
 * Note that this component also implements {@link HttpSessionListener} so that the cache is properly clean up whenever
 * a session is destroyed.
 *
 * @version $Id$
 * @since 14.3RC1
 */
@Component
@Singleton
public class DefaultTemporaryAttachmentSessionsManager
    implements TemporaryAttachmentSessionsManager, Initializable, Disposable,
    HttpSessionListener
{
    private Map<String, TemporaryAttachmentSession> temporaryAttachmentSessionMap;

    @Inject
    private Provider<XWikiContext> contextProvider;

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
            TemporaryAttachmentSession temporaryAttachmentSession =
                this.temporaryAttachmentSessionMap.remove(sessionId);
            temporaryAttachmentSession.dispose();
        }
    }

    private long getUploadMaxSize(DocumentReference documentReference)
    {
        XWikiContext context = this.contextProvider.get();
        SpaceReference lastSpaceReference = documentReference.getLastSpaceReference();
        String uploadMaxSizeValue = context.getWiki()
            .getSpacePreference(FileUploadPlugin.UPLOAD_MAXSIZE_PARAMETER, lastSpaceReference, context);
        return NumberUtils.toLong(uploadMaxSizeValue, FileUploadPlugin.UPLOAD_DEFAULT_MAXSIZE);
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
        XWikiAttachment xWikiAttachment;
        long uploadMaxSize = getUploadMaxSize(documentReference);
        if (part.getSize() > uploadMaxSize) {
            throw new TemporaryAttachmentException(String.format(
                "The file size [%s] is larger than the upload max size [%s]", part.getSize(), uploadMaxSize));
        }
        TemporaryAttachmentSession temporaryAttachmentSession = getOrCreateSession();
        try {
            xWikiAttachment = new XWikiAttachment();
            xWikiAttachment.setFilename(part.getSubmittedFileName());
            xWikiAttachment.setContent(part.getInputStream());
            temporaryAttachmentSession.addAttachment(documentReference, xWikiAttachment);
        } catch (IOException e) {
            throw new TemporaryAttachmentException("Error while reading the content of a request part", e);
        }
        return xWikiAttachment;
    }

    @Override
    public Collection<XWikiAttachment> getUploadedAttachments(DocumentReference documentReference)
    {
        TemporaryAttachmentSession temporaryAttachmentSession = getOrCreateSession();
        return temporaryAttachmentSession.getAttachments(documentReference);
    }

    @Override
    public Optional<XWikiAttachment> getUploadedAttachment(DocumentReference documentReference, String filename)
    {
        TemporaryAttachmentSession temporaryAttachmentSession = getOrCreateSession();
        return temporaryAttachmentSession.getAttachment(documentReference, filename);
    }

    @Override
    public boolean removeUploadedAttachment(DocumentReference documentReference, String filename)
    {
        TemporaryAttachmentSession temporaryAttachmentSession = getOrCreateSession();
        return temporaryAttachmentSession.removeAttachment(documentReference, filename);
    }

    @Override
    public boolean removeUploadedAttachments(DocumentReference documentReference)
    {
        TemporaryAttachmentSession temporaryAttachmentSession = getOrCreateSession();
        return temporaryAttachmentSession.removeAttachments(documentReference);
    }
}
