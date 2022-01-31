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
   * Hide or display the lightbox description, considering the caption.
   */
  var toggleDescription = function(display, withCaption) {
    if (display) {
      $('.lightboxDescription').css('display', 'block');
      if (withCaption) {
        $('#blueimp-gallery >div.slides').css('bottom', '10%');
        $('#blueimp-gallery .indicator').css('bottom', '12%');
      } else {
        $('#blueimp-gallery >div.slides').css('bottom', '5%');
        $('#blueimp-gallery .indicator').css('bottom', '7%');
      }
    } else {
      $('.lightboxDescription').css('display', 'none');
      // Center the image.
      $('#blueimp-gallery >div.slides').css('bottom', 0);
      $('#blueimp-gallery .indicator').css('bottom', '2%');
    }
  };

  /**
   * Update lightbox description using given information.
   */
  var updateLightboxDescription = function(metadata, caption) {
    $('.lightboxDescription .caption').empty();
    $('.lightboxDescription .title').empty();
    $('.lightboxDescription .publisher').empty();
    $('.lightboxDescription .date').empty();

    if (caption != undefined) {
      $('.lightboxDescription .caption').html(caption);
    }

    if (metadata != undefined) {
      if (metadata.name != undefined) {
        $('.lightboxDescription .title').text(metadata.name);
      }

      if (metadata.author != undefined) {
        $('.lightboxDescription .publisher')
          .text(l10n.get('author', XWiki.Model.resolve(metadata.author, XWiki.EntityType.DOCUMENT).name));
      }

      if (metadata.date != undefined) {
        $('.lightboxDescription .date').text(l10n.get('date', new Date(metadata.date).toLocaleDateString()));
      }
    }

    var displayDescription = metadata != undefined && $('.blueimp-gallery-controls').length > 0;
    toggleDescription(displayDescription, caption != undefined);
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
        if (jqXHR.status == 404) {
          cachedAttachments[imageURL] = {'name': getImageName(imageURL)};
          // For an external URL, try to display at least the image name.
          deferred.resolve({'name': getImageName(imageURL)});
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
        alt: $(this).attr('alt')
      });
    });

    var options = {
      container: '#blueimp-gallery',
      index: parseInt($('#openLightbox').data('index')),
      // The class names are overridden since we changed the styles.
      closeClass: 'escape',
      playPauseClass: 'autoPlay',
      onslide: function(index, slide) {
        var imageData = this.list[index];
        var slideImage = $(slide).find('img');
        var caption = imageData.caption;
        getAttachmentInfo(slideImage[0].src).done(function(data) {
          updateLightboxDescription(data, caption);
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

  $(document).on('click', '#blueimp-gallery .slides', function() {
    var hasControls = $('.blueimp-gallery-controls').length > 0;
    var isCaptionEmpty = $('#blueimp-gallery .caption').is(':empty');
    toggleDescription(hasControls, !isCaptionEmpty);
  });

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