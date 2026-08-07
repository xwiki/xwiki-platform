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
package org.xwiki.repository.internal.job;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Provider;

import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.Strings;
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
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.environment.Environment;
import org.xwiki.extension.Extension;
import org.xwiki.extension.ExtensionContext;
import org.xwiki.extension.ExtensionException;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.ExtensionManager;
import org.xwiki.extension.ExtensionSession;
import org.xwiki.extension.job.InstallRequest;
import org.xwiki.extension.job.internal.InstallPlanJob;
import org.xwiki.extension.job.plan.ExtensionPlan;
import org.xwiki.extension.job.plan.ExtensionPlanAction;
import org.xwiki.extension.job.plan.ExtensionPlanAction.Action;
import org.xwiki.extension.repository.CoreExtensionRepository;
import org.xwiki.extension.repository.ExtensionRepository;
import org.xwiki.extension.repository.ExtensionRepositoryManager;
import org.xwiki.extension.repository.LocalExtensionRepository;
import org.xwiki.extension.repository.aether.internal.AetherExtensionRepository;
import org.xwiki.extension.repository.aether.internal.XWikiRepositorySystemSession;
import org.xwiki.job.AbstractJob;
import org.xwiki.job.Job;
import org.xwiki.repository.job.XIPExportJobRequest;
import org.xwiki.repository.job.XIPExportJobStatus;
import org.xwiki.resource.temporary.TemporaryResourceStore;

/**
 * Job responsible to export an extension (along with its dependencies) as a XIP package that can be used to install the
 * extension offline on another XWiki instance.
 *
 * @version $Id$
 * @since 18.5.0RC1
 */
@Component
@Named(XIPExportJob.JOB_TYPE)
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
@SuppressWarnings("checkstyle:ClassFanOutComplexity")
public class XIPExportJob extends AbstractJob<XIPExportJobRequest, XIPExportJobStatus>
{
    /**
     * The XIP export job type.
     */
    public static final String JOB_TYPE = "repository/xip";

    @Inject
    private ExtensionContext extensionContext;

    @Inject
    private Environment environment;

    @Inject
    private Provider<PlexusContainer> plexusProvider;

    @Inject
    private XWikiRepositorySystemSession systemSession;

    @Inject
    private ExtensionRepositoryManager extensionRepositoryManager;

    @Inject
    private ExtensionManager extensionManager;

    @Inject
    @Named("tmp")
    private Provider<CoreExtensionRepository> tmpCoreRepositoryProvider;

    @Inject
    @Named("tmp")
    private Provider<LocalExtensionRepository> tmpLocalRepositoryProvider;

    @Inject
    @Named(InstallPlanJob.JOBTYPE)
    private Provider<Job> installPlanJobProvider;

    @Inject
    private TemporaryResourceStore temporaryResourceStore;

    private RepositorySystem repositorySystem;

    @Override
    public String getType()
    {
        return JOB_TYPE;
    }

    @Override
    protected XIPExportJobStatus createNewStatus(XIPExportJobRequest request)
    {
        return new XIPExportJobStatus(getType(), request, this.observationManager, this.loggerManager);
    }

    @Override
    protected void runInternal() throws Exception
    {
        this.progressManager.pushLevelProgress(5, this);

        ExtensionSession extensionSession = this.extensionContext.pushSession();

        try {
            this.progressManager.startStep(this);
            initializeRepositorySystem(extensionSession);
            this.progressManager.endStep(this);

            if (this.status.isCanceled()) {
                return;
            }

            this.progressManager.startStep(this);
            CoreExtensionRepository repository = resolveCoreExtensions();
            this.progressManager.endStep(this);

            if (this.status.isCanceled()) {
                return;
            }

            this.progressManager.startStep(this);
            ExtensionPlan plan = getInstallPlan(repository);
            this.progressManager.endStep(this);

            if (this.status.isCanceled()) {
                return;
            }

            this.progressManager.startStep(this);
            TemporaryLocalExtensionRepository localRepository = serializeInstallPlan(plan);
            this.progressManager.endStep(this);

            if (this.status.isCanceled()) {
                return;
            }

            try {
                this.progressManager.startStep(this);
                archiveLocalRepository(localRepository);
                this.progressManager.endStep(this);
            } finally {
                localRepository.dispose();
            }
        } finally {
            this.progressManager.popLevelProgress(this);
            this.extensionContext.popSession();
        }
    }

