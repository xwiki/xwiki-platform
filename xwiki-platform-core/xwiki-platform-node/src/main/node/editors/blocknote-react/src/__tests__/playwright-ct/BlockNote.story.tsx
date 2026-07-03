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
import { depsContainerMock } from "./depsContainer.mock";
import { BlockNoteViewWrapper } from "../../components/BlockNoteViewWrapper";
import { useMemo } from "react";
import type { BlockNoteViewWrapperProps } from "../../components/BlockNoteViewWrapper";
import type { SyntaxConfig } from "@xwiki/platform-syntaxes-config";

export type BlockNoteForTestProps = Omit<
  BlockNoteViewWrapperProps,
  "lang" | "label" | "depsContainer"
> & { syntax?: SyntaxConfig };

export const BlockNoteForTest: React.FC<BlockNoteForTestProps> = (props) => {
  const depsContainer = useMemo(depsContainerMock, []);

  return (
    <BlockNoteViewWrapper
      lang="en"
      label="Some Label"
      depsContainer={depsContainer}
      {...props}
    />
  );
};
