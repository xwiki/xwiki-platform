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
 *
 */
package org.xwiki.rendering.internal.macro.useravatar;

import java.util.Collections;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.bridge.SkinAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.ImageBlock;
import org.xwiki.rendering.macro.AbstractMacro;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.macro.descriptor.DefaultMacroDescriptor;
import org.xwiki.rendering.macro.useravatar.UserAvatarMacroParameters;
import org.xwiki.rendering.transformation.MacroTransformationContext;
import org.xwiki.rendering.listener.DefaultAttachement;
import org.xwiki.rendering.listener.DocumentImage;
import org.xwiki.rendering.listener.Image;
import org.xwiki.rendering.listener.URLImage;

/**
 * Allows displaying the avatar for a specific user.
 * 
 * @version $Id: $
 * @since 1.8RC2
 */
@Component("useravatar")
public class UserAvatarMacro extends AbstractMacro<UserAvatarMacroParameters>
{
    /**
     * The description of the macro.
     */
    private static final String DESCRIPTION = "Allows displaying the avatar for a specific user.";

    /**
     * Used to get the user avatar picture from his profile.
     */
    @Requirement
    private DocumentAccessBridge documentAccessBridge;

    /**
     * Used to get the default avatar picture when the user doesn't exist.
     */
    @Requirement
    private SkinAccessBridge skinAccessBridge;

    /**
     * Create and initialize the descriptor of the macro.
     */
    public UserAvatarMacro()
    {
        super(new DefaultMacroDescriptor(DESCRIPTION, null, UserAvatarMacroParameters.class));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.macro.Macro#execute(java.lang.Object, java.lang.String,
     *      org.xwiki.rendering.transformation.MacroTransformationContext)
     */
    public List<Block> execute(UserAvatarMacroParameters parameters, String content, MacroTransformationContext context)
        throws MacroExecutionException
    {
        String fileName = null;
        String userName = parameters.getUsername();
        Block resultedBlock = null;

        if (documentAccessBridge.exists(userName)) {
            try {
                fileName = documentAccessBridge.getProperty(userName, "XWiki.XWikiUsers", "avatar");
            } catch (Exception e) {
                throw new MacroExecutionException("Failed to retrieve user avatar for user [" + userName + "]", e);
            }
        } else {
            throw new MacroExecutionException("User " + userName + " is not registered in this wiki");
        }
        Image image = null;

        image =
            StringUtils.isBlank(fileName) ? new URLImage(skinAccessBridge.getSkinFile("noavatar.png"))
                : new DocumentImage(new DefaultAttachement(userName, fileName));

        ImageBlock imageBlock = new ImageBlock(image, false);
        String shortName = userName.split("[.]")[1];
        imageBlock.setParameter("alt", "Picture of " + shortName);
        imageBlock.setParameter("title", shortName);

        if (parameters.getWidth() != null) {
            imageBlock.setParameter("width", parameters.getWidth());
        }

        if (parameters.getHeight() != null) {
            imageBlock.setParameter("height", parameters.getHeight());
        }

        resultedBlock = imageBlock;

        return Collections.singletonList(resultedBlock);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.macro.Macro#supportsInlineMode()
     */
    public boolean supportsInlineMode()
    {
        return true;
    }
}
