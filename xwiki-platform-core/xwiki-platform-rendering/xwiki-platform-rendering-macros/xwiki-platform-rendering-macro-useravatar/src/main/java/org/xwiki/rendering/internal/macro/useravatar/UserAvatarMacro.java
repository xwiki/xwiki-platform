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
package org.xwiki.rendering.internal.macro.useravatar;

import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.bridge.SkinAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityManager;
import org.xwiki.model.EntityType;
import org.xwiki.model.ObjectPropertyEntity;
import org.xwiki.model.UniqueReference;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.EntityReferenceValueProvider;
import org.xwiki.model.reference.ObjectPropertyReference;
import org.xwiki.model.reference.ObjectReference;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.ImageBlock;
import org.xwiki.rendering.listener.reference.ResourceReference;
import org.xwiki.rendering.listener.reference.ResourceType;
import org.xwiki.rendering.macro.AbstractMacro;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.macro.useravatar.UserAvatarMacroParameters;
import org.xwiki.rendering.transformation.MacroTransformationContext;

/**
 * Allows displaying the avatar for a specific user.
 * 
 * @version $Id$
 * @since 1.8RC2
 */
@Component
@Named("useravatar")
@Singleton
public class UserAvatarMacro extends AbstractMacro<UserAvatarMacroParameters>
{
    /**
     * The description of the macro.
     */
    private static final String DESCRIPTION = "Allows displaying the avatar for a specific user.";

    /**
     * Space where XWiki user profiles are located.
     */
    private static final String USER_SPACE = "XWiki";

    /**
     * Used to get the user avatar picture from his profile.
     */
    @Inject
    private EntityManager entityManager;

    /**
     * Used to get the default avatar picture when the user doesn't exist.
     */
    @Inject
    private SkinAccessBridge skinAccessBridge;

    /**
     * Used to convert a user reference represented as a String (passed as a macro parameter by the user) to a Document
     * Reference.
     */
    @Inject
    @Named("current")
    private DocumentReferenceResolver<String> currentDocumentReferenceResolver;

    /**
     * Used to convert a Document Reference to string (compact form without the wiki part if it matches the current
     * wiki).
     */
    @Inject
    @Named("compactwiki")
    private EntityReferenceSerializer<String> compactWikiEntityReferenceSerializer;

    /**
     * Used to find out the current Wiki name.
     */
    @Inject
    @Named("current")
    private EntityReferenceValueProvider currentEntityReferenceValueProvider;

    /**
     * Create and initialize the descriptor of the macro.
     */
    public UserAvatarMacro()
    {
        super("User Avatar", DESCRIPTION, UserAvatarMacroParameters.class);
        setDefaultCategory(DEFAULT_CATEGORY_CONTENT);
    }

    @Override
    public List<Block> execute(UserAvatarMacroParameters parameters, String content, MacroTransformationContext context)
        throws MacroExecutionException
    {
        // Transform the user reference passed in parameter into a Document Reference, resolving against the current
        // wiki if the user has not specified the wiki part (e.g. {{useravatar username="VincentMassol"/}} or
        // {{useravatar username="XWiki.VincentMassol"/}}). Note that if the user has specified a space other than
        // "XWiki" then it's ignored and the "XWiki" space will be used. The reason is that currently all users are
        // located in the "XWiki" space.
        DocumentReference userReference = this.currentDocumentReferenceResolver.resolve(
            parameters.getUsername(), new EntityReference(USER_SPACE, EntityType.SPACE));

        // Find the avatar attachment name or null if not defined or an error happened when locating it.
        // Raises an error if the user doesn't exist.
        String avatarAttachmentFileName = null;
        if (this.entityManager.hasEntity(new UniqueReference(userReference))) {

            DocumentReference xwikiUsersClassReference = new DocumentReference(
                userReference.getWikiReference().getName(), USER_SPACE, "XWikiUsers");

            // TODO: Find a way to be able to address an object by its xclass directly...
            //BaseObjectReference objectReference = new BaseObjectReference(xwikiUsersClassReference, 0, userReference);
            ObjectReference objectReference = new ObjectReference(
                this.compactWikiEntityReferenceSerializer.serialize(xwikiUsersClassReference), userReference);

            ObjectPropertyReference propertyReference = new ObjectPropertyReference("avatar", objectReference);

            ObjectPropertyEntity<String> avatarPropertyEntity =
                this.entityManager.getEntity(new UniqueReference(propertyReference));

            if (avatarPropertyEntity != null) {
                avatarAttachmentFileName = avatarPropertyEntity.getValue();
            }
        } else {
            throw new MacroExecutionException(String.format("User [%s] is not registered in this wiki",
                this.compactWikiEntityReferenceSerializer.serialize(userReference)));
        }

        ResourceReference imageReference;
        if (avatarAttachmentFileName == null) {
            imageReference = new ResourceReference(this.skinAccessBridge.getSkinFile("noavatar.png"), ResourceType.URL);
        } else {
            AttachmentReference attachmentReference = new AttachmentReference(avatarAttachmentFileName, userReference);
            imageReference =
                new ResourceReference(this.compactWikiEntityReferenceSerializer.serialize(attachmentReference),
                    ResourceType.ATTACHMENT);
        }
        ImageBlock imageBlock = new ImageBlock(imageReference, false);

        imageBlock.setParameter("alt", "Picture of " + userReference.getName());
        imageBlock.setParameter("title", userReference.getName());

        if (parameters.getWidth() != null) {
            imageBlock.setParameter("width", String.valueOf(parameters.getWidth()));
        }

        if (parameters.getHeight() != null) {
            imageBlock.setParameter("height", String.valueOf(parameters.getHeight()));
        }

        return Collections.singletonList((Block) imageBlock);
    }

    @Override
    public boolean supportsInlineMode()
    {
        return true;
    }
}
