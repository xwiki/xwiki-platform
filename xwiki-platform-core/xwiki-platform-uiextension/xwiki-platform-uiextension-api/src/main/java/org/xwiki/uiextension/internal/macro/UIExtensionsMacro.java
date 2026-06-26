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

import java.util.ArrayList;
import java.util.List;
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
import org.xwiki.uiextension.macro.UIExtensionsMacroParameters;

/**
 * Insert UI extensions.
 * 
 * @version $Id$
 * @since 14.0RC1
 */
@Component
@Named("uiextensions")
@Singleton
// TODO: add support for UI extensions filters
public class UIExtensionsMacro extends AbstractMacro<UIExtensionsMacroParameters>
{
    /**
     * The description of the macro.
     */
    private static final String DESCRIPTION = "Insert UI extensions.";

    @Inject
    private UIExtensionManager uiExtensionManager;

    /**
     * Default constructor.
     */
    public UIExtensionsMacro()
    {
        super("UI Extensions", DESCRIPTION, UIExtensionsMacroParameters.class);

        setDefaultCategories(Set.of(DEFAULT_CATEGORY_DEVELOPMENT));
    }

    @Override
    public boolean supportsInlineMode()
    {
        return true;
    }

    @Override
    public List<Block> execute(UIExtensionsMacroParameters parameters, String content,
        MacroTransformationContext context) throws MacroExecutionException
    {
        List<UIExtension> extensions = this.uiExtensionManager.get(parameters.getExtensionPoint());

        List<Block> results = new ArrayList<>();

        for (UIExtension extension : extensions) {
            Block block = extension.execute(context.isInline());

            if (block instanceof XDOM || block instanceof CompositeBlock) {
                results.addAll(block.getChildren());
            } else {
                results.add(block);
            }
        }

        return results;
    }
}
