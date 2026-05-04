/**
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
import { SessionStorageManager } from "./SessionStorageManager";
import { TourTaskStatus } from "@xwiki/platform-guidedtour-api";
import { driver } from "driver.js";
import type { GuidedTourManager } from "./GuidedTourManager";
import type { TourStep, TourTask } from "@xwiki/platform-guidedtour-api";
import type { Config, DriveStep, Driver } from "driver.js";

/* eslint-disable max-statements */
const util = {
  /**
   * Do the necessary setup for rendering the `Skip All` link.
   * @param guidedTourManager - API
   * @param task - the task to make the button for
   * @returns The `Skip All` element
   */
  makeSkipAllButton(
    guidedTourManager: GuidedTourManager,
    task: TourTask,
  ): Element {
    const customSkipAll = document.createElement("a");
    customSkipAll.classList.add("driver-xwiki-skip-all-button");
    function onSkipAll() {
      guidedTourManager.setTaskStatus(task, TourTaskStatus.SKIPPED);
      guidedTourManager.activeDriverTask?.destroy();
    }
    customSkipAll.onclick = onSkipAll;
    customSkipAll.innerHTML = "Skip All"; // TODO: Add translation.
    return customSkipAll;
  },
  /**
   * For use in `waitForElement` below.
   *
   * @param element - The element to check
   * @returns true if the element is visible on the page, false otherwise.
   */
  isElementVisible(element: Element): boolean {
    const style = window.getComputedStyle(element);
    if (style.display == "none") {
      return false;
    } else {
      return true;
    }
  },
  /**
   * Wait until an element is visible on the page.
   *
   * @param selector - css selector for the element to wait for (should be compatible with document.querySelector)
   * @param probeInterval - time (in ms) to wait after a failed check for the specified element
   * @param maxIntervals - how many probe intervals to wait until rejecting
   * @returns a promise which succeeds if the element is found within the time limit, and fails otherwise
   */
  async waitForElement(
    selector: string,
    probeInterval = 500,
    maxIntervals = 6,
  ): Promise<Element | undefined> {
    return util.retryWithCallback(
      () => {
        const queriedElement = document.querySelector(selector);
        if (queriedElement && util.isElementVisible(queriedElement)) {
          return queriedElement;
        } else {
          return undefined;
        }
      },
      probeInterval,
      maxIntervals,
      selector,
    );
  },
  /**
   *
   * @param callbackFn - The function to run to test if our goal has been achieved. Should return truthy if achieved, false otherwise (if we still need to wait)
   * @param probeInterval - time (in ms) to wait after a failed check for the specified element
   * @param maxIntervals - how many probe intervals to wait until rejecting
   * @param consoleName - For debugging, to display in console
   * @returns undefined for timeout, the return value of callbackFn if successful.
   */
  async retryWithCallback<T>(
    callbackFn: () => T | undefined,
    probeInterval = 500,
    maxIntervals = 6,
    consoleName = "something",
  ) {
    // TODO: Could maybe use MutationObservers here?
    console.debug(`waiting for ${consoleName}...`);
    for (let i = 0; i < maxIntervals; i += 1) {
      const retValue = callbackFn();
      if (retValue) {
        return retValue;
      }
      console.debug(`(${i}/${maxIntervals}) waiting for ${consoleName}...`);
      await new Promise((resolve) => setTimeout(resolve, probeInterval));
    }
    console.debug(
      `Failed to confirm ${consoleName} after waiting ${probeInterval * maxIntervals} (${probeInterval} * ${maxIntervals}) ms.`,
    );
    return undefined;
  },
};

/**
 * This is a function to ensure each call has it's own object, and subsequent manipulation doesn't alter the defaults.
 */
