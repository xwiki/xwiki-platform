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
package org.xwiki.javascript.importmap.internal;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.extension.Extension;
import org.xwiki.extension.repository.CoreExtensionRepository;
import org.xwiki.extension.repository.InstalledExtensionRepository;
import org.xwiki.javascript.importmap.internal.parser.JavascriptImportmapException;
import org.xwiki.javascript.importmap.internal.parser.JavascriptImportmapParser;
import org.xwiki.model.namespace.WikiNamespace;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.RawBlock;
import org.xwiki.webjars.WebJarsUrlFactory;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;

import static com.fasterxml.jackson.databind.MapperFeature.SORT_PROPERTIES_ALPHABETICALLY;
import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCauseMessage;
import static org.xwiki.rendering.syntax.Syntax.HTML_5_0;

/**
 * Resolve the importmap from the webjar declarations, using the {@code "xwiki.extension.javascript.modules.importmap"}
 * pom property.
 *
 * @version $Id$
 * @since 17.10.0RC1
 */
@Component(roles = JavascriptImportmapResolver.class)
@Singleton
public class JavascriptImportmapResolver
{
    private static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder()
        // Keeps the output deterministic.
        .configure(SORT_PROPERTIES_ALPHABETICALLY, true)
        .build();

    private static final String JAVASCRIPT_IMPORTMAP_PROPERTY = "xwiki.extension.javascript.modules.importmap";

    private static final JavascriptImportmapParser JAVASCRIPT_IMPORTMAP_PARSER = new JavascriptImportmapParser();

    /**
     * Cache lock to prevent race conditions when retrieving cached values.
     */
    private final Object cacheLock = new Object();

    /**
     * The cached value, concurrent operation on this field must use the {@link #cacheLock} object for synchronization.
     */
    private Block cachedValue;

    @Inject
    private Logger logger;

    @Inject
    private InstalledExtensionRepository installedExtensionRepository;

    @Inject
    private CoreExtensionRepository coreExtensionRepository;

    @Inject
    private WebJarsUrlFactory webJarsUrlFactory;

    @Inject
    private WikiDescriptorManager wikiDescriptorManager;

    /**
     * @return the block to render from the importmap, possibly cached
     */
    public Block getBlock()
    {
        synchronized (this.cacheLock) {
            if (this.cachedValue == null) {
                compute();
            }
            return this.cachedValue;
        }
    }

    /**
     * Clear the cache of the resolver.
     */
    public void clearCache()
    {
        synchronized (this.cacheLock) {
            this.cachedValue = null;
        }
    }

    private void compute()
    {
        var wikiNamespace = new WikiNamespace(this.wikiDescriptorManager.getCurrentWikiId()).serialize();
        List<Map<String, String>> extensionsWithImportMap = Stream.concat(
                this.installedExtensionRepository.getInstalledExtensions(wikiNamespace).stream(),
                this.coreExtensionRepository.getCoreExtensions().stream())
            .filter(extension -> accessProperty(extension) != null)
            .map(extension -> {
                String importMapJSON = accessProperty(extension);
                Map<String, String> extensionImportMap;
                try {
                    extensionImportMap = JAVASCRIPT_IMPORTMAP_PARSER.parse(importMapJSON)
                        .entrySet()
                        .stream()
                        .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            e -> this.webJarsUrlFactory.url(e.getValue())
                        ));
                } catch (JavascriptImportmapException e) {
                    this.logger.warn("Unable to read property [{}] for extension [{}]. Cause: [{}]",
                        JAVASCRIPT_IMPORTMAP_PROPERTY, extension.getId(), getRootCauseMessage(e));
                    extensionImportMap = Map.of();
                }
                return extensionImportMap;
            })
            .toList();

        Map<String, String> resolvedMap = new HashMap<>();
        for (Map<String, String> objectObjectMap : extensionsWithImportMap) {
            for (Map.Entry<String, String> objectObjectEntry : objectObjectMap.entrySet()) {
                String key = objectObjectEntry.getKey();
                String value = objectObjectEntry.getValue();
                String existingValue = resolvedMap.get(key);
                if (existingValue == null) {
                    resolvedMap.put(key, value);
                } else if (!value.equals(existingValue)) {
                    this.logger.warn(
                        "Conflicting importmap resolution for key [{}]. Existing value: [{}], new value: [{}]",
                        key, existingValue, value);
                }
            }
        }

        String json;
        try {
            json = OBJECT_MAPPER.writeValueAsString(Map.of("imports", resolvedMap));
        } catch (JsonProcessingException e) {
            this.logger.warn("Failed to serialize the importmap. Cause: [{}]", getRootCauseMessage(e));
            json = "{}";
        }

        this.cachedValue = new RawBlock("<script type='importmap'>%s</script>".formatted(json), HTML_5_0);
    }

    private static String accessProperty(Extension extension)
    {
        return extension.getProperty(JAVASCRIPT_IMPORTMAP_PROPERTY);
    }
}
