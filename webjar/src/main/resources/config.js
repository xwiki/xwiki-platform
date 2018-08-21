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
CKEDITOR.editorConfig = function(config) {
  CKEDITOR.tools.extend(config, {
    // It's not the case by default.
    // https://dev.ckeditor.com/ticket/13093
    applyPasteFilterAfterPasteFromWord: true,
    // Modify the strike and underline core styles to match what the XWiki Rendering is generating and expecting.
    // See CKEDITOR-52: Unable to un-strike or un-underline saved content
    coreStyles_strike: {
      element: 'del',
      overrides: ['s', 'strike']
    },
    coreStyles_underline: {
      element: 'ins',
      overrides: 'u'
    },
    // Add support for overwriting the default configuration from a wiki page.
    customConfig: new XWiki.Document('Config', 'CKEditor').getURL('get', 'outputSyntax=plain'),
    // Enable the native (in-browser) spell checker because we don't bundle any spell checker plugin. Most of the spell
    // checker plugins are relying on an external service which leads to security and privacy concerns.
    disableNativeSpellChecker: false,
    // The editor input is a full HTML page because we need to include the XWiki skin (in order to achieve WYSIWYG).
    fullPage: true,
    // The maximum image width is limited from the skin to 100% of the available page width (responsive images).
    // Prefilling the image dimensions leads to a stretched image (horizontally) because the browser uses the prefilled
    // height but limits the width.
    // CKEDITOR-141: Image larger than the screen inserted with CKEditor gets distorted by default
    image2_prefillDimensions: false,
    // Simplify the link dialog.
    linkShowAdvancedTab: false,
    linkShowTargetTab: false,
    pasteFilter: {
      a: {
        propertiesOnly: true,
        attributes: {
          href: true
        }
      },
      img: {
        propertiesOnly: true,
        attributes: {
          alt: true,
          height: true,
          src: true,
          width: true
        }
      },
      'th td': {
        propertiesOnly: true,
        attributes: {
          colspan: true,
          rowspan: true
        }
      },
      $1: {
        // Allow all elements except DIV and SPAN which are generally used for styling.
        match: function(element) {
          var name = element.name.toLowerCase();
          return name !== 'div' && name !== 'span';
        }
      },
      $2: {
        // Replace lone paragraphs with their children in order to simplify the wiki syntax.
        match: function(element) {
          var targetParentNames = ['li', 'td', 'th', 'dd', 'blockquote'];
          if (element.name.toLowerCase() === 'p' && element.parent && element.parent.name &&
              targetParentNames.indexOf(element.parent.name.toLowerCase()) >= 0 &&
              element.parent.children.length === 1) {
            element.replaceWithChildren();
            return true;
          }
        }
      }
    },
    // Remove the features that are not well integrated or that are focused more on the presentation than the content.
    // Remove the paste buttons because most of the time they only tell you the shortcut key that needs to be used.
    removeButtons: 'Find,Anchor,Paste,PasteFromWord,PasteText',
    removePlugins: 'bidi,colorbutton,font,justify,save,specialchar',
    toolbarGroups: [
      {name: 'basicstyles', groups: ['basicstyles', 'cleanup']},
      {name: 'paragraph',   groups: ['list', 'indent', 'blocks', 'align', 'bidi']},
      {name: 'clipboard',   groups: ['clipboard', 'undo']},
      {name: 'editing',     groups: ['find', 'selection', 'spellchecker']},
      {name: 'forms'},
      '/',
      {name: 'links'},
      {name: 'insert'},
      {name: 'styles'},
      {name: 'colors'},
      {name: 'document',    groups: ['mode', 'document', 'doctools']},
      {name: 'tools'},
      {name: 'others'},
      {name: 'about'}
    ],
    'xwiki-save': {
      leaveConfirmation: true
    }
  }, true);
};
