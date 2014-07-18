/**
 * Enhances the behaviour a log display.
 */
require([ 'jquery' ], function($) {
	$('.log-item').each(function(index, element) {
		var stacktrace = $(this).find('.stacktrace');
		if (stacktrace) {
			// Hide the stacktrace by default.
			stacktrace.toggle();
			// Show the stacktrace when the log message is clicked.
			var logMessage = $(this).find('div');
			logMessage.css({
				"cursor" : "pointer"
			});
			logMessage.click(function() {
				stacktrace.toggle();
			});
		}
	});
});
