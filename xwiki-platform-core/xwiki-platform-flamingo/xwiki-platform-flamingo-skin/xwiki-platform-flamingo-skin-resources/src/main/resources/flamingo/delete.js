require(['jquery', 'xwiki-meta'], function($, xm) {
  $(document).ready(function() {
    var progressBar = $('#delete-progress-bar');
    var jobId = progressBar.data('job-id');
    var baseURL = xm.restURL.substr(0, xm.restURL.indexOf('/rest/'));
    
    var updateProgressBar = function(progress) {
      progressBar.css('width', progress * 100 + '%');
      progressBar.data('progress', progress);
    };
    
    var whenTerminated = function() {
      updateProgressBar(1);
      $('#delete-progress-bar-container').hide();
      // TODO: add a factory for the REST URL
      var url = baseURL + '/rest/joblog/' + jobId;
      $.ajax(url, {'data': {'media': 'json', 'level': 'ERROR'}}).done(function (data) {
      // Note: we use JSON because it is easier to parse with javascript
        if (data.logEvents.length > 0) {
          var errorList = '<ul>';
          for (var i = 0; i < data.logEvents.length; ++i) {
            errorList += '<li>'+data.logEvents[i].formattedMessage+'</li>';
          }
          errorList += '</ul>';
          $('#errorMessage').append(errorList).removeClass('hidden');
        } else {
          $('#successMessage').removeClass('hidden');
        }
      });
    };
    
    var getProgressStatus = function() {
      // TODO: add a factory for the REST URL
      var url = baseURL + '/rest/jobstatus/' + jobId;
      // Note: we use JSON because it is easier to parse with javascript
      $.ajax(url, {'data': {'media': 'json'}}).done(function (data) {
        if (data.progress.offset < 1) {
          updateProgressBar(data.progress.offset);
          setTimeout(getProgressStatus, 1000);
        } else {
          whenTerminated();
        }
      });
    };
    
    // Init
    updateProgressBar(progressBar.data('progress'));
    getProgressStatus();
    
  });
});
