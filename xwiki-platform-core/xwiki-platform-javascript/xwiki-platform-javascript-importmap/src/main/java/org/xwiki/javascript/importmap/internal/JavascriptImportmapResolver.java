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

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.jspecify.annotations.NonNull;
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
import org.xwiki.webjars.WebjarPathDescriptor;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;

import static com.fasterxml.jackson.databind.MapperFeature.SORT_PROPERTIES_ALPHABETICALLY;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCauseMessage;
import static org.xwiki.javascript.importmap.internal.parser.JavascriptImportmapParser.JAVASCRIPT_IMPORTMAP_PROPERTY;
import static org.xwiki.rendering.syntax.Syntax.HTML_5_0;

/**
 * Resolve the importmap from the webjar declarations, using the
 * {@link JavascriptImportmapParser#JAVASCRIPT_IMPORTMAP_PROPERTY} pom property.
 *
 * @version $Id$
 * @since 18.0.0RC1
 */
@Component(roles = JavascriptImportmapResolver.class)
@Singleton
public class JavascriptImportmapResolver
{
    private static final ObjectMapper OBJECT_MAPPER = JsonMapper.builder()
        // Keeps the output deterministic.
        .configure(SORT_PROPERTIES_ALPHABETICALLY, true)
        .build();

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
//            if (this.cachedValue == null) {
            compute();
//            }
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
        List<Extension> extensions = Stream.concat(
            this.installedExtensionRepository.getInstalledExtensions(wikiNamespace).stream(),
            this.coreExtensionRepository.getCoreExtensions().stream()
        ).toList();
        List<Map<String, String>> extensionsWithImportMap = computeExtensionsWithImportMap(extensions);

        Map<String, String> resolvedMap = new HashMap<>();
        resolveExtensionsWithImportMap(extensionsWithImportMap, resolvedMap);

        extensions.stream().filter(extension -> !extensionsWithImportMap.contains(extension))
            .map(extension -> {
                try (var s = extension.getFile().openStream()) {
//                    var destination = Files.createTempFile("jar", "").toFile();
//                    InputStream source = extension.getFile().openStream();
//                    FileUtils.copyInputStreamToFile(source, destination);
//                    JarFile zipInputStream = new JarFile(destination);
//                    var entry = zipInputStream.getEntry(
//                        "META-INF/resources/webjars/" + extension.getProperty("maven.artifactid") +
//                            "/package.json");
//                    if(entry != null) {
//                        return new Object[] {
//                            extension,
//                            new String(zipInputStream.getInputStream(entry).readAllBytes(), UTF_8)
//                        };
//                    }
                    ZipInputStream zipInputStream = new ZipInputStream(s);
                    ZipEntry ze;
                    while ((ze = zipInputStream.getNextEntry()) != null) {
                        if (ze.getName()
                            .equals("META-INF/resources/webjars/"+extension.getProperty("maven.artifactid")+"/package.json"))
                        {
                            // read current entry data [web:11][web:19]
                            return new Object[] { extension, new String(zipInputStream.readAllBytes(), UTF_8) };
                        }
                    }
                    return null;
                } catch (IOException e) {
                    // TODO: handle exception
                    return null;
                }
            }).filter(obj -> obj != null && obj[1] != null)
            .forEach(obj -> {
                // TODO: make type safe
                var packageJson = (String) obj[1];
                var extension = (Extension) obj[0];
                try {
                    Map<?, ?> map = new ObjectMapper().readValue(packageJson, Map.class);
                    var name = String.valueOf(map.get("name"));
                    if (!resolvedMap.containsKey(name)) {
                        Object module = map.get("module");
                        if (module == null) {
                            module = map.get("main");
                        }
                        resolvedMap.put(name, this.webJarsUrlFactory.url(extension.getId().toString(),
                            String.valueOf(module)));
                    }
                } catch (JsonProcessingException e) {
                    // TODO: handle errors
                }
            });

        String json;
        try {
            json = OBJECT_MAPPER.writeValueAsString(Map.of("imports", resolvedMap));
        } catch (JsonProcessingException e) {
            this.logger.warn("Failed to serialize the importmap. Cause: [{}]", getRootCauseMessage(e));
            json = "{}";
        }

        this.cachedValue = new RawBlock("<script type='importmap'>%s</script>".formatted(json), HTML_5_0);
    }

    private void resolveExtensionsWithImportMap(List<Map<String, String>> extensionsWithImportMap,
        Map<String, String> resolvedMap)
    {
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
    }

    private @NonNull List<Map<String, String>> computeExtensionsWithImportMap(List<Extension> allExtensions)
    {
        return allExtensions
            .stream()
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
                            e -> {
                                WebjarPathDescriptor descriptor = e.getValue();
                                return this.webJarsUrlFactory.url(descriptor);
                            }
                        ));
                } catch (JavascriptImportmapException e) {
                    this.logger.warn("Unable to read property [{}] for extension [{}]. Cause: [{}]",
                        JAVASCRIPT_IMPORTMAP_PROPERTY, extension.getId(), getRootCauseMessage(e));
                    extensionImportMap = Map.of();
                }
                return extensionImportMap;
            })
            .toList();
    }

    private static String accessProperty(Extension extension)
    {
        return extension.getProperty(JAVASCRIPT_IMPORTMAP_PROPERTY);
    }
}
