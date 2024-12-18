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
package org.xwiki.panels;

import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.script.ModelScriptService;
import org.xwiki.model.validation.internal.DefaultEntityNameValidationConfiguration;
import org.xwiki.model.validation.internal.DefaultEntityNameValidationManager;
import org.xwiki.model.validation.internal.EntityNameValidationConfigurationSource;
import org.xwiki.model.validation.internal.ReplaceCharacterEntityNameValidation;
import org.xwiki.model.validation.internal.ReplaceCharacterEntityNameValidationConfiguration;
import org.xwiki.model.validation.script.ModelValidationScriptService;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.page.HTML50ComponentList;
import org.xwiki.test.page.PageTest;
import org.xwiki.test.page.XWikiSyntax21ComponentList;
import org.xwiki.velocity.tools.EscapeTool;

import com.xpn.xwiki.doc.XWikiDocument;

import static java.util.Collections.singletonMap;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests of {@code CreatePanel.xml}.
 *
 * @version $Id$
 * @since 13.4
 */
@XWikiSyntax21ComponentList
@HTML50ComponentList
@ComponentList({
    ModelScriptService.class,
    // ModelValidationScriptService and its dependencies.
    ModelValidationScriptService.class,
    EntityNameValidationConfigurationSource.class,
    DefaultEntityNameValidationConfiguration.class,
    DefaultEntityNameValidationManager.class,
    ReplaceCharacterEntityNameValidation.class
})
class CreatePanelTest extends PageTest
{
    private static final String PAGE_TITLE = "Page name with a \\ backslash";

    private static final String TRANSFORMED_PAGE_TITLE = "Page name with a  backslash";

    // We mock the name validation configuration to define the replacement map without depending on the documents.
    @MockComponent
    private ReplaceCharacterEntityNameValidationConfiguration nameValidationConfiguration;

    @Test
    void createPanel() throws Exception
    {
        // Replace the response with a spy to be able to assert sendRedirect parameters.
        this.response = spy(this.response);
        this.context.setResponse(this.response);

        when(this.nameValidationConfiguration.getCharacterReplacementMap()).thenReturn(singletonMap("\\", ""));

        // Initialize the request parameters.
        this.request.put("create", "1");
        this.request.put("panelTitle", PAGE_TITLE);
        this.request.put("parent", "Panels.WebHome");
        String formToken = "csrfToken45";
        this.request.put("form_token", formToken);

        renderPage(new DocumentReference("xwiki", "Panels", "CreatePanel"));

        // Verify in particular that the document we are redirected to has a name that conforms to the naming strategy.
        EscapeTool tool = new EscapeTool();
        verify(this.response).sendRedirect("/xwiki/bin/edit/Panels/" + tool.url(TRANSFORMED_PAGE_TITLE)
            + "?template=Panels.PanelTemplate"
            + "&Panels.PanelClass_0_name=" + tool.url(PAGE_TITLE)
            + "&Panels.PanelClass_0_content=" + tool
            .url("{{velocity}}\n#panelheader('" + PAGE_TITLE + "')\n\n#panelfooter()\n{{/velocity}}")
            + "&parent=Panels.WebHome&form_token=" + formToken);
    }

    @Test
    void createPanelPageAlreadyExist() throws Exception
    {
        // Replace the response with a spy to be able to assert sendRedirect parameters.
        this.response = spy(this.response);
        this.context.setResponse(this.response);

        // Initialize the request parameters.
        this.request.put("create", "1");
        this.request.put("panelTitle", "a");
        this.request.put("parent", "Panels.WebHome");

        XWikiDocument doc = new XWikiDocument(new DocumentReference("xwiki", "Panels", "a"));
        this.xwiki.saveDocument(doc, this.context);

        renderPage(new DocumentReference("xwiki", "Panels", "CreatePanel"));

        // Verify in particular that the document we are redirected to has a name that conforms to the naming strategy.
        verify(this.response).sendRedirect("/xwiki/bin/view/Panels/a?xpage=docalreadyexists");
    }
}
