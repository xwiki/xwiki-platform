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

import org.junit.jupiter.api.AfterEach;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.formula.FormulaRenderer;
import org.xwiki.formula.ImageStorage;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.test.integration.junit5.RenderingTests;
import org.xwiki.test.annotation.AllComponents;
import org.xwiki.test.mockito.MockitoComponentManager;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
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
    private DocumentAccessBridge mockDocumentAccessBridge;

    private ImageStorage mockImageStorage;

    private FormulaMacroConfiguration mockConfiguration;

    private AttachmentReference attachmentReference1;

    private AttachmentReference attachmentReference2;

    @RenderingTests.Initialized
    public void initialize(MockitoComponentManager componentManager) throws Exception
    {
        // Document Access Bridge Mock
        mockDocumentAccessBridge = componentManager.registerMockComponent(DocumentAccessBridge.class);

        // Image Storage Mock
        mockImageStorage = componentManager.registerMockComponent(ImageStorage.class);

        // Configuration Mock
        mockConfiguration = componentManager.registerMockComponent(FormulaMacroConfiguration.class);

        DocumentReference documentReference = new DocumentReference("wiki", "space", "page");
        when(mockDocumentAccessBridge.getCurrentDocumentReference()).thenReturn(documentReference);

        attachmentReference1 = new AttachmentReference(
            "06fbba0acf130efd9e147fdfe91a943cc4f3e29972c6cd1d972e9aabf0900966", documentReference);
        when(mockDocumentAccessBridge.getAttachmentURL(attachmentReference1, false)).thenReturn(
            "/xwiki/bin/view/space/page/06fbba0acf130efd9e147fdfe91a943cc4f3e29972c6cd1d972e9aabf0900966");

        attachmentReference2 = new AttachmentReference(
            "190ef2f68e7fbd75c869d74dea959b1a48faadefc7a0c9219e3e94d005821935", documentReference);
        when(mockDocumentAccessBridge.getAttachmentURL(attachmentReference2, false)).thenReturn(
            "/xwiki/bin/view/space/page/190ef2f68e7fbd75c869d74dea959b1a48faadefc7a0c9219e3e94d005821935");

        when(mockConfiguration.getRenderer()).thenReturn("snuggletex");
        when(mockConfiguration.getDefaultType()).thenReturn(FormulaRenderer.Type.DEFAULT);
        when(mockConfiguration.getDefaultFontSize()).thenReturn(FormulaRenderer.FontSize.DEFAULT);
        when(mockImageStorage.get(any(String.class))).thenReturn(null);
    }

    @AfterEach
    public void after()
    {
        verify(mockDocumentAccessBridge, times(1)).getCurrentDocumentReference();

        verify(mockConfiguration, times(1)).getRenderer();
        verify(mockConfiguration, times(1)).getDefaultType();

        verify(mockImageStorage, times(1)).get(any(String.class));
    }
}