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
package com.xpn.xwiki;

import com.xpn.xwiki.objects.classes.BaseClass;

import junit.framework.TestCase;

/**
 * Unit tests for {@link com.xpn.xwiki.XWikiContext}.
 * 
 * @version $Id$
 */
public class XWikiContextTest extends TestCase
{
    /**
     * Validate that the context take into account base class wiki in base class cache.
     */
    public void testGetSetBaseClassMultiwiki()
    {
        XWikiContext context = new XWikiContext();
        context.setDatabase("xwiki");

        BaseClass baseclass = new BaseClass();
        baseclass.setName("Space.Name");
        baseclass.setWiki("wikiid");

        BaseClass baseclassNoWiki = new BaseClass();
        baseclassNoWiki.setName("Space.Name");

        context.addBaseClass(baseclass);

        assertNull("Should not find any base class", context.getBaseClass("Space.Name"));

        context.addBaseClass(baseclassNoWiki);

        context.setDatabase("wikiid");
        assertSame(baseclass, context.getBaseClass("Space.Name"));

        context.setDatabase("xwiki");
        assertSame(baseclassNoWiki, context.getBaseClass("Space.Name"));
    }
}
