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
import { readFileSync } from 'node:fs';
import { JSDOM } from 'jsdom';
import { beforeEach, describe, expect, it } from 'vitest';

const moduleDirectory = new URL('../../../', import.meta.url);
// Prototype isn't published on npm, so the Maven build extracts it from the webjar that this module already depends
// on. See the unpack-prototype-for-javascript-tests execution in pom.xml.
const prototypeSource = readFileSync(new URL('target/test-webjars/prototype.js', moduleDirectory), 'utf8');
const xwikiSource = readFileSync(new URL('src/main/webapp/resources/js/xwiki/xwiki.js', moduleDirectory), 'utf8');

// The tabs at the bottom of a page in view mode, as docextra.vm renders them.
const docExtraMarkup = `
  <div id="docextraanchors"><span id="Commentsanchor"></span><span id="Historyanchor"></span></div>
  <ul id="docExtrasTabsUl"><li id="Commentstab"></li><li id="Historytab"></li></ul>
  <div id="docextrapanes">
    <div id="Commentspane" class="hidden empty"></div>
    <div id="Historypane" class="hidden empty"></div>
  </div>`;

describe('XWiki.displayDocExtra', function() {
  let window;

  // The requests started to load the panes, in the order in which they have been started.
  let requests;

  beforeEach(function() {
    window = new JSDOM(`<html><body>${docExtraMarkup}</body></html>`, {runScripts: 'dangerously'}).window;
    window.eval(prototypeSource);
    // xwiki.js reads the reference of the current document when it's loaded. That code isn't under test, so we stub
    // the entityReference.js API that it needs.
    window.XWiki = {
      EntityType: {WIKI: 0, SPACE: 1, DOCUMENT: 2},
      Model: {resolve: () => ({getReversedReferenceChain: () => []})},
      DocumentReference: function() {}
    };
    // xwiki.js also registers RequireJS modules, which aren't needed here.
    window.require = () => {};
    window.eval(xwikiSource);

    // Collect the requests instead of sending them, so that the test decides when each pane is loaded and in which
    // order, which is what these tests are about.
    requests = [];
    window.Ajax = {Request: function(url, options) {
      requests.push(options);
    }};
  });

  /**
   * Answers the request that loads a pane, the way Prototype's Ajax.Request does when the response arrives.
   *
   * @param index the position of the request in the order in which they have been started
   * @param content the HTML of the pane
   */
  function answer(index, content) {
    requests[index].onSuccess({responseText: content});
    requests[index].onComplete({status: 200});
  }

  /**
   * Asserts that the given pane is the one displayed, and that the other one isn't.
   *
   * @param extraID the identifier of the pane that must be displayed, e.g. "Comments"
   */
  function assertDisplayed(extraID) {
    const isDisplayed = id => window.document.getElementById(id + 'tab').className === 'active'
      && window.document.getElementById(id + 'pane').className === '';
    expect(isDisplayed(extraID)).toBe(true);
    expect(isDisplayed(extraID === 'Comments' ? 'History' : 'Comments')).toBe(false);
  }

  it('displays the pane requested last even when an earlier pane is loaded after it', function() {
    // The first pane is loaded when the page is loaded, and the user opens another pane before it has been loaded.
    window.XWiki.displayDocExtra('Comments', 'commentsinline.vm', false);
    window.XWiki.displayDocExtra('History', 'historyinline.vm', false);

    // The pane requested last is loaded first, then the slow one is loaded.
    answer(1, '<div>history</div>');
    answer(0, '<div>comments</div>');

    assertDisplayed('History');
  });

  it("doesn't load a pane again when it has been loaded but not displayed", function() {
    window.XWiki.displayDocExtra('Comments', 'commentsinline.vm', false);
    window.XWiki.displayDocExtra('History', 'historyinline.vm', false);
    answer(1, '<div>history</div>');
    answer(0, '<div>comments</div>');

    // The user now opens the pane that has been loaded but not displayed.
    window.XWiki.displayDocExtra('Comments', 'commentsinline.vm', false);

    expect(requests.length).toBe(2);
    assertDisplayed('Comments');
    // The content would be inserted a second time if the pane were loaded again.
    expect(window.document.getElementById('Commentspane').innerHTML).toBe('<div>comments</div>');
  });
});
