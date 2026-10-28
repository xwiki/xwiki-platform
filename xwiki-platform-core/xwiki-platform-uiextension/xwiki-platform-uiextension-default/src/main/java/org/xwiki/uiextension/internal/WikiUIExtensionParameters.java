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
import java.lang.ref.WeakReference;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.wiki.WikiComponentException;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.logging.LoggerConfiguration;
import org.xwiki.model.EntityType;
import org.xwiki.model.ModelContext;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.security.authorization.AuthorExecutor;
import org.xwiki.security.authorization.DocumentAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.uiextension.internal.parameters.UIXParameterValue;
import org.xwiki.uiextension.internal.parameters.UIXParameterValueFactory;
import org.xwiki.uiextension.internal.parameters.VelocityParameterSession;
import org.xwiki.velocity.VelocityManager;

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

    /**
     * The parameter values, prepared once so that values without Velocity are never evaluated and that values with
     * Velocity are compiled only once.
     */
    private final Map<String, UIXParameterValue> values;

    /**
     * The not evaluated parameter values, returned when no value contains any script or when the author of the UI
     * extension isn't allowed to execute scripts.
     */
    private final Map<String, String> rawValues;

    private final boolean allStatic;

    private final boolean anyVelocity;

    /**
     * Parameters are evaluated through the velocity engine, to improve performance this variable is used as a cache,
     * it's emptied for each new request (because of the dynamic vars like: language, user, etc), or if the context
     * database has changed.
     * <p>
     * All the cache data is held by a single immutable object so that concurrent requests can't mix the evaluated
     * parameters of one request with the context of another one. The execution context is only weakly referenced to
     * avoid keeping it in memory after the request. This object is only ever replaced as a whole, never modified,
     * so volatile is exactly what is needed to publish it safely.
     */
    @SuppressWarnings("java:S3077")
    private volatile EvaluatedParameters evaluatedParameters;

    private final VelocityManager velocityManager;

    private final ModelContext modelContext;

    private final LoggerConfiguration loggerConfiguration;

    private final Execution execution;

    private final AuthorExecutor authorExecutor;

    private final DocumentReference documentReference;

    private final DocumentReference authorReference;

    private final DocumentAuthorizationManager authorizationManager;

    private record EvaluatedParameters(WeakReference<ExecutionContext> executionContext, String wiki,
        Map<String, String> values)
    {
        boolean isValid(ExecutionContext currentExecutionContext, String currentWiki)
        {
            return this.executionContext.get() == currentExecutionContext && Objects.equals(this.wiki, currentWiki);
        }
    }

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
        this.documentReference = baseObject.getDocumentReference();
        this.authorReference = baseObject.getOwnerDocument().getAuthorReference();

        UIXParameterValueFactory valueFactory;
        try {
            this.execution = cm.getInstance(Execution.class);
            this.velocityManager = cm.getInstance(VelocityManager.class);
            this.modelContext = cm.getInstance(ModelContext.class);
            this.loggerConfiguration = cm.getInstance(LoggerConfiguration.class);
            this.authorExecutor = cm.getInstance(AuthorExecutor.class);
            this.authorizationManager = cm.getInstance(DocumentAuthorizationManager.class);
            valueFactory = cm.getInstance(UIXParameterValueFactory.class);
        } catch (ComponentLookupException e) {
            throw new WikiComponentException(
                "Failed to get an instance for a component role required by Wiki Components.", e);
        }

        String id = baseObject.getStringValue(ID_PROPERTY);
        Properties parameters = parseParameters(baseObject.getStringValue(PARAMETERS_PROPERTY));

        Map<String, UIXParameterValue> parameterValues = new HashMap<>();
        Map<String, String> parameterRawValues = new HashMap<>();
        for (String key : parameters.stringPropertyNames()) {
            if (StringUtils.isNotBlank(key)) {
                String rawValue = parameters.getProperty(key);
                parameterValues.put(key, valueFactory.create(id + ':' + key, rawValue));
                parameterRawValues.put(key, rawValue);
            }
        }

        this.values = Collections.unmodifiableMap(parameterValues);
        this.rawValues = Collections.unmodifiableMap(parameterRawValues);
        this.allStatic = this.values.values().stream().allMatch(UIXParameterValue::isStatic);
        this.anyVelocity = this.values.values().stream().anyMatch(UIXParameterValue::requiresVelocity);
    }

    /**
     * Parse the parameters provided by the extension.
     * The parameters are provided in a LargeString property of the extension object. In the future it would be better
     * to have a Map&lt;String, String&gt; XClass property.
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
     * @return the parameters, after their values have been evaluated by the XWiki Velocity Engine
     */
    public Map<String, String> get()
    {
        // Values without any script are the same for everybody, there is nothing to evaluate or to cache.
        if (this.allStatic) {
            return this.rawValues;
        }

        // Even though the parameters are dynamic, we cache a rendered version of them in order to improve performance.
        // This cache has a short lifespan, it gets discarded for each new request, or if the database has been switched
        // during a request.
        ExecutionContext currentExecutionContext = this.execution.getContext();
        String currentWiki = getCurrentWiki();
        EvaluatedParameters cached = this.evaluatedParameters;
        if (cached != null && cached.isValid(currentExecutionContext, currentWiki)) {
            return cached.values();
        }

        Map<String, String> result;
        if (this.authorizationManager.hasAccess(Right.SCRIPT, EntityType.DOCUMENT, this.authorReference,
            this.documentReference))
        {
            result = evaluate();
        } else {
            result = this.rawValues;
        }

        this.evaluatedParameters =
            new EvaluatedParameters(new WeakReference<>(currentExecutionContext), currentWiki, result);

        return result;
    }

    /**
     * @return the name of the current wiki, or {@code null} when there is none (e.g. no XWiki context is
     *     initialized for the current thread), in which case the evaluated parameters are cached for "no wiki"
     */
    private String getCurrentWiki()
    {
        EntityReference currentReference = this.modelContext.getCurrentEntityReference();
        EntityReference wikiReference =
            currentReference != null ? currentReference.extractReference(EntityType.WIKI) : null;

        return wikiReference != null ? wikiReference.getName() : null;
    }

    private Map<String, String> evaluate()
    {
        if (!this.anyVelocity) {
            // Shortcut values don't depend on the author of the UI extension, so they don't need the author executor.
            return evaluate(null);
        }

        try {
            Map<String, String> result = this.authorExecutor.call(() -> evaluate(new VelocityParameterSession(
                this.velocityManager.getVelocityEngine(), this.velocityManager.getVelocityContext(),
                this.loggerConfiguration.isDeprecatedLogEnabled())), this.authorReference, this.documentReference);

            return Objects.requireNonNullElse(result, this.rawValues);
        } catch (Exception e) {
            LOGGER.warn("Failed to get velocity engine. Reason: [{}]", ExceptionUtils.getRootCauseMessage(e));

            return this.rawValues;
        }
    }

    private Map<String, String> evaluate(VelocityParameterSession session)
    {
        Map<String, String> result = new HashMap<>();

        this.values.forEach((key, value) -> {
            String evaluatedValue;
            try {
                evaluatedValue = value.evaluate(session);
            } catch (Exception e) {
                LOGGER.warn("Failed to evaluate UI extension data value, key [{}], value [{}]. Reason: [{}]", key,
                    value.raw(), ExceptionUtils.getRootCauseMessage(e));
                evaluatedValue = value.raw();
            }
            result.put(key, evaluatedValue);
        });

        return Collections.unmodifiableMap(result);
    }
}
