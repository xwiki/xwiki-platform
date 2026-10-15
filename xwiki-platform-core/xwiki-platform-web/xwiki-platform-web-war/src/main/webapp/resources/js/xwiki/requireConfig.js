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
## The global RequireJS configuration. It depends only on the wiki, the skin, the platform version and the installed
## extensions, so this file is served through the skin action (which evaluates the Velocity code below and sets a long
## expiration date) instead of being inlined in every page. The page and user specific values stay inline, in
## flamingo/javascript.vm, which is also where this file is pulled from.
##
## Note that the 'minify' request parameter is forwarded by javascript.vm, because this file is fetched by a separate
## request which doesn't carry the parameters of the page request.
#set ($jsExtension = '.min.js')
#if (!$services.debug.minify)
  #set ($jsExtension = '.js')
#end
## Requirejs will automatically add a ".js" suffix if the generated URL doesn't contain a "?". It happens that we
## don't generate a URL with "?" when we export in HTML for example. In this case we remove the ".js" suffix since
## requirejs will add one...
## Note that we cannot do this generically in the webjars module when exporting in HTML because the webjars module
## provide generic URLs and they don't all go through requirejs...
#macro (removeJsSuffix $expr)
## Note that velocity takes argument references by name (see: https://velocity.apache.org/engine/releases/velocity-1.5/user-guide.html#velocimacros).
## So we set the value of the $expr in the $url variable to not execute $expr multiple times.
#set ($url = $expr)
#if (!$url.contains('?'))$stringtool.removeEnd($url, '.js')#else$url#{end}
#end
##
## Start the requirejs config.
## See https://requirejs.org/docs/api.html#config
## Note that we have to declare momentjs as a RequireJS package in order to be able to load momentjs locales on demand
## using RequireJS. See https://github.com/requirejs/requirejs/issues/1554 .
##
#set ($jqueryMigrateId = 'org.webjars.npm:jquery-migrate')
## The WebJar version doesn't match the version of the packaged jquery-ui-touch-punch library so we are forced to
## specify the library version when computing the WebJar resource URL.
#set ($jqueryUITouchPunchPath = "jquery-ui-touch-punch/0.2.3/jquery.ui.touch-punch${jsExtension}")
#set ($requireConfig = {
  'packages': [
    {
      'name': 'scriptaculous',
      'location': $stringtool.removeEnd($services.webjars.url('scriptaculous', ''), '/'),
      'main': 'scriptaculous'
    }, {
      'name': 'moment',
      'location': $stringtool.removeEnd($services.webjars.url('org.webjars.npm:moment', ''), '/'),
      'main': 'min/moment.min'
    }
  ],
  'paths': {
    'bootstrap': "#removeJsSuffix($services.webjars.url('org.xwiki.platform:xwiki-platform-bootstrap', ""js/xwiki-bootstrap${jsExtension}""))",
    'css': $xwiki.getSkinFile('uicomponents/require/css.js'),
    'deferred': $xwiki.getSkinFile('uicomponents/require/deferred.js'),
    'node-module': $xwiki.getSkinFile('uicomponents/require/node-module.js'),
    'es-module': $xwiki.getSkinFile('uicomponents/require/es-module.js'),
    'iscroll': "#removeJsSuffix($services.webjars.url('org.webjars.npm:iscroll', 'build/iscroll-lite.js'))",
    'jquery': "#removeJsSuffix($services.webjars.url('jquery', ""jquery${jsExtension}""))",
    'jquery-migrate': "#removeJsSuffix($services.webjars.url($jqueryMigrateId, ""dist/jquery-migrate${jsExtension}""))",
    'jquery-ui': "#removeJsSuffix($services.webjars.url('jquery-ui', ""jquery-ui${jsExtension}""))",
    'jquery-ui-touch-punch': "#removeJsSuffix($services.webjars.url($jqueryUITouchPunchPath))",
    'jsTree': "#removeJsSuffix($services.webjars.url('jstree', ""jstree${jsExtension}""))",
    'moment-jdateformatparser': $services.webjars.url('org.webjars.npm:moment-jdateformatparser', 'moment-jdateformatparser.min'),
    'moment-timezone': $services.webjars.url('org.webjars.npm:moment-timezone', 'builds/moment-timezone-with-data.min'),
    'prototype': "#removeJsSuffix($services.webjars.url('prototype', 'prototype.js'))",
    'tom-select': "#removeJsSuffix($services.webjars.url('org.webjars.npm:tom-select', ""dist/js/tom-select.complete${jsExtension}""))",
    'xwiki-attachments-icon': $xwiki.getSkinFile('uicomponents/attachments/icons.js', true),
    'xwiki-autosave': "#removeJsSuffix($services.webjars.url('org.xwiki.platform:xwiki-platform-web-webjar', ""autosave${jsExtension}""))",
    'xwiki-document': "#removeJsSuffix($services.webjars.url('org.xwiki.platform:xwiki-platform-web-webjar', ""document${jsExtension}""))",
    'xwiki-document-lock': $xwiki.getSkinFile('uicomponents/lock/lock.js'),
    'xwiki-events-bridge': $xwiki.getSkinFile('js/xwiki/eventsBridge.js'),
    'xwiki-form-validation-async': $xwiki.getSkinFile('uicomponents/tools/formAsyncValidation.js'),
    'xwiki-icon': "#removeJsSuffix($services.webjars.url('org.xwiki.platform:xwiki-platform-icon-webjar', ""icon${jsExtension}""))",
    'xwiki-job-runner': "#removeJsSuffix($services.webjars.url('org.xwiki.platform:xwiki-platform-job-webjar', ""jobRunner${jsExtension}""))",
    'xwiki-locale-picker': $xwiki.getSkinFile('localePicker.js', true),
    'xwiki-meta': $xwiki.getSkinFile('js/xwiki/meta.js'),
    'xwiki-selectize': $xwiki.getSkinFile('uicomponents/suggest/xwiki.selectize.js'),
    'xwiki-suggestAttachments-bundle': $xwiki.getSkinFile('uicomponents/suggest/suggestAttachments.js'),
    'xwiki-tree-finder': "#removeJsSuffix($services.webjars.url('org.xwiki.platform:xwiki-platform-tree-webjar', ""finder${jsExtension}""))",
    'xwiki-tree': "#removeJsSuffix($services.webjars.url('org.xwiki.platform:xwiki-platform-tree-webjar', ""tree${jsExtension}""))",
    'xwiki-edit-confirmation': $xwiki.getSkinFile('uicomponents/edit/editConfirmation.js')
  },
  'shim': {
    'bootstrap': ['jquery'],
    'jquery-ui-touch-punch': ['jquery-ui'],
    'prototype': {
      'exports': '$'
    },
    'scriptaculous/dragdrop': ['scriptaculous/effects'],
    'xwiki-document-lock': {
      'exports': 'XWiki.DocumentLock'
    }
  },
  'bundles': {
    'xwiki-suggestAttachments-bundle': [
      'xwiki-attachments-store',
      'xwiki-attachments-filter',
      'xwiki-file-picker',
      'xwiki-suggestAttachments-messages',
      'xwiki-suggestAttachments',
      'xwiki-attachmentResourcePicker'
    ]
  },
  'config': {
    'xwiki-selectize': {
      'css': [
        $services.webjars.url('org.webjars.npm:tom-select', 'dist/css/tom-select.bootstrap4.min.css'),
        $xwiki.getSkinFile('uicomponents/suggest/xwiki.selectize.css')
      ]
    }
  }
})
#set ($jQueryModuleId = 'jQueryNoConflict')
## The minified version of jquery-migrate expects a global 'jQuery' variable to be defined (even though it also uses
## RequireJS to access jQuery). The unminified version is only relying on RequireJS so it doesn't have this problem.
#set ($declareJQueryGlobal = false)
## Check if the jQuery Migrate module is provided as a core extension or is installed.
#if ($services.extension.core.getCoreExtension($jqueryMigrateId) ||
    $services.extension.installed.getInstalledExtension($jqueryMigrateId, "wiki:$xcontext.database"))
  ## Load jQuery through jQuery Migrate in order to have the backwards compatibility layer.
  #set ($jQueryModuleId = 'jquery-migrate')
  #set ($declareJQueryGlobal = true)
