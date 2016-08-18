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
package com.xpn.xwiki.render;

import java.io.Reader;
import java.io.Writer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.script.ScriptContext;

import org.apache.commons.io.output.NullWriter;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.runtime.RuntimeSingleton;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.ObservationManager;
import org.xwiki.observation.event.Event;
import org.xwiki.script.ScriptContextManager;
import org.xwiki.skin.Skin;
import org.xwiki.skin.SkinManager;
import org.xwiki.template.Template;
import org.xwiki.template.TemplateManager;
import org.xwiki.template.event.TemplateDeletedEvent;
import org.xwiki.template.event.TemplateEvent;
import org.xwiki.template.event.TemplateUpdatedEvent;
import org.xwiki.velocity.VelocityConfiguration;
import org.xwiki.velocity.VelocityEngine;
import org.xwiki.velocity.VelocityFactory;
import org.xwiki.velocity.VelocityManager;
import org.xwiki.velocity.XWikiVelocityException;
import org.xwiki.velocity.XWikiWebappResourceLoader;
import org.xwiki.velocity.internal.VelocityExecutionContextInitializer;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.api.DeprecatedContext;
import com.xpn.xwiki.internal.template.SUExecutor;

/**
 * Note: This class should be moved to the Velocity module. However this is not possible right now since we need to
 * populate the Velocity Context with XWiki objects that are located in the Core (such as the XWiki object for example)
 * and since the Core needs to call the Velocity module this would cause a circular dependency.
 *
 * @version $Id$
 * @since 1.5M1
 */
@Component
@Singleton
// TODO: refactor to move it in xwiki-commons, the dependencies on the model are actually quite minor
public class DefaultVelocityManager implements VelocityManager, Initializable
{
    /**
     * The name of the Velocity configuration property that specifies the ResourceLoader name that Velocity should use
     * when locating templates.
     */
    private static final String RESOURCE_LOADER = "resource.loader";

    /**
     * The name of the Velocity configuration property that specifies the ResourceLoader class to use to locate Velocity
     * templates.
     */
    private static final String RESOURCE_LOADER_CLASS = "xwiki.resource.loader.class";

    private static final String VELOCITYENGINE_CACHEKEY_NAME = "velocity.engine.key";

    private static final List<Event> EVENTS =
        Arrays.<Event>asList(new TemplateUpdatedEvent(), new TemplateDeletedEvent());

    /**
     * Used to access the current {@link org.xwiki.context.ExecutionContext}.
     */
    @Inject
    private Execution execution;

    /**
     * Used to access the current {@link XWikiContext}.
     */
    @Inject
    private Provider<XWikiContext> xcontextProvider;

    /**
     * Used to get the current script context.
     */
    @Inject
    private ScriptContextManager scriptContextManager;

    @Inject
    private VelocityFactory velocityFactory;

    @Inject
    private VelocityConfiguration velocityConfiguration;

    /**
     * Accessing it trough {@link Provider} since {@link TemplateManager} depends on {@link VelocityManager}.
     */
    @Inject
    private Provider<TemplateManager> templates;

    @Inject
    private SkinManager skinManager;

    @Inject
    private ObservationManager observation;

    @Inject
    private SUExecutor suExecutor;

    @Inject
    private Logger logger;

    /**
     * Binding that should stay on Velocity side only.
     */
    private final Set<String> reservedBindings = new HashSet<>();

    @Override
    public void initialize() throws InitializationException
    {
        this.observation.addListener(new EventListener()
        {
            @Override
            public void onEvent(Event event, Object source, Object data)
            {
                if (event instanceof TemplateEvent) {
                    TemplateEvent templateEvent = (TemplateEvent) event;

                    DefaultVelocityManager.this.velocityFactory.removeVelocityEngine(templateEvent.getId());
                }
            }

            @Override
            public String getName()
            {
                return DefaultVelocityManager.class.getName();
            }

            @Override
            public List<Event> getEvents()
            {
                return EVENTS;
            }
        });

        // Set reserved bindings

        // "context" is a reserved binding in JSR223 world
        this.reservedBindings.add("context");

        // Macros directive
        this.reservedBindings.add("macro");
        // Foreach directive
        this.reservedBindings.add("foreach");
        this.reservedBindings.add(this.velocityConfiguration.getProperties().getProperty(RuntimeConstants.COUNTER_NAME,
            RuntimeSingleton.getString(RuntimeConstants.COUNTER_NAME)));
        this.reservedBindings.add(this.velocityConfiguration.getProperties().getProperty(RuntimeConstants.HAS_NEXT_NAME,
            RuntimeSingleton.getString(RuntimeConstants.HAS_NEXT_NAME)));
        // Evaluate directive
        this.reservedBindings.add("evaluate");
        // TryCatch directive
        this.reservedBindings.add("exception");
        this.reservedBindings.add("try");
        // Default directive
        this.reservedBindings.add("define");
        // The name of the context variable used for the template-level scope
        this.reservedBindings.add("template");
    }

    @Override
    public VelocityContext getVelocityContext()
    {
        ScriptVelocityContext velocityContext;

        // Make sure the velocity context support ScriptContext synchronization
        VelocityContext currentVelocityContext = getCurrentVelocityContext();
        if (currentVelocityContext instanceof ScriptVelocityContext) {
            velocityContext = (ScriptVelocityContext) currentVelocityContext;
        } else {
            velocityContext = new ScriptVelocityContext(currentVelocityContext, this.reservedBindings);
            this.execution.getContext().setProperty(VelocityExecutionContextInitializer.VELOCITY_CONTEXT_ID,
                velocityContext);
        }

        // Synchronize with ScriptContext
        ScriptContext scriptContext = this.scriptContextManager.getScriptContext();
        velocityContext.setScriptContext(scriptContext);

        // Velocity specific bindings
        XWikiContext xcontext = this.xcontextProvider.get();
        // Add the "context" binding which is deprecated since 1.9.1.
        velocityContext.put("context", new DeprecatedContext(xcontext));

        return velocityContext;
    }

