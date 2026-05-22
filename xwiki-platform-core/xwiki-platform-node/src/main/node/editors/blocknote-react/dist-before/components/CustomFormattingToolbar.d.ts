import { ImageEditionOverrideFn } from './images/CustomImageToolbar';
import { LinkEditionContext } from '../misc/linkEditionCtx';
import { LinkEditionHandler } from './links/linkEdition';
import { FormattingToolbarProps } from '@blocknote/react';
type CustomFormattingToolbarProps = {
    formattingToolbarProps: FormattingToolbarProps;
    linkEditionCtx: LinkEditionContext;
    linkEditionHandler: LinkEditionHandler;
    imageEditionOverrideFn?: ImageEditionOverrideFn;
};
export declare const CustomFormattingToolbar: React.FC<CustomFormattingToolbarProps>;
export type { CustomFormattingToolbarProps };
