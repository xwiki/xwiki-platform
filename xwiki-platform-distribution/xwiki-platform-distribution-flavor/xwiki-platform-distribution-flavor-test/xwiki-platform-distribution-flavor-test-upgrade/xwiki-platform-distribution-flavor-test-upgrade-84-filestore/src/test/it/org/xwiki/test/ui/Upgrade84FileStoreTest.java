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
package org.xwiki.test.ui;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Execute upgrade tests.
 * 
 * @version $Id$
 */
public class Upgrade84FileStoreTest extends UpgradeTest
{
    private void assertURLContent(String expected, URL url) throws IOException
    {
        String content = IOUtils.toString(url.openStream(), StandardCharsets.UTF_8);

        assertEquals(expected, content);
    }

    private void assertAttachments(String wiki) throws IOException
    {
        // Check migrated attachment
        URL attachmentURL = new URL(getUtil().getBaseBinURL(wiki) + "download/Attachments/WebHome/attachment.txt");

        assertURLContent("attachment", attachmentURL);

        // Check migrated attachment
        URL attachmentURLWithWhiteSpaces = new URL(
            getUtil().getBaseBinURL(wiki) + "download/Attachments/WebHome/attachment%20with%20white%20spaces.txt");

        assertURLContent("attachment with white spaces", attachmentURLWithWhiteSpaces);

        // Check migrated deleted attachment
        URL deletedAttachmentURL = new URL(
            getUtil().getBaseBinURL(wiki) + "downloadrev/Attachments/WebHome/deletedattachment.txt?rev=1.1&rid=1");

        assertURLContent("deletedattachment", deletedAttachmentURL);
    }

    @Override
    protected void postUpdateValidate() throws Exception
    {
        // Main wiki
        assertAttachments(null);

        // wiki1
        assertAttachments("wiki1");
    }

    @Override
    protected void setupLogs()
    {
        validateConsole.getLogCaptureConfiguration().registerExpected(
            // Caused by the fact that we upgrade from an old version of XWiki having these deprecated uses
            "Deprecated usage of getter [com.xpn.xwiki.api.Document.getName]",

            // The currently installed flavor is not valid anymore before the upgrade
            "Invalid extension [org.xwiki.enterprise:xwiki-enterprise-ui-wiki/8.4.6] on namespace [wiki:wiki1] "
                + "(InvalidExtensionException: Dependency [org.xwiki.platform:xwiki-platform-oldcore-[8.4.6]] is "
                + "incompatible with the core extension [org.xwiki.platform:xwiki-platform-legacy-oldcore/",
            "Invalid extension [org.xwiki.enterprise:xwiki-enterprise-ui-mainwiki/8.4.6] on namespace [wiki:xwiki] "
                + "(InvalidExtensionException: Dependency [org.xwiki.platform:xwiki-platform-oldcore-[8.4.6]] is "
                + "incompatible with the core extension [org.xwiki.platform:xwiki-platform-legacy-oldcore/",

            // Previous store contains an index of the deleted attachment based on the absolute filesystem path (testing
            // that migration works despite this very bad old design)
            // Keeping only the relative path, since the absolute path might change depending on who upgrading the data
            // last.
            "data/storage/xwiki/Attachments/WebHome/~this/"
                + "deleted-attachments/deletedattachment.txt-1549039065268] does not exist, "
                + "trying to find the new location",
            "data/storage/wiki1/Attachments/WebHome/~this/"
                + "deleted-attachments/deletedattachment.txt-1551887620946] does not exist, "
                + "trying to find the new location",

            // The data contains a Solr directory: we want to ensure that it is properly upgraded too.
            "The Solr home directory at [data/solr] is invalid.",

            // Deprecated are related to the Velocity upgrade performed in 12.0 (XCOMMONS-1529)
            "Deprecated usage of method [org.apache.velocity.tools.generic.SortTool.sort]",
            "Deprecated usage of method [org.apache.velocity.tools.generic.MathTool.toInteger]"
        );
    }
}
