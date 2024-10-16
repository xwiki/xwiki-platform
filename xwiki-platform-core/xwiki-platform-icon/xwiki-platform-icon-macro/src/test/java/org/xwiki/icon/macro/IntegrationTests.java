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

package org.xwiki.icon.macro;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.environment.Environment;
import org.xwiki.icon.Icon;
import org.xwiki.icon.IconException;
import org.xwiki.icon.IconSet;
import org.xwiki.icon.IconSetCache;
import org.xwiki.icon.IconSetManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.observation.ObservationManager;
import org.xwiki.rendering.test.integration.junit5.RenderingTests;
import org.xwiki.script.ScriptContextInitializer;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.skin.SkinManager;
import org.xwiki.skinx.SkinExtension;
import org.xwiki.test.annotation.AllComponents;
import org.xwiki.test.mockito.MockitoComponentManager;

import com.xpn.xwiki.doc.XWikiDocument;

import static org.mockito.Mockito.when;

/**
 * Run all tests found in {@code *.test} files located in the classpath. These {@code *.test} files must follow the
 * conventions described in {@link org.xwiki.rendering.test.integration.TestDataParser}.
 *
 * @version $Id$
 */
@AllComponents
public class IntegrationTests implements RenderingTests
{
    private static final DocumentReference ICON_DOCUMENT_REFERENCE = new DocumentReference("xwiki", "Icon", "Document");

    /**
     * Initializes various mocks to prevent errors in the integration tests.
     *
     * @param componentManager the component manager of the tests
     * @throws Exception when the initialization fails
     */
    @RenderingTests.Initialized
    public void initialize(MockitoComponentManager componentManager) throws Exception
    {
        // Inject a not failing Environment
        componentManager.registerMockComponent(Environment.class);

        // Mock the authorization managers as they try initializing a cache which fails (infinispan is not available
        // in rendering tests).
        ContextualAuthorizationManager authorizationManager =
            componentManager.registerMockComponent(ContextualAuthorizationManager.class);
        // Grant view right on the icon document.
        when(authorizationManager.hasAccess(Right.VIEW, ICON_DOCUMENT_REFERENCE)).thenReturn(true);
        // Grant script right to disable restricted cleaning in the HTML macro.
        when(authorizationManager.hasAccess(Right.SCRIPT)).thenReturn(true);
        componentManager.registerMockComponent(AuthorizationManager.class);

        // Mock the icon set cache as it fails.
        componentManager.registerMockComponent(IconSetCache.class);

        // Mock skin extensions.
        componentManager.registerMockComponent(SkinExtension.class, "ssx");
        componentManager.registerMockComponent(SkinExtension.class, "jsx");
        componentManager.registerMockComponent(SkinExtension.class, "linkx");

        // Mock various components for the script context initialization.
        componentManager.registerMockComponent(SkinManager.class);
        componentManager.registerMockComponent(ScriptContextInitializer.class, "xwiki");
        componentManager.registerMockComponent(ObservationManager.class);

        DocumentAccessBridge documentAccessBridge = componentManager.registerMockComponent(DocumentAccessBridge.class);
        XWikiDocument testDocument = new XWikiDocument(ICON_DOCUMENT_REFERENCE);
        when(documentAccessBridge.getDocumentInstance(ICON_DOCUMENT_REFERENCE)).thenReturn(testDocument);

        // Mock the icon set manager as we're not in a real enviornment where icon sets can be loaded.
        IconSetManager iconSetManager = componentManager.registerMockComponent(IconSetManager.class);
        setupIconThemes(iconSetManager);
    }

    private void setupIconThemes(IconSetManager iconSetManager) throws IconException
    {
        // The current icon set, the test icon set.
        IconSet iconSet = new IconSet("test");
        iconSet.addIcon("home", new Icon("homeIcon"));
        iconSet.addIcon("page", new Icon("pageIcon"));
        iconSet.setRenderWiki("(% class=\"icon\" data-xwiki-icon=\"$icon\" %)i(%%)");
        when(iconSetManager.getCurrentIconSet()).thenReturn(iconSet);

        // A special icon set to test loading icons from a different icon set.
        IconSet specialSet = new IconSet("special");
        specialSet.addIcon("home", new Icon("home"));
        specialSet.setRenderWiki("special $icon");
        when(iconSetManager.getIconSet("special")).thenReturn(specialSet);

        // A document-based icon set to test executing in the context of a document.
        IconSet documentIconSet = new IconSet("document");
        documentIconSet.addIcon("document", new Icon("executed"));
        documentIconSet.setRenderWiki("document $icon");
        documentIconSet.setSourceDocumentReference(new DocumentReference(ICON_DOCUMENT_REFERENCE));
        when(iconSetManager.getIconSet("document")).thenReturn(documentIconSet);

        // An icon theme that uses the HTML macro to display the icon
        IconSet htmlSet = new IconSet("html");
        htmlSet.addIcon("home", new Icon("htmlHome"));
        htmlSet.setRenderWiki("{{html clean=\"false\"}}<span class=\"fa fa-$icon\" aria-hidden=\"true\"></span>"
            + "{{/html}}");
        when(iconSetManager.getIconSet("html")).thenReturn(htmlSet);

        // The default icon set to test fallback to the default when the current or specified icon set doesn't
        // contain an icon.
        IconSet defaultSet = new IconSet("default");
        defaultSet.addIcon("fallback", new Icon("fallbackIcon"));
        defaultSet.setRenderWiki("fallback $icon");
        when(iconSetManager.getDefaultIconSet()).thenReturn(defaultSet);
    }
}
