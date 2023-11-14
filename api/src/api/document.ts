


export interface Document {

    getIdentifier() : string;
    setIdentifier(identifier : string) : void;

    getName() : string;
    setName(name : string) : void;

    getText() : string;
    setText(text : string) : void;

    get(fieldName : string) : any;
    set(fieldName : string, value: any) : void;

    /*
     * Allows to retrieve the source of the document
     */
    getSource() : any;
}