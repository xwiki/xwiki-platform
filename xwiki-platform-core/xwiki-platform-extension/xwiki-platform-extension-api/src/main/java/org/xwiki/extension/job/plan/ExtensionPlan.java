package org.xwiki.extension.job.plan;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import org.xwiki.extension.job.ExtensionRequest;
import org.xwiki.extension.job.internal.DefaultJobStatus;
import org.xwiki.logging.LoggerManager;
import org.xwiki.observation.ObservationManager;

public class ExtensionPlan<R extends ExtensionRequest> extends DefaultJobStatus<R>
{
    private List<ExtensionPlanNode> extensionTree = new ArrayList<ExtensionPlanNode>();

    private List<ExtensionPlanAction> extensionActions;

    public ExtensionPlan(R request, String id, ObservationManager observationManager, LoggerManager loggerManager,
        List<ExtensionPlanNode> extensionTree)
    {
        super(request, id, observationManager, loggerManager);

        this.extensionTree = extensionTree;
    }

    private void fillExtensionActions(List<ExtensionPlanAction> extensions, Collection<ExtensionPlanNode> nodes)
    {
        for (ExtensionPlanNode node : nodes) {
            fillExtensionActions(extensions, node.getChildren());

            extensions.add(node.getAction());
        }
    }

    public Collection<ExtensionPlanAction> getExtensionActions()
    {
        if (getState() != State.FINISHED) {
            List<ExtensionPlanAction> extensions = new ArrayList<ExtensionPlanAction>();
            fillExtensionActions(extensions, this.extensionTree);

            return extensions;
        } else {
            if (this.extensionActions == null) {
                this.extensionActions = new ArrayList<ExtensionPlanAction>();
                fillExtensionActions(this.extensionActions, this.extensionTree);
            }

            return Collections.unmodifiableCollection(this.extensionActions);
        }
    }

    public Collection<ExtensionPlanNode> getExtensionTree()
    {
        return Collections.unmodifiableCollection(this.extensionTree);
    }
}
