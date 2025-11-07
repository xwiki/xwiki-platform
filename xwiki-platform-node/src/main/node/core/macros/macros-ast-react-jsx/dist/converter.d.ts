import { MacroBlock, MacroInlineContent } from '@xwiki/platform-macros-api';
import { RemoteURLParser, RemoteURLSerializer } from '@xwiki/platform-model-remote-url-api';
import { JSX, Ref } from 'react';
/**
 * @since 0.23
 * @beta
 */
export type MacroEditableZoneRef = {
    type: "block";
    ref: Ref<HTMLDivElement>;
} | {
    type: "inline";
    ref: Ref<HTMLSpanElement>;
};
/**
 * Converter that transforms a macro's returned AST to React JSX
 *
 * @since 0.23
 * @beta
 */
export declare class MacrosAstToReactJsxConverter {
    private readonly remoteURLParser;
    private readonly remoteURLSerializer;
    constructor(remoteURLParser: RemoteURLParser, remoteURLSerializer: RemoteURLSerializer);
    /**
     * Render a macro's AST blocks to JSX elements
     *
     * Will force re-render every time, even if the AST is exactly the same
     * (see the private `generateId` function for more informations)
     *
     * @param blocks - The blocks to render
     * @param editableZoneRef - The macro's editable zone reference
     *
     * @returns The JSX elements
     */
    blocksToReactJSX(blocks: MacroBlock[], editableZoneRef: MacroEditableZoneRef): JSX.Element[] | Error;
    /**
     * Render a macro's AST inline contents to JSX elements
     *
     * Will force re-render every time, even if the AST is exactly the same
     * (see the private `generateId` function for more informations)
     *
     * @param inlineContents - The inline contents to render
     * @param editableZoneRef - The macro's editable zone reference
     *
     * @returns The JSX elements
     */
    inlineContentsToReactJSX(inlineContents: MacroInlineContent[], editableZoneRef: MacroEditableZoneRef): JSX.Element[] | Error;
    private generateId;
    private convertBlocks;
    private convertInlineContents;
    private convertBlock;
    private convertBlockStyles;
    private convertInlineContent;
    private getTargetUrl;
}
