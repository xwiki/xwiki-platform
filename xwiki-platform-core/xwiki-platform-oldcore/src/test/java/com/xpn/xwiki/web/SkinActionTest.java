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
package com.xpn.xwiki.web;

import java.io.IOException;

import org.jmock.cglib.MockObjectTestCase;

/**
 * Unit tests for the {@link com.xpn.xwiki.web.SkinAction} class.
 * 
 * @version $Id$
 */
public class SkinActionTest extends MockObjectTestCase
{
    private SkinAction action;

    @Override
    protected void setUp()
    {
        this.action = new SkinAction();
    }

    public void testIsTextJavascriptJavaScriptMimetype()
    {
        assertTrue(this.action.isJavascriptMimeType("text/javascript"));
    }

    public void testIsApplicationJavascriptJavaScriptMimetype()
    {
        assertTrue(this.action.isJavascriptMimeType("application/javascript"));
    }

    public void testIsApplicationXJavascriptJavaScriptMimetype()
    {
        assertTrue(this.action.isJavascriptMimeType("application/x-javascript"));
    }

    public void testIsTextEcmascriptJavaScriptMimetype()
    {
        assertTrue(this.action.isJavascriptMimeType("text/ecmascript"));
    }

    public void testIsApplicationEcmascriptJavaScriptMimetype()
    {
        assertTrue(this.action.isJavascriptMimeType("application/ecmascript"));
    }

    public void testNPEJavascriptMimetype()
    {
        assertFalse(this.action.isJavascriptMimeType(null));
    }

    public void testIncorrectSkinFile()
    {
        try {
            this.action.getSkinFilePath("../../resources/js/xwiki/xwiki.js", "colibri");
            assertTrue("should fail", false);
        } catch (IOException e) {
            // good
        }
        try {
            this.action.getSkinFilePath("../../../", "colibri");
            assertTrue("should fail", false);
        } catch (IOException e) {
            // good
        }
        try {
            this.action.getSkinFilePath("resources/js/xwiki/xwiki.js", "..");
            assertTrue("should fail", false);
        } catch (IOException e) {
            // good
        }
        try {
            this.action.getSkinFilePath("../resources/js/xwiki/xwiki.js", ".");
            assertTrue("should fail", false);
        } catch (IOException e) {
            // good
        }
    }

    public void testIncorrectResourceFile()
    {
        try {
            this.action.getResourceFilePath("../../skins/js/xwiki/xwiki.js");
            assertTrue("should fail", false);
        } catch (IOException e) {
            // good
        }
        try {
            this.action.getResourceFilePath("../../../");
            assertTrue("should fail", false);
        } catch (IOException e) {
            // good
        }
        try {
            this.action.getResourceFilePath("../../redirect");
            assertTrue("should fail", false);
        } catch (IOException e) {
            // good
        }
    }
}
