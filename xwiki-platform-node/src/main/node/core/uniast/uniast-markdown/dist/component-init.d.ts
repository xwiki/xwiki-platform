import { Container } from 'inversify';
/**
 * @since 0.22
 * @beta
 */
declare const markdownToUniAstConverterName = "MarkdownToUniAstConverter";
/**
 * @since 0.22
 * @beta
 */
declare const uniAstToMarkdownConverterName = "UniAstToMarkdownConverter";
/**
 * @since 0.22
 * @beta
 */
declare class ComponentInit {
    constructor(container: Container);
    private initXWikiFactory;
    private initNextcloudFactory;
    private initGitHubFactory;
    private initFileSystemFactory;
    /**
     * Registed the component in the container on demand.
     *
     * @param container - the container
     * @param name - the name of the component interface
     * @param component - the actual component to register
     * @param context - the context
     */
    private bindAndLoad;
}
export { ComponentInit, markdownToUniAstConverterName, uniAstToMarkdownConverterName, };
