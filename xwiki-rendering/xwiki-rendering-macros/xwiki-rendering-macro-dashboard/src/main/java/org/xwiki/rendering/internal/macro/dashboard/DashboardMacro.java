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

import java.util.List;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.macro.AbstractMacro;
import org.xwiki.rendering.macro.Macro;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.macro.container.ContainerMacroParameters;
import org.xwiki.rendering.macro.dashboard.DashboardMacroParameters;
import org.xwiki.rendering.transformation.MacroTransformationContext;

/**
 * The dashboard macro, to display other macros as gadgets in a dashboard, using a container to include its contents.
 * 
 * @version $Id$
 * @since 2.5M2
 */
@Component(DashboardMacro.MACRO_NAME)
public class DashboardMacro extends AbstractMacro<DashboardMacroParameters>
{
    /**
     * The name of this macro.
     */
    public static final String MACRO_NAME = "dashboard";

    /**
     * The description of this macro.
     */
    private static final String DESCRIPTION = "A macro to define a dashboard.";

    /**
     * The container macro, to delegate layouting the gadgets.
     */
    @Requirement("container")
    private Macro<ContainerMacroParameters> containerMacro;

    /**
     * Instantiates the dashboard macro, setting the name, description and parameters type.
     */
    public DashboardMacro()
    {
        super("Dashboard", DESCRIPTION, DashboardMacroParameters.class);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.macro.Macro#execute(java.lang.Object, java.lang.String,
     *      org.xwiki.rendering.transformation.MacroTransformationContext)
     */
    public List<Block> execute(DashboardMacroParameters parameters, String content, MacroTransformationContext context)
        throws MacroExecutionException
    {
        //TODO: include the dashboard javascript

        // just delegate to the container macro
        ContainerMacroParameters containerParameters = new ContainerMacroParameters();
        containerParameters.setJustify(true);
        containerParameters.setLayoutStyle(parameters.getLayout());

        return containerMacro.execute(containerParameters, content, context);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.macro.Macro#supportsInlineMode()
     */
    public boolean supportsInlineMode()
    {
        return false;
    }
}
