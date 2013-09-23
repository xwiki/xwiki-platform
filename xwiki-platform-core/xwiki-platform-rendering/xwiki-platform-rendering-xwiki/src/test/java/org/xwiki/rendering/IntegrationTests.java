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
package org.xwiki.rendering;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.runner.RunWith;
import org.xwiki.component.descriptor.DefaultComponentDescriptor;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.AttachmentReferenceResolver;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.test.MockWikiModel;
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

        // Attachment Reference Resolver Mock
        final AttachmentReferenceResolver<String> mockResolver = mockery.mock(AttachmentReferenceResolver.class);
        mockery.checking(new Expectations() {{
            allowing(mockResolver).resolve("Space.ExistingPage@my.png");
            will(returnValue(
                new AttachmentReference("my.png", new DocumentReference("wiki", "Space", "ExistingPage"))));
        }});
        DefaultComponentDescriptor<AttachmentReferenceResolver<String>> descriptorARS =
            new DefaultComponentDescriptor<AttachmentReferenceResolver<String>>();
        descriptorARS.setRoleType(AttachmentReferenceResolver.TYPE_STRING);
        descriptorARS.setRoleHint("current");
        componentManager.registerComponent(descriptorARS, mockResolver);

        // WikiModel Mock
        componentManager.registerComponent(MockWikiModel.getComponentDescriptor());
    }
}
