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
 *
 */
package org.xwiki.bridge;

import junit.framework.TestCase;

/**
 * Unit tests for {@link DocumentName}.
 * 
 * @version $Id$
 */
public class DocumentNameTest extends TestCase
{
    public void testEquals()
    {
        DocumentName documentName1 = new DocumentName("wiki", "space", "page");

        assertTrue(documentName1.equals(documentName1));
        assertTrue(documentName1.equals(new DocumentName("wiki", "space", "page")));
        assertFalse(documentName1.equals(new DocumentName("wiki2", "space", "page")));
        assertFalse(documentName1.equals(new DocumentName("wiki", "space2", "page")));
        assertFalse(documentName1.equals(new DocumentName("wiki", "space", "page2")));
        assertFalse(documentName1.equals(new DocumentName(null, null, null)));
        assertTrue(new DocumentName(null, null, null).equals(new DocumentName(null, null, null)));
    }

    public void testHashCode()
    {
        DocumentName documentName1 = new DocumentName("wiki", "space", "page");

        assertTrue(documentName1.hashCode() == documentName1.hashCode());
        assertTrue(documentName1.hashCode() == new DocumentName("wiki", "space", "page").hashCode());
        assertFalse(documentName1.hashCode() == new DocumentName("wiki2", "space", "page").hashCode());
        assertFalse(documentName1.hashCode() == new DocumentName("wiki", "space2", "page").hashCode());
        assertFalse(documentName1.hashCode() == new DocumentName("wiki", "space", "page2").hashCode());
    }
}