function XWikiDriverConfig(
  guidedTourManager: GuidedTourManager,
  task: TourTask,
): Config {
  console.log("Setting up", task);
  // Old code calls this variable `tour`.
  return {
    nextBtnText: "Next >", // TODO: Add translation.
    prevBtnText: "< Previous", // TODO: Add translation.
    showProgress: true,
    showButtons: ["previous", "next", "close"], // FIXME: Idk about this
    overlayOpacity: 0.3,
    onPopoverRender: (popDOM, options) => {
      // TODO: Need to handle this better
      const activeIndex = options.state.activeIndex ?? -1;
      // const activeStep = options.state.activeStep!;
      // console.info(popDOM, options, window.localStorage.getItem(tour.getConfig().name + '_current_step'));
      // window.localStorage.setItem(tour.getConfig().name + '_current_step', activeIndex); // This should be config.activeIndex instead.
      const showButtons = task.steps![activeIndex].reflex
        ? ["close"]
        : ["previous", "next", "close"];
      if (!showButtons?.includes("next")) {
        popDOM.footerButtons.removeChild(popDOM.nextButton);
      }
      if (!showButtons?.includes("previous")) {
        popDOM.footerButtons.removeChild(popDOM.previousButton);
      }
      console.log(
        `Reflex is ${task.steps![activeIndex].reflex} for step ${activeIndex}`,
        task.steps![activeIndex],
        options.config.steps![activeIndex],
        showButtons,
      );
      popDOM.progress.style.display = "";
      popDOM.progress.innerText =
        "⬤ ".repeat(activeIndex + 1) +
        "◯ ".repeat(options.config.steps!.length - activeIndex - 1);
      options.config.overlayOpacity = task.steps![activeIndex].backdrop
        ? 0.3
        : 0;
      popDOM.footer.appendChild(
        util.makeSkipAllButton(guidedTourManager, task),
      );
      popDOM.nextButton.classList.add("btn", "btn-sm", "btn-primary"); // TODO: Make this an <a> instead of <button>
      popDOM.previousButton.classList.add("btn", "btn-sm"); // TODO: Make this an <a> instead of <button>
      // To ensure the order is consistent, re-build the entire modal.
      popDOM.wrapper.insertBefore(popDOM.progress, popDOM.title);
    },
    onDestroyed: function (_element, _step, _options) {
      console.debug("onDestroyed", _element, _step, _options);
      // TODO: Add back the commented code, when you find a way to reliably detect whether a task was skipped.
      // The state is empty when this function is called.
      const status =
        Number.parseInt(
          SessionStorageManager.getStorageKey(
            SessionStorageManager.getTaskStepStorageKey(task),
          ) ?? "-1",
        ) +
          1 >=
        task.steps!.length
          ? TourTaskStatus.DONE
          : TourTaskStatus.SKIPPED;
      guidedTourManager.setTaskStatus(task, status);
      // TODO: See if this is needed.
      // TODO: Maybe move this to guidedTourManager.setTaskStatus(task, status) ?
      SessionStorageManager.setStorageKey(
        SessionStorageManager.getActiveTaskStorageKey(),
        undefined,
      );
      guidedTourManager.activeTask = undefined;
      guidedTourManager.activeDriverTask = undefined;
    },

    // TODO: Remove this linter disable and refactor the function.
    onNextClick: async (_highlightedElement, _step, options) => {
      if (options.state.activeIndex! + 1 == options.config.steps!.length) {
        console.debug(
          "No next step. We're probably in the last step attempting to go next.",
        );
        guidedTourManager.setTaskStatus(task, TourTaskStatus.DONE);
        // guidedTourManager.activeDriverTask!.destroy();
        guidedTourManager.activeDriverTask!.moveNext();
        return;
      } else {
        // FIXME: Refactor this
        const nextStep = task.steps![options.state.activeIndex! + 1];
        if (task.steps![options.state.activeIndex!].path == nextStep.path) {
          // FIXME: Is getActiveIndex() needed, or should I use options.activeIndex instead?
          const activeIndex =
            guidedTourManager.activeDriverTask!.getActiveIndex()!;
          if (nextStep.element !== undefined) {
            const targetedElement = await util.waitForElement(nextStep.element);
            if (targetedElement) {
              bindReflexEvents(
                targetedElement,
                nextStep,
                guidedTourManager,
                async () => {
                  console.warn(
                    "Calling callback for next step move (hopefully)",
                  );
                  // FIXME: This recursion should be guarded better, lest there be an infinite recursion.
                  await options.config.onNextClick!(
                    _highlightedElement,
                    _step,
                    options,
                  );
                },
              );
            } else {
              if (
                activeIndex ==
                guidedTourManager.activeDriverTask!.getActiveIndex()
              ) {
                // The task is still at the step we expect.
                // We had an error, so the task is probably broken, so skip it.
                guidedTourManager.setTaskStatus(task, TourTaskStatus.SKIPPED);
                guidedTourManager.activeDriverTask!.destroy();
                return;
              } else {
                // The task moved to some previous step while we were waiting, so no need to do anything.
                return;
              }
            }
          }
          if (
            activeIndex == guidedTourManager.activeDriverTask!.getActiveIndex()
          ) {
            guidedTourManager.activeDriverTask!.moveNext();
            return;
          } else {
            console.debug(
              `Tried to move from ${activeIndex} to next , but the step is actually now ${guidedTourManager.activeDriverTask!.getActiveIndex()}`,
            );
            return;
          }
        } else {
          console.debug(
            "Attempted to go to next step, but that one is on another page.",
          );
          // TODO: Maybe add a redirect here.
          return;
        }
      }
    },
    onPrevClick: async (_highlightedElement, _step, options) => {
      // Cache the current step index, so we can check later (after async operations) if we are in the same step we started in.
      const activeIndex = guidedTourManager.activeDriverTask!.getActiveIndex()!;
      if (options.state.activeIndex == 0) {
        console.debug(
          "No previous step. We're probably in the first step attempting to go back.",
        );
        guidedTourManager.activeDriverTask!.movePrevious();
        return;
      } else {
        const prevStep = task.steps![options.state.activeIndex! - 1];
        if (prevStep.element !== undefined) {
          const targetedElement = await util.waitForElement(prevStep.element!);
          if (targetedElement) {
            bindReflexEvents(
              targetedElement,
              prevStep,
              guidedTourManager,
              async () => {
                console.warn("Calling callback for prev step move (hopefully)");
                // FIXME: This recursion should be guarded better, lest there be an infinite recursion.
                await options.config.onPrevClick!(
                  _highlightedElement,
                  _step,
                  options,
                );
              },
            );
          } else {
            if (
              activeIndex ==
              guidedTourManager.activeDriverTask!.getActiveIndex()
            ) {
              // The task is still at the step we expect.
              // We had an error, so the task is probably broken, so skip it.
              console.error(
                `Failed to find ${prevStep.element} element in the page when going back to step ${prevStep.order}`,
                prevStep,
              );
              new XWiki.widgets.Notification(
                "Failed to find targeted element. Skipping the task.",
                "error",
              );
              guidedTourManager.setTaskStatus(task, TourTaskStatus.SKIPPED);
              guidedTourManager.activeDriverTask!.destroy();
              return; // TODO: Make this complicated stuff a promise, and resolve it only when we get here, so the caller can just .then() it to decide what to do next.
            } else {
              // The task moved to some previous step while we were waiting, so no need to do anything.
              return;
            }
          }
        }
        if (task.steps![options.state.activeIndex!].path == prevStep.path) {
          guidedTourManager.activeDriverTask!.movePrevious();
          return;
        } else {
          console.debug(
            "Attempted to go to prev step, but that one is on another page.",
          );
          // TODO: Maybe add a redirect here.
          return;
        }
      }
    },
    // overlayClickBehavior: () => {},
  };
}

