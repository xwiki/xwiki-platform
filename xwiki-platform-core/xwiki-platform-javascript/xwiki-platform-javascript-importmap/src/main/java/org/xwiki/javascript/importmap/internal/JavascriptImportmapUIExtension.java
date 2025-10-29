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

        Map<String, String> pouet = new HashMap<>();
        for (Map<String, String> objectObjectMap : extensionsWithImportMap) {
            for (Map.Entry<String, String> objectObjectEntry : objectObjectMap.entrySet()) {
                // TODO: merge resolutions + raise warning in case of conflict.
            }
        }

        String json = extensionsWithImportMap.toString();
        try {
            json = OBJECT_MAPPER.writeValueAsString(Map.of());
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
