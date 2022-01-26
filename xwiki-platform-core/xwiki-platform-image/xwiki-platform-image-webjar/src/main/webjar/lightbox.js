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
define('lightboxTranslationKeys', {
  prefix: 'core.viewers.attachments.',
  keys: [
    'date',
    'author'
  ]
});

require(['jquery', 'xwiki-meta', 'blueimp-gallery', 'xwiki-l10n!lightboxTranslationKeys', 'blueimp-gallery-fullscreen',
    'blueimp-gallery-indicator'], function($, xm, gallery, l10n) {
  var myOpenLightbox;
  var timeout;

  /*
   * Make sure that the toolbar will remain open also while hovering it, not just the image.
   */
  var keepToolbarOpenOnHover = function(image) {
    timeout = setTimeout(function() {
      image.popover('hide');
    }, 3000);
    $('.popover').on('mouseenter', function() {
      clearTimeout(timeout);
    });
    $('.popover').on('mouseleave', function() {
      image.popover('hide');
    });
  };

  /**
   * Assign to each selected image a toolbar popover with download and lightbox options.
   */
  var enableToolbarPopovers = function() {
    // Activate the lightbox for all images inside the xwikicontent. TODO: filter to consider only rendered images.
    $('#xwikicontent img').popover({
      content: function() {
        return $('#toolbarTemplate').html();
      },
      html: true,
      // The popover needs to be placed outside of the xwiki content to not depend on it's overflow.
      container: 'body',
      placement: 'bottom',
      trigger: 'manual'
    }).on("show.bs.popover", function(e){
      var img = e.target;
      // Hide all other popovers.
      $('.popover').hide();
      // Set the attributes for the download button inside lightbox.
      $('#lightboxDownload').attr('href', img.src);
      $('#lightboxDownload').attr('download', getImageName(img.src));
      // Remember the index of the image to show first.
      $('#openLightbox').data('index', [...$('#xwikicontent img')].indexOf(img));
    }).on('shown.bs.popover', function(e) {
      $('.popover .imageDownload').attr('href', e.target.src);
      $('.popover .imageDownload').attr('download', getImageName(e.target.src));
    }).on('mouseenter', function() {
      clearTimeout(timeout);
      $(this).popover('show');
    }).on('mouseleave', function() {
      keepToolbarOpenOnHover($(this));
    });
  };

  /**
   * Remove the image toolbars popovers.
   */
  var closeImageToolbars = function() {
    $('.popover').popover('destroy');
  };

  /*
   * Extract the image name from the url. This may not be applied for external urls.
   */
  var getImageName = function(imageLink) {
    return new URL(imageLink).pathname.split("/").pop();
  };

  /**
   * Rescale image to original size.
   */
  var removeResizeParams = function(imageSrc) {
    var url = new URL(imageSrc);
    var searchParams = new URLSearchParams(url.search);
    searchParams.delete('width');
    searchParams.delete('height');
    url.search = searchParams.toString();
    return url.toString();
  };

  /**
   * Scale image to thumbnail. This may not be applied for external urls.
   */
  var createThumbnailURL = function(imageSrc) {
    var url = new URL(imageSrc);
    var searchParams = new URLSearchParams(url.search);
    searchParams.append('width', '150');
    searchParams.append('height', '150');
    url.search = searchParams.toString();
    return url.toString();
  };

  /**
   * Open lightbox in fullscreen mode, or close it if already open.
   */
  var toggleLightboxFullscreen = function() {
    if (!$('#lightboxFullscreen').data('open')) {
      myOpenLightbox.requestFullScreen($('#blueimp-gallery')[0]);
      $('#lightboxFullscreen').data('open', true);
    } else {
      myOpenLightbox.exitFullScreen();
      $('#lightboxFullscreen').data('open', false);
    }
  };

  /**
   * Update lightbox description using given information.
   */
  var updateLightboxDescription = function(data, caption) {
    if (data != undefined) {
      $('.lightboxDescription').css('display', 'block');
      $('.lightboxDescription').find('.title').text(data.name);
      var translationPublisher = l10n.get('author', XWiki.Model.resolve(data.author, XWiki.EntityType.DOCUMENT).name);
      $('.lightboxDescription').find('.publisher').text(translationPublisher);
      var translationDate = l10n.get('date', new Date(data.date).toLocaleDateString());
      $('.lightboxDescription').find('.date').text(translationDate);

      if (caption != undefined) {
        $('.lightboxDescription').find('.caption').text(caption);
      } else {
        $('.lightboxDescription').find('.caption').empty();
      }
    } else {
      $('.lightboxDescription').css('display', 'none');
    }
  };

  /**
   * Get information for attachment, when this can be found inside the current page.
   * TODO: consider the path of the attachment, not just the current page.
   */
  var getAttachmentInfo = function(imageLink) {
    var deferred = $.Deferred();
    var attachmentsURL = xm.restURL + `/attachments`;
    $.ajax(attachmentsURL, {
      method: 'GET',
      dataType: 'json'
    }).done(function (data, textStatus, jqXHR) {
      var attachment = data.attachments.find(field => field.name == getImageName(imageLink));
      deferred.resolve(attachment);
    }).fail(function () {
      deferred.reject();
    });
    return deferred.promise();
  };

  var getImageCaption = function(image) {
    if (image.closest('figure').length > 0) {
      return image.closest('p').siblings('figcaption').text();
    }
  };

  /**
   * Open Gallery lightbox at the current index.
   */
  var openLightbox = function() {
    var media = [];
    $('#xwikicontent').find('img').each(function(index) {
      var imageURL = removeResizeParams(this.src);
      var caption = getImageCaption($(this));
      media.push({
        href: imageURL,
        thumbnail: createThumbnailURL(imageURL),
        caption: caption
      });
    });

    var options = {
      container: '#blueimp-gallery',
      index: parseInt($('#openLightbox').data('index')),
      // The class names are overridden since we changed the styles.
      closeClass: 'escape',
      playPauseClass: 'autoPlay',
      onslide: function(index, slide) {
        var caption = this.list[index].caption;
        getAttachmentInfo($(slide).find('img')[0].src).done(function(data) {
          updateLightboxDescription(data, caption);
        });
      }
    };
    myOpenLightbox = gallery(media, options);
  };

  /**
   * Initialize the lightbox functionality for a set of images.
   */
  var initLightboxFunctionality = function() {
    console.log("Init");
    enableToolbarPopovers();
  };

  $(document).on('click', '#openLightbox', openLightbox);

  $(document).on('click', '#lightboxFullscreen', toggleLightboxFullscreen);

  $(window).on('load', function() {
    initLightboxFunctionality();
  });

  $(document).on('xwiki:dom:updated', function() {
    if ($('#blueimp-gallery').data('isViewMode')) {
      initLightboxFunctionality();
    }
  });

  $(document).on('xwiki:actions:edit', function() {
    $('#blueimp-gallery').data('isViewMode', false);
    closeImageToolbars();
  });

  $(document).on('xwiki:actions:view', function() {
    $('#blueimp-gallery').data('isViewMode', true);
  });
});