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

import javax.inject.Provider;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.lesscss.compiler.LESSCompiler;
import org.xwiki.lesscss.compiler.LESSCompilerException;
import org.xwiki.lesscss.resources.LESSResourceReader;
import org.xwiki.lesscss.resources.LESSSkinFileResourceReference;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.web.XWikiEngineContext;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

/**
 * @since 6.4M2
 * @version $Id$
 */
public class CachedIntegratedLESSCompilerTest
{
    @Rule
    public MockitoComponentMockingRule<CachedIntegratedLESSCompiler> mocker =
            new MockitoComponentMockingRule<>(CachedIntegratedLESSCompiler.class);

    private Provider<XWikiContext> xcontextProvider;

    private LESSCompiler lessCompiler;

    private SkinDirectoryGetter skinDirectoryGetter;

    private LESSResourceReader lessResourceReader;

    private XWikiContext xcontext;

    private XWiki xwiki;

    private XWikiEngineContext engineContext;

    @Before
    public void setUp() throws Exception
    {
        lessCompiler = mocker.getInstance(LESSCompiler.class);
        skinDirectoryGetter = mocker.getInstance(SkinDirectoryGetter.class);
        lessResourceReader = mocker.getInstance(LESSResourceReader.class);
        xcontextProvider = mocker.registerMockComponent(XWikiContext.TYPE_PROVIDER);
        xcontext = mock(XWikiContext.class);
        when(xcontextProvider.get()).thenReturn(xcontext);
        xwiki = mock(XWiki.class);
        when(xcontext.getWiki()).thenReturn(xwiki);
        engineContext = mock(XWikiEngineContext.class);
        when(xwiki.getEngineContext()).thenReturn(engineContext);
        when(xwiki.getSkin(xcontext)).thenReturn("skin");
    }

    @Test
    public void computeSkinFile() throws Exception
    {
        // Mocks
        LESSSkinFileResourceReference resource = new LESSSkinFileResourceReference("file");

        Path path = mock(Path.class);
        when(skinDirectoryGetter.getLESSSkinFilesDirectory("skin2")).thenReturn(path);
        when(lessResourceReader.getContent(eq(resource), eq("skin2"))).thenReturn("Some LESS content");
        when(xwiki.parseContent(eq("Some LESS content"), eq(xcontext))).
            thenReturn("Some Velocity-rendered LESS content");
        when(lessCompiler.compile(eq("Some Velocity-rendered LESS content"), any(Path[].class))).thenReturn("output");

        // Tests
        assertEquals("output", mocker.getComponentUnderTest().compute(resource, false, true, true, "skin2"));

        // Verify
        verify(xcontext, times(1)).put("skin", "skin2");
        verify(xcontext, times(1)).put("skin", "skin");
    }

    @Test
    public void computeSkinFileWithoutVelocity() throws Exception
    {
        // Mocks
        LESSSkinFileResourceReference resource = new LESSSkinFileResourceReference("file");

        Path path = mock(Path.class);
        when(skinDirectoryGetter.getLESSSkinFilesDirectory("skin2")).thenReturn(path);
        when(lessResourceReader.getContent(eq(resource), eq("skin2"))).thenReturn("Some LESS content");
        when(lessCompiler.compile(eq("Some LESS content"), any(Path[].class))).thenReturn("output");

        // Tests
        assertEquals("output", mocker.getComponentUnderTest().compute(resource, false, false, true, "skin2"));

        // Verify
        verify(xcontext, never()).put(eq("skin"), anyString());
    }

    @Test
    public void computeSkinFileWithoutLESS() throws Exception
    {
        // Mocks
        LESSSkinFileResourceReference resource = new LESSSkinFileResourceReference("file");

        Path path = mock(Path.class);
        when(skinDirectoryGetter.getLESSSkinFilesDirectory("skin2")).thenReturn(path);
        when(lessResourceReader.getContent(eq(resource), eq("skin2"))).thenReturn("Some LESS content");
        when(xwiki.parseContent(eq("Some LESS content"), eq(xcontext))).
                thenReturn("Some Velocity-rendered LESS content");
        
        // Tests
        assertEquals("Some Velocity-rendered LESS content", mocker.getComponentUnderTest().compute(resource, false, 
            true, false, "skin2"));
        
        // Verify that the LESS compiler is never called
        verifyZeroInteractions(lessCompiler);
    }

    @Test
    public void computeSkinFileWithMainStyleIncluded() throws Exception
    {
        // Mocks
        LESSSkinFileResourceReference resource = new LESSSkinFileResourceReference("file");

        Path path = mock(Path.class);
        when(skinDirectoryGetter.getLESSSkinFilesDirectory("skin")).thenReturn(path);

        // Main style
        when(lessResourceReader.getContent(eq(new LESSSkinFileResourceReference("style.less.vm")), eq("skin"))).
                thenReturn("LESS code from main file");
        when(xwiki.parseContent(eq("LESS code from main file"), eq(xcontext))).
                thenReturn("Velocity-rendered main style");

        // Resource
        when(lessResourceReader.getContent(eq(resource), eq("skin"))).thenReturn("Some LESS content");
        when(xwiki.parseContent(eq("@import (reference) \"style.less.vm\";\n.realStartOfXWikiSSX{color:#000}"
            + "Some LESS content"), eq(xcontext)))
                .thenReturn("@import (reference) \"style.less.vm\";\n.realStartOfXWikiSSX{color:#000}"
                        +"Some Velocity-rendered LESS content");
        when(lessCompiler.compile(eq("@import (reference) \"style.less.vm\";\n.realStartOfXWikiSSX{color:#000}"
            +"Some Velocity-rendered LESS content"),any(Path[].class)))
                .thenReturn(".realStartOfXWikiSSX{color:#000}output");

        // Tests
        assertEquals("output", mocker.getComponentUnderTest().compute(resource, true, true, true, "skin"));

    }

    @Test
    public void computeSkinFileWhenException() throws Exception
    {
        // Mocks
        LESSSkinFileResourceReference resource = new LESSSkinFileResourceReference("file");

        Path path = mock(Path.class);
        when(skinDirectoryGetter.getLESSSkinFilesDirectory("skin2")).thenReturn(path);
        when(lessResourceReader.getContent(eq(resource), eq("skin2"))).thenReturn("Some LESS content");
        when(xwiki.parseContent(eq("Some LESS content"), eq(xcontext))).
                thenReturn("Some Velocity-rendered LESS content");
        LESSCompilerException lessCompilerException = new LESSCompilerException("error");
        when(lessCompiler.compile(eq("Some Velocity-rendered LESS content"), any(Path[].class))).
            thenThrow(lessCompilerException);

        // Tests
        LESSCompilerException caughtException = null;
        try {
            mocker.getComponentUnderTest().compute(resource, false, true, true, "skin2");
        } catch(LESSCompilerException e) {
            caughtException = e;
        }

        // Verify
        assertNotNull(caughtException);
        assertEquals(lessCompilerException, caughtException.getCause());
        assertEquals("Failed to compile the resource [LessSkinFileResourceReference[file]] with LESS.", caughtException.getMessage());

    }
}
