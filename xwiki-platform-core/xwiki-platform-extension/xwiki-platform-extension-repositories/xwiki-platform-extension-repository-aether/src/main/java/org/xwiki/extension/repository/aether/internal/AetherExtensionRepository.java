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
package org.xwiki.extension.repository.aether.internal;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.model.Developer;
import org.apache.maven.model.License;
import org.apache.maven.model.Model;
import org.codehaus.plexus.component.repository.exception.ComponentLookupException;
import org.sonatype.aether.RepositorySystemSession;
import org.sonatype.aether.artifact.Artifact;
import org.sonatype.aether.artifact.ArtifactTypeRegistry;
import org.sonatype.aether.graph.Dependency;
import org.sonatype.aether.impl.ArtifactDescriptorReader;
import org.sonatype.aether.impl.VersionRangeResolver;
import org.sonatype.aether.repository.RemoteRepository;
import org.sonatype.aether.resolution.ArtifactDescriptorRequest;
import org.sonatype.aether.resolution.ArtifactDescriptorResult;
import org.sonatype.aether.resolution.VersionRangeRequest;
import org.sonatype.aether.resolution.VersionRangeResolutionException;
import org.sonatype.aether.resolution.VersionRangeResult;
import org.sonatype.aether.util.artifact.DefaultArtifact;
import org.sonatype.aether.util.version.GenericVersionScheme;
import org.sonatype.aether.version.InvalidVersionSpecificationException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.extension.DefaultExtensionAuthor;
import org.xwiki.extension.DefaultExtensionDependency;
import org.xwiki.extension.Extension;
import org.xwiki.extension.ExtensionDependency;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.ExtensionLicense;
import org.xwiki.extension.ExtensionLicenseManager;
import org.xwiki.extension.ResolveException;
import org.xwiki.extension.repository.AbstractExtensionRepository;
import org.xwiki.extension.repository.ExtensionRepositoryId;
import org.xwiki.extension.repository.aether.internal.plexus.PlexusComponentManager;
import org.xwiki.extension.repository.result.CollectionIterableResult;
import org.xwiki.extension.repository.result.IterableResult;
import org.xwiki.extension.version.Version;
import org.xwiki.extension.version.VersionConstraint;
import org.xwiki.extension.version.VersionRange;
import org.xwiki.extension.version.internal.DefaultVersion;
import org.xwiki.extension.version.internal.DefaultVersionConstraint;
import org.xwiki.properties.ConverterManager;

public class AetherExtensionRepository extends AbstractExtensionRepository
{
    public static final String MPKEYPREFIX = "xwiki.extension.";

    public static final String MPKEY_FEATURES = MPKEYPREFIX + "features";

    /**
     * Used to parse the version.
     */
    private static final GenericVersionScheme AETHERVERSIONSCHEME = new GenericVersionScheme();

    private ComponentManager componentManager;

    private PlexusComponentManager plexusComponentManager;

    private RemoteRepository remoteRepository;

    private ArtifactDescriptorReader mavenDescriptorReader;

    private VersionRangeResolver versionRangeResolver;

    private static Method loadPomMethod;

    private static Method convertMethod;

    private ConverterManager converter;

    private ExtensionLicenseManager licenseManager;

    private AetherExtensionRepositoryFactory repositoryFactory;

    public AetherExtensionRepository(ExtensionRepositoryId repositoryId,
        AetherExtensionRepositoryFactory repositoryFactory, PlexusComponentManager mavenComponentManager,
        ComponentManager componentManager) throws Exception
    {
        super(repositoryId);

        this.repositoryFactory = repositoryFactory;
        this.componentManager = componentManager;
        this.plexusComponentManager = mavenComponentManager;

        this.remoteRepository = new RemoteRepository(repositoryId.getId(), "default", repositoryId.getURI().toString());

        this.converter = this.componentManager.lookup(ConverterManager.class);
        this.licenseManager = this.componentManager.lookup(ExtensionLicenseManager.class);

        this.versionRangeResolver = this.plexusComponentManager.getPlexus().lookup(VersionRangeResolver.class);

        try {
            this.mavenDescriptorReader = this.plexusComponentManager.getPlexus().lookup(ArtifactDescriptorReader.class);

            if (loadPomMethod == null) {
                // FIXME: not very nice
                // * use a private method of a library we don't control is not the nicest thing. But it's a big and very
                // uselfull method. A shame is not a bit more public.
                // * having to parse the pom.xml since we are supposed to support anything supported by aether is not
                // very
                // clean
                // either. But Aether almost resolve nothing, not even the type of the artifact, we pretty much get only
                // dependencies and licenses.
                loadPomMethod =
                    this.mavenDescriptorReader.getClass().getDeclaredMethod("loadPom", RepositorySystemSession.class,
                        ArtifactDescriptorRequest.class, ArtifactDescriptorResult.class);
                loadPomMethod.setAccessible(true);
                convertMethod =
                    this.mavenDescriptorReader.getClass().getDeclaredMethod("convert",
                        org.apache.maven.model.Dependency.class, ArtifactTypeRegistry.class);
                convertMethod.setAccessible(true);
            }
        } catch (ComponentLookupException e) {
            // Maven handler not found
        }
    }

