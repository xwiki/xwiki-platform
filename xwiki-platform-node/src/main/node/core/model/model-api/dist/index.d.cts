/**
 * See the LICENSE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
/**
 * @since 0.12
 * @beta
 */
declare enum EntityType {
    WIKI = 0,
    SPACE = 1,
    DOCUMENT = 2,
    ATTACHMENT = 3
}
/**
 * @since 0.12
 * @beta
 */
interface BaseEntityReference {
    type: EntityType;
}
/**
 * @since 0.12
 * @beta
 */
declare class WikiReference implements BaseEntityReference {
    type: EntityType.WIKI;
    name: string;
    constructor(name: string);
}
/**
 * @since 0.12
 * @beta
 */
declare class SpaceReference implements BaseEntityReference {
    type: EntityType.SPACE;
    wiki?: WikiReference;
    names: string[];
    constructor(wiki?: WikiReference, ...names: string[]);
}
/**
 * @since 0.12
 * @beta
 */
declare class DocumentReference implements BaseEntityReference {
    type: EntityType.DOCUMENT;
    space?: SpaceReference;
    name: string;
    /**
     * Indicates whether the current document reference is terminal.
     * @since 0.13
     * @beta
     */
    terminal: boolean;
    constructor(name: string, space?: SpaceReference, terminal?: boolean);
}
/**
 * @since 0.12
 * @beta
 */
declare class AttachmentReference implements BaseEntityReference {
    type: EntityType.ATTACHMENT;
    name: string;
    document: DocumentReference;
    /**
     * @since 0.22
     */
    private readonly _metadata;
    /**
     *
     * @param name - the name of the attachment
     * @param document - the document reference of the attachment
     * @param metadata - (since 0.22) the metadata of the attachment, can be used to store additional information
     *  about the attachment (e.g., additional information for a given backend)
     */
    constructor(name: string, document: DocumentReference, metadata?: Record<string, string>);
    get metadata(): Record<string, string>;
}
/**
 * @since 0.16
 * @beta
 */
type EntityReference = AttachmentReference | DocumentReference | SpaceReference | WikiReference;
export { AttachmentReference, type BaseEntityReference, DocumentReference, type EntityReference, EntityType, SpaceReference, WikiReference, };
