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
#set ($paths = {
  'js': {
    'vue': $services.webjars.url('org.webjars.npm:vue', 'dist/vue.runtime.esm-browser.prod'),
    'daterangepicker': $services.webjars.url('bootstrap-daterangepicker', 'js/bootstrap-daterangepicker.js')
  },
  'module': {
    'xwiki-livedata': $services.webjars.url('org.xwiki.platform:xwiki-platform-livedata-webjar', 'main.es.js')
  },
  'css': {
    'liveData': $services.webjars.url('org.xwiki.platform:xwiki-platform-livedata-webjar',
      'xwiki-platform-livedata.css'),
    'liveDataLessVariables': $services.webjars.url('org.xwiki.platform:xwiki-platform-livedata-webjar',
      'variables.less', {'evaluate': true}),
    'liveDataLessReactive': $services.webjars.url('org.xwiki.platform:xwiki-platform-livedata-webjar',
      'reactive.less', {'evaluate': true}),
    'dateRangePicker': $services.webjars.url('bootstrap-daterangepicker', 'css/bootstrap-daterangepicker.css'),
    'selectize': [
      $services.webjars.url('selectize.js', 'css/selectize.bootstrap3.css'),
      $xwiki.getSkinFile('uicomponents/suggest/xwiki.selectize.css', true)
    ]
  },
  'liveDataBasePath': $stringtool.removeEnd($liveDataPath, $liveDataEntry),
  'contextPath': $request.contextPath
})
#[[*/
// Start JavaScript-only code.
(function(paths) {
  "use strict";

  require.config({
    paths: paths.js,
    map: {
      "*": {
        "xwiki-livedata": "xwiki-livedata-with-css",
        daterangepicker: "daterangepicker-with-css",
        "xwiki-selectize": "xwiki-selectize-with-css",
      },
      "xwiki-livedata-with-css": {
        "xwiki-livedata": "xwiki-livedata",
      },
      "daterangepicker-with-css": {
        daterangepicker: "daterangepicker",
      },
      "xwiki-selectize-with-css": {
        "xwiki-selectize": "xwiki-selectize",
      },
    },
    config: {
      "xwiki-livedata-source": {
        contextPath: paths.contextPath,
      },
    },
  });

  function loadCSS(url) {
    const link = document.createElement("link");
    link.type = "text/css";
    link.rel = "stylesheet";
    link.href = url;
    document.getElementsByTagName("head")[0].appendChild(link);
  }

  function loadModule(url) {
    const script = document.createElement("script");
    script.type = "module";
    script.src = url;
    document.getElementsByTagName("head")[0].appendChild(script);
  }

  define("loadCSS", function() {
    return (url) => {
      const urls = Array.isArray(url) ? url : [url];
      urls.forEach(loadCSS);
    };
  });

  define("daterangepicker-with-css", ["loadCSS", "daterangepicker"], function(loadCSS) {
    // Load the CSS for the date range picker.
    loadCSS(paths.css.dateRangePicker);
    return arguments[1];
  });

  define("xwiki-selectize-with-css", ["loadCSS", "xwiki-selectize"], function(loadCSS) {
    // Load the CSS for the suggest picker.
    loadCSS(paths.css.selectize);
    return arguments[1];
  });

  loadModule(paths.module["xwiki-livedata"]);
  loadCSS(paths.css.liveData);
  // Load a small less file with the declarations of a few LESS values that are not exported
  // elsewhere
  loadCSS(paths.css.liveDataLessVariables);
  loadCSS(paths.css.liveDataLessReactive);

// End JavaScript-only code.
}).apply("]]#", $jsontool.serialize([$paths]));
