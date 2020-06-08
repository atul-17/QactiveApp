package com.libre.qactive.app.dlna.dmc.processor.interfaces;


import org.fourthline.cling.model.message.UpnpResponse;
import org.fourthline.cling.model.meta.Action;

public interface DMRProcessor {
	void setURI(String uri, String musicinfo);

	void play();

	void pause();

	void stop();
	
	void reset();

	void seek(String position);
	
	void seek(long position);

	void setVolume(int newVolume);
	
	int getVolume();
	
	int getMaxVolume();

	void addListener(DMRProcessorListener listener);

	void removeListener(DMRProcessorListener listener);
	
	void dispose();

	interface DMRProcessorListener {
		void onUpdatePosition(long position, long duration);
		
		void onUpdateVolume(int currentVolume);

		void onPaused();

		void onStoped();
		
		void onSetURI();
		
		void onPlayCompleted();

		void onPlaying();

		void onActionSuccess(Action action);

		@SuppressWarnings("rawtypes")
		void onActionFail(String actionCallback, UpnpResponse response, final String cause);

		@SuppressWarnings("rawtypes")
		void onExceptionHappend(Action actionCallback, String mTitle, final String cause);
	}

}
