package com.example.testvideo2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.jivesoftware.smack.SmackException;
import org.webrtc.VideoRenderer;

import com.conversation.ConversationFragment;
import com.incomeing.IncomeCallFragment;
import com.quickblox.auth.QBAuth;
import com.quickblox.auth.model.QBSession;
import com.quickblox.chat.QBChatService;
import com.quickblox.chat.QBSignaling;
import com.quickblox.chat.QBWebRTCSignaling;
import com.quickblox.chat.listeners.QBVideoChatSignalingManagerListener;
import com.quickblox.core.QBEntityCallbackImpl;
import com.quickblox.core.QBSettings;
import com.quickblox.users.QBUsers;
import com.quickblox.users.model.QBUser;
import com.quickblox.videochat.webrtc.QBRTCClient;
import com.quickblox.videochat.webrtc.QBRTCConfig;
import com.quickblox.videochat.webrtc.QBRTCException;
import com.quickblox.videochat.webrtc.QBRTCSession;
import com.quickblox.videochat.webrtc.QBRTCTypes;
import com.quickblox.videochat.webrtc.callbacks.QBRTCClientConnectionCallbacks;
import com.quickblox.videochat.webrtc.callbacks.QBRTCClientSessionCallbacks;
import com.quickblox.videochat.webrtc.callbacks.QBRTCClientVideoTracksCallbacks;
import com.quickblox.videochat.webrtc.view.QBGLVideoView;
import com.quickblox.videochat.webrtc.view.QBRTCVideoTrack;
import com.quickblox.videochat.webrtc.view.VideoCallBacks;

import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.view.Menu;
import android.widget.Chronometer;
import android.widget.Toast;

public class MainActivity extends Activity 
{
	private static final String TAG = "______MainActivity";
	private String appID = "25675";
	private String authorization_key = "9kyHfRghBQqQXn8";
	private String authorization_secret = "8LQaKC-3J6dYugK";
	private String login = "userA";//"jmmsrbjut2";
	private String password = "userAPassword";//"8d91144Z72"; 
	private QBUser user;
	private QBChatService chatService;
	private boolean isInited = false;
	
	private void initFramework()
	{
		if (isInited)
			return;
		
		isInited = true;
		QBSettings.getInstance().fastConfigInit(appID, authorization_key, authorization_secret);
	}
	
	public void createNewUser(final String login, final String password)
	{
		initFramework();
		
		QBAuth.createSession(new QBEntityCallbackImpl<QBSession>()
		{
		    @Override
		    public void onSuccess(QBSession session, Bundle params)
		    {
		    	updateMessage("Success to createSession.");
		    	// Register new user
		    	final QBUser user = new QBUser(login, password);
		    	 
		    	QBUsers.signUp(user, new QBEntityCallbackImpl<QBUser>()
		    	{
		    	    @Override
		    	    public void onSuccess(QBUser user, Bundle args) 
		    	    {
				    	updateMessage("Success to signUp.");
		    	    }
		    	 
		    	    @Override
		    	    public void onError(List<String> errors)
		    	    {
				    	updateMessage("Filed to signUp.");
		    	    }
		    	});
		    }
		 
		    @Override
		    public void onError(List<String> errors)
		    {
		    	updateMessage("Filed to createSession.");
		    }
		});
	}
	
	public void login(final String login, final String password)
	{
		initFramework();
		
		user = new QBUser(login, password);
		QBAuth.createSession(new QBEntityCallbackImpl<QBSession>()
		{
		    @Override
		    public void onSuccess(QBSession session, Bundle params)
		    {
		    	updateMessage("Success to createSession.");
		    	QBUsers.signIn(user, new QBEntityCallbackImpl<QBUser>() 
		    	{
		    	    @Override
		    	    public void onSuccess(QBUser user, Bundle args)
		    	    {
		    	    	updateMessage("Success to signIn.");
		    	    	
		    	    	user.setPassword(MainActivity.this.user.getPassword());
		    	    	MainActivity.this.user = user;
		    	    	initChatService();
		    	    }
		    	 
		    	    @Override
		    	    public void onError(List<String> errors)
		    	    {
		    	    	updateMessage("Failed to signIn.");
		    	    }
		    	});
		    }
		 
		    @Override
		    public void onError(List<String> errors) 
		    {
		    	updateMessage("Failed to createSession.");
		    }
		});
	}
	
