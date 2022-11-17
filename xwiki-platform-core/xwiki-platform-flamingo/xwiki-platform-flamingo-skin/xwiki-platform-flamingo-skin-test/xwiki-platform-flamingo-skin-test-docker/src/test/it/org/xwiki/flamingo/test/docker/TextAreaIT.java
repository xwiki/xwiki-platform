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
package org.xwiki.flamingo.test.docker;

import java.util.Arrays;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.test.docker.junit5.TestReference;
import org.xwiki.test.docker.junit5.UITest;
import org.xwiki.test.ui.TestUtils;
import org.xwiki.test.ui.po.ViewPage;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for the object editor.
 *
 * @since 12.4RC1
 * @version $Id$
 */
@UITest
class TextAreaIT
{
    private static final String TEXTAREA_CLASS = "TextAreaIT.NestedSpace.TextAreaClass";

    @BeforeAll
    public void beforeEach(TestUtils testUtils)
    {
        testUtils.loginAsSuperAdmin();

        DocumentReference textAreaClassReference =
            new DocumentReference("xwiki", Arrays.asList("TextAreaIT", "NestedSpace"), "TextAreaClass");

        testUtils.createPage(textAreaClassReference, "", "TextAreaClass");
        testUtils.addClassProperty(textAreaClassReference, "textarea", "TextArea");
    }

    @Test
    @Order(1)
    void authors(TestUtils testUtils, TestReference testReference)
    {
        // Cleanup
        testUtils.deletePage(testReference);

        // Create the page content with a normal user
        testUtils.createUserAndLogin("user", "password");
        testUtils.createPage(testReference, "");

        // Add object with superadmin
        testUtils.loginAsSuperAdmin();
        testUtils.addObject(testReference, TEXTAREA_CLASS, "textarea",
        // @formatter:off
            "{{velocity}}\n"
            + "document: $doc.documentReference\n"
            + "content author: $services.user.serialize($doc.authors.contentAuthor)\n"
            + "effective author: $services.user.serialize($doc.authors.effectiveMetadataAuthor)\n"
            + "original author: $services.user.serialize($doc.authors.originalMetadataAuthor)\n"
            + "current author: $services.user.serialize($xcontext.context.authorReference)\n"
            + "{{/velocity}}");
       // @formatter:on

        DocumentReference testReference2 =
            new DocumentReference(testReference.getName() + "2", testReference.getLastSpaceReference());

        ViewPage viewPage = testUtils.createPage(testReference2, "{{velocity}}$xwiki.getDocument('"
            + testUtils.serializeReference(testReference) + "').display('textarea'){{/velocity}}");

        assertEquals(
        // @formatter:off
            "document: xwiki:TextAreaIT.authors.WebHome\n"
            + "content author: xwiki:XWiki.user\n"
            + "effective author: XWiki.superadmin\n"
            + "original author: XWiki.superadmin\n"
            + "current author: XWiki.superadmin",
        // @formatter:on
            viewPage.getContent());
    }
}
