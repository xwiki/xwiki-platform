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

import org.junit.Rule;
import org.junit.Test;
import org.xwiki.mail.MailStatusStore;
import org.xwiki.mail.MailStoreException;
import org.xwiki.test.LogRule;

import static org.mockito.Mockito.*;
import static org.junit.Assert.*;

/**
 * Unit tests for {@link org.xwiki.mail.internal.DatabaseMailStatusResult}.
 *
 * @version $Id$
 * @since 6.4RC1
 */
public class DatabaseMailStatusResultTest
{
    @Rule
    public LogRule logRule = new LogRule() {{
        record(LogLevel.WARN);
        recordLoggingForType(DatabaseMailStatusResult.class);
    }};

    @Test
    public void getSize() throws Exception
    {
        MailStatusStore store = mock(MailStatusStore.class);
        when(store.count("batchid")).thenReturn(1L);

        DatabaseMailStatusResult result = new DatabaseMailStatusResult(store);
        result.setBatchId("batchid");

        assertEquals(1, result.getSize());
    }

    @Test
    public void getSizeWhenStorageError() throws Exception
    {
        MailStatusStore store = mock(MailStatusStore.class);
        when(store.count("batchid")).thenThrow(new MailStoreException("error"));

        DatabaseMailStatusResult result = new DatabaseMailStatusResult(store);
        result.setBatchId("batchid");

        assertEquals(0, result.getSize());
        assertEquals("Failed to get size of results for batch id [batchid]. Returning an empty result.",
            this.logRule.getMessage(0));
    }
}
