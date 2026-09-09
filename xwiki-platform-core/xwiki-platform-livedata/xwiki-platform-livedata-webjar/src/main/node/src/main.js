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

import { componentStore } from "@/components/store.js";

import displayerMixin from "./components/displayers/displayerMixin.js";
import BaseDisplayer from "./components/displayers/BaseDisplayer.vue";
import XWikiIcon from "./components/utilities/XWikiIcon.vue";

// The Live Data user interface is loaded with dynamic imports, which the page ready detection is not able to see: it
// watches the XMLHttpRequest and fetch calls, and the script elements added to the page head, while the modules are
// fetched by the module loader of the web browser. The entries are fetched asynchronously as well. We thus have to
// delay the page ready ourselves until the Live Data is displayed, otherwise the page can be marked as ready too
// early (e.g. the PDF export would print an empty Live Data).
require(["jquery", "xwiki-page-ready"], ($, pageReady) => {
  $.fn.liveData = function(config) {
    return this.each(function() {
      if (!$(this).data("liveData")) {
        const instanceConfig = $.extend($(this).data("config"), config);
        // The Live Data is displayed asynchronously: the layout and the displayers are loaded with dynamic imports
        // and the entries are fetched from the Live Data source. Listen for the event that marks the end of this
        // process, before initializing the Live Data, so that we know when it is fully displayed. The event is
        // triggered whatever the outcome, with the error that prevented the display, if any.
        const displayed = new Promise((resolve, reject) => {
          this.addEventListener("xwiki:livedata:instanceReady", ({detail}) => {
            if (detail.error) {
              reject(detail.error);
            } else {
              resolve(detail.livedata);
            }
          }, {once: true});
        });
        pageReady.delayPageReady(import("./services/init.js").then((init) => {
          $(this).attr("data-config", JSON.stringify(instanceConfig)).data("liveData", init.init(this, $));
          return displayed;
        }), "livedata:display");
      }
    });
  };

  const init = function(event, data) {
    pageReady.delayPageReady(import("@/components/populateStore.js").then(() => {
      const container = $((data && data.elements) || document);
      container.find(".liveData").liveData();
    }), "livedata:load");
  };
  $(document).on("xwiki:dom:updated", init);
  $(init);
});

export {
  componentStore, displayerMixin, BaseDisplayer, XWikiIcon,
};
