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
package org.xwiki.test.integration.maven;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.text.StringSubstitutor;
import org.apache.commons.text.lookup.StringLookup;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.building.DefaultModelBuilderFactory;
import org.apache.maven.model.building.DefaultModelBuildingRequest;
import org.apache.maven.model.building.ModelBuilder;
import org.apache.maven.model.building.ModelBuildingRequest;
import org.apache.maven.model.building.ModelBuildingResult;
import org.apache.maven.model.building.ModelProblem;
import org.apache.maven.model.resolution.ModelResolver;
import org.apache.maven.project.ProjectModelResolver;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.resolution.ArtifactResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.apache.maven.project.ProjectBuildingRequest.RepositoryMerging.POM_DOMINANT;

/**
 * Resolves Maven Artifacts either from a remote repository or from a local POM file.
 *
 * @version $Id$
 * @since 10.9
 */
public class MavenResolver
{
    private static final Logger LOGGER = LoggerFactory.getLogger(MavenResolver.class);

    private Map<String, Model> modelCache = new HashMap<>();

    private List<String> profiles;

    private ArtifactResolver artifactResolver;

    private RepositoryResolver repositoryResolver;

    /**
     * @param profiles the list of Maven profile to enable when reading/resolving pom files
     * @param artifactResolver the resolver to resolve artifacts from Maven repositories
     * @param repositoryResolver the resolver to create Maven repositories and sessions
     */
    public MavenResolver(List<String> profiles, ArtifactResolver artifactResolver,
        RepositoryResolver repositoryResolver)
    {
        this.profiles = profiles;
        this.artifactResolver = artifactResolver;
        this.repositoryResolver = repositoryResolver;
    }

    /**
     * @return the Maven Model object for the {@code pom.xml} file in the current directory (i.e. the POM of the module
     * executing functional tests)
     * @throws Exception if an error occurred during reading and parsing of the POM
     */
    public Model getModelFromCurrentPOM() throws Exception
    {
        return getModelFromPOM(new File("./pom.xml"));
    }

    /**
     * @return the version of the XWiki platform artifacts to download/resolve
     * @throws Exception if an error occurred during reading and parsing of the POM
     * @since 16.2.0RC1
     */
    public String getCommonsVersion() throws Exception
    {
        return getPropertyFromCurrentPOM("commons.version");
    }

    /**
     * @return the version of the XWiki platform artifacts to download/resolve
     * @throws Exception if an error occurred during reading and parsing of the POM
     */
    public String getPlatformVersion() throws Exception
    {
        return getPropertyFromCurrentPOM("platform.version");
    }

    /**
     * @param propertyName the maven property name for which to retrieve the value
     * @return the property value
     * @throws Exception if the property doesn't exist
     * @since 11.7RC1
     */
    public String getPropertyFromCurrentPOM(String propertyName) throws Exception
    {
        String propertyValue = getPropertiesFromCurrentPOM().getProperty(propertyName);
        if (propertyValue == null) {
            throw new Exception(String.format("Missing property [%s] in the current pom.xml", propertyName));
        }
        return propertyValue;
    }

    /**
     * @return the resolved POM properties
     * @throws Exception if the properties cannot be resolved
     * @since 15.2RC1
     */
    public Properties getPropertiesFromCurrentPOM() throws Exception
    {
        return getModelFromCurrentPOM().getProperties();
    }

