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
package org.xwiki.captcha.jcaptcha;

import java.util.List;

import org.jsoup.nodes.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.bridge.event.DocumentCreatedEvent;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.wiki.internal.bridge.DefaultWikiObjectComponentManagerEventListener;
import org.xwiki.context.internal.concurrent.DefaultContextStoreManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.observation.EventListener;
import org.xwiki.rendering.RenderingScriptServiceComponentList;
import org.xwiki.rendering.internal.configuration.DefaultRenderingConfigurationComponentList;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.page.HTML50ComponentList;
import org.xwiki.test.page.PageTest;
import org.xwiki.test.page.TestNoScriptMacro;
import org.xwiki.test.page.XWikiSyntax21ComponentList;
import org.xwiki.uiextension.internal.UIExtensionClassDocumentInitializer;
import org.xwiki.uiextension.script.UIExtensionScriptServiceComponentList;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.xwiki.uiextension.internal.WikiUIExtensionConstants.EXTENSION_POINT_ID_PROPERTY;
import static org.xwiki.uiextension.internal.WikiUIExtensionConstants.ID_PROPERTY;
import static org.xwiki.uiextension.internal.WikiUIExtensionConstants.PARAMETERS_PROPERTY;
import static org.xwiki.uiextension.internal.WikiUIExtensionConstants.SCOPE_PROPERTY;
import static org.xwiki.uiextension.internal.WikiUIExtensionConstants.UI_EXTENSION_CLASS;

/**
 * Page test for {@code XWiki.Captcha.JCaptcha.Administration}.
 *
 * @version $Id$
 */
@ComponentList({
    UIExtensionClassDocumentInitializer.class,
    DefaultContextStoreManager.class,
    TestNoScriptMacro.class
})
@UIExtensionScriptServiceComponentList
@RenderingScriptServiceComponentList
@DefaultRenderingConfigurationComponentList
@HTML50ComponentList
@XWikiSyntax21ComponentList
class JCaptchaAdministrationPageTest extends PageTest
{
    private static final String WIKI_NAME = "xwiki";

    private static final List<String> JCAPTCHA_SPACES = List.of("XWiki", "Captcha", "JCaptcha");

    private static final DocumentReference ADMINISTRATION_REFERENCE =
        new DocumentReference(WIKI_NAME, JCAPTCHA_SPACES, "Administration");

    private static final String ENGINE_EXTENSION_POINT = "org.xwiki.captcha.jcaptcha.engine";

    private static final String VALID_ENGINE = "org.xwiki.captcha.test.ValidEngine";

    /**
     * A malicious engine value that tries to break out of the surrounding {@code value="..."} attribute and the
     * enclosing {@code {{html}}} macro to execute a macro (here the {@code {{noscript /}}} test macro standing in for
     * the {@code {{groovy}}} macro of the original exploit). See XWIKI-24388.
     */
    private static final String MALICIOUS_ENGINE = "\">{{/html}}{{noscript /}}";

    @BeforeEach
    void setUp() throws Exception
    {
        // Initialize the UIExtension class and the mandatory documents.
        this.xwiki.initializeMandatoryDocuments(this.context);

        // Allow the engines contributed through UIXs to be registered at wiki level.
        when(this.oldcore.getMockAuthorizationManager().hasAccess(any(), any(), any())).thenReturn(true);
        when(this.oldcore.getMockDocumentAuthorizationManager().hasAccess(any(), any(), any(), any()))
            .thenReturn(true);

        // Register the component manager as wiki component manager so that the UIXs can be registered on the wiki.
        this.componentManager.registerComponent(ComponentManager.class, "wiki", this.componentManager);

        // Load the JCaptcha configuration documents required by the Administration page.
        loadPage(new DocumentReference(WIKI_NAME, JCAPTCHA_SPACES, "ConfigurationClass"));
        loadPage(new DocumentReference(WIKI_NAME, JCAPTCHA_SPACES, "Configuration"));
    }

    /**
     * Verify that the {@code engine} parameter of an engine UIX cannot be used to inject wiki/macro syntax, which would
     * lead to remote code execution since the Administration page is executed with the rights of its author.
     */
    @Test
    void engineParameterIsEscaped() throws Exception
    {
        registerEngineUIX("validEngine", VALID_ENGINE);
        registerEngineUIX("maliciousEngine", MALICIOUS_ENGINE);

        Document htmlPage = renderHTMLPage(ADMINISTRATION_REFERENCE);

        List<String> engineValues = htmlPage.select("form.jcaptcha input[type=radio]").eachAttr("value");

        // A legitimate engine contributed through a UIX is still displayed as a radio option (the fix does not break
        // valid contributions).
        assertThat("The valid engine should be displayed as a radio option", engineValues, hasItem(VALID_ENGINE));

        // The malicious engine value is rendered as a single, properly escaped attribute value: the fact that jsoup
        // returns it unchanged proves it did not break out of the attribute nor of the {{html}} macro (otherwise the
        // {{noscript /}} macro would have been executed and the value would not survive as-is).
        assertThat("The malicious engine value should be escaped and contained in a single attribute", engineValues,
            hasItem(MALICIOUS_ENGINE));

        // The form is fully rendered up to its submit button, which is the last element of the {{html}} macro. If the
        // malicious value had broken out of the macro, everything after it (including the submit button) would have
        // been rendered as escaped text instead of actual HTML elements.
        assertNotNull(htmlPage.selectFirst("form.jcaptcha input[type=submit]"),
            "The submit button is missing, the engine value broke the {{html}} macro.");
    }

    private void registerEngineUIX(String name, String engine) throws Exception
    {
        DocumentReference documentReference =
            new DocumentReference(WIKI_NAME, List.of("XWiki", "Captcha", "JCaptcha", "Engines"), name);
        XWikiDocument document = new XWikiDocument(documentReference);
        // Set the author to a user without script right (the default), to match the attack scenario.
        BaseObject uiExtensionObject = document.newXObject(UI_EXTENSION_CLASS, this.context);
        uiExtensionObject.setStringValue(EXTENSION_POINT_ID_PROPERTY, ENGINE_EXTENSION_POINT);
        uiExtensionObject.setStringValue(ID_PROPERTY, name);
        uiExtensionObject.setLargeStringValue(PARAMETERS_PROPERTY, """
            type=image
            engine=%s""".formatted(engine));
        uiExtensionObject.setStringValue(SCOPE_PROPERTY, "wiki");
        this.xwiki.saveDocument(document, this.context);

        // The event listeners are not registered by default. We trigger it manually so that the UIX is registered and
        // can be found and rendered.
        this.componentManager
            .<EventListener>getInstance(EventListener.class,
                DefaultWikiObjectComponentManagerEventListener.EVENT_LISTENER_NAME)
            .onEvent(new DocumentCreatedEvent(), document, null);
    }
}
