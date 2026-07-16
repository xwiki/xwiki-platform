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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.xwiki.annotation.reference.TypedStringEntityReferenceResolver;
import org.xwiki.index.IndexException;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.test.MockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.InjectMockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.OldcoreTest;
import com.xpn.xwiki.test.reference.ReferenceComponentList;

import ch.qos.logback.classic.Level;

import static com.xpn.xwiki.doc.XWikiDocument.COMMENTSCLASS_REFERENCE;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.xwiki.annotation.Annotation.TARGET_FIELD;
import static org.xwiki.model.EntityType.DOCUMENT;

/**
 * Test of {@link AnnotationInternalTargetFixTaskConsumer}.
 *
 * @version $Id$
 */
@OldcoreTest
@ReferenceComponentList
class AnnotationInternalTargetFixTaskConsumerTest
{
    private static final DocumentReference DOCUMENT_REFERENCE = new DocumentReference("xwiki", "Space", "Page");

    private static final DocumentReference DOCUMENT_REFERENCE_2 = new DocumentReference("xwiki", "Space", "Page2");

    @InjectMockComponents
    private AnnotationInternalTargetFixTaskConsumer consumer;

    @MockComponent
    private TypedStringEntityReferenceResolver referenceResolver;

    @InjectMockitoOldcore
    private MockitoOldcore oldcore;

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.INFO);

    @Test
    void consumeNewDocument() throws Exception
    {
        // No document is saved for DOCUMENT_REFERENCE, so it is new.
        this.consumer.consume(DOCUMENT_REFERENCE, "1.0");

        assertEquals("[Space.Page] skipped because it does not exist", this.logCapture.getMessage(0));
        assertEquals(Level.INFO, this.logCapture.getLogEvent(0).getLevel());
    }

    @Test
    void consumeGetDocException() throws Exception
    {
        XWikiContext context = this.oldcore.getXWikiContext();
        doThrow(XWikiException.class).when(this.oldcore.getSpyXWiki()).getDocument(DOCUMENT_REFERENCE, context);

        IndexException indexException =
            assertThrows(IndexException.class, () -> this.consumer.consume(DOCUMENT_REFERENCE, "1.0"));
        assertEquals("Failed to resolve document [xwiki:Space.Page]", indexException.getMessage());
        assertEquals(XWikiException.class, indexException.getCause().getClass());
    }

    @Test
    void consume() throws Exception
    {
        XWikiContext context = this.oldcore.getXWikiContext();
        XWikiDocument document = new XWikiDocument(DOCUMENT_REFERENCE);
        BaseObject comment = document.newXObject(COMMENTSCLASS_REFERENCE, context);
        comment.setStringValue(TARGET_FIELD, "xwiki:Space.Page");
        this.oldcore.getSpyXWiki().saveDocument(document, context);

        when(this.referenceResolver.resolve("xwiki:Space.Page", DOCUMENT)).thenReturn(DOCUMENT_REFERENCE);

        this.consumer.consume(DOCUMENT_REFERENCE, "1.0");

        // The target, which points to the document holding the comment, is reset. The cached instance is not
        // modified in place: the document is only updated through the save of its clone.
        XWikiDocument savedDocument = this.oldcore.getSpyXWiki().getDocument(DOCUMENT_REFERENCE, context);
        BaseObject savedComment = savedDocument.getXObjects(COMMENTSCLASS_REFERENCE).get(0);
        assertEquals("", savedComment.getStringValue(TARGET_FIELD));
    }

    @Test
    void consumeSkipWhenNotEquals() throws Exception
    {
        XWikiContext context = this.oldcore.getXWikiContext();
        XWikiDocument document = new XWikiDocument(DOCUMENT_REFERENCE);
        BaseObject comment = document.newXObject(COMMENTSCLASS_REFERENCE, context);
        comment.setStringValue(TARGET_FIELD, "xwiki:Space.Page2");
        this.oldcore.getSpyXWiki().saveDocument(document, context);

        when(this.referenceResolver.resolve("xwiki:Space.Page2", DOCUMENT)).thenReturn(DOCUMENT_REFERENCE_2);

        this.consumer.consume(DOCUMENT_REFERENCE, "1.0");

        // The target points elsewhere, it must be left untouched.
        XWikiDocument savedDocument = this.oldcore.getSpyXWiki().getDocument(DOCUMENT_REFERENCE, context);
        BaseObject savedComment = savedDocument.getXObjects(COMMENTSCLASS_REFERENCE).get(0);
        assertEquals("xwiki:Space.Page2", savedComment.getStringValue(TARGET_FIELD));
    }
}
