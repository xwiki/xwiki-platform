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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

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
        String propertyValue = getModelFromCurrentPOM().getProperties().getProperty(propertyName);
        if (propertyValue == null) {
            throw new Exception(String.format("Missing property [%s] in the current pom.xml", propertyName));
        }
        return propertyValue;
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
     * @param artifactCoordinates the Maven artifact coordinates to convert to a resolved list of Artifacts
     * @return the resolved list of artifacts
     * @throws Exception in case the POM model cannot be resolved
     * @since 10.11.2
     * @since 12.0RC1
     */
    public List<Artifact> convertToArtifacts(List<ArtifactCoordinate> artifactCoordinates) throws Exception
    {
        List<Artifact> artifacts = new ArrayList<>();
        if (!artifactCoordinates.isEmpty()) {
            for (ArtifactCoordinate artifactCoordinate : artifactCoordinates) {
                Artifact artifact = artifactCoordinate.toArtifact(getModelFromCurrentPOM().getVersion());
                LOGGER.info("Adding extra JAR to WEB-INF/lib: [{}]", artifact);
                artifacts.add(artifact);
            }
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
}
