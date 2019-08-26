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
package org.xwiki.test.docker.internal.junit5;

import java.util.Properties;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Unit tests for {@link PropertiesMerger}.
 *
 * @version $Id$
 * @since 11.3RC1
 */
public class PropertiesMergerTest
{
    @Test
    public void mergeWhenXWikiCfgPlugins()
    {
        PropertiesMerger merger = new PropertiesMerger();

        Properties original = new Properties();
        original.setProperty("xwikiCfgVirtualUsepath", "1");
        original.setProperty("xwikiCfgPlugins", "com.xpn.xwiki.plugin.skinx.JsSkinExtensionPlugin,"
            + "com.xpn.xwiki.plugin.skinx.JsSkinFileExtensionPlugin,"
            + "com.xpn.xwiki.plugin.skinx.CssSkinExtensionPlugin,"
            + "com.xpn.xwiki.plugin.skinx.CssSkinFileExtensionPlugin,"
            + "com.xpn.xwiki.plugin.skinx.LinkExtensionPlugin");

        Properties override = new Properties();
        override.setProperty("xwikiCfgPlugins", "com.xpn.xwiki.plugin.fileupload.FileUploadPlugin");
        override.setProperty("xwikiCfgEditCommentMandatory", "0");

        Properties merge = merger.merge(original, override);

        assertEquals(3, merge.size());
        assertEquals("1", merge.getProperty("xwikiCfgVirtualUsepath"));
        assertEquals("0", merge.getProperty("xwikiCfgEditCommentMandatory"));
        assertEquals("com.xpn.xwiki.plugin.skinx.JsSkinExtensionPlugin,"
            + "com.xpn.xwiki.plugin.skinx.JsSkinFileExtensionPlugin,"
            + "com.xpn.xwiki.plugin.skinx.CssSkinExtensionPlugin,"
            + "com.xpn.xwiki.plugin.skinx.CssSkinFileExtensionPlugin,"
            + "com.xpn.xwiki.plugin.skinx.LinkExtensionPlugin,"
            + "com.xpn.xwiki.plugin.fileupload.FileUploadPlugin", merge.getProperty("xwikiCfgPlugins"));
    }

    @Test
    public void overrideWhenXWikiCfgPlugins()
    {
        PropertiesMerger merger = new PropertiesMerger();

        Properties original = new Properties();
        original.setProperty("xwikiCfgPlugins", "com.xpn.xwiki.plugin.skinx.JsSkinExtensionPlugin,"
            + "com.xpn.xwiki.plugin.skinx.JsSkinFileExtensionPlugin,"
            + "com.xpn.xwiki.plugin.skinx.CssSkinExtensionPlugin,"
            + "com.xpn.xwiki.plugin.skinx.CssSkinFileExtensionPlugin,"
            + "com.xpn.xwiki.plugin.skinx.LinkExtensionPlugin");

        Properties override = new Properties();
        override.setProperty("xwikiCfgPlugins", "^com.xpn.xwiki.plugin.fileupload.FileUploadPlugin");

        Properties merge = merger.merge(original, override);

        assertEquals(1, merge.size());
        assertEquals("com.xpn.xwiki.plugin.fileupload.FileUploadPlugin", merge.getProperty("xwikiCfgPlugins"));
    }
}
