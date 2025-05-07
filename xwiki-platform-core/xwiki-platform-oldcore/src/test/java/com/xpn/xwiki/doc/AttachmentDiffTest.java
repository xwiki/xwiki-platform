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
package com.xpn.xwiki.doc;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit test for {@link AttachmentDiff}.
 *
 * @version $Id$
 * @since 5.0M2
 */
class AttachmentDiffTest
{
    @Test
    void toStringWhenVersionsNotNull()
    {
        AttachmentDiff diff = new AttachmentDiff("filename", "1.1", "1.2");
        assertEquals("filename: 1.1 \u21E8 1.2", diff.toString());
    }

    @Test
    void toStringWhenNoOriginalVersion()
    {
        AttachmentDiff diff = new AttachmentDiff("filename", null, "1.1");
        assertEquals("filename: () \u21E8 1.1", diff.toString());
    }

    @Test
    void toStringWhenNoNewVersion()
    {
        AttachmentDiff diff = new AttachmentDiff("filename", "1.1", null);
        assertEquals("filename: 1.1 \u21E8 ()", diff.toString());
    }

    @Test
    void toStringWhenNoVersions()
    {
        AttachmentDiff diff = new AttachmentDiff("filename", null, null);
        assertEquals("filename: () \u21E8 ()", diff.toString());
    }
}
