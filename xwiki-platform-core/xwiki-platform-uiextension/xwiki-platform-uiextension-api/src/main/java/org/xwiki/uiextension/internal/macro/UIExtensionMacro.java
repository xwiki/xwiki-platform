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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.CompositeBlock;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.macro.AbstractMacro;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.transformation.MacroTransformationContext;
import org.xwiki.uiextension.UIExtension;

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
    @Named("context")
    private Provider<ComponentManager> contextComponentManagerProvider;

    /**
     * Default constructor.
     */
    public UIExtensionMacro()
    {
        super("UI Extensions", DESCRIPTION, UIExtensionsMacroParameters.class);

        // The ui extensions macro must execute first since if it runs with the current context it needs to bring
        // all the macros from the extension before the other macros are executed.
        setPriority(10);
        setDefaultCategory(DEFAULT_CATEGORY_DEVELOPMENT);
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
        UIExtension extension;
        try {
            extension = this.contextComponentManagerProvider.get().getInstance(UIExtension.class);
        } catch (ComponentLookupException e) {
            throw new MacroExecutionException(
                "Failed to lookup UIExtension component with hint [" + parameters.getId() + "]", e);
        }

        Block block = extension.execute(context.isInline());

        if (block instanceof XDOM || block instanceof CompositeBlock) {
            return block.getChildren();
        }

        return Collections.singletonList(block);
    }
}
