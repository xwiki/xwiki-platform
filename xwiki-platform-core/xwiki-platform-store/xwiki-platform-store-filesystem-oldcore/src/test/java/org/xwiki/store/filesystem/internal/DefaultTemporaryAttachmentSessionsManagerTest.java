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
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import jakarta.inject.Provider;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.Part;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.xwiki.attachment.validation.AttachmentValidationException;
import org.xwiki.attachment.validation.AttachmentValidator;
import org.xwiki.bridge.event.ActionExecutingEvent;
import org.xwiki.container.Container;
import org.xwiki.container.servlet.ServletRequest;
import org.xwiki.container.servlet.ServletResponse;
import org.xwiki.container.servlet.ServletSession;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.remote.RemoteObservationManagerContext;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.store.TemporaryAttachmentException;
import org.xwiki.store.blob.Blob;
import org.xwiki.store.blob.BlobPath;
import org.xwiki.store.blob.BlobStore;
import org.xwiki.store.blob.BlobStoreException;
import org.xwiki.store.blob.BlobStoreManager;
import org.xwiki.store.blob.FileSystemBlobStoreProperties;
import org.xwiki.store.blob.internal.FileSystemBlobStore;
import org.xwiki.store.legacy.doc.internal.FilesystemAttachmentContent;
import org.xwiki.test.LogLevel;
import org.xwiki.test.TestEnvironment;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.XWikiTempDir;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.web.Utils;

import ch.qos.logback.classic.Level;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link DefaultTemporaryAttachmentSessionsManager}, with a filesystem blob store in a temporary directory.
 * <p>
 * The browser is simulated: its cookie is kept between requests, and a request can reach another cluster member (no
 * session attribute) or come from another browser (no cookie).
 *
 * @version $Id$
 */
@ComponentTest
@ComponentList({ TestEnvironment.class, TemporaryAttachmentStore.class, TemporaryAttachmentScopeManager.class,
    TemporaryAttachmentsLogoutListener.class })
@SuppressWarnings({ "checkstyle:ClassFanOutComplexity", "checkstyle:MultipleStringLiterals" })
class DefaultTemporaryAttachmentSessionsManagerTest
{
    private static final String NAME = TemporaryAttachmentScopeManager.NAME;

    private static final DocumentReference DOCUMENT = new DocumentReference("xwiki", "Space", "Document");

    private static final DocumentReference USER = new DocumentReference("xwiki", "XWiki", "User");

    private static final DocumentReference OTHER_USER = new DocumentReference("xwiki", "XWiki", "Other");

    @InjectMockComponents
    private DefaultTemporaryAttachmentSessionsManager attachmentManager;

    @MockComponent
    private Provider<XWikiContext> contextProvider;

    @MockComponent
    private Provider<AttachmentValidator> attachmentValidatorProvider;

    @MockComponent
    private Container container;

    @MockComponent
    private ContextualAuthorizationManager authorization;

    @MockComponent
    private BlobStoreManager blobStoreManager;

    @MockComponent
    private TemporaryAttachmentsConfiguration configuration;

    @MockComponent
    private EntityReferenceSerializer<String> serializer;

    @MockComponent
    private DocumentReferenceResolver<String> resolver;

    @MockComponent
    private RemoteObservationManagerContext remoteObservationManagerContext;

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    @XWikiTempDir
    private File tempDir;

    @Mock
    private AttachmentValidator attachmentValidator;

    @Mock
    private XWikiContext context;

    @Mock
    private HttpServletRequest httpRequest;

    @Mock
    private HttpServletResponse httpResponse;

    @Mock
    private HttpSession httpSession;

    private BlobStore blobStore;

    /**
     * The value of the scope cookie in the simulated browser.
     */
    private String cookie;

    private final List<String> setCookieHeaders = new ArrayList<>();

    private final Map<String, Object> requestAttributes = new HashMap<>();

    private final Map<String, Object> sessionAttributes = new HashMap<>();

    private DocumentReference user = USER;

    private MockitoComponentManager componentManager;