function convertToDriverStep(step: TourStep, task: TourTask): DriveStep {
  return {
    element: step.element,
    popover: {
      title: step.title ?? task.title,
      description: step.content,
    },
  };
}

function getDriverConfigForSteps(
  steps: TourStep[],
  task: TourTask,
  guidedTourManager: GuidedTourManager,
) {
  console.log(steps);
  const config = XWikiDriverConfig(guidedTourManager, task);
  config.steps = steps.map((step) => convertToDriverStep(step, task));
  return config;
}

function wrapTask(task: Driver, guidedTourManager: GuidedTourManager): Driver {
  const _drive = task.drive;
  const _moveNext = task.moveNext;
  const _movePrevious = task.movePrevious;
  const _destroy = task.destroy;
  task.drive = function (stepIndex: number = 0) {
    _drive(stepIndex);
    SessionStorageManager.setStorageKey(
      SessionStorageManager.getTaskStepStorageKey(
        guidedTourManager.activeTask!,
      ),
      stepIndex.toString(),
    );
  }.bind(task);

  task.moveNext = function () {
    let expectPageRefresh: boolean = false;
    // Cache the active task, since _moveNext() will destroy it if we're on the last step.
    const activeTask = guidedTourManager.activeTask!;
    // FIXME: This default should be changed maybe...
    const currentStepIndex =
      guidedTourManager.activeDriverTask?.getActiveIndex() ?? 1000000;
    const newStepIndex: number = currentStepIndex + 1;
    if (newStepIndex >= activeTask.steps!.length) {
      // No next step to go to.
      expectPageRefresh = false;
    } else {
      const currentStep =
        guidedTourManager.activeTask!.steps![currentStepIndex];
      const newStep = guidedTourManager.activeTask!.steps![newStepIndex];
      console.info("Comparing these paths:");
      console.info(currentStep);
      console.info(newStep);

      expectPageRefresh = currentStep.path != newStep.path;
    }
    // FIXME: This is just so the tours work for the branch merge, it should be removed.
    expectPageRefresh = false;
    console.debug(
      "newStepIndex",
      newStepIndex,
      "expectPageRefresh",
      expectPageRefresh,
    );
    let newStep;
    if (!expectPageRefresh) {
      // If the page is supposed to refresh/redirect, driver won't try to advance to the next step, but will instead
      // stay in place until the page reloads. The session storage key is still set, so we'll recover
      _moveNext();
      newStep =
        guidedTourManager.activeDriverTask?.getActiveIndex() ?? undefined;
    } else {
      newStep =
        guidedTourManager.activeDriverTask?.getActiveIndex() ?? undefined;
      newStep = newStep !== undefined ? newStep + 1 : undefined;
      console.log(
        "Not advancing the tour since we expect a refresh. Session storage is being set to",
        newStep,
      );
    }
    SessionStorageManager.setStorageKey(
      SessionStorageManager.getTaskStepStorageKey(activeTask),
      newStep?.toString() ?? undefined,
    );
  }.bind(task);

  task.movePrevious = function () {
    let expectPageRefresh: boolean = false;
    // Cache the active task, since _moveNext() will destroy it if we're on the last step.
    const activeTask = guidedTourManager.activeTask!;
    // FIXME: This default should be changed maybe...
    const currentStepIndex =
      guidedTourManager.activeDriverTask?.getActiveIndex() ?? -1000000;
    let newStepIndex: number | undefined = currentStepIndex - 1;
    if (newStepIndex < 0) {
      // No prev step to go to.
      newStepIndex = undefined;
      expectPageRefresh = false;
    } else {
      const currentStep =
        guidedTourManager.activeTask!.steps![currentStepIndex];
      const newStep = guidedTourManager.activeTask!.steps![newStepIndex];
      console.info("Comparing these paths:");
      console.info(currentStep);
      console.info(newStep);

      expectPageRefresh = currentStep.path != newStep.path;
    }
    // FIXME: This is just so the tours work for the branch merge, it should be removed.
    expectPageRefresh = false;
    console.debug(
      "newStepIndex",
      newStepIndex,
      "expectPageRefresh",
      expectPageRefresh,
    );
    let newStep;
    if (!expectPageRefresh) {
      // If the page is supposed to refresh/redirect, driver won't try to advance to the next step, but will instead
      // stay in place until the page reloads. The session storage key is still set, so we'll recover
      _movePrevious();
      newStep =
        guidedTourManager.activeDriverTask?.getActiveIndex() ?? undefined;
    } else {
      newStep =
        guidedTourManager.activeDriverTask?.getActiveIndex() ?? undefined;
      newStep = newStep !== undefined ? newStep - 1 : undefined;
      console.log(
        "Not advancing the tour since we expect a refresh. Session storage is being set to",
        newStep,
      );
    }
    SessionStorageManager.setStorageKey(
      SessionStorageManager.getTaskStepStorageKey(activeTask),
      newStep?.toString() ?? undefined,
    );
  }.bind(task);

  task.destroy = function () {
    const currentStep = task.getActiveIndex();
    console.info(`Trying to see if, on destroy, `);
    if (currentStep != task.getConfig().steps?.length) {
      SessionStorageManager.setStorageKey(
        SessionStorageManager.getTaskStepStorageKey(
          guidedTourManager.activeTask!,
        ),
        task.getActiveIndex()!.toString(),
      );
    } else {
      // Delete the current step storage key.
      SessionStorageManager.setStorageKey(
        SessionStorageManager.getTaskStepStorageKey(
          guidedTourManager.activeTask!,
        ),
        undefined,
      );
    }
    _destroy();
  }.bind(task);

  return task;
}

