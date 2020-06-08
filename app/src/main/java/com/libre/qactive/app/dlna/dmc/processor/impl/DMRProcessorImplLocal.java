package com.libre.qactive.app.dlna.dmc.processor.impl;

import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.util.Log;


import com.libre.qactive.app.dlna.dmc.processor.interfaces.DMRProcessor;

import org.fourthline.cling.model.ModelUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class DMRProcessorImplLocal implements DMRProcessor {
	private static final String TAG = DMRProcessorImplLocal.class.getSimpleName();
	private static final int UPDATE_INTERVAL = 1000;
	private boolean m_isRunning = true;
	private MediaPlayer m_mediaPlayer;
	private AudioManager m_audioManager;
	private List<DMRProcessorListener> m_listeners;
	private Thread m_updateThread = new Thread(new Runnable() {

		@Override
		public void run() {
			while (m_isRunning) {
				try {
					if (m_mediaPlayer == null || !m_mediaPlayer.isPlaying()) {
						Thread.sleep(UPDATE_INTERVAL);
						continue;
					}
					
					int currentPostion = m_mediaPlayer.getCurrentPosition() / 1000;
					int duration = m_mediaPlayer.getDuration() / 1000;
					fireUpdatePositionEvent(currentPostion, duration);
					Thread.sleep(UPDATE_INTERVAL);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	});
	
	private OnCompletionListener mediaOnCompletionListener = new OnCompletionListener() {
		
		@Override
		public void onCompletion(MediaPlayer mp) {
			// TODO Auto-generated method stub
			fireOnPlayCompletedEvent();
		}
	};
	
	private OnErrorListener mediaOnErrorListener = new OnErrorListener() {
		
		@Override
		public boolean onError(MediaPlayer mp, int what, int extra) {
			// TODO Auto-generated method stub
			if (what == MediaPlayer.MEDIA_ERROR_SERVER_DIED) {
				Log.e(TAG, "Media Error, Server Died " + extra);
			} else if (what == MediaPlayer.MEDIA_ERROR_UNKNOWN) {
				Log.e(TAG, "Media Error, Error Unknown " + extra);
			}
			return false;
		}
	};
	
	private OnPreparedListener mediaOnPreparedListener = new OnPreparedListener() {
		
		@Override
		public void onPrepared(MediaPlayer mp) {
			// TODO Auto-generated method stub
			int position = mp.getCurrentPosition() / 1000;
			int duration = mp.getDuration() / 1000;
			fireUpdatePositionEvent(position, duration);
			
			mp.start();
			fireOnPlayingEvent();
		}
	};
	
	public DMRProcessorImplLocal(AudioManager audioManager) {
		m_mediaPlayer = new MediaPlayer();
		m_mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
		m_mediaPlayer.setOnCompletionListener(mediaOnCompletionListener);
		m_mediaPlayer.setOnErrorListener(mediaOnErrorListener);
		m_mediaPlayer.setOnPreparedListener(mediaOnPreparedListener);
		
		m_audioManager = audioManager;
		m_listeners = new ArrayList<DMRProcessor.DMRProcessorListener>();
		m_updateThread.start();
	}

	@Override
	public void setURI(String uri, String musicInfo) {
		// TODO Auto-generated method stub
		try {
			fireOnSetURIEvent();
			m_mediaPlayer.reset();
			m_mediaPlayer.setDataSource(uri);
			m_mediaPlayer.prepareAsync();
		} catch (IllegalArgumentException e) {
			Log.e(TAG, "play on local failed, illegal url");
		} catch (SecurityException e) {
			Log.e(TAG, "play on local failed, security issue");
		} catch (IllegalStateException e) {
			Log.e(TAG, "play on local failed, illegal device state");
		} catch (IOException e) {
			Log.e(TAG, "play on local failed, io exception");
		}
	}

	@Override
	public void play() {
		// TODO Auto-generated method stub
		m_mediaPlayer.start();
	}

	@Override
	public void pause() {
		// TODO Auto-generated method stub
		m_mediaPlayer.pause();
	}

	@Override
	public void stop() {
		// TODO Auto-generated method stub
		m_mediaPlayer.stop();
	}

	@Override
	public void seek(String position) {
		// TODO Auto-generated method stub
		seek(ModelUtil.fromTimeString(position));
	}

	@Override
	public void seek(long position) {
		// TODO Auto-generated method stub
		m_mediaPlayer.seekTo((int) (position * 1000));
	}

	@Override
	public void setVolume(int newVolume) {
		// TODO Auto-generated method stub
		m_audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVolume, 0);
	}

	@Override
	public void addListener(DMRProcessorListener listener) {
		// TODO Auto-generated method stub
		synchronized (m_listeners) {
			m_listeners.add(listener);
		}
	}

	@Override
	public void removeListener(DMRProcessorListener listener) {
		// TODO Auto-generated method stub
		synchronized (m_listeners) {
			m_listeners.remove(listener);
		}
	}

	@Override
	public void dispose() {
		// TODO Auto-generated method stub
		m_isRunning = false;
		m_mediaPlayer.release();
	}

	@Override
	public void reset() {
		// TODO Auto-generated method stub
		m_mediaPlayer.reset();
		fireOnPausedEvent();
	}

	@Override
	public int getVolume() {
		// TODO Auto-generated method stub
		return m_audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
	}

	@Override
	public int getMaxVolume() {
		// TODO Auto-generated method stub
		return m_audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
	}

	private void fireUpdatePositionEvent(long position, long duration) {
		synchronized (m_listeners) {
			for (DMRProcessorListener listener : m_listeners) {
				listener.onUpdatePosition(position, duration);
			}
		}
	}

	
	
	@SuppressWarnings("unused")
	private void fireUpdateVolumeEvent(int currentVolume) {
		// TODO Auto-generated method stub
		synchronized (m_listeners) {
			for (DMRProcessorListener listener : m_listeners) {
				listener.onUpdateVolume(currentVolume);
			}
		}
	}

	@SuppressWarnings("unused")
	private void fireOnStopedEvent() {
		synchronized (m_listeners) {
			for (DMRProcessorListener listener : m_listeners) {
				listener.onStoped();
			}
		}
	}

	private void fireOnPausedEvent() {
		synchronized (m_listeners) {
			for (DMRProcessorListener listener : m_listeners) {
				listener.onPaused();
			}
		}
	}

	private void fireOnPlayingEvent() {
		synchronized (m_listeners) {
			for (DMRProcessorListener listener : m_listeners) {
				listener.onPlaying();
			}
		}
	}
	
	private void fireOnPlayCompletedEvent() {
		synchronized (m_listeners) {
			for (DMRProcessorListener listener : m_listeners) {
				listener.onPlayCompleted();
			}
		}
	}
	
	private void fireOnSetURIEvent() {
		synchronized (m_listeners) {
			for (DMRProcessorListener listener : m_listeners) {
				listener.onSetURI();
			}
		}
	}

}
