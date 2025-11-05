import { TreeNode } from '@xwiki/cristal-fn-utils';
/**
 * Represents a TreeNode that can be displayed in a Tree component.
 * @since 0.23
 * @beta
 */
type DisplayableTreeNode = TreeNode<{
    id: string;
    label: string;
    url?: string;
    activatable?: boolean;
}>;
/**
 * Props of the Tree component.
 * @since 0.23
 * @beta
 */
type TreeProps = {
    /**
     * Node that contains the nodes to display.
     */
    rootNode: DisplayableTreeNode;
    /**
     * Whether to display the root node itself (default: false).
     */
    showRootNode?: boolean;
    /**
     * Model value that contains the id of the current activated node.
     */
    activated?: string;
    /**
     * Model value that contains the ids of the current opened nodes.
     */
    opened?: string[];
};
export type { DisplayableTreeNode, TreeProps };
