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
import type { Collaboration } from "@xwiki/platform-collaboration-api";
import type { MacroWithUnknownParamsType } from "@xwiki/platform-macros-api";
import type { DocumentReference } from "@xwiki/platform-model-api";
import type { Container } from "inversify";

/**
 * How the editor looks.
 *
 * @since 18.10.0RC1
 * @beta
 */
type EditorAppearance = {
  /**
   * The display theme of the editor UI (toolbars, menus, ...). It follows the light or dark color scheme preferred by
   * the user (as set in the operating system or the browser) when not specified.
   */
  theme?: "light" | "dark";

  /**
   * Who styles the edited content: the editor (the default), or the page hosting the editor, e.g. to have the content
   * look the same in edit and view modes.
   */
  contentStyles?: "editor" | "host";
};

/**
 * The options of the code blocks feature.
 *
 * @since 18.10.0RC1
 * @beta
 */
type CodeBlocksFeatureOptions = {
  /**
   * The CSS classes (space separated) to add to the code blocks, e.g. to style them like the code snippets displayed in
   * view mode.
   */
  className?: string;
};

/**
 * The options of the macro feature.
 *
 * @since 18.10.0RC1
 * @beta
 */
type MacroFeatureOptions = {
  /**
   * The client-rendered macros, if any. The macros rendered server-side are inserted and edited with the
   * `MacroEditor` bound in the {@link EditorOptions.serviceContainer}, when there's one.
   */
  list?: MacroWithUnknownParamsType[];

  /**
   * The output of the server-rendered macro calls of the initial content, keyed by id: the block id for a block macro
   * call, the `id` property for an inline macro call or for a macro call nested in the parameters or content of another
   * macro call. Each output is associated with the macro call (JSON) it was rendered for, and is displayed only while
   * the macro call is unchanged. The output of the macro calls is kept outside the edited content because it is
   * rendered with the rights of the current user, so it must not be shared with the other users editing the same
   * content in realtime.
   *
   * @since 18.10.0
   */
  outputs?: Record<string, { call: string; output: unknown[] }>;
};

/**
 * The options of each editor feature, keyed by feature id (see `EditorFeature`). Setting the options of a feature to
 * `false` disables it. An interface, so that the packages adding editor features can declare their options here (with
 * TypeScript declaration merging).
 *
 * @since 18.10.0RC1
 * @beta
 */
interface EditorFeatures {
  /**
   * The code blocks are enabled when this is missing. `false` leaves BlockNote's own code blocks, without syntax
   * highlighting, nor support for the languages that its language selector doesn't know. Whether the content can hold
   * code blocks at all depends on the syntax.
   */
  codeBlocks?: CodeBlocksFeatureOptions | false;

  /**
   * The images are enabled when this is missing. `false` disables the image suggestions, the image toolbar and the
   * image panel, even when an `ImageEditor` is bound, and leaves BlockNote's own image blocks: the "Image" item of the
   * slash menu is BlockNote's own, which inserts an empty image block. Whether the content can hold images at all
   * depends on the syntax.
   */
  images?: false;

  /**
   * The links are enabled when this is missing. `false` disables the actions to insert and edit links (the slash menu
   * item, the link suggestions, the button creating a link, the link toolbar), even when a `LinkEditor` is bound. The
   * links already in the content are still displayed. The options of the links feature are those of the `LinkEditor`,
   * declared by the integration binding it (e.g. blocknote-headless declares those of its link modal).
   */
  links?: object | false;

  /**
   * The macros are enabled when this is missing. `false` disables them: no macro calls in the schema of the editor, so
   * content holding macro calls can't be edited, and no action to insert or edit a macro, even when a `MacroEditor` is
   * bound.
   */
  macros?: MacroFeatureOptions | false;

  /**
   * The content pasted (or dropped) in the editor is filtered when this is missing: the metadata entries it holds are
   * kept only when their key, bound in the service container, accepts them (see `BlockMetadataKey.filterPasted`).
   * `false` keeps the pasted content as is, metadata included, which lets the pasted content set metadata that the user
   * doesn't see (e.g. the target of a link): only for an integration that filters the pasted content itself.
   *
   * @since 18.10.0
   */
  pasteFilter?: false;

