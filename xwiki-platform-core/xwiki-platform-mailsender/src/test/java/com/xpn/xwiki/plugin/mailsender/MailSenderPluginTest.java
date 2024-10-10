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
package com.xpn.xwiki.plugin.mailsender;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.environment.Environment;
import org.xwiki.test.TestEnvironment;

import com.xpn.xwiki.web.Utils;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the {@link MailSenderPlugin mailsender plugin}.
 * 
 * @version $Id$
 */
public class MailSenderPluginTest
{
    /** The empty {@link Mail} object used for testing. */
    private Mail mail;

    /** The {@link MailSenderPlugin plugin} instance used for testing. */
    private static MailSenderPlugin plugin;

    @BeforeClass
    public static void setUpPlugin() throws ComponentLookupException
    {
        ComponentManager componentManager = mock(ComponentManager.class);
        when(componentManager.getInstance(ComponentManager.class, "context")).thenReturn(componentManager);
        when(componentManager.getInstance(Environment.class)).thenReturn(new TestEnvironment());
        Utils.setComponentManager(componentManager);

        plugin = new MailSenderPlugin("mail", MailSenderPlugin.class.getCanonicalName(), null);
    }

    /** Setup: create a new {@code Mail} object and a plugin instance. */
    @Before
    public void setUp()
    {
        this.mail = new Mail();
    }

    /** Test that a {@code null} Mail throws an exception. */
    @Test
    public void testParseRawMessageWithNullMail()
    {
        boolean thrown = false;
        try {
            plugin.parseRawMessage("Subject:Greetings!\n\nDear John,\nHello and Goodbye!", null);
        } catch (IllegalArgumentException ex) {
            thrown = true;
        }
        Assert.assertTrue(thrown);
    }

    /** Test that a {@code null} message throws an exception. */
    @Test
    public void testParseRawMessageWithNullMessage()
    {
        boolean thrown = false;
        try {
            plugin.parseRawMessage(null, this.mail);
        } catch (IllegalArgumentException ex) {
            thrown = true;
        }
        Assert.assertTrue(thrown);
    }

    /** Test that a simple mail with no headers becomes the Mail's textPart. */
    @Test
    public void testParseRawMessageWithSimpleMessage()
    {
        plugin.parseRawMessage("Dear John,\nHello and Goodbye!", this.mail);
        Assert.assertEquals("Dear John,\r\nHello and Goodbye!\r\n", this.mail.getTextPart());
        Assert.assertEquals(0, this.mail.getHeaders().size());

    }

    /** Test that the Subject header is treated as a special header and placed in the Mail correctly. */
    @Test
    public void testParseRawMessageWithSubject()
    {
        plugin.parseRawMessage("Subject:Greetings!\n\nDear John,\nHello and Goodbye!", this.mail);
        Assert.assertEquals("Dear John,\r\nHello and Goodbye!\r\n", this.mail.getTextPart());
        Assert.assertEquals("Greetings!", this.mail.getSubject());
        Assert.assertEquals(0, this.mail.getHeaders().size());
    }

    /** Test that both Subject and From are detected as special headers. */
    @Test
    public void testParseRawMessageWithSubjectAndFrom()
    {
        plugin
            .parseRawMessage("Subject:Greetings!\nFrom:user@example.org\n\nDear John,\nHello and Goodbye!", this.mail);
        Assert.assertEquals("Dear John,\r\nHello and Goodbye!\r\n", this.mail.getTextPart());
        Assert.assertEquals("Greetings!", this.mail.getSubject());
        Assert.assertEquals("user@example.org", this.mail.getFrom());
        Assert.assertEquals(0, this.mail.getHeaders().size());
    }

    /** Test that the first empty line marks the start of the body. */
    @Test
    public void testParseRawMessageWithFakeFrom()
    {
        plugin.parseRawMessage("Subject:Greetings!\n\nFrom:user@example.org\n\nDear John,\nHello and Goodbye!",
            this.mail);
        Assert.assertEquals("From:user@example.org\r\n\r\nDear John,\r\nHello and Goodbye!\r\n", this.mail.getTextPart());
        Assert.assertEquals("Greetings!", this.mail.getSubject());
        Assert.assertNull(this.mail.getFrom());
        Assert.assertEquals(0, this.mail.getHeaders().size());
    }

    /** Test that the header name stops at the first colon. */
    @Test
    public void testParseRawMessageWithColonInHeader()
    {
        plugin.parseRawMessage("Subject:Greetings:Human!\n\nDear John,\nHello and Goodbye!", this.mail);
        Assert.assertEquals("Dear John,\r\nHello and Goodbye!\r\n", this.mail.getTextPart());
        Assert.assertEquals("Greetings:Human!", this.mail.getSubject());
        Assert.assertNull(this.mail.getFrom());
        Assert.assertEquals(0, this.mail.getHeaders().size());
    }

    /** Test that custom header are simply passed to the mail as-is. */
    @Test
    public void testParseRawMessageWithExtraHeaders()
    {
        plugin.parseRawMessage("X-Header:Something extra!\n\nDear John,\nHello and Goodbye!", this.mail);
        Assert.assertEquals("Dear John,\r\nHello and Goodbye!\r\n", this.mail.getTextPart());
        Assert.assertNull(this.mail.getSubject());
        Assert.assertEquals(1, this.mail.getHeaders().size());
    }

    /** RFC 2822 allows headers to be split on multiple lines using "folding white spaces". */
    @Test
    public void testParseRawMessageWithMultilineSubject()
    {
        plugin.parseRawMessage("Subject:Greetings\n from\n\thome\nFrom:user@example.org\n\nHello and Goodbye!",
            this.mail);
        Assert.assertEquals("Hello and Goodbye!\r\n", this.mail.getTextPart());
        Assert.assertEquals("Greetings from\thome", this.mail.getSubject());
        Assert.assertEquals("user@example.org", this.mail.getFrom());
        Assert.assertEquals(0, this.mail.getHeaders().size());
    }

    /** Headers can't contain spaces. Test that such lines are correctly used as the body. */
    @Test
    public void testParseRawMessageWithFakeHeaders()
    {
        plugin.parseRawMessage("To Susan:Greetings!\n\nHello and Goodbye!", this.mail);
        Assert.assertEquals("To Susan:Greetings!\r\n\r\nHello and Goodbye!\r\n", this.mail.getTextPart());
        Assert.assertNull(this.mail.getSubject());
        Assert.assertEquals(0, this.mail.getHeaders().size());
    }
}
