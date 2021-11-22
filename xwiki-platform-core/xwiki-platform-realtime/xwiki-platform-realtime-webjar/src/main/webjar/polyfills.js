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
requirejs.config({
  config: {
    'netflux-client': {
      // Netflux Client is using ES6 Promise but we still have to support IE11 so we need the polyfill.
      polyfills: ['es6-promise']
    }
  },
  map: {
    '*': {
      // Replace es6-promise with es6-promise-auto in order to polyfill Promise automatically.
      'es6-promise': 'es6-promise-auto',

      // Make sure we load the required polyfills before Netflux Client.
      'netflux-client': 'xwiki-withPolyfills!netflux-client'
    },

    'es6-promise-auto': {'es6-promise': 'es6-promise'},
    'netflux-client-withoutPolyfills': {'netflux-client': 'netflux-client'}
  }
});

// Auto polyfill the ES6 Promise.
define('es6-promise-auto', ['es6-promise'], function(es6Promise) {
  es6Promise.polyfill();
  return Promise;
});

define('netflux-client-withoutPolyfills', ['netflux-client'], function(netfluxClient) {
  return netfluxClient;
});

// Helper RequireJS plugin that loads the polyfills required by a module before the module itself (because modules
// should not declare dependencies on polyfills).
define('xwiki-withPolyfills', {
  load: function(name, req, onLoad, config) {
    req(config.config[name].polyfills, function() {
      req([name + '-withoutPolyfills'], function(value) {
        onLoad(value);
      });
    });
  }
});

