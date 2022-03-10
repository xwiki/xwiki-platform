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

import java.util.Arrays;
import java.util.Locale;

import org.junit.jupiter.api.Test;
import org.xwiki.like.LikeManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.rendering.async.internal.AsyncRendererCache;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Tests for {@link CacheHandlingLikeEventsListener}.
 *
 * @version $Id$
 */
@ComponentTest
class CacheHandlingLikeEventsListenerTest
{
    @InjectMockComponents
    private CacheHandlingLikeEventsListener listener;

    @MockComponent
    private LikeManager likeManager;

    @MockComponent
    private AsyncRendererCache asyncRendererCache;

    @Test
    void onEvent()
    {
        this.listener.onEvent(null, null, null);
        verify(this.likeManager, never()).clearCache(any());
        verify(this.asyncRendererCache, never()).cleanCache(any(EntityReference.class));

        DocumentReference documentReference = new DocumentReference("toto", "Foo", "Bar");
        this.listener.onEvent(null, null, documentReference);
        verify(this.likeManager).clearCache(documentReference);
        verify(this.asyncRendererCache)
            .cleanCache(new DocumentReference("toto", Arrays.asList("XWiki", "Like"), "LikeUIX", Locale.ROOT));
    }
}
