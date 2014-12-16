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
import org.slf4j.Logger;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.bridge.SkinAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.EntityReferenceValueProvider;
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
    private DocumentAccessBridge documentAccessBridge;

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
     * Logging framework.
     */
    @Inject
    private Logger logger;

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
        DocumentReference userReference =
            this.currentDocumentReferenceResolver.resolve(parameters.getUsername(), new EntityReference(USER_SPACE,
                EntityType.SPACE));

        // Find the avatar attachment name or null if not defined or an error happened when locating it
        String fileName = null;
        if (this.documentAccessBridge.exists(userReference)) {
            Object avatarProperty =
                this.documentAccessBridge.getProperty(userReference, new DocumentReference(userReference
                    .getWikiReference().getName(), USER_SPACE, "XWikiUsers"), "avatar");
            if (avatarProperty != null) {
                fileName = avatarProperty.toString();
            }
        } else {
            throw new MacroExecutionException("User ["
                + this.compactWikiEntityReferenceSerializer.serialize(userReference)
                + "] is not registered in this wiki");
        }

        // Initialize with the default avatar.
        ResourceReference imageReference =
            new ResourceReference(this.skinAccessBridge.getSkinFile("noavatar.png"), ResourceType.URL);

        // Try to use the configured avatar.
        if (!StringUtils.isBlank(fileName)) {
            AttachmentReference attachmentReference = new AttachmentReference(fileName, userReference);

            // Check if the configured avatar file actually exists.
            try {
                if (documentAccessBridge.getAttachmentVersion(attachmentReference) != null) {
                    // Use it.
                    imageReference =
                        new ResourceReference(this.compactWikiEntityReferenceSerializer.serialize(attachmentReference),
                            ResourceType.ATTACHMENT);
                }
            } catch (Exception e) {
                // Log and fallback on default.
                logger.warn("Failed to get the avatar for user [{}]. Using default.",
                    this.compactWikiEntityReferenceSerializer.serialize(userReference));
            }
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
