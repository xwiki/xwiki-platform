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
describe('XWiki Macro Plugin for CKEditor', function() {
  var editor;

  beforeEach(function(done) {
    editor = CKEDITOR.appendTo(document.body, {
      allowedContent: true,
      customConfig: '',
      // Basic plugins, as per http://docs.ckeditor.com/#!/guide/dev_tests-section-test-requirements .
      plugins: 'wysiwygarea,toolbar,undo,basicstyles,xwiki-macro',
      skin: 'moono'
    });
    editor.on('instanceReady', function() {
      done();
    });
  });

  var getWikiMacroWidgets = function(editor) {
    var wikiMacroWidgets = [];
    for (var widgetId in editor.widgets.instances) {
      var widget = editor.widgets.instances[widgetId];
      if (widget.name === 'xwiki-macro') {
        wikiMacroWidgets.push(widget);
      }
    }
    return wikiMacroWidgets;
  };

  it('converts wiki macro output into a widget', function(done) {
    editor.setData('<p>before</p>' +
      '<!--startmacro:warning|-||-|warning--><div class="box warningmessage">warning</div><!--stopmacro-->' +
      '<!--startmacro:info|-||-|info--><div class="box infomessage">info</div><!--stopmacro-->' +
      '<p>after</p>', {
      callback: function() {
        var wikiMacroWidgets = getWikiMacroWidgets(editor);
        expect(wikiMacroWidgets.length).toBe(2);

        var warning = wikiMacroWidgets[1];
        expect(warning.pathName).toBe('macro:warning');
        expect(warning.element.getAttribute('data-macro')).toBe('startmacro:warning|-||-|warning');

        var info = wikiMacroWidgets[0];
        expect(info.pathName).toBe('macro:info');
        expect(info.element.getAttribute('data-macro')).toBe('startmacro:info|-||-|info');

        expect(editor.getData()).toBe('<p>before</p>' +
          '<!--startmacro:warning|-||-|warning--><!--stopmacro-->' +
          '<!--startmacro:info|-||-|info--><!--stopmacro-->' +
          '<p>after</p>');

        editor.config.fullData = true;
        expect(editor.getData()).toBe('<p>before</p>' +
          '<!--startmacro:warning|-||-|warning--><div class="box warningmessage">warning</div><!--stopmacro-->' +
          '<!--startmacro:info|-||-|info--><div class="box infomessage">info</div><!--stopmacro-->' +
          '<p>after</p>');

        done();
      }
    });
  });

  var serializeAndParseMacroCall = function(macroCall) {
    var wikiMacroPlugin = CKEDITOR.plugins.get('xwiki-macro');
    var serializedMacroCall = wikiMacroPlugin.serializeMacroCall(macroCall);
    var container = document.createElement('span');
    container.innerHTML = '<!--' + serializedMacroCall + '-->';
    var parsedMacroCall = wikiMacroPlugin.parseMacroCall(container.firstChild.nodeValue);
    expect(parsedMacroCall.name).toBe(macroCall.name);
    expect(parsedMacroCall.content).toBe(macroCall.content);
    expect(Object.keys(parsedMacroCall.parameters || {}).length).toBe(Object.keys(macroCall.parameters || {}).length);
    for (var parameterId in parsedMacroCall.parameters) {
      var parsedParameter = parsedMacroCall.parameters[parameterId];
      var parameter = (macroCall.parameters || {})[parameterId];
      expect(parsedParameter.name).toBe(parameter.name);
      expect(parsedParameter.value).toBe(parameter.value);
    }
  };

  var createMacroCall = function(text) {
    var macroCall = {
      name: text,
      content: text,
      parameters: {}
    };
    macroCall.parameters[text] = {name: text, value: text};
    return macroCall;
  };

  it('serializes and parses wiki macro calls', function() {
    // Tests if the start macro comment is parsed correctly when macro content and macro parameter values contain
    // special symbols like " and \ or the separator |-|.
    serializeAndParseMacroCall({
      name: 'html',
      content: '="|-|\\',
      parameters: {
        a: {name: 'a', value: '1"2|-|3=\\"4\\'},
        b: {name: 'b', value: ''}
      }
    });

    // Tests if the start macro comment is parsed correctly when it contains the '--' sequence.
    serializeAndParseMacroCall(createMacroCall('a--b\\c\\\\d-'));
    serializeAndParseMacroCall(createMacroCall('a--b-c\\\\d\\'));

    // Tests if the case used in parameter names is kept.
    expect(CKEDITOR.plugins.get('xwiki-macro').serializeMacroCall({
      name: 'box',
      content: '',
      parameters: {
        'start': {name: 'sTaRt', value: '2'}
      }
    })).toBe('startmacro:box|-|sTaRt="2" |-|');

    // Differentiate macros with empty content from macros without content.
    // No content and no parameters.
    serializeAndParseMacroCall({name: 'x'});
    // Empty content and no parameters.
    serializeAndParseMacroCall({name: 'y', content: ''});
    // No content but with parameters.
    serializeAndParseMacroCall({name: 'z', parameters: {c: {name: 'c', value: '1|-|2'}}});
    // Empty content with parameters.
    serializeAndParseMacroCall({
      name: 'w',
      content: '',
      parameters: {
        c: {name: 'c', value: '1|-|2'}
      }
    });
  });
});
