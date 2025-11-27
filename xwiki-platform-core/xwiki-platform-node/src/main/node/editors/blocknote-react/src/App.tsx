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
import { BlockNoteViewWrapper } from "./components/BlockNoteViewWrapper";
import i18n from "./i18n";
import { MantineProvider } from "@mantine/core";
import "@mantine/core/styles.layer.css";
import { useEffect } from "react";
import type { BlockNoteViewWrapperProps } from "./components/BlockNoteViewWrapper";

export const App: React.FC<BlockNoteViewWrapperProps> = (props) => {
  useEffect(() => {
    if (props.lang !== i18n.language) {
      i18n.changeLanguage(props.lang);
    }
  }, [props.lang]);

  return (
    <MantineProvider>
      <BlockNoteViewWrapper {...props} />
    </MantineProvider>
  );
};
