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
    moment: "$services.webjars.url('momentjs', 'min/moment.min')",
    'moment-jdateformatparser': $jsontool.serialize($services.webjars.url('org.webjars.bower:moment-jdateformatparser',
      'moment-jdateformatparser')),
    'bootstrap-datetimepicker': $jsontool.serialize($services.webjars.url('Eonasdan-bootstrap-datetimepicker',
      'js/bootstrap-datetimepicker.min'))
  },
  shim: {
    // This has been fixed in the latest version of moment-jdateformatparser.
    'moment-jdateformatparser': ['moment']
  }
});

// Workaround for bug in moment-jdateformatparser (remove after upgrading to the latest version).
var module = module || false;

// Hack for a limitation in moment-jdateformatparser that should be fixed in the latest version.
require(['moment'], function(moment) {
  window.moment = moment;
  require([
    'jquery',
    'bootstrap',
    'bootstrap-datetimepicker',
    'moment-jdateformatparser',
    'xwiki-events-bridge'
  ], function($) {
    var init = function(event, data) {
      var container = $((data && data.elements) || document);
      container.find('input.datetime').each(function() {
        $(this).datetimepicker({
          locale: $(this).data('locale'),
          format: moment().toMomentFormatString($(this).data('format'))
        });
      });
    };

    $(document).on('xwiki:dom:updated', init);
    return XWiki.domIsLoaded && init();
  });
});
