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
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.GroupBlock;
import org.xwiki.rendering.macro.AbstractMacro;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.macro.dashboard.DashboardMacroParameters;
import org.xwiki.rendering.macro.dashboard.DashboardRenderer;
import org.xwiki.rendering.macro.dashboard.Gadget;
import org.xwiki.rendering.macro.dashboard.GadgetReader;
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
     * The marker to set as class parameter for the gadget containers in this dashboard, i.e. the elements that can
     * contain gadgets.
     */
    public static final String GADGET_CONTAINER = "gadget-container";

    /**
     * The prefix of the id of the gadget containers in this dashboard, i.e. the elements that can contain gadgets. To
     * be completed by the individual renderers with the container ids.
     */
    public static final String GADGET_CONTAINER_PREFIX = "gadgetcontainer_";

    /**
     * The name of this macro.
     */
    public static final String MACRO_NAME = "dashboard";

    /**
     * The description of this macro.
     */
    private static final String DESCRIPTION = "A macro to define a dashboard.";

    /**
     * CSS file skin extension, to include the dashboard css.
     */
    @Requirement("ssfx")
    private SkinExtension ssfx;

    /**
     * JS file skin extension, to include the dashboard.js.
     */
    @Requirement("jsfx")
    private SkinExtension jsfx;

    /**
     * The component manager, to resolve the dashboard renderer by layout hint.
     */
    @Requirement
    private ComponentManager componentManager;

    /**
     * The gadget reader providing the list of {@link Gadget}s to render on this dashboard.
     */
    @Requirement
    private GadgetReader gadgetReader;

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
        // get the gadgets from the objects
        List<Gadget> gadgets;
        try {
            gadgets = gadgetReader.getGadgets(parameters.getSource(), context);
        } catch (Exception e) {
            String message = "Could not get the gadgets.";
            // log and throw further
            getLogger().error(message, e);
            throw new MacroExecutionException(message, e);
        }
        DashboardRenderer renderer =
            getDashboardRenderer(StringUtils.isEmpty(parameters.getLayout()) ? "columns" : parameters.getLayout());
        if (renderer == null) {
            String message = "Could not find dashboard renderer " + parameters.getLayout();
            // log and throw further
            getLogger().error(message);
            throw new MacroExecutionException(message);
        }
        // else, layout
        List<Block> layoutedResult;
        try {
            layoutedResult = renderer.renderGadgets(gadgets, context);
        } catch (Exception e) {
            String message = "Could not render the gadgets for layout " + parameters.getLayout();
            // log and throw further
            getLogger().error(message, e);
            throw new MacroExecutionException(message, e);
        }

        // include the css and js for this macro. here so that it's included after any dependencies have included their
        // css, so that it cascades properly
        Map<String, Object> fxParamsForceSkinAction = new HashMap<String, Object>();
        fxParamsForceSkinAction.put("forceSkinAction", true);
        ssfx.use("uicomponents/dashboard/dashboard.css", fxParamsForceSkinAction);
        // include the effects.js and dragdrop.js that are needed by the dashboard js
        jsfx.use("js/scriptaculous/effects.js");
        jsfx.use("js/scriptaculous/dragdrop.js");
        // this is only needed in inline mode, but it might be needed in view as well, in the future
        // FIXME: add a current action verification here, and only include in inline mode. This means that
        // dashboardmacro would depend on XWikiContext :(
        jsfx.use("uicomponents/dashboard/dashboard.js", fxParamsForceSkinAction);

        // put everything in a nice toplevel group for this dashboard, to be able to add classes to it
        GroupBlock topLevel = new GroupBlock();
        topLevel.addChildren(layoutedResult);
        // add the style attribute of the dashboard macro as a class to the toplevel container
        topLevel.setParameter("class", MACRO_NAME
            + (StringUtils.isEmpty(parameters.getStyle()) ? "" : " " + parameters.getStyle()));

        return Collections.<Block> singletonList(topLevel);
    }

    /**
     * @param layout the layout style parameter of this dashboard, to find the renderer for
     * @return the dashboard renderer, according to the style parameter of this dashboard macro
     */
    protected DashboardRenderer getDashboardRenderer(String layout)
    {
        try {
            return componentManager.lookup(DashboardRenderer.class, layout);
        } catch (ComponentLookupException e) {
            getLogger().warn("Could not find the Dashboard renderer for layout \"" + layout + "\"");
            return null;
        }
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
