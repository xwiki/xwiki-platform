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

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;

import javax.inject.Provider;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.Part;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.cache.Cache;
import org.xwiki.cache.CacheManager;
import org.xwiki.cache.config.CacheConfiguration;
import org.xwiki.cache.config.LRUCacheConfiguration;
import org.xwiki.environment.Environment;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.test.junit5.XWikiTempDir;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.web.Utils;
import com.xpn.xwiki.web.XWikiRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.xwiki.store.filesystem.internal.DefaultTemporaryAttachmentManager.UPLOAD_DEFAULT_MAXSIZE;
import static org.xwiki.store.filesystem.internal.DefaultTemporaryAttachmentManager.UPLOAD_MAXSIZE_PARAMETER;

/**
 * Tests for {@link DefaultTemporaryAttachmentManager}.
 *
 * @version $Id$
 * @since 14.3RC1
 */
@ComponentTest
class DefaultTemporaryAttachmentManagerTest
{
    @InjectMockComponents
    private DefaultTemporaryAttachmentManager attachmentManager;

    @MockComponent
    private CacheManager cacheManager;

    @MockComponent
    private Provider<XWikiContext> contextProvider;

    @MockComponent
    private EntityReferenceSerializer<String> stringEntityReferenceSerializer;

    @XWikiTempDir
    private File tmpDir;

    private XWikiContext context;
    private HttpSession httpSession;

    @BeforeEach
    void setup(MockitoComponentManager mockitoComponentManager) throws Exception
    {
        this.context = mock(XWikiContext.class);
        when(this.contextProvider.get()).thenReturn(this.context);

        this.httpSession = mock(HttpSession.class);
        XWikiRequest xWikiRequest = mock(XWikiRequest.class);
        when(xWikiRequest.getSession()).thenReturn(this.httpSession);
        when(this.context.getRequest()).thenReturn(xWikiRequest);
        Utils.setComponentManager(mockitoComponentManager);

        Environment environment = mockitoComponentManager.registerMockComponent(Environment.class);
        when(environment.getTemporaryDirectory()).thenReturn(this.tmpDir);
    }

    @Test
    void uploadAttachmentCacheInitialization() throws Exception
    {
        String sessionId = "mySession";
        when(httpSession.getId()).thenReturn(sessionId);

        DocumentReference documentReference = mock(DocumentReference.class);
        Part part = mock(Part.class);

        when(this.stringEntityReferenceSerializer.serialize(documentReference)).thenReturn("Some.XWiki.Doc");

        Cache cache = mock(Cache.class);
        when(this.cacheManager.createNewCache(any(CacheConfiguration.class))).thenAnswer(invocationOnMock -> {
            LRUCacheConfiguration cacheConfiguration = invocationOnMock.getArgument(0);
            assertEquals("temp.attachment.mySession_Some.XWiki.Doc", cacheConfiguration.getConfigurationId());
            assertEquals(10000, cacheConfiguration.getLRUEvictionConfiguration().getMaxEntries());
            assertEquals(3600, cacheConfiguration.getLRUEvictionConfiguration().getMaxIdle());
            return cache;
        });

        when(part.getSubmittedFileName()).thenReturn("fileFoo.xml");
        InputStream inputStream = new ByteArrayInputStream("foo".getBytes(StandardCharsets.UTF_8));
        when(part.getInputStream()).thenReturn(inputStream);

        XWiki xWiki = mock(XWiki.class);
        when(context.getWiki()).thenReturn(xWiki);
        when(xWiki.getSpacePreferenceAsLong(UPLOAD_MAXSIZE_PARAMETER, UPLOAD_DEFAULT_MAXSIZE, context)).thenReturn(42L);
        when(part.getSize()).thenReturn(41L);

        XWikiAttachment attachment = this.attachmentManager.uploadAttachment(documentReference, part);
        assertNotNull(attachment);
        assertEquals("fileFoo.xml", attachment.getFilename());

        Map<String, TemporaryAttachmentSession> attachmentSessionMap =
            this.attachmentManager.getTemporaryAttachmentSessionMap();
        assertEquals(1, attachmentSessionMap.size());

        TemporaryAttachmentSession temporaryAttachmentSession = attachmentSessionMap.get(sessionId);
        assertEquals(sessionId, temporaryAttachmentSession.getSessionId());

        Map<DocumentReference, TemporaryAttachmentDocumentSession> documentSessionMap =
            temporaryAttachmentSession.getTemporaryAttachmentDocumentSessionMap();
        assertEquals(1, documentSessionMap.size());

        TemporaryAttachmentDocumentSession attachmentDocumentSession = documentSessionMap.get(documentReference);
        assertSame(cache, attachmentDocumentSession.getAttachmentCache());
        verify(cache).addCacheEntryListener(any());
        verify(cache).set("fileFoo.xml", attachment);
        assertEquals(sessionId, attachmentDocumentSession.getSessionId());
        assertEquals(documentReference, attachmentDocumentSession.getDocumentReference());
    }

