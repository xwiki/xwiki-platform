require(['jquery', 'xwiki-meta'], function($, xm) {
  $(document).ready(function() {
    var progressBar = $('#delete-progress-bar');
    var jobId = progressBar.data('job-id');
    
    var updateProgressBar = function(progress) {
      progressBar.css('width', progress * 100 + '%');
      progressBar.data('progress', progress);
    };
    
    updateProgressBar(progressBar.data('progress'));
    
    var getProgressStatus = function() {
      // TODO: add a factory for the REST URL
      var url = '/rest/jobstatus/' + jobId;
      $.ajax(url).done(function (data) {
        if (data.progress.offset < 1) {
          updateProgressBar(data.progress.offset);
          setTimeout(getProgressStatus, 1000);
        } else {
          updateProgressBar(1);
          $('#delete-progress-bar-container').remove();
          $('#successMessage').removeClass('hidden');
        }
      });
    };
    
    getProgressStatus();
    
  });
});
