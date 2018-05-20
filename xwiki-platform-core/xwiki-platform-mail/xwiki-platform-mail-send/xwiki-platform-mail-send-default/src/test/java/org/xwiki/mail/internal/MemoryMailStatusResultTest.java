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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.xwiki.mail.ExtendedMimeMessage;
import org.xwiki.mail.MailState;
import org.xwiki.mail.MailStatus;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link MemoryMailStatusResult}.
 *
 * @version $Id$
 */
public class MemoryMailStatusResultTest
{
    private static final String BATCH_ID = "batch1";
    private static final String UNIQUE_MESSAGE_ID1 = "message1";
    private static final String UNIQUE_MESSAGE_ID2 = "message2";
    private static final String UNIQUE_MESSAGE_ID3 = "message3";
    private static final String UNIQUE_MESSAGE_ID4 = "message4";
    private static final String UNIQUE_MESSAGE_ID5 = "message5";

    @Test
    public void getAllErrorTest() throws Exception
    {
        MemoryMailStatusResult statusResult = new MemoryMailStatusResult();
        ExtendedMimeMessage message1 = mock(ExtendedMimeMessage.class);
        when(message1.getUniqueMessageId()).thenReturn(UNIQUE_MESSAGE_ID1);
        ExtendedMimeMessage message2 = mock(ExtendedMimeMessage.class);
        when(message2.getUniqueMessageId()).thenReturn(UNIQUE_MESSAGE_ID2);
        ExtendedMimeMessage message3 = mock(ExtendedMimeMessage.class);
        when(message3.getUniqueMessageId()).thenReturn(UNIQUE_MESSAGE_ID3);
        ExtendedMimeMessage message4 = mock(ExtendedMimeMessage.class);
        when(message4.getUniqueMessageId()).thenReturn(UNIQUE_MESSAGE_ID4);
        ExtendedMimeMessage message5 = mock(ExtendedMimeMessage.class);
        when(message5.getUniqueMessageId()).thenReturn(UNIQUE_MESSAGE_ID5);

        statusResult.setStatus(new MailStatus(BATCH_ID, message1, MailState.PREPARE_SUCCESS));
        statusResult.setStatus(new MailStatus(BATCH_ID, message2, MailState.PREPARE_ERROR));
        statusResult.setStatus(new MailStatus(BATCH_ID, message3, MailState.SEND_SUCCESS));
        statusResult.setStatus(new MailStatus(BATCH_ID, message4, MailState.SEND_ERROR));
        statusResult.setStatus(new MailStatus(BATCH_ID, message5, MailState.SEND_FATAL_ERROR));

        List<String> allErrorIds = new ArrayList<>();
        Iterator<MailStatus> it = statusResult.getAllErrors();

        while (it.hasNext()) {
            allErrorIds.add(it.next().getMessageId());
        }

        assertThat(allErrorIds, containsInAnyOrder(UNIQUE_MESSAGE_ID2, UNIQUE_MESSAGE_ID4, UNIQUE_MESSAGE_ID5));
        assertThat(allErrorIds.size(), equalTo(3));
    }
}
