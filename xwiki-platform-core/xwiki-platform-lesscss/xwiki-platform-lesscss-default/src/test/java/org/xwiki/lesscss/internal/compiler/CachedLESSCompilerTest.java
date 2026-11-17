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
import org.xwiki.lesscss.resources.WikiLESSResourceReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.template.Template;
import org.xwiki.template.TemplateContent;
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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
        when(this.lessConfiguration.getMaximumSimultaneousCompilations()).thenReturn(1);
        when(this.lessConfiguration.isGenerateInlineSourceMaps()).thenReturn(false);
    }

    @BeforeEach
    void beforeEach()
    {
        this.xcontext = mock(XWikiContext.class);
        when(this.xcontextProvider.get()).thenReturn(this.xcontext);
        this.xwiki = mock(XWiki.class);
        when(this.xcontext.getWiki()).thenReturn(this.xwiki);
        this.engineContext = mock(XWikiEngineContext.class);
        when(this.xwiki.getEngineContext()).thenReturn(this.engineContext);
        when(this.xwiki.getSkin(this.xcontext)).thenReturn("skin");

        this.template = mock(Template.class);
    }

    void mockTemplateExecution(LESSResourceReference resource, String input, String result) throws Exception
    {
        mockTemplateExecution(resource, input, result, InternalTemplateManager.SUPERADMIN_REFERENCE, null);
    }

    void mockTemplateExecution(LESSResourceReference resource, String input, String result, DocumentReference author,
        DocumentReference document) throws Exception
    {
        when(this.templateManager.createStringTemplate(resource.toString(), input, author, document))
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
        LESSResourceReference resource = mockSkinFile("skin2", "Some LESS content");
        mockTemplateExecution(resource, "Some LESS content", "Some Velocity-rendered LESS content");
        when(this.less4jCompiler.compile("Some Velocity-rendered LESS content", "skin2", false)).thenReturn("output");

        // Tests
        assertEquals("output", this.cachedCompiler.compute(resource, false, true, true, "skin2"));

        // Verify
        verify(this.xcontext).put("skin", "skin2");
        verify(this.xcontext).put("skin", "skin");
    }

    @Test
    void computeSkinFileWithoutVelocity() throws Exception
    {
        // Mocks
        LESSResourceReference resource = mockSkinFile("skin2", "Some LESS content");
        when(this.less4jCompiler.compile("Some LESS content", "skin2", false)).thenReturn("output");

        // Tests
        assertEquals("output", this.cachedCompiler.compute(resource, false, false, true, "skin2"));

        // Verify
        verify(this.xcontext, never()).put(eq("skin"), any());
    }

    @Test
    void computeSkinFileWithoutLESS() throws Exception
    {
        // Mocks
        LESSResourceReference resource = mockSkinFile("skin2", "Some LESS content");
        mockTemplateExecution(resource, "Some LESS content", "Some Velocity-rendered LESS content");

        // Tests
        assertEquals("Some Velocity-rendered LESS content",
            this.cachedCompiler.compute(resource, false, true, false, "skin2"));

        // Verify that the LESS compiler is never called
        verifyNoInteractions(this.less4jCompiler);
    }

    @Test
    void computeSkinFileWithMainStyleIncluded() throws Exception
    {
        // Mocks
        LESSResourceReference resource = mockSkinFile("skin", "Some LESS content");
        mockTemplateExecution(resource, "@import (reference) \"style.less.vm\";\nSome LESS content",
            "@import (reference) \"style.less.vm\";\nSome Velocity-rendered LESS content");
        when(this.less4jCompiler.compile("@import (reference) \"style.less.vm\";\nSome Velocity-rendered LESS content",
            "skin", false)).thenReturn("output");

        // Tests
        assertEquals("output", this.cachedCompiler.compute(resource, true, true, true, "skin"));
    }

    @Test
    void computeSkinFileWhenException() throws Exception
    {
        // Mocks
        LESSResourceReference resource = mockSkinFile("skin", "Some LESS content");
        mockTemplateExecution(resource, "Some LESS content", "Some Velocity-rendered LESS content");
        Less4jException lessCompilerException = mock(Less4jException.class);
        when(this.less4jCompiler.compile("Some Velocity-rendered LESS content", "skin", false))
            .thenThrow(lessCompilerException);

        // Tests
        LESSCompilerException caughtException = null;
        try {
            this.cachedCompiler.compute(resource, false, true, true, "skin");
        } catch (LESSCompilerException e) {
            caughtException = e;
        }

        // Verify
        assertNotNull(caughtException);
        assertEquals(lessCompilerException, caughtException.getCause());
        assertEquals("Failed to compile the resource [" + resource + "] with LESS.",
            caughtException.getMessage());
    }

    @Test
    void computeWithWikiLESSResourceReference() throws Exception
    {
        WikiLESSResourceReference mockWikiLESSResourceReference = mock(WikiLESSResourceReference.class);

        DocumentReference authorReference = new DocumentReference("xwiki", "Space", "User");
        DocumentReference documentReference = new DocumentReference("xwiki", "Space", "Page");

        when(mockWikiLESSResourceReference.getAuthorReference())
            .thenReturn(authorReference);
        when(mockWikiLESSResourceReference.getDocumentReference())
            .thenReturn(documentReference);
        mockStringTemplate(mockWikiLESSResourceReference, "", authorReference, documentReference);

        this.cachedCompiler.compute(mockWikiLESSResourceReference, false, true, false, "skin");

        verify(mockWikiLESSResourceReference).getAuthorReference();
        verify(mockWikiLESSResourceReference).getDocumentReference();
        verify(mockWikiLESSResourceReference, never()).getContent(any());
        // One template holds the content of the resource and the other one evaluates it.
        verify(this.templateManager, times(2)).createStringTemplate(mockWikiLESSResourceReference.toString(), "",
            authorReference, documentReference);
    }

    @Test
    void computeWikiSkinFileUsesTemplateAuthor() throws Exception
    {
        DocumentReference authorReference = new DocumentReference("xwiki", "XWiki", "Author");
        DocumentReference documentReference = new DocumentReference("xwiki", "Sandbox", "StdSkin");
        LESSResourceReference resource =
            mockSkinFile("Sandbox.StdSkin", "Some LESS content", authorReference, documentReference);
        mockTemplateExecution(resource, "Some LESS content", "Some Velocity-rendered LESS content", authorReference,
            documentReference);

        assertEquals("Some Velocity-rendered LESS content",
            this.cachedCompiler.compute(resource, false, true, false, "Sandbox.StdSkin"));

        verify(this.templateManager).createStringTemplate(resource.toString(), "Some LESS content", authorReference,
            documentReference);
        verify(this.templateManager, never()).createStringTemplate(any(), any(),
            eq(InternalTemplateManager.SUPERADMIN_REFERENCE), any());
    }

    @Test
    void computeSkinFileResolvesTemplateOnce() throws Exception
    {
        LESSSkinFileResourceReference resource = mockSkinFile("skin", "Some LESS content");
        mockTemplateExecution(resource, "@import (reference) \"style.less.vm\";" + System.lineSeparator()
            + "Some LESS content", "rendered");

        assertEquals("rendered", this.cachedCompiler.compute(resource, true, true, false, "skin"));

        verify(resource).getTemplateContent("skin");
        verify(resource, never()).getContent(any());
    }

    @Test
    void computeSkinFileWhenTemplateCannotBeResolved() throws Exception
    {
        LESSSkinFileResourceReference resource = mock(LESSSkinFileResourceReference.class);
        LESSCompilerException resolutionException = new LESSCompilerException("error");
        when(resource.getTemplateContent("skin")).thenThrow(resolutionException);

        LESSCompilerException exception =
            assertThrows(LESSCompilerException.class, () -> this.cachedCompiler.compute(resource, false, true, true,
                "skin"));

        assertSame(resolutionException, exception.getCause());
        assertEquals("Failed to compile the resource [" + resource + "] with LESS.", exception.getMessage());
        verify(this.templateManager, never()).createStringTemplate(any(), any(), any(), any());
    }

    @Test
    void computeUnknownResourceWithoutAuthor() throws Exception
    {
        LESSResourceReference resource = mock(LESSResourceReference.class);
        when(resource.getContent("skin")).thenReturn("Some LESS content");
        mockStringTemplate(resource, "Some LESS content", null, null);
        mockTemplateExecution(resource, "@import (reference) \"style.less.vm\";" + System.lineSeparator()
            + "Some LESS content", "rendered", null, null);

        assertEquals("rendered", this.cachedCompiler.compute(resource, true, true, false, "skin"));
    }

    private LESSSkinFileResourceReference mockSkinFile(String skin, String content) throws Exception
    {
        return mockSkinFile(skin, content, InternalTemplateManager.SUPERADMIN_REFERENCE, null);
    }

    private LESSSkinFileResourceReference mockSkinFile(String skin, String content, DocumentReference author,
        DocumentReference document) throws Exception
    {
        LESSSkinFileResourceReference resource = mock(LESSSkinFileResourceReference.class);
        TemplateContent templateContent = mockTemplateContent(content, author, document);
        when(resource.getTemplateContent(skin)).thenReturn(templateContent);
        return resource;
    }

    private void mockStringTemplate(LESSResourceReference resource, String content, DocumentReference author,
        DocumentReference document) throws Exception
    {
        Template stringTemplate = mock(Template.class);
        TemplateContent templateContent = mockTemplateContent(content, author, document);
        when(stringTemplate.getContent()).thenReturn(templateContent);
        when(this.templateManager.createStringTemplate(resource.toString(), content, author, document))
            .thenReturn(stringTemplate);
    }

    private TemplateContent mockTemplateContent(String content, DocumentReference author, DocumentReference document)
    {
        TemplateContent templateContent = mock(TemplateContent.class);
        when(templateContent.getContent()).thenReturn(content);
        when(templateContent.getAuthorReference()).thenReturn(author);
        when(templateContent.getDocumentReference()).thenReturn(document);
        return templateContent;
    }
}
