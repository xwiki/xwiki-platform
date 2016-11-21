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
    // We can't use the Advanced Content Filter (ACF) because wiki syntax can generate any HTML element (e.g. by using
    // the HTML macro) and any attribute (e.g. by using the parameter syntax). So we cannot really limit the allowed
    // HTML content.
    allowedContent: true,
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
    // Don't convert Latin characters to the corresponding named HTML entities in the HTML output because the HTML
    // cleaner used on the server side doesn't recognize some of them (XCOMMONS-929).
    // See CKEDITOR-38: CKEditor converts Umlaute to HTML equivalents
    entities_latin: false,
    // The editor input is a full HTML page because we need to include the XWiki skin (in order to achieve WYSIWYG).
    fullPage: true,
    // Simplify the link dialog.
    linkShowAdvancedTab: false,
    linkShowTargetTab: false,
    // Disable the features that are not well integrated or that are focused more on the presentation than the content.
    removeButtons: 'Find,Anchor',
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
