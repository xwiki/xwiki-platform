/*
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
// The Inplace edit mode is looking for a RequireJS module named "xwiki-<editorId>-inline".
define("xwiki-blocknote-inline", ["jquery", "xwiki-blocknote"], function ($, BlockNote) {
  $(document).on("xwiki:actions:edit", function (event, config) {
    if (config && config.contentType === "org.xwiki.rendering.syntax.SyntaxContent" && config.editMode === "wysiwyg") {
      createEditor(event.target, config);
    }
  });

  async function createEditor(container, config) {
    const editorElement = document.createElement("div");
    editorElement.classList.add("xwiki-blocknote-wrapper");
    editorElement.dataset.name = config.editorName;
    editorElement.dataset.value = config.document.renderedContent;
    editorElement.dataset.form = config.formId;
    editorElement.dataset.inputSyntax = `${BlockNote.syntax.type}/${BlockNote.syntax.version}`;
    editorElement.dataset.outputSyntax = config.document.syntax;
    editorElement.dataset.startupFocus = config.startupFocus;
    editorElement.dataset.sourceDocumentReference = XWiki.Model.serialize(config.document.documentReference);
    container.after(editorElement);
    const blockNote = await BlockNote.create(editorElement);

    const beforeSubmitHandler = beforeSubmit.bind(null, blockNote, config);
    $(document).on("xwiki:actions:beforeSave xwiki:actions:beforePreview", beforeSubmitHandler);

    $(document).one("xwiki:actions:view", () => {
      $(document).off("xwiki:actions:beforeSave xwiki:actions:beforePreview", beforeSubmitHandler);
      BlockNote.destroy(editorElement);
      editorElement.remove();
    });

    config.deferred.resolve(config.document);
  }

  function beforeSubmit(blockNote, config) {
    config.document.renderedContent = blockNote.data.value;
    // Delete the document content field that is not used so that the in-place editor knows what to submit on save.
    delete config.document.content;
    // Update the source syntax in case it has changed.
    config.document.syntax = blockNote.data.outputSyntax;
  }

  return {
    getRenderingConfig: function () {
      return {
        outputSyntax: BlockNote.syntax,
        // Currently, only the macro transformations are protected and thus can be edited.
        // See XRENDERING-78: Add markers to modified XDOM by Transformations/Macros
        transformations: ["macro"],
      };
    },
  };
});
