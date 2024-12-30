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

import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.AttachmentReferenceResolver;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.test.MockWikiModel;
import org.xwiki.rendering.test.integration.Initialized;
import org.xwiki.rendering.test.integration.junit5.RenderingTest;
import org.xwiki.test.annotation.AllComponents;
import org.xwiki.test.mockito.MockitoComponentManager;

import static org.mockito.Mockito.when;

/**
 * Run all tests found in {@code *.test} files located in the classpath. These {@code *.test} files must follow the
 * conventions described in {@link org.xwiki.rendering.test.integration.TestDataParser}.
 *
 * @version $Id$
 * @since 3.0RC1
 */
@AllComponents
public class IntegrationTests extends RenderingTest
{
    @Initialized
    public void initialize(MockitoComponentManager componentManager) throws Exception
    {
        // Attachment Reference Resolver Mock
        AttachmentReferenceResolver<String> ar = componentManager.registerMockComponent(
            new DefaultParameterizedType(null, AttachmentReferenceResolver.class, String.class), "current");
        when(ar.resolve("Space.ExistingPage@my.png")).thenReturn(
            new AttachmentReference("my.png", new DocumentReference("wiki", "Space", "ExistingPage")));

        // WikiModel Mock
        componentManager.registerComponent(MockWikiModel.getComponentDescriptor());
    }
}