    public RepositorySystemSession createRepositorySystemSession()
    {
        return this.repositoryFactory.createRepositorySystemSession();
    }

    @Override
    public Extension resolve(ExtensionId extensionId) throws ResolveException
    {
        return resolve(new DefaultExtensionDependency(extensionId.getId(), new DefaultVersionConstraint(null,
            extensionId.getVersion())));
    }

    @Override
    public Extension resolve(ExtensionDependency extensionDependency) throws ResolveException
    {
        if (getId().getType().equals("maven") && this.mavenDescriptorReader != null) {
            return resolveMaven(extensionDependency);
        } else {
            // FIXME: impossible to resolve extension type as well as most of the information with pure Aether API
            throw new ResolveException("Unsupported");
        }
    }

    @Override
    public IterableResult<Version> resolveVersions(String id, int offset, int nb) throws ResolveException
    {
        Artifact artifact = AetherUtils.createArtifact(id, "(,)");

        List<org.sonatype.aether.version.Version> versions;
        try {
            versions = resolveVersions(artifact, createRepositorySystemSession());

            if (versions.isEmpty()) {
                throw new ResolveException("No versions available for id [" + id + "]");
            }
        } catch (VersionRangeResolutionException e) {
            throw new ResolveException("Failed to resolve versions for id [" + id + "]", e);
        }

        if (nb == 0 || offset >= versions.size()) {
            return new CollectionIterableResult<Version>(versions.size(), offset, Collections.<Version> emptyList());
        }

        int fromId = offset < 0 ? 0 : offset;
        int toId = offset + nb > versions.size() || nb < 0 ? versions.size() - 1 : offset + nb;

        List<Version> result = new ArrayList<Version>(toId - fromId);
        for (int i = fromId; i < toId; ++i) {
            result.add(new DefaultVersion(versions.get(i).toString()));
        }

        return new CollectionIterableResult<Version>(versions.size(), offset, result);
    }

    private org.sonatype.aether.version.Version resolveVersionConstraint(String id,
        VersionConstraint versionConstraint, RepositorySystemSession session) throws ResolveException
    {
        if (versionConstraint.getVersion() != null) {
            try {
                return AETHERVERSIONSCHEME.parseVersion(versionConstraint.getVersion().getValue());
            } catch (InvalidVersionSpecificationException e) {
                throw new ResolveException("Invalid version [" + versionConstraint.getVersion() + "]", e);
            }
        }

        List<org.sonatype.aether.version.Version> commonVersions = null;

        for (VersionRange range : versionConstraint.getRanges()) {
            List<org.sonatype.aether.version.Version> versions = resolveVersionRange(id, range, session);

            if (commonVersions == null) {
                commonVersions =
                    versionConstraint.getRanges().size() > 1 ? new ArrayList<org.sonatype.aether.version.Version>(
                        versions) : versions;
            } else {
                // Find commons versions between all the ranges of the constraint
                for (Iterator<org.sonatype.aether.version.Version> it = commonVersions.iterator(); it.hasNext();) {
                    org.sonatype.aether.version.Version version = it.next();
                    if (!versions.contains(version)) {
                        it.remove();
                    }
                }
            }
        }

        if (commonVersions.isEmpty()) {
            throw new ResolveException("No versions available for id [" + id + "] and version constraint ["
                + versionConstraint + "]");
        }

        return commonVersions.get(commonVersions.size() - 1);
    }

    private org.sonatype.aether.version.Version resolveVersion(Artifact artifact, RepositorySystemSession session)
        throws ResolveException
    {
        try {
            List<org.sonatype.aether.version.Version> versions = resolveVersions(artifact, session);

            if (versions.isEmpty()) {
                throw new ResolveException("No versions available for artifact [" + artifact + "]");
            }

            return versions.get(versions.size() - 1);
        } catch (VersionRangeResolutionException e) {
            throw new ResolveException("Failed to resolve version range", e);
        }
    }

    private List<org.sonatype.aether.version.Version> resolveVersionRange(String id, VersionRange versionRange,
        RepositorySystemSession session) throws ResolveException
    {
        Artifact artifact = AetherUtils.createArtifact(id, versionRange.getValue());

        try {
            List<org.sonatype.aether.version.Version> versions = resolveVersions(artifact, session);

            if (versions.isEmpty()) {
                throw new ResolveException("No versions available for id [" + id + "] and version range ["
                    + versionRange + "]");
            }

            return versions;
        } catch (VersionRangeResolutionException e) {
            throw new ResolveException("Failed to resolve version range", e);
        }
    }

    private List<org.sonatype.aether.version.Version> resolveVersions(Artifact artifact, RepositorySystemSession session)
        throws VersionRangeResolutionException
    {
        VersionRangeRequest rangeRequest = new VersionRangeRequest();
        rangeRequest.setArtifact(artifact);
        rangeRequest.addRepository(this.remoteRepository);

        VersionRangeResult rangeResult = this.versionRangeResolver.resolveVersionRange(session, rangeRequest);

        return rangeResult.getVersions();
    }

