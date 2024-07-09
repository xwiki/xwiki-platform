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
import java.util.stream.Stream;

import javax.inject.Inject;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
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

    private static final String DOCUMENT_REFERENCE = "xwiki:space.</div>page";

    private static final String CREATE_EXCEPTION_VELOCITY_KEY = "createException";

    private static final String ERROR_MESSAGE_CLASS = "errormessage";

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
        Object[] args = { DOCUMENT_REFERENCE };
        XWikiException invalidNameException = new XWikiException(XWikiException.MODULE_XWIKI_STORE,
            XWikiException.ERROR_XWIKI_APP_DOCUMENT_NAME_INVALID,
            "Cannot create document {0} because its name does not respect the name strategy of the wiki.", null,
            args);
        this.velocityManager.getVelocityContext().put(CREATE_EXCEPTION_VELOCITY_KEY, invalidNameException);
        this.velocityManager.getVelocityContext().put("invalidNameReference", DOCUMENT_REFERENCE);

        // Render the template.
        Document document = Jsoup.parse(this.templateManager.render(CREATE_INLINE_VM));
        Element errormessage = document.getElementsByClass(ERROR_MESSAGE_CLASS).first();
        assertNotNull(errormessage);
        String expectedMessage = String.format("entitynamevalidation.create.invalidNameError [%s] "
            + "entitynamevalidation.create.invalidName.possibleSolution "
            + "entitynamevalidation.create.invalidName.strategyInfo "
            + "[entitynamevalidation.${currentStrategy}.name, entitynamevalidation.${currentStrategy}.usage]",
            DOCUMENT_REFERENCE);
        assertEquals(expectedMessage, errormessage.text());
    }

    /**
     * Test that when there is an exception about the document already existing, the name is correctly escaped.
     */
    @Test
    void testDocumentAlreadyExistsError() throws Exception
    {
        // Set "createException" to an XWikiException to simulate a document exists already error.
        String urlToDocument = "space/%3C%2Fdiv%3Epage";
        Object[] args = { DOCUMENT_REFERENCE };
        XWikiException documentAlreadyExistsException = new XWikiException(XWikiException.MODULE_XWIKI_STORE,
            XWikiException.ERROR_XWIKI_APP_DOCUMENT_NOT_EMPTY,
            "Cannot create document {0} because it already has content", null, args);
        this.velocityManager.getVelocityContext().put(CREATE_EXCEPTION_VELOCITY_KEY, documentAlreadyExistsException);
        this.velocityManager.getVelocityContext().put("existingDocumentReference", DOCUMENT_REFERENCE);

        // Render the template.
        Document document = Jsoup.parse(this.templateManager.render(CREATE_INLINE_VM));
        Element errormessage = document.getElementsByClass(ERROR_MESSAGE_CLASS).first();
        assertNotNull(errormessage);
        String viewURL = String.format("/xwiki/bin/view/%s", urlToDocument);
        String editURL = String.format("/xwiki/bin/edit/%s", urlToDocument);
        String expectedMessage = String.format("core.create.page.error.docalreadyexists [%s, %s, %s]",
            DOCUMENT_REFERENCE, viewURL, editURL);
        assertEquals(expectedMessage, errormessage.text());
    }

    /**
     * Test that when there is an exception about the template provider not allowing the chosen space, the allowed
     * spaces are correctly escaped.
     */
    @ParameterizedTest
    @MethodSource("allowedSpacesProvider")
    void templateProviderRestrictionErrorEscaping(List<String> allowedSpaces) throws Exception
    {
        String provider = "\"provider</div>";
        this.request.put("templateprovider", provider);
        String template = "template</div>";

        // Set "createException" to an XWikiException to simulate a template provider restriction error.
        Object[] args = { template, DOCUMENT_REFERENCE, DOCUMENT_REFERENCE };
        XWikiException exception = new XWikiException(XWikiException.MODULE_XWIKI_STORE,
            XWikiException.ERROR_XWIKI_APP_TEMPLATE_NOT_AVAILABLE,
            "Template {0} cannot be used in space {1} when creating page {2}", null, args);
        this.velocityManager.getVelocityContext().put(CREATE_EXCEPTION_VELOCITY_KEY, exception);
        // Set the allowed spaces to a list containing some HTML.
        this.velocityManager.getVelocityContext().put("createAllowedSpaces", allowedSpaces);

        // Render the template.
        Document document = Jsoup.parse(this.templateManager.render(CREATE_INLINE_VM));
        Element errormessage = document.getElementsByClass(ERROR_MESSAGE_CLASS).first();
        assertNotNull(errormessage);

        String expectedMessage;
        if (allowedSpaces.size() == 1) {
            expectedMessage = String.format("core.create.template.allowedspace.inline [%s, %s]",
                provider, allowedSpaces.get(0));
        } else {
            expectedMessage = String.format("core.create.template.allowedspaces.inline [%s, %s]",
                provider, allowedSpaces);
        }
        assertEquals(expectedMessage, errormessage.text());
    }

    static Stream<List<String>> allowedSpacesProvider()
    {
        return Stream.of(
            List.of("allowedSpace</div>"),
            List.of("allowedSpace1</div>", "allowedSpace2</div>")
        );
    }
}
