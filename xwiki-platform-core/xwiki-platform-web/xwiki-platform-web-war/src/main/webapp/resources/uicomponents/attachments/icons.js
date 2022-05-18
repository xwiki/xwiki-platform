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
#set ($discard = "#mimetypeimg('' '')")
#set ($discard = $mimetypeMap.put('attachment', ['attach', 'attachment']))
#foreach ($map in [$mimetypeMap, $extensionMap])
  #foreach ($entry in $map.entrySet())
    #set ($discard = $entry.value.set(0, $services.icon.getMetaData($entry.value.get(0))))
    #set ($translationKey = "core.viewers.attachments.mime.$entry.value.get(1)")
    #set ($discard = $entry.value.set(1, $services.localization.render($translationKey)))
  #end
#end
#[[*/
// Start JavaScript-only code.
(function (mimeTypeMap, extensionMap) {
  'use strict';

  define('xwiki-attachments-icon', [], function () {
    var getAttachmentIcon = function (attachment) {
      if (typeof attachment.mimeType === 'string' && attachment.mimeType.substring(0, 6) === 'image/') {
        var url = attachment.xwikiRelativeUrl;
        // If the image URL is relative to the current page or is absolute (HTTP) then we can pass the icon width as a
        // query string parameter to allow the image to be resized on the server side.
        if (url.substring(0, 1) === '/' || url.substring(0, 7) === 'http://') {
          url += (url.indexOf('?') < 0 ? '?' : '&') + 'width=48';
        }
        var icon = {
          iconSetType: 'IMAGE',
          url: url
        };
        if (attachment.file) {
          // Show the icon using the local file while the file is being uploaded.
          icon.promise = readAsDataURL(attachment.file).then(function (dataURL) {
            icon.url = dataURL;
            return dataURL;
          });
        }
        return icon;
      } else {
        return getIcon(attachment.mimeType, attachment.name);
      }
    };

    var readAsDataURL = function (file) {
      return new Promise((resolve) => {
        var fileReader = new FileReader();
        fileReader.onload = function (event) {
          resolve(event.target.result);
        };
        fileReader.readAsDataURL(file);
      });
    };

    var getIcon = function (mimeType, fileName) {
      var extension = fileName.substring(fileName.lastIndexOf('.') + 1);
      if (mimeTypeMap.hasOwnProperty(mimeType)) {
        return mimeTypeMap[mimeType][0];
      } else if (extensionMap.hasOwnProperty(extension)) {
        return extensionMap[extension][0];
      } else {
        var mimeTypePrefix = mimeType.substring(0, mimeType.indexOf('/') + 1);
        if (mimeTypeMap.hasOwnProperty(mimeTypePrefix)) {
          return mimeTypeMap[mimeTypePrefix][0];
        } else {
          return mimeTypeMap['attachment'][0];
        }
      }
    };

    var loadAttachmentIcon = function (attachment) {
      return new Promise((resolve) => {
        if (attachment.icon.iconSetType === 'IMAGE') {
          var image = new Image();
          image.onload = function () {
            resolve(attachment);
          };
          image.src = attachment.icon.url;
        } else {
          // Nothing to load.
          resolve(attachment);
        }
      });
    };

    return {
      getIcon: getAttachmentIcon,
      loadIcon: loadAttachmentIcon
    };
  });
// End JavaScript-only code.
}).apply(']]#', $jsontool.serialize([$mimetypeMap, $extensionMap]));
