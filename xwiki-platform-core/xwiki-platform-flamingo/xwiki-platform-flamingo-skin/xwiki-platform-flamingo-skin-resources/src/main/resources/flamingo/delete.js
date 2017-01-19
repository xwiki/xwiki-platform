require.config({
  paths: {
    jsTree: "$!services.webjars.url('jstree', 'jstree.min.js')",
    tree: "$!services.webjars.url('org.xwiki.platform:xwiki-platform-tree-webjar', 'tree.min.js')"
  },
  shim: {
    jsTree: {
      deps: ['jquery']
    }
  }
});
require(['jquery', 'xwiki-meta', 'tree'], function($, xm) {
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

    var handleQuestion = function() {
      $.ajax(new XWiki.Document(xm.documentReference).getURL('get', 'xpage=refactoring/delete_question&jobId='+jobId)).done(function (data) {
        $('#delete-progress-bar-container').after($('<div>').html(data));
        $('.deleteTree').xtree({plugins: ['checkbox'], core: {themes: {icons: true, dots: true}}});
        $('.btSelectAllTree').click(function(event){
          event.preventDefault();
           $('.deleteTree').jstree().check_all();
        });
        $('.btUnselectAllTree').click(function(event){
          event.preventDefault();
           $('.deleteTree').jstree().uncheck_all();
        });
        $('.btConfirmDelete').click(function(event){
          event.preventDefault();
          var selectedNodes = $('.deleteTree').jstree().get_selected(true);
          var selectedPages = [];
          for (var i = 0; i < selectedNodes.length; ++i) {
            if (selectedNodes[i].data.page) {
              selectedPages.push(selectedNodes[i].data.page);
            }
          }
          console.log(selectedPages);
          // TODO: send the answer
        });
      });
    }

    var getProgressStatus = function() {
      // TODO: add a factory for the REST URL
      var url = baseURL + '/rest/jobstatus/' + jobId;
      // Note: we use JSON because it is easier to parse with javascript
      $.ajax(url, {'data': {'media': 'json'}}).done(function (data) {
        updateProgressBar(data.progress.offset);
        if (data.state == 'WAITING') {
          handleQuestion();
          return;
        }
        if (data.progress.offset < 1) {
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
