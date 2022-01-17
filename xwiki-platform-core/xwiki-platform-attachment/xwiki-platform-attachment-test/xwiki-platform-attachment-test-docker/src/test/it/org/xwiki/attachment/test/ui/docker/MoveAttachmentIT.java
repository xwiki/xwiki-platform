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
package org.xwiki.attachment.test.ui.docker;

import java.io.File;

import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.docker.junit5.TestConfiguration;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.docker.junit5.servletengine.ServletEngine;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.AttachmentsPane;
import org.xwiki.test.ui.po.ViewPage;

import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Tests of the move attachment feature.
 *
 * @version $Id$
 * @since 14.0RC1
 */
@UITest//(servletEngine = ServletEngine.EXTERNAL)
class MoveAttachmentIT
{
    @Test
    @Order(1)
    void moveAttachment(TestUtils setup, TestInfo testInfo, TestReference testReference,
        TestConfiguration testConfiguration)
    {
        DocumentReference sourcePage = new DocumentReference("Source", testReference.getLastSpaceReference());
        DocumentReference targetPage = new DocumentReference("Target", testReference.getLastSpaceReference());

        setup.loginAsSuperAdmin();
        setup.deletePage(testReference.getParent(), true);
        setup.createPage(sourcePage, "");
        setup.createPage(targetPage, "");

        ViewPage viewSourcePage = setup.gotoPage(sourcePage);
        AttachmentsPane sourceAttachmentsPane = viewSourcePage.openAttachmentsDocExtraPane();
        sourceAttachmentsPane.setFileToUpload(
            new File(testConfiguration.getBrowser().getTestResourcesPath(), "moveme.txt").getAbsolutePath());
        sourceAttachmentsPane.waitForUploadToFinish("moveme.txt");

//        MovePage attachmentsPane = new MovePage(viewPage).openAttachmentsDocExtraPane();

    }
}