	private void updateMessage(String text)
	{
		Log.i(TAG, text);
		Toast.makeText(MainActivity.this, text, Toast.LENGTH_SHORT).show();
	}	
	
	@SuppressWarnings("rawtypes")
	private void initChatService()
	{
		// initialize Chat service
		if (!QBChatService.isInitialized())
		{
		    QBChatService.init(MainActivity.this);
		    chatService = QBChatService.getInstance();
		}
		 
		chatService.login(user, new QBEntityCallbackImpl()
		{
		    @Override
		    public void onSuccess() 
		    {
		    	Log.i(TAG, "Success to login chatService!");
		    	
		    	if (chatService.isLoggedIn())
		    	{
		    		addSignallingManager();
		    		
		    		addSessionCallbacksListener();
		    		addVideoTrackCallbacksListener();
		    		addConnectionCallbacksListener();
		    		 
		    		QBRTCClient.getInstance().prepareToProcessCalls(MainActivity.this);
		    	}
		    }
		 
		    @Override
		    public void onError(List errors)
		    {
		    	Log.e(TAG, "Failed to login chatService!");
		    }
		});
	}
	
	private void addSignallingManager()
	{
		Log.i(TAG, "Add Signaling ManagerListener.");
		QBChatService.getInstance().getVideoChatWebRTCSignalingManager()
        .addSignalingManagerListener(new QBVideoChatSignalingManagerListener() 
        {
            @Override
            public void signalingCreated(QBSignaling qbSignaling, boolean createdLocally)
            {
                if (!createdLocally)
                {
                    QBRTCClient.getInstance().addSignaling((QBWebRTCSignaling) qbSignaling);
                	Log.i(TAG, "Success to signalingCreated.");
                }
            }
        });
	}
	
	private void addSessionCallbacksListener()
	{
		QBRTCClient.getInstance().addSessionCallbacksListener(new QBRTCClientSessionCallbacks()
		{
			@Override
			public void onUserNotAnswer(QBRTCSession session, Integer userID) 
			{
				Log.w(TAG, "User Not Answer.");
				
				runOnUiThread(new Runnable() 
				{
					@Override
					public void run()
					{
						ConversationFragment fragment = (ConversationFragment) getFragmentManager()
								.findFragmentByTag(CONVERSATION_CALL_FRAGMENT);
						if (fragment != null)
						{
							fragment.actionButtonsEnabled(false);
							fragment.stopOutBeep();
						}
					}
				});
			}
			
			@Override
			public void onSessionStartClose(final QBRTCSession session) 
			{
				Log.w(TAG, "Session Start Close.");
				
				runOnUiThread(new Runnable()
				{
					@Override
					public void run()
					{
						ConversationFragment fragment = (ConversationFragment) getFragmentManager()
								.findFragmentByTag(CONVERSATION_CALL_FRAGMENT);
						if (fragment != null && session.equals(getCurrentSession())) 
							fragment.actionButtonsEnabled(false);
					}
				});
			}
			
			@Override
			public void onSessionClosed(final QBRTCSession session)
			{
				Log.w(TAG, "Session Closed " + session.getSessionID());
				
				runOnUiThread(new Runnable()
				{
					@Override
					public void run() 
					{

						Log.d(TAG, "Session " + session.getSessionID() + " start stop session");
						String curSession = (getCurrentSession() == null) ? null : getCurrentSession().getSessionID();
						Log.d(TAG, "Session " + curSession + " is current");

						if (session.equals(getCurrentSession())) 
						{
							if (isInCommingCall)
								stopIncomeCallTimer();

							Log.d(TAG, "Stop session");
							// addOpponentsFragmentWithDelay();
							addOpponentsFragment();

							// Remove current session
							Log.d(TAG, "Remove current session");
							Log.d("Crash", "onSessionClosed. Set session to null");
							currentSession = null;

							stopTimer();
							closeByWifiStateAllow = true;
							processCurrentWifiState(MainActivity.this);
						}
					}
				});
			}
			
			@Override
			public void onReceiveNewSession(final QBRTCSession session)
			{
				Log.d(TAG, "Session " + session.getSessionID() + " are income");
				String curSession = (getCurrentSession() == null) ? null: getCurrentSession().getSessionID();
				Log.d(TAG, "Session " + curSession + " is current");

				if (getCurrentSession() == null)
				{
					Log.d(TAG, "Start new session");

					setCurrentSession(session);
					Log.d("Crash", "onReceiveNewSession. Set session to " + session);

					addIncomeCallFragment(session);

					isInCommingCall = true;
					initIncommingCallTask();
					startIncomeCallTimer();
				} 
				else
				{
					Log.w(TAG, "Stop new session. Device now is busy");
					session.rejectCall(null);
				}
			}
			
			@Override
			public void onReceiveHangUpFromUser(QBRTCSession session, Integer userID)
			{
				Log.w(TAG, "Receive Hang Up From User " + userID);
			}
			
			@Override
			public void onCallRejectByUser(QBRTCSession session, Integer userID,
					Map<String, String> userInfo) 
			{
				Log.w(TAG, "Call Reject By User " + userID);
				
				runOnUiThread(new Runnable() 
				{
					@Override
					public void run() 
					{
						ConversationFragment fragment = (ConversationFragment) getFragmentManager()
								.findFragmentByTag(CONVERSATION_CALL_FRAGMENT);
						if (fragment != null) 
							fragment.stopOutBeep();
					}
				});
			}
		});
	}
	