export { XWikiDriverConfig, driver, getDriverConfigForSteps, wrapTask };

/**
 * Adds a callback that triggers when the HTML element is interacted with.
 *
 * @param element - The element which should be interacted with in order to proceed.
 * @param step - The current step. Used to confirm that the active step is the expected one.
 * @param guidedTourManager - The guidedTourManager Api instance.
 * @param callbackFn - A callback to execute once the element is interacted with.
 */
function bindReflexEvents(
  element: Element,
  step: TourStep,
  guidedTourManager: GuidedTourManager,
  callbackFn: () => void,
) {
  const callback = (event: Event) => {
    console.debug(event);
    if (
      event.target instanceof HTMLInputElement &&
      event.target.type == "text"
    ) {
      // Special case for text inputs.
      // Right now, the text input awaits for 5s before continuing, to allow the user to type stuff.
      // TODO: Maybe add a 'match text' setting for going next.
      const msTimeout = 5000;
      new Promise((resolve) => setTimeout(resolve, msTimeout))
        .then(() => {
          console.debug("sloip awoked");
          if (
            step.order != guidedTourManager.activeDriverTask?.getActiveIndex()
          ) {
            // FIXME: This might not work.
            console.debug("Removing reflex listener on ", element);
            element.removeEventListener("click", callback);
            callbackFn();
          }
          return;
        })
        .catch(console.error);
    } else {
      // FIXME: This might not work.
      console.debug("Removing reflex listener on ", element);
      element.removeEventListener("click", callback);
      callbackFn();
      /* The localStorage item should theoretically be set to the same value in the moveNext(), so no harm done
      new Promise(resolve => setTimeout(resolve, 1500)).then(() => {
        // Wait a bit to see if the clicked element was actually a redirect.
        if (!beforeUnloadFired) {
          tour.moveNext();
        }
      });*/
    }
  };
  console.debug("Adding reflex listener on ", element);
  element.addEventListener("click", callback);
}

