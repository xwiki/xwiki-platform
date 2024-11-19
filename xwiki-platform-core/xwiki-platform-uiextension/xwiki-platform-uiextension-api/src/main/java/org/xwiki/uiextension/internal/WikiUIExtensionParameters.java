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

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Map;
import java.util.Properties;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.velocity.VelocityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.wiki.WikiComponentException;
import org.xwiki.context.Execution;
import org.xwiki.logging.LoggerConfiguration;
import org.xwiki.model.EntityType;
import org.xwiki.model.ModelContext;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.security.authorization.AuthorExecutor;
import org.xwiki.security.authorization.DocumentAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.velocity.VelocityEngine;
import org.xwiki.velocity.VelocityManager;
import org.xwiki.velocity.XWikiVelocityContext;
import org.xwiki.velocity.XWikiVelocityException;

import com.xpn.xwiki.objects.BaseObject;

import static org.xwiki.uiextension.internal.WikiUIExtensionConstants.ID_PROPERTY;
import static org.xwiki.uiextension.internal.WikiUIExtensionConstants.PARAMETERS_PROPERTY;

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
     * @see #WikiUIExtensionParameters(String, String, ComponentManager)
     */
    private Properties parameters;

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
     * @see #WikiUIExtensionParameters(String, String, ComponentManager)
     */
    private VelocityManager velocityManager;

    /**
     * Model context.
     */
    private ModelContext modelContext;

    private LoggerConfiguration loggerConfiguration;

    /**
     * The execution context.
     */
    private Execution execution;

    private AuthorExecutor authorExecutor;

    private final DocumentReference documentReference;

    private final DocumentReference authorReference;

    private final DocumentAuthorizationManager authorizationManager;

    /**
     * Default constructor.
     *
     * @param baseObject the object from which the parameters shall be loaded
     * @param cm the XWiki component manager
     * @throws WikiComponentException if some required components can't be found in the Component Manager
     */
    public WikiUIExtensionParameters(BaseObject baseObject, ComponentManager cm)
        throws WikiComponentException
    {
        this.id = baseObject.getStringValue(ID_PROPERTY);
        this.parameters = parseParameters(baseObject.getStringValue(PARAMETERS_PROPERTY));

        this.documentReference = baseObject.getDocumentReference();
        this.authorReference = baseObject.getOwnerDocument().getAuthorReference();

        try {
            this.execution = cm.getInstance(Execution.class);
            this.velocityManager = cm.getInstance(VelocityManager.class);
            this.modelContext = cm.getInstance(ModelContext.class);
            this.loggerConfiguration = cm.getInstance(LoggerConfiguration.class);
            this.authorExecutor = cm.getInstance(AuthorExecutor.class);
            this.authorizationManager = cm.getInstance(DocumentAuthorizationManager.class);
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
    private Properties parseParameters(String rawParameters)
    {
        Properties properties = new Properties();
        try {
            properties.load(new StringReader(rawParameters));
        } catch (IOException e) {
            LOGGER.warn("Failed to parse UIX parameters [{}]. Cause [{}].", rawParameters,
                ExceptionUtils.getRootCauseMessage(e));
        }

        return properties;
    }

    /**
     * @return the parameters, after their values have been evaluated by the XWiki Velocity Engine.
     */
    public Map<String, String> get()
    {
        Map<String, String> result;

        // Even though the parameters are dynamic, we cache a rendered version of them in order to improve performance.
        // This cache has a short lifespan, it gets discarded for each new request, or if the database has been switched
        // during a request.
        int currentContextId = this.execution.getContext().hashCode();
        String currentWiki = modelContext.getCurrentEntityReference().extractReference(EntityType.WIKI).getName();
        if (currentContextId == this.previousContextId
                && currentWiki.equals(previousWiki) && this.evaluatedParameters != null)
        {
            result = this.evaluatedParameters;
        } else {
            result = this.parameters.stringPropertyNames().stream()
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toMap(Function.identity(), this.parameters::getProperty));

            if (!this.parameters.isEmpty()
                && this.authorizationManager.hasAccess(Right.SCRIPT, EntityType.DOCUMENT, this.authorReference,
                this.documentReference))
            {
                try {
                    this.authorExecutor.call(() -> {
                        VelocityEngine velocityEngine = this.velocityManager.getVelocityEngine();
                        VelocityContext velocityContext = this.velocityManager.getVelocityContext();

                        result.replaceAll((propertyKey, propertyValue) -> {
                            StringWriter writer = new StringWriter();
                            try {
                                String namespace = this.id + ':' + propertyKey;
                                velocityEngine.evaluate(
                                    new XWikiVelocityContext(velocityContext,
                                        this.loggerConfiguration.isDeprecatedLogEnabled()),
                                    writer, namespace, propertyValue);
                                return writer.toString();
                            } catch (XWikiVelocityException e) {
                                LOGGER.warn(String.format(
                                    "Failed to evaluate UI extension data value, key [%s], value [%s]. Reason: [%s]",
                                    propertyKey, propertyValue, e.getMessage()));
                            }

                            return propertyValue;
                        });

                        return null;
                    }, this.authorReference, this.documentReference);
                } catch (Exception ex) {
                    LOGGER.warn(String.format("Failed to get velocity engine. Reason: [%s]", ex.getMessage()));
                }
            }

            this.evaluatedParameters = result;
            this.previousContextId = currentContextId;
            this.previousWiki = currentWiki;
        }

        return result;
    }
}
