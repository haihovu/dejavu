/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.dejavu.activefx;

import java.nio.ByteBuffer;
import java.nio.channels.SelectableChannel;
import org.dejavu.util.DjvLogMsg;
import org.dejavu.util.DjvSystem;

/**
 * Default no-op implementation of the AFX event handler interface.
 * @author Hai Vu
 */
public class AfxEventAdaptor implements AfxEventHandler {

	@Override
	public void openCompleted() {
		DjvSystem.logInfo(DjvLogMsg.Category.DESIGN, "Open completed " + this);
	}

	@Override
	public void closed() {
		DjvSystem.logInfo(DjvLogMsg.Category.DESIGN, "Closed " + this);
	}

	@Override
	public void readCompleted(ByteBuffer returnedBuffer) {
		DjvSystem.logInfo(DjvLogMsg.Category.DESIGN, "Read completed " + this);
	}

	@Override
	public void writeCompleted() {
		DjvSystem.logInfo(DjvLogMsg.Category.DESIGN, "Write completed " + this);
	}

	@Override
	public void acceptCompleted(AfxConnection newConnection) {
		DjvSystem.logInfo(DjvLogMsg.Category.DESIGN, this + " accept completed " + newConnection);
	}

	@Override
	public void acceptCompleted(SelectableChannel newChannel) {
		DjvSystem.logInfo(DjvLogMsg.Category.DESIGN, this + " accept completed " + newChannel);
	}

	@Override
	public void openFailed(String theCause) {
		DjvSystem.logWarning(DjvLogMsg.Category.DESIGN, this + " open failed " + theCause);
	}

	@Override
	public void readFailed() {
		DjvSystem.logWarning(DjvLogMsg.Category.DESIGN, "Read failed " + this);
	}

	@Override
	public void writeFailed() {
		DjvSystem.logWarning(DjvLogMsg.Category.DESIGN, "Write failed " + this);
	}
}
