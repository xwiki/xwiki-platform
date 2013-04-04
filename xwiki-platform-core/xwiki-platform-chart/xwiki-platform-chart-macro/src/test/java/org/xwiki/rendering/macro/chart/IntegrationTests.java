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

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.runner.RunWith;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.display.internal.DocumentDisplayer;
import org.xwiki.display.internal.DocumentDisplayerParameters;
import org.xwiki.model.ModelContext;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.test.integration.RenderingTestSuite;
import org.xwiki.test.jmock.MockingComponentManager;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.rendering.parser.Parser;
import org.xwiki.rendering.block.XDOM;

import java.io.InputStreamReader;

/**
 * Run all tests found in {@code *.test} files located in the classpath. These {@code *.test} files must follow the
 * conventions described in {@link org.xwiki.rendering.test.integration.TestDataParser}.
 *
 * @version $Id$
 * @since 3.0RC1
 */
@RunWith(RenderingTestSuite.class)
public class IntegrationTests
{
    private final static String WIKI_CONTENT_FILE = "wiki.txt";

    @RenderingTestSuite.Initialized
    public void initialize(MockingComponentManager componentManager) throws Exception
    {
        Mockery mockery = new JUnit4Mockery();

        final ModelContext modelContext = componentManager.registerMockComponent(mockery, ModelContext.class);
        mockery.checking(new Expectations() {{
            allowing(modelContext).getCurrentEntityReference();
                will(returnValue(new WikiReference("currentWiki")));
        }});

        // Document Access Bridge Mock
        final DocumentAccessBridge dab = componentManager.registerMockComponent(mockery, DocumentAccessBridge.class);
        final DocumentReference documentReference = new DocumentReference("wiki", "space", "page");
        final DocumentReference currentDocumentReference =
            new DocumentReference("currentwiki", "currentspace", "currentpage");
        final DocumentModelBridge document = mockery.mock(DocumentModelBridge.class);
        final DocumentDisplayer displayer = componentManager.registerMockComponent(mockery, DocumentDisplayer.class);

        Parser parser = componentManager.getInstance(Parser.class, Syntax.XWIKI_2_0.toIdString());
        final XDOM xdom = parser.parse(new InputStreamReader(
            getClass().getClassLoader().getResourceAsStream(WIKI_CONTENT_FILE)));

        final AuthorizationManager authorizationManager =
            componentManager.registerMockComponent(mockery,  AuthorizationManager.class);

        mockery.checking(new Expectations() {{
            allowing(dab).getDocumentURL(new DocumentReference("currentWiki", "space", "page"), "temp", null, null);
                will(returnValue("temppath"));

            allowing(dab).getCurrentDocumentReference();
                will(returnValue(currentDocumentReference));
            allowing(dab).exists(documentReference);
                will(returnValue(true));
            allowing(dab).getDocument(documentReference);
                will(returnValue(document));
            allowing(displayer).display(with(equal(document)), with(any(DocumentDisplayerParameters.class)));
                will(returnValue(xdom));

            allowing(dab).getCurrentUserReference();
                will(returnValue(null));
            allowing(authorizationManager).hasAccess(Right.VIEW, null, documentReference);
                will(returnValue(true));
        }});
    }
}
