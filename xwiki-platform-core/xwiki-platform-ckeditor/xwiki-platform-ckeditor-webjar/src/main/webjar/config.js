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
  const $ = jQuery;

  // See http://docs.ckeditor.com/#!/guide/dev_allowed_content_rules
  const allowedContentBySyntax = {
    'xwiki/2.1': {
      '$1': {
        elements: {
          // Elements required because the editor input is a full HTML page.
          html: true, head: true, link: true, script: true, style: true, body: true,
          // Headings
          h1: true, h2: true, h3: true, h4: true, h5: true, h6: true,
          // Lists
          dl: true, ol: true, ul: true,
          // Tables
          table: true, tr: true, th: true, td: true,
          // Formatting
          span: true, strong: true, em: true, ins: true, del: true, sub: true, sup: true, tt: true, pre: true,
          // Others
          div: true, hr: true, p: true, a: true, img: true, blockquote: true, figure: true
        },
        // The elements above can have any attribute, through the parameter (%%) syntax.
        attributes: '*',
        styles: '*',
        classes: '*'
      },
      '$2': {
        // The XWiki syntax doesn't support parameters for the following elements.
        elements: {br: true, dd: true, dt: true, li: true, tbody: true, figcaption: true}
      },
      '$3': {
        // Wiki syntax macros can output any HTML.
        match: CKEDITOR.plugins.xwikiMacro.isMacroOutput,
        attributes: '*',
        styles: '*',
        classes: '*'
      }
    },
    'plain/1.0': ';'
  };
  allowedContentBySyntax['xwiki/2.0'] = allowedContentBySyntax['xwiki/2.1'];
  // This is a hack, increasing the technical debt since the CKEditor module should not know about the Markdown
  // syntax. Actually it should not know either about the xwiki/2.0 and xwiki/2.1 syntaxes ;)
  // This should be fixed by implementing https://jira.xwiki.org/browse/CKEDITOR-319
  allowedContentBySyntax['markdown/1.2'] = $.extend(true, {}, allowedContentBySyntax['xwiki/2.1']);
  // Markdown doesn't allow figures at the moment.
  delete allowedContentBySyntax['markdown/1.2'].$1.elements.figure;
  delete allowedContentBySyntax['markdown/1.2'].$2.elements.figcaption;

  CKEDITOR.tools.extend(config, {
    allowedContentBySyntax: allowedContentBySyntax,
    // It's not the case by default.
    // https://dev.ckeditor.com/ticket/13093
    applyPasteFilterAfterPasteFromWord: true,
    // Modify the strike and underline core styles to match what the XWiki Rendering is generating and expecting.
    // See CKEDITOR-52: Unable to un-strike or un-underline saved content
    coreStyles_strike: { // jshint ignore:line
      element: 'del',
      overrides: ['s', 'strike']
    },
    coreStyles_underline: { // jshint ignore:line
      element: 'ins',
      overrides: 'u'
    },
    // Add support for overwriting the default configuration from a wiki page.
    customConfig: new XWiki.Document('Config', 'CKEditor').getURL('get',
      'outputSyntax=plain&sheet=CKEditor.ConfigSheet'),
    // Enable the native (in-browser) spell checker because we don't bundle any spell checker plugin. Most of the spell
    // checker plugins are relying on an external service which leads to security and privacy concerns.
    disableNativeSpellChecker: false,
    // This is used in CKEditor.FileUploader so we must keep them in sync.
    fileTools_defaultFileName: '__fileCreatedFromDataURI__', // jshint ignore:line
    // The editor input is a full HTML page because we need to include the XWiki skin (in order to achieve WYSIWYG).
    fullPage: true,
    // The maximum image width is limited from the skin to 100% of the available page width (responsive images).
    // Prefilling the image dimensions leads to a stretched image (horizontally) because the browser uses the prefilled
    // height but limits the width.
    // CKEDITOR-141: Image larger than the screen inserted with CKEditor gets distorted by default
    image2_prefillDimensions: false, // jshint ignore:line
    // Simplify the link dialog.
    linkShowAdvancedTab: false,
    linkShowTargetTab: false,
    pasteFilter: {
      // Allowed anchor attributes.
      a: {
        propertiesOnly: true,
        attributes: {
          href: true
        }
      },
      // Allowed image attributes.
      img: {
        propertiesOnly: true,
        attributes: {
          alt: true,
          height: true,
          src: true,
          width: true
        }
      },
      // Allowed table header and table cell attributes.
      'th td': {
        propertiesOnly: true,
        attributes: {
          colspan: true,
          rowspan: true
        }
      },
      // Allowed elements.
      $1: {
        // Allow all elements except for:
        // * DIV and SPAN, which are generally used for styling (DIVs will be replaced with paragraphs, if possible,
        //   otherwise with their child nodes; SPANs will be replaced with their child nodes)
        // * lone paragraphs, when inside a list item, table cell, definition list or block quote (in order to simplify
        //   the generated wiki syntax; they will be replaced by their child nodes)
        match: function(element) {
          var loneParagraphParents = ['li', 'td', 'th', 'dd', 'blockquote'];
          var name = element.name.toLowerCase();
          var allowed = name !== 'div' && name !== 'span' && (
            name !== 'p' || !element.parent || !element.parent.name ||
            element.parent.children.length > 1 || loneParagraphParents.indexOf(element.parent.name.toLowerCase()) < 0
          );
          if (!allowed && name === 'p') {
            // In order to remove this lone paragraph we need to change its name, otherwise CKEditor will simply replace
            // it with another paragraph (it does this with most of the block level elements, when possible, in order to
            // preserve the content structure).
            element.name = 'lone-paragraph';
          }
          return allowed;
        }
      }
    },
    plugins: config.plugins + ',' + [
      'xwiki-config',
      'xwiki-filter',
      'xwiki-focusedplaceholder',
      'xwiki-icon',
      'xwiki-image-old',
      'xwiki-image',
      'xwiki-link',
      'xwiki-list',
      'xwiki-loading',
      'xwiki-localization',
      'xwiki-macro',
      'xwiki-maximize',
      'xwiki-office',
      'xwiki-realtime',
      'xwiki-save',
      'xwiki-selection',
      'xwiki-slash',
      'xwiki-source',
      'xwiki-sourcearea',
      'xwiki-syntax',
      'xwiki-table',
      'xwiki-toolbar',
      'xwiki-upload',
      'xwiki-wysiwygarea'
    ].join(','),
    // We simplify the tool bar by removing:
    // * the features that are not well integrated (e.g. bidi, save, Anchor, Find)
    // * the features that are focused more on the presentation than the content (e.g. colorbutton, font, justify,
    //     CopyFormatting)
    // * the paste buttons because most of the time they only tell you the shortcut key that needs to be used
    // * the Copy and Cut buttons because they are available on the context menu
    // * the SpecialChar, HorizontalRule and officeImporter buttons because they are not used very often and they are
    //     available in the Insert drop down menu
    // * the Language dropdown because it causes confusion with the page language
    // * the Strike, Subscript, Superscript and Remove Format buttons because they are not used very often and they are
    //     available in the basic styles drop down
    // * the Underline button because this style should be reserved for links
    // * the List and Indent buttons because they are grouped in the Lists drop down menu
    // * the Unlink button because it is available on the context menu and on the balloon toolbar
    // * the XWiki Macro button because we have it in the Insert menu
    removeButtons: 'Anchor,BulletedList,Copy,CopyFormatting,Cut,Find,HorizontalRule,Indent,Language,NumberedList,' +
      'Outdent,Paste,PasteFromWord,PasteText,RemoveFormat,SpecialChar,Strike,Subscript,Superscript,Underline,Unlink,' +
      'officeImporter,xwiki-macro',
    // We remove the default sourcearea plugin because we use our own xwiki-sourcearea plugin which supports switching
    // to Source while editing in-place. We still bundle the sourcearea plugin because we reuse its icons and
    // translations. We remove the realtime plugin by default because it's unstable.
    removePlugins: 'bidi,colorbutton,font,justify,save,sourcearea,xwiki-realtime',
    toolbarGroups: [
      {name: 'format'},
      {name: 'basicstyles', groups: ['basicstyles', 'cleanup']},
      {name: 'paragraph',   groups: ['list', 'indent', 'align', 'bidi']},
      {name: 'links'},
      {name: 'insert'},
      {name: 'forms'},
      {name: 'styles',      groups: ['styles', 'blocks']},
      {name: 'colors'},
      {name: 'editing',     groups: ['find', 'selection', 'spellchecker']},
      {name: 'clipboard',   groups: ['clipboard', 'undo']},
      {name: 'document',    groups: ['mode', 'document', 'doctools']},
      {name: 'tools'},
      {name: 'others'},
      {name: 'about'}
    ],
    toolbarMenus: {
      basicStyles: {
        toolbar: 'basicstyles,70',
        groups: [
          {id: 'basicStyles', items: ['bold', 'italic', 'strike', 'underline', 'subscript', 'superscript']},
          {id: 'removeFormat', items: ['removeFormat']}
        ]
      },
      lists: {
        toolbar: 'list',
        groups: [
          {id: 'lists', items: ['bulletedlist', 'numberedlist']},
          {id: 'indent', items: ['indent', 'outdent']}
        ]
      },
      insert: {
        icon: 'insert',
        toolbar: 'insert',
        groups: [
          {id: 'insert', items: ['image', 'table', 'horizontalrule', 'specialchar', 'officeImporter']},
          {id: 'macros', items: ['infoBox', 'successBox', 'warningBox', 'errorBox', 'toc', 'include', 'code']},
          {id: 'otherMacros', items: ['xwiki-macro']}
        ]
      }
    },
    toolbarMenuItems: {
      bold: {label: 'basicstyles.bold'},
      italic: {label: 'basicstyles.italic'},
      strike: {label: 'basicstyles.strike'},
      underline: {label: 'basicstyles.underline'},
      subscript: {label: 'basicstyles.subscript'},
      superscript: {label: 'basicstyles.superscript'},

      removeFormat: {label: 'removeformat.toolbar'},

      indent: {label: 'indent.indent'},
      outdent: {label: 'indent.outdent'},

      image: {label: 'common.image'},
      table: {label: 'table.toolbar'},
      officeImporter: {label: 'xwiki-office.importer.title', icon: 'pastefromword'},

      infoBox: {
        command: 'xwiki-macro-insert',
        data: {
          name: 'info',
          content: 'Type your information message here.'
        }
      },
      successBox: {
        command: 'xwiki-macro-insert',
        data: {
          name: 'success',
          content: 'Type your success message here.'
        }
      },
      warningBox: {
        command: 'xwiki-macro-insert',
        data: {
          name: 'warning',
          content: 'Type your warning message here.'
        }
      },
      errorBox: {
        command: 'xwiki-macro-insert',
        data: {
          name: 'error',
          content: 'Type your error message here.'
        }
      },
      toc: {
        command: 'xwiki-macro-insert',
        data: {name: 'toc'}
      },
      include: {
        command: 'xwiki-macro',
        data: {
          name: 'include',
          parameters: {reference: 'Sandbox.TestPage1'}
        }
      },
      code: {
        command: 'xwiki-macro',
        data: {
          name: 'code',
          parameters: {language: 'none'}
        }
      },
      'xwiki-macro': {label: 'xwiki-toolbar.otherMacros'}
    },
    versionCheck: false,
    'xwiki-macro': {
      // You can restrict here the type of content the users can input when editing the macro content / parameters
      // in-line using nested editables, depending on the macro content / parameter type.
      // See https://ckeditor.com/docs/ckeditor4/latest/api/CKEDITOR_plugins_widget_nestedEditable.html
      // See also https://ckeditor.com/docs/ckeditor4/latest/api/CKEDITOR_plugins_widget_nestedEditable_definition.html
      // on how to configure a nested editable.
      // See also https://ckeditor.com/docs/ckeditor4/latest/guide/dev_allowed_content_rules.html
      nestedEditableTypes: {
        // The type used when the macro content / parameter supports any wiki syntax (no restrictions).
        'java.util.List<org.xwiki.rendering.block.Block>': {}
      }
    },
    'xwiki-save': {
      leaveConfirmation: true
    }
  }, true);
};
