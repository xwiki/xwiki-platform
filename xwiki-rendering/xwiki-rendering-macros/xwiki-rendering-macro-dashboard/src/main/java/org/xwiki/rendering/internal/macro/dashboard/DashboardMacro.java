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

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.GroupBlock;
import org.xwiki.rendering.block.MacroBlock;
import org.xwiki.rendering.macro.AbstractMacro;
import org.xwiki.rendering.macro.Macro;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.macro.container.ContainerMacroParameters;
import org.xwiki.rendering.macro.dashboard.DashboardMacroParameters;
import org.xwiki.rendering.transformation.MacroTransformationContext;
import org.xwiki.skinx.SkinExtension;

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
     * CSS file skin extension, to include the dashboard css.
     */
    @Requirement("ssfx")
    private SkinExtension ssfx;

    /**
     * The component manager, to inject to the GadgetBoxMacro.
     */
    @Requirement
    private ComponentManager componentManager;

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
        Map<String, Object> fxParams = new HashMap<String, Object>();
        fxParams.put("forceSkinAction", true);

        ssfx.use("uicomponents/dashboard/dashboard.css", fxParams);

        // do the layouting with the container macro
        ContainerMacroParameters containerParameters = new ContainerMacroParameters();
        containerParameters.setJustify(true);
        containerParameters.setLayoutStyle(parameters.getLayout());
        List<Block> layoutedResult = containerMacro.execute(containerParameters, content, context);

        // get all the macro blocks inside and wrap them in a gadget macro, which will generate a title for it and a box
        // around it to be able to have decoration and drag it
        // TODO: expand container macros and descend into them. Container macro is a special case, used for layouting
        // dashboards and not as a gadget
        for (Block layoutedTopLevelBlock : layoutedResult) {
            for (MacroBlock macroBlock : layoutedTopLevelBlock.getChildrenByType(MacroBlock.class, true)) {
                // clone the macro block here, because otherwise we will not have it in the parent anymore
                GadgetMacro gadgetBoxMacro = new GadgetMacro((MacroBlock) macroBlock.clone(), this.componentManager);
                // execute with null params and null content since they will be generated anyway, from the set macro
                // block
                List<Block> boxMacroBlocks = gadgetBoxMacro.execute(null, null, context);
                // replace the macro block in its parent with the blocks returned by the box macro
                macroBlock.getParent().replaceChild(boxMacroBlocks, macroBlock);
            }
        }

        // put everything in a nice toplevel group for this dashboard, to be able to add classes to it
        GroupBlock topLevel = new GroupBlock();
        topLevel.addChildren(layoutedResult);
        // add the style attribute of the dashboard macro as a class to the toplevel container
        topLevel.setParameter("class", MACRO_NAME
            + (StringUtils.isEmpty(parameters.getStyle()) ? "" : " " + parameters.getStyle()));

        return Collections.<Block> singletonList(topLevel);
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
