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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.junit.Assert.assertEquals;

/**
 * Unit tests for {@link org.xwiki.mail.MailState}.
 *
 * @version $Id$
 * @since 6.4
 */
public class MailStateTest
{
    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void parseWhenUnknownStateString()
    {
        this.thrown.expect(IllegalArgumentException.class);
        this.thrown.expectMessage("Invalid mail state [unknown]");
        MailState.parse("unknown");
    }

    @Test
    public void parseWhenOldFailedState()
    {
        MailState state = MailState.parse("failed");
        assertEquals(MailState.SEND_ERROR, state);
    }

    @Test
    public void parseWhenPrepareSuccessState()
    {
        MailState state = MailState.parse("prepare_success");
        assertEquals(MailState.PREPARE_SUCCESS, state);
    }

    public void parseWhenPrepareErrorsState()
    {
        MailState state = MailState.parse("prepare_error");
        assertEquals(MailState.PREPARE_ERROR, state);
    }

    @Test
    public void parseWhenSendSuccessState()
    {
        MailState state = MailState.parse("send_success");
        assertEquals(MailState.SEND_SUCCESS, state);
    }

    @Test
    public void parseWhenSendErrorLowercase()
    {
        MailState state = MailState.parse("send_error");
        assertEquals(MailState.SEND_ERROR, state);
    }

    @Test
    public void parseWhenSendErrorStateUppercase()
    {
        MailState state = MailState.parse("SEND_ERROR");
        assertEquals(MailState.SEND_ERROR, state);
    }

    @Test
    public void parseWhenSendFatalErrorState()
    {
        MailState state = MailState.parse("send_fatal_error");
        assertEquals(MailState.SEND_FATAL_ERROR, state);
    }
}
