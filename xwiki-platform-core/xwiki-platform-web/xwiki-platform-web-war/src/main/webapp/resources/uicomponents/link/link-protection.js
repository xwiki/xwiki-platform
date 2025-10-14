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
define('link-protection-translations', {
  prefix: '',
  keys: [
    'url.api.followLinkConfirmationText'
  ]
});
require(['jquery', 'xwiki-l10n!link-protection-translations', 'xwiki-events-bridge'], function ($, l10n) {

  function protectLinks () {
    let configuration = null;
    try {
      const trustedDomainConfigElement = $('script#trusted-domains-configuration');
      if (trustedDomainConfigElement.length > 0) {
        configuration = JSON.parse(trustedDomainConfigElement.text());
      }
    } catch (err) {
      console.error("Error while parsing the trusted domain configurations, falling back on enforcing checks on all" +
        " links going outside current domain.", err);
    }
    $(document).on('click', 'a[href]', function (event) {
      return askIfLinkNotTrusted(event, this, configuration);
    });
  }

  function askIfLinkNotTrusted (event, anchor, configuration) {
    let currentHostname = window.location.hostname;
    let anchorHostname = anchor.hostname;
    let customizedMessage = l10n.get('url.api.followLinkConfirmationText', currentHostname, anchorHostname);
    if (configuration == null && !isAnchorCurrentDomain(anchor)) {
      return confirm(customizedMessage);
    } else if (!isAnchorTrustedOomain(anchor, configuration.trustedDomains, configuration.allowedUrls)) {
      return confirm(customizedMessage);
    } else {
      return true;
    }
  }

  function isAnchorCurrentDomain (anchor) {
    let currentHostname = window.location.hostname;
    let anchorHostname = anchor.hostname;
    return (!anchorHostname || anchorHostname === currentHostname);
  }

  function isAnchorTrustedOomain (anchor, trustedDomains, allowedUrls) {
    if (isAnchorCurrentDomain(anchor)) {
      return true;
    } else {
      if (allowedUrls.indexOf(anchor.href) > -1) {
        return true;
      }
      let host = anchor.hostname;
      do {
        if (trustedDomains.indexOf(host) > -1) {
          return true;
        } else if (host.indexOf(".") > -1) {
          host = host.substring(host.indexOf(".") + 1);
        } else {
          host = "";
        }
      } while (host !== "");
    }
    return false;
  }

  (XWiki.domIsLoaded && protectLinks()) || document.observe('xwiki:dom:loaded', protectLinks);
});