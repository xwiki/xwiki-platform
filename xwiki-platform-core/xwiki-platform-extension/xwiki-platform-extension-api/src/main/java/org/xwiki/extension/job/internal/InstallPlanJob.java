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
package org.xwiki.extension.job.internal;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import org.xwiki.component.annotation.Component;
import org.xwiki.extension.CoreExtension;
import org.xwiki.extension.DefaultExtensionDependency;
import org.xwiki.extension.Extension;
import org.xwiki.extension.ExtensionDependency;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.InstallException;
import org.xwiki.extension.LocalExtension;
import org.xwiki.extension.ResolveException;
import org.xwiki.extension.job.InstallRequest;
import org.xwiki.extension.job.plan.ExtensionPlanAction.Action;
import org.xwiki.extension.job.plan.ExtensionPlanNode;
import org.xwiki.extension.job.plan.internal.DefaultExtensionPlan;
import org.xwiki.extension.job.plan.internal.DefaultExtensionPlanAction;
import org.xwiki.extension.job.plan.internal.DefaultExtensionPlanNode;
import org.xwiki.extension.repository.CoreExtensionRepository;
import org.xwiki.extension.repository.ExtensionRepositoryManager;
import org.xwiki.extension.repository.LocalExtensionRepository;
import org.xwiki.extension.version.IncompatibleVersionConstraintException;
import org.xwiki.extension.version.Version;
import org.xwiki.extension.version.VersionConstraint;

/**
 * Create an Extension installation plan.
 * 
 * @version $Id$
 */
@Component
@Named("installplan")
public class InstallPlanJob extends AbstractJob<InstallRequest>
{
    private static class ModifableExtensionPlanNode
    {
        // never change

        private final ExtensionDependency initialDependency;

        // can change

        public DefaultExtensionPlanAction action;

        public List<ModifableExtensionPlanNode> children;

        public VersionConstraint versionConstraint;

        public final List<ModifableExtensionPlanNode> duplicates = new ArrayList<ModifableExtensionPlanNode>();

        // helpers

        public ModifableExtensionPlanNode()
        {
            this.initialDependency = null;
        }

        public ModifableExtensionPlanNode(ExtensionDependency initialDependency, VersionConstraint versionConstraint)
        {
            this.initialDependency = initialDependency;
            this.versionConstraint = versionConstraint;
        }

        public ModifableExtensionPlanNode(ExtensionDependency initialDependency, ModifableExtensionPlanNode node)
        {
            this.initialDependency = initialDependency;

            set(node);
        }

        public void set(ModifableExtensionPlanNode node)
        {
            this.action = node.action;
            this.children = node.children;
        }

        public List<ModifableExtensionPlanNode> getChildren()
        {
            return this.children != null ? this.children : Collections.<ModifableExtensionPlanNode> emptyList();
        }
    }

    /**
     * Used to resolve extensions to install.
     */
    @Inject
    private ExtensionRepositoryManager repositoryManager;

    /**
     * Used to check if extension or its dependencies are already core extensions.
     */
    @Inject
    private CoreExtensionRepository coreExtensionRepository;

    /**
     * Used to manipulate local extension repository.
     */
    @Inject
    private LocalExtensionRepository localExtensionRepository;

    /**
     * The install plan.
     */
    private List<ExtensionPlanNode> finalExtensionTree = new ArrayList<ExtensionPlanNode>();

    /**
     * The temporary install plan. There is two because we want the one in the status to be unmodifiable.
     */
    private List<ModifableExtensionPlanNode> extensionTree = new ArrayList<ModifableExtensionPlanNode>();

    /**
     * Used to make sure dependencies are compatible between each other in the whole plan.
     * <p>
     * <id, <namespace, node>>.
     */
    private Map<String, Map<String, ModifableExtensionPlanNode>> extensionsNodeCache =
        new HashMap<String, Map<String, ModifableExtensionPlanNode>>();

    @Override
    protected DefaultJobStatus<InstallRequest> createNewStatus(InstallRequest request)
    {
        return new DefaultExtensionPlan<InstallRequest>(request, getId(), this.observationManager, this.loggerManager,
            this.finalExtensionTree);
    }

    @Override
    protected void start() throws Exception
    {
        List<ExtensionId> extensions = getRequest().getExtensions();

        notifyPushLevelProgress(extensions.size());

        try {
            for (ExtensionId extensionId : extensions) {
                if (getRequest().hasNamespaces()) {
                    List<String> namespaces = getRequest().getNamespaces();

                    notifyPushLevelProgress(namespaces.size());

                    try {
                        for (String namespace : namespaces) {
                            installExtension(extensionId, namespace, this.extensionTree);

                            notifyStepPropress();
                        }
                    } finally {
                        notifyPopLevelProgress();
                    }
                } else {
                    installExtension(extensionId, null, this.extensionTree);
                }

                notifyStepPropress();
            }
        } finally {
            notifyPopLevelProgress();
        }

        // Create the final tree

        this.finalExtensionTree.addAll(createFinalTree(this.extensionTree));
    }

