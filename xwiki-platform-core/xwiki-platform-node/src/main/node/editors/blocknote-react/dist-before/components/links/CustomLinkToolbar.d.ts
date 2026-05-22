import { LinkEditionHandler } from './linkEdition';
import { LinkToolbarProps } from '@blocknote/react';
export type CustomLinkToolbarProps = {
    linkToolbarProps: LinkToolbarProps;
    linkEditionFn: LinkEditionHandler;
};
export declare const CustomLinkToolbar: React.FC<CustomLinkToolbarProps>;
