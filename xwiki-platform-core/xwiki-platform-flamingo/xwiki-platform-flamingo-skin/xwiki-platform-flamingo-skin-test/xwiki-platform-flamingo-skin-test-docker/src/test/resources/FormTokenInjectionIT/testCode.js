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
require(['xwiki-meta'], function (xm) {
  const postURL = xm.restURL + '/comments';
  const resultDiv = document.getElementById('results');

  async function addComments() {
    // Wait after each request as the REST API is not thread-safe (object ids aren't properly incremented and integrity
    // constraints are violated, see also https://jira.xwiki.org/browse/XWIKI-13473).
    await fetch(postURL, {
      method: "POST",
      body: "Simple POST"
    }).then(response => resultDiv.textContent += 'Simple POST: ' + response.status);

    await fetch(new Request(postURL, {
      method: "POST",
      body: "Request Body",
      headers: {"Accept": "application/json"}
    })).then(async response => {
      resultDiv.textContent += 'Only Request: ' + response.status;
      return response.json();
    }).then(comment => resultDiv.textContent += comment.text);

    await fetch(new Request(postURL), {
      method: "POST",
      body: "Request with init body",
      headers: {"Accept": "application/json"}
    }).then(response => {
      resultDiv.textContent += 'Request with init: ' + response.status;
      return response.json();
    }).then(comment => resultDiv.textContent += comment.text);

    await fetch(postURL, {
      method: "POST",
      body: "Simple with array headers body",
      headers: [["Accept", "application/json"]]
    }).then(response => {
      resultDiv.textContent += 'Simple with array headers: ' + response.status;
      return response.json();
    }).then(comment => resultDiv.textContent += comment.text);
  }

  addComments();
});