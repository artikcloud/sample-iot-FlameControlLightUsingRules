/*
 * Copyright (C) 2016 Samsung Electronics Co., Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.samsungsami.example.samirules;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;

import io.samsungsami.api.UsersApi;
import io.samsungsami.websocket.Acknowledgement;
import io.samsungsami.websocket.ActionOut;
import io.samsungsami.websocket.Error;
import io.samsungsami.websocket.FirehoseWebSocket;
import io.samsungsami.websocket.MessageOut;
import io.samsungsami.websocket.SamiWebSocketCallback;

public class SAMISession {
    private final static String TAG = SAMISession.class.getSimpleName();

    // Copy them from the corresponding application in the Developer Portal
    public static final String CLIENT_ID = "<YOUR CLIENT ID>";
    public static final String REDIRECT_URL = "android-app://redirect";

    // Copy them from the Device Info screen in the User Portal
    public final static String SMART_LIGHT_DEVICE_ID = "<YOUR SMART LIGHT DEVICE ID>";
    public final static String FIRE_DETECTOR_DEVICE_ID = "<YOUR FIRE DETECTOR DEVICE ID>";

    public static final String SAMI_AUTH_BASE_URL = "https://accounts.samsungsami.io";
    public static final String SAMI_REST_URL = "https://api.samsungsami.io/v1.1";

    private static final String AUTHORIZATION = "Authorization";
    public final static String SMART_LIGHT_DEVICE_NAME = "Smart Light";
    public final static String FIRE_DETECTOR_DEVICE_NAME = "Flame Detector";
    public final static int FIRE_DETECTOR_INDEX = 0;
    public final static int SMART_LIGHT_INDEX = 1;

    private static SAMISession ourInstance = new SAMISession();
    private static Context ourContext;

    public final static String WEBSOCKET_LIVE_ONOPEN =
            "io.samsungsami.example.iot.WEBSOCKET_LIVE_ONOPEN";
    public final static String WEBSOCKET_LIVE_ONMSG =
            "io.samsungsami.example.iot.WEBSOCKET_LIVE_ONMSG";
    public final static String WEBSOCKET_LIVE_ONCLOSE =
            "io.samsungsami.example.iot.WEBSOCKET_LIVE_ONCLOSE";
    public final static String WEBSOCKET_LIVE_ONERROR =
            "io.samsungsami.example.iot.WEBSOCKET_LIVE_ONERROR";
    public final static String DEVICE_INDEX = "dindex";
    public final static String SDID = "sdid";
    public final static String DEVICE_DATA = "data";
    public final static String TIMESTEP = "ts";
    public final static String ERROR = "error";

    private ArrayList<String> mDeviceIDArray;

    private UsersApi mUsersApi = null;
    private RulesApi mRulesApi = null;
    private String mAccessToken = null;
    private String mUserId = null;

    private FirehoseWebSocket mLive = null; //  end point: /live

    public static SAMISession getInstance() {
        return ourInstance;
    }

    private SAMISession() {
        mDeviceIDArray = new ArrayList<>();

        mDeviceIDArray.add(FIRE_DETECTOR_INDEX, FIRE_DETECTOR_DEVICE_ID);
        mDeviceIDArray.add(SMART_LIGHT_INDEX, SMART_LIGHT_DEVICE_ID);
    }

    public void setContext(Context context) {
        ourContext = context;
    }

    public void setAccessToken(String token) {
        if (token == null || token.length() <= 0) {
            Log.e(TAG, "Attempt to set a invalid token");
            mAccessToken = null;
            return;
        }
        mAccessToken = token;
    }

    public void setupSamiRestApis() {
        // Invoke the appropriate API
        mUsersApi = new UsersApi();
        mUsersApi.setBasePath(SAMI_REST_URL);
        mUsersApi.addHeader(AUTHORIZATION, "bearer " + mAccessToken);

        mRulesApi = new RulesApi();
        mRulesApi.setBasePath(SAMI_REST_URL);
        mRulesApi.addHeader(AUTHORIZATION, "bearer " + mAccessToken);
    }

    public UsersApi getUsersApi() {
        return mUsersApi;
    }

    public RulesApi getRulesApi() {
        return mRulesApi;
    }

    public String getAuthorizationRequestUri() {
        //example: https://accounts.samsungsami.io/authorize?client=mobile&client_id=xxxx&response_type=token&redirect_uri=http://localhost:81/samidemo/index.php
        return SAMISession.SAMI_AUTH_BASE_URL + "/authorize?client=mobile&response_type=token&" +
                "client_id=" + SAMISession.CLIENT_ID + "&redirect_uri=" + SAMISession.REDIRECT_URL;
    }

    public void reset() {
        mUsersApi = null;
        mRulesApi = null;
        mAccessToken = null;
        mUserId = null;
        mLive = null;
    }

    public void setUserId(String uid) {
        if (uid == null || uid.length() <= 0) {
            Log.w(TAG, "setUserId() get null uid");
        }
        mUserId = uid;
    }

    private void createLiveWebsocket() {
        String sdids = "";
        int numId = mDeviceIDArray.size();
        for (int i = 0; i < numId-1; ++i) {
            sdids = sdids + mDeviceIDArray.get(i) + ",";
        }
        sdids = sdids + mDeviceIDArray.get(numId - 1);

        try {
            mLive = new FirehoseWebSocket(mAccessToken, sdids, null, mUserId, new SamiWebSocketCallback() {
                @Override
                public void onOpen(short i, String s) {
                    Log.d(TAG, "connectLiveWebsocket: onOpen()");
                    final Intent intent = new Intent(WEBSOCKET_LIVE_ONOPEN);
                    LocalBroadcastManager.getInstance(ourContext).sendBroadcast(intent);
                }

                @Override
                public void onMessage(MessageOut messageOut) {
                    Log.d(TAG, "connectLiveWebsocket: onMessage(" + messageOut.toString() + ")");
                    String sdid = messageOut.getSdid();
                    int dIdx;
                    if (sdid.equals(mDeviceIDArray.get(0))) {
                        dIdx = 0;
                    } else if (sdid.equals(mDeviceIDArray.get(1))) {
                        dIdx = 1;
                    } else {
                        Log.w(TAG, ": /live receives a msg from unrecognized device with id " + sdid);
                        return;
                    }
                    final Intent intent = new Intent(WEBSOCKET_LIVE_ONMSG);
                    intent.putExtra(DEVICE_INDEX, dIdx);
                    intent.putExtra(SDID, sdid);
                    intent.putExtra(DEVICE_DATA, messageOut.getData().toString());
                    intent.putExtra(TIMESTEP, messageOut.getTs().toString());
                    LocalBroadcastManager.getInstance(ourContext).sendBroadcast(intent);
                }

                @Override
                public void onAction(ActionOut actionOut) {
                }

                @Override
                public void onAck(Acknowledgement acknowledgement) {
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    final Intent intent = new Intent(WEBSOCKET_LIVE_ONCLOSE);
                    intent.putExtra("error", "mLive is closed. code: " + code + "; reason: " + reason);
                    LocalBroadcastManager.getInstance(ourContext).sendBroadcast(intent);
                }

                @Override
                public void onError(Error ex) {
                    final Intent intent = new Intent(WEBSOCKET_LIVE_ONERROR);
                    intent.putExtra("error", "mLive error: " + ex.getMessage());
                    LocalBroadcastManager.getInstance(ourContext).sendBroadcast(intent);
                }
            });
        } catch (URISyntaxException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Closes a websocket /live connection
     */
    public void disconnectFirehoseWS() {
        if (mLive != null) {
            mLive.close();
        }
        mLive = null;
    }

    public void connectFirehoseWSBlocking() {
        if (!isReadyToConnectWebSocketLive()) {
            Log.w(TAG, "It is not ready to connect firehose WebSocket!");
            return;
        }
        createLiveWebsocket();
        try {
            mLive.connectBlocking();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public void connectFirehoseWS() {
        if (!isReadyToConnectWebSocketLive()) {
            Log.w(TAG, "It is not ready to connect firehose WebSocket!");
            return;
        }
        createLiveWebsocket();
        mLive.connect();
    }

    public boolean isReadyToConnectWebSocketLive() {
        return mUserId != null && mAccessToken != null;
    }

    public boolean canCallRulesApi() {
        return isReadyToConnectWebSocketLive();
    }

}