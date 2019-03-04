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
package org.xwiki.store;

import org.junit.jupiter.api.Test;
import org.xwiki.store.internal.FileSystemStoreUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Validate {@link FileSystemStoreUtils}.
 * 
 * @version $Id$
 */
public class FileSystemStoreUtilsTest
{
    private void assertEncode(String encoded, String decoded, boolean caseInsensitive)
    {
        assertEquals(encoded, FileSystemStoreUtils.encode(decoded, caseInsensitive));
        assertEquals(decoded, FileSystemStoreUtils.decode(encoded));
    }

    // Tests

    @Test
    public void encode()
    {
        assertEncode("", "", true);
        assertEncode("", "", false);

        assertEncode("%41a", "Aa", true);
        assertEncode("Aa", "Aa", false);

        assertEncode("%2E%25%2B%3C%3E%3A%5C%22.%2F%5C%5C%5C%5C%C2%B5%7C%3F%2A .ext", ".%+<>:\\\"./\\\\\\\\µ|?* .ext",
            false);
    }
}
