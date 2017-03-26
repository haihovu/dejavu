/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dejavu.util;

/**
 * Utility class with some convenience functions for dealing with exceptions.
 *
 * @author haiv
 */
public class DjvExceptionUtil {

	private static final Object gLock = new Object();
	private static int gDefaultTraceDepth = 6;

	/**
	 * Appends a single trace to a string.
	 *
	 * @param ret The builder of the target string.
	 * @param trace The trace to be appended.
	 */
	private static void appendTrace(StringBuilder ret, StackTraceElement trace) {
		ret.append(trace.getClassName()).append(".").append(trace.getMethodName())
			.append("[").append(trace.getFileName()).append("(").append(trace.getLineNumber())
			.append(")").append("]");
	}

	private static void getCauseTrace(Throwable e, StringBuilder str, int depth) {
		if (e == null) {
			return;
		}

		str.append(", due to ");
		str.append(getCompressedTrace(e, depth));
		getCauseTrace(e.getCause(), str, depth);
	}

	public static void setDefaultTraceDepth(int newDepth) {
		synchronized (gLock) {
			gDefaultTraceDepth = newDepth;
		}
	}

	public static int getDefaultTraceDepth() {
		synchronized (gLock) {
			return gDefaultTraceDepth;
		}
	}

	/**
	 * Creates a standard simple warning message about an exception, with a
	 * compact stack trace using the default trace depth. For use with logging.
	 *
	 * @param ex The exception to be processed.
	 * @return The warning string about the given exception, typically something
	 * like this: "Encountered <i>exception string</i> at <i>compact trace</i>".
	 */
	public static String simpleTrace(Throwable ex) {
		return simpleTrace(ex, getDefaultTraceDepth());
	}

	/**
	 * Creates a standard simple warning message about an exception, with a
	 * compact trace. For use with logging.
	 *
	 * @param ex The exception to be processed.
	 * @param depth The stack trace depth to use in generating the message.
	 * @return The warning string about the given exception, typically something
	 * like this: "Encountered <i>exception string</i> at <i>compact trace</i>".
	 */
	private static String simpleTrace(Throwable ex, int depth) {
		StringBuilder ret = new StringBuilder(256).append("Encountered ").append(ex);
		ret.append(" at ").append(getCompressedTrace(ex, depth));
		getCauseTrace(ex.getCause(), ret, depth);
		return ret.toString();
	}

	/**
	 * Returns a compressed, human-readable trace representation of the given
	 * throwable, for logging purposes.
	 *
	 * @param theThrowable The target throwable.
	 * @return The compressed representation of the trace of the given
	 * throwable.
	 */
	public static String getCompressedTrace(Throwable theThrowable) {
		return getCompressedTrace(theThrowable, getDefaultTraceDepth());
	}

	/**
	 * Returns a compressed, human-readable trace representation of the given
	 * throwable, for logging purposes.
	 *
	 * @param theThrowable The target throwable.
	 * @param depth The maximum depth of the trace to be presented.
	 * @return The compressed representation of the trace of the given
	 * throwable.
	 */
	private static String getCompressedTrace(Throwable theThrowable, int depth) {
		StringBuilder ret = new StringBuilder(256).append(getCompressedTrace(theThrowable.getStackTrace(), depth));
		if (theThrowable.getCause() != null) {
			ret.append(" due to ").append(getCompressedTrace(theThrowable.getCause(), depth));
		}
		return ret.toString();
	}

	/**
	 * Returns a compressed, human-readable representation stack trace of the
	 * current thread, for logging purposes.
	 *
	 * @return The compressed representation of the given stack trace.
	 */
	public static String getCompressedTrace() {
		StackTraceElement[] traces = Thread.currentThread().getStackTrace();
		int skipTopTraces = 2;
		if (traces.length > skipTopTraces) {
			StackTraceElement[] actual = new StackTraceElement[traces.length - skipTopTraces];
			for (int i = 0; i < actual.length; ++i) {
				actual[i] = traces[i + skipTopTraces];
			}
			return getCompressedTrace(actual, getDefaultTraceDepth());
		}
		return "";
	}

	/**
	 * Returns a compressed, human-readable representation of the given stack
	 * trace, for logging purposes.
	 *
	 * @param traces The stack trace to be processed
	 * @param depth The maximum depth of the trace to be presented.
	 * @return The compressed representation of the given stack trace.
	 */
	private static String getCompressedTrace(StackTraceElement[] traces, int depth) {
		StringBuilder ret = new StringBuilder(256);
		int count = 0;
		if ((traces == null) || (traces.length < 1)) {
			return "no trace";
		}
		for (StackTraceElement trace : traces) {
			if (count++ > depth) {
				break;
			} else if (count > 1) {
				ret.append("<-");
			}
			appendTrace(ret, trace);
		}
		return ret.toString();
	}
}
