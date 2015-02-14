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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.GroupBlock;
import org.xwiki.rendering.internal.macro.script.NestedScriptMacroEnabled;
import org.xwiki.rendering.macro.AbstractMacro;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.macro.dashboard.DashboardMacroParameters;
import org.xwiki.rendering.macro.dashboard.DashboardRenderer;
import org.xwiki.rendering.macro.dashboard.Gadget;
import org.xwiki.rendering.macro.dashboard.GadgetRenderer;
import org.xwiki.rendering.macro.dashboard.GadgetSource;
import org.xwiki.rendering.transformation.MacroTransformationContext;
import org.xwiki.skinx.SkinExtension;

/**
 * The dashboard macro, to display other macros as gadgets in a dashboard, using a container to include its contents.
 * 
 * @version $Id$
 * @since 2.5M2
 */
@Component
@Named(DashboardMacro.MACRO_NAME)
@Singleton
public class DashboardMacro extends AbstractMacro<DashboardMacroParameters> implements NestedScriptMacroEnabled
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
     * The identifier of the metadata block for this dashboard (class parameter of the generated XDOM container that
     * holds the rest of the metadata).
     */
    public static final String METADATA = "metadata";

    /**
     * The identifier of the edit url metadata.
     */
    public static final String EDIT_URL = "editurl";

    /**
     * The identifier of the add url metadata.
     */
    public static final String ADD_URL = "addurl";

    /**
     * The identifier of the remove url metadata.
     */
    public static final String REMOVE_URL = "removeurl";

    /**
     * The identifier of the source page metadata.
     */
    public static final String SOURCE_PAGE = "sourcepage";

    /**
     * The identifier of the source space metadata.
     */
    public static final String SOURCE_SPACE = "sourcespace";

    /**
     * The identifier of the source wiki metadata.
     */
    public static final String SOURCE_WIKI = "sourcewiki";

    /**
     * The identifier of the source url metadata.
     */
    public static final String SOURCE_URL = "sourceurl";

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
    @Inject
    @Named("ssfx")
    private SkinExtension ssfx;

    /**
     * JS file skin extension, to include the dashboard.js.
     */
    @Inject
    @Named("jsfx")
    private SkinExtension jsfx;

    /**
     * The component manager, to resolve the dashboard renderer by layout hint.
     */
    @Inject
    private ComponentManager componentManager;

    /**
     * The gadget reader providing the list of {@link Gadget}s to render on this dashboard.
     */
    @Inject
    private GadgetSource gadgetSource;

    /**
     * The logger to log.
     */
    @Inject
    private Logger logger;

    /**
     * Instantiates the dashboard macro, setting the name, description and parameters type.
     */
    public DashboardMacro()
    {
        super("Dashboard", DESCRIPTION, DashboardMacroParameters.class);
    }

    @Override
    public List<Block> execute(DashboardMacroParameters parameters, String content, MacroTransformationContext context)
        throws MacroExecutionException
    {
        // get the gadgets from the objects
        List<Gadget> gadgets;
        try {
            gadgets = this.gadgetSource.getGadgets(parameters.getSource(), context);
        } catch (Exception e) {
            String message = "Could not get the gadgets.";
            // log and throw further
            this.logger.error(message, e);
            throw new MacroExecutionException(message, e);
        }

        boolean isInEditMode = this.gadgetSource.isEditing();

        DashboardRenderer renderer =
            getDashboardRenderer(StringUtils.isEmpty(parameters.getLayout()) ? "columns" : parameters.getLayout());
        if (renderer == null) {
            String message = "Could not find dashboard renderer " + parameters.getLayout();
            // log and throw further
            this.logger.error(message);
            throw new MacroExecutionException(message);
        }

        GadgetRenderer gadgetRenderer = getGadgetRenderer(isInEditMode);

        // else, layout
        List<Block> layoutedResult;
        try {
            layoutedResult = renderer.renderGadgets(gadgets, gadgetRenderer, context);
        } catch (Exception e) {
            String message = "Could not render the gadgets for layout " + parameters.getLayout();
            // log and throw further
            this.logger.error(message, e);
            throw new MacroExecutionException(message, e);
        }

        // include the css and js for this macro. here so that it's included after any dependencies have included their
        // css, so that it cascades properly
        this.includeResources(isInEditMode);

        // put everything in a nice toplevel group for this dashboard, to be able to add classes to it
        GroupBlock topLevel = new GroupBlock();
        // just under the toplevel, above the content, slip in the metadata, for the client code, only if we're in edit
        // mode
        if (isInEditMode) {
            topLevel.addChildren(this.gadgetSource.getDashboardSourceMetadata(parameters.getSource(), context));
        }
        topLevel.addChildren(layoutedResult);
        // add the style attribute of the dashboard macro as a class to the toplevel container
        topLevel.setParameter("class",
            MACRO_NAME + (StringUtils.isEmpty(parameters.getStyle()) ? "" : " " + parameters.getStyle()));

        return Collections.<Block> singletonList(topLevel);
    }

    /**
     * Includes the js and css resources for the dashboard macro.
     * 
     * @param editMode whether the dashboard is in edit mode or not (js resources need to be loaded only in edit mode)
     */
    protected void includeResources(boolean editMode)
    {
        Map<String, Object> fxParamsForceSkinAction = new HashMap<String, Object>();
        fxParamsForceSkinAction.put("forceSkinAction", true);
        this.ssfx.use("uicomponents/dashboard/dashboard.css", fxParamsForceSkinAction);
        // include the js resources, for editing, in edit mode only
        if (editMode) {
            // include the effects.js and dragdrop.js that are needed by the dashboard js
            this.jsfx.use("js/scriptaculous/effects.js");
            this.jsfx.use("js/scriptaculous/dragdrop.js");
            Map<String, Object> fxParamsNonDeferredForceSkinAction = new HashMap<String, Object>();
            fxParamsNonDeferredForceSkinAction.put("defer", false);
            fxParamsNonDeferredForceSkinAction.putAll(fxParamsForceSkinAction);
            this.jsfx.use("js/xwiki/wysiwyg/xwe/XWikiWysiwyg.js", fxParamsNonDeferredForceSkinAction);
            this.jsfx.use("uicomponents/dashboard/dashboard.js", fxParamsForceSkinAction);
        }
    }

    /**
     * @param layout the layout style parameter of this dashboard, to find the renderer for
     * @return the dashboard renderer, according to the style parameter of this dashboard macro
     */
    protected DashboardRenderer getDashboardRenderer(String layout)
    {
        try {
            return this.componentManager.getInstance(DashboardRenderer.class, layout);
        } catch (ComponentLookupException e) {
            this.logger.warn("Could not find the Dashboard renderer for layout \"" + layout + "\"");
            return null;
        }
    }

    /**
     * @param isEditing whether this dashboard is in edit mode or in view mode
     * @return the gadgets renderer used by this dashboard
     * @throws MacroExecutionException if the gadget renderer cannot be found
     */
    protected GadgetRenderer getGadgetRenderer(boolean isEditing) throws MacroExecutionException
    {
        String hint = "default";
        if (isEditing) {
            hint = "edit";
        }
        try {
            return this.componentManager.getInstance(GadgetRenderer.class, hint);
        } catch (ComponentLookupException e) {
            throw new MacroExecutionException(String.format("Could not find the Gadgets renderer for hint [%s].",
                hint), e);
        }
    }

    @Override
    public boolean supportsInlineMode()
    {
        return false;
    }
}
