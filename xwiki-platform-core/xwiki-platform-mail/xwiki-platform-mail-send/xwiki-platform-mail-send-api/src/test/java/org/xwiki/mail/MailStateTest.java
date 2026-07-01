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
package org.xwiki.mail;

import org.junit.jupiter.api.Test;
import org.xwiki.test.junit5.mockito.ComponentTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for {@link org.xwiki.mail.MailState}.
 *
 * @version $Id$
 * @since 6.4
 */
@ComponentTest
class MailStateTest
{
    @Test
    void parseWhenUnknownStateString()
    {
        Throwable exception = assertThrows(IllegalArgumentException.class, () -> {
            MailState.parse("unknown");
        });
        assertEquals("Invalid mail state [unknown]", exception.getMessage());
    }

    @Test
    void parseWhenOldFailedState()
    {
        MailState state = MailState.parse("failed");
        assertEquals(MailState.SEND_ERROR, state);
    }

    @Test
    void parseWhenPrepareSuccessState()
    {
        MailState state = MailState.parse("prepare_success");
        assertEquals(MailState.PREPARE_SUCCESS, state);
    }

    @Test
    void parseWhenPrepareErrorsState()
    {
        MailState state = MailState.parse("prepare_error");
        assertEquals(MailState.PREPARE_ERROR, state);
    }

    @Test
    void parseWhenSendSuccessState()
    {
        MailState state = MailState.parse("send_success");
        assertEquals(MailState.SEND_SUCCESS, state);
    }

    @Test
    void parseWhenSendErrorLowercase()
    {
        MailState state = MailState.parse("send_error");
        assertEquals(MailState.SEND_ERROR, state);
    }

    @Test
    void parseWhenSendErrorStateUppercase()
    {
        MailState state = MailState.parse("SEND_ERROR");
        assertEquals(MailState.SEND_ERROR, state);
    }

    @Test
    void parseWhenSendFatalErrorState()
    {
        MailState state = MailState.parse("send_fatal_error");
        assertEquals(MailState.SEND_FATAL_ERROR, state);
    }
}
