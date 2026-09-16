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
package org.xwiki.officeimporter.test.ui;

import java.io.InputStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.administration.test.po.AdministrationPage;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.officeimporter.test.po.OfficeServerAdministrationSectionPage;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.docker.junit5.servletengine.ServletEngine;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.ViewPage;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

/**
 * Functional tests for the {@code office} macro.
 *
 * @version $Id$
 */
@UITest(office = true, servletEngine = ServletEngine.TOMCAT,
    forbiddenEngines = {
        // These tests need to have XWiki running inside a Docker container (we chose Tomcat since it's the most
        // used one), because they need LibreOffice to be installed, and we cannot guarantee that it is installed on the
        // host machine.
        ServletEngine.JETTY_STANDALONE
    },
    properties = {
        // Add the FileUploadPlugin which is needed by the test to attach the office file to view
        "xwikiCfgPlugins=com.xpn.xwiki.plugin.fileupload.FileUploadPlugin",
        // Starting or stopping the Office server requires PR (for the current user, on the main wiki reference)
        "xwikiPropertiesAdditionalProperties=test.prchecker.excludePattern=.*:XWiki\\.OfficeImporterAdmin"
    },
    extraJARs = {
        // The office macro and the viewer it relies on are not part of the minimal WAR built for the tests.
        "org.xwiki.platform:xwiki-platform-office-macro",
        "org.xwiki.platform:xwiki-platform-office-viewer"
    }
)
class OfficeMacroIT
{
    private static final String ATTACHMENT_NAME = "Test.odt";

    private static final String DOCUMENT_CONTENT = "This is a test document.";

    private static final String PASSWORD = "password";

    @BeforeEach
    void setUp(TestUtils setup)
    {
        setup.loginAsSuperAdmin();
        // Connect the wiki to the office server if it is not already done
        AdministrationPage administrationPage = AdministrationPage.gotoPage();
        administrationPage.clickSection("Content", "Office Server");
        OfficeServerAdministrationSectionPage officeServerAdministrationSectionPage =
            new OfficeServerAdministrationSectionPage();
        if (!"Connected".equals(officeServerAdministrationSectionPage.getServerState())) {
            officeServerAdministrationSectionPage.startServer();
        }
    }

    /**
     * The office macro displays the content of the office attachment to a user allowed to view it.
     */
    @Test
    void viewAttachment(TestUtils setup, TestReference testReference) throws Exception
    {
        DocumentReference sourceReference = getSourceReference(testReference);
        createFixture(setup, testReference, sourceReference);

        setup.createUserAndLogin(getUserName(testReference), PASSWORD);

        ViewPage viewPage = setup.gotoPage(testReference);
        assertThat("The office macro should display the document content to a user allowed to view the attachment.",
            viewPage.getContent(), containsString(DOCUMENT_CONTENT));
    }

    /**
     * The office macro doesn't display the content of an attachment the current user has no view right on.
     */
    @Test
    void viewAttachmentWithoutViewRight(TestUtils setup, TestReference testReference) throws Exception
    {
        DocumentReference sourceReference = getSourceReference(testReference);
        createFixture(setup, testReference, sourceReference);

        String userName = getUserName(testReference);
        setup.setRights(sourceReference, null, "XWiki." + userName, "view", false);

        // Display the macro once with a user who is allowed to see the attachment.
        ViewPage viewPage = setup.gotoPage(testReference);
        assertThat("The office macro should display the document content to a user allowed to view the attachment.",
            viewPage.getContent(), containsString(DOCUMENT_CONTENT));

        setup.createUserAndLogin(userName, PASSWORD);

        viewPage = setup.gotoPage(testReference);
        String content = viewPage.getContent();
        assertThat("The office macro shouldn't show the document content to a user who cannot view the attachment.",
            content, not(containsString(DOCUMENT_CONTENT)));
        assertThat("The office macro should fail for a user who cannot view the attachment.", content,
            containsString("Failed to execute the [office] macro"));
    }

    /**
     * @return the reference of the page holding the office attachment, a sibling of the test page
     */
    private DocumentReference getSourceReference(TestReference testReference)
    {
        return new DocumentReference("Source", testReference.getLastSpaceReference());
    }

    /**
     * @return the name of the user created by the test, unique so that the rights set by one test don't apply to
     *         another one
     */
    private String getUserName(TestReference testReference)
    {
        return testReference.getLastSpaceReference().getName() + "User";
    }

    /**
     * Attach the office file to the source page and make the test page display it through the office macro.
     */
    private void createFixture(TestUtils setup, TestReference testReference, DocumentReference sourceReference)
        throws Exception
    {
        setup.rest().delete(sourceReference);
        setup.rest().delete(testReference);

        // The source page is created by the attachment upload.
        try (InputStream officeFile = getClass().getResourceAsStream("/ooffice.3.0/" + ATTACHMENT_NAME)) {
            setup.attachFile(sourceReference, ATTACHMENT_NAME, officeFile, true);
        }

        setup.rest().savePage(testReference,
            String.format("{{office reference=\"attach:%s@%s\"/}}", setup.serializeReference(sourceReference),
                ATTACHMENT_NAME),
            "Viewer");
    }
}
