import { App, AsyncComponentLoader, AsyncComponentOptions, Component, ComponentPublicInstance } from 'vue';
/**
 * @since 0.1
 * @beta
 */
export interface DesignSystemLoader {
    loadDesignSystem(app: App): void;
}
/**
 * Register a component as async to lazy-load it. Avoiding loading resources
 * from all design systems at once (e.g., creating CSS conflicts).
 * @param app - the app to load the component to
 * @param name - the name of the Vue component
 * @param source - the source loaded (i.e., a call to import). The import must be
 * in the package where the component is as otherwise the import is made
 * relative to this package and the dependency is not found
 * @since 0.7
 * @beta
 */
export declare function registerAsyncComponent<T extends Component = {
    new (): ComponentPublicInstance;
}>(app: App, name: string, source: AsyncComponentLoader<T> | AsyncComponentOptions<T>): void;
