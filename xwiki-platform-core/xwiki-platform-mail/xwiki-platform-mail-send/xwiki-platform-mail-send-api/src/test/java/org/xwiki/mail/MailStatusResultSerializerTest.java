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

import java.util.Arrays;
import java.util.Date;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link MailStatusResultSerializer}.
 *
 * @version $Id$
 * @since 6.4RC1
 */
class MailStatusResultSerializerTest
{
    @Test
    void serializeErrors()
    {
        MailStatusResult statusResult = mock(MailStatusResult.class);
        Date date = new Date();

        // Return failures for the test
        MailStatus status1 = new MailStatus();
        status1.setBatchId("batch1");
        status1.setState("prepare_error");
        status1.setDate(date);
        status1.setMessageId("6ys1BeC6gnKA7srO/vs06XBZKZM=");
        status1.setRecipients("john@doe.com");
        status1.setErrorSummary("errorsummary1");
        status1.setErrorDescription("errordescription1");
        MailStatus status2 = new MailStatus();
        status2.setBatchId("batch2");
        status2.setState("send_error");
        status2.setDate(date);
        status2.setMessageId("6ys1BeC6gnKA7srO/vs06XBZKZM=");
        status2.setRecipients("jane@doe.com");
        status2.setErrorSummary("errorsummary2");
        status2.setErrorDescription("errordescription2");
        when(statusResult.getAllErrors()).thenReturn(Arrays.asList(status1, status2).iterator());

        assertEquals("Some messages have failed to be sent: "
            + "[[messageId = [6ys1BeC6gnKA7srO/vs06XBZKZM=], batchId = [batch1], state = [prepare_error], "
            + "date = [" + date + "], recipients = [john@doe.com], errorSummary = [errorsummary1], "
            + "errorDescription = [errordescription1]][messageId = [6ys1BeC6gnKA7srO/vs06XBZKZM=], "
            + "batchId = [batch2], state = [send_error], date = [" + date + "], recipients = [jane@doe.com], "
            + "errorSummary = [errorsummary2], errorDescription = [errordescription2]]]",
            MailStatusResultSerializer.serializeErrors(statusResult));
    }
}
