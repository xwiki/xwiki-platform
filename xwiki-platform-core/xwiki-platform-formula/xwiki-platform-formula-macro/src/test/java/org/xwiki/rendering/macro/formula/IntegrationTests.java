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

import org.junit.runner.RunWith;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.formula.FormulaRenderer;
import org.xwiki.formula.ImageStorage;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.test.integration.RenderingTestSuite;
import org.xwiki.test.annotation.AllComponents;
import org.xwiki.test.mockito.MockitoComponentManager;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Run all tests found in {@code *.test} files located in the classpath. These {@code *.test} files must follow the
 * conventions described in {@link org.xwiki.rendering.test.integration.TestDataParser}.
 *
 * @version $Id$
 * @since 3.0RC1
 */
@RunWith(RenderingTestSuite.class)
@AllComponents
public class IntegrationTests
{
    @RenderingTestSuite.Initialized
    public void initialize(MockitoComponentManager componentManager) throws Exception
    {
        // Document Access Bridge Mock
        final DocumentAccessBridge mockDocumentAccessBridge =
            componentManager.registerMockComponent(DocumentAccessBridge.class);

        // Image Storage Mock
        final ImageStorage mockImageStorage = componentManager.registerMockComponent(ImageStorage.class);

        // Configuration Mock
        final FormulaMacroConfiguration mockConfiguration =
            componentManager.registerMockComponent(FormulaMacroConfiguration.class);

        DocumentReference documentReference = new DocumentReference("wiki", "space", "page");
        when(mockDocumentAccessBridge.getCurrentDocumentReference()).thenReturn(documentReference);

        AttachmentReference attachmentReference = new AttachmentReference(
            "06fbba0acf130efd9e147fdfe91a943cc4f3e29972c6cd1d972e9aabf0900966", documentReference);
        when(mockDocumentAccessBridge.getAttachmentURL(attachmentReference, false)).thenReturn(
            "/xwiki/bin/view/space/page/06fbba0acf130efd9e147fdfe91a943cc4f3e29972c6cd1d972e9aabf0900966");

        when(mockConfiguration.getRenderer()).thenReturn("snuggletex");
        when(mockConfiguration.getDefaultType()).thenReturn(FormulaRenderer.Type.DEFAULT);
        when(mockImageStorage.get(any(String.class))).thenReturn(null);
    }
}