    private List<ExtensionPlanNode> createFinalTree(List<ModifableExtensionPlanNode> tree)
    {
        List<ExtensionPlanNode> finalTree = new ArrayList<ExtensionPlanNode>(tree.size());

        for (ModifableExtensionPlanNode node : tree) {
            List<ExtensionPlanNode> children;
            if (!node.getChildren().isEmpty()) {
                children = createFinalTree(node.children);
            } else {
                children = null;
            }

            // Set the initial dependency version constraint
            DefaultExtensionPlanAction action =
                new DefaultExtensionPlanAction(node.action.getExtension(), node.action.getPreviousExtension(),
                    node.action.getAction(), node.action.getNamespace(), node.initialDependency != null);

            finalTree.add(new DefaultExtensionPlanNode(action, children, node.initialDependency != null
                ? node.initialDependency.getVersionConstraint() : null));
        }

        return finalTree;
    }

    private ModifableExtensionPlanNode getExtensionNode(String id, String namespace)
    {
        Map<String, ModifableExtensionPlanNode> extensionsById = this.extensionsNodeCache.get(id);

        if (extensionsById != null) {
            ModifableExtensionPlanNode node = extensionsById.get(namespace);

            if (node == null && namespace != null) {
                node = extensionsById.get(null);
            }
        }

        return null;
    }

    private void addExtensionNode(ModifableExtensionPlanNode node)
    {
        String id = node.action.getExtension().getId().getId();

        Map<String, ModifableExtensionPlanNode> extensionsById = this.extensionsNodeCache.get(id);

        if (extensionsById == null) {
            extensionsById = new HashMap<String, ModifableExtensionPlanNode>();
            this.extensionsNodeCache.put(id, extensionsById);
        }

        ModifableExtensionPlanNode existingNode = extensionsById.get(node.action.getNamespace());

        if (existingNode != null) {
            existingNode.set(node);
            for (ModifableExtensionPlanNode duplicate : existingNode.duplicates) {
                duplicate.set(node);
            }
            existingNode.duplicates.add(node);
        } else {
            extensionsById.put(node.action.getNamespace(), node);
        }
    }

    /**
     * Install provided extension.
     * 
     * @param extensionId the identifier of the extension to install
     * @param namespace the namespace where to install the extension
     * @param parentBranch the children of the parent {@link DefaultExtensionPlanNode}
     * @throws InstallException error when trying to install provided extension
     */
    private void installExtension(ExtensionId extensionId, String namespace,
        List<ModifableExtensionPlanNode> parentBranch) throws InstallException
    {
        installExtension(extensionId, false, namespace, parentBranch);
    }

    /**
     * Install provided extension.
     * 
     * @param extensionId the identifier of the extension to install
     * @param dependency indicate if the extension is installed as a dependency
     * @param namespace the namespace where to install the extension
     * @param parentBranch the children of the parent {@link DefaultExtensionPlanNode}
     * @throws InstallException error when trying to install provided extension
     */
    private void installExtension(ExtensionId extensionId, boolean dependency, String namespace,
        List<ModifableExtensionPlanNode> parentBranch) throws InstallException
    {
        if (namespace != null) {
            this.logger.info("Resolving extension [{}] on namespace [{}]", extensionId, namespace);
        } else {
            this.logger.info("Resolving extension [{}]", extensionId);
        }

        // Make sure the extension is not already a core extension
        if (this.coreExtensionRepository.exists(extensionId.getId())) {
            throw new InstallException(MessageFormat.format("There is already a core extension with the id [{0}]",
                extensionId.getId()));
        }

        LocalExtension previousExtension = null;

        LocalExtension localExtension =
            this.localExtensionRepository.getInstalledExtension(extensionId.getId(), namespace);
        if (localExtension != null) {
            this.logger.info("Found already installed extension with id [{}]. Checking compatibility.", extensionId);

            if (extensionId.getVersion() == null) {
                throw new InstallException(MessageFormat.format("The extension with id [{0}] is already installed",
                    extensionId.getId()));
            }

            int diff = extensionId.getVersion().compareTo(localExtension.getId().getVersion());

            if (diff == 0) {
                throw new InstallException(
                    MessageFormat.format("The extension [{0}] is already installed", extensionId));
            } else if (diff < 0) {
                throw new InstallException(MessageFormat.format("A more recent version of [{0}] is already installed",
                    extensionId.getId()));
            } else {
                // upgrade
                previousExtension = localExtension;
            }
        }

        ModifableExtensionPlanNode node = installExtension(previousExtension, extensionId, dependency, namespace);

        addExtensionNode(node);
        parentBranch.add(node);
    }

