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
package org.xwiki.extension.repository.internal.core;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Developer;
import org.apache.maven.model.License;
import org.apache.maven.model.Model;
import org.apache.maven.model.Parent;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.reflections.Reflections;
import org.reflections.scanners.ResourcesScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;
import org.reflections.util.FilterBuilder;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.extension.DefaultExtensionAuthor;
import org.xwiki.extension.DefaultExtensionDependency;
import org.xwiki.extension.Extension;
import org.xwiki.extension.ExtensionDependency;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.ExtensionLicense;
import org.xwiki.extension.ExtensionLicenseManager;
import org.xwiki.extension.ResolveException;
import org.xwiki.extension.repository.ExtensionRepositoryManager;
import org.xwiki.extension.version.internal.DefaultVersionConstraint;
import org.xwiki.properties.ConverterManager;

import com.google.common.base.Predicates;

/**
 * Scan jars to find core extensions.
 * 
 * @version $Id$
 */
@Component
@Singleton
public class DefaultCoreExtensionScanner implements CoreExtensionScanner
{
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
     * Used to parse extension id into maven informations.
     */
    private static final Pattern PARSER_ID = Pattern.compile("([^: ]+):([^: ]+)(:([^: ]+))?");

    /**
     * The logger to log.
     */
    @Inject
    private Logger logger;

    /**
     * Used to resolve found core extensions.
     */
    @Inject
    private ExtensionRepositoryManager repositoryManager;

    /**
     * Used to parse some custom properties.
     */
    @Inject
    private ConverterManager converter;

    /**
     * Used to find proper {@link ExtensionLicense}.
     */
    @Inject
    private ExtensionLicenseManager licenseManager;

    private Dependency toDependency(String id, String version, String type) throws ResolveException
    {
        Matcher matcher = PARSER_ID.matcher(id);
        if (!matcher.matches()) {
            throw new ResolveException("Bad id " + id + ", expected format is <groupId>:<artifactId>[:<classifier>]");
        }

        Dependency dependency = new Dependency();

        dependency.setGroupId(matcher.group(1));
        dependency.setArtifactId(matcher.group(2));
        if (matcher.group(4) != null) {
            dependency.setClassifier(StringUtils.defaultString(matcher.group(4), ""));
        }

        if (version != null) {
            dependency.setVersion(version);
        }

        if (type != null) {
            dependency.setType(type);
        }

        return dependency;
    }

    private String getArtifactId(DefaultCoreExtension extension) throws ResolveException
    {
        Model model = (Model) extension.getProperty(MavenCoreExtension.PKEY_MAVEN_MODEL);

        String artifactId;
        if (model != null) {
            artifactId = model.getArtifactId();
        } else {
            Matcher matcher = PARSER_ID.matcher(extension.getId().getId());
            if (!matcher.matches()) {
                throw new ResolveException("Bad id " + extension.getId().getId()
                    + ", expected format is <groupId>:<artifactId>[:<classifier>]");
            }
            artifactId = matcher.group(2);
        }

        return artifactId;
    }

    private String toExtensionId(String groupId, String artifactId, String classifier)
    {
        StringBuilder builder = new StringBuilder();

        builder.append(groupId);
        builder.append(':');
        builder.append(artifactId);
        if (StringUtils.isNotEmpty(classifier)) {
            builder.append(':');
            builder.append(classifier);
        }

        return builder.toString();
    }

    private URL getExtensionURL(URL descriptorUrl) throws MalformedURLException
    {
        String extensionURLStr = descriptorUrl.toString();
        extensionURLStr =
            extensionURLStr.substring(0, descriptorUrl.toString().indexOf(MAVENPACKAGE.replace('.', '/')));
        return new URL(extensionURLStr);
    }

