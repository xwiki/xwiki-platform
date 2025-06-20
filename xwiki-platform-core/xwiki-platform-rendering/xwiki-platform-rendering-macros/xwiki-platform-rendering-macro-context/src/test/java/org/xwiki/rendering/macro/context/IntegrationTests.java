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
package org.xwiki.rendering.macro.context;

import java.io.StringReader;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.query.QueryManager;
import org.xwiki.refactoring.internal.ModelBridge;
import org.xwiki.refactoring.internal.ReferenceUpdater;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.parser.Parser;
import org.xwiki.rendering.test.integration.Initialized;
import org.xwiki.rendering.test.integration.junit5.RenderingTest;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.template.TemplateManager;
import org.xwiki.test.TestEnvironment;
import org.xwiki.test.annotation.AllComponents;
import org.xwiki.test.mockito.MockitoComponentManager;
import org.xwiki.user.CurrentUserReference;
import org.xwiki.user.UserReferenceResolver;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Run all tests found in {@code *.test} files located in the classpath. These {@code *.test} files must follow the
 * conventions described in {@link org.xwiki.rendering.test.integration.TestDataParser}.
 *
 * @version $Id$
 * @since 8.3RC1
 */
@AllComponents
public class IntegrationTests extends RenderingTest
{
    @Initialized
    public void initialize(MockitoComponentManager componentManager) throws Exception
    {
        // Replace the environment by a test compatible one
        componentManager.registerComponent(TestEnvironment.class);
        
        // For performance reasons we mock some components to avoid having to draw all oldcore components

        // Some components we don't really use and which trigger a lot of dependencies
        componentManager.registerMockComponent(TemplateManager.class);
        componentManager.registerMockComponent(ModelBridge.class);
        componentManager.registerMockComponent(QueryManager.class);
        componentManager.registerMockComponent(ReferenceUpdater.class);
        componentManager.registerMockComponent(AuthorizationManager.class);
        componentManager.registerMockComponent(ContextualAuthorizationManager.class);

        // Used by EffectiveAuthorSetterListener from oldcore.
        DefaultParameterizedType currentUserReferenceResolverType =
            new DefaultParameterizedType(null, UserReferenceResolver.class, CurrentUserReference.class);
        componentManager.registerMockComponent(currentUserReferenceResolverType);

        // Macro Reference Resolver
        DocumentReferenceResolver<String> macroResolver = componentManager.registerMockComponent(
            new DefaultParameterizedType(null, DocumentReferenceResolver.class, String.class), "macro");
        DocumentReference referencedDocumentReference = new DocumentReference("Wiki", "Space", "Page");
        when(macroResolver.resolve(eq("Space.Page"), any(MacroBlock.class))).thenReturn(referencedDocumentReference);

        // Document Access Bridge mock
        // Simulate the XDOM of the referenced document
        DocumentAccessBridge dab = componentManager.registerMockComponent(DocumentAccessBridge.class);
        DocumentModelBridge dmb = mock(DocumentModelBridge.class);
        when(dab.getTranslatedDocumentInstance(referencedDocumentReference)).thenReturn(dmb);

        Parser parser = componentManager.getInstance(Parser.class, "xwiki/2.1");
        XDOM xdom = parser.parse(new StringReader("= heading1 =\n==heading2=="));
        when(dmb.getPreparedXDOM()).thenReturn(xdom);

        // Replace the context component manager
        componentManager.registerComponent(ComponentManager.class, "context", componentManager);
    }
}
