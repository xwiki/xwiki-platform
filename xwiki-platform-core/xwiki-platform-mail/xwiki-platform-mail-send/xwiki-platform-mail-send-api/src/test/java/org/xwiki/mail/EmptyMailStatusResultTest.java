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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Validate {@link EmptyMailStatusResult}.
 * 
 * @version $Id$
 */
class EmptyMailStatusResultTest
{
    @Test
    void all()
    {
        EmptyMailStatusResult result = EmptyMailStatusResult.INSTANCE;

        assertFalse(result.getAll().hasNext());
        assertFalse(result.getAllErrors().hasNext());
        assertFalse(result.getByState(MailState.PREPARE_ERROR).hasNext());
        assertEquals(0, result.getProcessedMailCount());
        assertEquals(0, result.getTotalMailCount());
        assertTrue(result.isProcessed());
        assertThrows(UnsupportedOperationException.class, () -> result.waitTillProcessed(42));
    }
}
