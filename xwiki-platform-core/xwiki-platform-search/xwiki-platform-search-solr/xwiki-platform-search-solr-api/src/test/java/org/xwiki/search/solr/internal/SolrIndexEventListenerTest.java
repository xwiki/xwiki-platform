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
package org.xwiki.search.solr.internal;

import java.util.Arrays;
import java.util.Locale;

import org.junit.jupiter.api.Test;
import org.xwiki.bridge.event.DocumentDeletedEvent;
import org.xwiki.bridge.event.DocumentUpdatedEvent;
import org.xwiki.mail.GeneralMailConfigurationUpdatedEvent;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.search.solr.internal.api.SolrIndexer;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link SolrIndexEventListener}.
 * 
 * @version $Id$
 */
@ComponentTest
class SolrIndexEventListenerTest
{
    @InjectMockComponents
    private SolrIndexEventListener listener;

    @MockComponent
    private SolrIndexer indexer;

    @Test
    void onDocumentDeleted()
    {
        DocumentReference documentReference = new DocumentReference("aWiki", "aSpace", "aPage");
        XWikiDocument document = mock(XWikiDocument.class);
        when(document.getOriginalDocument()).thenReturn(document);
        when(document.getDocumentReference()).thenReturn(documentReference);
        when(document.getRealLocale()).thenReturn(Locale.FRENCH);

        this.listener.onEvent(new DocumentDeletedEvent(), document, null);

        verify(this.indexer).delete(new DocumentReference(documentReference, Locale.FRENCH), false);
    }

    @Test
    void onDocumentTranslationUpdated()
    {
        XWikiDocument translation = mock(XWikiDocument.class);
        DocumentReference translationReference = new DocumentReference("wiki", "Path", "Page", Locale.FRENCH);
        when(translation.getDocumentReferenceWithLocale()).thenReturn(translationReference);

        this.listener.onEvent(new DocumentUpdatedEvent(), translation, null);

        verify(this.indexer).index(translationReference, false);
        verify(this.indexer, times(1)).index(any(EntityReference.class), any(Boolean.class));
    }

    @Test
    void onDocumentDefaultTranslationUpdated() throws Exception
    {
        XWikiContext xcontext = mock(XWikiContext.class);

        XWikiDocument document = mock(XWikiDocument.class);
        when(document.getLocale()).thenReturn(Locale.ROOT);
        when(document.getTranslationLocales(xcontext)).thenReturn(Arrays.asList(Locale.FRENCH, Locale.GERMAN));

        DocumentReference documentReference = new DocumentReference("wiki", "Path", "Page");
        when(document.getDocumentReference()).thenReturn(documentReference);

        this.listener.onEvent(new DocumentUpdatedEvent(), document, xcontext);

        verify(this.indexer, times(3)).index(any(EntityReference.class), any(Boolean.class));
        verify(this.indexer).index(documentReference, false);
        verify(this.indexer).index(new DocumentReference(documentReference, Locale.FRENCH), false);
        verify(this.indexer).index(new DocumentReference(documentReference, Locale.GERMAN), false);
    }

    @Test
    void onGeneralMailConfigurationUpdatedEvent()
    {
        this.listener.onEvent(new GeneralMailConfigurationUpdatedEvent(), null, null);
        verify(this.indexer).index(null, true);

        String otherWiki = "otherwiki";
        this.listener.onEvent(new GeneralMailConfigurationUpdatedEvent(otherWiki), otherWiki, null);
        verify(this.indexer).index(new WikiReference(otherWiki), true);
    }
}
