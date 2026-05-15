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

import {guidedTourManager} from '@xwiki/platform-guidedtour-xwiki';
import {GuidedTourWidget} from "@xwiki/platform-guidedtour-ui";
import {createApp} from 'vue';


function init() {
  const app = createApp(GuidedTourWidget, { guidedTourManager });
  app.config.errorHandler = (err, instance, info) => {
    console.error('Vue error:', err, info, instance);
    throw err;
  };
  app.config.warnHandler = (msg, instance, trace) => {
    console.warn('Vue warn:', msg, trace, instance);
  };
  app.mount('#guidedtour-uix');
}

if (!document.querySelector('script[src*="/jsx/TourCode/TourJS"]')) {
  init();
} else {
  console.warn("Since the Tour Application is installed, not showing the GuidedTour Widget, to prevent conflicts.");
  init(); // TODO: Remove this, this is temporary for testing.
}