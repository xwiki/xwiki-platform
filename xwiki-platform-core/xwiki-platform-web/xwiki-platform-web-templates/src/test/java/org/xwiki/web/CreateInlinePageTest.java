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
package org.xwiki.web;

import java.util.List;

import javax.inject.Inject;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.template.TemplateManager;
import org.xwiki.test.page.PageTest;
import org.xwiki.velocity.VelocityManager;

import com.xpn.xwiki.XWikiException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Tests the {@code createinline.vm} template.
 *
 * @version $Id$
 * @since 14.10.12
 * @since 15.5RC1
 */
class CreateInlinePageTest extends PageTest
{
    /**
     * The name of the template to test.
     */
    private static final String CREATE_INLINE_VM = "createinline.vm";

    private VelocityManager velocityManager;

    @Inject
    private TemplateManager templateManager;

    @BeforeEach
    void setup() throws Exception
    {
        this.velocityManager = this.oldcore.getMocker().getInstance(VelocityManager.class);
        // Set an empty list of recommended template providers to avoid a Velocity error.
        this.velocityManager.getVelocityContext().put("recommendedTemplateProviders", List.of());
    }

    /**
     * Test that when there is a name validation error, the name is correctly escaped.
     */
    @Test
    void testNameValidationError() throws Exception
    {
        // Set "createException" to an XWikiException to simulate a validation error.
        String documentReference = "xwiki:space.</div>page";
        Object[] args = { documentReference };
        XWikiException invalidNameException = new XWikiException(XWikiException.MODULE_XWIKI_STORE,
            XWikiException.ERROR_XWIKI_APP_DOCUMENT_NAME_INVALID,
            "Cannot create document {0} because its name does not respect the name strategy of the wiki.", null,
            args);
        this.velocityManager.getVelocityContext().put("createException", invalidNameException);
        this.velocityManager.getVelocityContext().put("invalidNameReference", documentReference);

        // Render the template.
        Document document = Jsoup.parse(this.templateManager.render(CREATE_INLINE_VM));
        Element errormessage = document.getElementsByClass("errormessage").first();
        assertNotNull(errormessage);
        String expectedMessage = String.format("entitynamevalidation.create.invalidname [%s]", documentReference);
        assertEquals(expectedMessage, errormessage.text());
    }
}
