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
define('xwiki-realtime-wikiEditor-loader', [
  'jquery',
  'xwiki-realtime-loader'
], function($, Loader) {
  'use strict';

  const editorId = 'wiki', info = {
    type: editorId,
    // FIXME: Don't hard-code the field name. We should be able to edit any document field (e.g. TextArea xobject
    // properties), not just the document content.
    field: 'content',
    href: '&editor=wiki&force=1',
    name: 'Wiki',
    compatible: ['wiki', 'wysiwyg']
  };

  Loader.bootstrap(info).then(realtimeContext => {
    require(['xwiki-realtime-wikiEditor'], function (RealtimeWikiEditor) {
      realtimeContext.rtURL = Loader.getEditorURL(window.location.href, info);
      new RealtimeWikiEditor(realtimeContext);
    });
  });
});
