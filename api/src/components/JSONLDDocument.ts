
import { Document } from '../api/document';

export class JSONLDDocument implements Document {
    protected jsonld : any;

    constructor(jsonld: any) {
        this.jsonld = jsonld;
    }

    getIdentifier(): string {
        return this.jsonld.identifier;
    }

    setIdentifier(identifier: string): void {
        this.jsonld.identifier = identifier;
    }

    getName(): string {
        return this.jsonld.name;
    }

    setName(name: string): void {
        this.jsonld.name = name;
    }

    getText(): string {
        return this.jsonld.text;
    }

    setText(text: string): void {
        this.jsonld.text = text;
    }

    get(fieldName: string) : any {
        return this.jsonld[fieldName]
    }

    set(fieldName: string, value: any): void {
        this.jsonld[fieldName] = value;
    }

    getSource(): any {
        return this.jsonld;
    }
}