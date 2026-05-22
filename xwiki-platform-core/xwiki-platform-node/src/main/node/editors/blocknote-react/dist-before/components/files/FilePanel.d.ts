import { EditorType } from '../../blocknote';
import { LinkEditionContext } from '../../misc/linkEditionCtx';
import { default as React } from 'react';
export type FilePanelProps = {
    editor: EditorType;
    blockId: string;
    linkEditionCtx: LinkEditionContext;
};
export declare const FilePanel: React.FC<FilePanelProps>;
