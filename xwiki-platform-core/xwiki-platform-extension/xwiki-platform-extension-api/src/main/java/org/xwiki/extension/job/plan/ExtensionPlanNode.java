package org.xwiki.extension.job.plan;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

public class ExtensionPlanNode
{
    private ExtensionPlanAction action;

    private Collection<ExtensionPlanNode> children;

    public ExtensionPlanNode(ExtensionPlanNode node)
    {
        this(node.getAction(), node.getChildren());
    }

    public ExtensionPlanNode(ExtensionPlanAction action)
    {
        this(action, null);
    }

    public ExtensionPlanNode(ExtensionPlanAction action, Collection<ExtensionPlanNode> children)
    {
        this.action = action;
        if (children != null) {
            this.children = new ArrayList<ExtensionPlanNode>(children);
        } else {
            this.children = Collections.emptyList();
        }
    }

    public ExtensionPlanAction getAction()
    {
        return this.action;
    }

    public Collection<ExtensionPlanNode> getChildren()
    {
        return this.children;
    }
}
