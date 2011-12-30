package org.xwiki.extension.job.internal;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.LocalExtension;
import org.xwiki.extension.ResolveException;
import org.xwiki.extension.UninstallException;
import org.xwiki.extension.job.UninstallRequest;
import org.xwiki.extension.job.plan.ExtensionPlan;
import org.xwiki.extension.job.plan.ExtensionPlanAction;
import org.xwiki.extension.job.plan.ExtensionPlanAction.Action;
import org.xwiki.extension.job.plan.ExtensionPlanNode;
import org.xwiki.extension.repository.LocalExtensionRepository;

/**
 * Create an Extension uninstallation plan.
 * 
 * @version $Id$
 */
public class UninstallPlanJob extends AbstractJob<UninstallRequest>
{
    /**
     * Error message used in exception throw when trying to uninstall an extension which is not installed.
     */
    private static final String EXCEPTION_NOTINSTALLED = "Extension [{0}] is not installed";

    /**
     * Error message used in exception throw when trying to uninstall an extension which is not installed.
     */
    private static final String EXCEPTION_NOTINSTALLEDNAMESPACE = EXCEPTION_NOTINSTALLED + " on namespace [{1}]";

    /**
     * Used to manipulate local repository.
     */
    @Inject
    private LocalExtensionRepository localExtensionRepository;

    /**
     * The install plan.
     */
    private List<ExtensionPlanNode> extensionTree = new ArrayList<ExtensionPlanNode>();

    @Override
    protected DefaultJobStatus<UninstallRequest> createNewStatus(UninstallRequest request)
    {
        return new ExtensionPlan<UninstallRequest>(request, getId(), this.observationManager, this.loggerManager,
            this.extensionTree);
    }

    @Override
    protected void start() throws Exception
    {
        List<ExtensionId> extensions = getRequest().getExtensions();

        notifyPushLevelProgress(extensions.size());

        try {
            for (ExtensionId extensionId : extensions) {
                if (extensionId.getVersion() != null) {
                    LocalExtension localExtension = (LocalExtension) this.localExtensionRepository.resolve(extensionId);

                    if (getRequest().hasNamespaces()) {
                        uninstallExtension(localExtension, getRequest().getNamespaces(), this.extensionTree);
                    } else if (localExtension.getNamespaces() != null) {
                        // Duplicate the namespace list to avoid ConcurrentModificationException
                        uninstallExtension(localExtension, new ArrayList<String>(localExtension.getNamespaces()),
                            this.extensionTree);
                    } else {
                        uninstallExtension(localExtension, (String) null, this.extensionTree);
                    }
                } else {
                    if (getRequest().hasNamespaces()) {
                        uninstallExtension(extensionId.getId(), getRequest().getNamespaces(), this.extensionTree);
                    } else {
                        uninstallExtension(extensionId.getId(), (String) null, this.extensionTree);
                    }
                }

                notifyStepPropress();
            }
        } finally {
            notifyPopLevelProgress();
        }
    }

    /**
     * @param extensionId the identifier of the extension to uninstall
     * @param namespaces the namespaces from where to uninstall the extension
     * @param parentBranch the children of the parent {@link ExtensionPlanNode}
     * @throws UninstallException error when trying to uninstall provided extensions
     */
    private void uninstallExtension(String extensionId, Collection<String> namespaces,
        List<ExtensionPlanNode> parentBranch) throws UninstallException
    {
        notifyPushLevelProgress(namespaces.size());

        try {
            for (String namespace : namespaces) {
                uninstallExtension(extensionId, namespace, parentBranch);

                notifyStepPropress();
            }
        } finally {
            notifyPopLevelProgress();
        }
    }

    /**
     * @param extensionId the identifier of the extension to uninstall
     * @param namespace the namespace from where to uninstall the extension
     * @param parentBranch the children of the parent {@link ExtensionPlanNode}
     * @throws UninstallException error when trying to uninstall provided extension
     */
    private void uninstallExtension(String extensionId, String namespace, List<ExtensionPlanNode> parentBranch)
        throws UninstallException
    {
        LocalExtension localExtension = this.localExtensionRepository.getInstalledExtension(extensionId, namespace);

        if (localExtension == null) {
            throw new UninstallException(MessageFormat.format(EXCEPTION_NOTINSTALLED, extensionId));
        }

        try {
            uninstallExtension(localExtension, namespace, parentBranch);
        } catch (Exception e) {
            throw new UninstallException("Failed to uninstall extension", e);
        }
    }

    /**
     * @param localExtension the extension to uninstall
     * @param namespaces the namespaces from where to uninstall the extension
     * @param parentBranch the children of the parent {@link ExtensionPlanNode}
     * @throws UninstallException error when trying to uninstall provided extension
     */
    private void uninstallExtension(LocalExtension localExtension, Collection<String> namespaces,
        List<ExtensionPlanNode> parentBranch) throws UninstallException
    {
        for (String namespace : namespaces) {
            uninstallExtension(localExtension, namespace, parentBranch);
        }
    }

    /**
     * @param extensions the local extensions to uninstall
     * @param namespace the namespaces from where to uninstall the extensions
     * @param parentBranch the children of the parent {@link ExtensionPlanNode}
     * @throws UninstallException error when trying to uninstall provided extensions
     */
    private void uninstallExtensions(Collection<LocalExtension> extensions, String namespace,
        List<ExtensionPlanNode> parentBranch) throws UninstallException
    {
        for (LocalExtension backardDependency : extensions) {
            uninstallExtension(backardDependency, namespace, parentBranch);
        }
    }

    /**
     * @param localExtension the extension to uninstall
     * @param namespace the namespace from where to uninstall the extension
     * @param parentBranch the children of the parent {@link ExtensionPlanNode}
     * @throws UninstallException error when trying to uninstall provided extension
     */
    private void uninstallExtension(LocalExtension localExtension, String namespace,
        List<ExtensionPlanNode> parentBranch) throws UninstallException
    {
        if (!localExtension.isInstalled()) {
            throw new UninstallException(MessageFormat.format(EXCEPTION_NOTINSTALLED, localExtension, namespace));
        } else if (namespace != null
            && (localExtension.getNamespaces() == null || !localExtension.getNamespaces().contains(namespace))) {
            throw new UninstallException(MessageFormat.format(EXCEPTION_NOTINSTALLEDNAMESPACE, localExtension,
                namespace));
        }

        // Log progression
        if (namespace != null) {
            this.logger.info("Resolving extension [{}] from namespace [{}]", localExtension, namespace);
        } else {
            this.logger.info("Resolving extension [{}]", localExtension);
        }

        notifyPushLevelProgress(2);

        try {
            // Uninstall backward dependencies
            List<ExtensionPlanNode> children = new ArrayList<ExtensionPlanNode>();
            try {
                if (namespace != null) {
                    uninstallExtensions(this.localExtensionRepository.getBackwardDependencies(localExtension.getId()
                        .getId(), namespace), namespace, children);
                } else {
                    for (Map.Entry<String, Collection<LocalExtension>> entry : this.localExtensionRepository
                        .getBackwardDependencies(localExtension.getId()).entrySet()) {
                        uninstallExtensions(entry.getValue(), entry.getKey(), children);
                    }
                }
            } catch (ResolveException e) {
                throw new UninstallException("Failed to resolve backward dependencies of extension [" + localExtension
                    + "]", e);
            }

            notifyStepPropress();

            ExtensionPlanAction action =
                new ExtensionPlanAction(localExtension, null, Action.UNINSTALL, namespace, null);
            parentBranch.add(new ExtensionPlanNode(action, children));
        } finally {
            notifyPopLevelProgress();
        }
    }
}
