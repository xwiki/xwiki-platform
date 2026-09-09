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

// The Live Data user interface is loaded with dynamic imports, which the page ready detection is not able to see: it
// watches the XMLHttpRequest and fetch calls, and the script elements added to the page head, while the modules are
// fetched by the module loader of the web browser. The entries are fetched asynchronously as well. We thus have to
// delay the page ready ourselves until the Live Data is displayed, otherwise the page can be marked as ready too
// early (e.g. the PDF export would print an empty Live Data).
// The RequireJS require function is read from the global object rather than used as a free variable, so that this
// module can be loaded outside of a RequireJS environment (e.g. by the unit tests).
globalThis.require(["jquery", "xwiki-page-ready"], ($, pageReady) => {
  $.fn.liveData = function(config) {
    return this.each(function() {
      const elementConfig = $(this).data("config");
      // An element that carries no configuration is a live data that is already displayed: displaying it consumes the
      // configuration. The editor puts such an element back in the page when it moves the content it edits into its
      // editable area, and displaying it a second time would replace the table by an empty element.
      if (!$(this).data("liveData") && (elementConfig || config)) {
        const instanceConfig = $.extend({}, elementConfig, config);
        const displayed = import("./services/init.js").then(({init}) => {
          $(this).attr("data-config", JSON.stringify(instanceConfig));
          return init(this, $);
        });
        // Mark the element as displayed synchronously, before the asynchronous initialization completes: this method
        // is called again for every xwiki:dom:updated event and displaying the same element twice destroys it.
        $(this).data("liveData", displayed);
        pageReady.delayPageReady(displayed, "livedata:display");
      }
    });
  };

  const init = function(event, data) {
    pageReady.delayPageReady(import("@xwiki/platform-livedata-ui").then(({populateStore}) => {
      populateStore()
      const container = $((data && data.elements) || document);
      container.find(".liveData").liveData();
    }), "livedata:load");
  };
  $(document).on("xwiki:dom:updated", init);
  $(init);
});
