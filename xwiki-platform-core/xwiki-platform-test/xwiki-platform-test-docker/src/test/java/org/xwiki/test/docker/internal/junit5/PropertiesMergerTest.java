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
import org.xwiki.test.docker.internal.junit5.configuration.PropertiesMerger;
import org.xwiki.test.docker.junit5.DockerTestException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Unit tests for {@link PropertiesMerger}.
 *
 * @version $Id$
 * @since 11.3RC1
 */
class PropertiesMergerTest
{
    @Test
    void mergeWhenXWikiCfgPlugins() throws Exception
    {
        PropertiesMerger merger = new PropertiesMerger();

        Properties original = new Properties();
        original.setProperty("xwikiCfgVirtualUsepath", "1");
        original.setProperty("xwikiCfgPlugins", "com.xpn.xwiki.plugin.skinx.JsSkinExtensionPlugin,"
            + "com.xpn.xwiki.plugin.skinx.JsSkinFileExtensionPlugin,"
            + "com.xpn.xwiki.plugin.skinx.CssSkinExtensionPlugin,"
            + "com.xpn.xwiki.plugin.skinx.CssSkinFileExtensionPlugin,"
            + "com.xpn.xwiki.plugin.skinx.LinkExtensionPlugin");
        original.setProperty("xwikiCfgVirtualUsepath2", "1");

        Properties override = new Properties();
        override.setProperty("xwikiCfgPlugins", "com.xpn.xwiki.plugin.fileupload.FileUploadPlugin");
        override.setProperty("xwikiCfgEditCommentMandatory", "0");
        override.setProperty("xwikiCfgVirtualUsepath2", "0");

        Properties merge = merger.merge(original, override, true);

        assertEquals(4, merge.size());
        assertEquals("1", merge.getProperty("xwikiCfgVirtualUsepath"));
        assertEquals("0", merge.getProperty("xwikiCfgVirtualUsepath2"));
        assertEquals("0", merge.getProperty("xwikiCfgEditCommentMandatory"));
        assertEquals("com.xpn.xwiki.plugin.skinx.JsSkinExtensionPlugin,"
            + "com.xpn.xwiki.plugin.skinx.JsSkinFileExtensionPlugin,"
            + "com.xpn.xwiki.plugin.skinx.CssSkinExtensionPlugin,"
            + "com.xpn.xwiki.plugin.skinx.CssSkinFileExtensionPlugin,"
            + "com.xpn.xwiki.plugin.skinx.LinkExtensionPlugin,"
            + "com.xpn.xwiki.plugin.fileupload.FileUploadPlugin", merge.getProperty("xwikiCfgPlugins"));
    }

    @Test
    void mergeWhenXWikiCfgPluginsButWithOverrideListProperty() throws DockerTestException
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

        Properties merge = merger.merge(original, override, true);

        assertEquals(1, merge.size());
        assertEquals("com.xpn.xwiki.plugin.fileupload.FileUploadPlugin", merge.getProperty("xwikiCfgPlugins"));
    }

    @Test
    void mergeWhenXWikiCfgPluginsAndNoOverride() throws Exception
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

        Properties merge = merger.merge(original, override, false);

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
    void mergeWhenXWikiCfgPluginsAndNoOverrideAndConflict()
    {
        PropertiesMerger merger = new PropertiesMerger();

        Properties original = new Properties();
        original.setProperty("xwikiCfgVirtualUsepath", "0");

        Properties override = new Properties();
        override.setProperty("xwikiCfgVirtualUsepath", "1");

        Throwable exception = assertThrows(DockerTestException.class, () -> merger.merge(original, override, false));
        assertEquals("Cannot merge property [xwikiCfgVirtualUsepath] = [1] since it was already specified with value "
            + "[0]", exception.getMessage());
    }

    @Test
    void mergeWhenXWikiPropertiesAdditionalProperties() throws Exception
    {
        PropertiesMerger merger = new PropertiesMerger();

        Properties original = new Properties();
        original.setProperty("xwikiPropertiesAdditionalProperties", "a=b");

        Properties override = new Properties();
        override.setProperty("xwikiPropertiesAdditionalProperties", "c=d");

        Properties merge = merger.merge(original, override, false);

        assertEquals(1, merge.size());
        assertEquals("a=b\nc=d", merge.getProperty("xwikiPropertiesAdditionalProperties"));
    }
}
