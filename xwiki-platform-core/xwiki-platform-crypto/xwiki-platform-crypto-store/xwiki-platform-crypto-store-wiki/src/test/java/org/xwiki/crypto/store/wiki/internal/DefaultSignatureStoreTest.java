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
package org.xwiki.crypto.store.wiki.internal;

import javax.inject.Named;
import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.crypto.BinaryStringEncoder;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.BlockReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceProvider;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.model.reference.CurrentReferenceDocumentReferenceResolver;
import com.xpn.xwiki.internal.model.reference.CurrentReferenceEntityReferenceResolver;
import com.xpn.xwiki.objects.BaseObject;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link org.xwiki.crypto.store.wiki.internal.DefaultSignatureStore}
 *
 * @version $Id$
 * @since 6.0
 */
@ComponentTest
@ComponentList({ CurrentReferenceDocumentReferenceResolver.class, CurrentReferenceEntityReferenceResolver.class})
class DefaultSignatureStoreTest
{
    private static final byte[] SIGNATURE = "signature".getBytes();

    private static final String ENCODED_SIGNATURE = "encoded_signature";

    private static final WikiReference WIKI_REFERENCE = new WikiReference("wiki");
    private static final EntityReference SPACE_REFERENCE = new EntityReference("space", EntityType.WIKI);
    private static final EntityReference DOCUMENT_REFERENCE = new EntityReference("documents", EntityType.DOCUMENT);

    @InjectMockComponents
    private DefaultSignatureStore store;

    @MockComponent
    @Named("default")
    private EntityReferenceProvider defaultEntityReferenceProvider;

    @MockComponent
    @Named("current")
    private EntityReferenceProvider currentEntityReferenceProvider;

    @MockComponent
    private Provider<XWikiContext> xcontextProvider;

    @MockComponent
    @Named("Base64")
    private BinaryStringEncoder encoder;

    private XWikiContext xcontext;

    private XWiki xwiki;

    @BeforeEach
    void setUp() throws Exception
    {
        when(this.currentEntityReferenceProvider.getDefaultReference(EntityType.WIKI)).thenReturn(WIKI_REFERENCE);
        when(this.currentEntityReferenceProvider.getDefaultReference(EntityType.SPACE)).thenReturn(SPACE_REFERENCE);
        when(this.currentEntityReferenceProvider.getDefaultReference(EntityType.DOCUMENT))
            .thenReturn(DOCUMENT_REFERENCE);

        this.xcontext = mock(XWikiContext.class);
        when(this.xcontextProvider.get()).thenReturn(this.xcontext);
        this.xwiki = mock(XWiki.class);
        when(this.xcontext.getWiki()).thenReturn(this.xwiki);

        when(this.encoder.encode(SIGNATURE, 64)).thenReturn(ENCODED_SIGNATURE);
        when(this.encoder.decode(ENCODED_SIGNATURE)).thenReturn(SIGNATURE);
    }

    @Test
    void storingNewSignature() throws Exception
    {
        XWikiDocument sourceDocument = mock(XWikiDocument.class);
        when(this.xwiki.getDocument(new DocumentReference("wiki", "space", "document"), this.xcontext))
            .thenReturn(sourceDocument);

        BaseObject signatureObject = mock(BaseObject.class);
        when(sourceDocument.newXObject(DefaultSignatureStore.SIGNATURECLASS, this.xcontext))
            .thenReturn(signatureObject);

        this.store.store(new BlockReference("block", new DocumentReference("wiki", "space", "document")), SIGNATURE);

        verify(signatureObject).setStringValue(DefaultSignatureStore.SIGNATURECLASS_PROP_REFERENCE, "block");
        verify(signatureObject).setLargeStringValue(DefaultSignatureStore.SIGNATURECLASS_PROP_SIGNATURE,
            ENCODED_SIGNATURE);

        verify(this.xwiki).saveDocument(sourceDocument, this.xcontext);
    }

    @Test
    void updatingSignature() throws Exception
    {
        XWikiDocument sourceDocument = mock(XWikiDocument.class);
        when(this.xwiki.getDocument(new DocumentReference("wiki", "space", "document"), this.xcontext))
            .thenReturn(sourceDocument);

        BaseObject signatureObject = mock(BaseObject.class);
        when(sourceDocument.getXObject(
            new DocumentReference(DefaultSignatureStore.SIGNATURECLASS, new WikiReference("wiki")),
            DefaultSignatureStore.SIGNATURECLASS_PROP_REFERENCE,
            "block")).thenReturn(signatureObject);

        this.store.store(new BlockReference("block", new DocumentReference("wiki", "space", "document")), SIGNATURE);

        verify(signatureObject, never()).setStringValue(eq(DefaultSignatureStore.SIGNATURECLASS_PROP_REFERENCE),
            any(String.class));
        verify(signatureObject).setLargeStringValue(DefaultSignatureStore.SIGNATURECLASS_PROP_SIGNATURE,
            ENCODED_SIGNATURE);

        verify(this.xwiki).saveDocument(sourceDocument, this.xcontext);
    }

    @Test
    void retrievingExistingSignature() throws Exception
    {
        XWikiDocument sourceDocument = mock(XWikiDocument.class);
        when(this.xwiki.getDocument(new DocumentReference("wiki", "space", "document"), this.xcontext))
            .thenReturn(sourceDocument);

        BaseObject signatureObject = mock(BaseObject.class);
        when(sourceDocument.getXObject(
            new DocumentReference(DefaultSignatureStore.SIGNATURECLASS, new WikiReference("wiki")),
            DefaultSignatureStore.SIGNATURECLASS_PROP_REFERENCE,
            "block")).thenReturn(signatureObject);

        when(signatureObject.getLargeStringValue(DefaultSignatureStore.SIGNATURECLASS_PROP_SIGNATURE)).thenReturn(
            ENCODED_SIGNATURE);

        assertArrayEquals(SIGNATURE,
            this.store.retrieve(new BlockReference("block", new DocumentReference("wiki", "space", "document"))));
    }

    @Test
    void retrievingMissingSignature() throws Exception
    {
        XWikiDocument sourceDocument = mock(XWikiDocument.class);
        when(this.xwiki.getDocument(new DocumentReference("wiki", "space", "document"), this.xcontext))
            .thenReturn(sourceDocument);

        assertNull(
            this.store.retrieve(new BlockReference("block", new DocumentReference("wiki", "space", "document"))));
        verify(sourceDocument).getXObject(
            new DocumentReference(DefaultSignatureStore.SIGNATURECLASS, new WikiReference("wiki")),
            DefaultSignatureStore.SIGNATURECLASS_PROP_REFERENCE,
            "block");
    }
}
