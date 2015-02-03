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
package org.xwiki.lesscss.internal.skin;

import javax.inject.Provider;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.lesscss.compiler.LESSCompilerException;
import org.xwiki.lesscss.skin.DocumentSkinReference;
import org.xwiki.lesscss.skin.FSSkinReference;
import org.xwiki.lesscss.skin.SkinReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.test.mockito.MockitoComponentMockingRule;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @since 6.4RC1
 * @version $Id$
 */
public class DefaultSkinReferenceFactoryTest
{
    @Rule
    public MockitoComponentMockingRule<DefaultSkinReferenceFactory> mocker =
            new MockitoComponentMockingRule<>(DefaultSkinReferenceFactory.class);

    private Provider<XWikiContext> xcontextProvider;

    private DocumentReferenceResolver<String> documentReferenceResolver;

    private WikiDescriptorManager wikiDescriptorManager;

    private XWikiContext xcontext;

    private XWiki xwiki;

    @Before
    public void setUp() throws Exception
    {
        documentReferenceResolver = mocker.getInstance(new DefaultParameterizedType(null,
                DocumentReferenceResolver.class, String.class));
        wikiDescriptorManager = mocker.getInstance(WikiDescriptorManager.class);
        xcontextProvider = mocker.registerMockComponent(XWikiContext.TYPE_PROVIDER);
        xcontext = mock(XWikiContext.class);
        when(xcontextProvider.get()).thenReturn(xcontext);
        xwiki = mock(XWiki.class);
        when(xcontext.getWiki()).thenReturn(xwiki);
    }

    @Test
    public void createReferenceWhenSkinOnDB() throws Exception
    {
        // Mocks
        when(wikiDescriptorManager.getCurrentWikiId()).thenReturn("wikiId");
        DocumentReference skinDocRef = new DocumentReference("wikiId", "XWiki", "MySkin");
        when(documentReferenceResolver.resolve(eq("XWiki.MySkin"), eq(new WikiReference("wikiId"))))
            .thenReturn(skinDocRef);
        when(xwiki.exists(skinDocRef, xcontext)).thenReturn(true);
        XWikiDocument skinDoc = mock(XWikiDocument.class);
        when(xwiki.getDocument(skinDocRef, xcontext)).thenReturn(skinDoc);
        when(skinDoc.getXObjectSize(eq(new DocumentReference("wikiId", "XWiki", "XWikiSkins")))).thenReturn(1);

        // Test
        SkinReference skinReference = mocker.getComponentUnderTest().createReference("XWiki.MySkin");

        // Verify
        assertTrue(skinReference instanceof DocumentSkinReference);
        DocumentSkinReference docSkinRef = (DocumentSkinReference) skinReference;
        assertEquals(skinDocRef, docSkinRef.getSkinDocument());
        assertEquals("SkinDocument[wikiId:XWiki.MySkin]", docSkinRef.toString());
    }

    @Test
    public void createReferenceWhenSkinOnDBWithException() throws Exception
    {
        // Mocks
        when(wikiDescriptorManager.getCurrentWikiId()).thenReturn("wikiId");
        DocumentReference skinDocRef = new DocumentReference("wikiId", "XWiki", "MySkin");
        when(documentReferenceResolver.resolve(eq("XWiki.MySkin"), eq(new WikiReference("wikiId"))))
                .thenReturn(skinDocRef);
        when(xwiki.exists(skinDocRef, xcontext)).thenReturn(true);
        Exception exception = new XWikiException();
        when(xwiki.getDocument(skinDocRef, xcontext)).thenThrow(exception);

        // Test
        LESSCompilerException caughtException = null;
        try {
            mocker.getComponentUnderTest().createReference("XWiki.MySkin");
        } catch(LESSCompilerException e) {
            caughtException = e;
        }

        // Verify
        assertNotNull(caughtException);
        assertEquals(exception, caughtException.getCause());
        assertEquals("Unable to read document [wikiId:XWiki.MySkin]", caughtException.getMessage());
    }

    @Test
    public void createReferenceWhenSkinOnFS() throws Exception
    {
        // Mocks
        when(wikiDescriptorManager.getCurrentWikiId()).thenReturn("wikiId");
        DocumentReference skinDocRef = new DocumentReference("wikiId", "Main", "Flamingo");
        when(documentReferenceResolver.resolve(eq("flamingo"), eq(new WikiReference("wikiId"))))
                .thenReturn(skinDocRef);
        when(xwiki.exists(skinDocRef, xcontext)).thenReturn(true);
        XWikiDocument skinDoc = mock(XWikiDocument.class);
        when(xwiki.getDocument(skinDocRef, xcontext)).thenReturn(skinDoc);
        when(skinDoc.getXObjectSize(eq(new DocumentReference("wikiId", "XWiki", "XWikiSkins")))).thenReturn(0);

        // Test
        SkinReference skinReference = mocker.getComponentUnderTest().createReference("flamingo");

        // Verify
        assertTrue(skinReference instanceof FSSkinReference);
        FSSkinReference fsSkinRef = (FSSkinReference) skinReference;
        assertEquals("flamingo", fsSkinRef.getSkinName());
        assertEquals("SkinFS[flamingo]", fsSkinRef.toString());
    }
}