    @Test
    void uploadAttachmentExistingDocumentSession() throws Exception
    {
        String sessionId = "another";
        when(httpSession.getId()).thenReturn(sessionId);
        TemporaryAttachmentSession temporaryAttachmentSession = mock(TemporaryAttachmentSession.class);
        this.attachmentManager.getTemporaryAttachmentSessionMap().put(sessionId, temporaryAttachmentSession);

        DocumentReference documentReference = mock(DocumentReference.class);
        Part part = mock(Part.class);
        when(temporaryAttachmentSession.hasOpenEditionSession(documentReference)).thenReturn(true);

        Cache cache = mock(Cache.class);
        when(temporaryAttachmentSession.getCache(documentReference)).thenReturn(cache);

        String filename = "myfile.txt";
        when(part.getSubmittedFileName()).thenReturn(filename);
        InputStream inputStream = new ByteArrayInputStream("foo".getBytes(StandardCharsets.UTF_8));
        when(part.getInputStream()).thenReturn(inputStream);

        XWiki xWiki = mock(XWiki.class);
        when(context.getWiki()).thenReturn(xWiki);
        when(xWiki.getSpacePreferenceAsLong(UPLOAD_MAXSIZE_PARAMETER, UPLOAD_DEFAULT_MAXSIZE, context)).thenReturn(42L);
        when(part.getSize()).thenReturn(41L);

        XWikiAttachment attachment = this.attachmentManager.uploadAttachment(documentReference, part);
        assertNotNull(attachment);
        assertEquals(filename, attachment.getFilename());

        verifyNoInteractions(this.cacheManager);
        verify(cache).set(filename, attachment);
    }

    @Test
    void sessionDestroyed()
    {
        String sessionId = "fooo";
        when(httpSession.getId()).thenReturn(sessionId);
        TemporaryAttachmentSession temporaryAttachmentSession = mock(TemporaryAttachmentSession.class);
        this.attachmentManager.getTemporaryAttachmentSessionMap().put(sessionId, temporaryAttachmentSession);

        HttpSessionEvent sessionEvent = mock(HttpSessionEvent.class);
        when(sessionEvent.getSession()).thenReturn(this.httpSession);
        this.attachmentManager.sessionDestroyed(sessionEvent);
        verify(temporaryAttachmentSession).dispose();

        assertTrue(this.attachmentManager.getTemporaryAttachmentSessionMap().isEmpty());
    }

    @Test
    void getUploadedAttachments()
    {
        String sessionId = "uploadedAttachments";
        when(httpSession.getId()).thenReturn(sessionId);
        TemporaryAttachmentSession temporaryAttachmentSession = mock(TemporaryAttachmentSession.class);
        this.attachmentManager.getTemporaryAttachmentSessionMap().put(sessionId, temporaryAttachmentSession);
        DocumentReference documentReference = mock(DocumentReference.class);
        when(temporaryAttachmentSession.hasOpenEditionSession(documentReference)).thenReturn(false);
        assertEquals(Collections.emptyList(), this.attachmentManager.getUploadedAttachments(documentReference));

        when(temporaryAttachmentSession.hasOpenEditionSession(documentReference)).thenReturn(true);
        Cache cache = mock(Cache.class);
        when(temporaryAttachmentSession.getCache(documentReference)).thenReturn(cache);

        String filename1 = "foo1";
        String filename2 = "foo2";
        String filename3 = "foo3";

        XWikiAttachment attachment1 = mock(XWikiAttachment.class);
        XWikiAttachment attachment2 = mock(XWikiAttachment.class);
        XWikiAttachment attachment3 = mock(XWikiAttachment.class);

        when(cache.get(filename1)).thenReturn(attachment1);
        when(cache.get(filename2)).thenReturn(attachment2);
        when(cache.get(filename3)).thenReturn(attachment3);

        when(temporaryAttachmentSession.getFilenames(documentReference)).thenReturn(new HashSet<>(Arrays.asList(
            filename1, filename2, filename3
        )));
        assertEquals(Arrays.asList(attachment1, attachment2, attachment3),
            this.attachmentManager.getUploadedAttachments(documentReference));
    }

