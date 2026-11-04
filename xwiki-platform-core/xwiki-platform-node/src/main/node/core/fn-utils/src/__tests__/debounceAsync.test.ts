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

import { debounceAsync } from "..";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";

describe("debounceAsync", () => {
  beforeEach(() => {
    vi.useFakeTimers();
  });

  afterEach(() => {
    vi.useRealTimers();
  });

  it("calls the function only once the delay elapsed", async () => {
    const fn = vi.fn(async (value: string) => value.toUpperCase());
    const debounced = debounceAsync(fn, 50);

    const result = debounced("a");

    await vi.advanceTimersByTimeAsync(49);
    expect(fn).not.toHaveBeenCalled();

    await vi.advanceTimersByTimeAsync(1);
    expect(fn).toHaveBeenCalledExactlyOnceWith("a");
    await expect(result).resolves.toBe("A");
  });

  it("waits 100 milliseconds by default", async () => {
    const fn = vi.fn(async () => "done");
    const debounced = debounceAsync(fn);

    const result = debounced();

    await vi.advanceTimersByTimeAsync(99);
    expect(fn).not.toHaveBeenCalled();

    await vi.advanceTimersByTimeAsync(1);
    await expect(result).resolves.toBe("done");
  });

  // eslint-disable-next-line max-statements
  it("merges the calls made while waiting into a single call with the last arguments", async () => {
    const fn = vi.fn(async (value: string) => value.toUpperCase());
    const debounced = debounceAsync(fn, 50);

    const first = debounced("a");
    await vi.advanceTimersByTimeAsync(30);
    const second = debounced("ab");
    await vi.advanceTimersByTimeAsync(30);
    const third = debounced("abc");

    // The delay restarts with each call.
    await vi.advanceTimersByTimeAsync(49);
    expect(fn).not.toHaveBeenCalled();

    await vi.advanceTimersByTimeAsync(1);
    expect(fn).toHaveBeenCalledExactlyOnceWith("abc");

    // The superseded calls aren't rejected: they all settle with the result of the single call.
    await expect(first).resolves.toBe("ABC");
    await expect(second).resolves.toBe("ABC");
    await expect(third).resolves.toBe("ABC");
  });

  it("rejects all the merged calls when the function fails", async () => {
    const error = new Error("Failure");
    const fn = vi.fn(async () => {
      throw error;
    });
    const debounced = debounceAsync(fn, 50);

    const first = debounced();
    const second = debounced();
    // Handled before the timers run, so that the rejections aren't reported as unhandled.
    const outcomes = Promise.allSettled([first, second]);

    await vi.advanceTimersByTimeAsync(50);

    expect(fn).toHaveBeenCalledOnce();
    expect(await outcomes).toEqual([
      { status: "rejected", reason: error },
      { status: "rejected", reason: error },
    ]);
  });

  it("supports functions that don't return a promise", async () => {
    const debounced = debounceAsync((a: number, b: number) => a + b, 50);

    const result = debounced(1, 2);
    await vi.advanceTimersByTimeAsync(50);

    await expect(result).resolves.toBe(3);
  });

  // eslint-disable-next-line max-statements
  it("starts a new call for the calls made while the function is running", async () => {
    let finishFirstCall: (value: string) => void = () => {};
    const fn = vi
      .fn<(value: string) => Promise<string>>()
      .mockImplementationOnce(
        () =>
          new Promise((resolve) => {
            finishFirstCall = resolve;
          }),
      )
      .mockImplementationOnce(async (value) => value.toUpperCase());
    const debounced = debounceAsync(fn, 50);

    const first = debounced("a");
    await vi.advanceTimersByTimeAsync(50);
    expect(fn).toHaveBeenCalledExactlyOnceWith("a");

    // The first call is still running.
    const second = debounced("b");
    const third = debounced("c");
    await vi.advanceTimersByTimeAsync(50);
    expect(fn).toHaveBeenCalledTimes(2);
    expect(fn).toHaveBeenLastCalledWith("c");

    finishFirstCall("first");
    await expect(first).resolves.toBe("first");
    await expect(second).resolves.toBe("C");
    await expect(third).resolves.toBe("C");
  });
});
