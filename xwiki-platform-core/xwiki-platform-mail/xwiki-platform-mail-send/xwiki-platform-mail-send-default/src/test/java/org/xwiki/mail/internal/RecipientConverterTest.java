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

import org.junit.jupiter.api.Test;
import org.xwiki.properties.converter.ConversionException;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for {@link org.xwiki.mail.internal.RecipientConverter}.
 *
 * @version $Id$
 * @since 6.1RC1
 */
@ComponentTest
public class RecipientConverterTest
{
    @InjectMockComponents
    private RecipientConverter converter;

    @Test
    public void convert()
    {
        assertEquals(Message.RecipientType.TO,
            this.converter.convert(Message.RecipientType.class, "to"));
        assertEquals("To", this.converter.convert(String.class, Message.RecipientType.TO));

        assertEquals(Message.RecipientType.CC,
            this.converter.convert(Message.RecipientType.class, "cc"));
        assertEquals(Message.RecipientType.BCC,
            this.converter.convert(Message.RecipientType.class, "bcc"));
        assertEquals(MimeMessage.RecipientType.NEWSGROUPS,
            this.converter.convert(Message.RecipientType.class, "newsgroups"));
    }

    @Test
    public void convertWhenInvalidType()
    {
        Throwable exception = assertThrows(ConversionException.class, () -> {
            this.converter.convert(Message.RecipientType.class, "something");
        });
        assertEquals("Cannot convert [something] to [javax.mail.Message$RecipientType]", exception.getMessage());
    }

    @Test
    public void convertWhenTypeIsAlreadyARecipientType()
    {
        assertEquals(Message.RecipientType.TO,
            this.converter.convert(Message.RecipientType.class, Message.RecipientType.TO));
    }
}