    @Test
    void getUploadedAttachment()
    {
        String sessionId = "uploadedAttachmentSingular";
        when(httpSession.getId()).thenReturn(sessionId);
        TemporaryAttachmentSession temporaryAttachmentSession = mock(TemporaryAttachmentSession.class);
        this.attachmentManager.getTemporaryAttachmentSessionMap().put(sessionId, temporaryAttachmentSession);

        DocumentReference documentReference = mock(DocumentReference.class);
        String filename = "foobar";
        when(temporaryAttachmentSession.hasOpenEditionSession(documentReference)).thenReturn(false);
        assertEquals(Optional.empty(), this.attachmentManager.getUploadedAttachment(documentReference, filename));

        when(temporaryAttachmentSession.hasOpenEditionSession(documentReference)).thenReturn(true);
        Cache cache = mock(Cache.class);
        when(temporaryAttachmentSession.getCache(documentReference)).thenReturn(cache);

        String filename1 = "foo1";
        String filename2 = "foo2";
        String filename3 = "foobar";

        XWikiAttachment attachment1 = mock(XWikiAttachment.class);
        XWikiAttachment attachment2 = mock(XWikiAttachment.class);
        XWikiAttachment attachment3 = mock(XWikiAttachment.class);

        when(cache.get(filename1)).thenReturn(attachment1);
        when(cache.get(filename2)).thenReturn(attachment2);

        assertEquals(Optional.empty(), this.attachmentManager.getUploadedAttachment(documentReference, filename));
        when(cache.get(filename3)).thenReturn(attachment3);
        assertEquals(Optional.of(attachment3),
            this.attachmentManager.getUploadedAttachment(documentReference, filename));
    }

    @Test
    void removeUploadedAttachment()
    {
        String sessionId = "removeUploadedAttachment";
        when(httpSession.getId()).thenReturn(sessionId);
        TemporaryAttachmentSession temporaryAttachmentSession = mock(TemporaryAttachmentSession.class);
        this.attachmentManager.getTemporaryAttachmentSessionMap().put(sessionId, temporaryAttachmentSession);

        DocumentReference documentReference = mock(DocumentReference.class);
        String filename = "foobar";
        when(temporaryAttachmentSession.hasOpenEditionSession(documentReference)).thenReturn(false);
        assertFalse(this.attachmentManager.removeUploadedAttachment(documentReference, filename));

        when(temporaryAttachmentSession.hasOpenEditionSession(documentReference)).thenReturn(true);
        Cache cache = mock(Cache.class);
        when(temporaryAttachmentSession.getCache(documentReference)).thenReturn(cache);

        String filename1 = "foo1";
        String filename2 = "foo2";
        String filename3 = "foobar";

        XWikiAttachment attachment1 = mock(XWikiAttachment.class);
        XWikiAttachment attachment2 = mock(XWikiAttachment.class);
        XWikiAttachment attachment3 = mock(XWikiAttachment.class);

        when(cache.get(filename1)).thenReturn(attachment1);
        when(cache.get(filename2)).thenReturn(attachment2);
        when(cache.get(filename3)).thenReturn(attachment3);

        when(temporaryAttachmentSession.getFilenames(documentReference)).thenReturn(new HashSet<>(Arrays.asList(
            filename1, filename2
        )));
        assertFalse(this.attachmentManager.removeUploadedAttachment(documentReference, filename));

        when(temporaryAttachmentSession.getFilenames(documentReference)).thenReturn(new HashSet<>(Arrays.asList(
            filename1, filename2, filename3
        )));
        assertTrue(this.attachmentManager.removeUploadedAttachment(documentReference, filename));
        verify(cache).remove(filename);
    }

    @Test
    void removeUploadedAttachments()
    {
        String sessionId = "removeUploadedAttachmentsPlural";
        when(httpSession.getId()).thenReturn(sessionId);
        TemporaryAttachmentSession temporaryAttachmentSession = mock(TemporaryAttachmentSession.class);
        this.attachmentManager.getTemporaryAttachmentSessionMap().put(sessionId, temporaryAttachmentSession);
        DocumentReference documentReference = mock(DocumentReference.class);
        when(temporaryAttachmentSession.hasOpenEditionSession(documentReference)).thenReturn(false);
        assertFalse(this.attachmentManager.removeUploadedAttachments(documentReference));

        when(temporaryAttachmentSession.hasOpenEditionSession(documentReference)).thenReturn(true);
        Cache cache = mock(Cache.class);
        when(temporaryAttachmentSession.getCache(documentReference)).thenReturn(cache);
        assertTrue(this.attachmentManager.removeUploadedAttachments(documentReference));

        verify(cache).removeAll();
    }
}