    private DefaultCoreExtension parseMavenPom(URL descriptorUrl, DefaultCoreExtensionRepository repository)
        throws IOException, XmlPullParserException
    {
        DefaultCoreExtension coreExtension = null;

        InputStream descriptorStream = descriptorUrl.openStream();
        try {
            MavenXpp3Reader reader = new MavenXpp3Reader();
            Model mavenModel = reader.read(descriptorStream);

            String version = resolveVersion(mavenModel.getVersion(), mavenModel, false);
            String groupId = resolveGroupId(mavenModel.getGroupId(), mavenModel, false);

            URL extensionURL = getExtensionURL(descriptorUrl);

            coreExtension =
                new MavenCoreExtension(repository, extensionURL, new ExtensionId(groupId + ':'
                    + mavenModel.getArtifactId(), version), packagingToType(mavenModel.getPackaging()), mavenModel);

            coreExtension.setName(mavenModel.getName());
            coreExtension.setSummary(mavenModel.getDescription());
            for (Developer developer : mavenModel.getDevelopers()) {
                URL authorURL = null;
                if (developer.getUrl() != null) {
                    try {
                        authorURL = new URL(developer.getUrl());
                    } catch (MalformedURLException e) {
                        // TODO: log ?
                    }
                }

                coreExtension.addAuthor(new DefaultExtensionAuthor(developer.getId(), authorURL));
            }
            coreExtension.setWebsite(mavenModel.getUrl());

            // licenses
            for (License license : mavenModel.getLicenses()) {
                coreExtension.addLicense(getExtensionLicense(license));
            }

            // features
            String featuresString = mavenModel.getProperties().getProperty("xwiki.extension.features");
            if (StringUtils.isNotBlank(featuresString)) {
                coreExtension.setFeatures(this.converter.<Collection<String>> convert(List.class, featuresString));
            }

            // custom properties
            coreExtension.putProperty("maven.groupId", groupId);
            coreExtension.putProperty("maven.artifactId", mavenModel.getArtifactId());

            // dependencies
            for (Dependency mavenDependency : mavenModel.getDependencies()) {
                if (!mavenDependency.isOptional()
                    && (mavenDependency.getScope() == null || mavenDependency.getScope().equals("compile") || mavenDependency
                        .getScope().equals("runtime"))) {

                    String dependencyGroupId = resolveGroupId(mavenDependency.getGroupId(), mavenModel, true);
                    String dependencyArtifactId = mavenDependency.getArtifactId();
                    String dependencyClassifier = mavenDependency.getClassifier();
                    String dependencyVersion = resolveVersion(mavenDependency.getVersion(), mavenModel, true);

                    DefaultExtensionDependency extensionDependency =
                        new MavenCoreExtensionDependency(toExtensionId(dependencyGroupId, dependencyArtifactId,
                            dependencyClassifier), new DefaultVersionConstraint(dependencyVersion), mavenDependency);

                    coreExtension.addDependency(extensionDependency);
                }
            }
        } finally {
            IOUtils.closeQuietly(descriptorStream);
        }

        return coreExtension;
    }

    @Override
    public void updateExtensions(Collection<DefaultCoreExtension> extensions)
    {
        for (DefaultCoreExtension extension : extensions) {
            try {
                Extension remoteExtension = this.repositoryManager.resolve(extension.getId());

                extension.set(remoteExtension);
            } catch (ResolveException e) {
                this.logger.debug("Can't find remote extension with id [" + extension.getId() + "]", e);
            }
        }
    }

    @Override
    public Map<String, DefaultCoreExtension> loadExtensions(DefaultCoreExtensionRepository repository)
    {
        Set<URL> mavenURLs = ClasspathHelper.forPackage(MAVENPACKAGE);

        ConfigurationBuilder configurationBuilder = new ConfigurationBuilder();
        configurationBuilder.setScanners(new ResourcesScanner());
        configurationBuilder.setUrls(mavenURLs);
        configurationBuilder.filterInputsBy(new FilterBuilder.Include(FilterBuilder.prefix(MAVENPACKAGE)));

        Reflections reflections = new Reflections(configurationBuilder);

        Set<String> descriptors = reflections.getResources(Predicates.equalTo("pom.xml"));

        Map<String, DefaultCoreExtension> extensions = new HashMap<String, DefaultCoreExtension>(descriptors.size());

        for (String descriptor : descriptors) {
            URL descriptorUrl = getClass().getClassLoader().getResource(descriptor);

            try {
                DefaultCoreExtension coreExtension = parseMavenPom(descriptorUrl, repository);

                extensions.put(coreExtension.getId().getId(), coreExtension);
            } catch (Exception e) {
                this.logger.warn("Failed to pase extension descriptor [" + descriptorUrl + "]", e);
            }
        }

        // Try to find more

        guess(extensions, repository);

        return extensions;
    }

