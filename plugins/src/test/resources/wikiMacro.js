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
describe('Wiki Macro Plugin for CKEditor', function() {
  var editor;

  var getWikiMacroWidgets = function(editor) {
    var wikiMacroWidgets = [];
    for (var widgetId in editor.widgets.instances) {
      var widget = editor.widgets.instances[widgetId];
      if (widget.name === 'wikiMacro') {
        wikiMacroWidgets.push(widget);
      }
    }
    return wikiMacroWidgets;
  };

  beforeEach(function(done) {
    editor = CKEDITOR.appendTo(document.body, {
      customConfig: '',
      // Basic plugins, as per http://docs.ckeditor.com/#!/guide/dev_tests-section-test-requirements .
      plugins: 'wysiwygarea,toolbar,undo,basicstyles,wikiMacro',
      skin: 'moono'
    });
    editor.on('instanceReady', function() {
      done();
    });
  });

  it('converts wiki macro output into a widget', function(done) {
    editor.setData('<p>before</p>' +
      '<!--startmacro:warning|-||-|warning--><div class="box warningmessage">warning</div><!--stopmacro-->' +
      '<!--startmacro:info|-||-|info--><div class="box infomessage">info</div><!--stopmacro-->' +
      '<p>after</p>', {
      callback: function() {
        var wikiMacroWidgets = getWikiMacroWidgets(editor);
        expect(wikiMacroWidgets.length).toBe(2);

        var warning = wikiMacroWidgets[1];
        expect(warning.element.getAttribute('data-macro')).toBe('startmacro:warning|-||-|warning');

        var info = wikiMacroWidgets[0];
        expect(info.element.getAttribute('data-macro')).toBe('startmacro:info|-||-|info');

        expect(editor.getData()).toBe('<p>before</p>' +
          '<!--startmacro:warning|-||-|warning--><!--stopmacro-->' +
          '<!--startmacro:info|-||-|info--><!--stopmacro-->' +
          '<p>after</p>');

        done();
      }
    });
  });
});
