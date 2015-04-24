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
        var counter = 1;
        document.observe('xwiki:test', function(event) {
          document.stopObserving('xwiki:test');
          event.stop();
          counter += event.memo.delta;
        });
        $j(document).on('xwiki:test', function(event, data) {
          counter += data.delta;
        });
        document.fire('xwiki:test', {'delta': 3});
        $j(document).off('xwiki:test');
        expect(counter).toBe(4);
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
        var counter = 1;
        $j(document).one('xwiki:test', function(event, data) {
          event.stopPropagation();
          counter += data.delta;
        });
        document.observe('xwiki:test', function(event) {
          counter *= event.memo.delta;
        });
        $j(document).trigger($j.Event('xwiki:test'), [{'delta': 3}]);
        document.stopObserving('xwiki:test');
        expect(counter).toBe(4);
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
