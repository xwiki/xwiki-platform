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
package org.xwiki.migrations.internal;

import javax.inject.Inject;
import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.index.IndexException;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test of {@link XWikiPropertiesMetaFieldCleanupTaskConsumer}.
 *
 * @version $Id$
 */
@ComponentTest
class XWikiPropertiesMetaFieldCleanupTaskConsumerTest
{
    @InjectMockComponents
    private XWikiPropertiesMetaFieldCleanupTaskConsumer taskConsumer;

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Mock
    private XWikiContext context;

    @Mock
    private XWiki wiki;

    @BeforeEach
    void setUp()
    {
        when(this.contextProvider.get()).thenReturn(this.context);
        when(this.context.getWiki()).thenReturn(this.wiki);
        when(this.context.getWikiReference()).thenReturn(new WikiReference("xwiki"));
    }

    @Test
    void consumeFailOnGetDocument() throws Exception
    {
        DocumentReference documentReference = new DocumentReference("xwiki", "XWiki", "XWikiPreferences");
        doThrow(XWikiException.class)
            .when(this.wiki)
            .getDocument(documentReference, this.context);

        IndexException indexException =
            assertThrows(IndexException.class, () -> this.taskConsumer.consume(documentReference, null));

        assertEquals(XWikiException.class, indexException.getCause().getClass());
        assertEquals("Unable to retrieve document [xwiki:XWiki.XWikiPreferences]", indexException.getMessage());
    }

    @Test
    void consumeNoXObject() throws Exception
    {
        DocumentReference documentReference = new DocumentReference("xwiki", "XWiki", "XWikiPreferences");

        XWikiDocument document = mock(XWikiDocument.class);
        when(this.wiki.getDocument(documentReference, this.context)).thenReturn(document);
        when(document.clone()).thenReturn(document);

        this.taskConsumer.consume(documentReference, null);

        verify(document).getXObject(any(LocalDocumentReference.class));
        verify(this.wiki, never()).saveDocument(any(), any());
    }

    @Test
    void consumeNoMetaFieldAlreadyEmpty() throws Exception
    {
        BaseObject metaField = new BaseObject();
        metaField.setStringValue("meta", "");

        DocumentReference documentReference = new DocumentReference("xwiki", "XWiki", "XWikiPreferences");
        XWikiDocument document = mock(XWikiDocument.class);

        when(this.wiki.getDocument(documentReference, this.context)).thenReturn(document);
        when(document.clone()).thenReturn(document);
        when(document.getXObject(documentReference.getLocalDocumentReference())).thenReturn(metaField);

        this.taskConsumer.consume(documentReference, null);

        verify(document).getXObject(any(LocalDocumentReference.class));
        verify(this.wiki, never()).saveDocument(any(), any());
    }

    @Test
    void consumeDocumentSaveFail() throws Exception
    {
        BaseObject metaField = new BaseObject();
        metaField.setStringValue("meta", "a");

        DocumentReference documentReference = new DocumentReference("xwiki", "XWiki", "XWikiPreferences");
        XWikiDocument document = mock(XWikiDocument.class);

        when(this.wiki.getDocument(documentReference, this.context)).thenReturn(document);
        when(document.clone()).thenReturn(document);
        when(document.getXObject(documentReference.getLocalDocumentReference())).thenReturn(metaField);
        doThrow(XWikiException.class).when(this.wiki).saveDocument(any(), anyString(), any());

        IndexException indexException =
            assertThrows(IndexException.class, () -> this.taskConsumer.consume(documentReference, null));

        assertEquals(XWikiException.class, indexException.getCause().getClass());
        assertEquals("Unable to save document [xwiki:XWiki.XWikiPreferences]", indexException.getMessage());
        verify(document).getXObject(any(LocalDocumentReference.class));
        assertEquals("", metaField.getStringValue("meta"));
    }

    @Test
    void consume() throws Exception
    {
        BaseObject metaField = new BaseObject();
        metaField.setStringValue("meta", "a");

        DocumentReference documentReference = new DocumentReference("xwiki", "XWiki", "XWikiPreferences");
        XWikiDocument document = mock(XWikiDocument.class);

        when(this.wiki.getDocument(documentReference, this.context)).thenReturn(document);
        when(document.getXObject(documentReference.getLocalDocumentReference())).thenReturn(metaField);
        when(document.clone()).thenReturn(document);

        this.taskConsumer.consume(documentReference, null);

        verify(document).getXObject(any(LocalDocumentReference.class));
        assertEquals("", metaField.getStringValue("meta"));
        verify(this.wiki).saveDocument(document, "[UPGRADE] empty field [meta] because it matches the default values",
            this.context);
    }
}
