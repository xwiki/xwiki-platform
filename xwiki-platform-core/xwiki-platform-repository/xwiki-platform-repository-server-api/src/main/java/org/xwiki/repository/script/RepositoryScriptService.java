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
package org.xwiki.repository.script;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.codehaus.plexus.PlexusContainer;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.collection.CollectResult;
import org.eclipse.aether.collection.DependencyCollectionException;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.repository.RemoteRepository;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.context.Execution;
import org.xwiki.environment.Environment;
import org.xwiki.extension.Extension;
import org.xwiki.extension.ExtensionContext;
import org.xwiki.extension.ExtensionException;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.ExtensionManager;
import org.xwiki.extension.ExtensionSession;
import org.xwiki.extension.ResolveException;
import org.xwiki.extension.job.InstallRequest;
import org.xwiki.extension.job.internal.InstallPlanJob;
import org.xwiki.extension.job.plan.ExtensionPlan;
import org.xwiki.extension.job.plan.ExtensionPlanAction;
import org.xwiki.extension.job.plan.ExtensionPlanAction.Action;
import org.xwiki.extension.repository.CoreExtensionRepository;
import org.xwiki.extension.repository.ExtensionRepository;
import org.xwiki.extension.repository.ExtensionRepositoryFactory;
import org.xwiki.extension.repository.ExtensionRepositoryManager;
import org.xwiki.extension.repository.LocalExtensionRepository;
import org.xwiki.extension.repository.aether.internal.AetherExtensionRepository;
import org.xwiki.extension.repository.aether.internal.XWikiRepositorySystemSession;
import org.xwiki.extension.version.Version;
import org.xwiki.job.Job;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.repository.internal.RepositoryManager;
import org.xwiki.script.service.ScriptService;

@Component
@Named("repository")
@Singleton
public class RepositoryScriptService implements ScriptService, Initializable
{
    /**
     * The key under which the last encountered error is stored in the current execution context.
     */
    private static final String REPOSITORYERROR_KEY = "scriptservice.repository.error";

    @Inject
    private RepositoryManager repositoryManager;

    @Inject
    private ExtensionRepositoryManager extensionRepositoryManager;

    @Inject
    @Named("maven")
    private ExtensionRepositoryFactory mavenRepositoryFactory;

    /**
     * Provides access to the current context.
     */
    @Inject
    private Execution execution;

    @Inject
    private ExtensionContext extensionContext;

    @Inject
    private Provider<PlexusContainer> plexusProvider;

    @Inject
    private ExtensionManager extensionManager;

    @Inject
    @Named(InstallPlanJob.JOBTYPE)
    private Provider<Job> installPlanJobProvider;

    @Inject
    private Logger logger;

    @Inject
    private Environment environment;

    @Inject
    @Named("tmp")
    private Provider<CoreExtensionRepository> tmpCoreRepositoryProvider;

    @Inject
    @Named("tmp")
    private Provider<LocalExtensionRepository> tmpLocalRepositoryProvider;

    private RepositorySystem repositorySystem;

    @Override
    public void initialize() throws InitializationException
    {
        try {
            PlexusContainer plexusContainer = this.plexusProvider.get();
            this.repositorySystem = plexusContainer.lookup(RepositorySystem.class);
        } catch (org.codehaus.plexus.component.repository.exception.ComponentLookupException e) {
            throw new InitializationException("", e);
        }
    }

    /**
     * Store a caught exception in the context, so that it can be later retrieved using {@link #getLastError()}.
     * 
     * @param e the exception to store, can be {@code null} to clear the previously stored exception
     * @see #getLastError()
     */
    private void setError(Exception e)
    {
        this.execution.getContext().setProperty(REPOSITORYERROR_KEY, e);
    }

    /**
     * Get the error generated while performing the previously called action.
     * 
     * @return an eventual exception or {@code null} if no exception was thrown
     */
    public Exception getLastError()
    {
        return (Exception) this.execution.getContext().getProperty(REPOSITORYERROR_KEY);
    }

    public void validateExtensions()
    {
        setError(null);

        try {
            this.repositoryManager.validateExtensions();
        } catch (Exception e) {
            setError(e);
        }
    }

    public DocumentReference importExtension(String extensionId, String repositoryId)
    {
        setError(null);

        try {
            ExtensionRepository repository = this.extensionRepositoryManager.getRepository(repositoryId);

            if (repository == null) {
                throw new ExtensionException("Can't find any registered repository with id [" + repositoryId + "]");
            }

            return this.repositoryManager.importExtension(extensionId, repository, Version.Type.STABLE);
        } catch (Exception e) {
            setError(e);
        }

        return null;
    }

    public void exportExtension(ExtensionId extensionId, Version xwikiVersion, HttpServletResponse response)
        throws Throwable
    {
        // exportExtension(extensionId, List.of(new DefaultArtifact("org.xwiki.platform",
        // "xwiki-platform-distribution-war-dependencies", "pom", xwikiVersion.getValue())), outputStream);
        exportExtension(extensionId,
            List.of(new DefaultArtifact("org.xwiki.rendering", "xwiki-rendering-api", "pom", xwikiVersion.getValue())),
            response);
    }

