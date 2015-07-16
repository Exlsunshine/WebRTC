package com.conversation;

import java.io.Serializable;
import java.util.ArrayList;

import android.app.Fragment;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.example.testvideo2.MainActivity;
import com.example.testvideo2.R;
import com.quickblox.videochat.webrtc.QBRTCSession;
import com.quickblox.videochat.webrtc.QBRTCTypes;

public class ConversationFragment extends Fragment implements Serializable
{
	private static final String TAG = "ConversationFragment______";
	private TextView opponentNumber;
	private TextView connectionStatus;
	private ImageView opponentAvatar;
	private ToggleButton cameraToggle;
	private ToggleButton switchCameraToggle;
	private ToggleButton dynamicToggleVideoCall;
	private ToggleButton micToggleVideoCall;
	private ImageButton handUpVideoCall;
	private ImageView imgMyCameraOff;
	private TextView incUserName;
    private View view;
    
    private MediaPlayer ringtone;

	private LayoutInflater inflater;
	private ViewGroup container;
	private ArrayList<Integer> opponents;
	private int qbConferenceType;
	private int startReason;
	private String sessionID;
	private String callerName;
	private View localVideoView;
	private View remoteVideoView;
	private LinearLayout actionVideoButtonsLayout;
	private LinearLayout noVideoImageContainer;
	private CameraState cameraState = CameraState.NONE;
	private boolean isAudioEnabled = true;

	private AudioStreamReceiver audioStreamReceiver;
	private IntentFilter intentFilter;
	private boolean isMessageProcessed;
    
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
	{
		view = inflater.inflate(R.layout.fragment_conversation, container, false);
		this.inflater = inflater;
		this.container = container;
		Log.d(TAG, "Fragment. Thread id: " + Thread.currentThread().getId());
		
		if (getArguments() != null)
		{
			opponents = getArguments().getIntegerArrayList("opponents");
			qbConferenceType = getArguments().getInt("conference_type");
			startReason = getArguments().getInt(MainActivity.START_CONVERSATION_REASON);
			sessionID = getArguments().getString(MainActivity.SESSION_ID);
			callerName = getArguments().getString(MainActivity.CALLER_NAME);
			Log.d(TAG, "CALLER_NAME: " + callerName);
		}
		
		initViews(view);
		initButtonsListener();
		setUpUiByCallType(qbConferenceType);
		
		return view;
	}

	private void setUpUiByCallType(int qbConferenceType) 
	{
		if (qbConferenceType == QBRTCTypes.QBConferenceType.QB_CONFERENCE_TYPE_AUDIO.getValue())
		{
			cameraToggle.setVisibility(View.GONE);
			switchCameraToggle.setVisibility(View.INVISIBLE);

			localVideoView.setVisibility(View.INVISIBLE);
			remoteVideoView.setVisibility(View.INVISIBLE);

			imgMyCameraOff.setVisibility(View.INVISIBLE);
		}
	}
	
	private void initViews(View view) 
	{
		localVideoView = view.findViewById(R.id.localVideoView);
		remoteVideoView = view.findViewById(R.id.remoteVideoView);

		cameraToggle = (ToggleButton) view.findViewById(R.id.cameraToggle);
		switchCameraToggle = (ToggleButton) view.findViewById(R.id.switchCameraToggle);
		dynamicToggleVideoCall = (ToggleButton) view.findViewById(R.id.dynamicToggleVideoCall);
		micToggleVideoCall = (ToggleButton) view.findViewById(R.id.micToggleVideoCall);

		actionVideoButtonsLayout = (LinearLayout) view.findViewById(R.id.element_set_video_buttons);

		handUpVideoCall = (ImageButton) view.findViewById(R.id.handUpVideoCall);
		incUserName = (TextView) view.findViewById(R.id.incUserName);
		incUserName.setText(callerName);

		noVideoImageContainer = (LinearLayout) view.findViewById(R.id.noVideoImageContainer);
		imgMyCameraOff = (ImageView) view.findViewById(R.id.imgMyCameraOff);

		actionButtonsEnabled(false);
	}
	
