require(["jquery", "$!services.webjars.url('jstree', 'jstree.js')"], function($) {
  // Create the tree and open all nodes when ready
  $('#debug_performance_tree').jstree().on('ready.jstree', function () {
    $('#debug_performance_tree').jstree('open_all');
  });
});
