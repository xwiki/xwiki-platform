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
define(['jquery', 'testUtils', 'xwiki-filter'], function($, testUtils) {

  describe('XWiki Filter Plugin for CKEditor', function() {
    var editor;

    beforeEach(function(done) {
      editor = testUtils.createEditor(done, {extraPlugins: 'xwiki-filter'});
    });

    it('converts empty lines to empty paragraphs', function(done) {
      testUtils.assertSnapshot(editor, '<p>1</p><div class="wikimodel-emptyline"></div><p>2</p>',
        '<p>1</p><p><br></p><p>2</p>').then(done);
    });

    it('converts empty lines to empty paragraphs and back', function(done) {
      testUtils.assertNoChangeAfterDataRoundTrip(editor,
        '<p>before</p><div class="wikimodel-emptyline"></div><p>after</p>').then(done);
    });

    it('converts empty lines to empty paragraphs and back', function(done) {
      testUtils.assertData(editor, [
        '<table>',
          '<tbody>',
            '<tr>',
              '<td>',
                '<p>abc</p>',
                '<p><br/></p>',
              '</td>',
              '<td>',
                '<p>before</p>',
                '<p><br/></p>',
                '<p>after</p>',
              '</td>',
            '</tr>',
          '</tbody>',
        '</table>'
      ].join('\n'), [
        '<table>',
          '<tbody>',
            '<tr>',
              '<td>',
                '<p>abc</p>',
                '<p> </p>',
              '</td>',
              '<td>',
                '<p>before</p>',
                '<div class="wikimodel-emptyline"></div>',
                '<p>after</p>',
              '</td>',
            '</tr>',
          '</tbody>',
        '</table>'
      ].join('')).then(done);
    });

    it('submits only significant content', function() {
      editor.config.fullPage = true;
      editor.config.fullData = false;
      $(editor.document.$).find('body').attr('data-foo', 'bar').after('<style/>').append('<script/>');
      var data = editor.getData();
      expect(data).not.toContain('<head');
      expect(data).not.toContain('data-foo="bar"');
      expect(data).not.toContain('<style');
      expect(data).not.toContain('<script');
      editor.config.fullData = true;
      data = editor.getData();
      expect(data).toContain('<head');
      expect(data).toContain(' data-foo="bar"');
      expect(data).toContain('<style');
      expect(data).toContain('<script');
    });

    it('converts font to span', function(done) {
      testUtils.createEditor(function(event) {
        testUtils.assertSnapshot(event.editor,
          '<p><font color="red" face="Verdana" size="-2" class="foo">test</font></p>',
          '<p><span style="color:red; font-family:Verdana" class="foo">test</span></p>'
        ).then(done);
      }, {
        // Enable the Advanced Content Filter.
        allowedContent: 'p;span{*}(*)',
        extraPlugins: 'xwiki-filter'
      });
    });

    it('unprotect allowed scripts', function(done) {
      testUtils.createEditor(function(event) {
        testUtils.assertSnapshot(event.editor, [
          '<script type="text/javascript" src="foo.js" data-wysiwyg="true"></script>',
          '<script type="text/javascript" src="bar.js"></script>',
          '<script type="text/javascript" src="bar?language=en&wysiwyg=true"></script>',
          '<script type="text/javascript" src="foo?language=en"></script>',
          '<script type="text/javascript" data-wysiwyg="true">var x = 13;</script>',
          '<script type="text/javascript">var y = 27;</script>'
        ].join(''), [
          '<script type="text/javascript" src="foo.js" data-wysiwyg="true"></script>',
          '<!--{cke_protected}%3Cscript%20type%3D%22text%2Fjavascript%22%20src%3D%22bar.js%22%3E%3C%2Fscript%3E-->',
          '<script type="text/javascript" src="bar?language=en&amp;wysiwyg=true"></script>',
          '<!--{cke_protected}%3Cscript%20type%3D%22text%2Fjavascript%22%20src%3D%22foo%3Flanguage%3Den%22%3E%3C%2F' +
            'script%3E-->',
          '<script type="text/javascript" data-wysiwyg="true">var x = 13;</script>',
          '<!--{cke_protected}%3Cscript%20type%3D%22text%2Fjavascript%22%3Evar%20y%20%3D%2027%3B%3C%2Fscript%3E-->'
        ].join('')).then(done);
      }, {
        // Configure the editor to load the JavaScript Skin Extensions.
        loadJavaScriptSkinExtensions: true,
        extraPlugins: 'xwiki-filter'
      });
    });
  });

});
