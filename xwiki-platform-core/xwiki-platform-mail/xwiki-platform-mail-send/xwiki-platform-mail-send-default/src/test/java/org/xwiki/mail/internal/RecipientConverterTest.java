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
package org.xwiki.mail.internal;

import javax.mail.Message;
import javax.mail.internet.MimeMessage;

import org.junit.Rule;
import org.junit.Test;
import org.xwiki.properties.converter.ConversionException;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import static org.junit.Assert.*;

/**
 * Unit tests for {@link org.xwiki.mail.internal.RecipientConverter}.
 *
 * @version $Id$
 * @since 6.1RC1
 */
public class RecipientConverterTest
{
    @Rule
    public MockitoComponentMockingRule<RecipientConverter> mocker =
        new MockitoComponentMockingRule<>(RecipientConverter.class);

    @Test
    public void convert() throws Exception
    {
        assertEquals(Message.RecipientType.TO,
            this.mocker.getComponentUnderTest().convert(Message.RecipientType.class, "to"));
        assertEquals("To",
            this.mocker.getComponentUnderTest().convert(String.class, Message.RecipientType.TO));

        assertEquals(Message.RecipientType.CC,
            this.mocker.getComponentUnderTest().convert(Message.RecipientType.class, "cc"));
        assertEquals(Message.RecipientType.BCC,
            this.mocker.getComponentUnderTest().convert(Message.RecipientType.class, "bcc"));
        assertEquals(MimeMessage.RecipientType.NEWSGROUPS,
            this.mocker.getComponentUnderTest().convert(Message.RecipientType.class, "newsgroups"));
    }

    @Test
    public void convertWhenInvalidType() throws Exception
    {
        try {
            this.mocker.getComponentUnderTest().convert(Message.RecipientType.class, "something");
            fail("Should have thrown an exception here");
        } catch (ConversionException expected) {
            assertEquals("Cannot convert [something] to [javax.mail.Message$RecipientType]", expected.getMessage());
        }
    }

    @Test
    public void convertWhenTypeIsAlreadyARecipientType() throws Exception
    {
        assertEquals(Message.RecipientType.TO,
            this.mocker.getComponentUnderTest().convert(Message.RecipientType.class, Message.RecipientType.TO));
    }
}
