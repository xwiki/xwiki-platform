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
package org.xwiki.rendering.listener.chaining;

import java.util.Collections;
import java.util.Map;

import org.junit.*;
import org.xwiki.rendering.listener.MetaData;

/**
 * Unit tests for {@link LookaheadChainingListener}.
 * 
 * @version $Id$
 * @since 1.8RC1
 */
public class LookaheadChainingListenerTest
{
    public class TestChainingListener extends AbstractChainingListener
    {
        public int calls = 0;

        public TestChainingListener(ListenerChain listenerChain)
        {
            setListenerChain(listenerChain);
        }

        @Override
        public void beginDocument(MetaData metaData)
        {
            this.calls++;
        }

        @Override
        public void beginParagraph(Map<String, String> parameters)
        {
            this.calls++;
        }

        @Override
        public void endDocument(MetaData metaData)
        {
            this.calls++;
        }

        @Override
        public void endParagraph(Map<String, String> parameters)
        {
            this.calls++;
        }
    }

    @Test
    public void testLookahead()
    {
        ListenerChain chain = new ListenerChain();
        LookaheadChainingListener listener = new LookaheadChainingListener(chain, 2);
        chain.addListener(listener);
        TestChainingListener testListener = new TestChainingListener(chain);
        chain.addListener(testListener);

        // The begin document flushes
        listener.beginDocument(MetaData.EMPTY);
        Assert.assertEquals(1, testListener.calls);

        // 1st lookahead, nothing is sent to the test listener
        listener.beginParagraph(Collections.<String, String> emptyMap());
        Assert.assertEquals(1, testListener.calls);
        Assert.assertEquals(EventType.BEGIN_PARAGRAPH, listener.getNextEvent().eventType);
        Assert.assertNull(listener.getNextEvent(2));

        // 2nd lookahead, nothing is sent to the test listener
        listener.beginParagraph(Collections.<String, String> emptyMap());
        Assert.assertEquals(1, testListener.calls);
        Assert.assertEquals(EventType.BEGIN_PARAGRAPH, listener.getNextEvent().eventType);
        Assert.assertEquals(EventType.BEGIN_PARAGRAPH, listener.getNextEvent(2).eventType);
        Assert.assertNull(listener.getNextEvent(3));

        // 3rd events, the first begin paragraph is sent
        listener.endParagraph(Collections.<String, String> emptyMap());
        Assert.assertEquals(2, testListener.calls);
        Assert.assertEquals(EventType.BEGIN_PARAGRAPH, listener.getNextEvent().eventType);
        Assert.assertEquals(EventType.END_PARAGRAPH, listener.getNextEvent(2).eventType);
        Assert.assertNull(listener.getNextEvent(3));

        // The end document flushes
        listener.endDocument(MetaData.EMPTY);
        Assert.assertEquals(5, testListener.calls);
        Assert.assertNull(listener.getNextEvent());
    }
}
