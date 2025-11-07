import { DocumentReference } from '@xwiki/platform-model-api';
import { NavigationTreeNode } from '@xwiki/platform-navigation-tree-api';
/**
 * @since 0.15
 * @beta
 */
type NavigationTreeProps = {
    clickAction?: (node: NavigationTreeNode) => void;
    currentPageReference?: DocumentReference;
    /**
     * Whether to include terminal pages in the tree (default: true).
     * @since 0.16
     * @beta
     */
    includeTerminals?: boolean;
    /**
     * Whether to display a root node (default: false).
     * @since 0.20
     * @beta
     */
    showRootNode?: boolean;
};
/**
 * Default props values for NavigationTree implementations.
 * @since 0.16
 * @beta
 */
declare const navigationTreePropsDefaults: {
    includeTerminals: boolean;
};
export type { NavigationTreeProps };
export { navigationTreePropsDefaults };
