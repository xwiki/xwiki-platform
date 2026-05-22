import { LinkEditionContext, LinkType } from '../misc/linkEditionCtx';
import { ReactElement } from 'react';
type SearchBoxProps = {
    /**
     * The search box's initial value
     */
    initialValue?: string;
    /**
     * The search box's placeholder (when empty)
     */
    placeholder: string;
    /**
     * Link edition context, required for validate raw entity references on submit
     *
     * @since 18.0.0RC1
     */
    linkEditionCtx: LinkEditionContext | null;
    /**
     * Perform a search
     *
     * @param query - A user query (text)
     *
     * @returns Suggestions matching the provided query
     */
    getSuggestions: (query: string) => Promise<LinkSuggestion[] | false>;
    /**
     * Render a single suggestion
     *
     * @param suggestion -
     *
     * @returns A JSX element
     */
    renderSuggestion: (suggestion: LinkSuggestion) => ReactElement;
    /**
     * Triggered when a result is selected in the list of suggestions
     */
    onSelect: (url: string) => void;
    /**
     * Triggered when a result is submitted by the user
     *
     * e.g. when the user uses the `<Enter>` key on an URL
     */
    onSubmit: (url: string) => void;
};
/**
 * @since 18.4.0RC-1
 * @beta
 */
type LinkSuggestion = {
    title: string;
    segments: string[];
    reference: string;
    url: string;
    type: LinkType;
};
/**
 * This component provides a search (text) field with a dropdown for the results
 *
 * Raw URLs input is supported, as well as raw entity references.
 *
 * @see SearchBoxProps
 */
export declare const SearchBox: React.FC<SearchBoxProps>;
export type { LinkSuggestion, SearchBoxProps };