#end
## momentjs locales depend on '../moment' which gets resolved as 'moment/moment' due to our package configuration, which
## points to the unminified version. The consequence is that we end up loading both the minified and the unminified
## version of momentjs and, more importantly, the locales are loaded into the moment instance created by the unminified
## code. In order to fix this we map the unminified version to the minified version so that we work with a single moment
## instance (that has the locales loaded).
## See http://requirejs.org/docs/jquery.html#noconflictmap to understand why this works.
#set ($requireConfig.map = {
  '*': {
    'jquery': $jQueryModuleId,
    'jquery-ui': 'jquery-ui-touch-punch',
    'moment/moment': 'moment'
  },
  'jQueryNoConflict': {
    'jquery': 'jquery'
  },
  'jquery-migrate': {
    'jquery': 'jQueryNoConflict'
  },
  'jquery-ui-touch-punch': {
    'jquery-ui': 'jquery-ui'
  }
})
## Extend the RequireJS configuration.
#foreach ($uix in $services.uix.getExtensions('org.xwiki.platform.requirejs.module'))
  #set ($module = $uix.parameters)
  #if ("$!module.id" != '')
    #if ("$!module.path" != '')
      #set ($discard = $requireConfig.paths.put($module.id, $module.path))
    #end
    #if ("$!module.bundles" != '')
      #set ($discard = $requireConfig.bundles.put($module.id, $module.bundles.split('\s*,\s*')))
    #end
    #if ("$!module.deps" != '' || "$!module.exports" != '')
      #set ($shim = $requireConfig.shim.getOrDefault($module.id, {}))
      ## The shim value can be the list of module dependencies. We need to normalize the shim in this case.
      #if (!$shim.entrySet())
        #set ($shim = {'deps': $shim})
      #end
      #if ("$!module.deps" != '')
        #set ($shim.deps = $module.deps.split('\s*,\s*'))
      #end
      #if ("$!module.exports" != '')
        #set ($shim.exports = $module.exports)
      #end
      #set ($discard = $requireConfig.shim.put($module.id, $shim))
    #end
    #if ("$!module.config" != '')
      #set ($discard = $requireConfig.config.put($module.id, $jsontool.fromString($module.config)))
    #end
  #end