    @BeforeEach
    void setup(MockitoComponentManager componentManager) throws Exception
    {
        this.componentManager = componentManager;
        Utils.setComponentManager(componentManager);

        FileSystemBlobStoreProperties properties = new FileSystemBlobStoreProperties();
        properties.setRootDirectory(this.tempDir.toPath());
        this.blobStore = new FileSystemBlobStore(TemporaryAttachmentStore.STORE_NAME, properties);
        when(this.blobStoreManager.getBlobStore(TemporaryAttachmentStore.STORE_NAME)).thenReturn(this.blobStore);

        when(this.contextProvider.get()).thenReturn(this.context);
        when(this.context.getUserReference()).thenAnswer(invocation -> this.user);
        when(this.attachmentValidatorProvider.get()).thenReturn(this.attachmentValidator);
        when(this.configuration.getMaxCount()).thenReturn(-1);
        when(this.configuration.getMaxAge()).thenReturn(Duration.ofHours(24));
        when(this.authorization.hasAccess(eq(Right.EDIT), any())).thenReturn(true);
        when(this.configuration.getMaxTotalSize()).thenReturn(-1L);
        when(this.serializer.serialize(any())).thenAnswer(invocation -> invocation.getArgument(0).toString());
        when(this.resolver.resolve(anyString())).thenAnswer(invocation -> {
            String value = invocation.getArgument(0);
            String wiki = StringUtils.substringBefore(value, ':');
            String local = StringUtils.substringAfter(value, ':');
            return new DocumentReference(wiki, StringUtils.substringBeforeLast(local, "."),
                StringUtils.substringAfterLast(local, "."));
        });

        when(this.httpRequest.getContextPath()).thenReturn("/xwiki");
        when(this.httpRequest.getCookies()).thenAnswer(
            invocation -> this.cookie != null ? new Cookie[] { new Cookie("other", "value"), new Cookie(NAME, this.cookie) }
                : null);
        when(this.httpRequest.getAttribute(anyString()))
            .thenAnswer(invocation -> this.requestAttributes.get(invocation.getArgument(0)));
        doAnswer(invocation -> this.requestAttributes.put(invocation.getArgument(0), invocation.getArgument(1)))
            .when(this.httpRequest).setAttribute(anyString(), any());
        doAnswer(invocation -> this.requestAttributes.remove(invocation.getArgument(0))).when(this.httpRequest)
            .removeAttribute(anyString());
        when(this.httpSession.getAttribute(anyString()))
            .thenAnswer(invocation -> this.sessionAttributes.get(invocation.getArgument(0)));
        doAnswer(invocation -> this.sessionAttributes.put(invocation.getArgument(0), invocation.getArgument(1)))
            .when(this.httpSession).setAttribute(anyString(), any());
        doAnswer(invocation -> this.sessionAttributes.remove(invocation.getArgument(0))).when(this.httpSession)
            .removeAttribute(anyString());
        doAnswer(invocation -> {
            String header = invocation.getArgument(1);
            this.setCookieHeaders.add(header);
            String value = StringUtils.substringBetween(header, NAME + "=", ";");
            this.cookie = header.contains("Max-Age=0") ? null : value;
            return null;
        }).when(this.httpResponse).addHeader(eq("Set-Cookie"), anyString());
    }

    private void setupRequest()
    {
        when(this.container.getRequest()).thenReturn(new ServletRequest(this.httpRequest));
        when(this.container.getResponse()).thenReturn(new ServletResponse(this.httpResponse));
        when(this.container.getSession()).thenReturn(new ServletSession(this.httpSession));
    }

    /**
     * Simulates a new request of the same browser on the same member.
     */
    private void newRequest()
    {
        this.requestAttributes.clear();
    }

    /**
     * Simulates a new request of the same browser reaching another cluster member (another HTTP session).
     */
    private void switchMember()
    {
        newRequest();
        this.sessionAttributes.clear();
    }

    /**
     * Simulates a request from another browser.
     */
    private void newBrowser()
    {
        switchMember();
        this.cookie = null;
    }

    private Part part(String filename, String content) throws Exception
    {
        Part part = mock(Part.class);
        when(part.getSubmittedFileName()).thenReturn(filename);
        when(part.getInputStream()).thenAnswer(invocation -> new ByteArrayInputStream(content.getBytes(UTF_8)));
        return part;
    }

    private static String read(XWikiAttachment attachment) throws Exception
    {
        try (InputStream stream = attachment.getAttachment_content().getContentInputStream()) {
            return IOUtils.toString(stream, UTF_8);
        }
    }

    private long countBlobs() throws BlobStoreException
    {
        try (Stream<Blob> blobs = this.blobStore.listDescendants(BlobPath.root())) {
            return blobs.count();
        }
    }

    @Test
    void uploadAttachmentWithoutRequestOrSession() throws Exception
    {
        Part part = part("file.txt", "foo");

        TemporaryAttachmentException exception = assertThrows(TemporaryAttachmentException.class,
            () -> this.attachmentManager.uploadAttachment(DOCUMENT, part));
        assertEquals("Cannot find a user http session.", exception.getMessage());
        assertEquals(0, countBlobs());
    }

