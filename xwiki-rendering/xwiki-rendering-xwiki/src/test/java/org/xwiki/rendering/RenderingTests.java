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

import junit.framework.Test;
import junit.framework.TestCase;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.xwiki.component.descriptor.DefaultComponentDescriptor;
import org.xwiki.component.embed.EmbeddableComponentManager;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.AttachmentReferenceResolver;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.scaffolding.MockWikiModel;
import org.xwiki.rendering.scaffolding.RenderingTestSuite;
import org.xwiki.test.ComponentManagerTestSetup;

/**
 * All Rendering integration tests defined in text files using a special format.
 *
 * @version $Id$
 * @since 2.5RC1
 */
public class RenderingTests extends TestCase
{
    public static Test suite() throws Exception
    {
        RenderingTestSuite suite =
            new RenderingTestSuite("Rendering tests requiring the wiki notion and that run inside XWiki");

        ComponentManagerTestSetup testSetup = new ComponentManagerTestSetup(suite);
        testSetup.addComponentDescriptor(MockWikiModel.getComponentDescriptor());
        setUpMocks(testSetup.getComponentManager());

        return testSetup;
    }

    public static void setUpMocks(EmbeddableComponentManager componentManager) throws Exception
    {
        Mockery mockery = new Mockery();

        // Attachment Reference Resolver Mock
        final AttachmentReferenceResolver<String> mockResolver = mockery.mock(AttachmentReferenceResolver.class);
        mockery.checking(new Expectations() {{
            oneOf(mockResolver).resolve("Space.ExistingPage@my.png");
            will(returnValue(
                new AttachmentReference("my.png", new DocumentReference("wiki", "Space", "ExistingPage"))));
        }});
        DefaultComponentDescriptor<AttachmentReferenceResolver> descriptorARS =
            new DefaultComponentDescriptor<AttachmentReferenceResolver>();
        descriptorARS.setRole(AttachmentReferenceResolver.class);
        descriptorARS.setRoleHint("current");
        componentManager.registerComponent(descriptorARS, mockResolver);
    }
}
