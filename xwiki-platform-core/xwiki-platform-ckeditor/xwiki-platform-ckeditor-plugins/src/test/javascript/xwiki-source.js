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
define(['jquery', 'textSelection'], function($, TextSelection) {
  describe('XWiki Source Plugin for CKEditor', function() {
    it('Convert text selection between WYSIWYG and Source modes', async function(done) {
      await assertSelection('a|b|c', 'a|b|c');

      await assertSelection('abc *abc|*', 'abc <b>abc|</b>');

      await assertSelection('abc [[ab|c>>x|yz]] xyz', 'abc <a href="#xyz">ab|c|</a> xyz', 'abc [[ab|c|>>xyz]] xyz');

      await assertSelection('abc [|[abc>>xyz]|] xyz', 'abc |<a href="#xyz">abc|</a> xyz', 'abc |[[abc|>>xyz]] xyz');

      await assertSelection('{{abc/}} **a|bc**', 'xyz <b>a|bc</b>');

      await assertSelection('{{abc/}} [[ab|c>>x|yz]] xyz', '<b>xyz</b> <a href="#xyz">ab|c|</a> xyz',
        '{{abc/}} [[ab|c|>>xyz]] xyz');

      await assertSelection('one\ntwo\nthr|ee', '<p>one</p><p>two</p><p>thr|ee</p>');

      done();
    });
  });

  async function assertSelection(textBefore, html, textAfter) {
    textAfter = textAfter || textBefore;

    //
    // Setup
    //

    const textArea = $('<textarea/>').appendTo(document.body).val(textBefore)[0];
    let textSelection = getSpecifiedTextSelection(textArea);

    const root = $('<div/>').appendTo(document.body).attr('contenteditable', true).html(html)[0];
    const expectedRange = getSpecifiedRange(root);

    //
    // From Source to WYSIWYG
    //

    const selection = globalThis.getSelection();
    selection.removeAllRanges();
    textArea.setSelectionRange(textSelection.startOffset, textSelection.endOffset);

    (await new TextSelection().update(textArea).transform(root)).applyTo(root);

    const range = selection.getRangeAt(0);
    assertRange(range, expectedRange);

    //
    // From WYSIWYG to Source
    //

    textArea.value = textAfter;
    const expectedTextSelection = getSpecifiedTextSelection(textArea);

    (await new TextSelection().update(root, [selection.getRangeAt(0)]).transform(textArea)).applyTo(textArea);

    textSelection = new TextSelection().update(textArea);
    assertTextSelection(textSelection, expectedTextSelection);

    //
    // Cleanup
    //

    $(root).add(textArea).remove();
  }

  function assertRange(range, expectedRange) {
    expect(range.startContainer).toBe(expectedRange.startContainer);
    expect(range.startOffset).toBe(expectedRange.startOffset);
    expect(range.endContainer).toBe(expectedRange.endContainer);
    expect(range.endOffset).toBe(expectedRange.endOffset);
  }

  function assertTextSelection(textSelection, expectedTextSelection) {
    expect(textSelection.text).toBe(expectedTextSelection.text);
    expect(textSelection.startOffset).toBe(expectedTextSelection.startOffset);
    expect(textSelection.endOffset).toBe(expectedTextSelection.endOffset);
  }

  function getSpecifiedTextSelection(textArea) {
    let text = textArea.value;
    const start = text.indexOf('|');
    let end = text.indexOf('|', start + 1);
    end = end > 0 ? end - 1 : start;
    text = text.replaceAll('|', '');
    textArea.value = text;
    return {
      text: text,
      startOffset: start,
      endOffset: end
    };
  }

  function getSpecifiedRange(root) {
    const iterator = root.ownerDocument.createNodeIterator(root, NodeFilter.SHOW_TEXT, function(node) {
      return node.nodeValue.includes('|') ? NodeFilter.FILTER_ACCEPT : NodeFilter.FILTER_REJECT;
    });
    const points = [];
    for (let node = iterator.nextNode(); node && points.length < 2; node = iterator.nextNode()) {
      for (let i = node.nodeValue.indexOf('|'); i >= 0 && points.length < 2; i = node.nodeValue.indexOf('|', i)) {
        points.push({node: node, offset: i});
        node.nodeValue = node.nodeValue.substring(0, i) + node.nodeValue.substring(i + 1);
      }
    }
    if (points.length) {
      const range = root.ownerDocument.createRange();
      range.setStart(points[0].node, points[0].offset);
      const end = points.length > 1 ? 1 : 0;
      range.setEnd(points[end].node, points[end].offset);
      return range;
    }
  }
});
