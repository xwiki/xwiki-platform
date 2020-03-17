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
package org.xwiki.user.resource.internal.document;

import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.user.UserReference;
import org.xwiki.user.internal.document.DocumentUserReference;
import org.xwiki.user.resource.internal.UserResourceReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * Unit tests for {@link UserResourceReference}.
 *
 * @version $Id$
 */
public class UserResourceReferenceTest
{
    @Test
    void equalsAndHashcode()
    {
        DocumentReference documentReference1 = new DocumentReference("wiki", "space", "page");
        UserReference userReference1 = new DocumentUserReference(documentReference1, null);
        UserResourceReference userResourceReference1 = new UserResourceReference(userReference1);

        assertEquals(userResourceReference1, userResourceReference1);
        assertEquals(userReference1.hashCode(), userReference1.hashCode());

        DocumentReference documentReference2 = new DocumentReference("wiki2", "space", "page");
        UserReference userReference2 = new DocumentUserReference(documentReference2, null);
        UserResourceReference userResourceReference2 = new UserResourceReference(userReference2);

        assertNotEquals(userResourceReference2, userResourceReference1);
        assertNotEquals(userReference2.hashCode(), userReference1.hashCode());
    }
}