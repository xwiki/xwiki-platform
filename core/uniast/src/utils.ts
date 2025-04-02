/*
 * See the LICENSE file distributed with this work for additional
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

/**
 * Ensure a statement is unreachable
 *
 * @param value - the error value printed when this method is reached
 *
 * @since 0.16
 */
function assertUnreachable(value: never): never {
  console.error({ unreachable: value });
  throw new Error("Reached a theoretically unreachable statement");
}

/**
 * Assert that a value is in an array, and fix its type
 *
 * @since 0.16
 *
 * @param array - the array to check
 * @param value - the value expected in the array
 * @param message - a message displayed in case the value is not found in the array
 *
 * @returns -
 */
function assertInArray<T, U extends T>(
  value: T,
  array: U[],
  message: string,
): U {
  if (!array.includes(value as U)) {
    throw new Error(message + ": " + value);
  }

  return value as U;
}

/**
 * Get a function's output or `null` if thrown an error
 *
 * @since 0.16
 *
 * @param func - The function to try
 *
 * @returns -
 */
function tryFallible<T>(func: () => T): T | null {
  try {
    return func();
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
  } catch (e: unknown) {
    return null;
  }
}

export { assertInArray, assertUnreachable, tryFallible };
