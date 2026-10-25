/**
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

/**
 * The id of the Cards layout.
 */
const CARDS_LAYOUT_ID = "cards";

// The requested image widths are rounded up to a multiple of this step, so that the cards share the same resized images
// and the server only has a few sizes to generate and cache.
const IMAGE_WIDTH_STEP = 100;

// A value made only of a URL pointing to an image file.
const IMAGE_URL_PATTERN =
  /^[^\s<>"']+\.(?:apng|avif|bmp|gif|jpe?g|png|svg|webp)(?:[?#][^\s<>"']*)?$/i;

/**
 * The cover of the cards is stored on the descriptor of the Cards layout, in its `coverProperty` field: an empty value
 * means that the cards don't have a cover. Its default value can be set in the Live Data configuration. A hidden
 * property can't be used as the cover.
 *
 * @param logic - the Live Data logic
 * @returns the id of the property used as the card cover, or undefined when the cards don't have a cover
 */
function getCoverPropertyId(logic) {
  const coverProperty =
    logic.getLayoutDescriptor(CARDS_LAYOUT_ID)?.coverProperty;
  return coverProperty &&
    logic.data.query.properties.includes(coverProperty) &&
    logic.isPropertyVisible(coverProperty)
    ? coverProperty
    : undefined;
}

/**
 * Set the property used as the card cover.
 *
 * @param logic - the Live Data logic
 * @param propertyId - the id of the property to use as the card cover, or an empty value to remove the cover. Unknown
 *   properties are ignored.
 */
function setCoverPropertyId(logic, propertyId) {
  const layoutDescriptor = logic.getLayoutDescriptor(CARDS_LAYOUT_ID);
  if (!layoutDescriptor) {
    return;
  }
  if (!propertyId) {
    layoutDescriptor.coverProperty = "";
  } else if (logic.data.query.properties.includes(propertyId)) {
    layoutDescriptor.coverProperty = propertyId;
  }
}

/**
 * Return whether the given image source can be displayed: a http(s) or relative URL, or an image data URL.
 *
 * @param src - the image source
 * @returns true when the source can be used as the src of an image
 */
function isSafeImageSource(src) {
  if (!src) {
    return false;
  }
  if (src.startsWith("data:")) {
    return src.startsWith("data:image/");
  }
  try {
    const url = new URL(src, document.baseURI);
    return url.protocol === "http:" || url.protocol === "https:";
  } catch {
    return false;
  }
}

/**
 * @param src - the source of an image
 * @returns the URL of the image when it is an attachment of the wiki, downloaded through the download action, which
 *   the server can resize, undefined otherwise
 */
function getAttachmentURL(src) {
  if (!src || src.startsWith("data:")) {
    return undefined;
  }
  try {
    const url = new URL(src, document.baseURI);
    return url.origin === window.location.origin &&
      url.pathname.includes("/download/")
      ? url
      : undefined;
  } catch {
    return undefined;
  }
}

/**
 * Return the URL to fetch the given image with the given width. The attachments of the wiki are resized by the server
 * when their URL has a width parameter, so that the cards don't download large images to display them small. Other
 * images are left untouched.
 *
 * @param src - the source of the image
 * @param width - the width, in pixels, at which the image is displayed
 * @returns the URL of the image resized to the given width (rounded up), or the given source when the image can't be
 *   resized by the server
 */
function getResizedImageSource(src, width) {
  const url = width > 0 ? getAttachmentURL(src) : undefined;
  if (!url) {
    return src;
  }
  url.searchParams.set(
    "width",
    String(Math.ceil(width / IMAGE_WIDTH_STEP) * IMAGE_WIDTH_STEP),
  );
  // The height is computed from the width, keeping the aspect ratio (it would be a small thumbnail size otherwise).
  url.searchParams.delete("height");
  const isAbsolute = /^([a-z][a-z\d+.-]*:)?\/\//i.test(src);
  return isAbsolute ? url.href : url.pathname + url.search + url.hash;
}

/**
 * Parse the given value as HTML, in an inert document.
 *
 * @param value - the value to parse
 * @returns the parsed document
 */
function parseHTML(value) {
  return new DOMParser().parseFromString(value, "text/html");
}

/**
 * Extract the image to display as a card cover from the value of a property. The value is either an HTML fragment
 * containing an image, in which case its first image is used, or the URL of an image.
 *
 * @param value - the value of the property
 * @returns the image (with its `src` and `alt`), or undefined when the value doesn't hold an image
 */
function getCoverImage(value) {
  if (typeof value !== "string") {
    return undefined;
  }
  const trimmedValue = value.trim();
  return trimmedValue.includes("<img")
    ? getImageFromHTML(trimmedValue)
    : getImageFromURL(trimmedValue);
}

/**
 * @param value - a value that may be the URL of an image
 * @returns the image, or undefined when the value is not the URL of an image
 */
function getImageFromURL(value) {
  const isImageURL =
    value.startsWith("data:image/") || IMAGE_URL_PATTERN.test(value);
  return isImageURL && isSafeImageSource(value)
    ? { src: value, alt: "" }
    : undefined;
}

/**
 * @param value - an HTML fragment
 * @returns its first image, or undefined when it doesn't contain an image that can be displayed
 */
function getImageFromHTML(value) {
  const image = parseHTML(value).querySelector("img[src]");
  const src = image?.getAttribute("src").trim();
  return isSafeImageSource(src)
    ? { src, alt: image.getAttribute("alt") ?? "" }
    : undefined;
}

/**
 * @param value - the value of a property
 * @returns the text of the value, without its markup
 */
function getText(value) {
  if (value === undefined || value === null) {
    return "";
  }
  const text = String(value);
  return (text.includes("<") ? parseHTML(text).body.textContent : text).trim();
}

/**
 * Return the property whose text is displayed enlarged, as the card cover, when the cover property doesn't hold an
 * image for the given entry (or when its image fails to load). We use the first property with some text among: the
 * cover property (unless it holds an image, whose text would only be its URL), the title property, then the first
 * visible property.
 *
 * @param logic - the Live Data logic
 * @param entry - the entry displayed by the card
 * @param coverPropertyId - the id of the property used as the card cover
 * @param titlePropertyId - the id of the property used as the card title, if any
 * @returns the id of the property to display as the card cover
 */
function getCoverTextPropertyId(
  logic,
  entry,
  coverPropertyId,
  titlePropertyId,
) {
  const propertyIds = logic.data.query.properties;
  const firstVisiblePropertyId = propertyIds.find(
    (propertyId) => logic.getPropertyDescriptor(propertyId)?.visible,
  );
  const coverTextPropertyId = getCoverImage(entry[coverPropertyId])
    ? undefined
    : coverPropertyId;
  return (
    [coverTextPropertyId, titlePropertyId, firstVisiblePropertyId].find(
      (propertyId) =>
        propertyId &&
        propertyIds.includes(propertyId) &&
        getText(entry[propertyId]) !== "",
    ) ?? coverPropertyId
  );
}

export {
  CARDS_LAYOUT_ID,
  getCoverImage,
  getCoverPropertyId,
  getCoverTextPropertyId,
  getResizedImageSource,
  setCoverPropertyId,
};
