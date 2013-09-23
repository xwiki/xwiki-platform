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

import java.util.Map;
import java.util.Properties;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.script.ScriptContext;

import org.apache.commons.lang3.StringUtils;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.runtime.RuntimeConstants;
import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;
import org.xwiki.script.ScriptContextManager;
import org.xwiki.velocity.VelocityConfiguration;
import org.xwiki.velocity.VelocityEngine;
import org.xwiki.velocity.VelocityFactory;
import org.xwiki.velocity.VelocityManager;
import org.xwiki.velocity.XWikiVelocityException;
import org.xwiki.velocity.XWikiWebappResourceLoader;
import org.xwiki.velocity.internal.VelocityExecutionContextInitializer;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.api.DeprecatedContext;
import com.xpn.xwiki.web.Utils;

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
public class DefaultVelocityManager implements VelocityManager
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

    @Override
    @SuppressWarnings("unchecked")
    public VelocityContext getVelocityContext()
    {
        // The Velocity Context is set in VelocityRequestInterceptor, when the XWiki Request is initialized so we are
        // guaranteed it is defined when this method is called.
        VelocityContext vcontext =
            (VelocityContext) this.execution.getContext().getProperty(
                VelocityExecutionContextInitializer.VELOCITY_CONTEXT_ID);

        // Copy current JSR223 ScriptContext binding.
        ScriptContext scriptContext = this.scriptContextManager.getScriptContext();
        for (Map.Entry<String, Object> entry : scriptContext.getBindings(ScriptContext.ENGINE_SCOPE).entrySet()) {
            // Not ideal since it does not allow to modify a binding but it's too dangerous for existing velocity
            // scripts otherwise.
            if (!vcontext.containsKey(entry.getKey())) {
                vcontext.put(entry.getKey(), entry.getValue());
            }
        }
        
        XWikiContext xcontext = xcontextProvider.get();

        // Add the "context" binding which is deprecated since 1.9.1.
        vcontext.put("context", new DeprecatedContext(xcontext));

        // Make sure the execution context and the XWiki context point to the same Velocity context instance. There is
        // old code that accesses the Velocity context from the XWiki context.
        xcontext.put("vcontext", vcontext);

        return vcontext;
    }

    /**
     * @return the key used to cache the Velocity Engines. We have one Velocity Engine per skin which has a macros.vm
     *         file on the filesystem. Right now we don't support macros.vm defined in custom skins in wiki pages.
     */
    private String getVelocityEngineCacheKey(String skin, XWikiContext context)
    {
        // We need the path relative to the webapp's home folder so we need to remove all path before
        // the skins/ directory. This is a bit of a hack and should be improved with a proper api.
        String skinMacros = context.getWiki().getSkinFile("macros.vm", skin, context);
        // If we can't reach a filesystem based macros.vm with the current skin, then use a "default" cache id
        String cacheKey = "default";
        if (skinMacros != null) {
            // We're only using the path starting with the skin name since sometimes we'll
            // get ".../skins/skins/<skinname>/...", sometimes we get ".../skins/<skinname>/...",
            // sometimes we get "skins/<skinname>/..." and if the skin is done in wiki pages
            // we get ".../skin/...".
            int pos = skinMacros.indexOf("skins/");
            if (pos > -1) {
                cacheKey = skinMacros.substring(pos);
            }
        }
        // If no macros.vm file has been found for the passed skin, we can try to get a baseskin -
        // this only if the skin is a wiki page skin.
        // We first need to make sure the skin is actually a wiki page skin since otherwise
        // the notion of baseskin is meaningless.
        // We also need to ensure the presence of a dot in the skin name,
        // otherwise XWiki#exists will assume the context's space is the skin's space
        // (as in "XWiki.albatross" if the context space is XWiki) which can lead to misbehavior.
        else if (skin.indexOf(".") > 0 && context.getWiki().exists(skin, context)) {
            // If the macros.vm file is stored in a wiki page (in a macros.vm property in
            // a XWikiSkins object) then we use the parent skin's macros.vm since we
            // currently don't support having global velocimacros defined in wiki pages.
            String baseSkin = context.getWiki().getBaseSkin(skin, context);
            // Avoid plain recursive calls
            if (StringUtils.equals(baseSkin, skin)) {
                baseSkin = context.getWiki().getDefaultBaseSkin(context);
            }
            if (!StringUtils.equals(baseSkin, skin)) {
                try {
                    cacheKey = getVelocityEngineCacheKey(baseSkin, context);
                } catch (StackOverflowError ex) {
                    // Circular dependency, just return the default key
                }
            }
        }
        return cacheKey;
    }

    /**
     * @return the Velocity Engine corresponding to the current execution context. More specifically returns
     *         the Velocity Engine for the current skin since each skin has its own Velocity Engine so that each
     *         skin can have global velocimacros defined
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
        String skin = xcontext.getWiki().getSkin(xcontext);
        String cacheKey = getVelocityEngineCacheKey(skin, xcontext);

        // Get the Velocity Engine to use
        VelocityFactory velocityFactory = Utils.getComponent(VelocityFactory.class);
        VelocityEngine velocityEngine;
        if (velocityFactory.hasVelocityEngine(cacheKey)) {
            velocityEngine = velocityFactory.getVelocityEngine(cacheKey);
        } else {
            // Note 1: This block is synchronized to prevent threads from creating several instances of
            // Velocity Engines (for the same skin).
            // Note 2: We do this instead of marking the whole method as synchronized since it seems this method is
            // called quite often and we would incur the synchronization penalty. Ideally the engine should be
            // created only when a new skin is created and not be on the main execution path.
            synchronized (this) {
                if (velocityFactory.hasVelocityEngine(cacheKey)) {
                    velocityEngine = velocityFactory.getVelocityEngine(cacheKey);
                } else {
                    // Gather the global Velocity macros that we want to have. These are skin dependent.
                    Properties properties = new Properties();

                    // If the user hasn't specified any custom Velocity Resource Loader to use, use the XWiki Resource Loader
                    if (!Utils.getComponent(VelocityConfiguration.class).getProperties().containsKey(RESOURCE_LOADER)) {
                        properties.setProperty(RESOURCE_LOADER, "xwiki");
                        properties.setProperty(RESOURCE_LOADER_CLASS, XWikiWebappResourceLoader.class.getName());
                    }

                    // Note: if you don't want any template to be used set the property named
                    // xwiki.render.velocity.macrolist to an empty string value.
                    String macroList = xcontext.getWiki().Param("xwiki.render.velocity.macrolist");
                    if (macroList == null) {
                        macroList = "/templates/macros.vm" + (cacheKey.equals("default") ? "" : "," + cacheKey);
                    }
                    properties.put(RuntimeConstants.VM_LIBRARY, macroList);
                    velocityEngine = velocityFactory.createVelocityEngine(cacheKey, properties);
                }
            }
        }

        return velocityEngine;
    }
}
