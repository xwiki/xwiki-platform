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

import org.apache.commons.io.IOUtils;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.runner.RunWith;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.bridge.DocumentModelBridge;
import org.xwiki.component.descriptor.DefaultComponentDescriptor;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.rendering.syntax.Syntax;
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
        descriptorDAB.setRole(DocumentAccessBridge.class);
        componentManager.registerComponent(descriptorDAB, mockDocumentAccessBridge);

        final DocumentReference documentReference = new DocumentReference("xwiki", "Test", "Test");
        final DocumentModelBridge mockDocument = mockery.mock(DocumentModelBridge.class);

        mockery.checking(new Expectations() {{
            allowing(mockDocumentAccessBridge).getURL(with(any(String.class)), with(equal("charting")),
                with(any(String.class)), with(any(String.class)));
                will(returnValue("http://localhost/charts"));
            allowing(mockDocumentAccessBridge).getCurrentDocumentReference();
                will(returnValue(documentReference));
            allowing(mockDocumentAccessBridge).getDocument(documentReference);
                will(returnValue(mockDocument));
            allowing(mockDocument).getSyntax();
                will(returnValue(Syntax.XWIKI_2_0));
            allowing(mockDocumentAccessBridge).exists(with(any(String.class)));
                will(returnValue(true));
            allowing(mockDocumentAccessBridge).getDocumentSyntaxId(with(any(String.class)));
                will(returnValue("xwiki/2.0"));
            allowing(mockDocumentAccessBridge).getDocumentContent("Test.Test");
                will(returnValue(IOUtils.toString(
                    IntegrationTests.class.getClassLoader().getResourceAsStream("wiki.txt"))));
        }});

        // Document Name Serializer Mock
        final EntityReferenceSerializer mockEntityReferenceSerializer = mockery.mock(EntityReferenceSerializer.class);
        DefaultComponentDescriptor<EntityReferenceSerializer> descriptorERS =
            new DefaultComponentDescriptor<EntityReferenceSerializer>();
        descriptorERS.setRoleType(EntityReferenceSerializer.TYPE_STRING);
        componentManager.registerComponent(descriptorERS, mockEntityReferenceSerializer);

        mockery.checking(new Expectations() {{
            allowing(mockEntityReferenceSerializer).serialize(documentReference); will(returnValue("Test.Test"));
        }});
    }
}
