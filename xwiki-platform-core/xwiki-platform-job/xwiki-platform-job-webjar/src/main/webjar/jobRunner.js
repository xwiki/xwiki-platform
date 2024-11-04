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
  const createCallback = function(config, promise) {
    // Store if we were already waiting for a question answer
    let waitingForAnswer = false;

    const answerJobQuestion = function(data) {
      // Ensure that we update the status of the job after answering the question even if the job should wait for
      // another answer.
      waitingForAnswer = false;

      // 'this' is the job status.
      const request = config.createAnswerRequest(this.id, data);

      // Create a POST request
      const promise = $.post(request.url, request.data);

      // Automated progress and failure
      promise.then(onProgress, onFailure);

      return promise;
    };

    const onFailure = promise.reject.bind(promise);

    const refresh = function(job) {
      const request = config.createStatusRequest(job.id);
      $.get(request.url, request.data).then(onProgress, onFailure);
    };

    function computeNextRefreshInterval(job)
    {
      let timeout = config.updateInterval || 1000;
      // If we are waiting for a question answer and the timeout is earlier than the job status update interval,
      // then we should wait for the question timeout instead to ensure a prompt refresh when the question expires.
      if (waitingForAnswer && job.questionTimeLeft && job.questionTimeLeft > -1 &&
          job.questionTimeLeft / 1000000 <= timeout)
      {
        timeout = job.questionTimeLeft / 1000000; // The JSON contains nanoseconds
        waitingForAnswer = false; // We will refresh the job status after the question timeout
      }
      return timeout;
    }

    const onProgress = function(job) {
      if (job?.id && job?.state && job?.progress) {
        if (job.state === 'WAITING') {
          if (!waitingForAnswer) {
            promise.notify(job, answerJobQuestion.bind(job));
            waitingForAnswer = true;
          }
        } else {
          // The status could have been updated without answering the question, e.g., when the question was answered
          // in a different browser tab or when the job was resumed server-side.
          waitingForAnswer = false;
          // Even if the job is finished we still need to notify the last progress update.
          promise.notify(job);
        }

        if (job.state === 'FINISHED') {
          promise.resolve(job);
        } else {
          // The job is still running. Wait before asking for a job status update.
          setTimeout(function () {
            refresh(job);
          }, computeNextRefreshInterval(job));
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
      const promise = $.Deferred();
      const callback = createCallback(config, promise);
      const request = config.createStatusRequest(jobId);
      $.get(request.url, request.data).then(callback.onProgress, callback.onFailure);
      return promise;
    };

    this.run = function(url, data) {
      const promise = $.Deferred();
      const callback = createCallback(config, promise);
      $.post(url, data).then(callback.onProgress, callback.onFailure);
      return promise;
    };

    return this;
  };
});
