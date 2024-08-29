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
package org.xwiki.uiextension.internal.macro;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.CompositeBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.macro.AbstractMacro;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.transformation.MacroTransformationContext;
import org.xwiki.uiextension.UIExtension;
import org.xwiki.uiextension.UIExtensionManager;

/**
 * Insert UI extensions.
 * 
 * @version $Id$
 * @since 14.0RC1
 */
@Component
@Named("uiextension")
@Singleton
public class UIExtensionMacro extends AbstractMacro<UIExtensionMacroParameters>
{
    /**
     * The description of the macro.
     */
    private static final String DESCRIPTION = "Insert a UI extension.";

    @Inject
    private UIExtensionManager uiextensions;

    /**
     * Default constructor.
     */
    public UIExtensionMacro()
    {
        super("UI Extensions", DESCRIPTION, UIExtensionMacroParameters.class);

        setDefaultCategories(Set.of(DEFAULT_CATEGORY_DEVELOPMENT));
    }

    @Override
    public boolean supportsInlineMode()
    {
        return true;
    }

    @Override
    public List<Block> execute(UIExtensionMacroParameters parameters, String content,
        MacroTransformationContext context) throws MacroExecutionException
    {
        Optional<UIExtension> extension = this.uiextensions.getUIExtension(parameters.getId());

        if (extension.isEmpty()) {
            throw new MacroExecutionException("Failed to find an extension id id [" + parameters.getId() + "]");
        }

        Block block = extension.get().execute(context.isInline());

        if (block instanceof XDOM || block instanceof CompositeBlock) {
            return block.getChildren();
        }

        return Collections.singletonList(block);
    }
}
