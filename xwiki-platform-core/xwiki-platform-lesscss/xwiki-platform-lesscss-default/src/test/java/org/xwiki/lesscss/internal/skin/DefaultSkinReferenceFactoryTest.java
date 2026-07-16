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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.lesscss.compiler.LESSCompilerException;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @version $Id$
 * @since 6.4RC1
 */
@ComponentTest
class DefaultSkinReferenceFactoryTest
{
    @InjectMockComponents
    private DefaultSkinReferenceFactory defaultSkinReferenceFactory;

    @MockComponent
    private Provider<XWikiContext> xcontextProvider;

    @MockComponent
    private DocumentReferenceResolver<String> documentReferenceResolver;

    @MockComponent
    private WikiDescriptorManager wikiDescriptorManager;

    @Mock
    private XWikiContext xcontext;

    @Mock
    private XWiki xwiki;

    @BeforeEach
    void setUp()
    {
        when(this.xcontextProvider.get()).thenReturn(this.xcontext);
        when(this.xcontext.getWiki()).thenReturn(this.xwiki);
    }

    @Test
    void createReferenceWhenSkinOnDB() throws Exception
    {
        // Mocks
        when(this.wikiDescriptorManager.getCurrentWikiId()).thenReturn("wikiId");
        DocumentReference skinDocRef = new DocumentReference("wikiId", "XWiki", "MySkin");
        when(this.documentReferenceResolver.resolve("XWiki.MySkin", new WikiReference("wikiId")))
            .thenReturn(skinDocRef);
        when(this.xwiki.exists(skinDocRef, this.xcontext)).thenReturn(true);
        XWikiDocument skinDoc = mock(XWikiDocument.class);
        when(this.xwiki.getDocument(skinDocRef, this.xcontext)).thenReturn(skinDoc);
        when(skinDoc.getXObjectSize(new DocumentReference("wikiId", "XWiki", "XWikiSkins"))).thenReturn(1);

        // Test
        SkinReference skinReference = this.defaultSkinReferenceFactory.createReference("XWiki.MySkin");

        // Verify
        assertInstanceOf(DocumentSkinReference.class, skinReference);
        DocumentSkinReference docSkinRef = (DocumentSkinReference) skinReference;
        assertEquals(skinDocRef, docSkinRef.getSkinDocument());
    }

    @Test
    void createReferenceWhenSkinOnDBWithException() throws Exception
    {
        // Mocks
        when(this.wikiDescriptorManager.getCurrentWikiId()).thenReturn("wikiId");
        DocumentReference skinDocRef = new DocumentReference("wikiId", "XWiki", "MySkin");
        when(this.documentReferenceResolver.resolve("XWiki.MySkin", new WikiReference("wikiId")))
            .thenReturn(skinDocRef);
        when(this.xwiki.exists(skinDocRef, this.xcontext)).thenReturn(true);
        Exception exception = new XWikiException();
        when(this.xwiki.getDocument(skinDocRef, this.xcontext)).thenThrow(exception);

        // Test
        LESSCompilerException caughtException = null;
        try {
            this.defaultSkinReferenceFactory.createReference("XWiki.MySkin");
        } catch (LESSCompilerException e) {
            caughtException = e;
        }

        // Verify
        assertNotNull(caughtException);
        assertEquals(exception, caughtException.getCause());
        assertEquals("Unable to read document [wikiId:XWiki.MySkin]", caughtException.getMessage());
    }

    @Test
    void createReferenceWhenSkinOnFS() throws Exception
    {
        // Mocks
        when(this.wikiDescriptorManager.getCurrentWikiId()).thenReturn("wikiId");
        DocumentReference skinDocRef = new DocumentReference("wikiId", "Main", "Flamingo");
        when(this.documentReferenceResolver.resolve("flamingo", new WikiReference("wikiId")))
            .thenReturn(skinDocRef);
        when(this.xwiki.exists(skinDocRef, this.xcontext)).thenReturn(true);
        XWikiDocument skinDoc = mock(XWikiDocument.class);
        when(this.xwiki.getDocument(skinDocRef, this.xcontext)).thenReturn(skinDoc);
        when(skinDoc.getXObjectSize(new DocumentReference("wikiId", "XWiki", "XWikiSkins"))).thenReturn(0);

        // Test
        SkinReference skinReference = this.defaultSkinReferenceFactory.createReference("flamingo");

        // Verify
        assertInstanceOf(FSSkinReference.class, skinReference);
        FSSkinReference fsSkinRef = (FSSkinReference) skinReference;
        assertEquals("flamingo", fsSkinRef.getSkinName());
        assertEquals("SkinFS[flamingo]", fsSkinRef.toString());
    }
}