    @Test
    void uploadAttachmentAndReadItOnAnotherMember() throws Exception
    {
        setupRequest();

        XWikiAttachment attachment = this.attachmentManager.uploadAttachment(DOCUMENT, part("fileFoo.xml", "foo"));
        assertEquals("fileFoo.xml", attachment.getFilename());
        assertEquals(USER, attachment.getAuthorReference());
        assertEquals(DOCUMENT, attachment.getDoc().getDocumentReference());
        assertEquals(3, attachment.getLongSize());
        assertEquals("foo", read(attachment));
        assertInstanceOf(FilesystemAttachmentContent.class, attachment.getAttachment_content());

        // The scope cookie is sent once, and copied into the session.
        assertEquals(1, this.setCookieHeaders.size());
        assertEquals(NAME + "=" + this.cookie + "; Path=/xwiki; Max-Age=86400; HttpOnly; SameSite=Lax",
            this.setCookieHeaders.get(0));
        assertEquals(22, this.cookie.length());
        assertEquals(this.cookie, this.sessionAttributes.get(NAME));
        // Content and metadata.
        assertEquals(2, countBlobs());

        // Another member: no session attribute, the cookie is enough.
        switchMember();
        Optional<XWikiAttachment> found = this.attachmentManager.getUploadedAttachment(DOCUMENT, "fileFoo.xml");
        assertTrue(found.isPresent());
        assertEquals("foo", read(found.get()));
        assertEquals(USER, found.get().getAuthorReference());
        assertEquals(attachment.getDate(), found.get().getDate());
        assertEquals(3, found.get().getLongSize());
        assertTrue(found.get().getAttachment_content().isContentDirty());
        Collection<XWikiAttachment> all = this.attachmentManager.getUploadedAttachments(DOCUMENT);
        assertEquals(1, all.size());
        assertEquals("fileFoo.xml", all.iterator().next().getFilename());
        assertTrue(this.attachmentManager.getUploadedAttachment(DOCUMENT, "other.txt").isEmpty());
        assertTrue(this.attachmentManager
            .getUploadedAttachments(new DocumentReference("xwiki", "Space", "Other")).isEmpty());
        assertTrue(this.attachmentManager
            .getUploadedAttachments(new DocumentReference(DOCUMENT, Locale.FRENCH)).isEmpty());

        // A new upload on this member reuses the scope, copies it into the new session and refreshes the cookie.
        String scopeId = this.cookie;
        this.attachmentManager.uploadAttachment(DOCUMENT, part("second.txt", "bar"));
        assertEquals(2, this.setCookieHeaders.size());
        assertEquals(this.setCookieHeaders.get(0), this.setCookieHeaders.get(1));
        assertEquals(scopeId, this.cookie);
        assertEquals(scopeId, this.sessionAttributes.get(NAME));
        assertEquals(2, this.attachmentManager.getUploadedAttachments(DOCUMENT).size());
    }

    @Test
    void uploadAttachmentOnSecureRequest() throws Exception
    {
        setupRequest();
        when(this.httpRequest.isSecure()).thenReturn(true);
        when(this.httpRequest.getContextPath()).thenReturn("");

        this.attachmentManager.uploadAttachment(DOCUMENT, part("file.txt", "foo"));

        assertEquals(NAME + "=" + this.cookie + "; Path=/; Max-Age=86400; HttpOnly; SameSite=Lax; Secure",
            this.setCookieHeaders.get(0));
    }

    @Test
    void uploadAttachmentWithLostCookie() throws Exception
    {
        setupRequest();
        this.attachmentManager.uploadAttachment(DOCUMENT, part("file.txt", "foo"));
        String scopeId = this.cookie;

        // Same member, but the browser lost the cookie: the session keeps the scope and the cookie is sent again.
        newRequest();
        this.cookie = null;
        assertTrue(this.attachmentManager.getUploadedAttachment(DOCUMENT, "file.txt").isPresent());
        this.attachmentManager.uploadAttachment(DOCUMENT, part("other.txt", "bar"));
        assertEquals(scopeId, this.cookie);
        assertEquals(2, this.setCookieHeaders.size());
    }

    @Test
    void uploadAttachmentWithInvalidCookie() throws Exception
    {
        setupRequest();
        this.cookie = "../../invalid";

        assertTrue(this.attachmentManager.getUploadedAttachments(DOCUMENT).isEmpty());
        this.attachmentManager.uploadAttachment(DOCUMENT, part("file.txt", "foo"));

        assertNotEquals("../../invalid", this.cookie);
        assertEquals(22, this.cookie.length());
    }

    @Test
    void uploadAttachmentWithCommittedResponse() throws Exception
    {
        setupRequest();
        when(this.httpResponse.isCommitted()).thenReturn(true);

        this.attachmentManager.uploadAttachment(DOCUMENT, part("file.txt", "foo"));

        // No cookie, but the session still knows the scope.
        assertNull(this.cookie);
        newRequest();
        assertTrue(this.attachmentManager.getUploadedAttachment(DOCUMENT, "file.txt").isPresent());
    }

    @Test
    void uploadAttachmentWithFilename() throws Exception
    {
        setupRequest();
        Part part = part("partFilename", "foo");

        XWikiAttachment attachment = this.attachmentManager.uploadAttachment(DOCUMENT, part, "newFilename");

        assertEquals("newFilename", attachment.getFilename());
        verify(part, never()).getSubmittedFileName();
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = { "", " ", "  ", "\t" })
    void uploadAttachmentWithEmptyFilename(String filename) throws Exception
    {
        setupRequest();

        XWikiAttachment attachment =
            this.attachmentManager.uploadAttachment(DOCUMENT, part("partFilename", "foo"), filename);

        assertEquals("partFilename", attachment.getFilename());
    }

