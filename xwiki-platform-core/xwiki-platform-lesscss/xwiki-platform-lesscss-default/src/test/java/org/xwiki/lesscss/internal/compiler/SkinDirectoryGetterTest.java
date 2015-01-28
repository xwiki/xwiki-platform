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
package org.xwiki.lesscss.internal.compiler;

import java.nio.file.Path;
import java.nio.file.Paths;

import javax.inject.Provider;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.lesscss.compiler.LESSCompilerException;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.test.mockito.MockitoComponentMockingRule;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.web.XWikiEngineContext;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @since 6.4M2
 * @version $Id$
 */
public class SkinDirectoryGetterTest
{
    @Rule
    public MockitoComponentMockingRule<SkinDirectoryGetter> mocker =
            new MockitoComponentMockingRule<>(SkinDirectoryGetter.class);

    private WikiDescriptorManager wikiDescriptorManager;

    private Provider<XWikiContext> xcontextProvider;

    private DocumentReferenceResolver<String> referenceResolver;

    private XWikiContext xcontext;

    private XWiki xwiki;

    private XWikiEngineContext engineContext;

    @Before
    public void setUp() throws Exception
    {
        wikiDescriptorManager = mocker.getInstance(WikiDescriptorManager.class);
        referenceResolver = mocker.getInstance(new DefaultParameterizedType(null, DocumentReferenceResolver.class,
                String.class));
        xcontextProvider = mocker.registerMockComponent(XWikiContext.TYPE_PROVIDER);
        xcontext = mock(XWikiContext.class);
        when(xcontextProvider.get()).thenReturn(xcontext);
        xwiki = mock(XWiki.class);
        when(xcontext.getWiki()).thenReturn(xwiki);

        when(wikiDescriptorManager.getCurrentWikiId()).thenReturn("wikiId");

        engineContext = mock(XWikiEngineContext.class);
        when(xwiki.getEngineContext()).thenReturn(engineContext);
    }

    @Test
    public void getSkinDirectoryWhenSkinOnDB() throws Exception
    {
        DocumentReference skinDocRef = new DocumentReference("wikiId", "XWiki", "DefaultSkin");
        WikiReference mainWikiReference = new WikiReference("wikiId");
        when(referenceResolver.resolve(eq("XWiki.DefaultSkin"), eq(mainWikiReference))).thenReturn(skinDocRef);
        when(xwiki.exists(skinDocRef, xcontext)).thenReturn(true);
        DocumentReference skinClassRef = new DocumentReference("wikiId", "XWiki", "XWikiSkins");
        XWikiDocument skinDoc = mock(XWikiDocument.class);
        when(xwiki.getDocument(eq(skinDocRef), eq(xcontext))).thenReturn(skinDoc);
        BaseObject skinObj = mock(BaseObject.class);
        when(skinDoc.getXObject(eq(skinClassRef))).thenReturn(skinObj);
        when(skinObj.getStringValue(eq("baseskin"))).thenReturn("skinOnFs");

        DocumentReference wrongSkinDocRef = new DocumentReference("wikiId", "Main", "skinOnFs");
        when(referenceResolver.resolve(eq("skinOnFs"), eq(mainWikiReference))).thenReturn(wrongSkinDocRef);
        when(xwiki.exists(wrongSkinDocRef, xcontext)).thenReturn(false);

        // Test
        assertEquals("/skins/skinOnFs", mocker.getComponentUnderTest().getSkinDirectory("XWiki.DefaultSkin"));
    }

    @Test
    public void getSkinDirectoryWhenSkinIsOnDBWithBaseSkinLoop() throws Exception
    {
        // Mocks
        DocumentReference skinDocRef = new DocumentReference("wikiId", "XWiki", "DefaultSkin");
        WikiReference wikiReference = new WikiReference("wikiId");
        when(referenceResolver.resolve(eq("XWiki.DefaultSkin"), eq(wikiReference))).thenReturn(skinDocRef);
        when(xwiki.exists(skinDocRef, xcontext)).thenReturn(true);
        DocumentReference skinClassRef = new DocumentReference("wikiId", "XWiki", "XWikiSkins");
        XWikiDocument skinDoc = mock(XWikiDocument.class);
        when(xwiki.getDocument(eq(skinDocRef), eq(xcontext))).thenReturn(skinDoc);
        BaseObject skinObj = mock(BaseObject.class);
        when(skinDoc.getXObject(eq(skinClassRef))).thenReturn(skinObj);
        when(skinObj.getStringValue(eq("baseskin"))).thenReturn("XWiki.DefaultSkin2");

        DocumentReference skinDocRef2 = new DocumentReference("wikiId", "XWiki", "DefaultSkin2");
        when(referenceResolver.resolve(eq("XWiki.DefaultSkin2"), eq(wikiReference))).thenReturn(skinDocRef2);
        when(xwiki.exists(skinDocRef2, xcontext)).thenReturn(true);
        XWikiDocument skinDoc2 = mock(XWikiDocument.class);
        when(xwiki.getDocument(eq(skinDocRef2), eq(xcontext))).thenReturn(skinDoc2);
        BaseObject skinObj2 = mock(BaseObject.class);
        when(skinDoc2.getXObject(eq(skinClassRef))).thenReturn(skinObj2);
        when(skinObj2.getStringValue(eq("baseskin"))).thenReturn("XWiki.DefaultSkin");

        // Test
        Exception exception = null;
        try {
            mocker.getComponentUnderTest().getSkinDirectory("XWiki.DefaultSkin");
        } catch(Exception e) {
            exception = e;
        }

        // Verify
        assertNotNull(exception);
        assertEquals("Infinite loop of 'baseskin' dependencies [[XWiki.DefaultSkin, XWiki.DefaultSkin2]].",
                exception.getMessage());
    }

