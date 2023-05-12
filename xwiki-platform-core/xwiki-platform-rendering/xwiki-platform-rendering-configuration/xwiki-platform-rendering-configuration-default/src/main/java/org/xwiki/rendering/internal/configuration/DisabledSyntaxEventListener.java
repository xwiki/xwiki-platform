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
package org.xwiki.rendering.internal.configuration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.bridge.event.ApplicationReadyEvent;
import org.xwiki.bridge.event.WikiReadyEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.event.Event;
import org.xwiki.rendering.configuration.ExtendedRenderingConfiguration;
import org.xwiki.rendering.parser.Parser;
import org.xwiki.rendering.syntax.Syntax;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

import static
    org.xwiki.rendering.internal.configuration.RenderingConfigClassDocumentConfigurationSource.CLASS_REFERENCE;
import static org.xwiki.rendering.internal.configuration.RenderingConfigClassDocumentConfigurationSource.DOC_REFERENCE;

/**
 * We bundle {@code Rendering.RenderingConfig} without a {@code Rendering.RenderingConfigClass} xobject, allowing us to
 * migrate Rendering configuration that were previously stored in the {@code xwiki.rendering.syntaxes} property in
 * {@code xwiki.cfg}.
 *
 * @version $Id$
 * @since 15.4RC1
 */
@Component
@Named("DisabledSyntaxEventListener")
@Singleton
public class DisabledSyntaxEventListener implements EventListener
{
    @Inject
    private Logger logger;

    @Inject
    @Named("xwikicfg")
    private ConfigurationSource xwikiCfgConfiguration;

    @Inject
    private Provider<ExtendedRenderingConfiguration> configurationProvider;

    /**
     * Used to lookup parsers and renderers to discover available syntaxes.
     */
    @Inject
    @Named("context")
    private Provider<ComponentManager> componentManagerProvider;

    @Override
    public String getName()
    {
        return this.getClass().getAnnotation(Named.class).value();
    }

    @Override
    public List<Event> getEvents()
    {
        // We need to set the disabled syntax config for the main wiki (ApplicationReadyEvent) and all subwikis
        // (WikiReadyEvent).
        return List.of(new ApplicationReadyEvent(), new WikiReadyEvent());
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        // If there's no Rendering.RenderingConfigClass xobject in the Rendering.RenderingConfig document, then
        // compute the list of syntaxes that should be disabled by default and set them in Rendering.RenderingConfig.
        XWiki xwiki = (XWiki) source;
        XWikiContext xcontext = (XWikiContext) data;
        try {
            XWikiDocument document = xwiki.getDocument(DOC_REFERENCE, xcontext);
            if (document.getXObject(CLASS_REFERENCE) == null) {
                BaseObject xobject = document.newXObject(CLASS_REFERENCE, xcontext);
                xobject.set("disabledSyntaxes", getDisabledSyntaxes(), xcontext);
                xwiki.saveDocument(document,
                    "Migrated Rendering configuration found in xwiki.cfg (xwiki.rendering.syntaxes)", true, xcontext);
            }
        } catch (XWikiException e) {
            // Simply log an error since there's not much we can do. When XWiki is restarted next time, it'll try to
            // recreate the missing xobject.
            this.logger.error(String.format("Failed to migrate the rendering configuration by adding a [%s] xobject "
                + "to [%s]", CLASS_REFERENCE, DOC_REFERENCE), e);
        }
    }

    private List<String> getDisabledSyntaxes()
    {
        List<String> disabledSyntaxes = new ArrayList<>();

        // Get the disabled syntaxes value from the old location and set them as the current disabled syntaxes.
        List<String> configuredSyntaxes =
            this.xwikiCfgConfiguration.getProperty("xwiki.rendering.syntaxes", List.class);
        // If there's no such property, then only allow the default syntax. We do this since we don't want users to
        // see bundled syntaxes by default. We only want them to see automatically syntaxes installed thereafter by
        // the Extension Manager.
        if (configuredSyntaxes == null || configuredSyntaxes.isEmpty()) {
            ExtendedRenderingConfiguration configuration = this.configurationProvider.get();
            disabledSyntaxes.addAll(computeDisabledSyntaxes(
                Collections.singletonList(configuration.getDefaultContentSyntax().toIdString())));
        } else {
            // Disable all syntaxes except those in xwiki.rendering.syntaxes
            disabledSyntaxes.addAll(computeDisabledSyntaxes(configuredSyntaxes));
        }

        return disabledSyntaxes;
    }

    private List<String> computeDisabledSyntaxes(List<String> configuredSyntaxes)
    {
        List<String> disabledSyntaxes = new ArrayList<>();
        for (Syntax availableSyntax : getAvailableParserSyntaxes()) {
            if (!configuredSyntaxes.contains(availableSyntax.toIdString())) {
                disabledSyntaxes.add(availableSyntax.toIdString());
            }
        }
        return disabledSyntaxes;
    }

    /**
     * @return the list of syntaxes for which a Parser is available
     */
    private List<Syntax> getAvailableParserSyntaxes()
    {
        List<Syntax> syntaxes = new ArrayList<>();
        try {
            for (Parser parser : this.componentManagerProvider.get().<Parser>getInstanceList(Parser.class)) {
                syntaxes.add(parser.getSyntax());
            }
        } catch (ComponentLookupException e) {
            // This shouldn't happen; if it does then it's critical
            throw new RuntimeException(String.format("Failed to lookup [%s] implementations", Parser.class), e);
        }

        return syntaxes;
    }
}
