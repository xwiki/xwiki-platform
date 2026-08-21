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

import BlockNoteHeadlessForTest from "./BlockNoteHeadlessForTest.vue";
import type {
  MountOptions,
  MountResult,
} from "@playwright/experimental-ct-vue";

type BlockNoteHeadlessProps = InstanceType<
  typeof BlockNoteHeadlessForTest
>["$props"];

export function mountBlockNoteHeadless(
  mount: <HooksConfig, Component = unknown>(
    component: Component,
    options?: MountOptions<HooksConfig, Component>,
  ) => Promise<MountResult<Component>>,
  props: Omit<BlockNoteHeadlessProps, "editorProps"> & {
    editorProps: Omit<BlockNoteHeadlessProps["editorProps"], "label" | "lang">;
  },
): Promise<MountResult<typeof BlockNoteHeadlessForTest>> {
  const cpProps: BlockNoteHeadlessProps = {
    ...props,
    editorProps: {
      ...props.editorProps,
      label: "Editor",
      lang: "en",
    },
  };

  return mount(BlockNoteHeadlessForTest, {
    props: cpProps,
  });
}
