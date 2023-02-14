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
package org.xwiki.livedata.internal.livetable;

import java.util.function.Supplier;

import javax.inject.Named;
import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mock;
import org.xwiki.livedata.LiveDataQuery;
import org.xwiki.livedata.livetable.LiveDataLivetableException;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.security.authorization.AccessDeniedException;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.template.Template;
import org.xwiki.template.TemplateManager;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;

import ch.qos.logback.classic.Level;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test of {@link LiveTableLiveDataResultsRenderer}.
 *
 * @version $Id$
 * @since 13.10.4
 * @since 14.2RC1
 */
@ComponentTest
class LiveTableLiveDataResultsRendererTest
{
    private static final DocumentReference DOCUMENT_REFERENCE = new DocumentReference("wiki", "Space", "Page");

    private static final String PAGE = "Space.Page";

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    @InjectMockComponents
    private LiveTableLiveDataResultsRenderer resultsRenderer;

    @MockComponent
    @Named("current")
    private DocumentReferenceResolver<String> currentDocumentReferenceResolver;

    @MockComponent
    private ContextualAuthorizationManager authorization;

    @MockComponent
    private LiveTableRequestHandler liveTableRequestHandler;

    @MockComponent
    private TemplateManager templateManager;

    @MockComponent
    private Provider<XWikiContext> xcontextProvider;

    @Mock
    private XWikiContext xcontext;

    @Mock
    private XWiki wiki;

    @Mock
    private XWikiDocument document;

    @BeforeEach
    void setUp() throws Exception
    {
        when(this.currentDocumentReferenceResolver.resolve(PAGE)).thenReturn(DOCUMENT_REFERENCE);
        doNothing().when(this.authorization).checkAccess(Right.VIEW, DOCUMENT_REFERENCE);
        when(this.liveTableRequestHandler.getLiveTableResults(any(), any()))
            .thenAnswer(invocation -> invocation.<Supplier<String>>getArgument(1).get());
        when(this.xcontextProvider.get()).thenReturn(this.xcontext);
        when(this.xcontext.getWiki()).thenReturn(this.wiki);
        when(this.wiki.getDocument(DOCUMENT_REFERENCE, this.xcontext)).thenReturn(this.document);
        when(this.document.isNew()).thenReturn(false);
    }

    @Test
    void getLiveTableResultsFromPage() throws Exception
    {
        String json = "{}";
        when(this.document.getRenderedContent(Syntax.PLAIN_1_0, this.xcontext)).thenReturn(json);
        assertEquals(json, this.resultsRenderer.getLiveTableResultsFromPage(PAGE, new LiveDataQuery()));
        verify(this.templateManager).render("xwikivars.vm");
    }

    @Test
    void getLiveTableResultsFromSubwikiPage() throws Exception
    {
        DocumentReference documentReference = new DocumentReference("subwiki", "XWiki", "Page");
        when(this.currentDocumentReferenceResolver.resolve("subwiki:XWiki.Page")).thenReturn(documentReference);
        doNothing().when(this.authorization).checkAccess(Right.VIEW, documentReference);
        when(this.xcontextProvider.get()).thenReturn(this.xcontext);
        when(this.xcontext.getWiki()).thenReturn(this.wiki);
        when(this.xcontext.getWikiReference()).thenReturn(new WikiReference("currentwiki"));

        XWikiDocument document = mock(XWikiDocument.class);
        when(this.wiki.getDocument(documentReference, this.xcontext)).thenReturn(document);
        when(document.isNew()).thenReturn(false);

        String json = "{}";
        when(document.getRenderedContent(Syntax.PLAIN_1_0, this.xcontext)).thenReturn(json);
        assertEquals(json, this.resultsRenderer.getLiveTableResultsFromPage("subwiki:XWiki.Page", new LiveDataQuery()));
        verify(this.templateManager).render("xwikivars.vm");
        verify(this.xcontext).setWikiReference(new WikiReference("subwiki"));
        verify(this.xcontext).setWikiReference(new WikiReference("currentwiki"));
    }

    @Test
    void getLiveTableResultsFromPageXwikiversFail() throws Exception
    {
        String json = "{}";
        when(this.templateManager.render("xwikivars.vm")).thenThrow(Exception.class);
        when(this.document.getRenderedContent(Syntax.PLAIN_1_0, this.xcontext)).thenReturn(json);
        assertEquals(json, this.resultsRenderer.getLiveTableResultsFromPage(PAGE, new LiveDataQuery()));
        verify(this.templateManager).render("xwikivars.vm");
        assertEquals(1, this.logCapture.size());
        assertEquals("Failed to evaluate [xwikivars.vm] when getting the Livetable results from page [Space.Page]. "
            + "Cause: [Exception: ].", this.logCapture.getMessage(0));
        assertEquals(Level.WARN, this.logCapture.getLogEvent(0).getLevel());
    }

    @Test
    void getLiveTableResultsFromPagePageMissing() throws Exception
    {
        when(this.document.isNew()).thenReturn(true);
        LiveDataQuery query = new LiveDataQuery();
        RuntimeException runtimeException = assertThrows(RuntimeException.class,
            () -> this.resultsRenderer.getLiveTableResultsFromPage(PAGE, query));
        assertEquals(String.format("Page [%s] does not exist.", DOCUMENT_REFERENCE),
            runtimeException.getCause().getMessage());
        assertEquals(LiveDataLivetableException.class, runtimeException.getCause().getClass());
        verify(this.templateManager).render("xwikivars.vm");
        verify(this.document, never()).getRenderedContent(Syntax.PLAIN_1_0, this.xcontext);
    }
    
    @Test
    void getLiveTableResultsFromPageNoViewRight() throws Exception
    {
        doThrow(AccessDeniedException.class).when(this.authorization).checkAccess(Right.VIEW, DOCUMENT_REFERENCE);
        LiveDataQuery query = new LiveDataQuery();
        assertThrows(AccessDeniedException.class,
            () -> this.resultsRenderer.getLiveTableResultsFromPage(PAGE, query));
    }

    @Test
    void getLiveTableResultsFromTemplate() throws Exception
    {
        String templateName = "template.vm";
        String json = "{}";
        when(this.templateManager.getTemplate(templateName)).thenReturn(mock(Template.class));
        when(this.templateManager.render(templateName)).thenReturn(json);
        assertEquals(json, this.resultsRenderer.getLiveTableResultsFromTemplate(templateName, new LiveDataQuery()));
    }

    @Test
    void getLiveTableResultsFromTemplateMissingTemplate() throws Exception
    {
        String templateName = "template.vm";
        LiveDataQuery query = new LiveDataQuery();
        when(this.templateManager.getTemplate(templateName)).thenReturn(null);
        RuntimeException runtimeException = assertThrows(RuntimeException.class,
            () -> this.resultsRenderer.getLiveTableResultsFromTemplate(templateName, query));
        assertEquals(String.format("Template [%s] does not exist.", templateName),
            runtimeException.getCause().getMessage());
        assertEquals(LiveDataLivetableException.class, runtimeException.getCause().getClass());
        verify(this.templateManager, never()).render(templateName);
    }
}
