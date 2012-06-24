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
package org.xwiki.messagestream;

import org.jmock.Expectations;
import org.junit.Assert;
import org.junit.Test;
import org.xwiki.messagestream.internal.MessageStreamScriptService;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.AbstractMockingComponentTestCase;
import org.xwiki.test.annotation.MockingRequirement;

/**
 * Tests for the {@link org.xwiki.userstatus.internal.DefaultEvent default event} and
 * {@link org.xwiki.messagestream.internal.DefaultMessageStream default event factory}.
 * 
 * @version $Id$
 */
public class MessageStreamScriptServiceTest extends AbstractMockingComponentTestCase
{
    @MockingRequirement
    private MessageStreamScriptService streamService;

    private final DocumentReference targetUser = new DocumentReference("wiki", "XWiki", "JaneBuck");

    private final DocumentReference targetGroup = new DocumentReference("wiki", "XWiki", "MyFriends");

    @Test
    public void testPostPublicMessage() throws Exception
    {
        final MessageStream mockStream = getComponentManager().getInstance(MessageStream.class);
        getMockery().checking(new Expectations()
                    {
            {
                exactly(1).of(mockStream).postPublicMessage("Hello World!");
            }
        });
        Assert.assertTrue(this.streamService.postPublicMessage("Hello World!"));
    }

    @Test
    public void testPostPublicMessageWithFailure() throws Exception
    {
        final MessageStream mockStream = getComponentManager().getInstance(MessageStream.class);
        getMockery().checking(new Expectations()
                    {
            {
                exactly(1).of(mockStream).postPublicMessage("Hello World!");
                will(throwException(new NullPointerException()));
            }
        });
        Assert.assertFalse(this.streamService.postPublicMessage("Hello World!"));
    }

    @Test
    public void testPostPersonalMessage() throws Exception
    {
        final MessageStream mockStream = getComponentManager().getInstance(MessageStream.class);
        getMockery().checking(new Expectations()
                    {
            {
                exactly(1).of(mockStream).postPersonalMessage("Hello World!");
            }
        });
        Assert.assertTrue(this.streamService.postPersonalMessage("Hello World!"));
    }

    @Test
    public void testPostPersonalMessageWithFailure() throws Exception
    {
        final MessageStream mockStream = getComponentManager().getInstance(MessageStream.class);
        getMockery().checking(new Expectations()
                    {
            {
                exactly(1).of(mockStream).postPersonalMessage("Hello World!");
                will(throwException(new NullPointerException()));
            }
        });
        Assert.assertFalse(this.streamService.postPersonalMessage("Hello World!"));
    }

    @Test
    public void testPostDirectMessage() throws Exception
    {
        final MessageStream mockStream = getComponentManager().getInstance(MessageStream.class);
        getMockery().checking(new Expectations()
                    {
            {
                exactly(1).of(mockStream).postDirectMessageToUser("Hello World!",
                    MessageStreamScriptServiceTest.this.targetUser);
            }
        });
        Assert.assertTrue(this.streamService.postDirectMessageToUser("Hello World!", this.targetUser));
    }

    @Test
    public void testPostDirectMessageWithFailure() throws Exception
    {
        final MessageStream mockStream = getComponentManager().getInstance(MessageStream.class);
        getMockery().checking(new Expectations()
                    {
            {
                exactly(1).of(mockStream).postDirectMessageToUser("Hello World!",
                    MessageStreamScriptServiceTest.this.targetUser);
                will(throwException(new NullPointerException()));
            }
        });
        Assert.assertFalse(this.streamService.postDirectMessageToUser("Hello World!", this.targetUser));
    }

    @Test
    public void testPostGroupMessage() throws Exception
    {
        final MessageStream mockStream = getComponentManager().getInstance(MessageStream.class);
        getMockery().checking(new Expectations()
                    {
            {
                exactly(1).of(mockStream).postMessageToGroup("Hello World!",
                    MessageStreamScriptServiceTest.this.targetGroup);
            }
        });
        Assert.assertTrue(this.streamService.postMessageToGroup("Hello World!", this.targetGroup));
    }

    @Test
    public void testPostGroupMessageWithFailure() throws Exception
    {
        final MessageStream mockStream = getComponentManager().getInstance(MessageStream.class);
        getMockery().checking(new Expectations()
                    {
            {
                exactly(1).of(mockStream).postMessageToGroup("Hello World!",
                    MessageStreamScriptServiceTest.this.targetGroup);
                will(throwException(new NullPointerException()));
            }
        });
        Assert.assertFalse(this.streamService.postMessageToGroup("Hello World!", this.targetGroup));
    }
}
