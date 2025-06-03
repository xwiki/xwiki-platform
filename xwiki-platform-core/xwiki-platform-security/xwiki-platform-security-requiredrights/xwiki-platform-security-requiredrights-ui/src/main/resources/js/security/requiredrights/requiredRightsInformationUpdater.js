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
require(['jquery'], function($) {
    // Refresh information about required rights when the document is saved.
    $(document).on('xwiki:document:saved', function () {
        const warningID = 'missing-required-rights-warning';
        const warningContainer = $('#' + warningID);

        if (warningContainer.length > 0) {
            const getURL = XWiki.currentDocument.getURL('view',
                'xpage=security/requiredrights/getMissingRequiredRightsWarning');
            $.get(getURL, function (data) {
                if (data.length > 0) {
                    warningContainer.replaceWith(data);
                    $(document).trigger('xwiki:dom:updated', {'elements': [document.getElementById(warningID)]});
                }
            });
        }

        const informationContainer = $('dd.required-rights-information');

        if (informationContainer.length > 0) {
            const getURL = XWiki.currentDocument.getURL('view',
                'xpage=security/requiredrights/getRequiredRightsInformation');
            $.get(getURL, function (data) {
                if (data.length > 0) {
                    const requiredRightsInfo = $(data).filter('dd.required-rights-information');
                    informationContainer.replaceWith(requiredRightsInfo);
                    if (requiredRightsInfo.length > 0) {
                        $(document).trigger('xwiki:dom:updated', {'elements': [requiredRightsInfo[0]]});
                    }
                }
            });
        }
    });
});