#end
#[[*/
// Start JavaScript-only code.
(function (requireConfig, declareJQueryGlobal) {
  'use strict';

  if (declareJQueryGlobal) {
    // The minified version of jquery-migrate is expecting a global 'jQuery' variable to be defined (even though it also
    // uses RequireJS to access jQuery).
    window.jQuery = {};
  }

  require.config(requireConfig);

  define('jQueryNoConflict', ['jquery'], function($) {
    return $.noConflict();
  });

  // Add support for loading ECMAScript modules with RequireJS.
  const originalCreateNode = require.createNode;
  require.createNode = function(config, moduleName, stringURL) {
    const scriptElement = originalCreateNode.apply(this, arguments);
    const url = new URL(stringURL, window.location.href);
    if (url.pathname.endsWith('.es.js')) {
      scriptElement.type = 'module';
    }
    return scriptElement;
  };

  if (window.Prototype && Prototype.BrowserFeatures.ElementExtensions) {
    require(['jquery', 'bootstrap'], function($) {
      // Fix incompatibilities between BootStrap and Prototype
      const disablePrototypeJS = function(method, pluginsToDisable) {
        const handler = function(event) {
          event.target[method] = undefined;
          setTimeout(function() {
            delete event.target[method];
          }, 0);
        };
        pluginsToDisable.each(function(plugin) {
          $(window).on(method + '.bs.' + plugin, handler);
        });
      };
      const pluginsToDisable = ['collapse', 'dropdown', 'modal', 'tooltip', 'tab', 'popover'];
      disablePrototypeJS('show', pluginsToDisable);
      disablePrototypeJS('hide', pluginsToDisable);
    });
  }
// End JavaScript-only code.
}).apply(']]#', $jsontool.serialize([$requireConfig, $declareJQueryGlobal]));
