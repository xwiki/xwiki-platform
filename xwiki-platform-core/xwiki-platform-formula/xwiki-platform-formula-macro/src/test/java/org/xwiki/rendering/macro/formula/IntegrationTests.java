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
package org.xwiki.rendering.macro.formula;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.runner.RunWith;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.formula.ImageData;
import org.xwiki.formula.ImageStorage;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.test.integration.RenderingTestSuite;
import org.xwiki.test.jmock.MockingComponentManager;

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
    @RenderingTestSuite.Initialized
    public void initialize(MockingComponentManager componentManager) throws Exception
    {
        Mockery mockery = new JUnit4Mockery();

        // Document Access Bridge Mock
        final DocumentAccessBridge mockDocumentAccessBridge =
            componentManager.registerMockComponent(mockery, DocumentAccessBridge.class);

        // Image Storage Mock
        final ImageStorage mockImageStorage = componentManager.registerMockComponent(mockery, ImageStorage.class);

        // Configuration Mock
        final FormulaMacroConfiguration mockConfiguration =
            componentManager.registerMockComponent(mockery, FormulaMacroConfiguration.class);

        mockery.checking(new Expectations()
        {
            {
                atLeast(1).of(mockDocumentAccessBridge).getCurrentDocumentReference();
                DocumentReference documentReference = new DocumentReference("wiki", "space", "page");
                will(returnValue(documentReference));

                AttachmentReference attachmentReference = new AttachmentReference(
                    "06fbba0acf130efd9e147fdfe91a943cc4f3e29972c6cd1d972e9aabf0900966", documentReference);
                atLeast(2).of(mockDocumentAccessBridge).getAttachmentURL(attachmentReference, false);
                will(returnValue(
                    "/xwiki/bin/view/space/page/06fbba0acf130efd9e147fdfe91a943cc4f3e29972c6cd1d972e9aabf0900966"));

                atLeast(2).of(mockConfiguration).getRenderer();
                will(returnValue("snuggletex"));

                atLeast(2).of(mockImageStorage).get(with(any(String.class)));
                will(returnValue(null));

                atLeast(2).of(mockImageStorage).put(with(any(String.class)), with(any(ImageData.class)));
            }
        });
    }
}