    private void initializeRepositorySystem(ExtensionSession extensionSession) throws Exception
    {
        this.logger.info("Initializing repository system...");
        PlexusContainer plexusContainer = this.plexusProvider.get();
        this.repositorySystem = plexusContainer.lookup(RepositorySystem.class);

        Path path = this.environment.getPermanentDirectory().toPath().resolve("cache/repository/xip");
        this.systemSession.initialize(repositorySystem, path, false);
        extensionSession.set("maven.systemSession", systemSession);
    }

    private CoreExtensionRepository resolveCoreExtensions() throws ExtensionException
    {
        this.logger.info("Resolving core extensions...");
        this.progressManager.pushLevelProgress(5, this);
        try {
            this.progressManager.startStep(this);
            Set<Artifact> artifacts = collectCoreArtifacts();
            this.progressManager.endStep(this);

            if (artifacts.isEmpty() || this.status.isCanceled()) {
                return null;
            }

            this.progressManager.startStep(this);
            CoreExtensionRepository coreExtensionRepository = createCoreExtensionRepository(artifacts);
            this.progressManager.endStep(this);
            return coreExtensionRepository;
        } finally {
            this.progressManager.popLevelProgress(this);
        }
    }

    private Set<Artifact> collectCoreArtifacts() throws ExtensionException
    {
        this.logger.info("Collecting core artifacts...");
        List<Artifact> coreExtensions = getCoreExtensions();
        if (coreExtensions == null || coreExtensions.isEmpty()) {
            return Set.of();
        }

        CollectRequest request = new CollectRequest();

        request.setRepositories(getAllMavenRepositories());

        for (Artifact coreExtension : coreExtensions) {
            request.addDependency(new Dependency(coreExtension, null));
        }

        CollectResult collectResult;
        try {
            collectResult = this.repositorySystem.collectDependencies(this.systemSession, request);
        } catch (DependencyCollectionException e) {
            throw new ExtensionException("Failed to resolve artifacts", e);
        }

        if (!collectResult.getExceptions().isEmpty()) {
            throw new ExtensionException(String.format("Failed to resolve artifacts %s", coreExtensions),
                collectResult.getExceptions().get(0));
        }

        Set<Artifact> artifacts = new HashSet<>();

        collectResult.getRoot().getChildren().forEach(child -> addNode(child, artifacts));
        return artifacts;
    }

    private List<Artifact> getCoreExtensions()
    {
        String xwikiVersion = getRequest().getXWikiVersion();
        return getRequest().getCoreExtensions().stream().<Artifact>map(extensionId -> {
            String[] parts = extensionId.getId().split(":");
            if (parts.length == 2) {
                String groupId = parts[0];
                String artifactId = parts[1];
                return new DefaultArtifact(groupId, artifactId, "pom", xwikiVersion);
            } else {
                return null;
            }
        }).filter(Objects::nonNull).toList();
    }

    private List<RemoteRepository> getAllMavenRepositories()
    {
        Collection<ExtensionRepository> extensionRepositories = this.extensionRepositoryManager.getRepositories();

        List<RemoteRepository> repositories = new ArrayList<>(extensionRepositories.size());

        for (ExtensionRepository extensionRepository : extensionRepositories) {
            if (extensionRepository instanceof AetherExtensionRepository aetherExtensionRepository) {
                RemoteRepository repository = aetherExtensionRepository.getRemoteRepository();

                repositories.add(repository);
            }
        }

        this.repositorySystem.newResolutionRepositories(this.systemSession, repositories);

        return repositories;
    }

    private void addNode(DependencyNode node, Collection<Artifact> artifacts)
    {
        // TODO: find out why we end up with "system" scope dependency (seems to be specific to jdk.tools:jdk.tools)
        if (!node.getDependency().getScope().equals("system")) {
            artifacts.add(node.getArtifact());
            node.getChildren().forEach(child -> addNode(child, artifacts));
        }
    }