    private void guess(Map<String, DefaultCoreExtension> extensions, DefaultCoreExtensionRepository repository)
    {
        Set<ExtensionDependency> dependencies = new HashSet<ExtensionDependency>();

        for (DefaultCoreExtension coreExtension : extensions.values()) {
            for (ExtensionDependency dependency : coreExtension.getDependencies()) {
                dependencies.add(dependency);
            }
        }

        // Normalize and guess

        Map<String, Object[]> fileNames = new HashMap<String, Object[]>();
        Map<String, Object[]> guessedArtefacts = new HashMap<String, Object[]>();
        Set<URL> urls = ClasspathHelper.forClassLoader();

        for (URL url : urls) {
            try {
                String path = url.toURI().getPath();
                String filename = path.substring(path.lastIndexOf('/') + 1);
                String type = null;

                int extIndex = filename.lastIndexOf('.');
                if (extIndex != -1) {
                    type = filename.substring(extIndex + 1);
                    filename = filename.substring(0, extIndex);
                }

                int index;
                if (!filename.endsWith(SNAPSHOTSUFFIX)) {
                    index = filename.lastIndexOf('-');
                } else {
                    index = filename.lastIndexOf('-', filename.length() - SNAPSHOTSUFFIX.length());
                }

                if (index != -1) {
                    fileNames.put(filename, new Object[] {url});

                    String artefactname = filename.substring(0, index);
                    String version = filename.substring(index + 1);

                    guessedArtefacts.put(artefactname, new Object[] {version, url, type});
                }
            } catch (Exception e) {
                this.logger.warn("Failed to parse resource name [" + url + "]", e);
            }
        }

        // Try to resolve version no easy to find from the pom.xml
        try {
            for (DefaultCoreExtension coreExtension : extensions.values()) {
                String artifactId = getArtifactId(coreExtension);

                Object[] artefact = guessedArtefacts.get(artifactId);

                if (artefact != null) {
                    if (coreExtension.getId().getVersion().getValue().charAt(0) == '$') {
                        coreExtension.setId(new ExtensionId(coreExtension.getId().getId(), (String) artefact[0]));
                        coreExtension.setGuessed(true);
                    }

                    if (coreExtension.getType().charAt(0) == '$') {
                        coreExtension.setType((String) artefact[2]);
                        coreExtension.setGuessed(true);
                    }
                }
            }

            // Add dependencies that does not provide proper pom.xml resource and can't be found in the classpath
            for (ExtensionDependency extensionDependency : dependencies) {
                Dependency dependency =
                    (Dependency) extensionDependency.getProperty(MavenCoreExtensionDependency.PKEY_MAVEN_DEPENDENCY);

                if (dependency == null) {
                    dependency =
                        toDependency(extensionDependency.getId(),
                            extensionDependency.getVersionConstraint().getValue(), null);
                }

                String dependencyId = dependency.getGroupId() + ':' + dependency.getArtifactId();

                DefaultCoreExtension coreExtension = extensions.get(dependencyId);
                if (coreExtension == null) {
                    String dependencyFileName = dependency.getArtifactId() + '-' + dependency.getVersion();
                    if (dependency.getClassifier() != null) {
                        dependencyFileName += '-' + dependency.getClassifier();
                        dependencyId += ':' + dependency.getClassifier();
                    }

                    Object[] filenameArtifact = fileNames.get(dependencyFileName);
                    Object[] guessedArtefact = guessedArtefacts.get(dependency.getArtifactId());

                    if (filenameArtifact != null) {
                        coreExtension =
                            new DefaultCoreExtension(repository, (URL) filenameArtifact[0], new ExtensionId(
                                dependencyId, dependency.getVersion()), packagingToType(dependency.getType()));
                        coreExtension.setGuessed(true);
                    } else if (guessedArtefact != null) {
                        coreExtension =
                            new DefaultCoreExtension(repository, (URL) guessedArtefact[1], new ExtensionId(
                                dependencyId, (String) guessedArtefact[0]), packagingToType(dependency.getType()));
                        coreExtension.setGuessed(true);
                    }

                    if (coreExtension != null) {
                        extensions.put(dependencyId, coreExtension);
                    }
                }
            }
        } catch (Exception e) {
            this.logger.warn("Failed to guess extra information about some extensions", e);
        }
    }

    private String resolveVersion(String modelVersion, Model mavenModel, boolean dependency)
    {
        String version = modelVersion;

        // TODO: download parents and resolve pom.xml properties using aether ? could be pretty expensive for
        // the init
        if (version == null) {
            if (!dependency) {
                Parent parent = mavenModel.getParent();

                if (parent != null) {
                    version = parent.getVersion();
                }
            }
        } else if (version.startsWith("$")) {
            String propertyName = version.substring(2, version.length() - 1);

            if (propertyName.equals("project.version") || propertyName.equals("pom.version")
                || propertyName.equals("version")) {
                version = resolveVersion(mavenModel.getVersion(), mavenModel, false);
            } else {
                String value = mavenModel.getProperties().getProperty(propertyName);
                if (value != null) {
                    version = value;
                }
            }
        }

        if (version == null) {
            version = UNKNOWN;
        }

        return version;
    }

    private String resolveGroupId(String modelGroupId, Model mavenModel, boolean dependency)
    {
        String groupId = modelGroupId;

        // TODO: download parents and resolve pom.xml properties using aether ? could be pretty expensive for
        // the init
        if (groupId == null) {
            if (!dependency) {
                Parent parent = mavenModel.getParent();

                if (parent != null) {
                    groupId = parent.getGroupId();
                }
            }
        } else if (groupId.startsWith("$")) {
            String propertyName = groupId.substring(2, groupId.length() - 1);

            String value = mavenModel.getProperties().getProperty(propertyName);
            if (value != null) {
                groupId = value;
            }
        }

        if (groupId == null) {
            groupId = UNKNOWN;
        }

        return groupId;
    }

    // TODO: download custom licenses content
    private ExtensionLicense getExtensionLicense(License license)
    {
        if (license.getName() == null) {
            return new ExtensionLicense("noname", null);
        }

        ExtensionLicense extensionLicense = this.licenseManager.getLicense(license.getName());

        return extensionLicense != null ? extensionLicense : new ExtensionLicense(license.getName(), null);
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
