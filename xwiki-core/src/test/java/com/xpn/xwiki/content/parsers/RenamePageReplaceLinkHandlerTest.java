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
package com.xpn.xwiki.content.parsers;

import junit.framework.TestCase;

import com.xpn.xwiki.content.Link;

/**
 * Unit tests for {@link com.xpn.xwiki.content.parsers.RenamePageReplaceLinkHandler}.
 * 
 * @version $Id$
 */
public class RenamePageReplaceLinkHandlerTest extends TestCase
{
    public void testCompare() throws Exception
    {
        RenamePageReplaceLinkHandler handler = new RenamePageReplaceLinkHandler();
        Link linkToLookFor = new LinkParser().parse("Space.Page?param=something>_self");
        Link linkToReplace = new LinkParser().parse("Space.Page?param2=other");

        assertTrue(handler.compare(linkToLookFor, linkToReplace));
    }

    public void testGetReplacementLink() throws Exception
    {
        RenamePageReplaceLinkHandler handler = new RenamePageReplaceLinkHandler();
        Link newLink = new LinkParser().parse("Space.Page");
        Link linkToReplace = new LinkParser().parse("Hello|OldSpace.OldPage?param=1");

        Link replacementLink = handler.getReplacementLink(newLink, linkToReplace);
        assertEquals("Hello|Space.Page?param=1", replacementLink.toString());
    }
}
