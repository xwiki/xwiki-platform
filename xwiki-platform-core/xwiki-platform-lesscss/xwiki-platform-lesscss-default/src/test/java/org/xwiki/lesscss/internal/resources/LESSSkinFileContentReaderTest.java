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
package org.xwiki.lesscss.internal.resources;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.inject.Provider;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.lesscss.compiler.LESSCompilerException;
import org.xwiki.lesscss.resources.LESSResourceReference;
import org.xwiki.lesscss.resources.LESSSkinFileResourceReference;
import org.xwiki.lesscss.internal.compiler.SkinDirectoryGetter;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.web.XWikiEngineContext;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * @since 6.4M2
 * @version $Id$
 */
public class LESSSkinFileContentReaderTest
{
    @Rule
    public MockitoComponentMockingRule<LESSSkinFileReader> mocker =
            new MockitoComponentMockingRule<>(LESSSkinFileReader.class);

    private Provider<XWikiContext> xcontextProvider;

    private SkinDirectoryGetter skinDirectoryGetter;

    private XWikiContext xcontext;

    private XWiki xwiki;

    private XWikiEngineContext engineContext;

    @Before
    public void setUp() throws Exception
    {
        skinDirectoryGetter = mocker.getInstance(SkinDirectoryGetter.class);
        xcontextProvider = mocker.registerMockComponent(XWikiContext.TYPE_PROVIDER);
        xcontext = mock(XWikiContext.class);
        when(xcontextProvider.get()).thenReturn(xcontext);
        xwiki = mock(XWiki.class);
        when(xcontext.getWiki()).thenReturn(xwiki);
        engineContext = mock(XWikiEngineContext.class);
        when(xwiki.getEngineContext()).thenReturn(engineContext);
    }

    @Test
    public void getContent() throws Exception
    {
        // Mocks
        when(skinDirectoryGetter.getSkinDirectory("skin")).thenReturn("skins/skin");
        when(engineContext.getRealPath("skins/skin/less/style2.less")).thenReturn(
                getClass().getResource("/style2.less").getPath());
        FileInputStream is = new FileInputStream(getClass().getResource("/style2.less").getFile());
        when(engineContext.getResourceAsStream("skins/skin/less/style2.less")).thenReturn(is);

        // Test
        assertEquals("// My LESS file", mocker.getComponentUnderTest().getContent(
                new LESSSkinFileResourceReference("style2.less"), "skin"));
    }

    @Test
    public void getContentWithUnsupportedResource() throws Exception
    {
        // Test
        LESSCompilerException caughtException = null;
        try {
            mocker.getComponentUnderTest().getContent(new LESSResourceReference(){}, "skin");
        } catch (LESSCompilerException e) {
            caughtException = e;
        }

        // Verify
        assertNotNull(caughtException);
        assertEquals("Invalid LESS resource type.", caughtException.getMessage());
    }

    @Test
    public void getContentWhenFileDoesNotExist() throws Exception
    {
        // Mocks
        when(skinDirectoryGetter.getSkinDirectory("skin")).thenReturn("skins/skin");
        when(engineContext.getRealPath("skins/skin/less/not-existing-file.less")).
                thenReturn(getClass().getResource("/").getPath() + "not-existing-file.less");

        // Test
        LESSCompilerException caughtException = null;
        try {
            mocker.getComponentUnderTest().getContent(
                    new LESSSkinFileResourceReference("not-existing-file.less"), "skin");
        } catch (LESSCompilerException e) {
            caughtException = e;
        }

        // Verify
        assertNotNull(caughtException);
        assertEquals("The path [skins/skin/less/not-existing-file.less] is not a file or does not exists.",
            caughtException.getMessage());
    }

    @Test
    public void getContentWhenExceptionWhileReadingFile() throws Exception
    {
        // Mocks
        when(skinDirectoryGetter.getSkinDirectory("skin")).thenReturn(
                "skins/skin");

        InputStream is = mock(InputStream.class);
        when(engineContext.getRealPath("skins/skin/less/style2.less")).
                thenReturn(getClass().getResource("/style2.less").getPath());
        when(engineContext.getResourceAsStream("skins/skin/less/style2.less")).thenReturn(is);
        IOException exception = new IOException("Test Exception");

        // risky mock: it depends on the implementation of IOUtils.copy()
        when(is.read(any(byte[].class))).thenThrow(exception);

        // Test
        LESSCompilerException caughtException = null;
        try {
            mocker.getComponentUnderTest().getContent(
                    new LESSSkinFileResourceReference("style2.less"), "skin");
        } catch (LESSCompilerException e) {
            caughtException = e;
        }

        // Verify
        assertNotNull(caughtException);
        assertEquals("Error while reading the file [skins/skin/less/style2.less].", caughtException.getMessage());
    }

}