	private void toggleCamera(boolean isNeedEnableCam)
	{
		// temporary insertion will be removed when GLVideoView will be fixed
		DisplayMetrics displaymetrics = new DisplayMetrics();
		displaymetrics.setToDefaults();

		ViewGroup.LayoutParams layoutParams = imgMyCameraOff.getLayoutParams();

		layoutParams.height = localVideoView.getHeight();
		layoutParams.width = localVideoView.getWidth();

		imgMyCameraOff.setLayoutParams(layoutParams);

		Log.d(TAG, "Width is: " + imgMyCameraOff.getLayoutParams().width
				+ " height is:" + imgMyCameraOff.getLayoutParams().height);

		if (((MainActivity) getActivity()).getCurrentSession() != null)
		{
			((MainActivity) getActivity()).getCurrentSession().setVideoEnabled(isNeedEnableCam);
			cameraToggle.setChecked(isNeedEnableCam);

			if (isNeedEnableCam) 
			{
				Log.d(TAG, "Camera is on!");
				switchCameraToggle.setVisibility(View.VISIBLE);
				imgMyCameraOff.setVisibility(View.INVISIBLE);
			} 
			else 
			{
				Log.d(TAG, "Camera is off!");
				switchCameraToggle.setVisibility(View.INVISIBLE);
				imgMyCameraOff.setVisibility(View.VISIBLE);
			}
		}
	}
	
	private enum CameraState 
	{
		NONE, DISABLED_FROM_USER, ENABLED_FROM_USER
	}
	
