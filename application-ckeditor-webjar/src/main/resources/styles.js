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

// This file contains style definitions that can be used by CKEditor plugins.
//
// The most common use for it is the "stylescombo" plugin which shows the Styles drop-down
// list containing all styles in the editor toolbar. Other plugins, like
// the "div" plugin, use a subset of the styles for their features.
//
// For more information refer to: http://docs.ckeditor.com/#!/guide/dev_styles-section-style-rules

[4, 5].forEach(function (htmlVersion) {
    const styles = [
        /* Block styles */

        {name: 'Box', element: 'div', attributes: {'class': 'box'}},
        {name: 'Info Box', element: 'div', attributes: {'class': 'box infomessage'}},
        {name: 'Warning Box', element: 'div', attributes: {'class': 'box warningmessage'}},
        {name: 'Success Box', element: 'div', attributes: {'class': 'box successmessage'}},
        {name: 'Error Box', element: 'div', attributes: {'class': 'box errormessage'}},
        {name: 'Floating Box', element: 'div', attributes: {'class': 'box floatinginfobox'}},

        {name: 'Lead Paragraph', element: 'p', attributes: {'class': 'lead'}},
        {name: 'Reverse Block Quote', element: 'blockquote', attributes: {'class': 'blockquote-reverse'}}
    ];

    /* Inline styles */

    if (htmlVersion === 4) {
        styles.push({name: 'Typewriter', element: 'tt'});
    } else {
        styles.push({name: 'Typewriter', element: 'span', attributes: {'class': 'monospace'}});
    }

    styles.push(
        {name: 'Marker', element: 'span', attributes: {'class': 'mark'}},
        {name: 'Small', element: 'span', attributes: {'class': 'small'}},
        {name: 'Uppercase', element: 'span', attributes: {'class': 'text-uppercase'}},

        /* Object styles */

        {name: 'Striped Table', element: 'table', attributes: {'class': 'table-striped'}},
        {name: 'Bordered Table', element: 'table', attributes: {'class': 'table-bordered'}},
        {name: 'Hover Table', element: 'table', attributes: {'class': 'table-hover'}},
        {name: 'Condensed Table', element: 'table', attributes: {'class': 'table-condensed'}},
        {name: 'Responsive Table', element: 'table', attributes: {'class': 'responsive-table'}},

        {name: 'Active Row', element: 'tr', attributes: {'class': 'active'}},
        {name: 'Success Row', element: 'tr', attributes: {'class': 'success'}},
        {name: 'Info Row', element: 'tr', attributes: {'class': 'info'}},
        {name: 'Warning Row', element: 'tr', attributes: {'class': 'warning'}},
        {name: 'Danger Row', element: 'tr', attributes: {'class': 'danger'}},

        /* Widget styles */

        {name: 'Rounded Image', type: 'widget', widget: 'image', element: 'img', attributes: {'class': 'img-rounded'}},
        {name: 'Circle Image', type: 'widget', widget: 'image', element: 'img', attributes: {'class': 'img-circle'}},
        {
            name: 'Thumbnail Image',
            type: 'widget',
            widget: 'image',
            element: 'img',
            attributes: {'class': 'img-thumbnail'}
        });

    CKEDITOR.stylesSet.add('html' + htmlVersion, styles);
});
