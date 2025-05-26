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

require(["jquery"], ($) => {
  $.fn.liveData = function(config) {
    return this.each(function() {
      if (!$(this).data("liveData")) {
        const instanceConfig = $.extend($(this).data("config"), config);
        import("./services/init.js").then(
          (init) =>
            $(this).attr("data-config", JSON.stringify(instanceConfig)).data("liveData",
              init.init(this, $)));
      }
    });
  };

  const init = function(event, data) {
    import("@/components/populateStore.js").then(() => {
      const container = $((data && data.elements) || document);
      container.find(".liveData").liveData();
    });

  };
  $(document).on("xwiki:dom:updated", init);
  $(init);
});

export {
  componentStore, displayerMixin, BaseDisplayer, XWikiIcon,
};
