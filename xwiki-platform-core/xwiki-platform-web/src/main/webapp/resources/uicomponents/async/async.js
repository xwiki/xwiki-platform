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
require(["jquery"], function($) {
  var activateAsyncPlaceHolder = function(element)
  {
    var id = element.dataset.xwikiAsyncId;

    var url = "${request.contextPath}/asyncrenderer/" + id;

    element = $(element);
    
    // Insert spinner (it will replaced by the result)
    if (element.tagName == 'div') {
      element.html('<div class="fa fa-spinner fa-spin"/>');
    } else {
      element.html('<span class="fa fa-spinner fa-spin"/>');
    }

    // TODO: don't wait forever
    // TODO: show progress
    element.load(url);
  }

  var onMutations = function(mutations)
  {
    mutations.forEach(function(mutation) {
      mutation.addedNodes.forEach(function(element) {
        if (element.className == 'xwiki-async') {
          activateAsyncPlaceHolder(element);
        }
      });
    });
  }

  // Register a callback for when inserting an async placeholder in the DOM
  var observer = new MutationObserver(onMutations);
  observer.observe(document, { childList: true, subtree : true});

  // Activate all the place holders already in the DOM
  $(".xwiki-async").each(function(index, element) {
    activateAsyncPlaceHolder(element);
  });
});
