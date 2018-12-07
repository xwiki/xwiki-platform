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

  CKEDITOR.plugins.setLang('xwiki-macro', 'en', {
    'placeholder': 'macro:{0}'
  });

  beforeEach(function(done) {
    editor = testUtils.createEditor(done, {'extraPlugins': 'xwiki-macro'});
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

  it('converts empty macro output into a widget', function(done) {
    editor.setData(
      '<!--startmacro:html|-||-|--><!--stopmacro-->' +
      '<p>' +
        '<!--startmacro:id|-|name="foo"-->' +
        '<span id="foo"></span>' +
        '<!--stopmacro-->' +
      '</p>', {
      callback: function() {
        var wikiMacroWidgets = getWikiMacroWidgets(editor);
        expect(wikiMacroWidgets.length).toBe(2);

        var htmlMacro = wikiMacroWidgets[1];
        expect(htmlMacro.pathName).toBe('macro:html');
        expect(htmlMacro.element.getAttribute('data-macro')).toBe('startmacro:html|-||-|');

        var idMacro = wikiMacroWidgets[0];
        expect(idMacro.pathName).toBe('macro:id');
        expect(idMacro.element.getAttribute('data-macro')).toBe('startmacro:id|-|name="foo"');

        expect(editor.getData()).toBe(
          '<!--startmacro:html|-||-|--><!--stopmacro-->' +
          '<p><!--startmacro:id|-|name="foo"--><!--stopmacro--></p>'
        );

        editor.config.fullData = true;
        expect(editor.getData()).toBe(
          '<!--startmacro:html|-||-|-->' +
          '<div class="macro-placeholder">macro:html</div>' +
          '<!--stopmacro-->' +
          '<p>' +
            '<!--startmacro:id|-|name="foo"-->' +
            '<span class="macro-placeholder">macro:id</span>' +
            '<!--stopmacro-->' +
          '</p>'
        );

        done();
      }
    });
  });

  it('handles nested editable macros', function(done) {
    editor.setData([
      '<!--startmacro:info|-|-->',
      '<div class="box infomessage">',
        'Info',
        '<div data-xwiki-non-generated-content="java.util.List&lt; org.xwiki.rendering.block.Block &gt;">',
          '<p>before</p>',
          '<!--startmacro:error|-|-->',
          '<div class="box errormessage">',
            'Error',
            '<div data-xwiki-non-generated-content="java.util.List&lt; org.xwiki.rendering.block.Block &gt;">',
              '<p>test</p>',
            '</div>',
          '</div>',
          '<!--stopmacro-->',
          '<p>after</p>',
        '</div>',
      '</div>',
      '<!--stopmacro-->'
    ].join(''), {
      callback: function() {
        var wikiMacroWidgets = getWikiMacroWidgets(editor);
        expect(wikiMacroWidgets.length).toBe(2);

        expect(wikiMacroWidgets[1].editables.content.getData()).toBe('<p>test</p>');

        expect(wikiMacroWidgets[0].editables.content.getData()).toBe([
          '<p>before</p>',
          '<!--startmacro:error|-|-->',
          '<div data-xwiki-non-generated-content="java.util.List&lt; org.xwiki.rendering.block.Block &gt;">',
            '<p>test</p>',
          '</div>',
          '<!--stopmacro-->',
          '<p>after</p>',
        ].join(''));

        expect(editor.getData()).toBe([
          '<!--startmacro:info|-|-->',
          '<div data-xwiki-non-generated-content="java.util.List&lt; org.xwiki.rendering.block.Block &gt;">',
            '<p>before</p>',
            '<!--startmacro:error|-|-->',
            '<div data-xwiki-non-generated-content="java.util.List&lt; org.xwiki.rendering.block.Block &gt;">',
              '<p>test</p>',
            '</div>',
            '<!--stopmacro-->',
            '<p>after</p>',
          '</div>',
          '<!--stopmacro-->'
        ].join(''));

        done();
      }
    });
  });

  it("doesn't initialize nested editables inside nested macros that are outside of a nested editable", function(done) {
    editor.setData([
      '<!--startmacro:outer|-|-->',
        '<!--startmacro:inner|-|-->',
          // The nested editable that shouldn't be initialized.
          '<div data-xwiki-non-generated-content="java.util.List&lt; org.xwiki.rendering.block.Block &gt;">',
            '<p>test</p>',
          '</div>',
        '<!--stopmacro-->',
      '<!--stopmacro-->'
    ].join(''), {
      callback: function() {
        var wikiMacroWidgets = getWikiMacroWidgets(editor);
        expect(wikiMacroWidgets.length).toBe(1);
        expect(wikiMacroWidgets[0].editables).toEqual({});

        // Skips nested macros that are outside of a nested editable
        expect(editor.getData()).toBe('<!--startmacro:outer|-|--><!--stopmacro-->');

        done();
      }
    });
  });

  it("doesn't initialize nested editables inside in-line macros", function(done) {
    editor.setData([
      '<p>',
        'before',
        '<!--startmacro:outer|-|-->',
          '<em>',
            '<!--startmacro:inner|-|-->',
              '<span data-xwiki-non-generated-content="java.util.List&lt; org.xwiki.rendering.block.Block &gt;">',
                'test',
              '</span>',
            '<!--stopmacro-->',
          '</em>',
        '<!--stopmacro-->',
        'after',
      '</p>'
    ].join(''), {
      callback: function() {
        var wikiMacroWidgets = getWikiMacroWidgets(editor);
        expect(wikiMacroWidgets.length).toBe(1);
        expect(wikiMacroWidgets[0].editables).toEqual({});

        expect(editor.getData()).toBe('<p>before<!--startmacro:outer|-|--><!--stopmacro-->after</p>');

        done();
      }
    });
  });

  it('handles nested sibling macro markers', function(done) {
    testUtils.assertData(
      editor,
      '<p><!--startmacro:foo|-|--><!--startmacro:bar|-|--><!--stopmacro--><!--stopmacro--></p>',
      // The nested macro is not submitted because it can't be edited.
      '<p><!--startmacro:foo|-|--><!--stopmacro--></p>'
    ).then(done);
  });

  it('checks if the edited content remains unchanged after performing data round-trips', function(done) {
    jQuery.when.apply(jQuery, [
      // CKEDITOR-48: Wiki Page source gets into bad state when macro that produces no output is used with CKEditor
      '<!--startmacro:html|-||-|--><!--stopmacro--><p>text</p>'
    ].map(jQuery.proxy(testUtils.assertNoChangeAfterDataRoundTrip, testUtils, editor))).then(done);
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
    })).toBe('startmacro:box|-|sTaRt="2"|-|');

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
