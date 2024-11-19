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
package org.xwiki.rendering.macro.chart;

import java.io.InputStreamReader;

import org.mockito.Mockito;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.display.internal.DocumentDisplayer;
import org.xwiki.display.internal.DocumentDisplayerParameters;
import org.xwiki.model.ModelContext;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.SpaceReferenceResolver;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.parser.Parser;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.test.integration.junit5.RenderingTests;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.annotation.AllComponents;
import org.xwiki.test.mockito.MockitoComponentManager;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.when;

/**
 * Run all tests found in {@code *.test} files located in the classpath. These {@code *.test} files must follow the
 * conventions described in {@link org.xwiki.rendering.test.integration.TestDataParser}.
 *
 * @version $Id$
 * @since 3.0RC1
 */
@AllComponents
public class IntegrationTests implements RenderingTests
{
    private final static String WIKI_CONTENT_FILE = "wiki.txt";

    @RenderingTests.Initialized
    public void initialize(MockitoComponentManager componentManager) throws Exception
    {
        ModelContext modelContext = componentManager.registerMockComponent(ModelContext.class);
        when(modelContext.getCurrentEntityReference()).thenReturn(new WikiReference("currentWiki"));

        // Document Access Bridge mock
        DocumentAccessBridge dab = componentManager.registerMockComponent(DocumentAccessBridge.class);
        DocumentReference documentReference = new DocumentReference("wiki", "space", "page");
        DocumentReference currentDocumentReference =
            new DocumentReference("currentwiki", "currentspace", "currentpage");
        DocumentModelBridge document = Mockito.mock(DocumentModelBridge.class);
        when(dab.getDocumentURL(new DocumentReference("currentWiki", "space", "page"), "temp", null, null))
            .thenReturn("temppath");
        when(dab.getCurrentDocumentReference()).thenReturn(currentDocumentReference);
        when(dab.exists(documentReference)).thenReturn(true);
        when(dab.getDocumentInstance(documentReference)).thenReturn(document);
        when(dab.getCurrentUserReference()).thenReturn(null);

        DocumentDisplayer displayer = componentManager.registerMockComponent(DocumentDisplayer.class);
        Parser parser = componentManager.getInstance(Parser.class, Syntax.XWIKI_2_0.toIdString());
        final XDOM xdom = parser.parse(new InputStreamReader(
            getClass().getClassLoader().getResourceAsStream(WIKI_CONTENT_FILE)));
        when(displayer.display(eq(document), any(DocumentDisplayerParameters.class))).thenReturn(xdom);

        AuthorizationManager authorizationManager = componentManager.registerMockComponent(AuthorizationManager.class);
        when(authorizationManager.hasAccess(Right.VIEW, null, documentReference)).thenReturn(true);

        componentManager.registerMockComponent(DocumentReferenceResolver.TYPE_STRING, "current");
        componentManager.registerMockComponent(SpaceReferenceResolver.TYPE_STRING, "current");
    }
}
