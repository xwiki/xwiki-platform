import { Document } from '../api/document';
/**
 * @since 0.1
 * @beta
 */
export declare class JSONLDDocument implements Document {
    protected jsonld: any;
    constructor(jsonld: any);
    getIdentifier(): string;
    setIdentifier(identifier: string): void;
    getName(): string;
    setName(name: string): void;
    getText(): string;
    setText(text: string): void;
    get(fieldName: string): any;
    set(fieldName: string, value: any): void;
    getSource(): any;
}