    @Test
    void uploadAttachmentInvalid() throws Exception
    {
        setupRequest();
        doThrow(AttachmentValidationException.class).when(this.attachmentValidator).validateAttachment(any());

        assertThrows(AttachmentValidationException.class,
            () -> this.attachmentManager.uploadAttachment(DOCUMENT, part("file.txt", "foo")));

        assertEquals(0, countBlobs());
    }

    @Test
    void uploadAttachmentWithStoreFailure() throws Exception
    {
        setupRequest();
        when(this.blobStoreManager.getBlobStore(TemporaryAttachmentStore.STORE_NAME))
            .thenThrow(new BlobStoreException("unavailable"));

        TemporaryAttachmentException exception = assertThrows(TemporaryAttachmentException.class,
            () -> this.attachmentManager.uploadAttachment(DOCUMENT, part("file.txt", "foo")));
        assertEquals("Failed to store the temporary attachment [file.txt] of [xwiki:Space.Document]",
            exception.getMessage());

        // Reading and removing do not fail.
        assertTrue(this.attachmentManager.getUploadedAttachments(DOCUMENT).isEmpty());
        assertTrue(this.attachmentManager.getUploadedAttachment(DOCUMENT, "file.txt").isEmpty());
        assertFalse(this.attachmentManager.removeUploadedAttachments(DOCUMENT));
        // The second read failure within a minute is logged at debug level only.
        assertEquals(2, this.logCapture.size());
        assertEquals("Failed to list the temporary attachments of [xwiki:Space.Document]: "
            + "[BlobStoreException: unavailable]", this.logCapture.getMessage(0));
        assertEquals("Failed to remove the temporary attachments of [xwiki:Space.Document]: "
            + "[BlobStoreException: unavailable]", this.logCapture.getMessage(1));
    }

    @Test
    void uploadSameNameTwiceKeepsTheNewest() throws Exception
    {
        setupRequest();

        this.attachmentManager.uploadAttachment(DOCUMENT, part("file.txt", "first"));
        Thread.sleep(2);
        this.attachmentManager.uploadAttachment(DOCUMENT, part("file.txt", "second"));

        Collection<XWikiAttachment> attachments = this.attachmentManager.getUploadedAttachments(DOCUMENT);
        assertEquals(1, attachments.size());
        assertEquals("second", read(attachments.iterator().next()));
        // The older upload was deleted.
        assertEquals(2, countBlobs());
    }

    @Test
    void scopeIsolationBetweenUsersAndBrowsers() throws Exception
    {
        setupRequest();
        this.attachmentManager.uploadAttachment(DOCUMENT, part("file.txt", "foo"));

        // Another user with the same scope cookie sees nothing and removes nothing.
        newRequest();
        this.user = OTHER_USER;
        assertTrue(this.attachmentManager.getUploadedAttachments(DOCUMENT).isEmpty());
        assertTrue(this.attachmentManager.getUploadedAttachment(DOCUMENT, "file.txt").isEmpty());
        assertFalse(this.attachmentManager.removeUploadedAttachments(DOCUMENT));

        // Same for guest.
        this.user = null;
        assertTrue(this.attachmentManager.getUploadedAttachments(DOCUMENT).isEmpty());

        // The same user in another browser (another scope) sees nothing either.
        this.user = USER;
        String scopeId = this.cookie;
        newBrowser();
        assertTrue(this.attachmentManager.getUploadedAttachments(DOCUMENT).isEmpty());
        this.attachmentManager.uploadAttachment(DOCUMENT, part("file.txt", "bar"));
        assertNotEquals(scopeId, this.cookie);
        assertEquals("bar", read(this.attachmentManager.getUploadedAttachment(DOCUMENT, "file.txt").get()));

        // The first browser still has its own upload.
        switchMember();
        this.cookie = scopeId;
        assertEquals("foo", read(this.attachmentManager.getUploadedAttachment(DOCUMENT, "file.txt").get()));
    }

    @Test
    void guestUpload() throws Exception
    {
        setupRequest();
        this.user = null;

        XWikiAttachment attachment = this.attachmentManager.uploadAttachment(DOCUMENT, part("file.txt", "foo"));
        assertNull(attachment.getAuthorReference());

        switchMember();
        Optional<XWikiAttachment> found = this.attachmentManager.getUploadedAttachment(DOCUMENT, "file.txt");
        assertTrue(found.isPresent());
        assertNull(found.get().getAuthorReference());
    }

