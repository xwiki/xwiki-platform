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
  var cachedAttachments = {};

  /*
   * Make sure that the toolbar will remain open also while hovering it, not just the image.
   */
  var keepToolbarOpenOnHover = function(image, timeout) {
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
    var timeout;
    // Activate the lightbox for all images inside the xwikicontent. TODO: filter to consider only rendered images.
    $('#xwikicontent img').popover({
      content: function() {
        return $('#imageToolbarTemplate').html();
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
      keepToolbarOpenOnHover($(this), timeout);
    });
  };

  /*
   * Extract the image name from the url. This may not be applied for external urls.
   */
  var getImageName = function(imageURL) {
    var name = new URL(imageURL).pathname.split("/").pop();
    // Only accept names that may have a file extension.
    if (/\.[a-zA-Z]*/g.test(name)) {
      return name;
    }
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

  var clearDescription = function() {
    $('.lightboxDescription .caption').empty();
    $('.lightboxDescription .title').empty();
    $('.lightboxDescription .publisher').empty();
    $('.lightboxDescription .date').empty();
  };

  /**
   * Hide or display the lightbox description.
   */
  var toggleDescription = function() {
    var hasControls = $('.blueimp-gallery-controls').length > 0;
    var nonEmptyElements = $('#blueimp-gallery').find('.caption, .title, .publisher, .date')
      .filter((i, el) => !$(el).is(':empty'));

    if (hasControls && nonEmptyElements.length > 0) {
      $('.lightboxDescription').css('display', 'flex');
    } else {
      $('.lightboxDescription').css('display', 'none');
    }
  };

  /**
   * Update lightbox description using given information.
   */
  var updateLightboxDescription = function(metadata, caption, alt, title) {
    $('.lightboxDescription .caption').html(caption || alt || title || (metadata && metadata.name));

    if (metadata) {
      // If the first paragraph is a caption, then we should also display the file name, when exists.
      if (caption && metadata.name) {
        $('.lightboxDescription .title').text(metadata.name);
      }

      if (metadata.author) {
        $('.lightboxDescription .publisher')
          .text(l10n.get('author', XWiki.Model.resolve(metadata.author, XWiki.EntityType.DOCUMENT).name));
      }

      if (metadata.date) {
        $('.lightboxDescription .date').text(l10n.get('date', new Date(metadata.date).toLocaleDateString()));
      }
    }

    toggleDescription();
  };

  /**
   * Get information for attachment, when this can be found inside the current page.
   */
  var getAttachmentInfo = function(imageURL) {
    var deferred = $.Deferred();
    if (cachedAttachments[imageURL] != undefined) {
      deferred.resolve(cachedAttachments[imageURL]);
    } else {
      var serviceDoctRef = XWiki.Model.resolve('XWiki.Lightbox.GetPageAttachmentsService', XWiki.EntityType.DOCUMENT);
      var serviceDocURL = new XWiki.Document(serviceDoctRef).getURL('get', 'outputSyntax=plain');
      $.ajax(serviceDocURL, {
        method: 'GET',
        dataType: 'json',
        data: {'imageURL': imageURL}
      }).done(function (data) {
        var attachment = data.attachments.find(field => field.name == getImageName(imageURL));
        cachedAttachments[imageURL] = attachment;
        deferred.resolve(attachment);
      }).fail(function (jqXHR) {
        // For an external URL, try to display at least the image name.
        var fileName = getImageName(imageURL);
        if (fileName) {
          cachedAttachments[imageURL] = {'name': fileName};
          deferred.resolve({'name': fileName});
        } else {
          deferred.reject();
        }
      });
    }

    return deferred.promise();
  };

  var getImageCaption = function(image) {
    var figure = image.closest('figure');
    if (image.closest('figure').length > 0) {
      var figCaptionContent = figure.find('figcaption').html();
      return $('<div></div>').html(figCaptionContent);
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
        caption: caption,
        alt: $(this).attr('alt'),
        title: $(this).attr('title')
      });
    });

    var options = {
      container: '#blueimp-gallery',
      index: parseInt($('#openLightbox').data('index')),
      // The class names are overridden since we changed the styles.
      closeClass: 'escape',
      playPauseClass: 'autoPlay',
      // Avoid the hide done by the library on the default h3 title element.
      titleElement: 'h4',
      onslide: function(index, slide) {
        clearDescription();
        toggleDescription();
        var imageData = this.list[index];
        var slideImage = $(slide).find('img');
        var caption = imageData.caption;
        getAttachmentInfo(slideImage[0].src).done(function(data) {
          updateLightboxDescription(data, caption, imageData.alt, imageData.title);
        }).fail(function() {
          updateLightboxDescription();
        });
        slideImage.attr('alt', imageData.alt);
        // Set the attributes for the download button inside lightbox.
        $('#lightboxDownload').attr('href', imageData.href);
        $('#lightboxDownload').attr('download', getImageName(imageData.href));
      }
    };
    myOpenLightbox = gallery(media, options);
  };

  /**
   * Initialize the lightbox functionality for a set of images.
   */
  var initLightboxFunctionality = function() {
    cachedAttachments = {};
    enableToolbarPopovers();
  };

  $(document).on('click', '#openLightbox', openLightbox);

  $(document).on('click', '#lightboxFullscreen', function() {
    // Open lightbox in fullscreen mode, or close it if already open.
    if (!$('#lightboxFullscreen').data('open')) {
      myOpenLightbox.requestFullScreen($('#blueimp-gallery')[0]);
      $('#lightboxFullscreen').data('open', true);
    } else {
      myOpenLightbox.exitFullScreen();
      $('#lightboxFullscreen').data('open', false);
    }
  });

  $(document).on('click', '#blueimp-gallery .slides', toggleDescription());

  $(function() {
    initLightboxFunctionality();
  });

  $(document).on('xwiki:dom:updated', function() {
    if ($('#blueimp-gallery').data('isViewMode')) {
      initLightboxFunctionality();
    }
  });

  $(document).on('xwiki:actions:edit', function() {
    $('#blueimp-gallery').data('isViewMode', false);
    // Remove the image toolbars popovers.
    $('.popover').popover('destroy');
  });

  $(document).on('xwiki:actions:view', function() {
    $('#blueimp-gallery').data('isViewMode', true);
  });
});