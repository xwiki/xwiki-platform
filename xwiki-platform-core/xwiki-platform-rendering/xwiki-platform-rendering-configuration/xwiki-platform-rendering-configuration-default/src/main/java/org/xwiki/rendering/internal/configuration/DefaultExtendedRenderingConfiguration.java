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

import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.rendering.configuration.ExtendedRenderingConfiguration;
import org.xwiki.rendering.parser.ParseException;
import org.xwiki.rendering.parser.Parser;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.syntax.SyntaxRegistry;

import com.xpn.xwiki.CoreConfiguration;

/**
 * Extended configuration options for the Rendering subsystem.
 *
 * @version $Id$
 * @since 8.2M1
 */
@Component
@Singleton
public class DefaultExtendedRenderingConfiguration implements ExtendedRenderingConfiguration
{
    /**
     * Prefix for configuration keys for the Rendering module.
     */
    private static final String PREFIX = "rendering.";

    private static final String DISABLED_SYNTAXES_PROPERTY = "disabledSyntaxes";

    /**
     * Defines from where to read the rendering configuration data.
     */
    @Inject
    private ConfigurationSource configuration;

    @Inject
    @Named("rendering")
    private ConfigurationSource renderingConfiguration;

    @Inject
    @Named("xwikicfg")
    private ConfigurationSource xwikiCfgConfiguration;

    @Inject
    private CoreConfiguration coreConfiguration;

    /**
     * Used to lookup parsers and renderers to discover available syntaxes.
     */
    @Inject
    @Named("context")
    private Provider<ComponentManager> componentManagerProvider;

    @Inject
    private SyntaxRegistry syntaxRegistry;

    @Override
    public int getImageWidthLimit()
    {
        return this.configuration.getProperty(PREFIX + "imageWidthLimit", -1);
    }

    @Override
    public int getImageHeightLimit()
    {
        return this.configuration.getProperty(PREFIX + "imageHeightLimit", -1);
    }

    @Override
    public boolean isImageDimensionsIncludedInImageURL()
    {
        return this.configuration.getProperty(PREFIX + "imageDimensionsIncludedInImageURL", true);
    }

    @Override
    public List<Syntax> getDisabledSyntaxes()
    {
        List<Syntax> disabledSyntaxes = new ArrayList<>();

        // First, look in the document sources
        List<String> disabledSyntaxesAsStrings = this.renderingConfiguration.getProperty(DISABLED_SYNTAXES_PROPERTY);

        // Second, if there's no RenderingConfigClass xobject (i.e. when disabledSyntaxesAsStrings is null), then
        // convert the config values from "xwiki.rendering.syntaxes" in xwiki.cfg into disabled syntaxes.
        // Note: if disabledSyntaxesAsStrings is an empty list, it means that the user has not disabled any syntax and
        // we'll show all of them.
        if (disabledSyntaxesAsStrings == null) {
            List<Syntax> configuredSyntaxes =
                convertList(this.xwikiCfgConfiguration.getProperty("xwiki.rendering.syntaxes", List.class));
            // If there's no such property, then only allow the default syntax. We do this since we don't want users to
            // see bundled syntaxes by default. We only want them to see automatically syntaxes installed thereafter by
            // the Extension Manager.
            if (configuredSyntaxes == null || configuredSyntaxes.isEmpty()) {
                disabledSyntaxes.addAll(computeDisabledSyntaxes(
                    Collections.singletonList(getDefaultContentSyntax())));
            } else {
                // Disable all syntaxes except those in xwiki.rendering.syntaxes
                disabledSyntaxes.addAll(computeDisabledSyntaxes(configuredSyntaxes));
            }
        } else {
            // Convert into a list of Syntax objects
            disabledSyntaxes.addAll(convertList(disabledSyntaxesAsStrings));
        }
        return disabledSyntaxes;
    }

    @Override
    public List<Syntax> getConfiguredSyntaxes()
    {
        List<Syntax> configuredSyntaxes = new ArrayList<>();
        List<Syntax> disabledSyntaxes = getDisabledSyntaxes();
        for (Syntax availablesyntax : getAvailableParserSyntaxes()) {
            if (!disabledSyntaxes.contains(availablesyntax)) {
                configuredSyntaxes.add(availablesyntax);
            }
        }
        return configuredSyntaxes;
    }

    @Override
    public Syntax getDefaultContentSyntax()
    {
        // TODO: Move the code from DefaultCoreConfiguration here and introduce a new property for it, for ex:
        // rendering.defaultContentSyntax
        // However keep supporting the old property for backward-compatibility reasons.
        return this.coreConfiguration.getDefaultDocumentSyntax();
    }

    private List<Syntax> convertList(List<String> syntaxesAsStrings)
    {
        if (syntaxesAsStrings == null) {
            return null;
        }

        List<Syntax> syntaxes = new ArrayList<>();
        for (String syntaxAsString : syntaxesAsStrings) {
            try {
                syntaxes.add(this.syntaxRegistry.resolveSyntax(syntaxAsString));
            } catch (ParseException e) {
                throw new RuntimeException(String.format("Failed to convert [%s] into Syntax object", syntaxAsString),
                    e);
            }
        }
        return syntaxes;
    }

    private List<Syntax> computeDisabledSyntaxes(List<Syntax> configuredSyntaxes)
    {
        List<Syntax> disabledSyntaxes = new ArrayList<>();
        for (Syntax availableSyntax : getAvailableParserSyntaxes()) {
            if (!configuredSyntaxes.contains(availableSyntax)) {
                disabledSyntaxes.add(availableSyntax);
            }
        }
        return disabledSyntaxes;
    }

    /**
     * @return the list of syntaxes for which a Parser is available
     */
    public List<Syntax> getAvailableParserSyntaxes()
    {
        List<Syntax> syntaxes = new ArrayList<>();
        try {
            for (Parser parser : this.componentManagerProvider.get().<Parser>getInstanceList(Parser.class)) {
                syntaxes.add(parser.getSyntax());
            }
        } catch (ComponentLookupException e) {
            // This shouldn't happen; if it does then it's critical
            throw new RuntimeException("Failed to lookup parsers", e);
        }

        return syntaxes;
    }
}
