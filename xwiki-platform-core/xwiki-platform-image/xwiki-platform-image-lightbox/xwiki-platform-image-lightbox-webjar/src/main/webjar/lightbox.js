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
define('xwiki-lightbox-messages', {
  prefix: 'core.viewers.attachments.',
  keys: [
    'date',
    'author'
  ]
});

define('xwiki-lightbox-config', ['jquery'], function($) {
  var config = JSON.parse($('#lightbox-config').text());
  $('body').append($(config.HTMLTemplate));
});

define('xwiki-lightbox-description', [
  'jquery',
  'xwiki-l10n!xwiki-lightbox-messages',
  'moment',
  'moment-jdateformatparser'
], function($, l10n, moment) {
  var _cachedAttachments = {};

  var invalidateCachedAttachments = function() {
    _cachedAttachments = {};
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

  var updateDescriptionCaption = function(imageData, attachmentData) {
    if (imageData) {
      // Verify to not display the url as caption, since this is the default value for alt.
      var alt = imageData.alt == decodeURIComponent(imageData.href) ? '' : imageData.alt;
      $('.lightboxDescription .caption').html(imageData.caption || alt || imageData.title);
    }

    if (!$('.lightboxDescription .caption').is(':empty')) {
      return;
    }

    if (attachmentData && attachmentData.name) {
      $('.lightboxDescription .caption').html(attachmentData.name);
    } else if (imageData && imageData.fileName) {
      $('.lightboxDescription .caption').html(imageData.fileName);
    }
  };

  var updateDescriptionMetadata = function(imageData, attachmentData) {
    // If the first paragraph is a caption, then we should also display the file name, when exists.
    if (imageData.caption && attachmentData.name) {
      $('.lightboxDescription .title').text(attachmentData.name);
    }

    if (attachmentData.authorName) {
      $('.lightboxDescription .publisher').text(l10n.get('author', attachmentData.authorName));
    }

    if (attachmentData.date) {
      var dateFormat = moment().toMomentFormatString($('.lightboxDescription .date').attr('data-dateFormat'));
      $('.lightboxDescription .date').text(l10n.get('date', moment(attachmentData.date).format(dateFormat)));
    }
  };

  /**
   * Update lightbox description using given information.
   */
  var updateDescriptionData = function(imageData, attachmentData) {
    updateDescriptionCaption(imageData, attachmentData);

    if (attachmentData) {
      updateDescriptionMetadata(imageData, attachmentData);
    }
  };

  /**
   * Get information for attachment, when this can be found inside the current page.
   */
  var getAttachmentInfo = function(imageURL, fileName) {
    var deferred = $.Deferred();
    if (_cachedAttachments[imageURL] != undefined) {
      deferred.resolve(_cachedAttachments[imageURL]);
    } else {
      var serviceDocRef = XWiki.Model.resolve('XWiki.Lightbox.AttachmentMetaDataService', XWiki.EntityType.DOCUMENT);
      var serviceDocURL = new XWiki.Document(serviceDocRef).getURL('get', 'outputSyntax=plain');
      $.getJSON(serviceDocURL, {imageURL}).done(function (attachment) {
        _cachedAttachments[imageURL] = attachment;
        deferred.resolve(attachment);
      }).fail(function () {
        // For an external URL, try to display at least the image name.
        if (fileName) {
          _cachedAttachments[imageURL] = {'name': fileName};
          deferred.resolve({'name': fileName});
        } else {
          deferred.reject();
        }
      });
    }

    return deferred.promise();
  };

  var addSlideDescription = function(imageData) {
    clearDescription();
    toggleDescription();

    getAttachmentInfo(imageData.href, imageData.fileName).done(function(attachmentData) {
      updateDescriptionData(imageData, attachmentData);
      toggleDescription();
    }).fail(function() {
      updateDescriptionData(imageData);
      toggleDescription();
    });
  };

  $(document).on('click', '#blueimp-gallery .slides', toggleDescription);

  return {
    invalidateCachedAttachments: invalidateCachedAttachments,
    addSlideDescription: addSlideDescription
  };
});

define('xwiki-lightbox', [
  'jquery',
  'xwiki-lightbox-description',
  'blueimp-gallery',
  'xwiki-lightbox-config',
  'blueimp-gallery-fullscreen',
  'blueimp-gallery-indicator'
], function($, lightboxDescription, gallery) {
  var openedLightbox;
  var slidesData;
  var lightboxImages;

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
    lightboxImages.popover({
      content: function() {
        return $('#imageToolbarTemplate').html();
      },
      html: true,
      // The copyImageId tooltip attributes are deleted by sanitization.
      sanitize: false,
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
      $('.openLightbox').data('index', [...lightboxImages].indexOf(img));
    }).on('inserted.bs.popover', function() {
      $('.popover .imageDownload').attr('href', this.src);
      $('.popover .imageDownload').attr('download', getImageName(this.src));
      var imageId = getImageId(this);
      if (imageId) {
        $('.popover .permalink').attr('href', '#' + imageId);
        $('.popover .permalink').removeClass('hidden');
        $('.popover .copyImageId').attr('data-imageId', imageId);
        $('.popover .copyImageId').parent().removeClass('hidden');
      }
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
    var name = decodeURI(new URL(imageURL).pathname.split("/").pop());
    // Only accept names that may have a file extension.
    if (/\.[a-zA-Z]*/g.test(name)) {
      return name;
    }
  };

  /**
   * Extract the image id. Prefer the id manually added to the caption over the automatically generated one.
   */
  var getImageId = function(img) {
    // Select the first caption child element which specifies an id.
    var figureCaptionIdElement = $(img).closest('figure').find('figcaption').find('[id]')[0];
    if (figureCaptionIdElement) {
      return $(figureCaptionIdElement).attr('id');
    }
    return $(img).attr('id');
  };

  /**
   * Rescale image to original size.
   */
  var removeResizeParams = function(imageURL) {
    var url = new URL(imageURL);
    var searchParams = new URLSearchParams(url.search);
    searchParams.delete('width');
    searchParams.delete('height');
    url.search = searchParams.toString();
    return url.toString();
  };

  /**
   * Scale image to thumbnail. This may not be applied for external urls.
   */
  var createThumbnailURL = function(imageURL) {
    var url = new URL(imageURL);
    var searchParams = new URLSearchParams(url.search);
    searchParams.append('width', '150');
    searchParams.append('height', '150');
    url.search = searchParams.toString();
    return url.toString();
  };

  /**
   * Extract the image caption added using the Figure macro.
   */
  var getImageCaption = function(image) {
    var figure = image.closest('figure');
    if (image.closest('figure').length > 0) {
      var figCaptionContent = figure.find('figcaption').html();
      return $('<div></div>').html(figCaptionContent);
    }
  };

  /**
   * Compute the slides data by extracting information from the selected images.
   */
  var getSlidesData = function() {
    var slidesData = [];
    lightboxImages.each(function() {
      var imageURL = removeResizeParams(this.src);
      var caption = getImageCaption($(this));
      slidesData.push({
        href: imageURL,
        thumbnail: createThumbnailURL(imageURL),
        caption: caption,
        fileName: getImageName(imageURL),
        alt: $(this).attr('alt'),
        title: $(this).attr('title'),
        id: getImageId(this)
      });
    });
    return slidesData;
  };

  /**
   * Open Gallery lightbox at the current index.
   */
  var openLightbox = function(e) {
    e.preventDefault();
    var options = {
      container: '#blueimp-gallery',
      index: parseInt($('.openLightbox').data('index')),
      // The class names are overridden since we changed the styles.
      closeClass: 'escape',
      playPauseClass: 'autoPlay',
      onslide: function(index, slide) {
        var imageData = this.list[index];
        lightboxDescription.addSlideDescription(imageData);
        $(slide).find('img').attr('alt', imageData.alt);

        // Set the attributes for the download button inside lightbox.
        $('#lightboxDownload').attr('href', imageData.href);
        $('#lightboxDownload').attr('download', imageData.fileName);
        // Save the image ID, or hide the action in case it doesn't exist.
        var copyImageId = $('#blueimp-gallery .copyImageId');
        if (imageData.id) {
          copyImageId.parent().removeClass('hidden');
          copyImageId.attr('data-imageId', imageData.id);
        } else {
          copyImageId.parent().addClass('hidden');
          copyImageId.removeAttr('data-imageId');
        }
      }
    };
    openedLightbox = gallery(slidesData, options);
  };

  /**
   * Initialize the lightbox functionality for a set of images.
   */
  var initLightboxFunctionality = function() {
    // The lightbox will be added to xwikicontent images that don't explicitly disable it.
    lightboxImages = $('#xwikicontent img')
      .filter((i, img) => $(img.closest('[data-xwiki-lightbox]')).data('xwikiLightbox') != false);
    slidesData = getSlidesData();
    lightboxDescription.invalidateCachedAttachments();
    enableToolbarPopovers();
  };

  $(document).on('click', '.openLightbox', openLightbox);

  $(document).on('click', '#lightboxFullscreen', function() {
    // Open lightbox in fullscreen mode, or close it if already open.
    if (!$('#lightboxFullscreen').data('open')) {
      openedLightbox.requestFullScreen($('#blueimp-gallery')[0]);
      $('#lightboxFullscreen').data('open', true);
    } else {
      openedLightbox.exitFullScreen();
      $('#lightboxFullscreen').data('open', false);
    }
  });

  $(function() {
    initLightboxFunctionality();
  });

  $(document).on('click', '.copyImageId', function(event) {
    event.preventDefault();
    var copyIdButton = this;
    navigator.clipboard.writeText($(copyIdButton).attr('data-imageId')).then(function() {
      // Inform the user that the image ID was copied to clipboard.
      $(copyIdButton).tooltip('show');
      setTimeout(function() {
        $(copyIdButton).tooltip('hide');
      }, 2000);
    });
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