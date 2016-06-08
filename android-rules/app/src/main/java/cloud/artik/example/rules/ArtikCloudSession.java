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

package cloud.artik.example.rules;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;

import cloud.artik.api.RulesApi;
import cloud.artik.api.UsersApi;
import cloud.artik.client.ApiClient;
import cloud.artik.model.Acknowledgement;
import cloud.artik.model.ActionOut;
import cloud.artik.model.MessageOut;
import cloud.artik.model.WebSocketError;
import cloud.artik.websocket.ArtikCloudWebSocketCallback;
import cloud.artik.websocket.FirehoseWebSocket;

public class ArtikCloudSession {
    private final static String TAG = ArtikCloudSession.class.getSimpleName();

    // Copy them from the corresponding application in the Developer Dashboard
    public static final String CLIENT_ID = "<YOUR CLIENT ID>";
    public static final String REDIRECT_URL = "android-app://redirect";

    // Copy them from the Device Info screen in My ARTIK Cloud
    public final static String SMART_LIGHT_DEVICE_ID = "<YOUR SMART LIGHT DEVICE ID>";
    public final static String FIRE_DETECTOR_DEVICE_ID = "<YOUR FIRE DETECTOR DEVICE ID>";

    public static final String ARTIK_CLOUD_AUTH_BASE_URL = "https://accounts.artik.cloud";

    public final static String SMART_LIGHT_DEVICE_NAME = "Smart Light";
    public final static String FIRE_DETECTOR_DEVICE_NAME = "Flame Detector";
    public final static int FIRE_DETECTOR_INDEX = 0;
    public final static int SMART_LIGHT_INDEX = 1;

    private static ArtikCloudSession ourInstance = new ArtikCloudSession();
    private static Context ourContext;

    public final static String WEBSOCKET_LIVE_ONOPEN =
            "cloud.artik.example.iot.WEBSOCKET_LIVE_ONOPEN";
    public final static String WEBSOCKET_LIVE_ONMSG =
            "cloud.artik.example.iot.WEBSOCKET_LIVE_ONMSG";
    public final static String WEBSOCKET_LIVE_ONCLOSE =
            "cloud.artik.example.iot.WEBSOCKET_LIVE_ONCLOSE";
    public final static String WEBSOCKET_LIVE_ONERROR =
            "cloud.artik.example.iot.WEBSOCKET_LIVE_ONERROR";
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

    private FirehoseWebSocket mFirehoseWS = null; //  end point: /live

    public static ArtikCloudSession getInstance() {
        return ourInstance;
    }

    private ArtikCloudSession() {
        mDeviceIDArray = new ArrayList<>();

        mDeviceIDArray.add(FIRE_DETECTOR_INDEX, FIRE_DETECTOR_DEVICE_ID);
        mDeviceIDArray.add(SMART_LIGHT_INDEX, SMART_LIGHT_DEVICE_ID);
    }

    public void setContext(Context context) {
        ourContext = context;
    }

    public void setAccessToken(String token) {
        if (token == null || token.length() <= 0) {
            Log.e(TAG, "Attempt to set an invalid token");
            mAccessToken = null;
            return;
        }
        mAccessToken = token;
    }

    public void setupArtikCloudRestApis() {
        ApiClient apiClient = new ApiClient();
        apiClient.setAccessToken(mAccessToken);
        apiClient.setDebugging(true);

        mUsersApi = new UsersApi(apiClient);
        mRulesApi = new RulesApi(apiClient);
    }

    public UsersApi getUsersApi() {
        return mUsersApi;
    }

    public RulesApi getRulesApi() {
        return mRulesApi;
    }

    public String getUserId() {return mUserId; }

    public String getAuthorizationRequestUri() {
        //https://accounts.artik.cloud/authorize?client=mobile&client_id=xxxx&response_type=token&redirect_uri=android-app://redirect
        return ARTIK_CLOUD_AUTH_BASE_URL + "/authorize?client=mobile&response_type=token&" +
                "client_id=" + CLIENT_ID + "&redirect_uri=" + REDIRECT_URL;
    }

    public void reset() {
        mUsersApi = null;
        mRulesApi = null;
        mAccessToken = null;
        mUserId = null;
        mFirehoseWS = null;
    }

    public void setUserId(String uid) {
        if (uid == null || uid.length() <= 0) {
            Log.w(TAG, "setUserId() get null uid");
        }
        mUserId = uid;
    }

    private void createFirehoseWebsocket() {
        String sdids = "";
        int numId = mDeviceIDArray.size();
        for (int i = 0; i < numId-1; ++i) {
            sdids = sdids + mDeviceIDArray.get(i) + ",";
        }
        sdids = sdids + mDeviceIDArray.get(numId - 1);

        try {
            mFirehoseWS = new FirehoseWebSocket(mAccessToken, null, sdids, null, mUserId, new ArtikCloudWebSocketCallback() {
                @Override
                public void onOpen(int i, String s) {
                    Log.d(TAG, "FirehoseWebSocket: onOpen()");
                    final Intent intent = new Intent(WEBSOCKET_LIVE_ONOPEN);
                    LocalBroadcastManager.getInstance(ourContext).sendBroadcast(intent);
                }

                @Override
                public void onMessage(MessageOut messageOut) {
                    Log.d(TAG, "FirehoseWebSocket: onMessage(" + messageOut.toString() + ")");
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
                    intent.putExtra("error", "mFirehoseWS is closed. code: " + code + "; reason: " + reason);
                    LocalBroadcastManager.getInstance(ourContext).sendBroadcast(intent);
                }

                @Override
                public void onError(WebSocketError ex) {
                    final Intent intent = new Intent(WEBSOCKET_LIVE_ONERROR);
                    intent.putExtra("error", "mFirehoseWS error: " + ex.getMessage());
                    LocalBroadcastManager.getInstance(ourContext).sendBroadcast(intent);
                }

                @Override
                public void onPing(long timestamp) {
                    Log.d(TAG, "FirehoseWebSocket::onPing: " + timestamp);
                }
            });
        } catch (URISyntaxException | IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Closes a websocket /live connection
     */
    public void disconnectFirehoseWS() {
        if (mFirehoseWS != null) {
            new DisconnectWSInBackground().execute();
        }
    }

    public void connectFirehoseWSBlocking() {
        if (!isReadyToConnectWebSocketLive()) {
            Log.w(TAG, "It is not ready to connect firehose WebSocket!");
            return;
        }
        createFirehoseWebsocket();
        try {
            mFirehoseWS.connectBlocking();
        } catch (InterruptedException | IOException e) {
            e.printStackTrace();
        }
    }

    public void connectFirehoseWS() {
        if (!isReadyToConnectWebSocketLive()) {
            Log.w(TAG, "It is not ready to connect firehose WebSocket!");
            return;
        }
        createFirehoseWebsocket();
        try {
            mFirehoseWS.connect();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean isReadyToConnectWebSocketLive() {
        return mUserId != null && mAccessToken != null;
    }

    public boolean canCallRulesApi() {
        return isReadyToConnectWebSocketLive();
    }

    class DisconnectWSInBackground extends AsyncTask<Void, Void, Void> {
        @Override
        protected Void doInBackground(Void... params) {
            try {
                mFirehoseWS.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return null;
        }


        @Override
        protected void onPostExecute(Void unused) {
            mFirehoseWS = null;
        }

    }
}
