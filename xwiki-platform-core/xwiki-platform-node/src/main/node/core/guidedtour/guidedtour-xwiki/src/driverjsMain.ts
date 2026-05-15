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
import { StorageManager } from "./StorageManager";
import { TourTaskStatus } from "@xwiki/platform-guidedtour-api";
import { driver } from "driver.js";
import type { DefaultGuidedTourManager } from "./rest/DefaultGuidedTourManager";
import type { TourStep, TourTask } from "@xwiki/platform-guidedtour-api";
import type { Config, DriveStep, Driver, PopoverDOM } from "driver.js";

const util = {
  /**
   * Do the necessary setup for rendering the `Skip All` link.
   * @param guidedTourManager - API
   * @param guidedTourTask - the task to make the button for
   * @returns The `Skip All` element
   */
  makeSkipAllButton(
    guidedTourManager: DefaultGuidedTourManager,
    guidedTourTask: TourTask,
  ): Element {
    const customSkipAll = document.createElement("a");
    customSkipAll.classList.add("driver-xwiki-skip-all-button");
    function onSkipAll() {
      guidedTourManager.setTaskStatus(guidedTourTask, TourTaskStatus.SKIPPED);
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
    selector: string | undefined,
    probeInterval = 500,
    maxIntervals = 6,
  ): Promise<Element | undefined> {
    if (!selector) {
      return;
    }
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
  /**
   * Decides which buttons should be visible in the modal, and updates the DOM.
   * @param popDOM - The DOM of the driverjs modal
   * @param step - The current step
   */
  solveButtons(
    popDOM: PopoverDOM,
    step: TourStep,
    guidedTourManager: DefaultGuidedTourManager,
    guidedTourTask: TourTask,
  ) {
    if (step.reflex) {
      popDOM.footerButtons.removeChild(popDOM.nextButton);
      popDOM.footerButtons.removeChild(popDOM.previousButton);
    } else {
      popDOM.nextButton.classList.add("btn", "btn-sm", "btn-primary"); // TODO: Make this an <a> instead of <button>
      popDOM.previousButton.classList.add("btn", "btn-sm"); // TODO: Make this an <a> instead of <button>
    }
    popDOM.footer.appendChild(
      util.makeSkipAllButton(guidedTourManager, guidedTourTask),
    );
  },
  async shouldSwitchSteps(
    guidedTourTask: TourTask,
    thisStepActiveIndex: number,
    currentStep: TourStep,
    nextStep: TourStep,
    guidedTourManager: DefaultGuidedTourManager,
  ) {
    // TODO: Add a better check here, in a separate method.
    // if (currentStep.path != nextStep.path) {
    //   console.debug(
    //     "Attempted to go to next step, but that one is on another page.",
    //     currentStep,
    //     nextStep,
    //   );
    //   // TODO: Maybe add a redirect here.
    //   return false;
    // }

    // FIXME: Refactor this
    return await util
      .waitForElement(nextStep.element)
      .then((targetedElement) => {
        if (
          thisStepActiveIndex !=
          guidedTourManager.activeDriverTask!.getActiveIndex()
        ) {
          // The task moved to some other step while we were waiting, so no need to do anything.
          return false;
        }

        if (nextStep.element !== undefined && targetedElement === undefined) {
          // We didn't find the expected element in the page, so the task is probably broken, so skip it.
          console.error(
            `Failed to find ${nextStep.element} element in the page when going back to step ${nextStep.order}`,
            nextStep,
          );
          new XWiki.widgets.Notification(
            "Failed to find targeted element. Skipping the task.",
            "error",
          );
          guidedTourManager.activeDriverTask!.destroy();
          return false;
        }
        return targetedElement;
      });
  },
};

/**
 * This is a function to ensure each call has it's own object, and subsequent manipulation doesn't alter the defaults.
 */
function XWikiDriverConfig(
  guidedTourManager: DefaultGuidedTourManager,
  guidedTourTask: TourTask,
): Config {
  console.log("Setting up", guidedTourTask);
  // Old code calls this variable `tour`.
  return {
    nextBtnText: "Next >", // TODO: Add translation.
    prevBtnText: "< Previous", // TODO: Add translation.
    showProgress: true,
    showButtons: ["previous", "next", "close"],
    overlayOpacity: 0.3,
    onPopoverRender: (popDOM, options) => {
      // TODO: Need to handle this better
      const activeIndex = options.state.activeIndex ?? -1;
      util.solveButtons(
        popDOM,
        guidedTourTask.steps![activeIndex],
        guidedTourManager,
        guidedTourTask,
      );

      popDOM.progress.style.display = "";
      popDOM.progress.innerText =
        "⬤ ".repeat(activeIndex + 1) +
        "◯ ".repeat(options.config.steps!.length - activeIndex - 1);
      options.config.overlayOpacity = guidedTourTask.steps![activeIndex]
        .backdrop
        ? 0.3
        : 0;
      popDOM.wrapper.insertBefore(popDOM.progress, popDOM.title);

      // The user will see this step, so update the storage key.
      StorageManager.setStorageKey(
        StorageManager.getTaskCurrentStepStorageKey(guidedTourTask),
        activeIndex.toString(),
      );
    },
    onDestroyed: function (_element, _step, _options) {
      console.debug("onDestroyed", _element, _step, _options);
      // TODO: Add back the commented code, when you find a way to reliably detect whether a task was skipped.
      // The state is empty when this function is called.
      const status =
        Number.parseInt(
          StorageManager.getStorageKey(
            StorageManager.getTaskCurrentStepStorageKey(guidedTourTask),
          ) ?? "-1",
        ) +
          1 >=
        guidedTourTask.steps!.length
          ? TourTaskStatus.DONE
          : TourTaskStatus.SKIPPED;
      guidedTourManager.setTaskStatus(guidedTourTask, status);
      // TODO: See if this is needed.
      // TODO: Maybe move this to guidedTourManager.setTaskStatus(task, status) ?
      StorageManager.setStorageKey(
        StorageManager.getActiveTaskStorageKey(),
        undefined,
      );
      guidedTourManager.activeTask = undefined;
      guidedTourManager.activeDriverTask = undefined;
    },
    // TODO: Remove this linter disable and refactor the function.
    onNextClick: async () => {
      /*
       * TODO: Things to consider:
       * - Is the current step the one I expect?
       * - Am I in the last step?
       * - Am I expecting a redirect?
       * - Wait for element to appear
       * - Setting the guidedTourTask status shouldn't be done here
       */
      // Cache the current step index, so we can check later (after async operations) if we are in the same step we started in.
      const thisStepActiveIndex =
        guidedTourManager.activeDriverTask!.getActiveIndex()!;
      const nextStep = guidedTourTask.steps![thisStepActiveIndex + 1];
      if (!nextStep) {
        guidedTourManager.activeDriverTask!.moveNext();
        return;
      }
      const targetedElement = await util.shouldSwitchSteps(
        guidedTourTask,
        thisStepActiveIndex,
        guidedTourTask.steps![thisStepActiveIndex],
        nextStep,
        guidedTourManager,
      );
      if (targetedElement !== false) {
        bindReflexEvents(targetedElement, nextStep, guidedTourManager, () => {
          console.warn(
            "Calling callback for next step move (hopefully)",
            nextStep.order,
            guidedTourTask.steps![thisStepActiveIndex].order,
          );
          const thisStepActiveIndex2 =
            guidedTourManager.activeDriverTask!.getActiveIndex()!;
          const nextStep2 = guidedTourTask.steps![thisStepActiveIndex2 + 1];
          if (nextStep.path != nextStep2.path) {
            console.debug(
              "Setting the task step index prematurely to",
              guidedTourTask.steps!.indexOf(nextStep2),
            );
            StorageManager.setStorageKey(
              StorageManager.getTaskCurrentStepStorageKey(guidedTourTask),
              guidedTourTask.steps!.indexOf(nextStep2).toString(),
            );
          }
          // FIXME: This recursion should be guarded better, lest there be an infinite recursion.
          guidedTourManager
            .activeDriverTask!.getState()
            .popover!.nextButton.click();
        });
        guidedTourManager.activeDriverTask!.moveNext();
      }
      return;
    },
    onPrevClick: async () => {
      // Cache the current step index, so we can check later (after async operations) if we are in the same step we started in.
      const thisStepActiveIndex =
        guidedTourManager.activeDriverTask!.getActiveIndex()!;
      const prevStep = guidedTourTask.steps![thisStepActiveIndex - 1];
      if (!prevStep) {
        guidedTourManager.activeDriverTask!.movePrevious();
        return;
      }
      const targetedElement = await util.shouldSwitchSteps(
        guidedTourTask,
        thisStepActiveIndex,
        guidedTourTask.steps![thisStepActiveIndex],
        prevStep,
        guidedTourManager,
      );
      if (targetedElement !== false) {
        bindReflexEvents(targetedElement, prevStep, guidedTourManager, () => {
          console.warn("Calling callback for prev step move (hopefully)");
          const thisStepActiveIndex2 =
            guidedTourManager.activeDriverTask!.getActiveIndex()!;
          const nextStep2 = guidedTourTask.steps![thisStepActiveIndex2 - 1];
          if (prevStep.path != nextStep2.path) {
            console.debug(
              "Setting the task step index prematurely to",
              guidedTourTask.steps!.indexOf(prevStep),
            );
            StorageManager.setStorageKey(
              StorageManager.getTaskCurrentStepStorageKey(guidedTourTask),
              guidedTourTask.steps!.indexOf(prevStep).toString(),
            );
          }
          // FIXME: This recursion should be guarded better, lest there be an infinite recursion.
          guidedTourManager
            .activeDriverTask!.getState()
            .popover!.nextButton.click();
        });
        guidedTourManager.activeDriverTask!.movePrevious();
      }
      return;
    },
    // overlayClickBehavior: () => {},
  };
}

function convertToDriverStep(
  step: TourStep,
  guidedTourTask: TourTask,
): DriveStep {
  return {
    element: step.element,
    popover: {
      title: step.title ?? guidedTourTask.title,
      description: step.content,
    },
  };
}

function getDriverConfigForSteps(
  guidedTourTask: TourTask,
  guidedTourManager: DefaultGuidedTourManager,
) {
  if (!guidedTourTask.steps) {
    console.error("Task has no steps:", guidedTourTask);
    throw "Task has no steps";
  }
  console.log(guidedTourTask.steps);
  const config = XWikiDriverConfig(guidedTourManager, guidedTourTask);
  config.steps = guidedTourTask.steps!.map((step) =>
    convertToDriverStep(step, guidedTourTask),
  );
  return config;
}

function wrapTask(
  guidedTourTask: Driver,
  guidedTourManager: DefaultGuidedTourManager,
): Driver {
  const _drive = guidedTourTask.drive;
  // const _moveNext = guidedTourTask.moveNext;
  // const _movePrevious = guidedTourTask.movePrevious;
  const _destroy = guidedTourTask.destroy;
  guidedTourTask.drive = async function (stepIndex: number = 0) {
    StorageManager.setStorageKey(
      StorageManager.getTaskStepStorageStorageKey(
        guidedTourManager.activeTask!,
      ),
      JSON.stringify(guidedTourManager.activeTask!.steps!),
    );
    StorageManager.setStorageKey(
      StorageManager.getTaskCurrentStepStorageKey(
        guidedTourManager.activeTask!,
      ),
      stepIndex.toString(),
    );
    // TODO: Add bindReflexEvents call here.
    bindReflexEvents(
      await util.waitForElement(
        guidedTourManager.activeTask!.steps![stepIndex].element,
      ),
      guidedTourManager.activeTask!.steps![stepIndex],
      guidedTourManager,
    );
    _drive(stepIndex);
  }.bind(guidedTourTask);

  // guidedTourTask.moveNext = function () {
  //   /**
  //    * - Cache active task
  //    * - Compute expectPageRefresh
  //    * FIXME: Shouldn't this be in onNextClick() ?
  //    */
  //   let expectPageRefresh: boolean = false;
  //   // Cache the active task, since _moveNext() will destroy it if we're on the last step.
  //   const activeTask = guidedTourManager.activeTask!;
  //   // FIXME: This default should be changed maybe...
  //   const currentStepIndex =
  //     guidedTourManager.activeDriverTask?.getActiveIndex() ?? 1000000;
  //   const newStepIndex1: number = currentStepIndex + 1;
  //   if (newStepIndex1 >= activeTask.steps!.length) {
  //     // No next step to go to.
  //     expectPageRefresh = false;
  //   } else {
  //     const currentStep =
  //       guidedTourManager.activeTask!.steps![currentStepIndex];
  //     const newStep = guidedTourManager.activeTask!.steps![newStepIndex1];
  //     console.info("Comparing these paths:", currentStep, newStep);

  //     expectPageRefresh = currentStep.path != newStep.path;
  //   }
  //   console.debug(
  //     "newStepIndex",
  //     newStepIndex1,
  //     "expectPageRefresh",
  //     expectPageRefresh,
  //   );
  //   let newStepIndex;
  //   if (!expectPageRefresh) {
  //     // If the page is supposed to refresh/redirect, driver won't try to advance to the next step, but will instead
  //     // stay in place until the page reloads. The session storage key is still set, so we'll recover
  //     _moveNext();
  //     newStepIndex =
  //       guidedTourManager.activeDriverTask?.getActiveIndex() ?? undefined;
  //   } else {
  //     newStepIndex =
  //       guidedTourManager.activeDriverTask?.getActiveIndex() ?? undefined;
  //     newStepIndex = newStepIndex !== undefined ? newStepIndex + 1 : undefined;
  //     console.log(
  //       "Not advancing the tour since we expect a refresh. Session storage is being set to",
  //       newStepIndex,
  //     );
  //   }
  //   StorageManager.setStorageKey(
  //     StorageManager.getTaskCurrentStepStorageKey(activeTask),
  //     newStepIndex?.toString() ?? undefined,
  //   );
  // }.bind(guidedTourTask);

  // guidedTourTask.movePrevious = function () {
  //   let expectPageRefresh: boolean = false;
  //   // Cache the active task, since _moveNext() will destroy it if we're on the last step.
  //   const activeTask = guidedTourManager.activeTask!;
  //   // FIXME: This default should be changed maybe...
  //   const currentStepIndex =
  //     guidedTourManager.activeDriverTask?.getActiveIndex() ?? -1000000;
  //   let newStepIndex: number | undefined = currentStepIndex - 1;
  //   if (newStepIndex < 0) {
  //     // No prev step to go to.
  //     newStepIndex = undefined;
  //     expectPageRefresh = false;
  //   } else {
  //     const currentStep =
  //       guidedTourManager.activeTask!.steps![currentStepIndex];
  //     const newStep = guidedTourManager.activeTask!.steps![newStepIndex];
  //     console.info("Comparing these paths:");
  //     console.info(currentStep);
  //     console.info(newStep);

  //     expectPageRefresh = currentStep.path != newStep.path;
  //   }
  //   console.debug(
  //     "newStepIndex",
  //     newStepIndex,
  //     "expectPageRefresh",
  //     expectPageRefresh,
  //   );
  //   let newStep;
  //   if (!expectPageRefresh) {
  //     // If the page is supposed to refresh/redirect, driver won't try to advance to the next step, but will instead
  //     // stay in place until the page reloads. The session storage key is still set, so we'll recover
  //     _movePrevious();
  //     newStep =
  //       guidedTourManager.activeDriverTask?.getActiveIndex() ?? undefined;
  //   } else {
  //     newStep =
  //       guidedTourManager.activeDriverTask?.getActiveIndex() ?? undefined;
  //     newStep = newStep !== undefined ? newStep - 1 : undefined;
  //     console.log(
  //       "Not advancing the tour since we expect a refresh. Session storage is being set to",
  //       newStep,
  //     );
  //   }
  //   StorageManager.setStorageKey(
  //     StorageManager.getTaskCurrentStepStorageKey(activeTask),
  //     newStep?.toString() ?? undefined,
  //   );
  // }.bind(guidedTourTask);

  guidedTourTask.destroy = function () {
    const currentStep = guidedTourTask.getActiveIndex();
    // FIXME: Don't use guidedTourManager.activeTask! null assertion everywhere.
    console.info(
      `Trying to see if, on destroy, the guidedTourTask is actually done, or we're expecting a redirect`,
    );
    if (currentStep != guidedTourTask.getConfig().steps?.length) {
      StorageManager.setStorageKey(
        StorageManager.getTaskCurrentStepStorageKey(
          guidedTourManager.activeTask!,
        ),
        guidedTourTask.getActiveIndex()!.toString(),
      );
      guidedTourManager.setTaskStatus(
        guidedTourManager.activeTask!,
        TourTaskStatus.DONE,
      );
    } else {
      // Delete the current step storage key.
      StorageManager.setStorageKey(
        StorageManager.getTaskCurrentStepStorageKey(
          guidedTourManager.activeTask!,
        ),
        undefined,
      );
      // Clear the step cache.
      StorageManager.setStorageKey(
        StorageManager.getTaskStepStorageStorageKey(
          guidedTourManager.activeTask!,
        ),
        undefined,
      );
      guidedTourManager.setTaskStatus(
        guidedTourManager.activeTask!,
        TourTaskStatus.SKIPPED,
      );
    }
    _destroy();
  }.bind(guidedTourTask);

  return guidedTourTask;
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
  element: Element | undefined,
  step: TourStep,
  guidedTourManager: DefaultGuidedTourManager,
  callbackFn: () => void = () => {
    console.warn("Calling callback for prev step move (hopefully)");
    // FIXME: This recursion should be guarded better, lest there be an infinite recursion.
    guidedTourManager.activeDriverTask!.getState().popover!.nextButton.click();
  },
) {
  console.warn("Doing reflex bind");
  if (!step.reflex || element === undefined) {
    if (step.reflex && element === undefined) {
      console.warn("WARNING: reflex step with empty element:", step);
    }
    return;
  }
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

  // TODO: Check for unused translation strings at the end of development.

// FIXME: From old TourJS.xml
      // TODO: Check precondition for next step;
      // TODO: Check if the next step is on the right page (to account for href redirects, etc);
  // Helper to bind click events, TODO: could be deleted.
  function bindFloaterClickEvent(selector, callback) {
    $('.guidedtour-widget ' + selector).on('click', (event) => {
      callback(event);
    });
  };

  bindFloaterClickEvent('.top-bar', (event) => {
    window.localStorage.setItem('TourFloaterCollapsed', document.querySelector('.guidedtour-widget').classList.toggle('collapsed'));
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
