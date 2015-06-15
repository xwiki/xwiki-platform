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

import javax.inject.Provider;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.crypto.BinaryStringEncoder;
import org.xwiki.crypto.store.SignatureStore;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.BlockReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceValueProvider;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.model.reference.CurrentReferenceDocumentReferenceResolver;
import com.xpn.xwiki.internal.model.reference.CurrentReferenceEntityReferenceResolver;
import com.xpn.xwiki.objects.BaseObject;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
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
@ComponentList({ CurrentReferenceDocumentReferenceResolver.class, CurrentReferenceEntityReferenceResolver.class })
public class DefaultSignatureStoreTest
{
    private static final byte[] SIGNATURE = "signature".getBytes();

    private static final String ENCODED_SIGNATURE = "encoded_signature";

    @Rule
    public MockitoComponentMockingRule<SignatureStore> mocker =
        new MockitoComponentMockingRule<SignatureStore>(DefaultSignatureStore.class);

    private XWikiContext xcontext;

    private XWiki xwiki;

    private SignatureStore store;

    @Before
    public void setUp() throws Exception
    {
        EntityReferenceValueProvider valueProvider = mock(EntityReferenceValueProvider.class);
        when(valueProvider.getDefaultValue(EntityType.WIKI)).thenReturn("wiki");
        when(valueProvider.getDefaultValue(EntityType.SPACE)).thenReturn("space");
        when(valueProvider.getDefaultValue(EntityType.DOCUMENT)).thenReturn("document");

        this.mocker.registerComponent(EntityReferenceValueProvider.class, "current", valueProvider);

        Provider<XWikiContext> xcontextProvider =
            this.mocker.registerMockComponent(XWikiContext.TYPE_PROVIDER);
        this.xcontext = mock(XWikiContext.class);
        when(xcontextProvider.get()).thenReturn(this.xcontext);
        this.xwiki = mock(com.xpn.xwiki.XWiki.class);
        when(this.xcontext.getWiki()).thenReturn(this.xwiki);

        BinaryStringEncoder encoder = this.mocker.getInstance(BinaryStringEncoder.class, "Base64");
        when(encoder.encode(SIGNATURE, 64)).thenReturn(ENCODED_SIGNATURE);
        when(encoder.decode(ENCODED_SIGNATURE)).thenReturn(SIGNATURE);

        this.store = this.mocker.getComponentUnderTest();
    }

    @Test
    public void testStoringNewSignature() throws Exception
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
    public void testUpdatingSignature() throws Exception
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
    public void testRetrievingExistingSignature() throws Exception
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

        assertThat(
            this.store.retrieve(new BlockReference("block", new DocumentReference("wiki", "space", "document"))),
            equalTo(SIGNATURE));
    }

    @Test
    public void testRetrievingMissingSignature() throws Exception
    {
        XWikiDocument sourceDocument = mock(XWikiDocument.class);
        when(this.xwiki.getDocument(new DocumentReference("wiki", "space", "document"), this.xcontext))
            .thenReturn(sourceDocument);

        assertThat(
            this.store.retrieve(new BlockReference("block", new DocumentReference("wiki", "space", "document"))),
            nullValue());
        verify(sourceDocument).getXObject(
            new DocumentReference(DefaultSignatureStore.SIGNATURECLASS, new WikiReference("wiki")),
            DefaultSignatureStore.SIGNATURECLASS_PROP_REFERENCE,
            "block");
    }
}
