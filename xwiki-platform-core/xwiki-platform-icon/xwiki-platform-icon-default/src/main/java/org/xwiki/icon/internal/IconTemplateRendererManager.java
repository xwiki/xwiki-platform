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
package org.xwiki.icon.internal;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;

import org.xwiki.cache.Cache;
import org.xwiki.cache.CacheException;
import org.xwiki.cache.CacheFactory;
import org.xwiki.cache.CacheManager;
import org.xwiki.cache.config.CacheConfiguration;
import org.xwiki.cache.config.LRUCacheConfiguration;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.icon.IconException;
import org.xwiki.velocity.VelocityManager;
import org.xwiki.velocity.VelocityTemplate;
import org.xwiki.velocity.internal.util.VelocityDetector;
import org.xwiki.webjars.WebJarsUrlFactory;

import com.xpn.xwiki.XWikiContext;

/**
 * Manager for creating and caching {@link IconTemplateRenderer} instances based on templates.
 * <p>
 * This class handles the parsing of templates, and returns either a generic Velocity renderer or a specialized, more
 * efficient renderer.
 *
 * @version $Id$
 * @since 17.6.0RC1
 */
@Singleton
@Component(roles = IconTemplateRendererManager.class)
public class IconTemplateRendererManager implements Initializable
{
    private static final String ICON_RENDERER_CACHE_ID = "iconset.renderer";

    private static final Pattern SIMPLE_REPLACEMENT_PATTERN =
        Pattern.compile("^([^$#]*)\\$(?:icon|\\{icon\\})(|[^$#._a-zA-Z0-9][^$#]*)$");

    private static final String ARGUMENT = "\\s*'([^']+)'\\s*";

    private static final Pattern WEBJAR_URL_PATTERN =
        Pattern.compile("^\\s*\\$services\\.webjars\\.url\\s*\\(%s(?:,%s(?:,%s)?)?\\)\\s*$"
            .formatted(ARGUMENT, ARGUMENT, ARGUMENT));

    // Match icons in the form [[image:path:$xwiki.getSkinFile("icons/silk/${icon}.png")||data-xwiki-lightbox="false"]].
    private static final Pattern SKIN_FILE_PATTERN =
        Pattern.compile("^([^#$]*)\\$xwiki\\.getSkinFile\\(\"([^\"$#]*)\\$\\{icon\\}([^\"$#]*)\"\\)([^#$]*)$");

    @Inject
    private VelocityDetector velocityDetector;

    /**
     * Make testing easier by not injecting a {@link WebJarsUrlFactory} directly.
     */
    @Inject
    private Provider<WebJarsUrlFactory> webJarsUrlFactoryProvider;

    @Inject
    private VelocityManager velocityManager;

    @Inject
    private VelocityRenderer velocityRenderer;

    @Inject
    private CacheManager cacheManager;

    @Inject
    private Provider<XWikiContext> contextProvider;

    private Cache<IconTemplateRenderer> cache;

    @Override
    public void initialize() throws InitializationException
    {
        try {
            // We expect about 5 entries per icon theme, so 100 should be more than enough, this is primarily
            // protection against excessive memory usage in unforeseen circumstances.
            CacheConfiguration configuration = new LRUCacheConfiguration(ICON_RENDERER_CACHE_ID, 100);
            CacheFactory cacheFactory = this.cacheManager.getCacheFactory();
            this.cache = cacheFactory.newCache(configuration);
        } catch (ComponentLookupException | CacheException e) {
            throw new InitializationException("Failed to initialize the icon renderer Cache.", e);
        }
    }

    /**
     * Get a {@link IconTemplateRenderer} based on the given template.
     *
     * @param template the template to use for rendering icons
     * @return a new instance of {@link IconTemplateRenderer}
     * @throws IconException if the template is invalid or cannot be processed
     */
    public IconTemplateRenderer getRenderer(String template) throws IconException
    {
        IconTemplateRenderer renderer = this.cache.get(template);

        if (renderer == null) {
            renderer = this.createRenderer(template);

            this.cache.set(template, renderer);
        }

        return renderer;
    }

    private IconTemplateRenderer createRenderer(String template) throws IconException
    {
        if (!this.velocityDetector.containsVelocityScript(template)) {
            // No Velocity code - just return the template.
            return (icon, documentReference) -> template;
        } else {
            Matcher matcher = SIMPLE_REPLACEMENT_PATTERN.matcher(template);
            if (matcher.matches()) {
                String start = matcher.group(1);
                String end = matcher.group(2);
                return (icon, documentReference) -> start + icon + end;
            }

            Optional<IconTemplateRenderer> webJarRenderer = getWebJarRenderer(template);

            if (webJarRenderer.isPresent()) {
                return webJarRenderer.get();
            }

            Optional<IconTemplateRenderer> skinFileRenderer = getSkinFileRenderer(template);

            if (skinFileRenderer.isPresent()) {
                return skinFileRenderer.get();
            }
        }

        return getVelocityRenderer(template);
    }

    private IconTemplateRenderer getVelocityRenderer(String template) throws IconException
    {
        try {
            VelocityTemplate velocityTemplate = this.velocityManager.compile(template, new StringReader(template));

            return (icon, documentReference) ->
                this.velocityRenderer.render(velocityTemplate, icon, documentReference);
        } catch (Exception e) {
            throw new IconException("Failed to compile Velocity template: " + template, e);
        }
    }

    private Optional<IconTemplateRenderer> getWebJarRenderer(String template) throws IconException
    {
        Matcher webJarUrlMatcher = WEBJAR_URL_PATTERN.matcher(template);
        if (webJarUrlMatcher.matches()) {
            List<String> arguments = new ArrayList<>();
            for (int i = 1; i <= webJarUrlMatcher.groupCount(); i++) {
                String argument = webJarUrlMatcher.group(i);
                if (argument != null) {
                    arguments.add(argument);
                }
            }

            WebJarsUrlFactory webJarsUrlFactory = this.webJarsUrlFactoryProvider.get();

            if (arguments.size() == 1) {
                return Optional.of((icon, documentReference) -> webJarsUrlFactory.url(arguments.get(0)));
            } else if (arguments.size() == 2) {
                return Optional.of((icon, documentReference) ->
                    webJarsUrlFactory.url(arguments.get(0), arguments.get(1)));
            } else if (arguments.size() == 3) {
                return Optional.of((icon, documentReference) ->
                    webJarsUrlFactory.url(arguments.get(0), arguments.get(1), arguments.get(2)));
            } else {
                throw new IconException("Invalid number of arguments for webjar URL: " + arguments.size());
            }
        }

        return Optional.empty();
    }

    private Optional<IconTemplateRenderer> getSkinFileRenderer(String template)
    {
        Matcher skinFileMatcher = SKIN_FILE_PATTERN.matcher(template);
        if (skinFileMatcher.matches()) {
            String start = skinFileMatcher.group(1);
            String iconPrefix = skinFileMatcher.group(2);
            String iconSuffix = skinFileMatcher.group(3);
            String end = skinFileMatcher.group(4);

            return Optional.of((icon, documentReference) -> {
                XWikiContext context = this.contextProvider.get();
                String fileName = iconPrefix + icon + iconSuffix;
                return start + context.getWiki().getSkinFile(fileName, context) + end;
            });
        }

        return Optional.empty();
    }
}
