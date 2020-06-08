package com.libre.qactive.app.dlna.dmc.utility;


import android.media.AudioManager;


import com.libre.qactive.LibreApplication;
import com.libre.qactive.app.dlna.dmc.processor.impl.DMRProcessorImpl;
import com.libre.qactive.app.dlna.dmc.processor.impl.DMRProcessorImplLocal;
import com.libre.qactive.app.dlna.dmc.processor.interfaces.DMRProcessor;


import org.fourthline.cling.controlpoint.ControlPoint;
import org.fourthline.cling.model.meta.RemoteDevice;
import org.fourthline.cling.model.meta.RemoteService;
import org.fourthline.cling.model.types.ServiceType;

public class DMRControlHelper {
	public final static String SERVICE_NAMESPACE = "schemas-upnp-org";
	public final static String SERVICE_AVTRANSPORT_TYPE = "AVTransport";
	private boolean isLocalDevice;
	private String deviceUdn;
	
	private RemoteDevice deviceMeta;
	private DMRProcessor dmrProcessor;
	private ControlPoint controlPoint;

	public DMRProcessor getDmrProcessor() {
		return dmrProcessor;
	}

	public boolean isLocalDevice() {
		return isLocalDevice;
	}

	public String getDeviceUdn() {
		return deviceUdn;
	}

	
	public RemoteDevice getDeviceMeta() {
		return deviceMeta;
	}

	public DMRControlHelper(String udn, ControlPoint cpoint, RemoteDevice remoteDevice, RemoteService remoteService) {
		isLocalDevice = false;
		deviceUdn = udn;
		controlPoint = cpoint;
		//deviceSocket = DevUtil.getDev(deviceUdn);
		deviceMeta = remoteDevice;
		dmrProcessor = new DMRProcessorImpl(remoteDevice,remoteService, controlPoint);

	}
	
	public DMRControlHelper(AudioManager audioManager) {
		isLocalDevice = true;
		deviceUdn = LibreApplication.LOCAL_UDN;
		
		dmrProcessor = new DMRProcessorImplLocal(audioManager);
	}
	
	public String getDmrDisplayName() {
		
		return deviceUdn;
		
	}
	
	public void refresh() {
		// local device no need to refresh
		if (isLocalDevice) return;


		deviceMeta = UpnpDeviceManager.getInstance().getRemoteDmrMap().get(deviceUdn);
		RemoteService service = deviceMeta.findService(new ServiceType(SERVICE_NAMESPACE, SERVICE_AVTRANSPORT_TYPE));
		if (service != null) {
			dmrProcessor.reset();
			dmrProcessor = new DMRProcessorImpl(deviceMeta, service,controlPoint);
		}
	}
}
