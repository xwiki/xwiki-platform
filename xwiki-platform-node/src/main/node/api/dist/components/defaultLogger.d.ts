import { Logger } from '../api/logger';
import { LoggerConfig } from '../api/loggerConfig';
/**
 * @since 0.1
 * @beta
 */
export declare class DefaultLogger implements Logger {
    readonly loggerConfig?: LoggerConfig | undefined;
    module: string;
    constructor(loggerConfig?: LoggerConfig | undefined);
    setModule(module: string): void;
    debug(...data: any[]): void;
    info(...data: any[]): void;
    warn(...data: any[]): void;
    error(...data: any[]): void;
}
