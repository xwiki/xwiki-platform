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
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.bridge.SkinAccessBridge;
import org.xwiki.component.descriptor.ComponentDescriptor;
import org.xwiki.component.descriptor.DefaultComponentDescriptor;
import org.xwiki.component.embed.EmbeddableComponentManager;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.EntityReferenceValueProvider;
import org.xwiki.rendering.scaffolding.RenderingTestSuite;
import org.xwiki.test.ComponentManagerTestSetup;

/**
 * All Rendering integration tests defined in text files using a special format.
 * 
 * @version $Id$
 * @since 1.8RC2
 */
public class RenderingTests extends TestCase
{
    public static Test suite() throws Exception
    {
        RenderingTestSuite suite = new RenderingTestSuite("Test user avatar macro");
        
        suite.addTestsFromResource("macrouseravatar1", true);
        suite.addTestsFromResource("macrouseravatar2", true);
        suite.addTestsFromResource("macrouseravatar3", true);

        ComponentManagerTestSetup testSetup = new ComponentManagerTestSetup(suite);
        setUpMocks(testSetup.getComponentManager());

        return testSetup;
    }
    
    public static void setUpMocks(EmbeddableComponentManager componentManager) throws Exception
    {
        Mockery context = new Mockery();

        // Skin Access Bridge Mock
        final SkinAccessBridge mockSkinAccessBridge =
            registerMockComponent(componentManager, context, SkinAccessBridge.class);
        context.checking(new Expectations() {{
            allowing(mockSkinAccessBridge).getSkinFile("noavatar.png"); will(returnValue("/xwiki/noavatar.png"));
        }});        

        // Document Access Bridge Mock
        final DocumentReference adminUserReference = new DocumentReference("wiki", "XWiki", "Admin");
        final DocumentReference userWithoutAvatarReference =
            new DocumentReference("wiki", "XWiki", "ExistingUserWithoutAvatar");
        final DocumentReference userClassReference = new DocumentReference("wiki", "XWiki", "XWikiUsers");
        final DocumentAccessBridge mockDocumentAccessBridge =
            registerMockComponent(componentManager, context, DocumentAccessBridge.class);
        context.checking(new Expectations() {{
            allowing(mockDocumentAccessBridge).exists(adminUserReference); will(returnValue(true));
            allowing(mockDocumentAccessBridge).exists(userWithoutAvatarReference); will(returnValue(true));
            allowing(mockDocumentAccessBridge).exists(with(any(String.class))); will(returnValue(false));
            allowing(mockDocumentAccessBridge).getProperty(adminUserReference, userClassReference, "avatar");
                will(returnValue("mockAvatar.png"));
            allowing(mockDocumentAccessBridge).getProperty(userWithoutAvatarReference, userClassReference,
                "avatar"); will(returnValue(null));
        }});

        // Document Resolver Mock
        final DocumentReferenceResolver<String> mockDocumentReferenceResolver =
            registerMockComponent(componentManager, context, DocumentReferenceResolver.class, "current");
        context.checking(new Expectations() {{
            allowing(mockDocumentReferenceResolver).resolve("XWiki.Admin"); will(returnValue(adminUserReference));
            allowing(mockDocumentReferenceResolver).resolve("XWiki.ExistingUserWithoutAvatar");
                will(returnValue(userWithoutAvatarReference));
        }});

        // Entity Reference Serializer Mock
        final EntityReferenceSerializer<String> mockEntityReferenceSerializer =
            registerMockComponent(componentManager, context, EntityReferenceSerializer.class, "compactwiki");
        context.checking(new Expectations() {{
            allowing(mockEntityReferenceSerializer).serialize(
                new AttachmentReference("mockAvatar.png", adminUserReference));
                will(returnValue("XWiki.Admin@mockAvatar.png"));
        }});

        // Entity Reference Serializer Mock
        final EntityReferenceValueProvider mockEntityReferenceValueProvider =
            registerMockComponent(componentManager, context, EntityReferenceValueProvider.class, "current");
        context.checking(new Expectations() {{
            allowing(mockEntityReferenceValueProvider).getDefaultValue(EntityType.WIKI); will(returnValue("wiki"));
        }});
    }

    private static <T> T registerMockComponent(EmbeddableComponentManager componentManager, Mockery mockery,
        Class<T> role, String hint) throws Exception
    {
        DefaultComponentDescriptor<T> descriptor = createComponentDescriptor(role);
        descriptor.setRoleHint(hint);
        return registerMockComponent(componentManager, mockery, descriptor);
    }

    private static <T> T registerMockComponent(EmbeddableComponentManager componentManager, Mockery mockery,
        Class<T> role) throws Exception
    {
        return registerMockComponent(componentManager, mockery, createComponentDescriptor(role));
    }

    private static <T> T registerMockComponent(EmbeddableComponentManager componentManager, Mockery mockery,
        ComponentDescriptor<T> descriptor) throws Exception
    {
        T mock = mockery.mock(descriptor.getRole());
        componentManager.registerComponent(descriptor, mock);
        return mock;
    }

    private static <T> DefaultComponentDescriptor<T> createComponentDescriptor(Class<T> role)
    {
        DefaultComponentDescriptor<T> descriptor = new DefaultComponentDescriptor<T>();
        descriptor.setRole(role);
        return descriptor;
    }
}