	private void addVideoTrackCallbacksListener()
	{
		QBRTCClient.getInstance().addVideoTrackCallbacksListener(new QBRTCClientVideoTracksCallbacks()
		{
			@Override
			public void onRemoteVideoTrackReceive(QBRTCSession session, QBRTCVideoTrack videoTrack, Integer userID) 
			{
				remoteVideoView = (QBGLVideoView) findViewById(R.id.remoteVideoView);
				Log.d(TAG, "remoteVideoView is " + remoteVideoView);
				if (remoteVideoView != null)
				{
					VideoRenderer remouteRenderer = new VideoRenderer(new VideoCallBacks(remoteVideoView, QBGLVideoView.Endpoint.REMOTE));
					videoTrack.addRenderer(remouteRenderer);
					remoteVideoView.setVideoTrack(videoTrack, QBGLVideoView.Endpoint.REMOTE);
					Log.d(TAG, "onRemoteVideoTrackReceive() is raned");
				}
			}
			
			@Override
			public void onLocalVideoTrackReceive(QBRTCSession session, QBRTCVideoTrack videoTrack) 
			{
				localVideoVidew = (QBGLVideoView) findViewById(R.id.localVideoView);
				Log.d(TAG, "localVideoVidew is " + localVideoVidew);
				if (localVideoVidew != null)
				{
					videoTrack.addRenderer(new VideoRenderer(new VideoCallBacks(localVideoVidew, QBGLVideoView.Endpoint.LOCAL)));
					localVideoVidew.setVideoTrack(videoTrack, QBGLVideoView.Endpoint.LOCAL);
					Log.d(TAG, "onLocalVideoTrackReceive() is raned");
				}
			}
		});
	}
	
