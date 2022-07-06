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
(function() {
  'use strict';
  var $ = jQuery;

  // Empty plugin required to bundle this code with CKEditor.
  CKEDITOR.plugins.add('xwiki-config', {});

  CKEDITOR.on('instanceCreated', function(event) {
    // The editor instance was created but it not yet initialized. The configuration object passed when the instance was
    // created has not been merged with the global configuration yet.
    event.editor.once('configLoaded', function(event) {
      // TODO: The the complexity can onlu be lowered. Once below the default maxcomplexity (10 at the time of 
      //  writing), the jshint annotation can be removed.
      /*jshint maxcomplexity:16 */
      // The editor configuration has been loaded (the instance configuration has been merged with the global
      // configuration) but the editor has not been fully initialized yet so we can modify the configuration.
      var config = event.editor.config;
      var isHTML5 = config.htmlSyntax !== 'annotatedxhtml/1.0';

      var allowedContent;
      var allowedContentWithoutFigure;
      if (config.sourceDocument && config.sourceDocument.syntax in config.allowedContentBySyntax) {
        allowedContent = $.extend(true, {}, config.allowedContentBySyntax[config.sourceDocument.syntax]);

        // Forbid script tags if JavaScript skin extensions are not loaded.
        if (!config.loadJavaScriptSkinExtensions && '$1' in allowedContent && 'elements' in allowedContent.$1) {
          delete allowedContent.$1.elements.script;
        }

        // Removing the figure/figcaption tags for caption content to forbid nested figures.
        allowedContentWithoutFigure = $.extend(true, {}, allowedContent);
        if ('$1' in allowedContentWithoutFigure && 'elements' in allowedContentWithoutFigure.$1) {
          delete allowedContentWithoutFigure.$1.elements.figure;
        }
        if ('$2' in allowedContentWithoutFigure && 'elements' in allowedContentWithoutFigure.$2) {
          delete allowedContentWithoutFigure.$2.elements.figcaption;
        }

        // Disable figure support if the syntax isn't HTML 5.
        if (!isHTML5) {
          allowedContent = allowedContentWithoutFigure;
        }
      }

      if (!config.hasOwnProperty('allowedContent') && allowedContent) {
        config.allowedContent = allowedContent;
      }

      var imageConfig = config['xwiki-image'] = config['xwiki-image'] || {};
      if (!imageConfig.hasOwnProperty('captionAllowedContent') && allowedContentWithoutFigure) {
        imageConfig.captionAllowedContent = allowedContentWithoutFigure;
      }
    });
  });
})();