    private CoreExtensionRepository createCoreExtensionRepository(Set<Artifact> artifacts) throws ExtensionException
    {
        TemporaryCoreExtensionRepository repository =
            (TemporaryCoreExtensionRepository) this.tmpCoreRepositoryProvider.get();

        this.logger.info("Resolving [{}] core extensions...", artifacts.size());
        this.progressManager.pushLevelProgress(artifacts.size(), this);

        try {
            for (Artifact artifact : artifacts) {
                if (this.status.isCanceled()) {
                    break;
                }
                this.progressManager.startStep(this);
                this.logger.info("Resolving core extension [{}:{}]...", artifact.getGroupId(),
                    artifact.getArtifactId());
                Extension extension = this.extensionManager.resolveExtension(
                    new ExtensionId(artifact.getGroupId() + ':' + artifact.getArtifactId(), artifact.getVersion()));
                repository.addExtension(extension);
                this.progressManager.endStep(this);
            }
        } finally {
            this.progressManager.popLevelProgress(this);
        }

        return repository;
    }

    private ExtensionPlan getInstallPlan(CoreExtensionRepository repository) throws ExtensionException
    {
        this.logger.info("Computing install plan for [{}]...", this.request.getExtension());
        InstallRequest planRequest = new InstallRequest();
        planRequest.addExtension(this.request.getExtension());
        planRequest.setInstalledIgnored(true);
        planRequest.setCoreExtensionRepository(repository);
        planRequest.setVerbose(false);
        Job installPlanJob = installPlanJobProvider.get();
        installPlanJob.initialize(planRequest);
        installPlanJob.run();
        ExtensionPlan plan = (ExtensionPlan) installPlanJob.getStatus();
        if (plan.getError() != null) {
            throw new ExtensionException(
                "Failed to resolve the install plan for extension [" + this.request.getExtension() + "]",
                plan.getError());
        }
        return plan;
    }

    private TemporaryLocalExtensionRepository serializeInstallPlan(ExtensionPlan plan) throws ExtensionException
    {
        this.logger.info("Downloading extension files...");
        List<ExtensionPlanAction> installActions =
            plan.getActions().stream().filter(item -> item.getAction() == Action.INSTALL).toList();

        TemporaryLocalExtensionRepository localRepository =
            (TemporaryLocalExtensionRepository) this.tmpLocalRepositoryProvider.get();

        this.logger.info("Storing [{}] extensions...", installActions.size());
        this.progressManager.pushLevelProgress(installActions.size(), this);

        try {
            for (ExtensionPlanAction action : installActions) {
                if (this.status.isCanceled()) {
                    break;
                }
                this.progressManager.startStep(this);
                this.logger.info("Storing extension [{}]...", action.getExtension().getId());
                localRepository.storeExtension(action.getExtension());
                this.progressManager.endStep(this);
            }
        } finally {
            this.progressManager.popLevelProgress(this);
        }

        return localRepository;
    }

    private void archiveLocalRepository(TemporaryLocalExtensionRepository localRepository) throws IOException
    {
        this.logger.info("Creating XIP archive...");
        String suffix = ".xip";
        String prefix = Strings.CS.removeEnd(this.status.getXIPFileReference().getResourceName(), suffix);
        File xip = Files.createTempFile(this.environment.getTemporaryDirectory().toPath(), prefix, suffix).toFile();
        try {
            try (ZipArchiveOutputStream zipStream = new ZipArchiveOutputStream(xip)) {
                zipStream.setEncoding("UTF8");
                zipFile(localRepository.getPath().toFile(), "", zipStream);
            }
            if (!this.status.isCanceled()) {
                try (FileInputStream xipStream = new FileInputStream(xip)) {
                    this.temporaryResourceStore.createTemporaryFile(this.status.getXIPFileReference(), xipStream);
                }
            }
        } finally {
            Files.delete(xip.toPath());
        }
    }

    private void zipFile(File file, String path, ZipArchiveOutputStream zipStream) throws IOException
    {
        if (this.status.isCanceled()) {
            return;
        }

        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File childFile : children) {
                    zipFile(childFile, path + "/" + childFile.getName(), zipStream);
                }
            }
        } else {
            zipStream.putArchiveEntry(zipStream.createArchiveEntry(file, path));
            try (FileInputStream inputStream = new FileInputStream(file)) {
                IOUtils.copy(inputStream, zipStream);
            }
            zipStream.closeArchiveEntry();
        }
    }
}
