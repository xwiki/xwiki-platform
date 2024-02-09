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
package org.xwiki.annotation.io.internal.migration.hibernate;

import java.util.List;

import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mock;
import org.xwiki.annotation.reference.TypedStringEntityReferenceResolver;
import org.xwiki.index.IndexException;
import org.xwiki.model.reference.DocumentReference;
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

import static com.xpn.xwiki.doc.XWikiDocument.COMMENTSCLASS_REFERENCE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.xwiki.annotation.Annotation.TARGET_FIELD;
import static org.xwiki.model.EntityType.DOCUMENT;

/**
 * Test of {@link AnnotationInternalTargetFixTaskConsumer}.
 *
 * @version $Id$
 * @since 14.10.20
 * @since 15.5.5
 * @since 15.10.1
 * @since 16.0RC1
 */
@ComponentTest
class AnnotationInternalTargetFixTaskConsumerTest
{
    private static final DocumentReference DOCUMENT_REFERENCE = new DocumentReference("xwiki", "Space", "Page");

    public static final DocumentReference DOCUMENT_REFERENCE_2 = new DocumentReference("xwiki", "Space", "Page2");

    @InjectMockComponents
    private AnnotationInternalTargetFixTaskConsumer consumer;

    @MockComponent
    private Provider<XWikiContext> contextProvider;

    @MockComponent
    private TypedStringEntityReferenceResolver referenceResolver;

    @Mock
    private XWikiContext context;

    @Mock
    private XWiki wiki;

    @Mock
    private XWikiDocument document;

    @Mock
    private BaseObject xObject;

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.INFO);

    @BeforeEach
    void setUp() throws Exception
    {
        when(this.contextProvider.get()).thenReturn(this.context);
        when(this.context.getWiki()).thenReturn(this.wiki);
        when(this.wiki.getDocument(DOCUMENT_REFERENCE, this.context)).thenReturn(this.document);
        when(this.document.isNew()).thenReturn(false);
        when(this.document.getDocumentReference()).thenReturn(DOCUMENT_REFERENCE);
    }

    @Test
    void consumeNewDocument() throws Exception
    {
        when(this.document.isNew()).thenReturn(true);
        this.consumer.consume(DOCUMENT_REFERENCE, "1.0");
        assertEquals("[document] skipped because it does not exist", this.logCapture.getMessage(0));
        assertEquals(Level.INFO, this.logCapture.getLogEvent(0).getLevel());
    }

    @Test
    void consumeGetDocException() throws Exception
    {
        when(this.wiki.getDocument(DOCUMENT_REFERENCE, this.context)).thenThrow(XWikiException.class);
        IndexException indexException =
            assertThrows(IndexException.class, () -> this.consumer.consume(DOCUMENT_REFERENCE, "1.0"));
        assertEquals("Failed to resolve document [xwiki:Space.Page]", indexException.getMessage());
        assertEquals(XWikiException.class, indexException.getCause().getClass());
    }

    @Test
    void consume() throws Exception
    {
        when(this.document.getXObjects(COMMENTSCLASS_REFERENCE)).thenReturn(List.of(this.xObject));
        when(this.xObject.getStringValue(TARGET_FIELD)).thenReturn("xwiki:Space.Page");
        when(this.referenceResolver.resolve("xwiki:Space.Page", DOCUMENT)).thenReturn(DOCUMENT_REFERENCE);
        this.consumer.consume(DOCUMENT_REFERENCE, "1.0");
        verify(this.xObject).setStringValue(TARGET_FIELD, "");
        verify(this.wiki).saveDocument(this.document, "Updating annotation targets", this.context);
    }

    @Test
    void consumeSkipWhenNotEquals() throws Exception
    {
        when(this.document.getXObjects(COMMENTSCLASS_REFERENCE)).thenReturn(List.of(this.xObject));
        when(this.xObject.getStringValue(TARGET_FIELD)).thenReturn("xwiki:Space.Page2");
        when(this.referenceResolver.resolve("xwiki:Space.Page2", DOCUMENT)).thenReturn(DOCUMENT_REFERENCE_2);
        this.consumer.consume(DOCUMENT_REFERENCE, "1.0");
        verify(this.xObject, never()).setStringValue(TARGET_FIELD, "");
        verify(this.wiki, never()).saveDocument(this.document, "Updating annotation targets", this.context);
    }
}