// if (activeStep.reflex) {
//   document.querySelector(String(activeStep.element))?.addEventListener('click', (event) => {
//     console.debug(event);
//     if (event.target.tagName == 'INPUT' && event.target.type == 'text') {
//       // Special case for text inputs.
//       // Right now, the text input awaits for 5s before continuing, to allow the user to type stuff.
//       // TODO: Maybe add a 'match text' setting for going next.
//       const msTimeout = 5000;
//       new Promise(resolve => setTimeout(resolve, msTimeout)).then(() => {
//         console.debug('sloip awoked');
//         // Increment the local storage step. (For cases when clicking the element leads to a redirect.)
//       //debugger;
//         window.localStorage.setItem(tour.getConfig().name + '_current_step', 1 + activeIndex);
//         if (window.localStorage.getItem(tour.getConfig().name + '_current_step') == tour.getConfig().steps.length) {
//           window.localStorage.setItem(tour.getConfig().name + '_end', 'yes');
//           window.guidedTourInProgress = false;
//         }
//         // The localStorage item should theoretically be set to the same value in the moveNext(), so no harm done
//         // Fire onNext ?
//         document.fire('arrowRightPress')
//         if (activeIndex != options.state.activeIndex) {
//           tour.moveNext();
//         }
//       })
//     } else {
//       // Increment the local storage step. (For cases when clicking the element leads to a redirect.)
//       //debugger;
//       window.localStorage.setItem(tour.getConfig().name + '_current_step', 1 + activeIndex);
//       if (window.localStorage.getItem(tour.getConfig().name + '_current_step') == tour.getConfig().steps.length) {
//         window.localStorage.setItem(tour.getConfig().name + '_end', 'yes');
//         window.guidedTourInProgress = false;
//       }
//       console.info(tour)
//       document.fire('arrowRightPress')
//       tour.moveNext();
//       /* The localStorage item should theoretically be set to the same value in the moveNext(), so no harm done
//       new Promise(resolve => setTimeout(resolve, 1500)).then(() => {
//         // Wait a bit to see if the clicked element was actually a redirect.
//         if (!beforeUnloadFired) {
//           tour.moveNext();
//         }
//       });*/
//     }
//   });
// }
/*

      // Look if the tour should be started regardless of its status on the local storage
      var getQueryStringParameterByName = function (name) {
        var match = RegExp('[?&]' + name + '=([^&]*)').exec(window.location.search);
        return match && decodeURIComponent(match[1].replace(/\+/g, ' '));
      }
      var forceStart = getQueryStringParameterByName('startTour') == 'true';
      var tourEnded = window.localStorage.getItem(tourName + '_end') === 'yes';

      // Initialize the current step index from local storage.
      var currentStep = tour.getActiveStep();
      var tourAutoStart = !tourEnded; // && !tourNeedsRedirect
      if (window.localStorage.getItem(tour.getConfig().name + '_current_step') === null) {
        // Set the current step if the tour hasn't ran.
        window.localStorage.setItem(tour.getConfig().name + '_current_step', 1);
      }
      if (forceStart) {
        // Just start at the first step, man. (I didn't test that this is the behavior on the old Tour Extension too)
        if (!window.guidedTourInProgress) {
          window.guidedTourInProgress = true;
          tour.drive(0);
        }
      } else if (tourAutoStart) {
        if (!(window.localStorage.getItem(tour.getConfig().name + '_end') === 'yes')) {
          // Should probably just straigth up monkey patch the .drive function to handle persistance too, since it seems to be the common entry point for all driver.js functions.
          let stepIndex = window.localStorage.getItem(tour.getConfig().name + '_current_step') - 1;
          console.info(tour, stepIndex);
          console.info("I want " + tour.getConfig().steps[stepIndex].path + ", got " + window.location.pathname)
          if (tour.getConfig().steps[stepIndex].path == window.location.pathname && !window.guidedTourInProgress) {
            window.guidedTourInProgress = true;
            tour.drive(stepIndex);
          }
        } else {
            window.guidedTourInProgress = false;
        }
      }
*/

