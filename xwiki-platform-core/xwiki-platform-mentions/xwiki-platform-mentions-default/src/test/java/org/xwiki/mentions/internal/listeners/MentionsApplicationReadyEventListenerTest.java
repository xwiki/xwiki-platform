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
package org.xwiki.mentions.internal.listeners;

import javax.inject.Named;
import javax.inject.Provider;

import org.junit.jupiter.api.Test;
import org.xwiki.mentions.internal.MentionsEventExecutor;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWikiContext;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test of {@link MentionsApplicationReadyEventListener}.
 *
 * @version $Id$
 * @since 12.6RC1
 */
@ComponentTest
class MentionsApplicationReadyEventListenerTest
{
    @InjectMockComponents
    private MentionsApplicationReadyEventListener eventListener;

    @MockComponent
    @Named("readonly")
    private Provider<XWikiContext> contextProvider;

    @MockComponent
    private MentionsEventExecutor eventExecutor;

    @BeforeComponent("onEvent")
    void beforeOnEvent()
    {
        // Ensure that the context is not mocked for the onEvent test so that startThreads is not called during
        // initialize method.
        when(this.contextProvider.get()).thenReturn(null);
    }

    @Test
    void onEvent()
    {
        this.eventListener.onEvent(null, null, null);
        verify(this.eventExecutor).startThreads();
    }

    @BeforeComponent("initialize")
    void beforeInitialize()
    {
        when(this.contextProvider.get()).thenReturn(mock(XWikiContext.class));
    }

    @Test
    void initialize()
    {
        verify(this.eventExecutor).startThreads();
    }
}
