import { BlockOfType } from '../../blocknote';
import { LinkEditionContext } from '../../misc/linkEditionCtx';
export type ImageFilePanelProps = {
    currentBlock: BlockOfType<"image">;
    linkEditionCtx: LinkEditionContext;
};
export declare const ImageFilePanel: React.FC<ImageFilePanelProps>;
