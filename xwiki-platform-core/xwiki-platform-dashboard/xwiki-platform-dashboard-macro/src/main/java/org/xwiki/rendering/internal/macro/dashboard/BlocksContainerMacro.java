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
package org.xwiki.rendering.internal.macro.dashboard;

import java.util.ArrayList;
import java.util.List;

import org.xwiki.component.manager.ComponentManager;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.macro.container.AbstractContainerMacro;
import org.xwiki.rendering.macro.container.ContainerMacroParameters;
import org.xwiki.rendering.transformation.MacroTransformationContext;

/**
 * Version of the container macro that takes a list of blocks as content and renders them according to the parameters in
 * {@link ContainerMacroParameters}. Also, it can be instantiated and used as a POJO, not only injected by the component
 * manager, as long as an instance of the component manager is being passed with
 * {@link #setComponentManager(ComponentManager)}. <br>
 * TODO: this macro should be defined in the container macro package, since it's generic enough. Unfortunately, this
 * macro cannot be made public, since ftm there is no mechanism to hide macros from user (in the wysiwyg list, for
 * example), which forces us to keep it here with package visibility, so it can be used in the gadgets implementation.
 * 
 * @version $Id$
 * @since 3.0M3
 */
class BlocksContainerMacro extends AbstractContainerMacro<ContainerMacroParameters>
{
    /**
     * The component manager, injected to this macro by the caller, since this macro cannot be exposed to the cm because
     * we don't want it available in the macros list. <br>
     * FIXME: when we will be able to hide macros from the user, this should be implemented properly as a component.
     */
    private ComponentManager componentManager;

    /**
     * The macro block of the gadget macro inside the dashboard, will be used as the content of this box.
     */
    private List<Block> content = new ArrayList<Block>();

    /**
     * Default constructor, building a blocks macro with the default name and description.
     */
    BlocksContainerMacro()
    {
        super("Container", "Lays out the blocks passed as content according to the passed parameters.",
            null, ContainerMacroParameters.class);
    }

    @Override
    protected List<Block> getContent(ContainerMacroParameters parameters, String content,
        MacroTransformationContext context) throws MacroExecutionException
    {
        return this.content;
    }

    /**
     * @return the componentManager used by this macro to find components.
     */
    @Override
    public ComponentManager getComponentManager()
    {
        return componentManager;
    }

    /**
     * Sets the component manager of this container macro.
     * 
     * @param componentManager the {@link ComponentManager} to set
     */
    public void setComponentManager(ComponentManager componentManager)
    {
        this.componentManager = componentManager;
    }

    /**
     * @return the content of this container
     */
    public List<Block> getContent()
    {
        return content;
    }

    /**
     * Sets the content of this container, as a list of blocks.
     * 
     * @param content the content to set to this container
     */
    public void setContent(List<Block> content)
    {
        this.content = content;
    }
}
