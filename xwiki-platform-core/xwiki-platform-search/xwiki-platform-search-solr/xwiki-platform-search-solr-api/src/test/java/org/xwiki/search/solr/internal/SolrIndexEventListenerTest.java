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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Locale;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.bridge.event.DocumentDeletedEvent;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.observation.EventListener;
import org.xwiki.search.solr.internal.api.SolrIndexer;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Unit tests for {@link SolrIndexEventListener}.
 * 
 * @version $Id$
 */
public class SolrIndexEventListenerTest
{
    @Rule
    public MockitoComponentMockingRule<EventListener> mocker = new MockitoComponentMockingRule<EventListener>(
        SolrIndexEventListener.class);

    private SolrIndexer indexer;

    @Before
    public void setUp() throws Exception
    {
        indexer = mocker.registerMockComponent(SolrIndexer.class);
    }

    @Test
    public void onDocumentDeleted() throws Exception
    {
        DocumentReference documentReference = new DocumentReference("aWiki", "aSpace", "aPage");
        XWikiDocument document = mock(XWikiDocument.class);
        when(document.getOriginalDocument()).thenReturn(document);
        when(document.getDocumentReference()).thenReturn(documentReference);
        when(document.getRealLocale()).thenReturn(Locale.FRENCH);

        mocker.getComponentUnderTest().onEvent(new DocumentDeletedEvent(), document, null);

        verify(indexer).delete(new DocumentReference(documentReference, Locale.FRENCH), false);
    }
}