    @Override
    public VelocityContext getCurrentVelocityContext()
    {
        // The Velocity Context is set in VelocityExecutionContextInitializer, when the XWiki Request is initialized
        // so we are guaranteed it is defined when this method is called.
        return (VelocityContext) this.execution.getContext()
            .getProperty(VelocityExecutionContextInitializer.VELOCITY_CONTEXT_ID);
    }

    /**
     * @return the key used to cache the Velocity Engines. We have one Velocity Engine per skin which has a macros.vm
     *         file on the filesystem. Right now we don't support macros.vm defined in custom skins in wiki pages.
     */
    private Template getVelocityEngineMacrosTemplate()
    {
        Template template = null;
        Map<String, Template> templateCache = null;

        Skin currentSkin = this.skinManager.getCurrentSkin(true);

        // Generating this key is very expensive so we cache it in the context
        ExecutionContext econtext = this.execution.getContext();
        if (econtext != null) {
            templateCache = (Map<String, Template>) econtext.getProperty(VELOCITYENGINE_CACHEKEY_NAME);
            if (templateCache == null) {
                templateCache = new HashMap<>();
                econtext.setProperty(VELOCITYENGINE_CACHEKEY_NAME, templateCache);
            } else {
                template = templateCache.get(currentSkin.getId());
            }
        }

        if (template == null) {
            template = this.templates.get().getTemplate("macros.vm");

            if (templateCache != null) {
                templateCache.put(currentSkin.getId(), template);
            }
        }

        return template;
    }

    /**
     * @return the Velocity Engine corresponding to the current execution context. More specifically returns the
     *         Velocity Engine for the current skin since each skin has its own Velocity Engine so that each skin can
     *         have global velocimacros defined
     * @throws XWikiVelocityException in case of an error while creating a Velocity Engine
     */
    @Override
    public VelocityEngine getVelocityEngine() throws XWikiVelocityException
    {
        // Note: For improved performance we cache the Velocity Engines in order not to
        // recreate them all the time. The key we use is the location to the skin's macro.vm
        // file since caching on the skin would create more Engines than needed (some skins
        // don't have a macros.vm file and some skins inherit from others).

        // Create a Velocity context using the Velocity Manager associated to the current skin's
        // macros.vm

        // Get the location of the skin's macros.vm file
        XWikiContext xcontext = this.xcontextProvider.get();

        final Template template;
        if (xcontext != null && xcontext.getWiki() != null) {
            template = getVelocityEngineMacrosTemplate();
        } else {
            template = null;
        }

        String cacheKey = template != null ? template.getId() : "default";

        // Get the Velocity Engine to use
        VelocityEngine velocityEngine = this.velocityFactory.getVelocityEngine(cacheKey);
        if (velocityEngine == null) {
            // Note 1: This block is synchronized to prevent threads from creating several instances of
            // Velocity Engines (for the same skin).
            // Note 2: We do this instead of marking the whole method as synchronized since it seems this method is
            // called quite often and we would incur the synchronization penalty. Ideally the engine should be
            // created only when a new skin is created and not be on the main execution path.
            synchronized (this) {
                velocityEngine = this.velocityFactory.getVelocityEngine(cacheKey);
                if (velocityEngine == null) {
                    // Gather the global Velocity macros that we want to have. These are skin dependent.
                    Properties properties = new Properties();

                    // If the user hasn't specified any custom Velocity Resource Loader to use, use the XWiki Resource
                    // Loader
                    if (!this.velocityConfiguration.getProperties().containsKey(RESOURCE_LOADER)) {
                        properties.setProperty(RESOURCE_LOADER, "xwiki");
                        properties.setProperty(RESOURCE_LOADER_CLASS, XWikiWebappResourceLoader.class.getName());
                    }

                    if (xcontext != null && xcontext.getWiki() != null) {
                        // Note: if you don't want any template to be used set the property named
                        // xwiki.render.velocity.macrolist to an empty string value.
                        String macroList = xcontext.getWiki().Param("xwiki.render.velocity.macrolist");
                        if (macroList == null) {
                            macroList = "/templates/macros.vm";
                        }
                        properties.put(RuntimeConstants.VM_LIBRARY, macroList);
                    }
                    velocityEngine = this.velocityFactory.createVelocityEngine(cacheKey, properties);

                    if (template != null) {
                        // Local macros template
                        // We execute it ourself to support any kind of template, Velocity only support resource
                        // template by default
                        try {
                            final VelocityEngine finalVelocityEngine = velocityEngine;

                            this.suExecutor.call(() -> {
                                finalVelocityEngine.evaluate(new VelocityContext(), NullWriter.NULL_WRITER, "",
                                    template.getContent().getContent());

                                return null;
                            }, template.getContent().getAuthorReference());
                        } catch (Exception e) {
                            this.logger.error("Failed to evaluate macros templates [{}]", template.getPath(), e);
                        }
                    }
                }
            }
        }

        return velocityEngine;
    }

    @Override
    public boolean evaluate(Writer out, String templateName, Reader source) throws XWikiVelocityException
    {
        // Get up to date Velocity context
        VelocityContext velocityContext = getVelocityContext();

        // Execute Velocity context
        return getVelocityEngine().evaluate(velocityContext, out, templateName, source);
    }
}