	private void addConnectionCallbacksListener()
	{
		QBRTCClient.getInstance().addConnectionCallbacksListener(new QBRTCClientConnectionCallbacks()
		{
			@Override
			public void onStartConnectToUser(QBRTCSession arg0, Integer arg1) 
			{
				Log.i(TAG, "Start Connect To User.");
				runOnUiThread(new Runnable()
				{
					@Override
					public void run() 
					{
						ConversationFragment fragment = (ConversationFragment) getFragmentManager()
								.findFragmentByTag(CONVERSATION_CALL_FRAGMENT);
						if (fragment != null) 
							fragment.stopOutBeep();
					}
				});
			}
			
			@Override
			public void onError(QBRTCSession arg0, QBRTCException arg1)
			{
				Log.e(TAG, "Error in addConnectionCallbacksListener.");
			}
			
			@Override
			public void onDisconnectedTimeoutFromUser(QBRTCSession session, Integer userID) 
			{
				Log.w(TAG, "Disconnected Timeout From User " + userID);
			}
			
			@Override
			public void onDisconnectedFromUser(QBRTCSession session, Integer userID) 
			{
				Log.w(TAG, "Disconnected From User " + userID);
			}
			
			@Override
			public void onConnectionFailedWithUser(QBRTCSession session, Integer userID)
			{
				Log.e(TAG, "Connection Failed With User " + userID);
			}
			
			@Override
			public void onConnectionClosedForUser(QBRTCSession session, Integer userID) 
			{
				Log.w(TAG, "Connection Closed For User " + userID);
				
				runOnUiThread(new Runnable()
				{
					@Override
					public void run()
					{
						// Close app after session close of network was disabled
						Log.d(TAG, "onConnectionClosedForUser()");
						if (hangUpReason != null && hangUpReason.equals(WIFI_DISABLED))
						{
							Intent returnIntent = new Intent();
							setResult(CALL_ACTIVITY_CLOSE_WIFI_DISABLED, returnIntent);
							finish();
						}
					}
				});
			}
			
			@Override
			public void onConnectedToUser(QBRTCSession session, final Integer userID) 
			{
				Log.i(TAG, "Connected To User.");
				
				forbidenCloseByWifiState();
				runOnUiThread(new Runnable()
				{
					@Override
					public void run() 
					{
						if (isInCommingCall) 
							stopIncomeCallTimer();

						startTimer();

						Log.d(TAG, "onConnectedToUser() is started");

						ConversationFragment fragment = (ConversationFragment) getFragmentManager().findFragmentByTag(CONVERSATION_CALL_FRAGMENT);
						if (fragment != null) 
							fragment.actionButtonsEnabled(true);
					}
				});
			}
		});
	}
	
	/*private void foo()
	{
		login("userA", "userAPassword");
	}*/
	
	public void loginAsUserA()
	{
		login("userA", "userAPassword");
	}
	
	public void loginAsUserB()
	{
		login("userB", "userBPassword");
	}
	
	@Override
	protected void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		if (savedInstanceState == null)
			initLayout();
		
