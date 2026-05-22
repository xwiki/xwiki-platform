import { BlockOfType } from '../../blocknote';
import { LinkEditionContext } from '../../misc/linkEditionCtx';
type CustomImageToolbarProps = {
    currentBlock: BlockOfType<"image">;
    linkEditionCtx: LinkEditionContext;
    imageEditionOverrideFn?: ImageEditionOverrideFn;
};
/**
 * Interceptor for the image edition mechanism
 *
 * @since 0.26
 * @beta
 */
type ImageEditionOverrideFn = (image: BlockOfType<"image">["props"], update: (updateResult: ImageUpdateResult) => void) => void;
/**
 * Result of an image update process, from `ImageEditionOverrideFn`
 *
 * @since 18.0.0RC1
 * @beta
 */
type ImageUpdateResult = {
    type: "update";
    updatedProps: Partial<BlockOfType<"image">["props"]>;
} | {
    type: "aborted";
};
export declare const CustomImageToolbar: React.FC<CustomImageToolbarProps>;
export type { CustomImageToolbarProps, ImageEditionOverrideFn, ImageUpdateResult, };
