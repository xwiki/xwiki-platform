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
(function () {
    'use strict';
    const $ = jQuery;

    // Reload the content of the CKEditor instances when the document rights are changed to reflect the new rights
    // in the executed macros.
    $(document).on('xwiki:document:requiredRightsUpdated.ckeditor', function (event, data) {
        $.each(CKEDITOR.instances, function(key, editor) {
            if (matchesRequiredRightsChangeEvent(editor, event, data)) {
                maybeReload(editor);
            }
        });
    });

    const matchesRequiredRightsChangeEvent = function (editor, event, data) {
        // Check if the syntax change event targets the edited document (the source document).
        return editor.config.sourceDocument.documentReference.equals(data.documentReference) &&
            // Check if the syntax plugin is enabled for this editor instance.
            editor.plugins['xwiki-rights'];
    };

    const maybeReload = function (editor) {
        // Only reload in WYSIWYG mode. In source mode, rights aren't influencing anything.
        // TODO: it would be nice if we could mark something in source mode to ensure that on the next switch to
        //  wysiwyg mode, the content would be reloaded regardless if it has been modified or not.
        if (editor.mode === 'wysiwyg') {
            editor.execCommand('xwiki-refresh');
        }
    };

    // An empty plugin that can be used to enable / disable the reloading on required rights changes on a particular
    // CKEditor instance.
    CKEDITOR.plugins.add('xwiki-rights', {
        requires: 'xwiki-macro,xwiki-source'
    });
})();
