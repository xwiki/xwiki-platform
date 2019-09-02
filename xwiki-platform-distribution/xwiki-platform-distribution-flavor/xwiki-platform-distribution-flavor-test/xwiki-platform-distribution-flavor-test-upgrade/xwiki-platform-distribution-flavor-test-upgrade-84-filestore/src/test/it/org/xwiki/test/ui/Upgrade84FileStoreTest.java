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
        URL deletedAttachmentURL =
            new URL(getUtil().getBaseBinURL(wiki) + "downloadrev/Attachments/WebHome/deletedattachment.txt?rev=1.1&rid=1");

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
}
