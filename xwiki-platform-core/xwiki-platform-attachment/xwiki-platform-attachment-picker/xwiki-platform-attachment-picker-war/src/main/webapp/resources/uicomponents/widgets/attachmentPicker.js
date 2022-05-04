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
#set ($attachmentPickerEntry = 'attachmentPicker.min')
#set ($attachmentPickerPath = $services.webjars.url('org.xwiki.platform:xwiki-platform-attachment-picker-webjar', 
  $attachmentPickerEntry))
#set ($paths = {
  'js': {
    'xwiki-attachment-picker': $attachmentPickerPath,
    'xwiki-attachments-icon': $xwiki.getSkinFile('uicomponents/attachments/icons.js', true) 
  }
})
#[[*/
// Start JavaScript-only code.
(function (paths) {
  "use strict";

  require.config({
    paths: paths.js,
  });

  // Bootstrap the attachment picker, all the business code is located in the module initialization.
  require(['xwiki-attachment-picker'], function () {});
// End JavaScript-only code.
}).apply(']]#', $jsontool.serialize([$paths]));
