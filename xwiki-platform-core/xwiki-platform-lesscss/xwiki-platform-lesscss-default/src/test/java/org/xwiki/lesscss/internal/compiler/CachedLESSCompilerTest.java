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

import java.io.Writer;

import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.xwiki.lesscss.compiler.LESSCompilerException;
import org.xwiki.lesscss.internal.LESSConfiguration;
import org.xwiki.lesscss.internal.compiler.less4j.Less4jCompiler;
import org.xwiki.lesscss.internal.resources.LESSSkinFileResourceReference;
import org.xwiki.lesscss.resources.LESSResourceReference;
import org.xwiki.template.Template;
import org.xwiki.template.TemplateManager;
import org.xwiki.test.annotation.AfterComponent;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.github.sommeri.less4j.Less4jException;
import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.internal.template.InternalTemplateManager;
import com.xpn.xwiki.web.XWikiEngineContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Validate {@link CachedLESSCompiler}.
 * 
 * @version $Id$
 */
@ComponentTest
class CachedLESSCompilerTest
{
    @InjectMockComponents
    private CachedLESSCompiler cachedCompiler;

    @MockComponent
    private Provider<XWikiContext> xcontextProvider;

    @MockComponent
    private Less4jCompiler less4jCompiler;

    @MockComponent
    private LESSConfiguration lessConfiguration;

    @MockComponent
    private TemplateManager templateManager;

    private XWikiContext xcontext;

    private XWiki xwiki;

    private XWikiEngineContext engineContext;

    private Template template;

    @AfterComponent
    public void afterComponents()
    {
        when(lessConfiguration.getMaximumSimultaneousCompilations()).thenReturn(1);
        when(lessConfiguration.isGenerateInlineSourceMaps()).thenReturn(false);
    }

    @BeforeEach
    public void beforeEach() throws Exception
    {
        xcontext = mock(XWikiContext.class);
        when(xcontextProvider.get()).thenReturn(xcontext);
        xwiki = mock(XWiki.class);
        when(xcontext.getWiki()).thenReturn(xwiki);
        engineContext = mock(XWikiEngineContext.class);
        when(xwiki.getEngineContext()).thenReturn(engineContext);
        when(xwiki.getSkin(xcontext)).thenReturn("skin");

        this.template = mock(Template.class);
    }

    void mockTemplateExecution(LESSResourceReference resource, String input, String result) throws Exception
    {
        when(this.templateManager.createStringTemplate(resource.toString(), input, InternalTemplateManager.SUPERADMIN_REFERENCE, null))
            .thenReturn(this.template);

        doAnswer(new Answer<Void>()
        {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable
            {
                invocation.<Writer>getArgument(1).write(result);

                return null;
            }
        }).when(this.templateManager).renderNoException(same(this.template), any());
    }

    @Test
    void computeSkinFile() throws Exception
    {
        // Mocks
        LESSResourceReference resource = mock(LESSSkinFileResourceReference.class);
        when(resource.getContent("skin2")).thenReturn("Some LESS content");
        mockTemplateExecution(resource, "Some LESS content", "Some Velocity-rendered LESS content");
        when(less4jCompiler.compile("Some Velocity-rendered LESS content", "skin2", false)).thenReturn("output");

        // Tests
        assertEquals("output", cachedCompiler.compute(resource, false, true, true, "skin2"));

        // Verify
        verify(xcontext, times(1)).put("skin", "skin2");
        verify(xcontext, times(1)).put("skin", "skin");
    }

    @Test
    void computeSkinFileWithoutVelocity() throws Exception
    {
        // Mocks
        LESSResourceReference resource = mock(LESSSkinFileResourceReference.class);
        when(resource.getContent("skin2")).thenReturn("Some LESS content");
        when(less4jCompiler.compile("Some LESS content", "skin2", false)).thenReturn("output");

        // Tests
        assertEquals("output", cachedCompiler.compute(resource, false, false, true, "skin2"));

        // Verify
        verify(xcontext, never()).put(eq("skin"), any());
    }

    @Test
    void computeSkinFileWithoutLESS() throws Exception
    {
        // Mocks
        LESSResourceReference resource = mock(LESSSkinFileResourceReference.class);
        when(resource.getContent("skin2")).thenReturn("Some LESS content");
        mockTemplateExecution(resource, "Some LESS content", "Some Velocity-rendered LESS content");

        // Tests
        assertEquals("Some Velocity-rendered LESS content",
            cachedCompiler.compute(resource, false, true, false, "skin2"));

        // Verify that the LESS compiler is never called
        verifyNoInteractions(less4jCompiler);
    }

    @Test
    void computeSkinFileWithMainStyleIncluded() throws Exception
    {
        // Mocks
        LESSResourceReference resource = mock(LESSSkinFileResourceReference.class);
        when(resource.getContent("skin")).thenReturn("Some LESS content");
        mockTemplateExecution(resource, "@import (reference) \"style.less.vm\";\nSome LESS content",
            "@import (reference) \"style.less.vm\";\nSome Velocity-rendered LESS content");
        when(less4jCompiler.compile("@import (reference) \"style.less.vm\";\nSome Velocity-rendered LESS content",
            "skin", false)).thenReturn("output");

        // Tests
        assertEquals("output", cachedCompiler.compute(resource, true, true, true, "skin"));
    }

    @Test
    void computeSkinFileWhenException() throws Exception
    {
        // Mocks
        LESSResourceReference resource = mock(LESSSkinFileResourceReference.class);
        when(resource.getContent("skin")).thenReturn("Some LESS content");
        mockTemplateExecution(resource, "Some LESS content", "Some Velocity-rendered LESS content");
        Less4jException lessCompilerException = mock(Less4jException.class);
        when(less4jCompiler.compile("Some Velocity-rendered LESS content", "skin", false))
            .thenThrow(lessCompilerException);

        // Tests
        LESSCompilerException caughtException = null;
        try {
            cachedCompiler.compute(resource, false, true, true, "skin");
        } catch (LESSCompilerException e) {
            caughtException = e;
        }

        // Verify
        assertNotNull(caughtException);
        assertEquals(lessCompilerException, caughtException.getCause());
        assertEquals("Failed to compile the resource [" + resource.toString() + "] with LESS.",
            caughtException.getMessage());
    }
}