  /**
   * The subscript and superscript text styles are enabled when this is missing (and the syntax supports them). `false`
   * removes them from the editor, with their toolbar buttons: content using them can't be edited.
   */
  textStyles?: false;

  /**
   * The block types and text styles produced by the XWiki rendering (definition list items, groups, raw content,
   * verbatim text, XWiki parameters) are in the editor when this is missing and the integration binds the
   * `xwikiRenderingFeature`. `false` removes them: content holding them can't be edited.
   */
  xwikiRendering?: false;
}

/**
 * The configuration of a BlockNote editor instance.
 *
 * Where an option goes:
 *
 * 1. Options are named after what the editor does, not after the library implementing it: no BlockNote (or Mantine)
 *    types and no options forwarded as is to BlockNote. A BlockNote setting that an integration needs becomes an
 *    option of its own.
 * 2. An option identifying the editor instance, changing how the editor is created, or used by several features,
 *    goes at the top level (e.g. {@link EditorOptions.collaboration}, {@link EditorOptions.upload}). An option used by
 *    a single feature goes in the options of that feature, under {@link EditorOptions.features}. A feature adds
 *    something to the editor and can be turned off without affecting the other features: it reads only its own
 *    options.
 * 3. Services are not passed: the editor takes them from {@link EditorOptions.serviceContainer}. Only the options that
 *    change how a service is used are passed, and only when they can't be derived from the other options.
 * 4. An option that can be derived from another one is not passed (e.g. the references of the links are resolved
 *    relative to {@link EditorOptions.sourceDocumentReference}).
 * 5. Each option is declared once, by the lowest layer using it (e.g. the options of the link modal are declared by
 *    the Vue integration that shows it).
 * 6. Feature options should be JSON-serializable, so that they can come from a server-side configuration.
 * 7. Options hold values, not behavior, which is injected through {@link EditorOptions.serviceContainer}. Callbacks
 *    are used only at a framework boundary or for the editor's own events.
 *
 * See the README of this package for the architecture the editor and its features follow.
 *
 * @since 18.10.0RC1
 * @beta
 */
type EditorOptions = {
  /**
   * The Inversify container the editor takes the services and components it needs from.
   */
  serviceContainer: Container;

  /**
   * The name of the editor instance, usually the name of the form field it is attached to. It tells apart the editor
   * instances editing different contents of the same document (e.g. a form with several BlockNote fields). When
   * editing in realtime, the editor instances with the same name, in the same collaboration session, edit the same
   * content. Realtime collaboration is disabled without a name.
   */
  name?: string;

  /**
   * The reference of the document holding the edited content: the files are uploaded to it, and the references of the
   * linked pages, attachments and images are relative to it, so that they keep working when the document is renamed
   * or copied. The current document (as provided by the `DocumentService`) when not specified.
   */
  sourceDocumentReference?: DocumentReference;

  /**
   * The id of the syntax of the edited content (e.g. "xwiki/2.1"), which is normally the syntax of the source document.
   * Its configuration, looked up in the {@link EditorOptions.serviceContainer}, determines the editor features that
   * are available, since only what the syntax can represent can be saved. The minimal syntax, which disables most
   * editor features, is used when the syntax has no configuration.
   */
  syntax: string;

  /**
   * The language of the editor UI (menus, toolbars, placeholders), usually the language of the page hosting the
   * editor.
   */
  lang: EditorLanguage;

  /**
   * The accessible name of the editing area (its `aria-label`), announced by the screen readers to tell which content
   * is being edited.
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
   * Whether the user can upload files to the source document: by pasting or dropping them in the content, or with the
   * upload actions (e.g. of the link and image menus). The files are stored with the `FileUploader` bound in the
   * {@link EditorOptions.serviceContainer}, or with the `AttachmentsService` otherwise. Enabled by default, as long as
   * there's something to store the files with.
   */
  upload?: boolean;

  /**
   * The options of each editor feature.
   */
  features?: EditorFeatures;
};

export type {
  CodeBlocksFeatureOptions,
  EditorAppearance,
  EditorFeatures,
  EditorOptions,
  MacroFeatureOptions,
};
