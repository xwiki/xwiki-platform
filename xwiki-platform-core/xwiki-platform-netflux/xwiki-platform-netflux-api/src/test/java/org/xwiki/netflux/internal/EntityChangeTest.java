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
package org.xwiki.netflux.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.netflux.internal.EntityChange.ScriptLevel;
import org.xwiki.user.UserReference;

/**
 * Unit tests for {@link EntityChange}.
 *
 * @version $Id$
 */
class EntityChangeTest
{
    private DocumentReference documentReference = new DocumentReference("test", "Some", "Page");

    private UserReference userReference = mock(UserReference.class);

    @Test
    void verifyToString()
    {
        when(this.userReference.toString()).thenReturn("Alice");
        EntityChange entityChange = new EntityChange(this.documentReference, this.userReference, ScriptLevel.SCRIPT);
        assertTrue(entityChange.toString()
            .startsWith("entity = [test:Some.Page], author = [Alice], scriptLevel = [SCRIPT], timestamp = ["));
    }

    @Test
    void verifyEquals() throws Exception
    {
        EntityChange firstChange = new EntityChange(this.documentReference, this.userReference, ScriptLevel.SCRIPT);
        assertEquals(firstChange, firstChange);

        // Entity change records the timestamp when it is created, so we need to wait at least 1 millisecond before
        // creating the second instance.
        Thread.sleep(1);

        EntityChange secondChange = new EntityChange(this.documentReference, this.userReference, ScriptLevel.SCRIPT);
        assertNotEquals(firstChange, secondChange);
    }
}
