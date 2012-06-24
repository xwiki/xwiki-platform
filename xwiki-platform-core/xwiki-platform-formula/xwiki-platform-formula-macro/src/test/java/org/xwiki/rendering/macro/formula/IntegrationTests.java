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
import org.xwiki.component.descriptor.DefaultComponentDescriptor;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.formula.ImageData;
import org.xwiki.formula.ImageStorage;
import org.xwiki.rendering.test.integration.RenderingTestSuite;

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
    public void initialize(ComponentManager componentManager) throws Exception
    {
        Mockery mockery = new JUnit4Mockery();

        // Document Access Bridge Mock
        final DocumentAccessBridge mockDocumentAccessBridge = mockery.mock(DocumentAccessBridge.class);
        DefaultComponentDescriptor<DocumentAccessBridge> descriptorDAB =
            new DefaultComponentDescriptor<DocumentAccessBridge>();
        descriptorDAB.setRoleType(DocumentAccessBridge.class);
        componentManager.registerComponent(descriptorDAB, mockDocumentAccessBridge);

        // Image Storage Mock
        final ImageStorage mockImageStorage = mockery.mock(ImageStorage.class);
        DefaultComponentDescriptor<ImageStorage> descriptorIS = new DefaultComponentDescriptor<ImageStorage>();
        descriptorIS.setRoleType(ImageStorage.class);
        componentManager.registerComponent(descriptorIS, mockImageStorage);

        // Configuration Mock
        final FormulaMacroConfiguration mockConfiguration = mockery.mock(FormulaMacroConfiguration.class);
        DefaultComponentDescriptor<FormulaMacroConfiguration> descriptorEMC =
            new DefaultComponentDescriptor<FormulaMacroConfiguration>();
        descriptorEMC.setRoleType(FormulaMacroConfiguration.class);
        componentManager.registerComponent(descriptorEMC, mockConfiguration);

        mockery.checking(new Expectations()
        {
            {
                atLeast(2).of(mockDocumentAccessBridge).getDocumentURL(null, "tex", null, null);
                will(returnValue("/xwiki/bin/view/Main/"));

                atLeast(2).of(mockConfiguration).getRenderer();
                will(returnValue("snuggletex"));

                atLeast(2).of(mockImageStorage).get(with(any(String.class)));
                will(returnValue(null));

                atLeast(2).of(mockImageStorage).put(with(any(String.class)), with(any(ImageData.class)));
            }
        });
    }
}
