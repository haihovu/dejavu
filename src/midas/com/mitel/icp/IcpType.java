/*
 * IcpApi.java
 *
 * Created on April 28, 2004, 10:46 AM
 */

package com.mitel.icp;

import com.mitel.miutil.MiDateTime;

/**
 * Interface for accessing information from ICP's. Each type of ICP will provide
 * concrete implementation of this interface. I.e. there is one instance of
 * concrete implementation per ICP type, not per ICP system.
 * @author haiv
 */
public interface IcpType
{
	
	public abstract void enableFtp(IcpDescriptor icpDesc, IcpTypeListener listener);
	public abstract void enableCscDebug(IcpDescriptor icpDesc, IcpTypeListener listener);
	public abstract void disableCscDebug(IcpDescriptor icpDesc, IcpTypeListener listener);
	public abstract void printCscLog(int levelOfDetails, IcpDescriptor icpDesc, IcpTypeListener listener);
	public abstract void activateCscLog(int queueSize, IcpDescriptor icpDesc, IcpTypeListener listener);
	public abstract void deactivateCscLog(IcpDescriptor icpDesc, IcpTypeListener listener);
	public abstract void printCscMap(int levelOfDetails, IcpDescriptor icpDesc, IcpTypeListener listener);
	public abstract void printRscMap(int levelOfDetails, IcpDescriptor icpDesc, IcpTypeListener listener);
	public abstract MiDateTime getDateTime(IcpDescriptor icpDesc, IcpTypeListener listener);
	
	/**
	 * Retrieves the type info associated with this type.
	 * @return The type info, not null.
	 */
	public abstract IcpTypeInfo getTypeInfo();
	
	public abstract void cancelRequest();
	
	/**
	 * This method returns a runnable object, with which client may use to cancel
	 * previous requests sent to the ICP.
	 */	
	public abstract Runnable getRequestCancelRunnable();
	
	public abstract String compIdToString(java.lang.String compId);
		
	/**
	 * Interface for clients to implement if they wish to receive asynchronous
	 * responses from requests made to ICP's.
	 */	
	public static interface IcpTypeListener
	{
		public abstract void onGetDateTime(java.lang.String response, MiDateTime dateTime, boolean success, int percentDone);
		public abstract void onEnableFtp(java.lang.String response, boolean success, int percentDone);
		public abstract void onEnableCscDebug(java.lang.String response, boolean success, int percentDone);
		public abstract void onDisableCscDebug(java.lang.String response, boolean success, int percentDone);
		public abstract void onActivateCscLog(java.lang.String response, boolean success, int percentDone);
		public abstract void onDeactivateCscLog(java.lang.String response, boolean success, int percentDone);
		public abstract void onPrintCscLog(java.lang.String response, boolean success, int percentDone);
		public abstract void onPrintCscMap(java.lang.String response, boolean success, int percentDone);
		public abstract void onPrintRscMap(java.lang.String response, boolean success, int percentDone);
		
		public java.awt.Frame getFrame();
	}
	
}
