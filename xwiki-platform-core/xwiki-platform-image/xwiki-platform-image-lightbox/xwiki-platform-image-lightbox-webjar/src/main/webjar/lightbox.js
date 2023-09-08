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
  'moment-jdateformatparser',
  'moment-timezone'
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
      // This should be updated after fixing XWIKI-19808: Proper javascript API to perform Date Formatting.
      var dateFormat = moment().toMomentFormatString($('.lightboxDescription .date').data('dateFormat'));
      var timezone = $('.lightboxDescription .date').data('userTimeZone');
      $('.lightboxDescription .date')
        .text(l10n.get('date', moment(attachmentData.date).tz(timezone).format(dateFormat)));
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
  var keepToolbarOpenOnHover = function() {
    var hideTimeout;
    // Hide the image popover after 3 seconds (if the mouse doesn't enter the popover).
    hideTimeout = setTimeout(function() {
      $('#imagePopoverContainer').popover('hide');
    }, 3000);
    // Don't hide the image popover when the mouse is over it.
    $('#imagePopoverContainer .popover').on('mouseenter', function() {
      clearTimeout(hideTimeout);
    });
    // Hide the image popover when the mouse leaves its area, but with a delay since it's easy to go out
    // without wanting to (e.g. when going diagonally towards the image popover, to click on the anchor button).
    $('#imagePopoverContainer .popover').on('mouseleave', function() {
      hideTimeout = setTimeout(function() {
        $('#imagePopoverContainer').popover('hide');
      }, 500);
    });
    return hideTimeout;
  };

  /**
   * Assign to each selected image a toolbar popover with download and lightbox options.
   */
  var enableToolbarPopovers = function() {
    var hideTimeout;
    var showTimeout;
    // In order to place the popover at cursor location, it would be added to an element that changes its position
    // based on mouse events.
    var popoverContainer = $('#imagePopoverContainer');
    popoverContainer.popover({
      content: function() {
        return $('#imageToolbarTemplate').html();
      },
      html: true,
      // The copyImageId tooltip attributes are deleted by sanitization.
      sanitize: false,
      container: '#imagePopoverContainer',
      placement: 'bottom',
      trigger: 'manual'
    }).on("show.bs.popover", function(){
      var img = $("#imagePopoverContainer").data('target');
      // Hide all other popovers.
      $('.popover').hide();
      // Set the attributes for the download button inside lightbox.
      $('#lightboxDownload').attr('href', img.src);
      $('#lightboxDownload').attr('download', getImageName(img.src));
      // Remember the index of the image to show first.
      $('.openLightbox').data('index', [...lightboxImages].indexOf(img));
    }).on('inserted.bs.popover', function() {
      // Move the popover 2 pixels below, in order to still have the cursor over the image when the popover is
      // displayed.
      $(this).css('top', $(this).position().top + 2 +'px');
      var img = $("#imagePopoverContainer").data('target');
      $('.popover .imageDownload').attr('href', img.src);
      $('.popover .imageDownload').attr('download', getImageName(img.src));
      var imageId = getImageId(img);
      if (imageId) {
        $('.popover .permalink').attr('href', '#' + imageId);
        $('.popover .permalink').removeClass('hidden');
        $('.popover .copyImageId').attr('data-imageId', imageId);
        $('.popover .copyImageId').parent().removeClass('hidden');
      }
    });
    // We add the sr-only buttons to expand the lightbox
    let toggler = $(JSON.parse($('#lightbox-config').text()).togglerTemplate);
    lightboxImages.each(function() {
      let lightboxToggle = toggler.clone();
      lightboxToggle.get(0)
        .addEventListener('focus', (e) => {lightboxToggle.get(0).classList.remove('sr-only');});
      lightboxToggle.get(0)
        .addEventListener('focusout', (e) => {lightboxToggle.get(0).classList.add('sr-only');});

      lightboxToggle.get(0).addEventListener('click', (e) => {
        clearTimeout(showTimeout);
        popoverContainer.data('target', this);
        let offsetX = e.pageX;
        let offsetY = e.pageY;
        if (offsetX === 0 && offsetY === 0) {
          // When the click is triggered from keyboard,
          // we display the lightbox menu popover from the toggler.
          let offset = lightboxToggle.offset();
          offsetX = offset.left;
          offsetY = offset.top;
        }
        popoverContainer.css({left: offsetX, top: offsetY});
        popoverContainer.popover('show');
        $('.openLightbox').focus();
        /* The lightbox actions popover (bootstrap modal) is generated at the end of the DOM tree.
          Navigating with the keyboard to its end makes the focus cursor leave the document,
          and it's not possible to force it back where we want it.
          Since this position in the DOM is different to its visual position and this behavior is unexpected,
          we want to prevent this behavior.
          In order to do so, we introduce a hidden element that will shortly receive focus
          (instead of the focus leaving to the browser tabs for example)
          before focus can be moved back to its expected position.
          This does not create a focus trap since this element and the lightbox can only be
          accessed from and leaving to a specific context.
          Moving the popover to its proper place in the DOM involves UI changes that would need
          a complete overhaul of the popover element, making it a worse alternative. */
        popoverContainer.append($("<div id='popoverKeyboardEscaper' class='sr-only' tabindex='0'>"));
        // Make sure the popover can be exited with basic keyboard navigation.
        let focusLeavingPopover = function (event) {
          if (!popoverContainer.children().get(0).contains(event.relatedTarget)) {
            popoverContainer.get(0).removeEventListener('focusout', focusLeavingPopover);
            lightboxToggle.focus();
            popoverContainer.popover('hide');
            $('#popoverKeyboardEscaper').remove();
          }
        };
        popoverContainer.get(0).addEventListener('focusout', focusLeavingPopover);
      });
      lightboxToggle.insertAfter(this);
    });
    lightboxImages.on('mouseenter', function(e) {
      clearTimeout(hideTimeout);
      popoverContainer.data('target', e.target);
    }).on('mousemove', function(e) {
      // Delay to show the popover until the mouse stops moving.
      clearTimeout(showTimeout);
      showTimeout = setTimeout(function() {
        popoverContainer.css({top: e.pageY, left: e.pageX });
        popoverContainer.popover('show');
      }, 500);
    }).on('mouseleave', function() {
      clearTimeout(showTimeout);
      hideTimeout = keepToolbarOpenOnHover();
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
