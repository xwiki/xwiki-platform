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
define(['ckeditor', 'xwiki-table'], function(CKEDITOR) {
  describe('XWiki Table Plugin for CKEditor', function() {
    var assertReplaceMarginAutoWithAlign = function(attributes, expectedHTML) {
      var table = new CKEDITOR.htmlParser.element('table', attributes);
      CKEDITOR.plugins.xwikiTable.replaceMarginAutoWithAlign(table);
      expect(table.getOuterHtml()).toBe(expectedHTML);
    };

    it('converts margin:auto style to align attribute', function() {
      assertReplaceMarginAutoWithAlign({}, '<table></table>');
      assertReplaceMarginAutoWithAlign({'style': 'color: red;'}, '<table style="color: red;"></table>');
      assertReplaceMarginAutoWithAlign({'style': 'margin-left: auto'}, '<table style="" align="right"></table>');
      assertReplaceMarginAutoWithAlign({'style': 'margin-right: auto'}, '<table style="" align="left"></table>');
      assertReplaceMarginAutoWithAlign({'style': 'margin-left: auto; color: red; margin-right: auto'},
        '<table style="color:red" align="center"></table>');
      assertReplaceMarginAutoWithAlign({'style': 'margin: auto'},
        '<table style="margin-top:auto; margin-bottom:auto" align="center"></table>');
      assertReplaceMarginAutoWithAlign({'style': 'margin: 1em auto'},
        '<table style="margin-top:1em; margin-bottom:1em" align="center"></table>');
      assertReplaceMarginAutoWithAlign({'style': 'margin: 1em auto 6px'},
        '<table style="margin-top:1em; margin-bottom:6px" align="center"></table>');
      assertReplaceMarginAutoWithAlign({'style': 'margin: 1em auto 3% 0'},
        '<table style="margin-top:1em; margin-bottom:3%; margin-left:0" align="left"></table>');
      assertReplaceMarginAutoWithAlign({'style': 'margin: 1em 6px 3% auto'},
        '<table style="margin-top:1em; margin-right:6px; margin-bottom:3%" align="right"></table>');
    });

    var assertReplaceAlignWithMarginAuto = function(attributes, expectedHTML) {
      var table = new CKEDITOR.htmlParser.element('table', attributes);
      CKEDITOR.plugins.xwikiTable.replaceAlignWithMarginAuto(table);
      expect(table.getOuterHtml()).toBe(expectedHTML);
    };

    it('converts align attribute to margin:auto style', function() {
      assertReplaceAlignWithMarginAuto({}, '<table></table>');
      assertReplaceAlignWithMarginAuto({'align': 'top'}, '<table align="top"></table>');
      assertReplaceAlignWithMarginAuto({'align': 'left'}, '<table style="margin-right:auto"></table>');
      assertReplaceAlignWithMarginAuto({'align': 'right', 'style': 'color: red'},
        '<table style="color:red; margin-left:auto"></table>');
      assertReplaceAlignWithMarginAuto({'align': 'center'},
        '<table style="margin-left:auto; margin-right:auto"></table>');
      assertReplaceAlignWithMarginAuto({'align': 'center', 'style': 'margin-top: 1em; margin-bottom: 3%'},
        '<table style="margin:1em auto 3%"></table>');
      assertReplaceAlignWithMarginAuto({'align': 'center', 'style': 'margin: 0'},
        '<table style="margin:0 auto"></table>');
    });
  });
});
