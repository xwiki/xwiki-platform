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
// NOTICE: We don't need this file anymore because this RequireJS configuration is now published from javascript.vm
/*!
#set ($minified = '.min')
#if (!$services.debug.minify)
  #set ($minified = '')
#end
#set ($paths = {
  'jsTree': $services.webjars.url('jstree', "jstree${minified}"),
  'xwiki-tree-finder': $services.webjars.url('org.xwiki.platform:xwiki-platform-tree-webjar', "finder${minified}"),
  'tree': $services.webjars.url('org.xwiki.platform:xwiki-platform-tree-webjar', "tree${minified}")
})
#[[*/
// Start JavaScript-only code.
(function(paths) {
  "use strict";

  require.config({
    paths
  });

// End JavaScript-only code.
}).apply(']]#', $jsontool.serialize([$paths]));