    @Test
    void maxCount() throws Exception
    {
        setupRequest();
        when(this.configuration.getMaxCount()).thenReturn(1);

        this.attachmentManager.uploadAttachment(DOCUMENT, part("file.txt", "foo"));
        // Replacing the same file is allowed.
        this.attachmentManager.uploadAttachment(DOCUMENT, part("file.txt", "bar"));

        TemporaryAttachmentException exception = assertThrows(TemporaryAttachmentException.class,
            () -> this.attachmentManager.uploadAttachment(DOCUMENT, part("other.txt", "foo")));
        assertEquals("Too many pending temporary attachments, save or cancel the current edits first.",
            exception.getMessage());
        assertEquals(1, this.logCapture.size());
        assertEquals("Temporary upload to [xwiki:Space.Document] rejected: the user already has [1] pending "
            + "temporary attachments in this browser (store.temporaryAttachments.maxCount = [1]).",
            this.logCapture.getMessage(0));

        // Another user in the same browser has its own limit.
        this.user = OTHER_USER;
        this.attachmentManager.uploadAttachment(DOCUMENT, part("other.txt", "foo"));
    }

    @Test
    void maxTotalSize() throws Exception
    {
        setupRequest();
        when(this.configuration.getMaxTotalSize()).thenReturn(5L);

        this.attachmentManager.uploadAttachment(DOCUMENT, part("file.txt", "foo"));

        TemporaryAttachmentException exception = assertThrows(TemporaryAttachmentException.class,
            () -> this.attachmentManager.uploadAttachment(new DocumentReference("xwiki", "Space", "Other"),
                part("other.txt", "bar")));
        assertEquals("The pending temporary attachments are too big, save or cancel the current edits first.",
            exception.getMessage());
        assertEquals("Temporary upload to [xwiki:Space.Other] rejected: [3] bytes pending plus [3] bytes exceed "
            + "store.temporaryAttachments.maxTotalSize = [5].", this.logCapture.getMessage(0));

        // Replacing the same file only counts the new size.
        this.attachmentManager.uploadAttachment(DOCUMENT, part("file.txt", "12345"));
        assertEquals("12345", read(this.attachmentManager.getUploadedAttachment(DOCUMENT, "file.txt").get()));
    }

    @Test
    void interruptedUploadIsIgnored() throws Exception
    {
        setupRequest();
        this.attachmentManager.uploadAttachment(DOCUMENT, part("file.txt", "complete"));

        // A newer upload of the same file that was interrupted before its metadata was written.
        TemporaryAttachmentStore store = this.componentManager.getInstance(TemporaryAttachmentStore.class);
        BlobPath filePath = store.getFilePath(store.getDocumentPath(store.getUserPath(this.cookie, USER), DOCUMENT),
            "file.txt");
        this.blobStore.getBlob(filePath.resolve(BlobPath.relative("9999999999999-7-uinterru", "content")))
            .writeFromStream(new ByteArrayInputStream("partial".getBytes(UTF_8)));
        // And an interrupted upload of another file.
        BlobPath otherPath =
            store.getFilePath(store.getDocumentPath(store.getUserPath(this.cookie, USER), DOCUMENT), "other.txt");
        this.blobStore.getBlob(otherPath.resolve(BlobPath.relative("0000000000001-7-uinterru", "content")))
            .writeFromStream(new ByteArrayInputStream("partial".getBytes(UTF_8)));

        newRequest();
        Collection<XWikiAttachment> attachments = this.attachmentManager.getUploadedAttachments(DOCUMENT);
        assertEquals(1, attachments.size());
        assertEquals("complete", read(attachments.iterator().next()));
        assertTrue(this.attachmentManager.getUploadedAttachment(DOCUMENT, "other.txt").isEmpty());
    }

