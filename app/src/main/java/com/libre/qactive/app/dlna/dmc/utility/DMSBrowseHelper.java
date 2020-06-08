	package com.libre.qactive.app.dlna.dmc.utility;




import com.libre.qactive.app.dlna.dmc.server.ContentTree;
import com.libre.qactive.app.dlna.dmc.utility.UpnpDeviceManager;

import org.fourthline.cling.model.meta.Device;
import org.fourthline.cling.support.model.DIDLObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import static com.cumulations.libreV2.activity.CTNowPlayingActivity.REPEAT_ALL;
import static com.cumulations.libreV2.activity.CTNowPlayingActivity.REPEAT_OFF;

	public class DMSBrowseHelper implements Cloneable {
	
	@SuppressWarnings("rawtypes")
	public Device getDevice(UpnpDeviceManager deviceManager) {
		if (isLocalDevice) {
			if (deviceManager.getLocalDmsMap().containsKey(deviceUdn)) {
				return deviceManager.getLocalDmsMap().get(deviceUdn);
			}
			return null;
		}
		
		if (deviceManager.getRemoteDmsMap().containsKey(deviceUdn)) {
			return deviceManager.getRemoteDmsMap().get(deviceUdn);
		}
		
		return null;
	}

	private boolean isLocalDevice;
	public boolean isLocalDevice() {
		return isLocalDevice;
	}

	private String deviceUdn;
	public String getDeviceUdn() {
		return deviceUdn;
	}

	private List<DIDLObject> didlList;
	private int adapterPosition = 0;
	public void saveDidlListAndPosition(List<DIDLObject> list, int position) {
		didlList = list;
		adapterPosition = position;
	}

	public DIDLObject getNextDIDLObject(int repeatState){

		if (repeatState== REPEAT_ALL ){

			int mNewAdapterPosition = (adapterPosition+1) % didlList.size();
			if (didlList != null &&
					mNewAdapterPosition >= 0 &&
					mNewAdapterPosition < didlList.size()) {

				return didlList.get(mNewAdapterPosition);

			}

		}else if (repeatState== REPEAT_OFF){

			int mNewAdapterPosition = adapterPosition+1;
			if (didlList != null &&
					mNewAdapterPosition > 0 &&
					mNewAdapterPosition < didlList.size()) {

				return didlList.get(mNewAdapterPosition);

			}
		}

		return null;
	}
	
	public DIDLObject getDIDLObject() {
		if (didlList != null && 
			adapterPosition >= 0 && 
			adapterPosition < didlList.size()) {
			return didlList.get(adapterPosition);
		}
		
		return null;
	}
	
	public int getAdapterPosition() {
		return adapterPosition;
	}

	public void setAdapterPosition(int adapterPosition) {
		this.scrollPosition += adapterPosition - this.adapterPosition;
		if (this.scrollPosition < 0) {
			this.scrollPosition = 0;
		} else if (this.scrollPosition >= didlList.size()) {
			this.scrollPosition = didlList.size() - 1;
		}
		this.adapterPosition = adapterPosition;
	}

	public List<DIDLObject> getDidlList() {
		return didlList;
	}

	public void setDidlList(List<DIDLObject> didlList) {
		this.didlList = didlList;
	}
	
	public DIDLObject getDIDLObject(int position) {
		if (didlList != null && 
			position >= 0 && 
			position < didlList.size()) {
			return didlList.get(position);
		}
		
		return null;
	}
	
	private Stack<DIDLObject> browseObjectStack;
	public Stack<DIDLObject> getBrowseObjectStack() {
		return browseObjectStack;
	}

	public void setBrowseObjectStack(Stack<DIDLObject> browseObjectStack) {
		this.browseObjectStack = browseObjectStack;
	}

	private int scrollPosition;
	public int getScrollPosition() {
		return scrollPosition;
	}

	public void setScrollPosition(int scrollPosition) {
		this.scrollPosition = scrollPosition;
	}

	public DMSBrowseHelper(boolean isLocal, String udn) {
		// TODO Auto-generated constructor stub
		isLocalDevice = isLocal;
		deviceUdn = udn;
		browseObjectStack = new Stack<DIDLObject>();
		browseObjectStack.push(ContentTree.getNode(ContentTree.ROOT_ID).getContainer());
		scrollPosition = 0;
	}
	
	public DMSBrowseHelper clone() {
		DMSBrowseHelper cloneObj = new DMSBrowseHelper(isLocalDevice, deviceUdn);

		List<DIDLObject> cloneList = new ArrayList<DIDLObject>(didlList);
		
		cloneObj.saveDidlListAndPosition(cloneList, adapterPosition);
		cloneObj.setBrowseObjectStack(browseObjectStack);
		cloneObj.setScrollPosition(scrollPosition);
		return cloneObj;
	}

	public boolean checkIfTheNextURLIsTheLastSong() {
		if(adapterPosition+2 >= didlList.size())
			return true;
		else
			return false;
	}
}