    private void exportExtension(ExtensionId extensionId, List<Artifact> coreExtensions, HttpServletResponse response)
        throws Throwable
    {
        ExtensionSession extensionSession = this.extensionContext.pushSession();

        try {
            XWikiRepositorySystemSession systemSession;
            try {
                Path path = this.environment.getPermanentDirectory().toPath().resolve("cache/repository/xip");

                systemSession = new XWikiRepositorySystemSession(this.repositorySystem, path, false);
            } catch (IOException e) {
                throw new ResolveException("Failed to create the repository system session", e);
            }
            extensionSession.set("maven.systeSession", systemSession);

            // Resolve core extensions
            CoreExtensionRepository repository = resolveCoreExtensions(coreExtensions, systemSession);

            // Resolve isolated install plan
            InstallRequest planRequest = new InstallRequest();
            planRequest.addExtension(extensionId);
            planRequest.setInstalledIgnored(true);
            planRequest.setCoreExtensionRepository(repository);
            planRequest.setVerbose(false);
            Job installPlanJob = installPlanJobProvider.get();
            installPlanJob.initialize(planRequest);
            installPlanJob.run();
            ExtensionPlan plan = (ExtensionPlan) installPlanJob.getStatus();
            if (plan.getError() != null) {
                throw plan.getError();
            }

            // Serialize extensions
            TemporaryLocalExtensionRepository localRepository =
                (TemporaryLocalExtensionRepository) this.tmpLocalRepositoryProvider.get();
            for (ExtensionPlanAction action : plan.getActions()) {
                if (action.getAction() == Action.INSTALL) {
                    localRepository.storeExtension(action.getExtension());
                }
            }

            response.setContentType("application/zip;charset=UTF-8");
            response.addHeader("Content-disposition",
                "attachment; filename=" + StringUtils.substringAfterLast(extensionId.getId(), ':') + ".xip");

            // Package
            try (ZipArchiveOutputStream zipStream = new ZipArchiveOutputStream(response.getOutputStream())) {
                zipStream.setEncoding("UTF8");

                zipFile(localRepository.getPath().toFile(), "", zipStream);
            } finally {
                localRepository.dispose();
            }

            response.flushBuffer();
        } finally {
            this.extensionContext.popSession();
        }
    }

    private void zipFile(File file, String path, ZipArchiveOutputStream zipStream) throws IOException
    {
        if (file.isDirectory()) {
            for (File childFile : file.listFiles()) {
                zipFile(childFile, path + "/" + childFile.getName(), zipStream);
            }
        } else {
            zipStream.putArchiveEntry(zipStream.createArchiveEntry(file, path));
            try (FileInputStream inputStream = new FileInputStream(file)) {
                IOUtils.copy(inputStream, zipStream);
            }
            zipStream.closeArchiveEntry();
        }
    }

    public CoreExtensionRepository resolveCoreExtensions(List<Artifact> coreExtensions,
        XWikiRepositorySystemSession systemSession) throws ExtensionException
    {
        if (coreExtensions == null || coreExtensions.isEmpty()) {
            return null;
        }

        CollectRequest request = new CollectRequest();

        request.setRepositories(getAllMavenRepositories(systemSession));

        for (Artifact coreExtension : coreExtensions) {
            request.addDependency(new Dependency(coreExtension, null));
        }

        CollectResult collectResult;
        try {
            collectResult = this.repositorySystem.collectDependencies(systemSession, request);
        } catch (DependencyCollectionException e) {
            throw new ExtensionException("Failed to resolve artifacts", e);
        }

        if (!collectResult.getExceptions().isEmpty()) {
            throw new ExtensionException(String.format("Failed to resolve artifacts %s", coreExtensions),
                collectResult.getExceptions().get(0));
        }

        Set<Artifact> artifacts = new HashSet<>();

        addNodes(collectResult.getRoot().getChildren(), artifacts);

        TemporaryCoreExtensionRepository repository =
            (TemporaryCoreExtensionRepository) this.tmpCoreRepositoryProvider.get();

        for (Artifact artifact : artifacts) {
            Extension extension = this.extensionManager.resolveExtension(
                new ExtensionId(artifact.getGroupId() + ':' + artifact.getArtifactId(), artifact.getVersion()));

            repository.addExtension(extension);
        }

        return repository;
    }

    private void addNode(DependencyNode node, Collection<Artifact> artifacts)
    {
        // TODO: find out why we end up with "system" scope dependency (seems to be specific to jdk.tools:jdk.tools)
        if (!node.getDependency().getScope().equals("system")) {
            artifacts.add(node.getArtifact());

            addNodes(node.getChildren(), artifacts);
        }
    }

    private void addNodes(List<DependencyNode> nodes, Collection<Artifact> artifacts)
    {
        nodes.forEach(c -> addNode(c, artifacts));
    }

    protected List<RemoteRepository> getAllMavenRepositories(XWikiRepositorySystemSession systemSession)
    {
        Collection<ExtensionRepository> extensionRepositories = this.extensionRepositoryManager.getRepositories();

        List<RemoteRepository> reposirories = new ArrayList<>(extensionRepositories.size());

        // Add other repositories (and filter first one)
        for (ExtensionRepository extensionRepository : extensionRepositories) {
            if (extensionRepository instanceof AetherExtensionRepository) {
                RemoteRepository repository = ((AetherExtensionRepository) extensionRepository).getRemoteRepository();

                reposirories.add(repository);
            }
        }

        this.repositorySystem.newResolutionRepositories(systemSession, reposirories);

        return reposirories;
    }
}
