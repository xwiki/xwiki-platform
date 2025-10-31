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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.extension.repository.CoreExtensionRepository;
import org.xwiki.extension.repository.InstalledExtensionRepository;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.block.RawBlock;
import org.xwiki.uiextension.UIExtension;
import org.xwiki.webjars.WebJarsUrlFactory;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCauseMessage;
import static org.xwiki.rendering.syntax.Syntax.HTML_5_0;

/**
 * Resolve the importmap from the webjar declarations, using the {@code "xwiki.javascript.modules.importmap"} pom
 * property. The resolved mapping in injected in the page headers with the {@code "org.xwiki.platform.html.head"}
 * extension point.
 *
 * @version $Id$
 * @since 17.10.0RC1
 */
@Component
@Singleton
@Named("JavascriptImportmap")
public class JavascriptImportmapUIExtension implements UIExtension
{
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final String JAVASCRIPT_IMPORTMAP_PROPERTY = "xwiki.javascript.modules.importmap";

    @Inject
    private Logger logger;

    @Inject
    private InstalledExtensionRepository installedExtensionRepository;

    @Inject
    private CoreExtensionRepository coreExtensionRepository;

    @Inject
    private WebJarsUrlFactory webJarsUrlFactory;

    @Override
    public String getId()
    {
        return "org.xwiki.platform.javascript.importmap.html.head";
    }

    @Override
    public String getExtensionPointId()
    {
        return "org.xwiki.platform.html.head";
    }

    @Override
    public Map<String, String> getParameters()
    {
        return Map.of("order", "1000");
    }

    @Override
    public Block execute()
    {
        List<Map<String, String>> extensionsWithImportMap = Stream.concat(
                this.installedExtensionRepository.getInstalledExtensions().stream(),
                this.coreExtensionRepository.getCoreExtensions().stream())
            .filter(extension -> extension.getProperty(JAVASCRIPT_IMPORTMAP_PROPERTY) != null)
            .map(extension -> {
                String importMapJSON = extension.getProperty(JAVASCRIPT_IMPORTMAP_PROPERTY);
                Map<String, String> extensionImportMap;
                try {
                    Object parsedJSON = OBJECT_MAPPER.readValue(importMapJSON, Object.class);
                    extensionImportMap = new LinkedHashMap<>();
                    if (parsedJSON instanceof Map parsedJSONMap) {
                        for (Map.Entry o : (Set<Map.Entry>) parsedJSONMap.entrySet()) {
                            extensionImportMap.put(String.valueOf(o.getKey()),
                                parseValue(String.valueOf(o.getValue())));
                        }
                    }
                } catch (JsonProcessingException e) {
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
            json = OBJECT_MAPPER.writeValueAsString(resolvedMap);
        } catch (JsonProcessingException e) {
            this.logger.warn("Failed to serialize the importmap. Cause: [{}]", getRootCauseMessage(e));
            json = "{}";
        }

        return new RawBlock("<script type='importmap'>%s</script>".formatted(json), HTML_5_0);
    }

    private String parseValue(String value)
    {
        var slashSplit = value.split("/", 2);
        return this.webJarsUrlFactory.url(slashSplit[0], slashSplit[1]);
    }
}
