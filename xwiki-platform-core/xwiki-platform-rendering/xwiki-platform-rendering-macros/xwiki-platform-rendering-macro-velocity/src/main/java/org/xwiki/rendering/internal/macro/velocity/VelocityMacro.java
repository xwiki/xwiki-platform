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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.apache.velocity.VelocityContext;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.rendering.macro.MacroExecutionException;
import org.xwiki.rendering.macro.descriptor.DefaultContentDescriptor;
import org.xwiki.rendering.macro.script.AbstractScriptMacro;
import org.xwiki.rendering.macro.velocity.VelocityMacroConfiguration;
import org.xwiki.rendering.macro.velocity.VelocityMacroParameters;
import org.xwiki.rendering.macro.velocity.filter.VelocityMacroFilter;
import org.xwiki.rendering.transformation.MacroTransformationContext;
import org.xwiki.velocity.VelocityManager;
import org.xwiki.velocity.XWikiVelocityException;

/**
 * Executes <a href="http://velocity.apache.org/">Velocity</a> on the content of this macro and optionally parse the
 * resulting content with a wiki syntax parser.
 *
 * @version $Id$
 * @since 1.5M2
 */
@Component
@Named("velocity")
@Singleton
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
    @Inject
    private VelocityManager velocityManager;

    /**
     * The velocity macro configuration.
     */
    @Inject
    private VelocityMacroConfiguration configuration;

    /**
     * The logger to log.
     */
    @Inject
    private Logger logger;

    /**
     * Default constructor.
     */
    public VelocityMacro()
    {
        super("Velocity", DESCRIPTION, new DefaultContentDescriptor(CONTENT_DESCRIPTION),
            VelocityMacroParameters.class);
    }

    @Override
    public boolean supportsInlineMode()
    {
        return true;
    }

    @Override
    protected String evaluateString(VelocityMacroParameters parameters, String content,
        MacroTransformationContext context) throws MacroExecutionException
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

            // Use the Transformation id as the name passed to the Velocity Engine. This name is used internally
            // by Velocity as a cache index key for caching macros.
            String key = context.getTransformationContext().getId();
            if (key == null) {
                key = "unknown namespace";
            }

            this.velocityManager.getVelocityEngine().evaluate(velocityContext, writer, key, cleanedContent);
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
     * @since 2.0M1
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
                filter = getComponentManager().getInstance(VelocityMacroFilter.class, filterName);
            } catch (ComponentLookupException e) {
                this.logger.error("Can't find velocity maco cleaner", e);
            }
        }

        return filter;
    }
}
