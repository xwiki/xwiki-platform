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
package org.xwiki.user;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for {@link Editor}.
 *
 * @version $Id$
 */
public class EditorTest
{
    @Test
    void fromString()
    {
        assertEquals(Editor.TEXT, Editor.fromString("Text"));
        assertEquals(Editor.WYSIWYG, Editor.fromString("Wysiwyg"));
        assertEquals(Editor.WYSIWYG, Editor.fromString("nottext"));
        assertEquals(Editor.UNDEFINED, Editor.fromString(null));
        assertEquals(Editor.UNDEFINED, Editor.fromString(""));
    }
}