	private void initButtonsListener() 
	{
		switchCameraToggle.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				if (((MainActivity) getActivity()).getCurrentSession() != null) 
				{
					((MainActivity) getActivity()).getCurrentSession().switchCapturePosition(new Runnable() 
					{
						@Override
						public void run() 
						{
							Log.i(TAG, "Camera was switched.");
						}
					});
					Log.d(TAG, "Camera was switched!");
				}
			}
		});

		cameraToggle.setOnClickListener(new View.OnClickListener() 
		{
			@Override
			public void onClick(View v) 
			{
				if (cameraState != CameraState.DISABLED_FROM_USER)
				{
					toggleCamera(false);
					cameraState = CameraState.DISABLED_FROM_USER;
				} 
				else
				{
					toggleCamera(true);
					cameraState = CameraState.ENABLED_FROM_USER;
				}
			}
		});

		dynamicToggleVideoCall.setOnClickListener(new View.OnClickListener() 
		{
			@Override
			public void onClick(View v) 
			{
				if (((MainActivity) getActivity()).getCurrentSession() != null)
				{
					Log.d(TAG, "Dynamic switched!");
					((MainActivity) getActivity()).getCurrentSession().switchAudioOutput();
				}
			}
		});

		micToggleVideoCall.setOnClickListener(new View.OnClickListener()
		{
			@Override
			public void onClick(View v) 
			{
				if (((MainActivity) getActivity()).getCurrentSession() != null)
				{
					if (isAudioEnabled) 
					{
						Log.d(TAG, "Mic is off!");
						((MainActivity) getActivity()).getCurrentSession().setAudioEnabled(false);
						isAudioEnabled = false;
					} 
					else
					{
						Log.d(TAG, "Mic is on!");
						((MainActivity) getActivity()).getCurrentSession().setAudioEnabled(true);
						isAudioEnabled = true;
					}
				}
			}
		});

		handUpVideoCall.setOnClickListener(new View.OnClickListener() 
		{
			@Override
			public void onClick(View v)
			{
				stopOutBeep();
				actionButtonsEnabled(false);
				handUpVideoCall.setEnabled(false);
				Log.d(TAG, "Call is stopped");

				((MainActivity) getActivity()).hangUpCurrentSession();
				handUpVideoCall.setEnabled(false);
				handUpVideoCall.setActivated(false);
			}
		});
	}
	
	public void actionButtonsEnabled(boolean enability) 
	{
        cameraToggle.setEnabled(enability);
        switchCameraToggle.setEnabled(enability);
        imgMyCameraOff.setEnabled(enability);
        micToggleVideoCall.setEnabled(enability);
        dynamicToggleVideoCall.setEnabled(enability);
        cameraToggle.setActivated(enability);
        switchCameraToggle.setActivated(enability);
        imgMyCameraOff.setActivated(enability);
        micToggleVideoCall.setActivated(enability);
        dynamicToggleVideoCall.setActivated(enability);
    }
	
	public void stopOutBeep()
	{
		if (ringtone != null) 
		{
			try 
			{
                ringtone.stop();
            } catch (IllegalStateException e) {
                e.printStackTrace();
            } catch (Exception e) {
                e.printStackTrace();
            }
            
			ringtone.release();
            ringtone = null;
        }
    }
	
	@Override
	public void onCreate(Bundle savedInstanceState) 
	{
		Log.d(TAG, "onCreate() from " + TAG);
		super.onCreate(savedInstanceState);

		intentFilter = new IntentFilter();
		intentFilter.addAction(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED);

		audioStreamReceiver = new AudioStreamReceiver();
	}
	
	@Override
	public void onStart() 
	{
		super.onStart();
		
		getActivity().registerReceiver(audioStreamReceiver, intentFilter);

		super.onStart();
		QBRTCSession session = ((MainActivity) getActivity()).getCurrentSession();
		if (!isMessageProcessed) 
		{
			if (startReason == StartConversetionReason.INCOME_CALL_FOR_ACCEPTION.ordinal()) 
				session.acceptCall(session.getUserInfo());
			else
			{
				session.startCall(session.getUserInfo());
				startOutBeep();
			}
			isMessageProcessed = true;
		}
	}
	
	private void startOutBeep()
	{
		ringtone = MediaPlayer.create(getActivity(), R.raw.beep);
		ringtone.setLooping(true);
		ringtone.start();

	}
	
	private class AudioStreamReceiver extends BroadcastReceiver
	{
		@Override
		public void onReceive(Context context, Intent intent) 
		{
			/*
			 * if (intent.getAction().equals(AudioManager.ACTION_HEADSET_PLUG))
			 * { Log.d(TAG, "ACTION_HEADSET_PLUG " + intent.getIntExtra("state",
			 * -1)); } else
			 */if (intent.getAction().equals(AudioManager.ACTION_SCO_AUDIO_STATE_UPDATED))
			 {
				 Log.d(TAG, "ACTION_SCO_AUDIO_STATE_UPDATED "
						 + intent.getIntExtra("EXTRA_SCO_AUDIO_STATE", -2));
			 }

			 if (intent.getIntExtra("state", -1) == 0 /*
													 * || intent.getIntExtra(
													 * "EXTRA_SCO_AUDIO_STATE",
													 * -1) == 0
													 */)
			 {
				 dynamicToggleVideoCall.setChecked(false);
			 }
			 else if (intent.getIntExtra("state", -1) == 1) 
			 {
				dynamicToggleVideoCall.setChecked(true);
			 } 
			 else
			 {
				// Toast.makeText(context, "Output audio stream is incorrect",
				// Toast.LENGTH_LONG).show();
			 }
			 dynamicToggleVideoCall.invalidate();
		}
	}
	
	public static enum StartConversetionReason {
		INCOME_CALL_FOR_ACCEPTION, OUTCOME_CALL_MADE;
	}

	@Override
	public void onResume()
	{
		super.onResume();
		
		// If user changed camera state few times and last state was
		// CameraState.ENABLED_FROM_USER 
		// than we turn on camera, else we nothing change
		if (cameraState != CameraState.DISABLED_FROM_USER && qbConferenceType == QBRTCTypes.QBConferenceType.QB_CONFERENCE_TYPE_VIDEO.getValue())
			toggleCamera(true);
	}
	
	@Override
	public void onPause()
	{
		// If camera state is CameraState.ENABLED_FROM_USER or CameraState.NONE
		// than we turn off camera
		if (cameraState != CameraState.DISABLED_FROM_USER) 
			toggleCamera(false);
				
		super.onPause();
	}

	@Override
	public void onStop() 
	{
		super.onStop();
		
		stopOutBeep();
		getActivity().unregisterReceiver(audioStreamReceiver);
	}
}
