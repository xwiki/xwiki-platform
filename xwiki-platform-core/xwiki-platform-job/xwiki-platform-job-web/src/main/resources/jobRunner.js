define(['jquery'], function($) {
  var createCallback = function(config, promise) {
    var answerJobQuestion = function(data) {
      // 'this' is the job status.
      var request = config.createAnswerRequest(this.id, data);
      $.post(request.url, request.data).done(onProgress).fail(onFailure);
    };

    var onFailure = $.proxy(promise.reject, promise);

    var onProgress = function(job) {
      if (job && job.id && job.state && job.progress) {
        if (job.state == 'WAITING') {
          promise.notify(job, $.proxy(answerJobQuestion, job));
        } else {
          // Even if the job is finished we still need to notify the last progress update.
          promise.notify(job);
          if (job.state == 'FINISHED') {
            promise.resolve(job);
          } else {
            // The job is still running. Wait before asking for a job status update.
            setTimeout(function() {
              var request = config.createStatusRequest(job.id);
              $.get(request.url, request.data).done(onProgress).fail(onFailure);
            }, config.updateInterval || 1000);
          }
        }
      } else {
        promise.resolve(job);
      }
    };

    return {
      answerJobQuestion: answerJobQuestion,
      onFailure: onFailure,
      onProgress: onProgress
    };
  };

  /**
   * Configuration object:
   * {
   *   createStatusRequest: function(jobId) {},
   *   createAnswerRequest: function(jobId, data) {},
   *   updateInterval: 1000 (in milliseconds)
   * }
   */
  return function(config) {
    this.resume = function(jobId) {
      var promise = $.Deferred();
      var callback = createCallback(config, promise);
      var request = config.createStatusRequest(jobId);
      $.get(request.url, request.data).done(callback.onProgress).fail(callback.onFailure);
      return promise;
    };

    this.run = function(url, data) {
      var promise = $.Deferred();
      var callback = createCallback(config, promise);
      $.post(url, data).done(callback.onProgress).fail(callback.onFailure);
      return promise;
    };

    return this;
  }
});
