/**
 * See the NOTICE file distributed with this work for additional
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
import type { EditorType } from "./blocknote";
import type { EditorOptions } from "./editorOptions";
import type { BlockMetadataStore } from "./misc/blockMetadata";
import type { Container } from "inversify";

/**
 * A mounted editor instance, as seen by the services acting on it. The services are shared by all the editor instances
 * of the page, so each call gets the instance it acts on (as its first argument), rather than the integration creating
 * a closure per instance.
 *
 * @since 18.10.0RC1
 * @beta
 */
type EditorInstance = {
  /**
   * The BlockNote editor, to read and change the edited content.
   */
  editor: EditorType;

  /**
   * The configuration of the editor instance, e.g. to resolve references relative to its
   * {@link EditorOptions.sourceDocumentReference}. It doesn't hold the options that are resolved for the editor
   * instance, which are exposed by the editor instance itself, so that there's only one place to read them from.
   */
  options: Readonly<Omit<EditorOptions, "serviceContainer" | "metadata">>;

  /**
   * The data kept about the blocks of the edited content, outside of the content itself: the
   * {@link EditorOptions.metadata} store when specified, and a store created for the editor instance otherwise.
   */
  metadata: BlockMetadataStore;

  /**
   * The service container of the editor instance: a child of {@link EditorOptions.serviceContainer} created for the
   * editor instance, so it provides the shared services, and the integration can bind in it what is specific to the
   * editor instance. The services acting on the editor instance take what they need from it.
   */
  serviceContainer: Container;
};

export type { EditorInstance };
