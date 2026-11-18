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
import type {
  EditorFeatures,
  EditorOptions,
} from "@xwiki/platform-editors-blocknote-react";

/**
 * The options of the link feature, i.e. of the link modal shown by the link editor of the Vue integration.
 *
 * @since 18.10.0RC1
 * @beta
 */
type HeadlessLinkFeatureOptions = {
  /**
   * The link target types (by type) that the link modal doesn't offer for new links, while still using them to edit
   * the links they handle. Unknown types are ignored.
   */
  excludedTargetTypes?: string[];
};

/**
 * The configuration of a BlockNote editor instance integrated in Vue: the options of the editor itself (see
 * {@link EditorOptions} for where an option goes), and those of the Vue integration.
 *
 * @since 18.10.0RC1
 * @beta
 */
type HeadlessEditorOptions = Omit<EditorOptions, "features"> & {
  features?: EditorFeatures & {
    links?: HeadlessLinkFeatureOptions;
  };
};

export type { HeadlessEditorOptions, HeadlessLinkFeatureOptions };
