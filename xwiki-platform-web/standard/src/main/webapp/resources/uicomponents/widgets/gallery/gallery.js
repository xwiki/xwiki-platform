var XWiki = (function (XWiki) {
// Start XWiki augmentation.
XWiki.Gallery = Class.create({
  initialize : function(container) {
    this.images = this._collectImages(container);

    this.container = container.update('<input type="text" tabindex="-1" class="focusCatcher"/><div class="currentImageWrapper"><img class="currentImage" alt="${escapetool.xml($msg.get("core.widgets.gallery.currentImage"))}"/></div><div class="navigation"><div class="previous" title="${escapetool.xml($msg.get("core.widgets.gallery.previousImage"))}">&lt;</div><div class="next" title="${escapetool.xml($msg.get("core.widgets.gallery.nextImage"))}">&gt;</div><div style="clear:both"></div></div><div class="index">0 / 0</div><div class="maximize" title="${escapetool.xml($msg.get("core.widgets.gallery.maximize"))}"></div>');
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
    var imageElements = container.getElementsByTagName('img');
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
    Element.removeClassName(this.currentImage.parentNode, 'loading');
    this.currentImage.style.visibility = 'visible';
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
      defualt:
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
      this._updateSize();
    } else {
      this._resetSize();
    }
  },
  _updateSize : function() {
    var dimensions = document.viewport.getDimensions();
    // Remove container padding;
    var width = dimensions.width - 20;
    var height = dimensions.height - 20;
    this.container.style.width = width + 'px';
    this.container.style.height = height + 'px';
    this.currentImage.parentNode.style.height = height + 'px';
    this.currentImage.parentNode.style.lineHeight = height + 'px';
    this.currentImage.style.maxHeight = height + 'px';
    // Remove width reserved for the navigation arrows.
    this.currentImage.style.maxWidth = (width - 128) + 'px';
  },
  _resetSize : function() {
    this.container.style.cssText = '';
    this.container.removeAttribute('style');
    this.currentImage.parentNode.style.cssText = '';
    this.currentImage.parentNode.removeAttribute('style');
    this.currentImage.style.cssText = '';
    this.currentImage.removeAttribute('style');
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
    this.indexDisplay.firstChild.nodeValue = (index + 1) + ' / ' + this.images.length;
  }
});
// End XWiki augmentation.
return XWiki;
}(XWiki || {}));

Element.observe(document, "dom:loaded", function() {
  $$('.gallery').each(function(gallery) {
    new XWiki.Gallery(gallery);
  });
});