    @Test
    public void getSkinDirectoryWhenSkinIsOnDBWithNoBaseSkin() throws Exception
    {
        // Mocks
        DocumentReference skinDocRef = new DocumentReference("wikiId", "XWiki", "DefaultSkin");
        WikiReference wikiReference = new WikiReference("wikiId");
        when(referenceResolver.resolve(eq("XWiki.DefaultSkin"), eq(wikiReference))).thenReturn(skinDocRef);
        when(xwiki.exists(skinDocRef, xcontext)).thenReturn(true);
        DocumentReference skinClassRef = new DocumentReference("wikiId", "XWiki", "XWikiSkins");
        XWikiDocument skinDoc = mock(XWikiDocument.class);
        when(xwiki.getDocument(eq(skinDocRef), eq(xcontext))).thenReturn(skinDoc);
        BaseObject skinObj = mock(BaseObject.class);
        when(skinDoc.getXObject(eq(skinClassRef))).thenReturn(skinObj);
        when(skinObj.getStringValue(eq("baseskin"))).thenReturn(" ");

        // Test
        Exception exception = null;
        try {
            mocker.getComponentUnderTest().getSkinDirectory("XWiki.DefaultSkin");
        } catch(Exception e) {
            exception = e;
        }

        // Verify
        assertNotNull(exception);
        assertEquals("Failed to get the base skin of the skin [XWiki.DefaultSkin].", exception.getMessage());
    }

    @Test
    public void getSkinDirectoryWhenSkinDocumentHasNoObject() throws Exception
    {
        // Mocks
        DocumentReference skinDocRef = new DocumentReference("wikiId", "Main", "flamingo");
        WikiReference wikiReference = new WikiReference("wikiId");
        when(referenceResolver.resolve(eq("flamingo"), eq(wikiReference))).thenReturn(skinDocRef);
        when(xwiki.exists(skinDocRef, xcontext)).thenReturn(true);
        XWikiDocument skinDoc = mock(XWikiDocument.class);
        when(xwiki.getDocument(eq(skinDocRef), eq(xcontext))).thenReturn(skinDoc);

        // Test
        assertEquals("/skins/flamingo", mocker.getComponentUnderTest().getSkinDirectory("flamingo"));
    }

    @Test
    public void getSkinDirectoryWhenSkinOnDBAndException() throws Exception
    {
        DocumentReference skinDocRef = new DocumentReference("wikiId", "XWiki", "DefaultSkin");
        WikiReference mainWikiReference = new WikiReference("wikiId");
        when(referenceResolver.resolve(eq("XWiki.DefaultSkin"), eq(mainWikiReference))).thenReturn(skinDocRef);
        when(xwiki.exists(skinDocRef, xcontext)).thenReturn(true);

        XWikiException exception = new XWikiException();
        when(xwiki.getDocument(eq(skinDocRef), eq(xcontext))).thenThrow(exception);

        // Test
        Exception exceptionCaught = null;
        try {
            mocker.getComponentUnderTest().getLESSSkinFilesDirectory("XWiki.DefaultSkin");
        } catch(LESSCompilerException e) {
            exceptionCaught = e;
        }

        // Verify
        assertNotNull(exceptionCaught);
        assertEquals("Failed to get the document [wikiId:XWiki.DefaultSkin].",
                exceptionCaught.getMessage());
    }

    @Test
    public void getLESSSkinFilesDirectory() throws Exception
    {
        // Mocks
        DocumentReference wrongSkinDocRef = new DocumentReference("wikiId", "Main", "skin");
        WikiReference mainWikiReference = new WikiReference("wikiId");
        when(referenceResolver.resolve(eq("skinOnFs"), eq(mainWikiReference))).thenReturn(wrongSkinDocRef);
        when(xwiki.exists(wrongSkinDocRef, xcontext)).thenReturn(false);

        when(engineContext.getRealPath("/skins/skinOnFs/less")).thenReturn(getClass().getResource("/").getPath());
        when(engineContext.getRealPath("/skins/skinOnFs/less/style2.less")).thenReturn(
                getClass().getResource("/style2.less").getPath());

        // Test
        Path expectedPath = Paths.get(getClass().getResource("/").getPath());
        assertEquals(expectedPath, mocker.getComponentUnderTest().getLESSSkinFilesDirectory("skinOnFs"));
    }

    @Test
    public void getLESSSkinFilesDirectoryWhenDirectoryDoesNotExist() throws Exception
    {
        // Mocks
        DocumentReference wrongSkinDocRef = new DocumentReference("wikiId", "Main", "skin");
        WikiReference mainWikiReference = new WikiReference("wikiId");
        when(referenceResolver.resolve(eq("skinOnFs"), eq(mainWikiReference))).thenReturn(wrongSkinDocRef);
        when(xwiki.exists(wrongSkinDocRef, xcontext)).thenReturn(false);
        when(engineContext.getRealPath("/skins/skinOnFs/less")).thenReturn("ighgzuheubigvugvbzekvbzekvuzkkkhguiiiii");

        // Test
        Exception exceptionCaught = null;
        try {
            mocker.getComponentUnderTest().getLESSSkinFilesDirectory("skinOnFs");
        } catch(LESSCompilerException e) {
            exceptionCaught = e;
        }

        // Verify
        assertNotNull(exceptionCaught);
        assertEquals("The path [/skins/skinOnFs/less] is not a directory or does not exists.",
                exceptionCaught.getMessage());
    }


}