    public AetherExtension resolveMaven(ExtensionDependency extensionDependency) throws ResolveException
    {
        RepositorySystemSession session = createRepositorySystemSession();

        // Get artifact and resolve version

        Artifact artifact;
        if (extensionDependency instanceof AetherExtensionDependency) {
            artifact = ((AetherExtensionDependency) extensionDependency).getAetherDependency().getArtifact();
            artifact = artifact.setVersion(resolveVersion(artifact, session).toString());
        } else {
            artifact =
                AetherUtils.createArtifact(extensionDependency.getId(), extensionDependency.getVersionConstraint()
                    .getValue());
            artifact =
                artifact.setVersion(resolveVersionConstraint(extensionDependency.getId(),
                    extensionDependency.getVersionConstraint(), session).toString());
        }

        // Get Maven descriptor

        Model model;
        try {
            model = loadPom(artifact, session);
        } catch (Exception e) {
            throw new ResolveException("Failed to resolve extension [" + extensionDependency + "] descriptor", e);
        }

        // Set type

        String artifactExtension;
        if (extensionDependency instanceof AetherExtensionDependency) {
            artifactExtension =
                ((AetherExtensionDependency) extensionDependency).getAetherDependency().getArtifact().getExtension();
        } else {
            // See bundle as jar packages since bundle are actually store as jar files
            artifactExtension = model.getPackaging().equals("bundle") ? "jar" : model.getPackaging();
        }

        artifact =
            new DefaultArtifact(artifact.getGroupId(), artifact.getArtifactId(), artifact.getClassifier(),
                artifactExtension, artifact.getVersion());

        AetherExtension extension = new AetherExtension(artifact, model, this, this.plexusComponentManager);

        extension.setName(model.getName());
        extension.setSummary(model.getDescription());
        for (Developer developer : model.getDevelopers()) {
            URL authorURL = null;
            if (developer.getUrl() != null) {
                try {
                    authorURL = new URL(developer.getUrl());
                } catch (MalformedURLException e) {
                    // TODO: log ?
                }
            }

            extension.addAuthor(new DefaultExtensionAuthor(StringUtils.defaultIfBlank(developer.getName(),
                developer.getId()), authorURL));
        }
        extension.setWebsite(model.getUrl());

        // licenses
        for (License license : model.getLicenses()) {
            extension.addLicense(getExtensionLicense(license));
        }

        // features
        String featuresString = model.getProperties().getProperty(MPKEY_FEATURES);
        if (StringUtils.isNotBlank(featuresString)) {
            extension.setFeatures(this.converter.<Collection<String>> convert(List.class, featuresString));
        }

        // dependencies
        try {
            ArtifactTypeRegistry stereotypes = session.getArtifactTypeRegistry();

            for (org.apache.maven.model.Dependency mavenDependency : model.getDependencies()) {
                if (!mavenDependency.isOptional()
                    && (mavenDependency.getScope().equals("compile") || mavenDependency.getScope().equals("runtime"))) {
                    extension
                        .addDependency(new AetherExtensionDependency(convertToAether(mavenDependency, stereotypes)));
                }
            }
        } catch (Exception e) {
            throw new ResolveException("Failed to resolve dependencies", e);
        }

        return extension;
    }

    private Dependency convertToAether(org.apache.maven.model.Dependency dependency, ArtifactTypeRegistry stereotypes)
        throws IllegalArgumentException, IllegalAccessException, InvocationTargetException
    {
        return (Dependency) convertMethod.invoke(this.mavenDescriptorReader, dependency, stereotypes);
    }

    // TODO: download custom licenses content
    private ExtensionLicense getExtensionLicense(License license)
    {
        if (license.getName() == null) {
            return new ExtensionLicense("noname", null);
        }

        return createLicenseByName(license.getName());
    }

    private ExtensionLicense createLicenseByName(String name)
    {
        ExtensionLicense extensionLicense = this.licenseManager.getLicense(name);

        return extensionLicense != null ? extensionLicense : new ExtensionLicense(name, null);

    }

    private Model loadPom(Artifact artifact, RepositorySystemSession session) throws IllegalArgumentException,
        IllegalAccessException, InvocationTargetException
    {
        ArtifactDescriptorRequest artifactDescriptorRequest = new ArtifactDescriptorRequest();
        artifactDescriptorRequest.setArtifact(artifact);
        artifactDescriptorRequest.addRepository(this.remoteRepository);

        ArtifactDescriptorResult artifactDescriptorResult = new ArtifactDescriptorResult(artifactDescriptorRequest);

        return (Model) loadPomMethod.invoke(this.mavenDescriptorReader, session, artifactDescriptorRequest,
            artifactDescriptorResult);
    }

    public RemoteRepository getRemoteRepository()
    {
        return this.remoteRepository;
    }
}
