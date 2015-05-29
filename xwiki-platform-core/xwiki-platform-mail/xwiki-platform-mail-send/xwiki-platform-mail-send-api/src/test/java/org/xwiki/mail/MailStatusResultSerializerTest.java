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

import org.junit.Test;

import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

/**
 * Unit tests for {@link MailStatusResultSerializer}.
 *
 * @version $Id$
 * @since 6.4RC1
 */
public class MailStatusResultSerializerTest
{
    @Test
    public void serializeErrors() throws Exception
    {
        MailStatusResult statusResult = mock(MailStatusResult.class);

        // Return failures for the test
        MailStatus status1 = new MailStatus();
        status1.setErrorSummary("errorsummary1");
        status1.setErrorDescription("errordescription1");
        MailStatus status2 = new MailStatus();
        status2.setErrorSummary("errorsummary2");
        status2.setErrorDescription("errordescription2");
        when(statusResult.getByState(MailState.SEND_ERROR)).thenReturn(Arrays.asList(status1, status2).iterator());

        assertEquals("Some messages have failed to be sent for the following reasons: "
            + "[[[errorsummary1],[errordescription1]][[errorsummary2],[errordescription2]]]",
            MailStatusResultSerializer.serializeErrors(statusResult));
    }
}
