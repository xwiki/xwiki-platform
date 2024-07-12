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
package org.xwiki.like.internal;

import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.bridge.event.DocumentDeletedEvent;
import org.xwiki.bridge.event.WikiDeletedEvent;
import org.xwiki.like.LikeManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.event.XObjectDeletedEvent;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests of {@link DeletedEntityLikeListener}.
 *
 * @version $Id$
 */
@ComponentTest
class DeletedEntityLikeListenerTest
{
    @InjectMockComponents
    private DeletedEntityLikeListener listener;

    @MockComponent
    private Provider<LikeManager> likeManagerProvider;

    private LikeManager likeManager;

    @BeforeEach
    void setup()
    {
        this.likeManager = mock(LikeManager.class);
        when(likeManagerProvider.get()).thenReturn(this.likeManager);
    }

    @Test
    void onEventDeletedDocument()
    {
        XWikiDocument sourceDocument = mock(XWikiDocument.class);
        DocumentReference documentReference = mock(DocumentReference.class);
        when(sourceDocument.getDocumentReference()).thenReturn(documentReference);

        this.listener.onEvent(new DocumentDeletedEvent(documentReference), sourceDocument, null);
        verify(this.likeManager).clearCache(documentReference);
    }

    @Test
    void onEventWiki()
    {
        this.listener.onEvent(new WikiDeletedEvent("mywiki"), null, null);
        verify(this.likeManager).clearCache();
    }

    @Test
    void onEventUser()
    {
        this.listener.onEvent(new XObjectDeletedEvent(), null, null);
        verify(this.likeManager).clearCache();
    }
}
