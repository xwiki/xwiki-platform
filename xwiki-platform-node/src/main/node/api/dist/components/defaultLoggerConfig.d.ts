import { LoggerConfig } from '../api/loggerConfig';
/**
 * @since 0.1
 * @beta
 */
export declare class DefaultLoggerConfig implements LoggerConfig {
    protected config: Map<string, string>;
    protected computedConfig: Map<string, number>;
    protected defaultLevel: string;
    protected defaultLevelId: number;
    protected levels: Map<string, number>;
    constructor(defaultLevel?: string);
    addLevel(module: string, level: string): void;
    getLevels(): Map<string, string>;
    getLevel(module: string): string;
    getLevelId(level: string): number;
    setDefaultLevel(level: string): void;
    getDefaultLevel(): string;
    getDefaultLevelId(): number;
    hasLevel(module: string, level: string): boolean;
    hasLevelId(module: string, levelId: number): boolean;
}