    @Test
    void attachTemporaryAttachmentsInDocument() throws Exception
    {
        XWikiDocument document = mock(XWikiDocument.class);
        when(document.getDocumentReference()).thenReturn(DOCUMENT);
        this.attachmentManager.attachTemporaryAttachmentsInDocument(document, List.of());
        verify(document, never()).setAttachment(any());

        setupRequest();
        this.attachmentManager.uploadAttachment(DOCUMENT, part("myfile.txt", "content1"));
        this.attachmentManager.uploadAttachment(DOCUMENT, part("otherfile.gif", "content2"));

        // Save through another member.
        switchMember();
        XWikiAttachment previousAttachment = mock(XWikiAttachment.class);
        when(previousAttachment.getNextVersion()).thenReturn("4.5");
        List<XWikiAttachment> attached = new ArrayList<>();
        when(document.setAttachment(any())).thenAnswer(invocation -> {
            XWikiAttachment attachment = invocation.getArgument(0);
            attached.add(attachment);
            return "myfile.txt".equals(attachment.getFilename()) ? previousAttachment : null;
        });

        this.attachmentManager.attachTemporaryAttachmentsInDocument(document,
            List.of("foo", "myfile.txt", "", "otherfile.gif"));

        assertEquals(2, attached.size());
        XWikiAttachment attachment1 = attached.get(0);
        assertEquals("myfile.txt", attachment1.getFilename());
        assertEquals("4.5", attachment1.getVersion());
        assertEquals(USER, attachment1.getAuthorReference());
        XWikiAttachment attachment2 = attached.get(1);
        assertEquals("otherfile.gif", attachment2.getFilename());
        assertEquals("1.1", attachment2.getVersion());

        // The attached contents are local copies: they stay readable once the temporary attachments are removed.
        assertFalse(attachment1.getAttachment_content() instanceof FilesystemAttachmentContent);
        assertTrue(this.attachmentManager.removeUploadedAttachments(DOCUMENT));
        assertEquals(0, countBlobs());
        assertEquals("content1", read(attachment1));
        assertEquals("content2", read(attachment2));
        assertEquals(8, attachment2.getLongSize());

        // The unknown name is reported.
        assertEquals(1, this.logCapture.size());
        assertEquals("The temporary attachment [foo] of [xwiki:Space.Document] cannot be found (expired, already "
            + "saved or uploaded in another browser): it is not attached to the document.",
            this.logCapture.getMessage(0));
        assertEquals(Level.WARN, this.logCapture.getLogEvent(0).getLevel());
    }

    @Test
    void removeUploadedAttachment() throws Exception
    {
        // No request and no session.
        assertFalse(this.attachmentManager.removeUploadedAttachment(DOCUMENT, "foobar"));
        assertEquals(1, this.logCapture.size());
        assertEquals("Cannot find a user http session to remove attachment [foobar] from [xwiki:Space.Document].",
            this.logCapture.getMessage(0));
        assertEquals(Level.WARN, this.logCapture.getLogEvent(0).getLevel());

        // A request without scope: nothing to remove, no warning.
        setupRequest();
        assertFalse(this.attachmentManager.removeUploadedAttachment(DOCUMENT, "foobar"));
        assertEquals(1, this.logCapture.size());

        this.attachmentManager.uploadAttachment(DOCUMENT, part("foobar", "foo"));
        this.attachmentManager.uploadAttachment(DOCUMENT, part("other", "bar"));

        switchMember();
        assertTrue(this.attachmentManager.removeUploadedAttachment(DOCUMENT, "foobar"));
        assertFalse(this.attachmentManager.removeUploadedAttachment(DOCUMENT, "foobar"));
        assertFalse(this.attachmentManager.removeUploadedAttachment(DOCUMENT, null));
        assertTrue(this.attachmentManager.getUploadedAttachment(DOCUMENT, "foobar").isEmpty());
        assertTrue(this.attachmentManager.getUploadedAttachment(DOCUMENT, null).isEmpty());
        assertTrue(this.attachmentManager.getUploadedAttachment(DOCUMENT, "other").isPresent());
    }

    @Test
    void removeUploadedAttachments() throws Exception
    {
        // No request and no session.
        assertFalse(this.attachmentManager.removeUploadedAttachments(DOCUMENT));
        assertEquals(1, this.logCapture.size());
        assertEquals("Cannot find a user http session to remove attachments from [xwiki:Space.Document].",
            this.logCapture.getMessage(0));

        setupRequest();
        assertFalse(this.attachmentManager.removeUploadedAttachments(DOCUMENT));
        DocumentReference otherDocument = new DocumentReference("xwiki", "Space", "Other");
        this.attachmentManager.uploadAttachment(DOCUMENT, part("file1", "foo"));
        this.attachmentManager.uploadAttachment(DOCUMENT, part("file2", "foo"));
        this.attachmentManager.uploadAttachment(otherDocument, part("file1", "foo"));

        switchMember();
        assertTrue(this.attachmentManager.removeUploadedAttachments(DOCUMENT));
        assertFalse(this.attachmentManager.removeUploadedAttachments(DOCUMENT));
        assertTrue(this.attachmentManager.getUploadedAttachments(DOCUMENT).isEmpty());
        assertEquals(1, this.attachmentManager.getUploadedAttachments(otherDocument).size());
    }

    @Test
    void temporarilyAttach() throws Exception
    {
        XWikiAttachment attachment = new XWikiAttachment();
        attachment.setFilename("attached.txt");
        attachment.setContent(new ByteArrayInputStream("attached".getBytes(UTF_8)));
        attachment.setAuthorReference(OTHER_USER);

        // No request and no session.
        this.attachmentManager.temporarilyAttach(attachment, DOCUMENT);
        assertEquals(1, this.logCapture.size());
        assertEquals("Cannot find a user http session to attach [attached.txt] to [xwiki:Space.Document].",
            this.logCapture.getMessage(0));
        assertEquals(Level.ERROR, this.logCapture.getLogEvent(0).getLevel());

        setupRequest();
        // The limits do not apply to temporarilyAttach.
        when(this.configuration.getMaxCount()).thenReturn(0);
        this.attachmentManager.temporarilyAttach(attachment, DOCUMENT);

        switchMember();
        XWikiAttachment found = this.attachmentManager.getUploadedAttachment(DOCUMENT, "attached.txt").get();
        assertEquals("attached", read(found));
        assertEquals(OTHER_USER, found.getAuthorReference());
        assertEquals(8, found.getLongSize());
    }

