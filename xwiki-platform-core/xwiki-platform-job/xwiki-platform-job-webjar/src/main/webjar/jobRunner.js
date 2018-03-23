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
define(['jquery'], function($) {
  'use strict';
  var createCallback = function(config, promise) {
    var answerJobQuestion = function(data) {
      // 'this' is the job status.
      var request = config.createAnswerRequest(this.id, data);

      // Create a POST request
      var promise = $.post(request.url, request.data);

      // Automated progress and failure
      promise.done(onProgress).fail(onFailure);

      return promise;
    };

    var onFailure = $.proxy(promise.reject, promise);

    var refresh = function(job) {
      var request = config.createStatusRequest(job.id);
      $.get(request.url, request.data).done(onProgress).fail(onFailure);
    };

    var onProgress = function(job) {
      if (job && job.id && job.state && job.progress) {
        if (job.state == 'WAITING') {
          promise.notify(job, $.proxy(answerJobQuestion, job));

          // Restart the progress if the question timeout is reached
          var timeout = job.questionTimeLeft;
          if (timeout && timeout > -1) {
            setTimeout(function() {
                refresh(job);
              }, timeout / 1000000); // The JSON contains nanoseconds
          }
        } else {
          // Even if the job is finished we still need to notify the last progress update.
          promise.notify(job);
          if (job.state == 'FINISHED') {
            promise.resolve(job);
          } else {
            // The job is still running. Wait before asking for a job status update.
            setTimeout(function() {
              refresh(job);
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
  };
});
