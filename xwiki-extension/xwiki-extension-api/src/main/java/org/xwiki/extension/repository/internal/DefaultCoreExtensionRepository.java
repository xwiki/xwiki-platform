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
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
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
import org.xwiki.component.annotation.Component;
import org.xwiki.component.logging.AbstractLogEnabled;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.extension.CoreExtension;
import org.xwiki.extension.Extension;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.ResolveException;
import org.xwiki.extension.repository.CoreExtensionRepository;
import org.xwiki.extension.repository.ExtensionRepositoryId;

import com.google.common.base.Predicates;

@Component
public class DefaultCoreExtensionRepository extends AbstractLogEnabled implements CoreExtensionRepository,
    Initializable
{
    public static final String COMPONENT_OVERRIDE_LIST = "META-INF/pom.xml";

    private ExtensionRepositoryId repositoryId;

    protected Map<String, CoreExtension> extensions;

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.component.phase.Initializable#initialize()
     */
    public void initialize() throws InitializationException
    {
        this.repositoryId = new ExtensionRepositoryId("core", "xwiki-core", null);

        loadExtensions();
    }

    private void loadExtensions()
    {
        Set<URL> basURLs = ClasspathHelper.getUrlsForPackagePrefix("META-INF.maven");

        basURLs = filterURLs(basURLs);

        ConfigurationBuilder configurationBuilder = new ConfigurationBuilder();
        configurationBuilder.setScanners(new ResourcesScanner());
        configurationBuilder.setUrls(basURLs);
        configurationBuilder.filterInputsBy(new FilterBuilder.Include(FilterBuilder.prefix("META-INF.maven")));

        Reflections reflections = new Reflections(configurationBuilder);

        Set<String> descriptors = reflections.getResources(Predicates.equalTo("pom.xml"));

        this.extensions = new LinkedHashMap<String, CoreExtension>(descriptors.size());

        List<Dependency> dependencies = new ArrayList<Dependency>();

        for (String descriptor : descriptors) {
            URL descriptorUrl = getClass().getClassLoader().getResource(descriptor);

            // TODO: extract jar URL from descriptorUrl

            InputStream descriptorStream = getClass().getClassLoader().getResourceAsStream(descriptor);
            try {
                MavenXpp3Reader reader = new MavenXpp3Reader();
                Model mavenModel = reader.read(descriptorStream);

                String version = mavenModel.getVersion();
                String groupId = mavenModel.getGroupId();

                // TODO: add support for properties
                // TODO: add support for parents
                if (version == null || groupId == null) {
                    Parent parent = mavenModel.getParent();

                    if (groupId == null) {
                        groupId = parent.getGroupId();
                    }

                    if (version == null) {
                        version = parent.getVersion();
                    }

                    if (version == null) {
                        version = "unknown";
                    }
                    if (groupId == null) {
                        groupId = "unknown";
                    }
                }

                DefaultCoreExtension coreExtension =
                    new DefaultCoreExtension(this, ClasspathHelper.getBaseUrl(descriptorUrl, basURLs), groupId + ":"
                        + mavenModel.getArtifactId(), version, packagingToType(mavenModel.getPackaging()));

                this.extensions.put(coreExtension.getId(), coreExtension);

                for (Dependency dependency : mavenModel.getDependencies()) {
                    if (dependency.getGroupId().equals("${project.groupId}")) {
                        dependency.setGroupId(groupId);
                    }
                    if (dependency.getVersion() == null) {
                        dependency.setVersion("unknown");
                    } else if (dependency.getVersion().equals("${project.version}")
                        || dependency.getVersion().equals("${pom.version}")) {
                        dependency.setVersion(version);
                    }
                    dependencies.add(dependency);
                }
            } catch (Exception e) {
                getLogger().error("Failed to parse descriptor [" + descriptorUrl + "]", e);
            } finally {
                try {
                    descriptorStream.close();
                } catch (IOException e) {
                    // Should not happen
                    getLogger().error("Failed to close descriptor stream [" + descriptorUrl + "]", e);
                }
            }

            // Add dependencies that does not provide proper pom.xml resource and can't be found in the classpath
            for (Dependency dependency : dependencies) {
                String dependencyId = dependency.getGroupId() + ":" + dependency.getArtifactId();
                if (!this.extensions.containsKey(dependencyId)) {
                    CoreExtension coreExtension =
                        new DefaultCoreExtension(this, ClasspathHelper.getBaseUrl(descriptorUrl, basURLs), dependencyId, dependency.getVersion(),
                            packagingToType(dependency.getType()));

                    this.extensions.put(dependencyId, coreExtension);
                }
            }
        }
    }

    private Set<URL> filterURLs(Set<URL> urls)
    {
        Set<URL> results = new HashSet<URL>(urls.size());
        for (URL url : urls) {
            String cleanURL = url.toString();
            // Fix JBoss URLs
            if (url.getProtocol().startsWith("vfszip:")) {
                cleanURL = cleanURL.replaceFirst("vfszip:", "file:");
            } else if (url.getProtocol().startsWith("vfsfile:")) {
                cleanURL = cleanURL.replaceFirst("vfsfile:", "file:");
            }
            cleanURL = cleanURL.replaceFirst("\\.jar/", ".jar!/");
            try {
                results.add(new URL(cleanURL));
            } catch (MalformedURLException ex) {
                // Shouldn't happen, but we can't do more to fix this URL.
            }
        }
        return results;
    }

    private String packagingToType(String packaging)
    {
        // support bundle packaging
        if (packaging.equals("bundle")) {
            return "jar";
        }

        return packaging;
    }

    // Repository

    public Extension resolve(ExtensionId extensionId) throws ResolveException
    {
        Extension extension = getCoreExtension(extensionId.getId());

        if (extension == null
            || (extensionId.getVersion() != null && !extension.getVersion().equals(extensionId.getVersion()))) {
            throw new ResolveException("Could not find extension [" + extensionId + "]");
        }

        return extension;
    }

    public boolean exists(ExtensionId extensionId)
    {
        Extension extension = getCoreExtension(extensionId.getId());

        if (extension == null
            || (extensionId.getVersion() != null && !extension.getVersion().equals(extensionId.getVersion()))) {
            return false;
        }

        return true;
    }

    public boolean exists(String id)
    {
        return this.extensions.containsKey(id);
    }

    public ExtensionRepositoryId getId()
    {
        return this.repositoryId;
    }

    // LocalRepository

    public int countExtensions()
    {
        return this.extensions.size();
    }

    public List<CoreExtension> getCoreExtensions()
    {
        return new ArrayList<CoreExtension>(this.extensions.values());
    }

    public CoreExtension getCoreExtension(String id)
    {
        return this.extensions.get(id);
    }

    public List< ? extends CoreExtension> getExtensions(int nb, int offset)
    {
        return getCoreExtensions().subList(offset, offset + nb);
    }
}