    private boolean isCompatible(Version existingVersion, VersionConstraint versionConstraint)
    {
        boolean compatible = true;

        if (versionConstraint.getVersion() == null) {
            compatible = versionConstraint.containsVersion(existingVersion);
        } else {
            compatible = existingVersion.compareTo(versionConstraint.getVersion()) >= 0;
        }

        return compatible;
    }

    private boolean checkCoreExtension(ExtensionDependency extensionDependency,
        List<ModifableExtensionPlanNode> parentBranch) throws InstallException
    {
        CoreExtension coreExtension = this.coreExtensionRepository.getCoreExtension(extensionDependency.getId());

        if (coreExtension != null) {
            if (!isCompatible(coreExtension.getId().getVersion(), extensionDependency.getVersionConstraint())) {
                throw new InstallException("Dependency [" + extensionDependency
                    + "] is not compatible with core extension [" + coreExtension + "]");
            } else {
                this.logger.info("There is already a core extension [{}] covering extension dependency [{}]",
                    coreExtension, extensionDependency);

                ModifableExtensionPlanNode node =
                    new ModifableExtensionPlanNode(extensionDependency, extensionDependency.getVersionConstraint());
                node.action = new DefaultExtensionPlanAction(coreExtension, null, Action.NONE, null, true);

                parentBranch.add(node);

                return true;
            }
        }

        return false;
    }

    private VersionConstraint checkExistingPlanNode(ExtensionDependency extensionDependency, String namespace,
        List<ModifableExtensionPlanNode> parentBranch, VersionConstraint previousVersionConstraint)
        throws InstallException
    {
        VersionConstraint versionConstraint = previousVersionConstraint;

        ModifableExtensionPlanNode existingNode = getExtensionNode(extensionDependency.getId(), namespace);
        if (existingNode != null) {
            if (isCompatible(existingNode.action.getExtension().getId().getVersion(), versionConstraint)) {
                ModifableExtensionPlanNode node = new ModifableExtensionPlanNode(extensionDependency, existingNode);
                addExtensionNode(node);
                parentBranch.add(node);

                return null;
            } else {
                if (existingNode.versionConstraint != null) {
                    try {
                        versionConstraint = versionConstraint.merge(existingNode.versionConstraint);
                    } catch (IncompatibleVersionConstraintException e) {
                        throw new InstallException("Dependency [" + extensionDependency
                            + "] is incompatible with current containt [" + existingNode.versionConstraint + "]", e);
                    }
                } else {
                    throw new InstallException("Dependency [" + extensionDependency + "] incompatible with extension ["
                        + existingNode.action.getExtension() + "]");
                }
            }
        }

        return versionConstraint;
    }

    private ExtensionDependency checkInstalledExtension(LocalExtension installedExtension,
        ExtensionDependency extensionDependency, VersionConstraint versionConstraint, String namespace,
        List<ModifableExtensionPlanNode> parentBranch) throws InstallException
    {
        ExtensionDependency targetDependency = extensionDependency;

        if (installedExtension != null) {
            if (isCompatible(installedExtension.getId().getVersion(), versionConstraint)) {
                this.logger.info("There is already an installed extension [{}] covering extension dependency [{}]",
                    installedExtension, extensionDependency);

                ModifableExtensionPlanNode node =
                    new ModifableExtensionPlanNode(extensionDependency, versionConstraint);
                node.action =
                    new DefaultExtensionPlanAction(installedExtension, null, Action.NONE, namespace,
                        installedExtension.isDependency());

                addExtensionNode(node);
                parentBranch.add(node);

                return null;
            }

            VersionConstraint mergedVersionContraint;
            try {
                if (installedExtension.isInstalled(null)) {
                    Map<String, Collection<LocalExtension>> backwardDependencies =
                        this.localExtensionRepository.getBackwardDependencies(installedExtension.getId());

                    mergedVersionContraint =
                        mergeVersionConstraints(backwardDependencies.get(null), extensionDependency.getId(),
                            versionConstraint);
                    if (namespace != null) {
                        mergedVersionContraint =
                            mergeVersionConstraints(backwardDependencies.get(namespace), extensionDependency.getId(),
                                mergedVersionContraint);
                    }
                } else {
                    Collection<LocalExtension> backwardDependencies =
                        this.localExtensionRepository.getBackwardDependencies(installedExtension.getId().getId(),
                            namespace);

                    mergedVersionContraint =
                        mergeVersionConstraints(backwardDependencies, extensionDependency.getId(), versionConstraint);
                }
            } catch (IncompatibleVersionConstraintException e) {
                throw new InstallException("Provided depency is incompatible with already installed extensions", e);
            } catch (ResolveException e) {
                throw new InstallException("Failed to resolve backward dependencies", e);
            }

            if (mergedVersionContraint != versionConstraint) {
                targetDependency = new DefaultExtensionDependency(extensionDependency, mergedVersionContraint);
            }
        }

        return targetDependency;
    }

