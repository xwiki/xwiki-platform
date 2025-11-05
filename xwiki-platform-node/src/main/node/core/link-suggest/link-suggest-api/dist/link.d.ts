import { LinkType } from './linkType';
/**
 * Minimal data required to describe a link.
 * @since 0.8
 * @beta
 */
type Link = {
    id: string;
    url: string;
    reference: string;
    label: string;
    hint: string;
    type: LinkType;
};
export { type Link };
