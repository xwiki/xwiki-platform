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
package org.xwiki.image.style.internal;

import java.util.List;
import java.util.Set;

import javax.inject.Named;
import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mock;
import org.xwiki.context.ExecutionContext;
import org.xwiki.context.ExecutionContextException;
import org.xwiki.context.ExecutionContextManager;
import org.xwiki.image.style.ImageStyleException;
import org.xwiki.image.style.model.ImageStyle;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryException;
import org.xwiki.query.QueryManager;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

import ch.qos.logback.classic.Level;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test of {@link DefaultImageStyleManager}.
 *
 * @version $Id$
 * @since 14.3RC1
 */
@ComponentTest
class DefaultImageStyleManagerTest
{
    private static final LocalDocumentReference IMAGE_STYLE_CLASS_LOCAL_REFERENCE =
        new LocalDocumentReference(List.of("Image", "Style", "Code"), "ImageStyleClass");

    private static final DocumentReference DOCUMENT_REFERENCE_DOC1 = new DocumentReference("wiki", "space", "doc1");

    private static final DocumentReference DOCUMENT_REFERENCE_DOC2 = new DocumentReference("wiki", "space", "doc2");

    private static final DocumentReference DOCUMENT_REFERENCE_DOCFAIL =
        new DocumentReference("wiki", "space", "docfail");

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    @InjectMockComponents
    private DefaultImageStyleManager manager;

    @MockComponent
    private QueryManager queryManager;

    @MockComponent
    private Provider<XWikiContext> xcontextProvider;

    @MockComponent
    private ExecutionContextManager contextManager;

    @MockComponent
    @Named("current")
    private DocumentReferenceResolver<String> documentReferenceResolver;

    @Mock
    private XWikiContext context;

    @Mock
    private Query query;

    @Mock
    private XWiki wiki;

    @Mock
    private XWikiDocument doc1;

    @Mock
    private XWikiDocument doc2;

    @Mock
    private BaseObject doc1BaseObject;

    @Mock
    private BaseObject doc2BaseObject;

    @BeforeEach
    void setUp() throws Exception
    {
        when(this.xcontextProvider.get()).thenReturn(this.context);
        when(this.queryManager.createQuery("select doc.fullName "
                + "from Document doc, doc.object(Image.Style.Code.ImageStyleClass) as obj "
                + "where doc.space = 'Image.Style.Code.ImageStyles'",
            Query.XWQL)).thenReturn(this.query);
        when(this.query.setWiki(anyString())).thenReturn(this.query);
        when(this.documentReferenceResolver.resolve("doc1")).thenReturn(DOCUMENT_REFERENCE_DOC1);
        when(this.documentReferenceResolver.resolve("doc2")).thenReturn(DOCUMENT_REFERENCE_DOC2);
        when(this.documentReferenceResolver.resolve("docfail")).thenReturn(DOCUMENT_REFERENCE_DOCFAIL);
        when(this.context.getWiki()).thenReturn(this.wiki);
        when(this.wiki.getDocument(DOCUMENT_REFERENCE_DOC1, this.context)).thenReturn(this.doc1);
        when(this.wiki.getDocument(DOCUMENT_REFERENCE_DOC2, this.context)).thenReturn(this.doc2);
        when(this.wiki.getDocument(DOCUMENT_REFERENCE_DOCFAIL, this.context)).thenThrow(XWikiException.class);
        when(this.doc1.getDocumentReference()).thenReturn(DOCUMENT_REFERENCE_DOC1);
        when(this.doc2.getDocumentReference()).thenReturn(DOCUMENT_REFERENCE_DOC2);
        when(this.doc1.getXObject(IMAGE_STYLE_CLASS_LOCAL_REFERENCE)).thenReturn(this.doc1BaseObject);
        when(this.doc2.getXObject(IMAGE_STYLE_CLASS_LOCAL_REFERENCE)).thenReturn(this.doc2BaseObject);
    }

    @Test
    void getImageStyles() throws Exception
    {
        when(this.query.execute()).thenReturn(List.of("doc1", "docfail", "doc2"));
        when(this.doc1BaseObject.getStringValue("defaultWidth")).thenReturn("");
        when(this.doc1BaseObject.getStringValue("defaultHeight")).thenReturn("100");
        when(this.doc1BaseObject.getLongValue("defaultHeight")).thenReturn(100L);
        when(this.doc2BaseObject.getStringValue("defaultWidth")).thenReturn("");
        when(this.doc2BaseObject.getStringValue("defaultHeight")).thenReturn("");
        Set<ImageStyle> imageStyles = this.manager.getImageStyles("wiki");
        assertEquals(2, imageStyles.size());
        assertEquals(Set.of(
            getDefaultStyle().setIdentifier("doc1").setDefaultHeight(100L),
            getDefaultStyle().setIdentifier("doc2")
        ), imageStyles);
        verify(this.contextManager).pushContext(any(ExecutionContext.class), eq(true));
        verify(this.contextManager).popContext();
        verify(this.context).setWikiId("wiki");
        verify(this.query).setWiki("wiki");
        assertEquals(1, this.logCapture.size());
        assertEquals("Failed to resolve document reference [docfail]. Cause: [XWikiException: Error number 0 in 0].",
            this.logCapture.getMessage(0));
        assertEquals(Level.WARN, this.logCapture.getLogEvent(0).getLevel());
    }

    @Test
    void getImageStylesQueryException() throws Exception
    {
        when(this.query.execute()).thenThrow(QueryException.class);
        ImageStyleException exception =
            assertThrows(ImageStyleException.class, () -> this.manager.getImageStyles("wiki"));
        assertEquals("Failed to retrieve the list of image styles", exception.getMessage());
        assertEquals(QueryException.class, exception.getCause().getClass());
        verify(this.contextManager).pushContext(any(ExecutionContext.class), eq(true));
        verify(this.contextManager).popContext();
        verify(this.context).setWikiId("wiki");
        verify(this.query).setWiki("wiki");
    }

    @Test
    void getImageStylesContextException() throws Exception
    {
        doThrow(ExecutionContextException.class).when(this.contextManager)
            .pushContext(any(ExecutionContext.class), anyBoolean());
        ImageStyleException exception =
            assertThrows(ImageStyleException.class, () -> this.manager.getImageStyles("wiki"));
        assertEquals("Failed to initialize a context for wiki [wiki]", exception.getMessage());
        assertEquals(ExecutionContextException.class, exception.getCause().getClass());
        verify(this.contextManager).pushContext(any(ExecutionContext.class), eq(true));
        verify(this.contextManager).popContext();
    }

    private ImageStyle getDefaultStyle()
    {
        return new ImageStyle()
            .setAdjustableSize(false)
            .setAdjustableBorder(false)
            .setDefaultBorder(false)
            .setAdjustableAlignment(false)
            .setAdjustableTextWrap(false)
            .setDefaultTextWrap(false);
    }
}
