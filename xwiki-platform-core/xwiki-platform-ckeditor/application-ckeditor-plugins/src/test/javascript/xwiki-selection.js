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
define(['jquery', 'ckeditor', 'testUtils', 'xwiki-macro'], function($, CKEDITOR, testUtils) {

  describe('XWiki Selection Plugin for CKEditor', function() {
    var editor;

    beforeEach(function(done) {
      editor = testUtils.createEditor(done, {
        extraPlugins: 'xwiki-macro',
        'xwiki-macro': {
          nestedEditableTypes: {
            'java.util.List<org.xwiki.rendering.block.Block>': {}
          }
        },
        // Style used by the hidden macro output placeholder.
        contentsCss: 'div.hidden, span.hidden {display: none}'
      });
    });

    it('preserves the selection on macro insert', function(done) {
      editor.setData(
        '<p>' +
          'one ' +
          '<!--startmacro:info|-||-|two-->' +
            '<span class="box infomessage">two</span>' +
          '<!--stopmacro-->' +
          ' three' +
        '</p>' +
        '<!--startmacro:warning|-||-|warning-->' +
          '<div class="box warningmessage">warning</div>' +
        '<!--stopmacro-->' +
        '<p>after</p>', {
        callback: function() {
          assertWidgetRemainsSelected('macro:info')
            .then(assertWidgetRemainsSelected.bind(null, 'macro:warning'))
            .done(done);
        }
      });
    });

    var assertWidgetRemainsSelected = function(pathName) {
      var deferred = $.Deferred();

      // Select the specified widget.
      findWidgetByPathName(pathName).focus();

      // Save the selection.
      CKEDITOR.plugins.xwikiSelection.saveSelection(editor);

      // Reload the edited content.
      editor.config.fullData = true;
      editor.setData(editor.getData(), {
        callback: function() {
          // Restore the selection.
          CKEDITOR.plugins.xwikiSelection.restoreSelection(editor);

          // Verify that the same widget is selected.
          expect(editor.widgets.focused.pathName).toBe(pathName);

          deferred.resolve();
        }
      });

      return deferred.promise();
    };

    var findWidgetByPathName = function(pathName) {
      return Object.values(editor.widgets.instances).find(function(widget) {
        return widget.pathName === pathName;
      });
    };
  });

});
