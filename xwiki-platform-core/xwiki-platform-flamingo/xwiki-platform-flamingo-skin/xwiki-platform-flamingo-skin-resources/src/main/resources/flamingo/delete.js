require.config({
  paths: {
    jsTree: "$!services.webjars.url('jstree', 'jstree.min.js')",
    JobRunner: "$!services.webjars.url('org.xwiki.platform:xwiki-platform-job-webjar', 'jobRunner.min.js')",
    'tree-finder': "$!services.webjars.url('org.xwiki.platform:xwiki-platform-tree-webjar', 'finder.min.js')",
    tree: "$!services.webjars.url('org.xwiki.platform:xwiki-platform-tree-webjar', 'tree.min.js')"
  },
  shim: {
    jsTree: {
      deps: ['jquery']
    }
  }
});
require(['jquery', 'xwiki-meta', 'tree'], function($, xm) {
  // prevent the user from leaving the delete
  $(window).on("beforeunload", function(e) {
    e.preventDefault();
    e.returnValue = "";
  });

  $(document).ready(function() {
    var progressBar = $('#delete-progress-bar');
    var jobId = progressBar.data('job-id');
    var baseURL = xm.restURL.substr(0, xm.restURL.indexOf('/rest/'));

    /**
     * Called to update the display of the progress bar
     */
    var updateProgressBar = function(progress) {
      progressBar.css('width', progress * 100 + '%');
      progressBar.data('progress', progress);
    };

    /**
     * Called when the job is terminated
     */
    var whenTerminated = function() {
      updateProgressBar(1);
      $('#delete-progress-bar-container').hide();

      // Now get the logs and display them
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

    /**
     * Called when a question is being asked because some page should not be deleted
     */
    var handleQuestion = function() {
      var jobUrl =
        new XWiki.Document(xm.documentReference).getURL('get', 'xpage=refactoring/delete_question&jobId='+jobId);
      $.ajax(jobUrl).done(function (data) {
        /**
         * Represent the selected pages & extensions the user can chose to delete
         */
        var selection = {
          // Selected extensions to be removed (all contained pages will be removed, even those the user haven't seen
          // because of the pagination)
          extensions: [],
          // Pages that have been manually marked by the user to be removed
          pages: [],
          // Either or not all pages that don't belong to any extension should be removed (even those the user haven't
          // seen because of the pagination)
          allFreePages: false,
          // Either or not all extensions should be removed (even those the user haven't seen because of the pagination)
          allExtensions: false
        };

        // Display the data we got from the ajax request in a div after the progress bar
        var questionPanel = $('<div>').html(data);
        $('#delete-progress-bar-container').after(questionPanel);

        // Enable the tree that will display the pages to delete
        $('.deleteTree').xtree({plugins: ['checkbox'], core: {themes: {icons: true, dots: true}}});

        // Called when a node has been clicked on the tree
        $('.deleteTree').on('changed.jstree', function (event) {
          // It's the only safe way to prevent unwanted deletion of extension
          selection.allExtensions = false;
        });

        // Called when the user click on "select all"
        $('.btSelectAllTree').click(function(event){
          event.preventDefault();
          // The following comment tells jshint not to worry about the camel case, since we cannot control
          // the method name of jstree (must be a bug from jshint).
          /*jshint camelcase:false */
          $('.deleteTree').jstree().check_all();
          /*jshint camelcase:true */
          selection.allExtensions = true;
        });

        // Called when the user click on "select none"
        $('.btUnselectAllTree').click(function(event){
          event.preventDefault();
          /*jshint camelcase:false */
          $('.deleteTree').jstree().uncheck_all();
          /*jshint camelcase:true */
        });

        // Called when the user click on "delete"
        $('.btConfirmDelete').click(function(event){
          event.preventDefault();

          // we don't ask again the user to confirm he's leaving the page
          $(window).off("beforeunload");

          /*jshint camelcase:false */
	        // Get the selection
          var selectedNodes = $('.deleteTree').jstree().get_selected(true);
          /*jshint camelcase:true */
          selection.allFreePages = false;
          for (var i = 0; i < selectedNodes.length; ++i) {
            var node = selectedNodes[i];
            if (node.data.type == 'extension') {
              selection.extensions.push(node.id);
            } else if (node.data.type == 'page') {
              selection.pages.push(node.id);
            } else if (node.id == 'freePages') {
              // For free pages, we can rely on the state of the "freePage" node
              selection.allFreePages = true;
            }
          }
          // Send the selection to the server
          $.ajax(jobUrl, {
            method: 'POST',
            data: {
              selection: JSON.stringify(selection)
            }
          }).done(function(){
            getProgressStatus();
            questionPanel.remove();
          });
        });

        // Called when the user click on "cancel"
        $('.btCancelDelete').click( function(event) {
          event.preventDefault();

          // we don't ask again the user to confirm he's leaving the page
          $(window).off("beforeunload");
          $('.btConfirmDelete').prop('disabled', 'disabled');
          var notif = new XWiki.widgets.Notification(
            "$escapetool.javascript($services.localization.render('core.delete.warningExtensions.canceling'))",
            'inprogress'
          );
          $.ajax(new XWiki.Document(xm.documentReference).getURL('get'), {
            data: {
              xpage: 'refactoring/delete_question',
              jobId: jobId,
              cancel: true
            }
          }).done(function () {
            notif.hide();
            new XWiki.widgets.Notification(
              "$escapetool.javascript($services.localization.render('core.delete.warningExtensions.canceled'))",
              'done'
            );
            // Redirect to the page with the "view" mode
            window.location = new XWiki.Document(xm.documentReference).getURL();
          });
        });

        // Handle the timeout
        // On the server, the timeout is 5 minutes.
        // We remove 10 seconds to be safe because the connexion between the server and the browsers
        // might be slow
        var timeout = (5 * 60 * 1000) - (10 * 1000);
        setTimeout(function() {
          // Remove everything and replace it by an error message
          $('#delete-progress-bar-container').remove();
          questionPanel.html('<div class="box errormessage"><p>' +
            '$escapetool.javascript($services.localization.render("core.delete.warningExtensions.timeout"))' +
            '</p></div>'
          );
        }, timeout);
      });
    };

    /**
     * Get the current status of the job (it is called recursively until the job is terminated or a question is asked)
     */
    var getProgressStatus = function() {
      // TODO: add a factory for the REST URL
      var url = baseURL + '/rest/jobstatus/' + jobId;
      // Note: we use JSON because it is easier to parse with javascript
      $.ajax(url, {'data': {'media': 'json'}}).done(function (data) {
        updateProgressBar(data.progress.offset);
        if (data.state === 'WAITING') {
          handleQuestion();
        } else if (data.state === 'FINISHED') {
          whenTerminated();
        } else {
          setTimeout(getProgressStatus, 1000);
        }
      });
    };

    // Init
    updateProgressBar(progressBar.data('progress'));
    getProgressStatus();

  });
});
