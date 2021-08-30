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

import java.util.Collections;
import java.util.Iterator;

import org.junit.jupiter.api.Test;
import org.xwiki.mail.MailStatus;
import org.xwiki.mail.MailStatusStore;
import org.xwiki.test.junit5.mockito.ComponentTest;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link org.xwiki.mail.internal.DatabaseMailStatusResult}.
 *
 * @version $Id$
 * @since 6.4RC1
 */
@ComponentTest
class DatabaseMailStatusResultTest
{
    @Test
    void getAll() throws Exception
    {
        MailStatusStore store = mock(MailStatusStore.class);
        MailStatus status = new MailStatus();
        when(store.load(Collections.singletonMap("batchId", "batchid"), 0, 0, "date", true))
            .thenReturn(Collections.singletonList(status));

        DatabaseMailStatusResult result = new DatabaseMailStatusResult(store);
        result.setBatchId("batchid");

        Iterator<MailStatus> resultStatuses = result.getAll();
        MailStatus resultStatus = resultStatuses.next();
        assertSame(status, resultStatus);
        assertFalse(resultStatuses.hasNext());
    }
}
