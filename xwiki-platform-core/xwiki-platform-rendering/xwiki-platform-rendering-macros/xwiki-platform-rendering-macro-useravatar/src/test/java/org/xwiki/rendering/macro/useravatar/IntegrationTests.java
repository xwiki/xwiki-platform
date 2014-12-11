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
package org.xwiki.rendering.macro.useravatar;

import java.lang.reflect.Type;

import org.jmock.Expectations;
import org.jmock.Mockery;
import org.jmock.integration.junit4.JUnit4Mockery;
import org.junit.runner.RunWith;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.bridge.SkinAccessBridge;
import org.xwiki.component.descriptor.ComponentDescriptor;
import org.xwiki.component.descriptor.DefaultComponentDescriptor;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.util.ReflectionUtils;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.EntityReferenceValueProvider;
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

        // Skin Access Bridge Mock
        final SkinAccessBridge mockSkinAccessBridge =
            registerMockComponent(componentManager, mockery, SkinAccessBridge.class);
        mockery.checking(new Expectations() {{
            allowing(mockSkinAccessBridge).getSkinFile("noavatar.png"); will(returnValue("/xwiki/noavatar.png"));
        }});

        // Document Access Bridge Mock
        final DocumentReference adminUserReference = new DocumentReference("wiki", "XWiki", "Admin");
        final DocumentReference userWithoutAvatarReference =
            new DocumentReference("wiki", "XWiki", "ExistingUserWithoutAvatar");
        final DocumentReference userNotExistingReference = new DocumentReference("wiki", "XWiki", "UserNotExisting");
        final DocumentReference userWithNonExistingAvatarFileReference =
            new DocumentReference("wiki", "XWiki", "UserWithNonExistingAvatarFile");
        final DocumentReference userClassReference = new DocumentReference("wiki", "XWiki", "XWikiUsers");
        final DocumentAccessBridge mockDocumentAccessBridge =
            registerMockComponent(componentManager, mockery, DocumentAccessBridge.class);
        mockery.checking(new Expectations() {{
            allowing(mockDocumentAccessBridge).exists(adminUserReference); will(returnValue(true));
            allowing(mockDocumentAccessBridge).exists(userWithoutAvatarReference); will(returnValue(true));
            allowing(mockDocumentAccessBridge).exists(with(any(String.class))); will(returnValue(false));
            allowing(mockDocumentAccessBridge).exists(userNotExistingReference); will(returnValue(false));
            allowing(mockDocumentAccessBridge).exists(userWithNonExistingAvatarFileReference); will(returnValue(true));

            allowing(mockDocumentAccessBridge).getProperty(adminUserReference, userClassReference, "avatar");
                will(returnValue("mockAvatar.png"));
            allowing(mockDocumentAccessBridge).getProperty(userWithoutAvatarReference, userClassReference,
                "avatar"); will(returnValue(null));
            allowing(mockDocumentAccessBridge).getProperty(userWithNonExistingAvatarFileReference,
                userClassReference, "avatar"); will(returnValue("mockAvatar.png"));

            allowing(mockDocumentAccessBridge).getAttachmentVersion(new AttachmentReference("mockAvatar.png",
                adminUserReference)); will(returnValue("1.1"));
            allowing(mockDocumentAccessBridge).getAttachmentVersion(new AttachmentReference("mockAvatar.png",
                userWithNonExistingAvatarFileReference)); will(returnValue(null));
        }});

        // Document Resolver Mock
        final DocumentReferenceResolver<String> mockDocumentReferenceResolver =
            registerMockComponent(componentManager, mockery, DocumentReferenceResolver.TYPE_STRING, "current");
        mockery.checking(new Expectations() {{
            allowing(mockDocumentReferenceResolver).resolve("XWiki.Admin",
                new EntityReference("XWiki", EntityType.SPACE));
                will(returnValue(adminUserReference));
            allowing(mockDocumentReferenceResolver).resolve("XWiki.ExistingUserWithoutAvatar",
                new EntityReference("XWiki", EntityType.SPACE));
                will(returnValue(userWithoutAvatarReference));
            allowing(mockDocumentReferenceResolver).resolve("XWiki.UserNotExisting",
                new EntityReference("XWiki", EntityType.SPACE));
                will(returnValue(userNotExistingReference));
            allowing(mockDocumentReferenceResolver).resolve("XWiki.UserWithNonExistingAvatarFile",
                new EntityReference("XWiki", EntityType.SPACE));
                will(returnValue(userWithNonExistingAvatarFileReference));
        }});

        // Entity Reference Serializer Mock
        final EntityReferenceSerializer<String> mockEntityReferenceSerializer =
            registerMockComponent(componentManager, mockery, EntityReferenceSerializer.TYPE_STRING, "compactwiki");
        mockery.checking(new Expectations() {{
            allowing(mockEntityReferenceSerializer).serialize(
                new AttachmentReference("mockAvatar.png", adminUserReference));
                will(returnValue("XWiki.Admin@mockAvatar.png"));
            allowing(mockEntityReferenceSerializer).serialize(userNotExistingReference);
                will(returnValue("XWiki.UserNotExisting"));
            allowing(mockEntityReferenceSerializer).serialize(
                new AttachmentReference("mockAvatar.png", userWithNonExistingAvatarFileReference));
                will(returnValue("XWiki.UserWithNonExistingAvatarFile@mockAvatar.png"));
        }});

        // Entity Reference Serializer Mock
        final EntityReferenceValueProvider mockEntityReferenceValueProvider =
            registerMockComponent(componentManager, mockery, EntityReferenceValueProvider.class, "current");
        mockery.checking(new Expectations() {{
            allowing(mockEntityReferenceValueProvider).getDefaultValue(EntityType.WIKI); will(returnValue("wiki"));
        }});
    }

    private static <T> T registerMockComponent(ComponentManager componentManager, Mockery mockery,
        Type role, String hint) throws Exception
    {
        DefaultComponentDescriptor<T> descriptor = createComponentDescriptor(role);
        descriptor.setRoleHint(hint);

        return registerMockComponent(componentManager, mockery, descriptor);
    }

    private static <T> T registerMockComponent(ComponentManager componentManager, Mockery mockery,
        Type role) throws Exception
    {
        return registerMockComponent(componentManager, mockery, IntegrationTests.<T> createComponentDescriptor(role));
    }

    private static <T> T registerMockComponent(ComponentManager componentManager, Mockery mockery,
        ComponentDescriptor<T> descriptor) throws Exception
    {
        T mock = mockery.mock((Class<T>)ReflectionUtils.getTypeClass(descriptor.getRoleType()));
        componentManager.registerComponent(descriptor, mock);

        return mock;
    }

    private static <T> DefaultComponentDescriptor<T> createComponentDescriptor(Type role)
    {
        DefaultComponentDescriptor<T> descriptor = new DefaultComponentDescriptor<T>();
        descriptor.setRoleType(role);

        return descriptor;
    }
}
