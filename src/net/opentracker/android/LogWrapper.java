package net.opentracker.android;

import android.util.Log;

/**
 * 
 * Wrapper for Androids logging mechanism.
 * 
 * While researching android's logging mechanics, logging seems to be hard to
 * turn off. This class is intended to turn it on or off easily.
 * 
 * Verbose should never be compiled into an application except during
 * development. Debug logs are compiled in but stripped at runtime. Error,
 * warning and info logs are always kept.
 * 
 * As per
 * http://stackoverflow.com/questions/2018263/android-logging/2019002#2019002
 * 
 * 
 * @author $Author: eddie $ (latest svn author)
 * @version $Id: LogWrapper.java 13603 2011-11-29 11:55:42Z eddie $
 */
public class LogWrapper {
    
    public enum LogLevel{ kVerbose, kDebug, kInfo, kWarn, kError; };

    /*
     * Common way is make a LogLevel variable, and define it as Debug
     * This will show all the logs of level Debug and up
     * 
     * By default this should be INFO or above.
     * 
     * Lower levels will give more details but will also clutter up the LogCat
     * output.
     */
    private static final LogLevel m_logLevel = LogLevel.kVerbose;

    public static void v(String tag, Object msg) {
        if (m_logLevel.compareTo(LogLevel.kVerbose) >= 0)
            Log.v(tag, msg.toString());
    }

    public static void d(String tag, Object msg) {
        if (m_logLevel.compareTo(LogLevel.kDebug) >= 0)
            Log.v(tag, msg.toString());
    }

    public static void i(String tag, Object msg) {
        if (m_logLevel.compareTo(LogLevel.kInfo) >= 0)
            Log.i(tag, msg.toString());
    }

    public static void w(String tag, Object msg) {
        if (m_logLevel.compareTo(LogLevel.kWarn) >= 0)
            Log.w(tag, msg.toString());
    }

    public static void e(String tag, Object msg) {
        if (m_logLevel.compareTo(LogLevel.kError) >= 0)
            Log.e(tag, msg.toString());
    }
}