var XWiki = (function (XWiki) {
// Start XWiki augmentation.
XWiki.Gallery = Class.create({
  initialize : function(container) {
    this.images = this._collectImages(container);

    this.container = container.update('<input type="text" tabindex="-1" class="focusCatcher"/><div class="currentImageWrapper"><img class="currentImage" alt="${escapetool.xml($msg.get("core.widgets.gallery.currentImage"))}"/><span class="currentImagePinPoint"></span></div><div class="previous" title="${escapetool.xml($msg.get("core.widgets.gallery.previousImage"))}">&lt;</div><div class="next" title="${escapetool.xml($msg.get("core.widgets.gallery.nextImage"))}">&gt;</div><div class="index">0 / 0</div><div class="maximize" title="${escapetool.xml($msg.get("core.widgets.gallery.maximize"))}"></div>');
    this.container.addClassName('xGallery');

    this.focusCatcher = this.container.down('.focusCatcher');
    this.focusCatcher.observe('keydown', this._onKeyDown.bindAsEventListener(this));
    this.container.observe('click', function() {
      this.focusCatcher.focus();
    }.bind(this));

    this.container.down('.previous').observe('click', this._onPreviousImage.bind(this));
    this.container.down('.next').observe('click', this._onNextImage.bind(this));

    this.currentImage = this.container.down('.currentImage');
    this.currentImage.observe('load', this._onLoadImage.bind(this));
    this.currentImage.observe('error', this._onErrorImage.bind(this));
    this.currentImage.observe('abort', this._onAbortImage.bind(this));

    this.indexDisplay = this.container.down('.index');

    this.maximizeToggle = this.container.down('.maximize');
    this.maximizeToggle.observe('click', this._onToggleMaximize.bind(this));

    this.show(0);
  },
  _collectImages : function(container) {
    var images = [];
    var imageElements = container.select('img');
    for(var i = 0; i < imageElements.length; i++) {
      var imageElement = imageElements[i];
      images.push({url: imageElement.getAttribute('src'), title: imageElement.title});
      imageElement.removeAttribute('src');
    }
    return images;
  },
  _onPreviousImage : function() {
    this.show(this.index > 0 ? this.index - 1 : this.images.length - 1);
  },
  _onNextImage : function() {
    this.show(this.index < this.images.length - 1 ? this.index + 1 : 0);
  },
  _onLoadImage : function() {
    this._maybeLimitImageSize();
    Element.removeClassName(this.currentImage.parentNode, 'loading');
    this.currentImage.style.visibility = 'visible';
  },
  _maybeLimitImageSize : function() {
    // Limit image size if the browser doesn't support the max-width and max-height CSS properties.
    this.currentImage.style.height = this.currentImage.style.width = '';
    var imageHeight = this.currentImage.offsetHeight;
    var imageWidth = this.currentImage.offsetWidth;
    var availableHeight = this.currentImage.parentNode.offsetHeight;
    // Remove width reserved for the navigation arrows.
    var availableWidth = this.currentImage.parentNode.offsetWidth - 128;
    if (imageHeight > availableHeight || imageWidth > availableWidth) {
      var aspectRatio = imageWidth / imageHeight;
      var height = availableWidth / aspectRatio;
      if (height > availableHeight) {
        this.currentImage.style.height = availableHeight + 'px';
      } else {
        this.currentImage.style.width = availableWidth + 'px';
      }
    }
  },
  _onErrorImage: function() {
  },
  _onAbortImage: function() {
  },
  _onKeyDown : function(event) {
    var stop = true;
    switch(event.keyCode) {
      case Event.KEY_LEFT:
        this._onPreviousImage();
        break;
      case Event.KEY_RIGHT:
        this._onNextImage();
        break;
      case Event.KEY_HOME:
        this.show(0);
        break;
      case Event.KEY_END:
        this.show(this.images.length - 1);
        break;
      case Event.KEY_ESC:
        if (this.container.hasClassName('maximized')) {
          this._onToggleMaximize();
        }
        break;
      case 70: /* F */
        this._onToggleMaximize();
        break;
      default:
        stop = false;
        break;
    }
    if (stop) {
      Event.stop(event);
    }
  },
  _onToggleMaximize : function() {
    this.maximizeToggle.toggleClassName('maximize');
    this.maximizeToggle.toggleClassName('minimize');
    this.maximizeToggle.title = this.maximizeToggle.hasClassName('maximize') ? '${escapetool.javascript($msg.get("core.widgets.gallery.maximize"))}' : '${escapetool.javascript($msg.get("core.widgets.gallery.minimize"))}';
    this.container.toggleClassName('maximized');
    $(document.documentElement).toggleClassName('maximized');
    if (this.container.hasClassName('maximized')) {
      this._maybeUpdatePosition();
      this._updateSize();
    } else {
      this._resetSize();
      this._maybeResetPosition();
    }
  },
  _isIE6 : function() {
    return Prototype.Browser.IE && navigator.appVersion.indexOf('MSIE 6') > -1;
  },
  /* Hack required to overcome the broken support for position:fixed in IE6. */
  _maybeUpdatePosition : function() {
    if (this._isIE6()) {
      this.placeHolder = this.placeHolder || new Element('div', {'class': 'xGalleryPlaceHolder'});
      this.container.parentNode.replaceChild(this.placeHolder, this.container);
      document.body.appendChild(this.container);
      // The focus was lost when the focus catcher was detached.
      setTimeout(function() {
        this.focusCatcher.focus();
      }.bind(this), 0);
    }
  },
  /* Hack required to overcome the broken support for position:fixed in IE6. */
  _maybeResetPosition : function() {
    if (this._isIE6()) {
      this.placeHolder.parentNode.replaceChild(this.container, this.placeHolder);
      // The focus was lost when the focus catcher was detached.
      setTimeout(function() {
        this.focusCatcher.focus();
      }.bind(this), 0);
    }
  },
  _updateSize : function() {
    var dimensions = document.viewport.getDimensions();
    // Adjust dimensions for IE6 Quirks mode, which isn't supported by Prototype.js
    if (dimensions.width <= 0) dimensions.width = document.body.clientWidth;
    if (dimensions.height <= 0) dimensions.height = document.body.clientHeight;
    // Remove container padding;
    var width = dimensions.width - 20;
    var height = dimensions.height - 20;
    if (!this._isIE6()) {
      this.container.setStyle({width: width + 'px', height: height + 'px'});
    }
    this.currentImage.up().setStyle({height: height + 'px', lineHeight: height + 'px'});
    // Remove width reserved for the navigation arrows.
    this.currentImage.setStyle({maxHeight: height + 'px', maxWidth: (width - 128) + 'px'});
    this._maybeLimitImageSize();
  },
  _resetSize : function() {
    this.container.style.cssText = '';
    this.container.removeAttribute('style');
    this.currentImage.parentNode.style.cssText = '';
    this.currentImage.parentNode.removeAttribute('style');
    this.currentImage.style.cssText = '';
    this.currentImage.removeAttribute('style');
    this._maybeLimitImageSize();
  },
  show : function(index) {
    if (index < 0 || index >= this.images.length || index == this.index) {
      return;
    }
    this.currentImage.style.visibility = 'hidden';
    Element.addClassName(this.currentImage.parentNode, 'loading');
    this.currentImage.title = this.images[index].title;
    this.currentImage.src = this.images[index].url;
    this.index = index;
    this.indexDisplay.update((index + 1) + ' / ' + this.images.length);
  }
});

function init() {
  $$('.gallery').each(function(gallery) {
    new XWiki.Gallery(gallery);
  });
}

// When the document is loaded, install galleries
(XWiki.isInitialized && init())
|| document.observe('xwiki:dom:loading', init);

// End XWiki augmentation.
return XWiki;
}(XWiki || {}));