    /**
     * Install provided extension dependency.
     * 
     * @param extensionDependency the extension dependency to install
     * @param namespace the namespace where to install the extension
     * @param parentBranch the children of the parent {@link DefaultExtensionPlanNode}
     * @throws InstallException error when trying to install provided extension
     */
    private void installExtensionDependency(ExtensionDependency extensionDependency, String namespace,
        List<ModifableExtensionPlanNode> parentBranch) throws InstallException
    {
        if (namespace != null) {
            this.logger.info("Resolving extension dependency [{}] on namespace [{}]", extensionDependency, namespace);
        } else {
            this.logger.info("Resolving extension dependency [{}]", extensionDependency);
        }

        VersionConstraint versionConstraint = extensionDependency.getVersionConstraint();

        // Make sure the dependency is not already a core extension
        if (checkCoreExtension(extensionDependency, parentBranch)) {
            // Already exists and added to the tree by #checkExistingPlan
            return;
        }

        // Make sure the dependency is not already in the current plan
        versionConstraint = checkExistingPlanNode(extensionDependency, namespace, parentBranch, versionConstraint);
        if (versionConstraint == null) {
            // Already exists and added to the tree by #checkExistingPlan
            return;
        }

        // Check local extensions
        LocalExtension previousExtension =
            this.localExtensionRepository.getInstalledExtension(extensionDependency.getId(), namespace);
        ExtensionDependency targetDependency =
            checkInstalledExtension(previousExtension, extensionDependency, versionConstraint, namespace, parentBranch);
        if (targetDependency == null) {
            // Already exists and added to the tree by #checkExistingPlan
            return;
        }

        // Not found locally, search it remotely
        ModifableExtensionPlanNode node = installExtension(previousExtension, targetDependency, true, namespace);

        node.versionConstraint = versionConstraint;

        addExtensionNode(node);
        parentBranch.add(node);
    }

    /**
     * Install provided extension.
     * 
     * @param previousExtension the previous installed version of the extension to install
     * @param targetDependency used to search the extension to install in remote repositories
     * @param dependency indicate if the extension is installed as a dependency
     * @param namespace the namespace where to install the extension
     * @return the install plan node for the provided extension
     * @throws InstallException error when trying to install provided extension
     */
    private ModifableExtensionPlanNode installExtension(LocalExtension previousExtension,
        ExtensionDependency targetDependency, boolean dependency, String namespace) throws InstallException
    {
        notifyPushLevelProgress(2);

        try {
            // Check if the extension is already in local repository
            Extension extension = resolveExtension(targetDependency);

            notifyStepPropress();

            try {
                return installExtension(previousExtension, extension, dependency, namespace, targetDependency);
            } catch (Exception e) {
                throw new InstallException("Failed to resolve extension dependency", e);
            }
        } finally {
            notifyPopLevelProgress();
        }
    }

    /**
     * @param extensions the extensions containing the dependencies for which to merge the constraints
     * @param dependencyId the id of the dependency
     * @param previousMergedVersionContraint if not null it's merged with the provided extension dependencies version
     *            constraints
     * @return the merged version constraint
     * @throws IncompatibleVersionConstraintException the provided version constraint is compatible with the provided
     *             version constraint
     */
    private VersionConstraint mergeVersionConstraints(Collection< ? extends Extension> extensions, String dependencyId,
        VersionConstraint previousMergedVersionContraint) throws IncompatibleVersionConstraintException
    {
        VersionConstraint mergedVersionContraint = previousMergedVersionContraint;

        if (extensions != null) {
            for (Extension extension : extensions) {
                ExtensionDependency dependency = getDependency(extension, dependencyId);

                if (dependency != null) {
                    if (mergedVersionContraint == null) {
                        mergedVersionContraint = dependency.getVersionConstraint();
                    } else {
                        mergedVersionContraint = mergedVersionContraint.merge(dependency.getVersionConstraint());
                    }
                }
            }
        }

        return mergedVersionContraint;
    }

