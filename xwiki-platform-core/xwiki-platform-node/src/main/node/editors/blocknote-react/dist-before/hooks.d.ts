import { EditorType } from './blocknote';
declare function useEditor(): EditorType;
/**
 * Check if a specified number of milliseconds passed since the hook's initial call
 *
 * @param milliseconds - The number of milliseconds to wait
 *
 * @returns Whether the delay is expired
 */
declare function useTimeoutCheck(milliseconds: number): boolean;
export { useEditor, useTimeoutCheck };
