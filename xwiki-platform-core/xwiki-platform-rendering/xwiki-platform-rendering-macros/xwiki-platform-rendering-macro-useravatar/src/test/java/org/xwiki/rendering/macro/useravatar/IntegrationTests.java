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

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.bridge.SkinAccessBridge;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.EntityReferenceValueProvider;
import org.xwiki.rendering.test.integration.junit5.RenderingTests;
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
@RenderingTests.Scope(pattern = "macrouseravatar.*")
public class IntegrationTests implements RenderingTests
{
    @RenderingTests.Initialized
    public void initialize(MockitoComponentManager componentManager) throws Exception
    {
        // Skin Access Bridge Mock
        SkinAccessBridge sab = componentManager.registerMockComponent(SkinAccessBridge.class);
        when(sab.getSkinFile("icons/xwiki/noavatar.png")).thenReturn("/xwiki/resources/icons/xwiki/noavatar.png");

        // Document Access Bridge Mock
        DocumentReference adminUserReference = new DocumentReference("wiki", "XWiki", "Admin");
        DocumentReference userWithoutAvatarReference =
            new DocumentReference("wiki", "XWiki", "ExistingUserWithoutAvatar");
        DocumentReference userNotExistingReference = new DocumentReference("wiki", "XWiki", "UserNotExisting");
        DocumentReference userWithNonExistingAvatarFileReference =
            new DocumentReference("wiki", "XWiki", "UserWithNonExistingAvatarFile");
        DocumentReference userWithExceptionRetrievingAvatarFileReference =
            new DocumentReference("wiki", "XWiki", "UserWithExceptionRetrievingAvatarFile");
        DocumentReference userClassReference = new DocumentReference("wiki", "XWiki", "XWikiUsers");
        DocumentAccessBridge dab = componentManager.registerMockComponent(DocumentAccessBridge.class);
        when(dab.exists(adminUserReference)).thenReturn(true);
        when(dab.exists(userWithoutAvatarReference)).thenReturn(true);
        when(dab.exists(userWithNonExistingAvatarFileReference)).thenReturn(true);
        when(dab.exists(userWithExceptionRetrievingAvatarFileReference)).thenReturn(true);
        when(dab.getProperty(adminUserReference, userClassReference, "avatar"))
            .thenReturn("mockAvatar.png");
        when(dab.getProperty(userWithoutAvatarReference, userClassReference, "avatar"))
            .thenReturn(null);
        when(dab.getProperty(userWithNonExistingAvatarFileReference, userClassReference, "avatar"))
            .thenReturn("mockAvatar.png");
        when(dab.getProperty(userWithExceptionRetrievingAvatarFileReference, userClassReference, "avatar"))
            .thenReturn("mockAvatar.png");
        when(dab.getAttachmentVersion(new AttachmentReference("mockAvatar.png", adminUserReference))).thenReturn("1.1");
        when(dab.getAttachmentVersion(new AttachmentReference("mockAvatar.png",
            userWithNonExistingAvatarFileReference))).thenReturn(null);
        when(dab.getAttachmentVersion(new AttachmentReference("mockAvatar.png",
            userWithExceptionRetrievingAvatarFileReference))).thenThrow(new Exception("Sum Ting Wong"));

        // Document Resolver Mock
        DocumentReferenceResolver<String> drr =
            componentManager.registerMockComponent(DocumentReferenceResolver.TYPE_STRING, "current");
        when(drr.resolve("XWiki.Admin", new EntityReference("XWiki", EntityType.SPACE))).thenReturn(adminUserReference);
        when(drr.resolve("XWiki.ExistingUserWithoutAvatar", new EntityReference("XWiki", EntityType.SPACE)))
            .thenReturn(userWithoutAvatarReference);
        when(drr.resolve("XWiki.UserNotExisting", new EntityReference("XWiki", EntityType.SPACE)))
            .thenReturn(userNotExistingReference);
        when(drr.resolve("XWiki.UserWithNonExistingAvatarFile", new EntityReference("XWiki", EntityType.SPACE)))
            .thenReturn(userWithNonExistingAvatarFileReference);
        when(drr.resolve("XWiki.UserWithExceptionRetrievingAvatarFile", new EntityReference("XWiki", EntityType.SPACE)))
            .thenReturn(userWithExceptionRetrievingAvatarFileReference);

        // Entity Reference Serializer Mock
        EntityReferenceSerializer<String> ers =
            componentManager.registerMockComponent(EntityReferenceSerializer.TYPE_STRING, "compactwiki");
        when(ers.serialize(new AttachmentReference("mockAvatar.png", adminUserReference)))
            .thenReturn("XWiki.Admin@mockAvatar.png");
        when(ers.serialize(userNotExistingReference)).thenReturn("XWiki.UserNotExisting");
        when(ers.serialize(new AttachmentReference("mockAvatar.png", userWithNonExistingAvatarFileReference)))
            .thenReturn("XWiki.UserWithNonExistingAvatarFile@mockAvatar.png");
        when(ers.serialize(new AttachmentReference("mockAvatar.png", userWithExceptionRetrievingAvatarFileReference)))
            .thenReturn("XWiki.UserWithExceptionRetrievingAvatarFile@mockAvatar.png");
        when(ers.serialize(userWithExceptionRetrievingAvatarFileReference))
            .thenReturn("XWiki.UserWithExceptionRetrievingAvatarFile");

        // Entity Reference Serializer Mock
        EntityReferenceValueProvider ervp =
            componentManager.registerMockComponent(EntityReferenceValueProvider.class, "current");
        when(ervp.getDefaultValue(EntityType.WIKI)).thenReturn("wiki");
    }
}
