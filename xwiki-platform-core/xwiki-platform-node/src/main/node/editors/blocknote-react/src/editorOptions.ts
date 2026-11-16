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
import type { EditorLanguage } from "./blocknote";
import type { ContextForMacros } from "./blocknote/utils";
import type { ImageEditionOverrideFn } from "./components/images/CustomImageToolbar";
import type { ImageEditionHooks } from "./components/images/imageEditionHooks";
import type { ImageInsertionOverrideFn } from "./components/images/imageInsertionOverride";
import type { LinkEditionHooks } from "./components/links/linkEditionHooks";
import type { Collaboration } from "@xwiki/platform-collaboration-api";
import type { MacroWithUnknownParamsType } from "@xwiki/platform-macros-api";
import type { DocumentReference } from "@xwiki/platform-model-api";
import type { SyntaxConfig } from "@xwiki/platform-syntaxes-config";
import type { Container } from "inversify";

/**
 * How the editor looks.
 *
 * @since 18.10.0RC1
 * @beta
 */
type EditorAppearance = {
  /**
   * The display theme of the editor UI (toolbars, menus, ...), light by default.
   */
  theme?: "light" | "dark";

  /**
   * Who styles the edited content: the editor (the default), or the page hosting the editor, e.g. to have the content
   * look the same in edit and view modes.
   */
  contentStyles?: "editor" | "host";

  /**
   * The CSS classes (space separated) to add to the code blocks, e.g. to style them like the code snippets displayed in
   * view mode.
   */
  codeBlockClassName?: string;
};

/**
 * How the files pasted, dropped or picked in the editor are uploaded.
 *
 * @since 18.10.0RC1
 * @beta
 */
type UploadOptions = {
  /**
   * Uploads the given file.
   *
   * @param file - the file to upload
   * @param blockId - the block the file is uploaded for, when there is one
   * @returns the URL of the uploaded file, or the (partial) block to replace the uploading block with
   */
  uploadFile: (
    file: File,
    blockId?: string,
  ) => Promise<string | Record<string, unknown>>;
};

/**
 * The options of the link feature.
 *
 * @since 18.10.0RC1
 * @beta
 */
type LinkFeatureOptions = {
  /**
   * Hooks to intercept the link creation / edition flow.
   */
  hooks?: LinkEditionHooks;
};

/**
 * The options of the image feature.
 *
 * @since 18.10.0RC1
 * @beta
 */
type ImageFeatureOptions = {
  /**
   * Replaces the default image edition UI (i.e. the edition action of the image toolbar).
   */
  edition?: ImageEditionOverrideFn;

  /**
   * Replaces the default image insertion UI (i.e. browsing the images to insert, from the image quick action).
   */
  insertion?: ImageInsertionOverrideFn;

  /**
   * Hooks to intercept the image insertion / edition flow.
   */
  hooks?: ImageEditionHooks;
};

/**
 * The options of the macro feature.
 *
 * @since 18.10.0RC1
 * @beta
 */
type MacroFeatureOptions = {
  /**
   * The client-rendered macros. Optional: an integration that only uses the server-rendered macro blocks (edited and
   * inserted through {@link MacroFeatureOptions.ctx}) can omit it.
   */
  list?: MacroWithUnknownParamsType[];

  /**
   * How the macros are edited and inserted, and where their output comes from.
   */
  ctx: ContextForMacros;
};

/**
 * The options of each editor feature, keyed by feature.
 *
 * @since 18.10.0RC1
 * @beta
 */
type EditorFeatures = {
  links?: LinkFeatureOptions;
  images?: ImageFeatureOptions;

  /**
   * The macros are disabled when this is missing or `false`, since inserting and editing them requires an
   * integration.
   */
  macros?: MacroFeatureOptions | false;
};

/**
 * The configuration of a BlockNote editor instance.
 *
 * Where an option goes:
 *
 * 1. Options are named after what the editor does, not after the library implementing it: no BlockNote (or Mantine)
 *    types and no options forwarded as is to BlockNote. A BlockNote setting that an integration needs becomes an
 *    option of its own.
 * 2. An option identifying the editor instance, or used by several features, goes at the top level. An option used
 *    by a single feature goes in the options of that feature, under {@link EditorOptions.features}.
 * 3. Services are not passed: the editor takes them from {@link EditorOptions.depsContainer}. Only the options that
 *    change how a service is used are passed, and only when they can't be derived from the other options.
 * 4. An option that can be derived from another one is not passed (e.g. the references of the links are resolved
 *    relative to {@link EditorOptions.document}).
 * 5. Each option is declared once, by the lowest layer using it (e.g. the options of the link modal are declared by
 *    the Vue integration that shows it).
 * 6. Feature options should be JSON-serializable, so that they can come from a server-side configuration.
 *
 * @since 18.10.0RC1
 * @beta
 */
type EditorOptions = {
  /**
   * The container the editor takes the services it needs from.
   */
  depsContainer: Container;

  /**
   * The name of the editor instance, usually the name of the form field it is attached to. It identifies the edited
   * content within the edited document: when editing in realtime, the editor instances with the same name, in the
   * same collaboration session, edit the same content. Realtime collaboration is disabled without a name.
   */
  name?: string;

  /**
   * The edited document: the files are uploaded to it, and the references of the linked pages, attachments and images
   * are relative to it. The current document (as provided by the `DocumentService`) when not specified.
   */
  document?: DocumentReference;

  /**
   * The syntax of the edited content, which determines the editor features that are available.
   */
  syntax: SyntaxConfig;

  /**
   * The language of the editor UI.
   */
  lang: EditorLanguage;

  /**
   * The accessible label of the editor.
   */
  label: string;

  /**
   * Whether to focus the editor, with the caret at the start of the content, as soon as it is created.
   */
  autofocus?: boolean;

  /**
   * How the editor looks.
   */
  appearance?: EditorAppearance;

  /**
   * The realtime collaboration session to join. Ignored when the editor instance has no
   * {@link EditorOptions.name}.
   */
  collaboration?: Collaboration;

  /**
   * How the files are uploaded. Uploading is disabled when not specified.
   */
  upload?: UploadOptions;

  /**
   * The options of each editor feature.
   */
  features?: EditorFeatures;
};

export type {
  EditorAppearance,
  EditorFeatures,
  EditorOptions,
  ImageFeatureOptions,
  LinkFeatureOptions,
  MacroFeatureOptions,
  UploadOptions,
};
