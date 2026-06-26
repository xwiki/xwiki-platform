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
package org.xwiki.security.authorization.internal;

import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.function.FailableFunction;
import org.apache.commons.lang3.mutable.Mutable;
import org.apache.commons.lang3.mutable.MutableObject;
import org.junit.jupiter.api.Test;
import org.xwiki.internal.document.DocumentRequiredRightsReader;
import org.xwiki.internal.document.SimpleDocumentCache;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.security.authorization.AuthorizationException;
import org.xwiki.security.authorization.requiredrights.DocumentRequiredRights;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.test.MockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.InjectMockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.OldcoreTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Component test for {@link DefaultDocumentRequiredRightsManager}.
 *
 * @version $Id$
 */
// Tests can use many different classes.
@SuppressWarnings("checkstyle:ClassFanOutComplexity")
@OldcoreTest
class DefaultDocumentRequiredRightsManagerTest
{
    private static final DocumentReference DOCUMENT_REFERENCE = new DocumentReference("wiki", "space", "page");

    @InjectMockitoOldcore
    private MockitoOldcore mockitoOldcore;

    @InjectMockComponents
    private DefaultDocumentRequiredRightsManager documentRequiredRightsManager;

    @MockComponent
    private DocumentRequiredRightsReader documentRequiredRightsReader;

    private SimpleDocumentCache<Optional<DocumentRequiredRights>, AuthorizationException> simpleDocumentCache;

    @BeforeComponent
    void beforeComponent(MockitoComponentManager componentManager) throws Exception
    {
        // We intentionally register the component without any type as it registered with @ComponentRole to not consider
        // generics.
        this.simpleDocumentCache = componentManager.registerMockComponent(SimpleDocumentCache.class);
        when(this.simpleDocumentCache.get(any(), any())).then(invocationOnMock -> {
                FailableFunction<DocumentReference, DocumentRequiredRights, AuthorizationException> function =
                    invocationOnMock.getArgument(1);
                return function.apply(invocationOnMock.getArgument(0));
            }
        );
    }

    @Test
    void nullDocument() throws AuthorizationException
    {
        assertTrue(this.documentRequiredRightsManager.getRequiredRights(null).isEmpty());
        verifyNoInteractions(this.documentRequiredRightsReader);
        verifyNoInteractions(this.simpleDocumentCache);
    }

    @Test
    void existingDocument() throws Exception
    {
        DocumentRequiredRights documentRequiredRights = initializeMockDocument();

        assertEquals(documentRequiredRights,
            this.documentRequiredRightsManager.getRequiredRights(DOCUMENT_REFERENCE).orElseThrow());

        verify(this.simpleDocumentCache).initializeCache(any());
        verify(this.simpleDocumentCache).get(eq(DOCUMENT_REFERENCE), any());
    }

    @Test
    void documentWithLocale() throws Exception
    {
        DocumentRequiredRights documentRequiredRights = initializeMockDocument();

        DocumentReference documentReferenceWithLocale = new DocumentReference(DOCUMENT_REFERENCE, Locale.GERMAN);

        assertSame(documentRequiredRights,
            this.documentRequiredRightsManager.getRequiredRights(documentReferenceWithLocale).orElseThrow());

        verify(this.simpleDocumentCache).initializeCache(any());
        verify(this.simpleDocumentCache).get(eq(DOCUMENT_REFERENCE), any());
    }

    @Test
    void missingDocument() throws Exception
    {
        assertTrue(this.documentRequiredRightsManager.getRequiredRights(DOCUMENT_REFERENCE).isEmpty());
        verifyNoInteractions(this.documentRequiredRightsReader);

        verify(this.simpleDocumentCache).initializeCache(any());
        verify(this.simpleDocumentCache).get(eq(DOCUMENT_REFERENCE), any());
    }

    @Test
    void failedLoad() throws Exception
    {
        XWikiException expected = mock();
        when(this.mockitoOldcore.getSpyXWiki().getDocument(DOCUMENT_REFERENCE, this.mockitoOldcore.getXWikiContext()))
            .thenThrow(expected);

        AuthorizationException actual = assertThrows(AuthorizationException.class,
            () -> this.documentRequiredRightsManager.getRequiredRights(DOCUMENT_REFERENCE));

        assertEquals("Failed to load the document", actual.getMessage());
        assertEquals(expected, actual.getCause());

        verify(this.simpleDocumentCache).initializeCache(any());
        verify(this.simpleDocumentCache).get(eq(DOCUMENT_REFERENCE), any());
    }

