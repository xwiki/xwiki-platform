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
#set ($l10nKeys = [
 'core.widgets.gallery.previousImage',
 'core.widgets.gallery.currentImage',
 'core.widgets.gallery.nextImage',
 'core.widgets.gallery.index.description',
 'core.widgets.gallery.maximize',
 'core.widgets.gallery.minimize'
])
#set ($l10n = {})
#foreach ($key in $l10nKeys)
  #set ($discard = $l10n.put($key, $services.localization.render($key)))
#end
#[[*/
// Start JavaScript-only code.
define('xwiki-gallery-icons', {
  icons: ['maximize', 'minimize']
});

(function(l10n) {
  "use strict";
require(['jquery', 'xwiki-icon!xwiki-gallery-icons', 'xwiki-events-bridge'], function($, icons) {
const XWiki = window.XWiki = window.XWiki || {};

function createButton(className, title, text) {
  const button = document.createElement('button');
  Object.assign(button, {className, title, textContent: text || ''});
  return button;
}

/**
 * Note that this class can be extended using Prototype.js' Class.create(XWiki.Gallery, {...}). This is why the
 * constructor only delegates to the initialize method (Prototype.js calls only the initialize method when creating an
 * instance of a subclass).
 */
class Gallery {
  constructor(...args) {
    this.initialize(...args);
  }

  initialize(container) {
    this.images = this._collectImages(container);
    // Generate the different parts of the gallery
    const maximizeButton = createButton('maximize', l10n['core.widgets.gallery.maximize']);
    // The CSS expects the expand icon to come first, in order to show only the icon matching the current action.
    maximizeButton.append(...[icons['maximize']?.render(), icons['minimize']?.render()].filter(icon => icon));
    const previousButton = createButton('previous', l10n['core.widgets.gallery.previousImage'], '<');
    const currentImage = document.createElement('img');
    Object.assign(currentImage, {className: 'currentImage', title: l10n['core.widgets.gallery.currentImage']});
    const nextButton = createButton('next', l10n['core.widgets.gallery.nextImage'], '>');
    const imageIndex = document.createElement('div');
    Object.assign(imageIndex, {
      className: 'index',
      tabIndex: 0,
      title: l10n['core.widgets.gallery.index.description'],
      textContent: '0 / 0'
    });
    imageIndex.setAttribute('aria-description', l10n['core.widgets.gallery.index.description']);
    // Replace the content that's left in the container with the gallery parts, in the correct order.
    container.replaceChildren(maximizeButton, previousButton, currentImage, nextButton, imageIndex);
    this.container = container;
    this.container.classList.add('xGallery');

    // Instead of an arbitrary element to catch focus, we use the index.
    // This index already stores the current image state, might as well be responsible for providing quick controls and
    // explanations about these quick controls.
    // Note that wrapping the image in an interactive container to handle this would have been a good solution too.
    // However, this wrapping caused the image to overflow the CSS grid vertically when in maximized mode. 
    // Technically I couldn't find a CSS solution to prevent this, so I decided to make do without wrapping.
    this.focusCatcher = imageIndex;
    this.focusCatcher.addEventListener('keydown', event => this._onKeyDown(event));

    previousButton.addEventListener('click', () => this._onPreviousImage());
    nextButton.addEventListener('click', () => this._onNextImage());
    this.container.addEventListener('click', () => this.focusCatcher.focus());

    this.currentImage = currentImage;
    this.currentImage.addEventListener('load', () => this._onLoadImage());
    this.currentImage.addEventListener('error', () => this._onErrorImage());
    this.currentImage.addEventListener('abort', () => this._onAbortImage());

    this.indexDisplay = imageIndex;

    this.maximizeToggle = maximizeButton;
    this.maximizeToggle.addEventListener('click', () => this._onToggleMaximize());

    this.show(0);
  }

  _collectImages(container) {
    const images = [];
    const imageElements = container.querySelectorAll('img');
    for (const imageElement of imageElements) {
      images.push({url: imageElement.getAttribute('src'), title: imageElement.title, alt: imageElement.alt});
      imageElement.removeAttribute('src');
    }
    return images;
  }

  _onPreviousImage() {
    this.show(this.index > 0 ? this.index - 1 : this.images.length - 1);
  }

  _onNextImage() {
    this.show(this.index < this.images.length - 1 ? this.index + 1 : 0);
  }

  _onLoadImage() {
    this.currentImage.parentNode.classList.remove('loading');
    this.currentImage.style.visibility = 'visible';
  }

  _onErrorImage() {
  }

  _onAbortImage() {
  }

  _onKeyDown(event) {
    let stop = true;
    switch(event.key) {
      case 'ArrowLeft':
        this._onPreviousImage();
        break;
      case 'ArrowRight':
        this._onNextImage();
        break;
      case 'Home':
        this.show(0);
        break;
      case 'End':
        this.show(this.images.length - 1);
        break;
      case 'Escape':
        if (this.container.classList.contains('maximized')) {
          this._onToggleMaximize();
        }
        break;
      case 'f':
      case 'F':
        this._onToggleMaximize();
        break;
      default:
        stop = false;
        break;
    }
    if (stop) {
      event.preventDefault();
      event.stopPropagation();
    }
  }

  _onToggleMaximize() {
    this.maximizeToggle.classList.toggle('maximize');
    this.maximizeToggle.classList.toggle('minimize');
    this.maximizeToggle.title = this.maximizeToggle.classList.contains('maximize') ?
      l10n['core.widgets.gallery.maximize'] : l10n['core.widgets.gallery.minimize'];
    this.container.classList.toggle('maximized');
    document.documentElement.classList.toggle('maximized');
    // When a keyboard shortcut is used, the gallery is not focused by default. In order to keep the screen at the
    // level of the gallery even when minimizing, we need to make sure it's always focused.
    // Without this forced focus, minimizing the gallery by pressing the `Escape` key will
    // unexpectedly send the user to the top of the page.
    this.maximizeToggle.focus();
  }

  show(index) {
    if (index < 0 || index >= this.images.length || index == this.index) {
      return;
    }
    // Update only if it's a different image. Some browsers, e.g. Chrome, don't fire the load event if the image URL
    // doesn't change. Another trick would be to reset the src attribute before setting the actual URL (set to '').
    const imageData = this.images[index];
    if (this.currentImage.src !== imageData.url) {
      this.currentImage.style.visibility = 'hidden';
      this.currentImage.parentNode.classList.add('loading');
      this.currentImage.title = imageData.title;
      const filename = decodeURI(imageData.url.split('/').pop().split('?')[0]);
      // If the alt is just the name of the file, we instead fall back on the human-readable currentImage translation.
      if (filename !== imageData.alt) {
        this.currentImage.alt = imageData.alt;
      } else {
        this.currentImage.alt = l10n['core.widgets.gallery.currentImage'];
      }
      this.currentImage.src = imageData.url;
    }
    this.index = index;
    this.indexDisplay.textContent = (index + 1) + ' / ' + this.images.length;
  }
}

/** Required by Prototype.js' Class.create(), which registers each new subclass on its parent class. */
Gallery.subclasses = [];

XWiki.Gallery = Gallery;

function init(event, data) {
  for (const element of (data?.elements || [document.body])) {
    const galleries = element.matches('.gallery') ? [element] : element.querySelectorAll('.gallery');
    galleries.forEach(gallery => new XWiki.Gallery(gallery));
  }
}

// Don't initialize the galleries when exporting to PDF because we want to include all the images.
if (XWiki.contextaction !== 'export') {
  // When the document is loaded, install galleries
  $(init);

  // Initialize the gallery when it is added after the page is loaded.
  $(document).on('xwiki:dom:updated', init);
}
});
// End JavaScript-only code.
}).apply(']]#', $jsontool.serialize([$l10n]));