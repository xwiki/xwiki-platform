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
    JobRunner: "$!services.webjars.url('org.xwiki.platform:xwiki-platform-job-webjar', 'jobRunner.min')"
  }
});

require(['jquery', 'JobRunner'], function($, JobRunner) {
  'use strict';
  var updateProgress = function(jobUI, job) {
    var percent = Math.floor((job.progress.offset || 0) * 100);
    jobUI.find('.ui-progress-bar').css('width', percent + '%');
    var jobLog = job.log.items || [];
    if (jobLog.size() > 0) {
      jobUI.find('.ui-progress-message').html(jobLog[jobLog.size() - 1].renderedMessage);
    }
  };

  var updateLog = function(jobUI, job) {
    var jobLog = job.log.items || [];
    var jobLogUI = jobUI.find('.log');
    if (job.log.offset === 0) {
      jobLogUI.html('');
    }
    jobLogUI.find('.log-item-loading').removeClass('log-item-loading');
    $.each(jobLog, function(index, item) {
      var classNames = ['log-item', 'log-item-' + item.level];
      if (job.state !== 'FINISHED' && index === jobLog.size() - 1) {
        classNames.push('log-item-loading');
      }
      $(document.createElement('li')).addClass(classNames.join(' ')).html(item.renderedMessage).appendTo(jobLogUI);
    })
  };

  var updateStatus = function(job) {
    var jobUI = $(this);
    updateProgress(jobUI, job);
    updateLog(jobUI, job);
  };

  var notifyJobDone = function(job) {
    var jobUI = $(this);
    jobUI.find('.ui-progress').replaceWith(job.message);
  };

  var notifyConnectionFailure = function() {
  };

  $('.job-status').has('.ui-progress').each(function() {
    var jobStatus = $(this);
    var url = jobStatus.attr('data-url');
    if (url !== '') {
      var jobLog = jobStatus.find('.log');
      new JobRunner({
        createStatusRequest: function() {
          return {
            url: url,
            data: {
              'logOffset': jobLog.find('.log-item').size()
            }
          };
        }
      }).resume()
        .progress($.proxy(updateStatus, this))
        .done($.proxy(notifyJobDone, this))
        .fail($.proxy(notifyConnectionFailure, this));
    }
  });
});
