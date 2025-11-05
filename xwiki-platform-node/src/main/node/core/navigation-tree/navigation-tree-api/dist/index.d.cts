import { DocumentReference, SpaceReference } from '@xwiki/cristal-model-api';
/**
 * Description of a navigation tree node.
 * @since 0.10
 * @beta
 */
type NavigationTreeNode = {
    /** the id of a node, used by the NavigationTreeSource to access children */
    id: string;
    label: string;
    /** the location of the corresponding page on Cristal */
    location: SpaceReference | DocumentReference;
    url: string;
    has_children: boolean;
    /**
     * Whether this node corresponds to a terminal page.
     * @since 0.16
     * @beta
     */
    is_terminal: boolean;
};
/**
 * A NavigationTreeSource computes and returns a wiki's navigation tree.
 *
 * @since 0.10
 * @beta
 **/
interface NavigationTreeSource {
    /**
     * Returns the direct child nodes for a given page id in the navigation tree.
     * If the page id is omitted, returns the root nodes instead.
     *
     * @param id - the page id
     * @returns the descendants in the navigation tree
     */
    getChildNodes(id?: string): Promise<Array<NavigationTreeNode>>;
    /**
     * Returns the ids of the parents nodes for a given page.
     *
     * @param page - the reference to the page
     * @param includeTerminal - whether to include the final terminal page (default: true)
     * @param includeRootNode - whether to include a root node with empty id (default: false)
     * @returns the parents nodes ids
     * @since 0.20
     * @beta
     **/
    getParentNodesId(page: DocumentReference, includeTerminal?: boolean, includeRootNode?: boolean): Array<string>;
}
/**
 * A NavigationTreeSourceProvider returns the instance of NavigationTreeSource
 * matching the current wiki configuration.
 *
 * @since 0.10
 * @beta
 **/
interface NavigationTreeSourceProvider {
    /**
     * Returns the instance of NavigationTreeSource matching the current wiki
     * configuration.
     *
     * @returns the instance of NavigationTreeSource
     */
    get(): NavigationTreeSource;
}
/**
 * The component id of NavigationTreeSource.
 * @since 0.10
 * @beta
 */
declare const name = "NavigationTreeSource";
export { type NavigationTreeNode, type NavigationTreeSource, type NavigationTreeSourceProvider, name, };
