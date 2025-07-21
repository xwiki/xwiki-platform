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

describe('XWiki Macro Plugin for CKEditor', function() {
  var editor;

  CKEDITOR.plugins.setLang('xwiki-macro', 'en', {
    'placeholder': 'macro:{0}'
  });

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
          '<!--startmacro:warning|-||-|warning-->' +
            '<div class="macro-placeholder hidden">macro:warning</div>' +
            '<div class="box warningmessage">warning</div>' +
          '<!--stopmacro-->' +
          '<!--startmacro:info|-||-|info-->' +
            '<div class="macro-placeholder hidden">macro:info</div>' +
            '<div class="box infomessage">info</div>' +
          '<!--stopmacro-->' +
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
              '<span id="foo"></span>' +
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
        '<div data-xwiki-non-generated-content="java.util.List&lt;org.xwiki.rendering.block.Block&gt;">',
          '<p>before</p>',
          '<!--startmacro:error|-|-->',
          '<div class="box errormessage">',
            'Error',
            // Add whitespaces in the type name to test that they are handled correctly.
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

        expect(wikiMacroWidgets[1].editables.$content.getData()).toBe('<p>test</p>');

        expect(wikiMacroWidgets[0].editables.$content.getData()).toBe([
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
          '<div data-xwiki-non-generated-content="java.util.List&lt;org.xwiki.rendering.block.Block&gt;">',
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

  it('handles unknown nested editable types as plain text', function(done) {
    editor.setData([
      '<!--startmacro:test|-|-->',
      '<div class="test">',
        '<div data-xwiki-non-generated-content="java.util.List&lt;org.xwiki.rendering.block.Block&gt;"',
            ' data-xwiki-parameter-name="one">',
          '<p>1<em>2</em>3</p>',
        '</div>',
        '<div data-xwiki-non-generated-content="java.util.Date" data-xwiki-parameter-name="two">',
          '<p>1<em>2</em>3</p>',
        '</div>',
      '</div>',
      '<!--stopmacro-->'
    ].join(''), {
      callback: function() {
        var wikiMacroWidgets = getWikiMacroWidgets(editor);
        expect(wikiMacroWidgets.length).toBe(1);

        expect(wikiMacroWidgets[0].editables.one.getData()).toBe('<p>1<em>2</em>3</p>');
        expect(wikiMacroWidgets[0].editables.two.getData()).toBe('123');

        done();
      }
    });
  });

  it("doesn't initialize nested editables inside nested macros that are outside of a nested editable", function(done) {
    editor.setData([
      '<!--startmacro:outer|-|-->',
        '<!--startmacro:inner|-|-->',
          // The nested editable that shouldn't be initialized.
          '<div data-xwiki-non-generated-content="java.util.List&lt;org.xwiki.rendering.block.Block&gt;">',
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
              '<span data-xwiki-non-generated-content="java.util.List&lt;org.xwiki.rendering.block.Block&gt;">',
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

  // We generate the macro placeholder for nested empty macros even if they are outside of a nested editable (so they
  // cannot be edited) because CKEditor removes empty in-line elements and this can lead to empty in-line widgets which
  // are not well supported by CKEditor (we end up with widgets that don't have the widget element; the widget is not
  // fully removed because of its drag handler).
  it("adds placeholder for nested empty macros that are outside of a nested editable", function(done) {
    testUtils.assertData(
      editor,
      '<p><!--startmacro:outer|-|--><!--startmacro:inner|-|--><!--stopmacro--><!--stopmacro--></p>',
      [
        '<p>',
          '<!--startmacro:outer|-|-->',
            '<span class="macro-placeholder">macro:outer</span>',
            '<span class="macro" data-macro="startmacro:inner|-|">',
              // The inner macro is not initialized as a widget because the outer macro is not editable in-place.
              '<span class="macro-placeholder hidden">macro:inner</span>',
            '</span>',
          '<!--stopmacro-->',
        '</p>'
      ].join(''),
      true
    ).then(done);
  });

  it('checks if the edited content remains unchanged after performing data round-trips', function(done) {
    $.when.apply($, [
      // CKEDITOR-48: Wiki Page source gets into bad state when macro that produces no output is used with CKEditor
      '<!--startmacro:html|-||-|--><!--stopmacro--><p>text</p>'
    ].map(testUtils.assertNoChangeAfterDataRoundTrip.bind(testUtils, editor))).then(done);
  });

  it('protects empty elements in macro output', function(done) {
    testUtils.assertData(
      editor,
      [
        '<p>1<span></span>2<!--startmacro:outer|-|-->3<span></span>4<!--startmacro:inner|-|-->',
          '5<strong></strong>6<!--stopmacro-->7<i></i>8<!--stopmacro-->9<i></i>0</p>',
        '<!--startmacro:test|-|-->',
        '<div>',
          '1<em></em>2',
          '<div data-xwiki-non-generated-content="java.util.List&lt;org.xwiki.rendering.block.Block&gt;" ',
              'data-xwiki-parameter-name="title">',
            '<p>3<strong></strong>4</p>',
          '</div>',
          '<div data-xwiki-non-generated-content="java.util.List&lt;org.xwiki.rendering.block.Block&gt;">',
            '<p>5<em></em>6</p>',
          '</div>',
        '</div>',
        '<!--stopmacro-->'
      ].join(''),
      [
        '<p>',
          '12',
          '<!--startmacro:outer|-|-->',
            '<span class="macro-placeholder hidden">macro:outer</span>',
            '3<span></span>4',
            '<span class="macro" data-macro="startmacro:inner|-|">',
              '<span class="macro-placeholder hidden">macro:inner</span>',
              '5<strong></strong>6',
            '</span>',
            '7<i></i>8',
          '<!--stopmacro-->',
          '90',
        '</p>',
        '<!--startmacro:test|-|-->',
        '<div class="macro-placeholder hidden">macro:test</div>',
        '<div>',
          '1<em></em>2',
          '<div data-xwiki-non-generated-content="java.util.List&lt;org.xwiki.rendering.block.Block&gt;" ',
              'data-xwiki-parameter-name="title">',
            '<p>34</p>',
          '</div>',
          '<div data-xwiki-non-generated-content="java.util.List&lt;org.xwiki.rendering.block.Block&gt;">',
            '<p>56</p>',
          '</div>',
        '</div>',
        '<!--stopmacro-->'
      ].join(''),
      true
    ).then(done);
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

});
