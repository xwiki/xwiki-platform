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
package org.xwiki.rendering.internal.macro.velocity;

import java.io.StringWriter;

import org.apache.commons.lang.StringUtils;
import org.apache.velocity.VelocityContext;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.macro.descriptor.DefaultContentDescriptor;
import org.xwiki.rendering.macro.descriptor.DefaultMacroDescriptor;
import org.xwiki.rendering.macro.script.AbstractScriptMacro;
import org.xwiki.rendering.macro.velocity.VelocityMacroConfiguration;
import org.xwiki.rendering.macro.velocity.VelocityMacroParameters;
import org.xwiki.rendering.macro.velocity.filter.VelocityMacroFilter;
import org.xwiki.rendering.transformation.MacroTransformationContext;
import org.xwiki.velocity.VelocityManager;
import org.xwiki.velocity.XWikiVelocityException;

/**
 * @version $Id$
 * @since 1.5M2
 */
@Component("velocity")
public class VelocityMacro extends AbstractScriptMacro<VelocityMacroParameters>
{
    /**
     * The description of the macro.
     */
    private static final String DESCRIPTION = "Executes a Velocity script.";

    /**
     * The description of the macro content.
     */
    private static final String CONTENT_DESCRIPTION = "the velocity script to execute";

    /**
     * Used to get the Velocity Engine and Velocity Context to use to evaluate the passed Velocity script.
     */
    @Requirement
    private VelocityManager velocityManager;

    /**
     * The velocity macro configuration.
     */
    @Requirement
    private VelocityMacroConfiguration configuration;

    /**
     * Default constructor.
     */
    public VelocityMacro()
    {
        super(new DefaultMacroDescriptor(DESCRIPTION, new DefaultContentDescriptor(CONTENT_DESCRIPTION),
            VelocityMacroParameters.class));
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

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.rendering.macro.script.AbstractScriptMacro#evaluate(java.lang.Object, java.lang.String,
     *      org.xwiki.rendering.transformation.MacroTransformationContext)
     */
    @Override
    protected String evaluate(VelocityMacroParameters parameters, String content, MacroTransformationContext context)
        throws MacroExecutionException
    {
        String result = "";

        try {
            VelocityContext velocityContext = this.velocityManager.getVelocityContext();

            VelocityMacroFilter filter = getFilter(parameters);

            String cleanedContent = content;

            if (filter != null) {
                cleanedContent = filter.before(cleanedContent, velocityContext);
            }

            StringWriter writer = new StringWriter();
            this.velocityManager.getVelocityEngine()
                .evaluate(velocityContext, writer, "velocity macro", cleanedContent);
            result = writer.toString();

            if (filter != null) {
                result = filter.after(result, velocityContext);
            }
        } catch (XWikiVelocityException e) {
            throw new MacroExecutionException("Failed to evaluate Velocity Macro for content [" + content + "]", e);
        }

        return result;
    }

    /**
     * @param parameters the velocity macros parameters
     * @return the velocity content filter
     */
    private VelocityMacroFilter getFilter(VelocityMacroParameters parameters)
    {
        String filterName = parameters.getFilter();

        if (StringUtils.isEmpty(filterName)) {
            filterName = this.configuration.getFilter();

            if (StringUtils.isEmpty(filterName)) {
                filterName = null;
            }
        }

        VelocityMacroFilter filter = null;
        if (filterName != null) {
            try {
                filter = (VelocityMacroFilter) getComponentManager().lookup(VelocityMacroFilter.class, filterName);
            } catch (ComponentLookupException e) {
                getLogger().error("Can't find velocity maco cleaner", e);
            }
        }

        return filter;
    }
}
