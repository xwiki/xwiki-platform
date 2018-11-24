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
package org.xwiki.test.docker.junit5;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

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

    private ArtifactResolver artifactResolver;

    private RepositoryResolver repositoryResolver;

    /**
     * @param artifactResolver the resolver to resolve artifacts from Maven repositories
     * @param repositoryResolver the resolver to create Maven repositories and sessions
     */
    public MavenResolver(ArtifactResolver artifactResolver, RepositoryResolver repositoryResolver)
    {
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
        String platformVersion = getModelFromCurrentPOM().getProperties().getProperty("platform.version");
        if (platformVersion == null) {
            throw new Exception("Missing property <platform.version> in the current pom.xml");
        }
        return platformVersion;
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
}