// FIXME: From old TourJS.xml
/*
function loadCss(href) {
    var link = document.createElement("link");
    link.type = "text/css";
    link.rel = "stylesheet";
    //link.href = href;
    //link.href = "$services.webjars.url('org.webjars.npm:driver.js', 'dist/driver.css')";
    document.getElementsByTagName("head")[0].appendChild(link);
}

//----------------------------------
// Display a tour if needed
//----------------------------------
require(['jquery', 'xwiki-meta', 'guidedtour-utils'], function ($, xm, utils) {
  'use strict';
  // TODO: Check for unused translation strings at the end of development.
  // TODO: Make use of _redirect_to localStorage key somewhere in the code.

  /**
   * Load asynchronously the list of steps concerning the current page.
   * It's done asynchronously so it does not improve the page rendering time. It's important since this code is used
   * everywhere.
   *\/
  $(function() {
window.guidedTourInProgress = false;
    /**
     * The tour is not adapted for little screen sizes like mobile phones have.
     * The value 768 is taken from bootstrap in order to be consistent with their media queries.
     *\/
    if ($(window).innerWidth() <= 768) {
      // return; // This is so annoying when debugging.
    }
          if (step['element'] == '') {
            if (false == step['backdrop']) {
              step['element'] = 'body'; // FIXME: This is NOT FULL PROOF, this should be changed (eg. I want to highlight a random element without a backdrop).
              step['popover']['side'] = 'over';
            } else {
              delete step.element;
            }
          }
        if (tour.steps.length > 0) {
          createTour(tour); // Gave a driver object, but didn't start it.
        }
      }

// FIXME: From old TourJS.xml
tour.drive = function (stepIndex) {
      // TODO: Check precondition for next step;
      // TODO: Check if the next step is on the right page (to account for href redirects, etc);
      // TODO: Wait for next step element to appear;
      // TODO: Set the right localStorage stuff;
      console.info('custom drive() here:', tour, stepIndex, this);
      originalDrive(stepIndex);
      /**if (!(window.localStorage.getItem(tour.getConfig().name + '_end') === 'yes')) {
        let localStorageStepIndex = window.localStorage.getItem(tour.getConfig().name + '_current_step') - 1;
        console.info(tour, localStorageStepIndex);
        console.info("I want " + tour.getConfig().steps[localStorageStepIndex].path + ", got " + window.location.pathname)
        if (tour.getConfig().steps[localStorageStepIndex].path == window.location.pathname &amp;&amp; !window.guidedTourInProgress) {
          window.guidedTourInProgress = true;
          originalDrive(stepIndex);
        }
      } else {
        window.guidedTourInProgress = false;
      }**\/
    };
  // Helper to bind click events, TODO: could be deleted.
  function bindFloaterClickEvent(selector, callback) {
    $('.guidedtour-widget ' + selector).on('click', (event) => {
      callback(event);
    });
  };

  bindFloaterClickEvent('.top-bar', (event) => {
    window.localStorage.setItem('TourFloaterCollapsed', document.querySelector('.guidedtour-widget').classList.toggle('collapsed'));
  });

  bindFloaterClickEvent('#widget-close', (event) => {
    if (event.target.closest('.guidedtour-widget').classList.contains('collapsed')) {
      event.target.closest('.guidedtour-widget').remove();
      // window.localStorage.setItem('TourFloaterCollapsed', 'hidden') // Commented to not disable the widget completely, permanently.
      event.stopPropagation();
      // TODO: Or just hide it, and set some cookie/option to not load the javascript code at all the next time.
    }
  });

  bindFloaterClickEvent('#widget-options', (event) => {
    event.stopPropagation();
    console.info('Opened settings menu');
  });

  if (window.localStorage.getItem('guidedtour-widget-position-x')) {
    // FIXME: Could be XSS i think, if someone edits this key. But that's how the right side panel works too.
    // FIXME: Clamp the allowed values, so the widget is always visible on the screen. Maybe set the value as percentage of screen width? In the dragging functions I mean.
    document.querySelector('.guidedtour-widget').style.left = window.localStorage.getItem('guidedtour-widget-position-x');
  }

  // Definitions.
  /*
   * Function to set up a draggable element, for the widget.
   * Taken from https://www.w3schools.com/howto/howto_js_draggable.asp
   *\/
  function dragElement(elmnt) {
    console.debug(elmnt)
    if (!elmnt.classList.contains('draggable')) {
      return;
    }
    var pos1 = 0, pos2 = 0, pos3 = 0, pos4 = 0;
    // otherwise, move the DIV from anywhere inside the DIV:
    elmnt.onmousedown = dragMouseDown;

    function dragMouseDown(e) {
      e = e || window.event;
      e.preventDefault();
      e.stopPropagation();
      e.stopImmediatePropagation();
      // get the mouse cursor position at startup:
      pos3 = e.clientX;
      pos4 = e.clientY;
      document.onmouseup = closeDragElement;
      // call a function whenever the cursor moves:
      document.onmousemove = elementDrag;
    }

    function elementDrag(e) {
      e = e || window.event;
      e.preventDefault();
      e.stopPropagation();
      e.stopImmediatePropagation();
      document.body.style.setProperty('cursor', 'grabbing', 'important');
      elmnt.classList.add('dragging');
      // calculate the new cursor position:
      pos1 = pos3 - e.clientX;
      pos2 = pos4 - e.clientY;
      pos3 = e.clientX;
      pos4 = e.clientY;
      // set the element's new position:
      //elmnt.style.top = (elmnt.offsetTop - pos2) + "px"; // Commented so the drag only goes side-to-side, not up-down.
      // TODO: Make sure the widget doesn't end up outside the window post-window-resize.
      elmnt.style.left = (elmnt.offsetLeft - pos1) + "px";
    }

    function closeDragElement(e) {
      // Stop moving when mouse button is released:
      e.preventDefault();
      e.stopPropagation();
      e.stopImmediatePropagation();
      document.onmouseup = null;
      document.onmousemove = null;
      document.body.style.cursor = "";
      elmnt.classList.remove('dragging');
      window.localStorage.setItem('guidedtour-widget-position-x', elmnt.style.left);
    }
  }
});
*/