    /**
     * @param pomFile the location of the pom file to read
     * @return the Maven Model object for the {@code pom.xml} file passed in parameters
     * @throws Exception if an error occurred during reading and parsing of the POM
     */
    public Model getModelFromPOM(File pomFile) throws Exception
    {
        Model model = this.modelCache.get(pomFile.toString());
        if (model == null) {
            ModelResolver resolver = new ProjectModelResolver(this.repositoryResolver.getSession(), null,
                this.repositoryResolver.getSystem(), this.repositoryResolver.getRemoteRepositoryManager(),
                this.repositoryResolver.getRepositories(), POM_DOMINANT, null);

            DefaultModelBuildingRequest modelBuildingRequest = new DefaultModelBuildingRequest()
                .setPomFile(pomFile)
                .setActiveProfileIds(this.profiles)
                .setModelResolver(resolver)
                // We don't care about many things in the POM such as plugins for example so asking for minimal
                // validation.
                .setValidationLevel(ModelBuildingRequest.VALIDATION_LEVEL_MINIMAL)
                .setSystemProperties(toProperties(this.repositoryResolver.getSession().getUserProperties(),
                    this.repositoryResolver.getSession().getSystemProperties()))
                // We don't care about resolving plugins.
                .setProcessPlugins(false)
                .setTwoPhaseBuilding(false);

            ModelBuilder modelBuilder = new DefaultModelBuilderFactory().newInstance();
            ModelBuildingResult modelBuildingResult = modelBuilder.build(modelBuildingRequest);

            for (ModelProblem problem : modelBuildingResult.getProblems()) {
                LOGGER.warn("Problem [{}] [{}] [{}]", problem.getModelId(), problem.getSource(), problem.getMessage());
            }

            model = modelBuildingResult.getEffectiveModel();
            this.modelCache.put(pomFile.toString(), model);
        }

        return model;
    }

    /**
     * @param artifact the artifact for which to read and parse the POM
     * @return the Maven Model object for the passed artifact
     * @throws Exception if an error occurred during resolving of the artifact or during reading and parsing of the POM
     */
    public Model getModelFromPOMArtifact(Artifact artifact) throws Exception
    {
        ArtifactResult artifactResult = this.artifactResolver.resolveArtifact(artifact);
        return getModelFromPOM(artifactResult.getArtifact().getFile());
    }

    private Properties toProperties(Map<String, String> dominant, Map<String, String> recessive)
    {
        Properties props = new Properties();
        if (recessive != null) {
            props.putAll(recessive);
        }
        if (dominant != null) {
            props.putAll(dominant);
        }
        return props;
    }

    /**
     * @param dependency the dependency to convert to an Artifact instance
     * @return the converted Artifact instance
     * @since 10.11RC1
     */
    public Artifact convertToArtifact(Dependency dependency)
    {
        return new DefaultArtifact(dependency.getGroupId(), dependency.getArtifactId(),
            dependency.getClassifier(), dependency.getType(), dependency.getVersion());
    }

    /**
     * @param dependencies the dependencies to convert to Artifact instances
     * @return the converted Artifact instances
     * @since 12.5RC1
     */
    public List<Artifact> convertToArtifacts(Collection<Dependency> dependencies)
    {
        List<Artifact> artifacts = new ArrayList<>();
        for (Dependency dependency : dependencies) {
            artifacts.add(convertToArtifact(dependency));
        }
        return artifacts;
    }

    /**
     * @param artifactCoordinates the Maven artifact coordinates to convert to a resolved list of Artifacts
     * @param resolveExtraJARs whether or not extra JARs without version should get their version resolved from the
     *        current POM or not
     * @return the resolved list of artifacts
     * @throws Exception in case the POM model cannot be resolved
     * @since 12.5RC1
     */
    public List<Artifact> convertToArtifacts(Collection<ArtifactCoordinate> artifactCoordinates,
        boolean resolveExtraJARs)
        throws Exception
    {
        List<Artifact> artifacts = resolveVersions(artifactCoordinates, resolveExtraJARs);
        for (Artifact artifact : artifacts) {
            LOGGER.info("Adding extra JAR to WEB-INF/lib: [{}]", artifact);
        }
        return artifacts;
    }

    /**
     * @param artifacts the list of artifacts to which to add the Clover JAR
     * @throws Exception in case the POM model cannot be resolved
     * @since 10.11.2
     * @since 12.0RC1
     */
    public void addCloverJAR(List<Artifact> artifacts) throws Exception
    {
        // Add the Clover JAR if it's defined in the current pom.xml since it's needed when the clover profile is
        // enabled. Note that we need this since by default we don't add any JAR to WEB-INF/lib (since we install
        // module artifacts as XWiki Extensions).
        Model model = getModelFromCurrentPOM();
        for (Dependency dependency : model.getDependencies()) {
            if (dependency.getArtifactId().equals("clover") && dependency.getGroupId().equals("org.openclover")) {
                artifacts.add(convertToArtifact(dependency));
            }
        }
    }

