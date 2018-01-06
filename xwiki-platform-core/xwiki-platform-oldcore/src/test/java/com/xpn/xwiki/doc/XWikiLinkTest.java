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

import org.junit.Assert;
import org.junit.Test;

/**
 * Unit tests for the {@link XWikiList} class.
 *
 * @version $Id$
 */
public class XWikiLinkTest
{

    @Test
    public void testXWikiListEquals()
    {
        final long docId = 101;
        final String link = "test link";
        final String fullName = "test fullName";

        XWikiLink l1 = new XWikiLink(docId, link, fullName);
        XWikiLink l2 = new XWikiLink(docId, link, fullName);
        XWikiLink l3 = new XWikiLink(docId, link, fullName);

        // Reflexive
        Assert.assertEquals(l1, l1);

        // Symmetric
        Assert.assertEquals(l1, l2);
        Assert.assertEquals(l2, l1);

        // Transitive
        Assert.assertEquals(l1, l2);
        Assert.assertEquals(l2, l3);
        Assert.assertEquals(l1, l3);

        // equals null == false
        Assert.assertFalse(l1.equals(null));
    }
}
