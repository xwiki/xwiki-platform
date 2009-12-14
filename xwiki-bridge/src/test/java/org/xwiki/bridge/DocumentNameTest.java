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
package org.xwiki.bridge;

import org.junit.Test;

import junit.framework.Assert;

/**
 * Unit tests for {@link DocumentName}.
 * 
 * @version $Id$
 */
@Deprecated
public class DocumentNameTest
{
    @Test
    public void testEquals()
    {
        DocumentName documentName1 = new DocumentName("wiki", "space", "page");

        Assert.assertTrue(documentName1.equals(documentName1));
        Assert.assertTrue(documentName1.equals(new DocumentName("wiki", "space", "page")));
        Assert.assertFalse(documentName1.equals(new DocumentName("wiki2", "space", "page")));
        Assert.assertFalse(documentName1.equals(new DocumentName("wiki", "space2", "page")));
        Assert.assertFalse(documentName1.equals(new DocumentName("wiki", "space", "page2")));
        Assert.assertFalse(documentName1.equals(new DocumentName(null, null, null)));
        Assert.assertTrue(new DocumentName(null, null, null).equals(new DocumentName(null, null, null)));
    }

    @Test
    public void testHashCode()
    {
        DocumentName documentName1 = new DocumentName("wiki", "space", "page");

        Assert.assertTrue(documentName1.hashCode() == documentName1.hashCode());
        Assert.assertTrue(documentName1.hashCode() == new DocumentName("wiki", "space", "page").hashCode());
        Assert.assertFalse(documentName1.hashCode() == new DocumentName("wiki2", "space", "page").hashCode());
        Assert.assertFalse(documentName1.hashCode() == new DocumentName("wiki", "space2", "page").hashCode());
        Assert.assertFalse(documentName1.hashCode() == new DocumentName("wiki", "space", "page2").hashCode());
    }
}
