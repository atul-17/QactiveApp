package com.libre.qactive.app.dlna.dmc.processor.impl;

import android.util.Log;


import com.libre.qactive.app.dlna.dmc.processor.interfaces.DMSProcessor;

import org.fourthline.cling.controlpoint.ActionCallback;
import org.fourthline.cling.controlpoint.ControlPoint;
import org.fourthline.cling.model.action.ActionInvocation;
import org.fourthline.cling.model.message.UpnpResponse;
import org.fourthline.cling.model.meta.Action;
import org.fourthline.cling.model.meta.Device;
import org.fourthline.cling.model.meta.Service;
import org.fourthline.cling.model.types.ServiceType;
import org.fourthline.cling.model.types.UnsignedIntegerFourBytes;
import org.fourthline.cling.support.contentdirectory.DIDLParser;
import org.fourthline.cling.support.model.DIDLContent;
import org.fourthline.cling.support.model.DIDLObject;
import org.fourthline.cling.support.model.container.Container;
import org.fourthline.cling.support.model.item.Item;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("rawtypes")
public class DMSProcessorImpl implements DMSProcessor {

	private static final String TAG = DMSProcessorImpl.class.getName();
	private Device m_server;
	private ControlPoint m_controlPoint;
	private List<DMSProcessorListener> m_listeners;

	public DMSProcessorImpl(Device device, ControlPoint controlPoint) {
		
		m_server = device;
		m_controlPoint = controlPoint;
		m_listeners = new ArrayList<DMSProcessor.DMSProcessorListener>();
	}

	@SuppressWarnings("unchecked")
	public synchronized void browse(final String objectID) {
		Service cds = m_server.findService(new ServiceType(UpnpProcessorImpl.DMS_NAMESPACE, "ContentDirectory"));

		if (cds != null) {
			final Action action = cds.getAction("Browse");
			ActionInvocation actionInvocation = new ActionInvocation(action);
			actionInvocation.setInput("ObjectID", objectID);
			actionInvocation.setInput("BrowseFlag", "BrowseDirectChildren");
			actionInvocation.setInput("Filter", "*");
			actionInvocation.setInput("StartingIndex", new UnsignedIntegerFourBytes(0));
			actionInvocation.setInput("RequestedCount", new UnsignedIntegerFourBytes(999));
			actionInvocation.setInput("SortCriteria", null);
			ActionCallback actionCallback = new ActionCallback(actionInvocation) {

				@Override
				public void success(ActionInvocation invocation) {
					Log.d(TAG, invocation.getOutput("Result").toString());

					try {
						DIDLParser parser = new DIDLParser();
						DIDLContent content = parser.parse(invocation.getOutput("Result").toString());

						HashMap<String, List<? extends DIDLObject>> m_result = new HashMap<>();

						for (Container container : content.getContainers()) {
							Log.d(TAG, "Container: " + container.getTitle() + ", Id is =" + container.getId());
						}
						m_result.put("Containers", content.getContainers());

						for (Item item : content.getItems()) {
							item.setRefID(objectID);
							Log.d(TAG, "Item: " + item.getTitle()+", setRefID = "+objectID);
						}

						m_result.put("Items", content.getItems());

						fireOnBrowseCompleteEvent(objectID,m_result);

					} catch (Exception e) {
						e.printStackTrace();
						Log.e(TAG, e.getMessage());
					}
				}

				@Override
				public void failure(ActionInvocation invocation, UpnpResponse operation, String defaultMsg) {
					Log.e(TAG, defaultMsg);
					fireOnBrowseFailEvent(defaultMsg);
				}

			};

			m_controlPoint.execute(actionCallback);
		}
	}

	public void dispose() {

	}

	@Override
	public void addListener(DMSProcessorListener listener) {
		synchronized (m_listeners) {
			m_listeners.add(listener);
		}

	}

	@Override
	public void removeListener(DMSProcessorListener listener) {
		synchronized (m_listeners) {
			m_listeners.remove(listener);
		}
	}

	private void fireOnBrowseCompleteEvent(String parentObjectId, HashMap<String, List<? extends DIDLObject>> m_result) {
		synchronized (m_listeners) {
			for (DMSProcessorListener listener : m_listeners) {
				listener.onBrowseComplete(parentObjectId,m_result);
			}
		}
	}

	private void fireOnBrowseFailEvent(String message) {
		synchronized (m_listeners) {
			for (DMSProcessorListener listener : m_listeners) {
				listener.onBrowseFail(message);
			}
		}
	}
}
