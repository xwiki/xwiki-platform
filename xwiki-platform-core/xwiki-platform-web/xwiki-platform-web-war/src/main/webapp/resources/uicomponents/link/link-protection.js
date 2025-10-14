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
require(['jquery', 'xwiki-events-bridge'], function ($) {

  let protectLinks = function (elements) {
    let trustedDomainConfigElement = $('script#trusted-domains-configuration');
    if (trustedDomainConfigElement.length > 0) {
      let configuration = JSON.parse(trustedDomainConfigElement.text());
      $(document).on('click', 'a[href]', function (event) {
        return askIfLinkNotTrusted(event, this, configuration);
      });
    }
  };

function askIfLinkNotTrusted (event, anchor, configuration) {
    if (!isAnchorTrustedOomain(anchor, configuration.trustedDomains, configuration.allowedUrls)) {
      let currentHostname = window.location.hostname;
      let anchorHostname = anchor.hostname;
      let customizedMessage = configuration.confirmationText
        .replace('__CURRENT_DOMAIN__', currentHostname)
        .replace('__EXTERNAL_DOMAIN__', anchorHostname);
      return confirm(customizedMessage);
    } else {
      return true;
    }
  };

  let isAnchorTrustedOomain = function (anchor, trustedDomains, allowedUrls) {
    let currentHostname = window.location.hostname;
    let anchorHostname = anchor.hostname;
    if (currentHostname === anchorHostname) {
      return true;
    } else {
      for (let i = 0; i < allowedUrls.length; i++) {
        if (anchor.href.startsWith(allowedUrls[i])) {
          return true;
        }
      }
      for (let i = 0; i < trustedDomains.length; i++) {
        if (anchorHostname.indexOf(trustedDomains[i]) !== -1) {
          return true;
        }
      }
    }
    return false;
  };

  $(document).on('xwiki:dom:updated', function(event, data) {
    protectLinks($(data.elements));
  });
  protectLinks($('body'));
});