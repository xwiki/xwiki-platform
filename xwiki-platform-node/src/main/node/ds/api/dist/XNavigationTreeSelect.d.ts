import { DocumentReference, SpaceReference } from '@xwiki/platform-model-api';
/**
 * @since 0.15
 * @beta
 */
type NavigationTreeSelectProps = {
    label: string;
    help?: string;
    currentPageReference?: DocumentReference;
    modelValue?: SpaceReference;
    /**
     * Whether to include terminal pages as select options (default: false).
     * @since 0.16
     * @beta
     */
    includeTerminals?: boolean;
};
/**
 * Default props values for NavigationTreeSelect implementations.
 * @since 0.16
 * @beta
 */
declare const navigationTreeSelectPropsDefaults: {
    includeTerminals: boolean;
};
export type { NavigationTreeSelectProps };
export { navigationTreeSelectPropsDefaults };