    /**
     * @param model the model from which to find the current artifact and its transitive dependencies
     * @return the full transitive dependencies for the current POM
     * @throws Exception in case an error occurred during resolving
     * @since 12.5RC1
     */
    public Collection<Artifact> getDependencies(Model model) throws Exception
    {
        List<Artifact> artifacts = new ArrayList<>();
        Artifact currentPOMArtifact = new DefaultArtifact(model.getGroupId(), model.getArtifactId(),
            model.getPackaging(), model.getVersion());
        // Don't include "test" dependencies as they should not be used to resolve extra jar artifact versions.
        List<Dependency> normalizedDependencies = new ArrayList<>();
        for (Dependency dependency : model.getDependencies()) {
            if (!"test".equals(dependency.getScope())) {
                normalizedDependencies.add(dependency);
            }
        }
        Collection<ArtifactResult> results = this.artifactResolver.getArtifactDependencies(currentPOMArtifact,
            convertToArtifacts(normalizedDependencies));
        for (ArtifactResult artifactResult : results) {
            artifacts.add(artifactResult.getArtifact());
        }
        return artifacts;
    }

    /**
     * Find the version to use if it's not specified. We look in the current Maven POM's dependencies to try to find
     * a matching artifact and get its version. If not, we default to the version of the current Maven POM module:
     * {@code getModelFromCurrentPOM().getVersion()}. Since resolving takes time and since it'll resolve
     * SNAPSHOTs, and it may not be what you want (you may have modifications done locally that you want to be used),
     * we check if the {@code resolveExtraJARs} parameter is set or not.
     */
    private String resolveVersion(ArtifactCoordinate artifactCoordinate, boolean resolveExtraJARs,
        Collection<Artifact> dependencies, String modelVersion)
    {
        String version = null;
        // Only resolve the version if not specified, for performance reasons.
        if (artifactCoordinate.getVersion() == null && resolveExtraJARs) {
            for (Artifact dependencyArtifact : dependencies) {
                if (dependencyArtifact.getGroupId().equals(artifactCoordinate.getGroupId())
                    && dependencyArtifact.getArtifactId().equals(artifactCoordinate.getArtifactId())
                    && dependencyArtifact.getExtension().equals(artifactCoordinate.getType()))
                {
                    version = dependencyArtifact.getVersion();
                    break;
                }
            }
        }
        if (artifactCoordinate.getVersion() == null && version == null) {
            version = modelVersion;
        }
        return version;
    }

    private List<Artifact> resolveVersions(Collection<ArtifactCoordinate> artifactCoordinates, boolean resolveExtraJARs)
        throws Exception
    {
        Model model = getModelFromCurrentPOM();
        // Compute the dependencies once for performance reasons.
        Collection<Artifact> dependencies = getDependencies(model);
        List<Artifact> artifacts = new ArrayList<>();
        for (ArtifactCoordinate artifactCoordinate : artifactCoordinates) {
            Artifact artifact = artifactCoordinate.toArtifact(resolveVersion(artifactCoordinate, resolveExtraJARs,
                dependencies, model.getVersion()));
            artifacts.add(artifact);
        }
        return artifacts;
    }

    /**
     * @param value the value in which to resolve Maven properties
     * @return the resolved value
     */
    public String replacePropertiesFromCurrentPOM(String value)
    {
        StringSubstitutor substitutor = new StringSubstitutor(new StringLookup()
        {
            @Override
            public String lookup(String key)
            {
                try {
                    return getPropertyFromCurrentPOM(key);
                } catch (Exception e) {
                    LOGGER.error("Failed to resolve Maven property [{}] in value [{}]", key, value, e);
                }

                return null;
            }
        });

        return substitutor.replace(value);
    }
}
