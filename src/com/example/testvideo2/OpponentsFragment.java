package com.example.testvideo2;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import com.quickblox.core.QBCallback;
import com.quickblox.core.QBEntityCallback;
import com.quickblox.core.exception.QBResponseException;
import com.quickblox.core.request.QBPagedRequestBuilder;
import com.quickblox.core.result.Result;
import com.quickblox.users.QBUsers;
import com.quickblox.users.model.QBUser;
import com.quickblox.videochat.webrtc.QBRTCTypes;

import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;

public class OpponentsFragment extends Fragment implements Serializable
{
	private static final String DEBUG_TAG = "OpponentsFragment______";
    private View view;
    private Button makeCall;
    private Button userA;
    private Button userB;
    
    private static boolean iAmUserA = true;
    

    private List<Integer> opponents;
    
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) 
	{
		view = inflater.inflate(R.layout.fragment_opponents, container, false);
		setupLayout(view);
		setupListeners();
		
		return view;
	}
	
	private void setupLayout(View view)
	{
		makeCall = (Button) view.findViewById(R.id.make_call);
		userA = (Button) view.findViewById(R.id.user_a);
		userB = (Button) view.findViewById(R.id.user_b);
	}
	
	private void setupListeners()
	{
		makeCall.setOnClickListener(new OnClickListener()
		{
			@SuppressWarnings("deprecation")
			@Override
			public void onClick(View v)
			{
				Log.i(getTag(), "Making call.");
				
				if (iAmUserA)
					makeCallToUser("userB");
				else
					makeCallToUser("userA");
			}
		});
		
		userA.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				((MainActivity) getActivity()).loginAsUserA();
				Log.i(getTag(), "Logining. A");
				iAmUserA = true;
			}
		});
		
		userB.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				((MainActivity) getActivity()).loginAsUserB();
				Log.i(getTag(), "Logining. B");
				iAmUserA = false;
			}
		});
	}
	
	private void makeCallToUser(String login)
	{
		List<String> logins = new LinkedList<String>();
		logins.add(login);
		QBPagedRequestBuilder requestBuilder = new QBPagedRequestBuilder();
		requestBuilder.setPerPage(100);
		QBUsers.getUsersByLogins(logins, requestBuilder, new QBEntityCallback<ArrayList<QBUser>>() 
		{
			@Override
			public void onError(List<String> arg0) 
			{
				Log.e(DEBUG_TAG, "1");
			}

			@Override
			public void onSuccess()
			{
				Log.i(DEBUG_TAG, "2");
			}

			@Override
			public void onSuccess(ArrayList<QBUser> arg0,
					Bundle arg1)
			{
				Log.i(DEBUG_TAG, "3");

				Log.i(DEBUG_TAG, "User id is " + arg0.get(0).getId());
				Log.i(DEBUG_TAG, "User login is " + arg0.get(0).getLogin());
				
				
				QBRTCTypes.QBConferenceType qbConferenceType = null;
				qbConferenceType = QBRTCTypes.QBConferenceType.QB_CONFERENCE_TYPE_VIDEO;
				Map<String, String> userInfo = new HashMap<String, String>();
				userInfo.put("any_custom_data", "some data");
				userInfo.put("my_avatar_url", "avatar_reference");
				opponents = new ArrayList<Integer>();
				opponents.add(arg0.get(0).getId());
				((MainActivity) getActivity()).addConversationFragmentStartCall(
						opponents, qbConferenceType, userInfo);
			}
		});
	}
	
	@Override
	public void onCreate(Bundle savedInstanceState) 
	{
		super.onCreate(savedInstanceState);
	}
	
	@Override
	public void onStart() 
	{
		super.onStart();
	}

	@Override
	public void onPause()
	{
		super.onPause();
	}
}