    /**
     * Verify that two calls that if the initialization is ongoing, the required rights manager still answers as
     * expected.
     */
    @Test
    void parallelLoad() throws Exception
    {
        CompletableFuture<Void> arrivedInInitializeCacheFuture = new CompletableFuture<>();
        CompletableFuture<Void> blockInitializeCacheFuture = new CompletableFuture<>();

        doAnswer(invocationOnMock -> {
            arrivedInInitializeCacheFuture.complete(null);
            blockInitializeCacheFuture.get(20, TimeUnit.SECONDS);
            return null;
        }).when(this.simpleDocumentCache).initializeCache(any());

        DocumentRequiredRights documentRequiredRights = initializeMockDocument();

        // Launch a background thread so we can test blocking in the initialization of the cache.
        ExecutorService executor = Executors.newFixedThreadPool(1);

        try {
            CompletableFuture<Optional<DocumentRequiredRights>> firstFuture = CompletableFuture.supplyAsync(() -> {
                try {
                    return this.documentRequiredRightsManager.getRequiredRights(DOCUMENT_REFERENCE);
                } catch (AuthorizationException e) {
                    throw new RuntimeException(e);
                }
            }, executor);

            arrivedInInitializeCacheFuture.get(20, TimeUnit.SECONDS);

            // Ensure that we can still get required rights while the initialization is running.
            assertSame(documentRequiredRights,
                this.documentRequiredRightsManager.getRequiredRights(DOCUMENT_REFERENCE).orElseThrow());

            blockInitializeCacheFuture.complete(null);

            assertSame(documentRequiredRights, firstFuture.get(20, TimeUnit.SECONDS).orElseThrow());

            // Ensure if we call it again, it'll call the cache again.
            assertSame(documentRequiredRights,
                this.documentRequiredRightsManager.getRequiredRights(DOCUMENT_REFERENCE).orElseThrow());

            verify(this.simpleDocumentCache, times(2)).get(any(), any());
            verify(this.simpleDocumentCache).initializeCache(any());
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void recursiveLoad() throws Exception
    {
        DocumentRequiredRights documentRequiredRights = initializeMockDocument();

        Mutable<DocumentRequiredRights> recursiveRights = new MutableObject<>();

        doAnswer(invocation -> {
            recursiveRights.setValue(
                this.documentRequiredRightsManager.getRequiredRights(DOCUMENT_REFERENCE).orElseThrow());
            return null;
        }).when(this.simpleDocumentCache).initializeCache(any());

        assertEquals(documentRequiredRights,
            this.documentRequiredRightsManager.getRequiredRights(DOCUMENT_REFERENCE).orElseThrow());
        assertEquals(documentRequiredRights, recursiveRights.getValue());

        verify(this.simpleDocumentCache).initializeCache(any());
        verify(this.simpleDocumentCache).get(eq(DOCUMENT_REFERENCE), any());

        // Check that loading the required rights again only calls the get() method, and doesn't initialize the cache
        // again.
        assertEquals(documentRequiredRights,
            this.documentRequiredRightsManager.getRequiredRights(DOCUMENT_REFERENCE).orElseThrow());

        verify(this.simpleDocumentCache).initializeCache(any());
        verify(this.simpleDocumentCache, times(2)).get(eq(DOCUMENT_REFERENCE), any());
    }

    private DocumentRequiredRights initializeMockDocument() throws XWikiException
    {
        XWikiDocument document =
            this.mockitoOldcore.getSpyXWiki().getDocument(DOCUMENT_REFERENCE, this.mockitoOldcore.getXWikiContext());
        this.mockitoOldcore.getSpyXWiki().saveDocument(document, this.mockitoOldcore.getXWikiContext());

        DocumentRequiredRights documentRequiredRights = mock();
        when(this.documentRequiredRightsReader.readRequiredRights(document)).thenReturn(documentRequiredRights);
        return documentRequiredRights;
    }
}
