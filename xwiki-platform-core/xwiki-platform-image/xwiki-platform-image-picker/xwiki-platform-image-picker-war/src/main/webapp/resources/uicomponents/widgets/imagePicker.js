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
/*!
#set ($imagePickerEntry = 'imagePicker.min')
## #set ($imagePickerPath = $services.webjars.url('org.xwiki.platform:xwiki-platform-image-picker-webjar', 
##   $imagePickerEntry))
#set ($imagePickerPath = $xwiki.getSkinFile('uicomponents/imagePicker/imagePicker.js'))
#set ($blueimpGalleryPath = $services.webjars.url('org.xwiki.contrib:webjar-blueimp-gallery', 
  'js/blueimp-gallery-bundle.min'))
#set ($lightboxPath = $services.webjars.url('org.xwiki.platform:xwiki-platform-image-lightbox-webjar', "lightbox"))
#set ($paths = {
  'js': {
    'xwiki-image-picker': $imagePickerPath,
    'blueimp-gallery-bundle': $blueimpGalleryPath,
    'xwiki-lightbox-description': $lightboxPath 
  },
  'css': [$services.webjars.url('org.xwiki.contrib:webjar-blueimp-gallery', 'css/blueimp-gallery.min.css')]
})
#[[*/
// Start JavaScript-only code.
(function (paths) {
  "use strict";

  // TODO: add blueimp? how to factorize it with the dependencies from lightbox? Is it ok to redeclare blueimp twice 
  // (possibly with different bundles) in require.config?
  require.config({
    paths: paths.js,
  });

  require(['jquery'], function ($) {
    paths.css.forEach(function (url) {
      $('<link/>').attr({
        type: 'text/css',
        rel: 'stylesheet',
        href: url
      }).appendTo('head');
    });
  });

  // Bootstrap the image picker, all the business code is located in the module initialization.
  require(['xwiki-image-picker'], function () {});
// End JavaScript-only code.
}).apply(']]#', $jsontool.serialize([$paths]));
