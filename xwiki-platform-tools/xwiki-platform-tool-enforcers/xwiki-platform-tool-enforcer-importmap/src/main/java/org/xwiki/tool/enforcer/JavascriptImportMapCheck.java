package org.xwiki.tool.enforcer;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import jakarta.inject.Named;

import org.apache.maven.enforcer.rule.api.EnforcerRuleException;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.xwiki.javascript.importmap.internal.parser.JavascriptImportmapException;
import org.xwiki.javascript.importmap.internal.parser.JavascriptImportmapParser;
import org.xwiki.webjars.WebjarDescriptor;

import static org.xwiki.javascript.importmap.internal.parser.JavascriptImportmapParser.JAVASCRIPT_IMPORTMAP_PROPERTY;

/**
 * Verify if the {@link JavascriptImportmapParser#JAVASCRIPT_IMPORTMAP_PROPERTY} property is well-formed.
 *
 * @version $Id$
 * @since 17.10.0RC1
 */
@Named("javascriptImportMapCheck")
public class JavascriptImportMapCheck extends AbstractPomCheck
{
    @Override
    public void execute() throws EnforcerRuleException
    {
        Model model = getResolvedModel();
        var property = model.getProperties().getProperty(JAVASCRIPT_IMPORTMAP_PROPERTY);
        if (property == null) {
            return;
        }
        try {
            var importMap = new JavascriptImportmapParser().parse(property);
            List<Dependency> dependencies = model.getDependencies();
            for (Map.Entry<String, WebjarDescriptor> entry : importMap.entrySet()) {
                var key = entry.getKey();
                var value = entry.getValue();
                var webjarId = value.webjarId().split(":", 2);
                getLog().debug("Checking key [%s] for webjar reference [%s]".formatted(key, value));
                var isSelf = areDependenciesEquals(model.getGroupId(), model.getArtifactId(), webjarId[0],
                    webjarId[1]);
                if (isSelf) {
                    continue;
                }
                if (dependencies.stream().noneMatch(
                    dependency -> areDependenciesEquals(dependency.getGroupId(), dependency.getArtifactId(),
                        webjarId[0], webjarId[1])))
                {
                    throw new EnforcerRuleException(
                        "Unable to find a declared dependency for [%s]".formatted(value.webjarId()));
                }
            }
        } catch (JavascriptImportmapException e) {
            throw new EnforcerRuleException(
                "Failed to parse the [%s] property".formatted(JAVASCRIPT_IMPORTMAP_PROPERTY), e);
        }
    }

    private static boolean areDependenciesEquals(String groupId0, String artifactId0, String groupId1,
        String artifactId1)
    {
        return Objects.equals(groupId0, groupId1) && Objects.equals(artifactId0, artifactId1);
    }
}
