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
import { PastedMetadataFilterExtension } from "./pastedMetadata";
import type {
  EditorFeature,
  FeatureContext,
  FeatureContribution,
} from "../editorFeature";

/**
 * Filters the content pasted (or dropped) in the editor, which can come from anywhere (another page, another
 * application, a malicious web page): the metadata entries held by the pasted content are kept only when their key,
 * bound in the service container under `blockMetadataKeyName`, accepts them (see `BlockMetadataKey.filterPasted`).
 *
 * @since 18.10.0
 * @beta
 */
const pasteFilterFeature: EditorFeature = {
  id: "pasteFilter",
  contribute({ serviceContainer }: FeatureContext): FeatureContribution {
    return {
      blockNoteExtensions: [
        PastedMetadataFilterExtension({ serviceContainer }),
      ],
    };
  },
};

export { pasteFilterFeature };