    @Test
    void temporarilyAttachWithStoreFailure() throws Exception
    {
        setupRequest();
        when(this.blobStoreManager.getBlobStore(TemporaryAttachmentStore.STORE_NAME))
            .thenThrow(new BlobStoreException("unavailable"));
        XWikiAttachment attachment = new XWikiAttachment();
        attachment.setFilename("attached.txt");
        attachment.setContent(new ByteArrayInputStream("attached".getBytes(UTF_8)));

        assertThrows(TemporaryAttachmentException.class,
            () -> this.attachmentManager.temporarilyAttach(attachment, DOCUMENT));
    }

    @Test
    void sessionOnlyWithoutRequest() throws Exception
    {
        // A thread with a session but no request (no cookie can be read or sent): the session holds the scope.
        when(this.container.getSession()).thenReturn(new ServletSession(this.httpSession));

        this.attachmentManager.uploadAttachment(DOCUMENT, part("file.txt", "foo"));

        assertNotNull(this.sessionAttributes.get(NAME));
        assertTrue(this.attachmentManager.getUploadedAttachment(DOCUMENT, "file.txt").isPresent());
        assertTrue(this.attachmentManager.removeUploadedAttachments(DOCUMENT));
    }

    @Test
    void logoutDeletesTheScope() throws Exception
    {
        setupRequest();
        this.attachmentManager.uploadAttachment(DOCUMENT, part("file.txt", "foo"));
        this.user = OTHER_USER;
        this.attachmentManager.uploadAttachment(DOCUMENT, part("file.txt", "bar"));
        String scopeId = this.cookie;
        // Another browser of the same user.
        newBrowser();
        this.user = USER;
        this.attachmentManager.uploadAttachment(DOCUMENT, part("file.txt", "other browser"));
        String otherScopeId = this.cookie;

        // Logout in the first browser, through another member, the user being already logged out.
        switchMember();
        this.cookie = scopeId;
        this.user = null;
        EventListener listener =
            this.componentManager.getInstance(EventListener.class, TemporaryAttachmentsLogoutListener.NAME);
        listener.onEvent(new ActionExecutingEvent("logout"), null, this.context);

        // The cookie is expired, and everything uploaded in that browser is deleted.
        assertNull(this.cookie);
        assertEquals(NAME + "=; Path=/xwiki; Max-Age=0; Expires=Thu, 01 Jan 1970 00:00:00 GMT; HttpOnly; SameSite=Lax",
            this.setCookieHeaders.get(this.setCookieHeaders.size() - 1));
        assertEquals(Boolean.FALSE, this.requestAttributes.get(NAME));
        assertEquals(2, countBlobs());

        // The other browser keeps its upload.
        newRequest();
        this.cookie = otherScopeId;
        this.user = USER;
        assertEquals("other browser",
            read(this.attachmentManager.getUploadedAttachment(DOCUMENT, "file.txt").get()));

        // A logout without scope does nothing.
        newBrowser();
        listener.onEvent(new ActionExecutingEvent("logout"), null, this.context);
        assertEquals(2, countBlobs());
    }

    @Test
    void uploadAttachmentWithoutEditRight() throws Exception
    {
        setupRequest();
        when(this.authorization.hasAccess(Right.EDIT, DOCUMENT)).thenReturn(false);
        Part part = part("file.txt", "foo");

        TemporaryAttachmentException exception = assertThrows(TemporaryAttachmentException.class,
            () -> this.attachmentManager.uploadAttachment(DOCUMENT, part));

        assertEquals("The current user is not allowed to edit [xwiki:Space.Document].", exception.getMessage());
        assertEquals(0, countBlobs());
        assertNull(this.cookie);
        verify(part, never()).getInputStream();
    }

    @Test
    void uploadAttachmentWithoutFileName() throws Exception
    {
        setupRequest();

        TemporaryAttachmentException exception = assertThrows(TemporaryAttachmentException.class,
            () -> this.attachmentManager.uploadAttachment(DOCUMENT, part(null, "foo"), " "));
        assertEquals("A temporary attachment needs a file name.", exception.getMessage());

        XWikiAttachment attachment = new XWikiAttachment();
        exception = assertThrows(TemporaryAttachmentException.class,
            () -> this.attachmentManager.temporarilyAttach(attachment, DOCUMENT));
        assertEquals("A temporary attachment needs a file name.", exception.getMessage());
        assertEquals(0, countBlobs());
    }

