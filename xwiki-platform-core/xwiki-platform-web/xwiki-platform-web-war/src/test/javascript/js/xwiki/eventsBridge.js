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
require.config({
  paths: {
    'xwiki-events-bridge': 'js/xwiki/eventsBridge.min'
  },
  shim: {
    'xwiki-events-bridge': {
      // The dependency on Prototype.js is not declared.
      deps: ['jquery', 'prototype']
    }
  }
});

define(['jquery', 'prototype', 'xwiki-events-bridge'], function($j, $p) {
  describe('Events Bridge', function() {
    it('Dependencies', function() {
      expect(typeof $j).toBe('function');
      expect(typeof $p).toBe('function');
      expect($).toBe($p);
    });

    describe('Prototype -> jQuery', function() {

      it('document.fire', function() {
        var counter = 1;
        document.observe('xwiki:test', function(event) {
          document.stopObserving('xwiki:test');
          counter *= event.memo.delta;
        });
        $j(document).one('xwiki:test', function(event, data) {
          counter += data.delta;
        });
        document.fire('xwiki:test', {'delta': 3});
        expect(counter).toBe(6);
      });

      it('$(element).fire', function() {
        var counter = 7;
        $p(document.body).observe('xwiki:test', function(event) {
          $p(document.body).stopObserving('xwiki:test');
          counter -= event.memo;
        });
        $j(document.body).one('xwiki:test', function(event, data) {
          counter *= data;
        });
        $p(document.body).fire('xwiki:test', 2);
        expect(counter).toBe(10);
      });

      it('event.stop', function() {
        var names = [];

        var registerPrototypeEventListener = function(node, name) {
          $p(node).observe('xwiki:test', function(event) {
            $p(node).stopObserving('xwiki:test');
            event.stop();
            names.push(name);
          });
        };

        var registerJQueryEventListener = function(node, name) {
          $j(node).one('xwiki:test', function(event, data) {
            names.push(name);
          });
        };

        registerPrototypeEventListener(document, 'pDoc');
        registerPrototypeEventListener(document.body, 'pBody');

        registerJQueryEventListener(document, 'jDoc');
        registerJQueryEventListener(document.body, 'jBody');

        document.body.fire('xwiki:test');

        // Clean up.
        $p(document).stopObserving('xwiki:test');
        $p(document.body).stopObserving('xwiki:test');
        $j(document).off('xwiki:test');
        $j(document.body).off('xwiki:test');

        // Stopping an event prevents it from propagating in the DOM but it shouldn't prevent the other event listeners
        // that were registered directly on the target node to be called.
        expect(names).toEqual(['pBody', 'jBody']);
      });

      it('event.preventDefault', function() {
        $j(document).one('xwiki:test', function(event) {
          event.preventDefault();
        });
        var event  = document.fire('xwiki:test');
        expect(event.stopped).toBe(true);
      });

      it('filter xwiki:* events', function() {
        var counter = 7;
        $p(document.body).observe('wiki:test', function(event) {
          $p(document.body).stopObserving('wiki:test');
          counter -= event.memo;
        });
        $j(document.body).one('wiki:test', function(event, data) {
          counter *= data;
        });
        $p(document.body).fire('wiki:test', 2);
        expect(counter).toBe(5);
      });

    });

    describe('jQuery -> Prototype', function() {

      it('$(document).trigger', function() {
        var counter = -6;
        $j(document).one('xwiki:test', function(event, data) {
          counter += data;
        });
        document.observe('xwiki:test', function(event) {
          document.stopObserving('xwiki:test');
          counter *= event.memo;
        });
        $j(document).trigger('xwiki:test', 4);
        expect(counter).toBe(-8);
      });

      it('$(element).trigger', function() {
        var counter = 6;
        $j(document.body).one('xwiki:test', function(event, data) {
          counter += data.delta;
        });
        $p(document.body).observe('xwiki:test', function(event) {
          $p(document.body).stopObserving('xwiki:test');
          counter /= event.memo.delta;
        });
        $j(document.body).trigger('xwiki:test', {'delta': 2});
        expect(counter).toBe(4);
      });

      it('event.stopPropagation', function() {
        var names = [];

        var registerPrototypeEventListener = function(node, name) {
          $p(node).observe('xwiki:test', function(event) {
            $p(node).stopObserving('xwiki:test');
            names.push(name);
          });
        };

        var registerJQueryEventListener = function(node, name) {
          $j(node).one('xwiki:test', function(event, data) {
            event.stopPropagation();
            names.push(name);
          });
        };

        registerPrototypeEventListener(document, 'pDoc');
        registerPrototypeEventListener(document.body, 'pBody');

        registerJQueryEventListener(document, 'jDoc');
        registerJQueryEventListener(document.body, 'jBody');
      
        $j(document.body).trigger($j.Event('xwiki:test'));

        // Clean up.
        $p(document).stopObserving('xwiki:test');
        $p(document.body).stopObserving('xwiki:test');
        $j(document).off('xwiki:test');
        $j(document.body).off('xwiki:test');

        // stopPropagation should still allow the event listeners registered directly on the target node to be called.
        expect(names).toEqual(['jBody', 'pBody']);
      });
      
      it('event.stopImmediatePropagation', function() {
        var names = [];

        var registerPrototypeEventListener = function(node, name) {
          $p(node).observe('xwiki:test', function(event) {
            $p(node).stopObserving('xwiki:test');
            names.push(name);
          });
        };

        var registerJQueryEventListener = function(node, name) {
          $j(node).one('xwiki:test', function(event, data) {
            event.stopImmediatePropagation();
            names.push(name);
          });
        };

        registerPrototypeEventListener(document, 'pDoc');
        registerPrototypeEventListener(document.body, 'pBody');

        registerJQueryEventListener(document, 'jDoc');
        registerJQueryEventListener(document.body, 'jBody');
      
        $j(document.body).trigger($j.Event('xwiki:test'));

        // Clean up.
        $p(document).stopObserving('xwiki:test');
        $p(document.body).stopObserving('xwiki:test');
        $j(document).off('xwiki:test');
        $j(document.body).off('xwiki:test');

        // stopImmediatePropagation prevents the remaining event listeners to be called.
        expect(names).toEqual(['jBody']);
      });

      it('event.stop', function() {
        document.observe('xwiki:test', function(event) {
          document.stopObserving('xwiki:test');
          event.stop();
        });
        var event = $j.Event('xwiki:test');
        $j(document).trigger(event);
        expect(event.isDefaultPrevented()).toBe(true);
      });

      it('filter xwiki:* events', function() {
        var counter = 7;
        $j(document.body).one('wiki:test', function(event, data) {
          counter -= data;
        });
        $p(document.body).observe('wiki:test', function(event) {
          $p(document.body).stopObserving('wiki:test');
          counter *= event.memo;
        });
        $j(document.body).trigger('wiki:test', 2);
        expect(counter).toBe(5);
      });

    });
  });
});
