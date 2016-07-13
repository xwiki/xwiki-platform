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
/*
 * This script is a guard for making sure that the xwiki:dom:loading and xwiki:dom:loaded events are correctly sent
 * after all the deferred scripts have executed. This is needed because sometimes the browser fires DOMContentLoaded
 * before deferred scripts have actually executed, against the HTML5 specification. However, all browsers do respect
 * the order in which the scripts are declared. This script should always be the last script declared in the HEAD,
 * so that it will be executed when all the other scripts have trully executed.
 */
XWiki.lastScriptLoaded = true;
if (XWiki.failedInit) {
  XWiki.initialize();
}
