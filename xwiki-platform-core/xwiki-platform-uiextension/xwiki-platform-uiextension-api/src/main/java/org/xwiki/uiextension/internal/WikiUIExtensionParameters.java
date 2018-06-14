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
package org.xwiki.uiextension.internal;

import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;

import org.apache.velocity.VelocityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.wiki.WikiComponentException;
import org.xwiki.context.Execution;
import org.xwiki.model.EntityType;
import org.xwiki.model.ModelContext;
import org.xwiki.velocity.VelocityEngine;
import org.xwiki.velocity.VelocityManager;
import org.xwiki.velocity.XWikiVelocityContext;
import org.xwiki.velocity.XWikiVelocityException;

/**
 * Wiki UI Extension parameter manager.
 *
 * @version $Id$
 * @since 5.0M1
 */
public class WikiUIExtensionParameters
{
    /**
     * The logger to log.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(WikiUIExtensionParameters.class);

    private String id;

    /**
     * @see #WikiUIExtensionParameters(String, org.xwiki.component.manager.ComponentManager)
     */
    private Map<String, String> parameters;

    /**
     * Parameters are evaluated through the velocity engine, to improve performance this variable is used as a cache,
     * it's emptied for each new request (because of the dynamic vars like: language, user, etc), or if the context
     * database has changed.
     */
    private Map<String, String> evaluatedParameters;

    /**
     * HashCode of the previous execution context during the last parameters evaluation.
     */
    private int previousContextId;

    /**
     * Context database during the last parameters evaluation.
     */
    private String previousWiki;

    /**
     * @see #WikiUIExtensionParameters(String, org.xwiki.component.manager.ComponentManager)
     */
    private VelocityManager velocityManager;

    /**
     * Model context.
     */
    private ModelContext modelContext;

    /**
     * The execution context.
     */
    private Execution execution;

    /**
     * Default constructor.
     *
     * @param id the unique identifier of this set of parameters, mostly used to isolate parameters value execution
     * @param rawParameters raw parameters, their values can contain velocity directives
     * @param cm the XWiki component manager
     * @throws WikiComponentException if some required components can't be found in the Component Manager
     */
    public WikiUIExtensionParameters(String id, String rawParameters, ComponentManager cm)
        throws WikiComponentException
    {
        this.id = id;
        this.parameters = parseParameters(rawParameters);

        try {
            this.execution = cm.getInstance(Execution.class);
            this.velocityManager = cm.getInstance(VelocityManager.class);
            this.modelContext = cm.getInstance(ModelContext.class);
        } catch (ComponentLookupException e) {
            throw new WikiComponentException(
                "Failed to get an instance for a component role required by Wiki Components.", e);
        }
    }

    /**
     * Parse the parameters provided by the extension.
     * The parameters are provided in a LargeString property of the extension object. In the future it would be better
     * to have a Map<String, String> XClass property.
     *
     * @param rawParameters the string to parse
     * @return a map of parameters
     */
    private Map<String, String> parseParameters(String rawParameters)
    {
        Map<String, String> result = new HashMap<String, String>();
        for (String line : rawParameters.split("[\\r\\n]+")) {
            String[] pair = line.split("=", 2);
            if (pair.length == 2 && !"".equals(pair[0]) && !"".equals(pair[1])) {
                result.put(pair[0], pair[1]);
            }
        }

        return result;
    }

    /**
     * @return the parameters, after their values have been evaluated by the XWiki Velocity Engine.
     */
    public Map<String, String> get()
    {
        boolean isCacheValid = false;

        // Even though the parameters are dynamic, we cache a rendered version of them in order to improve performance.
        // This cache has a short lifespan, it gets discarded for each new request, or if the database has been switched
        // during a request.
        int currentContextId = this.execution.getContext().hashCode();
        String currentWiki = modelContext.getCurrentEntityReference().extractReference(EntityType.WIKI).getName();
        if (currentContextId == this.previousContextId
            && currentWiki.equals(previousWiki) && this.evaluatedParameters != null) {
            isCacheValid = true;
        }

        if (!isCacheValid) {
            this.evaluatedParameters = new HashMap<String, String>();

            if (this.parameters.size() > 0) {
                try {
                    VelocityEngine velocityEngine = this.velocityManager.getVelocityEngine();
                    VelocityContext velocityContext = this.velocityManager.getVelocityContext();

                    for (Map.Entry<String, String> entry : this.parameters.entrySet()) {
                        StringWriter writer = new StringWriter();
                        try {
                            String namespace = this.id + ':' + entry.getKey();
                            velocityEngine.evaluate(new XWikiVelocityContext(velocityContext), writer, namespace,
                                entry.getValue());
                            this.evaluatedParameters.put(entry.getKey(), writer.toString());
                        } catch (XWikiVelocityException e) {
                            LOGGER.warn(String.format(
                                "Failed to evaluate UI extension data value, key [%s], value [%s]. Reason: [%s]",
                                entry.getKey(), entry.getValue(), e.getMessage()));
                        }
                    }
                } catch (XWikiVelocityException ex) {
                    LOGGER.warn(String.format("Failed to get velocity engine. Reason: [%s]", ex.getMessage()));
                }
                this.previousContextId = currentContextId;
                this.previousWiki = currentWiki;
            }
        }

        return this.evaluatedParameters;
    }
}