		//foo();
	}

	public void initLayout()
	{
		getFragmentManager().beginTransaction().replace(R.id.fragment_container, new OpponentsFragment(), OPPONENTS_CALL_FRAGMENT).commit();
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) 
	{
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}
	
	@Override
	public void onBackPressed() 
	{
		// Logout on back btn click
		Fragment fragment = getFragmentManager().findFragmentByTag(CONVERSATION_CALL_FRAGMENT);
		if (fragment == null) 
		{
			super.onBackPressed();
			if (QBChatService.isInitialized()) 
			{
				try 
				{
					QBRTCClient.getInstance().close(true);
					QBChatService.getInstance().logout();
					updateMessage("Logout chatService.");
				} catch (SmackException.NotConnectedException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	/***
	 * Callback relate begins.
	 */
	private QBRTCSession currentSession;
	private boolean isInCommingCall;
	private QBGLVideoView localVideoVidew;
	private QBGLVideoView remoteVideoView;
	public static final String INCOME_CALL_FRAGMENT = "income_call_fragment";
	public static final String OPPONENTS_CALL_FRAGMENT = "opponents_call_fragment";
	public static final String CONVERSATION_CALL_FRAGMENT = "conversation_call_fragment";
	public static final String CALLER_NAME = "caller_name";
	public static final String SESSION_ID = "sessionID";
	public static final String START_CONVERSATION_REASON = "start_conversation_reason";
	
	public QBRTCSession getCurrentSession() 
	{
		return currentSession;
	}
	
	public void setCurrentSession(QBRTCSession sesion) 
	{
		Log.d("Crash", "setCurrentSession. Set session to " + sesion);
		this.currentSession = sesion;
	}
	
	private void addIncomeCallFragment(QBRTCSession session)
	{
		Log.d(TAG, "QBRTCSession in addIncomeCallFragment is " + session);
		if (session != null) 
		{
			Fragment fragment = new IncomeCallFragment();
			Bundle bundle = new Bundle();
			bundle.putSerializable("sessionDescription", session.getSessionDescription());
			bundle.putIntegerArrayList("opponents", new ArrayList<Integer>(session.getOpponents()));
			bundle.putInt("conference_type", session.getConferenceType().getValue());
			fragment.setArguments(bundle);
			getFragmentManager().beginTransaction().replace(R.id.fragment_container, fragment, INCOME_CALL_FRAGMENT).commit();
		} 
		else
			Log.e(TAG, "SKIP addIncomeCallFragment method");
	}
	
	public void rejectCurrentSession()
	{
		if (getCurrentSession() != null) 
			getCurrentSession().rejectCall(new HashMap<String, String>());
	}
	
	public void removeIncomeCallFragment() 
	{
		FragmentManager fragmentManager = getFragmentManager();
		Fragment fragment = fragmentManager.findFragmentByTag(INCOME_CALL_FRAGMENT);

		if (fragment != null) 
			fragmentManager.beginTransaction().remove(fragment).commit();
	}
	
	public void addOpponentsFragment()
	{
		getFragmentManager().beginTransaction().replace(R.id.fragment_container, new OpponentsFragment(), OPPONENTS_CALL_FRAGMENT).commit();
	}
	
	public void addConversationFragmentReceiveCall()
	{
		QBRTCSession session = getCurrentSession();
		
		if (getCurrentSession() != null)
		{
			Integer myId = QBChatService.getInstance().getUser().getId();
			ArrayList<Integer> opponentsWithoutMe = new ArrayList<Integer>(session.getOpponents());
			opponentsWithoutMe.remove(Integer.valueOf(myId));
			opponentsWithoutMe.add(session.getCallerID());

			// init conversation fragment
			ConversationFragment fragment = new ConversationFragment();
			Bundle bundle = new Bundle();
			bundle.putIntegerArrayList("opponents", opponentsWithoutMe);
			bundle.putInt("conference_type", session.getConferenceType().getValue());
			bundle.putInt(START_CONVERSATION_REASON, StartConversetionReason.INCOME_CALL_FOR_ACCEPTION.ordinal());
			bundle.putString(SESSION_ID, session.getSessionID());
			bundle.putString(CALLER_NAME, String.valueOf(session.getCallerID()));

			if (session.getUserInfo() != null) 
			{
				for (String key : session.getUserInfo().keySet()) 
					bundle.putString("UserInfo:" + key, session.getUserInfo().get(key));
			}

			fragment.setArguments(bundle);

			// Start conversation fragment
			getFragmentManager().beginTransaction().replace(R.id.fragment_container, fragment, CONVERSATION_CALL_FRAGMENT).commit();
		}
	}
	
	public static enum StartConversetionReason 
	{
		INCOME_CALL_FOR_ACCEPTION, OUTCOME_CALL_MADE;
	}
	
	///////////////////////////////////////////////////////////////////////////////////////////////////
	///////////////////////////////////////////////////////////////////////////////////////////////////
	///////////////////////////////////////////////////////////////////////////////////////////////////
	private Runnable showIncomingCallWindowTask;
	private Handler showIncomingCallWindowTaskHandler;
	
	private void initIncommingCallTask()
	{
		showIncomingCallWindowTaskHandler = new Handler(Looper.myLooper());
		showIncomingCallWindowTask = new Runnable() 
		{
			@Override
			public void run()
			{
				IncomeCallFragment incomeCallFragment = (IncomeCallFragment) getFragmentManager().findFragmentByTag(INCOME_CALL_FRAGMENT);
				if (incomeCallFragment == null)
				{
					ConversationFragment conversationFragment = (ConversationFragment) getFragmentManager().findFragmentByTag(CONVERSATION_CALL_FRAGMENT);
					if (conversationFragment != null)
					{
						disableConversationFragmentButtons();
						stopConversationFragmentBeeps();
						hangUpCurrentSession();
					}
				}
				else
					rejectCurrentSession();
				
				Toast.makeText(MainActivity.this, "Call was stopped by timer", Toast.LENGTH_LONG).show();
			}
		};
	}
	
	private void disableConversationFragmentButtons() 
	{
		ConversationFragment fragment = (ConversationFragment) getFragmentManager().findFragmentByTag(CONVERSATION_CALL_FRAGMENT);
		if (fragment != null) 
			fragment.actionButtonsEnabled(false);
	}
	
	private void stopConversationFragmentBeeps() 
	{
		ConversationFragment fragment = (ConversationFragment) getFragmentManager()
				.findFragmentByTag(CONVERSATION_CALL_FRAGMENT);
		if (fragment != null) {
			fragment.stopOutBeep();
		}
	}
	
	public void hangUpCurrentSession() 
	{
		if (getCurrentSession() != null)
			getCurrentSession().hangUp(new HashMap<String, String>());
	}
	
	///////////////////////////////////////////////////////////////////////////////////////////////////
	///////////////////////////////////////////////////////////////////////////////////////////////////
	///////////////////////////////////////////////////////////////////////////////////////////////////
	private void startIncomeCallTimer()
	{
		showIncomingCallWindowTaskHandler.postAtTime(showIncomingCallWindowTask,
				SystemClock.uptimeMillis() +  TimeUnit.SECONDS.toMillis(QBRTCConfig.getAnswerTimeInterval()));
	}
	
	///////////////////////////////////////////////////////////////////////////////////////////////////
	///////////////////////////////////////////////////////////////////////////////////////////////////
	///////////////////////////////////////////////////////////////////////////////////////////////////
	private boolean closeByWifiStateAllow = true;
	private String hangUpReason;
    public static final String WIFI_DISABLED = "wifi_disabled";
    private Chronometer timerABWithTimer;
    private boolean isStarted = false;
	
	private void stopIncomeCallTimer()
	{
		Log.d(TAG, "stopIncomeCallTimer");
		showIncomingCallWindowTaskHandler.removeCallbacks(showIncomingCallWindowTask);
	}
	
	private void processCurrentWifiState(Context context) 
	{
		WifiManager wifi = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
		if (!wifi.isWifiEnabled()) 
		{
			Log.d(TAG, "WIFI is turned off");
			if (closeByWifiStateAllow)
			{
				if (currentSession != null) 
				{
					Log.d(TAG, "currentSession NOT null");
					// Close session safely
					disableConversationFragmentButtons();
					stopConversationFragmentBeeps();

					hangUpCurrentSession();

					hangUpReason = WIFI_DISABLED;
				} 
				else 
				{
					Log.d(TAG, "Call finish() on activity");
					finish();
				}
			}
			else 
				Log.d(TAG, "WIFI is turned on");
		}
	}
	
    public void stopTimer()
    {
        if (timerABWithTimer != null)
        {
            timerABWithTimer.stop();
            isStarted = false;
        }
    }
    
	///////////////////////////////////////////////////////////////////////////////////////////////////
	///////////////////////////////////////////////////////////////////////////////////////////////////
	///////////////////////////////////////////////////////////////////////////////////////////////////
    public static final int CALL_ACTIVITY_CLOSE_WIFI_DISABLED = 1001;
    
    public void startTimer() 
    {
        if (!isStarted && (timerABWithTimer != null))
        {
            timerABWithTimer.setBase(SystemClock.elapsedRealtime());
            timerABWithTimer.start();
            isStarted = true;
        }
    }
    
    private void forbidenCloseByWifiState()
    {
		closeByWifiStateAllow = false;
	}
	///////////////////////////////////////////////////////////////////////////////////////////////////
	///////////////////////////////////////////////////////////////////////////////////////////////////
	///////////////////////////////////////////////////////////////////////////////////////////////////
	/***
	 * Callback relate ends.
	 */
    
    
    
    
    
    
    
    
    
    
    
    
    
    
    public void addConversationFragmentStartCall(List<Integer> opponents,
			QBRTCTypes.QBConferenceType qbConferenceType, Map<String, String> userInfo) 
    {
		// init session for new call
		try 
		{
			QBRTCSession newSessionWithOpponents = QBRTCClient.getInstance()
					.createNewSessionWithOpponents(opponents, qbConferenceType);
			Log.d("Crash", "addConversationFragmentStartCall. Set session "
					+ newSessionWithOpponents);
			setCurrentSession(newSessionWithOpponents);

			ConversationFragment fragment = new ConversationFragment();
			Bundle bundle = new Bundle();
			bundle.putIntegerArrayList("opponents", new ArrayList<Integer>(opponents));
			bundle.putInt("conference_type", qbConferenceType.getValue());
			bundle.putInt(START_CONVERSATION_REASON, StartConversetionReason.OUTCOME_CALL_MADE.ordinal());
			bundle.putString(CALLER_NAME, String.valueOf(opponents.get(0)));

			for (String key : userInfo.keySet()) 
				bundle.putString("UserInfo:" + key, userInfo.get(key));

			fragment.setArguments(bundle);
			getFragmentManager().beginTransaction().replace(R.id.fragment_container,
				fragment, CONVERSATION_CALL_FRAGMENT).commit();

		} catch (IllegalStateException e) {
			Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
		}
	}
}