import { Processor } from 'unified';
/**
 * Extension to *partially* support Github's Front Matter (Markdown) syntax flavor
 *
 * Does **NOT** include some of GFM features like autolinks or footnotes, which are implemented differently in another
 * part of the code
 */
export declare function remarkPartialGfm(this: Processor): void;
