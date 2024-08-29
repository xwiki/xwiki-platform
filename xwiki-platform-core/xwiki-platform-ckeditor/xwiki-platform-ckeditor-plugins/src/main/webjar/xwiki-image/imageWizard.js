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

define('imageWizard', ['jquery', 'imageSelector', 'imageEditor'], function($, imageSelector, imageEditor) {
  'use strict';

  function backToSelectionOrFinish(data) {
    if (data.action === 'selectImage') {
      return selectAndEdit(data);
    } else {
      return data;
    }
  }

  function editOnly(params) {
    return imageEditor(params)
      .then(backToSelectionOrFinish);
  }

  function selectAndEdit(params) {
    params = params || {};
    if (params.newImage === undefined) {
      params.newImage = true;
    }
    return imageSelector.open(params)
      .then(imageEditor)
      .then(backToSelectionOrFinish);
  }

  return function(params) {
    if (CKEDITOR.currentInstance) {
      params.currentDocument = CKEDITOR.currentInstance.config.sourceDocument.documentReference;
    }

    // Skip the wizard if setImageData is set.
    if (params.setImageData) {
      return $.Deferred().resolve(params.setImageData);
    }

    if (params.isInsert === false) {
      return editOnly(params);
    } else {
      return selectAndEdit(params);
    }
  };
});
