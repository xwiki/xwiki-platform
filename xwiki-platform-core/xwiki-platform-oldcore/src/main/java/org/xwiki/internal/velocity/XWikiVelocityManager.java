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
package org.xwiki.internal.velocity;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.context.ExecutionContext;
import org.xwiki.environment.Environment;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.ObservationManager;
import org.xwiki.observation.event.Event;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.skin.Skin;
import org.xwiki.skin.SkinManager;
import org.xwiki.template.Template;
import org.xwiki.template.TemplateManager;
import org.xwiki.template.event.TemplateDeletedEvent;
import org.xwiki.template.event.TemplateEvent;
import org.xwiki.template.event.TemplateUpdatedEvent;
import org.xwiki.velocity.ScriptVelocityContext;
import org.xwiki.velocity.VelocityEngine;
import org.xwiki.velocity.VelocityManager;
import org.xwiki.velocity.VelocityTemplate;
import org.xwiki.velocity.XWikiVelocityException;
import org.xwiki.velocity.internal.DefaultVelocityManager;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.api.DeprecatedContext;
import com.xpn.xwiki.internal.template.InternalTemplateManager;

/**
 * Override {@link DefaultVelocityManager} to add XWiki platform specific things and especially deliver and cache a
 * different {@link VelocityEngine} depending on the context skin macros.vm.
 *
 * @version $Id$
 * @since 15.9RC1
 */
@Component
@Singleton
public class XWikiVelocityManager extends DefaultVelocityManager implements Initializable
{
    private static final String VELOCITYENGINE_CACHEKEY_NAME = "velocity.engine.key";

    private static final List<Event> EVENTS =
        Arrays.asList(new TemplateUpdatedEvent(), new TemplateDeletedEvent());

    /**
     * Used to access the current {@link XWikiContext}.
     */
    @Inject
    private Provider<XWikiContext> xcontextProvider;

    /**
     * Accessing it trough {@link Provider} since {@link TemplateManager} depends on {@link VelocityManager}.
     */
    @Inject
    private Provider<InternalTemplateManager> templates;

    @Inject
    private SkinManager skinManager;

    @Inject
    private ObservationManager observation;

    @Inject
    private Environment environment;

    @Inject
    private AuthorizationManager authorizationManager;

    @Inject
    private Logger logger;

    private final Map<String, VelocityEngine> velocityEngines = new ConcurrentHashMap<>();

    @Override
    public void initialize() throws InitializationException
    {
        super.initialize();

        this.observation.addListener(new EventListener()
        {
            @Override
            public void onEvent(Event event, Object source, Object data)
            {
                if (event instanceof TemplateEvent) {
                    TemplateEvent templateEvent = (TemplateEvent) event;

                    XWikiVelocityManager.this.velocityEngines.remove(templateEvent.getId());
                }
            }

            @Override
            public String getName()
            {
                return XWikiVelocityManager.class.getName();
            }

            @Override
            public List<Event> getEvents()
            {
                return EVENTS;
            }
        });
    }

    @Override
    protected ScriptVelocityContext getScriptVelocityContext()
    {
        ScriptVelocityContext velocityContext = super.getScriptVelocityContext();

        // Velocity specific bindings
        XWikiContext xcontext = this.xcontextProvider.get();
        // Add the "context" binding which is deprecated since 1.9.1.
        velocityContext.put("context", new DeprecatedContext(xcontext));

        return velocityContext;
    }

    private Template getSkinMacrosTemplate()
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
            template = this.templates.get().getSkinTemplate("macros.vm");

            if (templateCache != null) {
                templateCache.put(currentSkin.getId(), template);
            }
        }

        return template;
    }

    /**
     * @return the Velocity Engine corresponding to the current execution context. More specifically returns the
     *     Velocity Engine for the current skin since each skin has its own Velocity Engine so that each skin can have
     *     global velocimacros defined
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

        final Template skinMacrosTemplate;
        if (xcontext != null && xcontext.getWiki() != null) {
            skinMacrosTemplate = getSkinMacrosTemplate();
        } else {
            skinMacrosTemplate = null;
        }

        String cacheKey = skinMacrosTemplate != null ? skinMacrosTemplate.getId() : "default";

        // Get the Velocity Engine to use
        VelocityEngine velocityEngine = this.velocityEngines.get(cacheKey);
        if (velocityEngine == null) {
            velocityEngine = createVelocityEngine(cacheKey, skinMacrosTemplate);
        }

        return velocityEngine;
    }

    private synchronized VelocityEngine createVelocityEngine(String cacheKey, Template skinMacrosTemplate)
        throws XWikiVelocityException
    {
        VelocityEngine velocityEngine = this.velocityEngines.get(cacheKey);
        if (velocityEngine == null) {
            velocityEngine = createVelocityEngine();

            // Add default global macros to the engine
            try {
                injectBaseMacros(velocityEngine, skinMacrosTemplate);
            } catch (Exception e) {
                this.logger.warn("Failed to load global macros for engine with key [{}]: {}", cacheKey,
                    ExceptionUtils.getRootCauseMessage(e));
            }

            // Cache the VelocityEngine
            this.velocityEngines.put(cacheKey, velocityEngine);
        }

        return velocityEngine;
    }

    private void injectBaseMacros(VelocityEngine velocityEngine, Template skinMacrosTemplate) throws Exception
    {
        // Inject main macros
        try (InputStream stream = this.environment.getResourceAsStream("/templates/macros.vm")) {
            if (stream != null) {
                try (InputStreamReader reader = new InputStreamReader(stream)) {
                    VelocityTemplate mainMacros = compile("", reader);

                    velocityEngine.addGlobalMacros(mainMacros.getMacros());
                }
            }
        }

        // Inject skin macros if their author has at least Script rights.
        if (skinMacrosTemplate != null
            && this.authorizationManager.hasAccess(Right.SCRIPT, skinMacrosTemplate.getContent().getAuthorReference(),
            skinMacrosTemplate.getContent().getDocumentReference()))
        {
            VelocityTemplate skinMacros =
                compile("", new StringReader(skinMacrosTemplate.getContent().getContent()));
            velocityEngine.addGlobalMacros(skinMacros.getMacros());
        }
    }
}