    @Test
    void cookieSentOncePerRequest() throws Exception
    {
        setupRequest();
        XWikiAttachment attachment = new XWikiAttachment();
        attachment.setFilename("attached.txt");
        attachment.setContent(new ByteArrayInputStream("attached".getBytes(UTF_8)));

        this.attachmentManager.temporarilyAttach(attachment, DOCUMENT);
        this.attachmentManager.temporarilyAttach(attachment, new DocumentReference("xwiki", "Space", "Other"));
        this.attachmentManager.uploadAttachment(DOCUMENT, part("file.txt", "foo"));

        assertEquals(1, this.setCookieHeaders.size());
    }

    @Test
    void scopeIsResetWhenTheBrowserHasNoPendingUpload() throws Exception
    {
        setupRequest();
        DocumentReference otherDocument = new DocumentReference("xwiki", "Space", "Other");
        this.attachmentManager.uploadAttachment(DOCUMENT, part("file.txt", "foo"));
        this.attachmentManager.uploadAttachment(otherDocument, part("file.txt", "bar"));
        String scopeId = this.cookie;

        // Save of the first document: the browser still has a pending upload, the scope is kept.
        switchMember();
        assertTrue(this.attachmentManager.removeUploadedAttachments(DOCUMENT));
        assertEquals(scopeId, this.cookie);

        // Cancel of the second document: nothing is pending any more, the cookie is expired.
        newRequest();
        this.sessionAttributes.put(NAME, scopeId);
        int headers = this.setCookieHeaders.size();
        assertTrue(this.attachmentManager.removeUploadedAttachment(otherDocument, "file.txt"));
        assertNull(this.cookie);
        assertEquals(headers + 1, this.setCookieHeaders.size());
        assertTrue(this.setCookieHeaders.get(headers).contains("; Max-Age=0;"));
        assertNull(this.sessionAttributes.get(NAME));

        // For the rest of the request, the cookie it carries is ignored.
        this.cookie = scopeId;
        assertTrue(this.attachmentManager.getUploadedAttachments(otherDocument).isEmpty());
        assertFalse(this.attachmentManager.removeUploadedAttachments(otherDocument));

        // The next upload creates a new scope.
        newRequest();
        this.cookie = null;
        this.attachmentManager.uploadAttachment(DOCUMENT, part("file.txt", "new"));
        assertNotNull(this.cookie);
        assertNotEquals(scopeId, this.cookie);
    }

    @Test
    void temporarilyAttachIsNotCountedInTheLimits() throws Exception
    {
        setupRequest();
        when(this.configuration.getMaxCount()).thenReturn(1);
        when(this.configuration.getMaxTotalSize()).thenReturn(5L);
        XWikiAttachment attachment = new XWikiAttachment();
        attachment.setFilename("attached.txt");
        attachment.setContent(new ByteArrayInputStream("attached content".getBytes(UTF_8)));

        this.attachmentManager.temporarilyAttach(attachment, DOCUMENT);
        this.attachmentManager.temporarilyAttach(attachment, new DocumentReference("xwiki", "Space", "Other"));

        // The user's own upload is still accepted.
        this.attachmentManager.uploadAttachment(DOCUMENT, part("file.txt", "foo"));
        assertThrows(TemporaryAttachmentException.class,
            () -> this.attachmentManager.uploadAttachment(DOCUMENT, part("other.txt", "foo")));
        assertEquals(1, this.logCapture.size());
        assertEquals("Temporary upload to [xwiki:Space.Document] rejected: the user already has [1] pending "
            + "temporary attachments in this browser (store.temporaryAttachments.maxCount = [1]).",
            this.logCapture.getMessage(0));
    }

    @Test
    void newUploadWinsOverAnUploadFromAMemberWithAClockAhead() throws Exception
    {
        setupRequest();
        this.attachmentManager.uploadAttachment(DOCUMENT, part("file.txt", "first"));

        // Another member, whose clock is one hour ahead, uploaded the same file.
        TemporaryAttachmentStore store = this.componentManager.getInstance(TemporaryAttachmentStore.class);
        BlobPath filePath = store.getFilePath(store.getDocumentPath(store.getUserPath(this.cookie, USER), DOCUMENT),
            "file.txt");
        store.write(filePath, System.currentTimeMillis() + 3_600_000L,
            new ByteArrayInputStream("future".getBytes(UTF_8)), DOCUMENT,
            new TemporaryAttachmentStore.Metadata("file.txt", USER, 6), true);
        assertEquals("future", read(this.attachmentManager.getUploadedAttachment(DOCUMENT, "file.txt").get()));

        // A new upload through this member is still the newest one.
        this.attachmentManager.uploadAttachment(DOCUMENT, part("file.txt", "latest"));

        assertEquals("latest", read(this.attachmentManager.getUploadedAttachment(DOCUMENT, "file.txt").get()));
        assertEquals(2, countBlobs());
    }
}