    /**
     * Extract extension with the provided id from the provided extension.
     * 
     * @param extension the extension
     * @param dependencyId the id of the dependency
     * @return the extension dependency or null if none has been found
     */
    private ExtensionDependency getDependency(Extension extension, String dependencyId)
    {
        for (ExtensionDependency dependency : extension.getDependencies()) {
            if (dependency.getId().equals(dependencyId)) {
                return dependency;
            }
        }

        return null;
    }

    /**
     * Install provided extension.
     * 
     * @param previousExtension the previous installed version of the extension to install
     * @param extensionId the identifier of the extension to install
     * @param dependency indicate if the extension is installed as a dependency
     * @param namespace the namespace where to install the extension
     * @return the install plan node for the provided extension
     * @throws InstallException error when trying to install provided extension
     */
    private ModifableExtensionPlanNode installExtension(LocalExtension previousExtension, ExtensionId extensionId,
        boolean dependency, String namespace) throws InstallException
    {
        notifyPushLevelProgress(2);

        try {
            // Check is the extension is already in local repository
            Extension extension = resolveExtension(extensionId);

            notifyStepPropress();

            try {
                return installExtension(previousExtension, extension, dependency, namespace, null);
            } catch (Exception e) {
                throw new InstallException("Failed to resolve extension", e);
            }
        } finally {
            notifyPopLevelProgress();
        }
    }

    /**
     * @param extensionId the identifier of the extension to install
     * @return the extension
     * @throws InstallException error when trying to resolve extension
     */
    private Extension resolveExtension(ExtensionId extensionId) throws InstallException
    {
        // Check is the extension is already in local repository
        Extension extension;
        try {
            extension = this.localExtensionRepository.resolve(extensionId);
        } catch (ResolveException e) {
            this.logger.debug("Can't find extension in local repository, trying to download it.", e);

            // Resolve extension
            try {
                extension = this.repositoryManager.resolve(extensionId);
            } catch (ResolveException e1) {
                throw new InstallException(MessageFormat.format("Failed to resolve extension [{0}]", extensionId), e1);
            }
        }

        return extension;
    }

    /**
     * @param extensionDependency describe the extension to install
     * @return the extension
     * @throws InstallException error when trying to resolve extension
     */
    private Extension resolveExtension(ExtensionDependency extensionDependency) throws InstallException
    {
        // Check is the extension is already in local repository
        Extension extension;
        try {
            extension = this.localExtensionRepository.resolve(extensionDependency);
        } catch (ResolveException e) {
            this.logger.debug("Can't find extension dependency in local repository, trying to download it.", e);

            // Resolve extension
            try {
                extension = this.repositoryManager.resolve(extensionDependency);
            } catch (ResolveException e1) {
                throw new InstallException(MessageFormat.format("Failed to resolve extension dependency [{0}]",
                    extensionDependency), e1);
            }
        }

        return extension;
    }

    /**
     * @param previousExtension the previous installed version of the extension to install
     * @param extension the new extension to install
     * @param dependency indicate if the extension is installed as a dependency
     * @param namespace the namespace where to install the extension
     * @return the install plan node for the provided extension
     * @param initialDependency the initial dependency used to resolve the extension
     * @throws InstallException error when trying to install provided extension
     */
    private ModifableExtensionPlanNode installExtension(LocalExtension previousExtension, Extension extension,
        boolean dependency, String namespace, ExtensionDependency initialDependency) throws InstallException
    {
        List< ? extends ExtensionDependency> dependencies = extension.getDependencies();

        notifyPushLevelProgress(dependencies.size() + 1);

        try {
            List<ModifableExtensionPlanNode> children = null;
            if (!dependencies.isEmpty()) {
                children = new ArrayList<ModifableExtensionPlanNode>();
                for (ExtensionDependency dependencyDependency : extension.getDependencies()) {
                    installExtensionDependency(dependencyDependency, namespace, children);

                    notifyStepPropress();
                }
            }

            ModifableExtensionPlanNode node =
                initialDependency != null ? new ModifableExtensionPlanNode(initialDependency,
                    initialDependency.getVersionConstraint()) : new ModifableExtensionPlanNode();

            node.children = children;
            node.action =
                new DefaultExtensionPlanAction(extension, previousExtension, previousExtension != null
                    ? DefaultExtensionPlanAction.Action.UPGRADE : DefaultExtensionPlanAction.Action.INSTALL, namespace,
                    dependency);

            return node;
        } finally {
            notifyPopLevelProgress();
        }
    }
}
