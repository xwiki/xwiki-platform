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
package org.xwiki.extension.repository.internal;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.reflections.Reflections;
import org.reflections.scanners.ResourcesScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.FilterBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.extension.ExtensionId;

import com.google.common.base.Predicates;

/**
 * Scan jars to find core extensions.
 * 
 * @version $Id$
 */
public class DefaultCoreExtensionScanner
{
    /**
     * Logging tool.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultCoreExtensionScanner.class);

    /**
     * The package containing maven informations in a jar file.
     */
    private static final String MAVENPACKAGE = "META-INF.maven";

    /**
     * Unknown.
     */
    private static final String UNKNOWN = "unknown";

    /**
     * SNAPSHOT suffix in versions.
     */
    private static final String SNAPSHOTSUFFIX = "-SNAPSHOT";

    /**
     * Scan classpath to find core extensions.
     * 
     * @param repository the repository where to store found extensions
     * @return scan jar files in classpath to find core extensions
     */
    public Map<String, DefaultCoreExtension> loadExtensions(DefaultCoreExtensionRepository repository)
    {
        Set<URL> baseURLs = ClasspathHelper.forPackage(MAVENPACKAGE);

        // FIXME: remove that as soon as possible
        baseURLs = filterURLs(baseURLs);

        ConfigurationBuilder configurationBuilder = new ConfigurationBuilder();
        configurationBuilder.setScanners(new ResourcesScanner());
        configurationBuilder.setUrls(baseURLs);
        configurationBuilder.filterInputsBy(new FilterBuilder.Include(FilterBuilder.prefix(MAVENPACKAGE)));

        Reflections reflections = new Reflections(configurationBuilder);

        Set<String> descriptors = reflections.getResources(Predicates.equalTo("pom.xml"));

        Map<String, DefaultCoreExtension> extensions = new HashMap<String, DefaultCoreExtension>(descriptors.size());

        List<Dependency> dependencies = new ArrayList<Dependency>();
        List<Object[]> coreArtefactIds = new ArrayList<Object[]>();

        for (String descriptor : descriptors) {
            URL descriptorUrl = getClass().getClassLoader().getResource(descriptor);

            InputStream descriptorStream = getClass().getClassLoader().getResourceAsStream(descriptor);
            try {
                MavenXpp3Reader reader = new MavenXpp3Reader();
                Model mavenModel = reader.read(descriptorStream);

                Properties properties = mavenModel.getProperties();
                String version = mavenModel.getVersion();
                String groupId = mavenModel.getGroupId();

                // TODO: download parents and resolve pom.xml properties using aether
                if (version == null || groupId == null) {
                    Parent parent = mavenModel.getParent();

                    if (groupId == null) {
                        groupId = parent.getGroupId();
                    }

                    if (version == null) {
                        version = parent.getVersion();
                    }

                    if (version == null) {
                        version = UNKNOWN;
                    }
                    if (groupId == null) {
                        groupId = UNKNOWN;
                    }
                } else {
                    if (version.startsWith("$")) {
                        version = properties.getProperty(version.substring(2, version.length() - 1));
                    }
                }

                DefaultCoreExtension coreExtension =
                    new DefaultCoreExtension(repository, descriptorUrl, new ExtensionId(groupId + ':'
                        + mavenModel.getArtifactId(), version), packagingToType(mavenModel.getPackaging()));

                extensions.put(coreExtension.getId().getId(), coreExtension);
                coreArtefactIds.add(new Object[] {mavenModel.getArtifactId(), coreExtension});

                for (Dependency dependency : mavenModel.getDependencies()) {
                    if (dependency.getGroupId().equals("${project.groupId}")) {
                        dependency.setGroupId(groupId);
                    }
                    if (dependency.getVersion() == null) {
                        dependency.setVersion(UNKNOWN);
                    } else if (dependency.getVersion().equals("${project.version}")
                        || dependency.getVersion().equals("${pom.version}")) {
                        dependency.setVersion(version);
                    }
                    dependencies.add(dependency);
                }
            } catch (Exception e) {
                LOGGER.warn("Failed to parse descriptor [" + descriptorUrl
                    + "], it will be ignored and not found in core extensions.", e);
            } finally {
                try {
                    descriptorStream.close();
                } catch (IOException e) {
                    // Should not happen
                    LOGGER.error("Failed to close descriptor stream [" + descriptorUrl + "]", e);
                }
            }

            // Normalize and guess

            Map<String, Object[]> artefacts = new HashMap<String, Object[]>();
            Set<URL> urls = ClasspathHelper.forClassLoader();

            for (URL url : urls) {
                try {
                    String path = url.toURI().getPath();
                    String filename = path.substring(path.lastIndexOf('/') + 1);

                    int extIndex = filename.lastIndexOf('.');
                    if (extIndex != -1) {
                        filename = filename.substring(0, extIndex);
                    }

                    int index;
                    if (!filename.endsWith(SNAPSHOTSUFFIX)) {
                        index = filename.lastIndexOf('-');
                    } else {
                        index = filename.lastIndexOf('-', filename.length() - SNAPSHOTSUFFIX.length());
                    }

                    if (index != -1) {
                        String artefactname = filename.substring(0, index);
                        String version = filename.substring(index + 1);

                        artefacts.put(artefactname, new Object[] {version, url});
                    }
                } catch (Exception e) {
                    LOGGER.warn("Failed to parse resource name [" + url + "]", e);
                }
            }

            // Try to resolve version no easy to find from the pom.xml
            try {
                for (Object[] coreArtefactId : coreArtefactIds) {
                    Object[] artefact = artefacts.get(coreArtefactId[0]);

                    DefaultCoreExtension coreExtension = (DefaultCoreExtension) coreArtefactId[1];
                    if (artefact != null && coreExtension.getId().getVersion().charAt(0) == '$') {
                        coreExtension.setId(new ExtensionId(coreExtension.getId().getId(), (String) artefact[0]));
                        coreExtension.setGuessed(true);
                    }
                }

                // Add dependencies that does not provide proper pom.xml resource and can't be found in the classpath
                for (Dependency dependency : dependencies) {
                    String dependencyId = dependency.getGroupId() + ':' + dependency.getArtifactId();

                    Object[] artefact = artefacts.get(dependency.getArtifactId());
                    if (artefact != null) {
                        DefaultCoreExtension coreExtension = extensions.get(dependencyId);
                        if (coreExtension == null) {
                            coreExtension =
                                new DefaultCoreExtension(repository, (URL) artefact[1], new ExtensionId(dependencyId,
                                    (String) artefact[0]), packagingToType(dependency.getType()));
                            coreExtension.setGuessed(true);

                            extensions.put(dependencyId, coreExtension);
                        }
                    }
                }
            } catch (Exception e) {
                LOGGER.warn("Failed to guess extra information about some extensions", e);
            }
        }

        return extensions;
    }

    /**
     * Very nasty hack which unescape invalid characters from the {@link URL} path because
     * {@link Reflections#Reflections(org.reflections.Configuration)} does not do it...
     * 
     * @param urls base URLs to modify
     * @return modified base URLs
     */
    // TODO: remove when http://code.google.com/p/reflections/issues/detail?id=87 is fixed
    private Set<URL> filterURLs(Set<URL> urls)
    {
        Set<URL> results = new HashSet<URL>(urls.size());
        for (URL url : urls) {
            try {
                results.add(new URL(url.getProtocol(), url.getHost(), url.getPort(), URLDecoder.decode(url.getFile(),
                    "UTF-8")));
            } catch (Exception e) {
                LOGGER.error("Failed to convert url [" + url + "]", e);

                results.add(url);
            }
        }

        return results;
    }

    /**
     * Get the extension type from maven packaging.
     * 
     * @param packaging the maven packaging
     * @return the extension type
     */
    private String packagingToType(String packaging)
    {
        // support bundle packaging
        if (packaging.equals("bundle")) {
            return "jar";
        }

        return packaging;
    }
}
