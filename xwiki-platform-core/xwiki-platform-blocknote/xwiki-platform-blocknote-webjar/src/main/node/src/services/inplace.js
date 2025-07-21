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
define("xwiki-blocknote-inline", ["jquery", "xwiki-blocknote", "css!xwiki-blocknote"], function ($, BlockNote) {
  $(document).on("xwiki:actions:edit", function (event, config) {
    if (config && config.contentType === "org.xwiki.rendering.syntax.SyntaxContent" && config.editMode === "wysiwyg") {
      createEditor(event.target, config);
    }
  });

  async function createEditor(container, config) {
    container.classList.add("xwiki-blocknote-wrapper");
    container.dataset.config = JSON.stringify({
      name: config.editorName,
      value: config.document.renderedContent,
      form: config.formId,
      inputSyntax: `${BlockNote.syntax.type}/${BlockNote.syntax.version}`,
      outputSyntax: config.document.syntax,
      startupFocus: config.startupFocus,
      sourceDocumentReference: XWiki.Model.serialize(config.document.documentReference),
    });
    const blockNote = await BlockNote.create(container);

    const beforeSubmitHandler = beforeSubmit.bind(null, blockNote, config);
    $(document).on("xwiki:actions:beforeSave xwiki:actions:beforePreview", beforeSubmitHandler);

    $(document).one("xwiki:actions:view", () => {
      $(document).off("xwiki:actions:beforeSave xwiki:actions:beforePreview", beforeSubmitHandler);
      BlockNote.destroy(container);
      container.classList.remove("xwiki-blocknote-wrapper");
      container.removeAttribute("data-config");
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
        // We currently use Markdown as output syntax, for which we don't have a way to protect the rendering
        // transformations, so we disable all of them. We'll have to add support for protecting the macro
        // transformation, at least, when switching to the UniAst syntax.
        transformations: [""],
      };
    },
  };
});
