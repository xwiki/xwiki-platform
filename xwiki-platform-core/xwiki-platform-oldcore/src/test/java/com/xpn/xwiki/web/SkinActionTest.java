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
package com.xpn.xwiki.web;

import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for the {@link com.xpn.xwiki.web.SkinAction} class.
 * 
 * @version $Id$
 */
public class SkinActionTest
{
    private SkinAction action;

    @BeforeEach
    void setUp()
    {
        this.action = new SkinAction();
    }

    @Test
    public void isTextJavascriptJavaScriptMimetype()
    {
        assertTrue(this.action.isJavascriptMimeType("text/javascript"));
    }

    @Test
    public void isApplicationJavascriptJavaScriptMimetype()
    {
        assertTrue(this.action.isJavascriptMimeType("application/javascript"));
    }

    @Test
    public void isApplicationXJavascriptJavaScriptMimetype()
    {
        assertTrue(this.action.isJavascriptMimeType("application/x-javascript"));
    }

    @Test
    public void isTextEcmascriptJavaScriptMimetype()
    {
        assertTrue(this.action.isJavascriptMimeType("text/ecmascript"));
    }

    @Test
    public void isApplicationEcmascriptJavaScriptMimetype()
    {
        assertTrue(this.action.isJavascriptMimeType("application/ecmascript"));
    }

    @Test
    public void npeJavascriptMimetype()
    {
        assertFalse(this.action.isJavascriptMimeType(null));
    }

    @Test
    public void incorrectSkinFile()
    {
        Throwable exception = assertThrows(IOException.class, () -> {
            this.action.getSkinFilePath("../../resources/js/xwiki/xwiki.js", "colibri");
        });
        assertEquals("Invalid filename: '../../resources/js/xwiki/xwiki.js' for skin 'colibri'",
            exception.getMessage());

        exception = assertThrows(IOException.class, () -> {
            this.action.getSkinFilePath("../../../", "colibri");
        });
        assertEquals("Invalid filename: '../../../' for skin 'colibri'", exception.getMessage());

        exception = assertThrows(IOException.class, () -> {
            this.action.getSkinFilePath("resources/js/xwiki/xwiki.js", "..");
        });
        assertEquals("Invalid filename: 'resources/js/xwiki/xwiki.js' for skin '..'", exception.getMessage());

        exception = assertThrows(IOException.class, () -> {
            this.action.getSkinFilePath("../resources/js/xwiki/xwiki.js", ".");
        });
        assertEquals("Invalid filename: '../resources/js/xwiki/xwiki.js' for skin '.'", exception.getMessage());
    }

    @Test
    public void incorrectResourceFile()
    {
        Throwable exception = assertThrows(IOException.class, () -> {
            this.action.getResourceFilePath("../../skins/js/xwiki/xwiki.js");
        });
        assertEquals("Invalid filename: '../../skins/js/xwiki/xwiki.js'", exception.getMessage());

        exception = assertThrows(IOException.class, () -> {
            this.action.getResourceFilePath("../../../");
        });
        assertEquals("Invalid filename: '../../../'", exception.getMessage());

        exception = assertThrows(IOException.class, () -> {
            this.action.getResourceFilePath("../../redirect");
        });
        assertEquals("Invalid filename: '../../redirect'", exception.getMessage());
    }
}
