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
import { AutoSaver } from "./autoSaver";
import { TiptapCollabProvider } from "@hocuspocus/provider";
import { Extension } from "@tiptap/core";
import { default as Collaboration } from "@tiptap/extension-collaboration";
import { default as CollaborationCursor } from "@tiptap/extension-collaboration-cursor";
import * as Y from "yjs";
import type { CollaborationKitOptions } from "./collaborationKitOptions";

export const CollaborationKit = Extension.create<CollaborationKitOptions>({
  name: "cristalCollaborationKit",

  addStorage() {
    return {
      // The collaboration provider.
      provider: undefined,

      // The auto-saver.
      autoSaver: undefined,
    };
  },

  addExtensions() {
    // Initialize Y.Doc for collaborative editing.
    const ydoc = new Y.Doc();

    this.storage.provider = new TiptapCollabProvider({
      // See server.js from @xwiki/cristal-web
      baseUrl: this.options.baseUrl,
      name: this.options.channel,
      // The Y.Doc that is synchronized between editing users.
      document: ydoc,
      // Don't connect to the WebSocket server immediately. We'll connect when the editor is ready.
      connect: false,
    });

    this.storage.autoSaver = new AutoSaver(
      this.storage.provider,
      this.options.saveCallback,
    );

    return [
      Collaboration.configure({
        // Configure Y.Doc for collaboration.
        document: ydoc,
      }),
      CollaborationCursor.configure({
        provider: this.storage.provider,
        user: {
          ...this.options.user,
        },
      }),
    ];
  },

  onBeforeCreate() {
    const provider = this.storage.provider;
    // The onSynced callback ensures initial content is set only once using editor.setContent(), preventing repetitive
    // content loading on editor syncs.
    provider.on("synced", () => {
      if (!provider.document.getMap("config").get("initialContentLoaded")) {
        provider.document.getMap("config").set("initialContentLoaded", true);
        this.editor.commands.setContent(this.editor.options.content);
      }
    });
    provider.connect();

    this.editor.on("destroy", () => {
      provider.destroy();
    });
  },
});
