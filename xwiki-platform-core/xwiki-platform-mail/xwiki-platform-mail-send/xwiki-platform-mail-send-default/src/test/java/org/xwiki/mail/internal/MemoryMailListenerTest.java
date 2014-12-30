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

import javax.mail.internet.MimeMessage;

import org.junit.Rule;
import org.junit.Test;
import org.xwiki.mail.MailListener;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * Unit tests for {@link MemoryMailListener}.
 *
 * @version $Id$
 * @since 6.2M1
 */
public class MemoryMailListenerTest
{
    @Rule
    public MockitoComponentMockingRule<MemoryMailListener> mocker =
        new MockitoComponentMockingRule<>(MemoryMailListener.class);

    @Test
    public void errorAndRetrieveError() throws Exception
    {
        MemoryMailListener listener = this.mocker.getInstance(MailListener.class, "memory");
        MimeMessage message = mock(MimeMessage.class);
        listener.onError(message, new Exception("error"));
        assertTrue(listener.getErrors().hasNext());
        String error = listener.getErrors().next().getError();
        assertEquals("Exception: error", error );
    }
}
