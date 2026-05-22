import { LinkEditionContext } from '../../misc/linkEditionCtx';
export type ImageSelectorProps = {
    linkEditionCtx: LinkEditionContext;
    currentSelection?: string;
    onSelected: (url: string) => void;
};
export declare const ImageSelector: React.FC<ImageSelectorProps>;
