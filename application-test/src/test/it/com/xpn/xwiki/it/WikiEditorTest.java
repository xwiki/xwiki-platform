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

import com.xpn.xwiki.it.framework.AbstractXWikiTestCase;
import com.xpn.xwiki.it.framework.AlbatrossSkinExecutor;
import com.xpn.xwiki.it.framework.XWikiTestSuite;
import junit.framework.Test;

/**
 * Tests the wiki editor.
 *
 * @version $Id: $
 */
public class WikiEditorTest extends AbstractXWikiTestCase
{
    public static Test suite()
    {
        XWikiTestSuite suite = new XWikiTestSuite("Tests the wiki editor");
        suite.addTestSuite(WikiEditorTest.class, AlbatrossSkinExecutor.class);
        return suite;
    }

    protected void setUp() throws Exception
    {
        super.setUp();
        loginAsAdmin();    
    }

    public void testEmptyLineAndSpaceCharactersBeforeSectionTitleIsNotRemoved()
    {
        open("/xwiki/bin/edit/Test/WikiEdit?editor=wiki");
        setFieldValue("content", "\n  1.1 Section\n\ntext");
        clickEditSaveAndView();
        open("/xwiki/bin/edit/Test/WikiEdit?editor=wiki");
        assertEquals("\n  1.1 Section\n\ntext", getFieldValue("content"));
    }
}
