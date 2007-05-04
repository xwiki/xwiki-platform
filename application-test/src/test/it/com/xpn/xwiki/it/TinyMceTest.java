/*
 * Copyright 2007, XpertNet SARL, and individual contributors as indicated
 * by the contributors.txt.
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
package com.xpn.xwiki.it;

/**
 * Tests the Tiny MCE editor also known as the WYSIWYG editor.
 *
 * @version $Id: $
 */
public class TinyMceTest extends AbstractTinyMceTestCase
{
    public void testSimpleList() throws Exception
    {
        open("/xwiki/bin/edit/Test/TestTinyMCE");
        clearTinyMceContent();
        typeInTinyMce("item1");
        clickTinyMceUnorderedListButton();
        typeEnterInTinyMce();
        typeInTinyMce("item2");
        typeEnterInTinyMce();
        typeInTinyMce("item3");
        saveAndViewEdition();

        assertWikiTextGeneratedByTinyMCE("* item1\n* item2\n* item3");
    }
}
