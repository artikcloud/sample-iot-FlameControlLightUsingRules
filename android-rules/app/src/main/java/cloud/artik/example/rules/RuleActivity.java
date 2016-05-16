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

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import cloud.artik.client.ApiCallback;
import cloud.artik.client.ApiException;
import cloud.artik.client.JSON;
import cloud.artik.model.OutputRule;
import cloud.artik.model.RuleCreationInfo;
import cloud.artik.model.RuleEnvelope;
import cloud.artik.model.RulesEnvelope;
import cloud.artik.model.User;
import cloud.artik.model.UserEnvelope;


public class RuleActivity extends Activity {
    private static final String TAG = RuleActivity.class.getSimpleName();
    private TextView mSmartLightDeviceStatus;
    private TextView mSmartLightStatusUpdateTime;
    private TextView mWebSocketLiveStatus;
    private TextView mFireDetectorDeviceStatus;
    private TextView mFireDetectorStatusUpdateTime;
    private TextView mRulesAPICallResponse;
    private Button mCreateRuleBtn;
    private Button mDeleteRuleBtn;
    private Button mGetRulesBtn;
    private String[] mRuleIds = null;
    private int mRuleIdx = 0; // The index of the current rule in mRuleIds[]

    private static final String LIVE_HEADER = "WebSocket /live: ";
    private static final String CONNECTED = "connected ";
    private static final int NUM_OF_RULES = 2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        TextView smartLightDeviceName;
        TextView smartLightDeviceID;
        TextView fireDetectorDeviceName;
        TextView fireDetectorDeviceID;

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mRulesAPICallResponse = (TextView)findViewById(R.id.rules_apicall_response);

        smartLightDeviceName = (TextView)findViewById(R.id.smart_light_device_name);
        smartLightDeviceID = (TextView)findViewById(R.id.smart_light_device_id);
        mSmartLightDeviceStatus = (TextView)findViewById(R.id.smart_light_device_status);
        mSmartLightStatusUpdateTime = (TextView)findViewById(R.id.smart_light_status_update_time);
        mWebSocketLiveStatus = (TextView)findViewById(R.id.websocket_live_status);

        fireDetectorDeviceName = (TextView)findViewById(R.id.fire_detector_device_name);
        fireDetectorDeviceID = (TextView)findViewById(R.id.fire_detector_device_id);
        mFireDetectorDeviceStatus = (TextView)findViewById(R.id.fire_detector_device_status);
        mFireDetectorStatusUpdateTime = (TextView)findViewById(R.id.fire_detector_status_update_time);

        setTitle(R.string.device_monitor_title);

        ArtikCloudSession.getInstance().setContext(this);
        smartLightDeviceID.setText("Device ID: " + ArtikCloudSession.SMART_LIGHT_DEVICE_ID);
        smartLightDeviceName.setText("Device Name: " + ArtikCloudSession.SMART_LIGHT_DEVICE_NAME);
        fireDetectorDeviceID.setText("Device ID: " + ArtikCloudSession.FIRE_DETECTOR_DEVICE_ID);
        fireDetectorDeviceName.setText("Device Name: " + ArtikCloudSession.FIRE_DETECTOR_DEVICE_NAME);

        mCreateRuleBtn = (Button)findViewById(R.id.createRuleBtn);
        mCreateRuleBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                try {
                    Log.v(TAG, ": create a rule button is clicked.");
                    mRuleIdx = 0; //reset the index, starting from zero
                    createRules();
                } catch (Exception e) {
                    Log.v(TAG, "Run into Exception");
                    e.printStackTrace();
                }
            }
        });

        mGetRulesBtn = (Button)findViewById(R.id.getRulesBtn);
        mGetRulesBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                try {
                    Log.v(TAG, ": getRules button is clicked.");
                    getRules();
                } catch (Exception e) {
                    Log.v(TAG, "Run into Exception");
                    e.printStackTrace();
                }
            }

            }
        );

        mDeleteRuleBtn = (Button)findViewById(R.id.deleteRuleBtn);
        mDeleteRuleBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                try {
                    Log.v(TAG, ": delete button is clicked.");
                    mRuleIdx = 0; //reset the index, starting from zero
                    deleteRule();
                } catch (Exception e) {
                    Log.v(TAG, "Run into Exception");
                    e.printStackTrace();
                }
            }
        });

        getUserInfo();

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(this).registerReceiver(mWSUpdateReceiver,
                makeWebsocketUpdateIntentFilter());
        ArtikCloudSession.getInstance().connectFirehoseWS();//non blocking
        if (ArtikCloudSession.getInstance().canCallRulesApi()) {
            mGetRulesBtn.setEnabled(true);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        ArtikCloudSession.getInstance().disconnectFirehoseWS();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mWSUpdateReceiver);
    }

    @Override
    public void onBackPressed()
    {
        // Disable going back to the previous screen
    }

    private static IntentFilter makeWebsocketUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(ArtikCloudSession.WEBSOCKET_LIVE_ONOPEN);
        intentFilter.addAction(ArtikCloudSession.WEBSOCKET_LIVE_ONMSG);
        intentFilter.addAction(ArtikCloudSession.WEBSOCKET_LIVE_ONCLOSE);
        intentFilter.addAction(ArtikCloudSession.WEBSOCKET_LIVE_ONERROR);
        return intentFilter;
    }

    private final BroadcastReceiver mWSUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();
            if (ArtikCloudSession.WEBSOCKET_LIVE_ONOPEN.equals(action)) {
                displayLiveStatus(LIVE_HEADER + CONNECTED);
            } else if (ArtikCloudSession.WEBSOCKET_LIVE_ONMSG.equals(action)) {
                int deviceIndex = intent.getIntExtra(ArtikCloudSession.DEVICE_INDEX, -1);
                 String status = intent.getStringExtra(ArtikCloudSession.DEVICE_DATA);
                String updateTime = intent.getStringExtra(ArtikCloudSession.TIMESTEP);
                displayDeviceStatus(deviceIndex, status, updateTime);
            } else if (ArtikCloudSession.WEBSOCKET_LIVE_ONCLOSE.equals(action) ||
                ArtikCloudSession.WEBSOCKET_LIVE_ONERROR.equals(action)) {
                displayLiveStatus(LIVE_HEADER + intent.getStringExtra(ArtikCloudSession.ERROR));
            }
        }
    };

    private void displayLiveStatus(String status) {
        Log.d(TAG, status);
        mWebSocketLiveStatus.setText(status);
    }

    private void displayDeviceStatus(int idx, String status, String updateTimems) {
        long time_ms = Long.parseLong(updateTimems);
        String tmsStr = DateFormat.getDateTimeInstance().format(new Date(time_ms));
        switch (idx) {
            case 0: //fire detector
                mFireDetectorDeviceStatus.setText(status);
                mFireDetectorStatusUpdateTime.setText(tmsStr);
                break;
            case 1: //smart light
                mSmartLightDeviceStatus.setText(status);
                mSmartLightStatusUpdateTime.setText(tmsStr);
                break;
            default:
                Log.w(TAG, "displayDeviceStatus received invalid device index " + idx);
        }
    }

    private void getUserInfo()
    {
        final String tag = TAG + " getSelfAsync";
        try {
            ArtikCloudSession.getInstance().getUsersApi().getSelfAsync(new ApiCallback<UserEnvelope>() {
                @Override
                public void onFailure(ApiException exc, int statusCode, Map<String, List<String>> map) {
                    processFailure(tag, exc);
                }

                @Override
                public void onSuccess(UserEnvelope result, int statusCode, Map<String, List<String>> map) {
                    Log.v(tag, " onSuccess() self name = " + result.getData().getFullName());
                    handleGetUserInfoSuccessOnUIThread(result.getData());
                }

                @Override
                public void onUploadProgress(long bytes, long contentLen, boolean done) {
                }

                @Override
                public void onDownloadProgress(long bytes, long contentLen, boolean done) {
                }
            });
        } catch (ApiException exc) {
            processFailure(tag, exc);
        }
    }

    private void handleGetUserInfoSuccessOnUIThread(final User user) {
        if (user == null) {
            return;
        }
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                displayLiveStatus("Start connecting to /live for " + user.getFullName());
                ArtikCloudSession.getInstance().setUserId(user.getId());
                ArtikCloudSession.getInstance().connectFirehoseWSBlocking();
                if (ArtikCloudSession.getInstance().canCallRulesApi()) {
                    mGetRulesBtn.setEnabled(true);
                }

            }
        });
    }

////// RULES
    private void createRules() {
        final String tag = TAG + " createRuleAsync";
        RuleCreationInfo rule = generateARule();
        try {
            ArtikCloudSession.getInstance().getRulesApi().createRuleAsync(rule,
                    ArtikCloudSession.getInstance().getUserId(),
                    new ApiCallback<RuleEnvelope>() {
                        @Override
                        public void onFailure(ApiException exc, int statusCode, Map<String, List<String>> map) {
                            processFailure(tag, exc);
                        }

                        @Override
                        public void onSuccess(RuleEnvelope result, int statusCode, Map<String, List<String>> map) {
                            Log.v(tag, "response after posting a rule: " + result);
                            handleRuleCreationSuccessOnUIThread(result.getData());
                        }

                        @Override
                        public void onUploadProgress(long bytes, long contentLen, boolean done) {
                        }

                        @Override
                        public void onDownloadProgress(long bytes, long contentLen, boolean done) {
                        }
                    });
        } catch (ApiException exc) {
            processFailure(tag, exc);
        }
    }

    private void getRules() {
        final String tag = TAG + " getUserRuleAsync";
        try {
            ArtikCloudSession.getInstance().getUsersApi().getUserRulesAsync(
                    ArtikCloudSession.getInstance().getUserId(), true, false, null, null,
                    new ApiCallback<RulesEnvelope>() {
                        @Override
                        public void onFailure(ApiException exc, int statusCode, Map<String, List<String>> map) {
                            processFailure(tag, exc);
                        }

                        @Override
                        public void onSuccess(RulesEnvelope result, int statusCode, Map<String, List<String>> map) {
                            Log.v(tag, "response after getting rules: " + result);
                            handleGetRulesSuccessOnUIThread(result);
                        }

                        @Override
                        public void onUploadProgress(long bytes, long contentLen, boolean done) {
                        }

                        @Override
                        public void onDownloadProgress(long bytes, long contentLen, boolean done) {
                        }
                    });
        } catch (ApiException exc) {
            processFailure(tag, exc);
        }
    }

    private void deleteRule(){
        final String tag = TAG + " deleteRuleAsync";
        try {
            ArtikCloudSession.getInstance().getRulesApi().deleteRuleAsync(
                    mRuleIds[mRuleIdx],
                    new ApiCallback<RuleEnvelope>() {
                        @Override
                        public void onFailure(ApiException exc, int statusCode, Map<String, List<String>> map) {
                            processFailure(tag, exc);
                        }

                        @Override
                        public void onSuccess(RuleEnvelope result, int statusCode, Map<String, List<String>> map) {
                            Log.v(tag, "response deleting a rule: " + result);
                            handleDeleteRulesSuccessOnUIThread(result);
                        }

                        @Override
                        public void onUploadProgress(long bytes, long contentLen, boolean done) {
                        }

                        @Override
                        public void onDownloadProgress(long bytes, long contentLen, boolean done) {
                        }
                    });
        } catch (ApiException exc) {
            processFailure(tag, exc);
        }
    }


    private RuleCreationInfo generateARule() {
        RuleCreationInfo rule = new RuleCreationInfo();
        boolean onFireTriggerValue;
        String actionName;
        switch (mRuleIdx) {
            case 0: //if true then on
                rule.setName("light on rule");
                rule.setDescription("If onFire is true, turn on the light");
                onFireTriggerValue = true;
                actionName = "setOn";
                break;
            case 1: //if false then off
                rule.setName("light off rule");
                rule.setDescription("If onFire is false, turn off the light");
                onFireTriggerValue = false;
                actionName = "setOff";
                break;
            default:
                Log.e("","generateARule ran into invalid mRuleIdx " + mRuleIdx);
                return null;
        }
        HashMap<String, Object> ruleBody = new HashMap<>();

        Object[] arrayToPut = new Object[1];
        HashMap<String, Object> and = new HashMap<>();
        HashMap<String, Object> andElem = new HashMap<>();
        HashMap<String, Object> valueObj = new HashMap<>();
        valueObj.put("value", onFireTriggerValue);
        andElem.put("sdid", "45176de99e424d98b1a3c42558bfccf4");
        andElem.put("field", "onFire");
        andElem.put("operator", "=");
        andElem.put("operand", valueObj);
        arrayToPut[0] = andElem;
        and.put("and", arrayToPut);
        ruleBody.put("if", and);

        HashMap<String, Object> thenElem = new HashMap<>();
        thenElem.put("ddid", "0f8b470c6e214a76b914bc864e2c2b6b");
        thenElem.put("action", actionName);
        thenElem.put("parameters", null);
        Object[] thensToPut = new Object[1];
        thensToPut[0] = thenElem;
        ruleBody.put("then", thensToPut);

        rule.setRule(ruleBody);
        try {
            JSON json = new JSON(ArtikCloudSession.getInstance().getUsersApi().getApiClient());
            String ruleString = json.serialize(rule);
            Log.v("", "generateARule rule:" + ruleString);
        } catch (Exception e) {
            Log.v("", "generateARule run into Exception");
            e.printStackTrace();
        }
        return rule;
    }


///// Helpers
    private void processFailure(final String context, ApiException exc) {
        String errorDetail = " onFailure with exception" + exc;
        Log.w(context, errorDetail);
        exc.printStackTrace();
        showErrorOnUIThread(context+errorDetail, RuleActivity.this);
    }

    static void showErrorOnUIThread(final String text, final Activity activity) {
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                int duration = Toast.LENGTH_LONG;
                Toast toast = Toast.makeText(activity.getApplicationContext(), text, duration);
                toast.show();
            }
        });
    }

    private void handleRuleCreationSuccessOnUIThread(final OutputRule ruleObj) {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mRuleIds == null) {
                    mRuleIds = new String[NUM_OF_RULES];
                }
                mRuleIds[mRuleIdx] = ruleObj.getId();
                mRuleIdx++;
                if (mRuleIdx < NUM_OF_RULES) {
                    createRules();
                } else {
                    handleBtnsEnable();
                }
                displayRulesApiCallResponse(" Response after creating rule " + (mRuleIdx - 1) + ":\n" + ruleObj.toString());
            }
        });
   }

   private void handleGetRulesSuccessOnUIThread(final RulesEnvelope rules) {
       this.runOnUiThread(new Runnable() {
           @Override
           public void run() {
               String displayInfo = rules.toString();
               int receivedRules = rules.getCount();
               mRuleIdx = 0;
               if (receivedRules <= 0) {
                   mRuleIds = null;
               } else {
                   List<OutputRule> rulesObj = rules.getData();
                   int count = receivedRules > NUM_OF_RULES? NUM_OF_RULES : receivedRules;
                   mRuleIds = new String[NUM_OF_RULES];
                   displayInfo = "total:" + rules.getTotal() + ", count:" + receivedRules +'\n';

                   for (int i = 0; i < count; i++) {
                       OutputRule thisRule = rulesObj.get(i);
                       mRuleIds[i] = thisRule.getId();
                       displayInfo += "id:" + mRuleIds[i] + '\n';
                       displayInfo += "description:" + thisRule.getDescription() +'\n';
                   }
               }
               handleBtnsEnable();
               displayRulesApiCallResponse("Rules:\n" + displayInfo);

           }
       });
   }

    private void handleDeleteRulesSuccessOnUIThread(final RuleEnvelope rule) {
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (mRuleIdx == NUM_OF_RULES - 1) { //This is the last rule to delete
                    mRuleIds = null;
                    mRuleIdx = 0;
                    handleBtnsEnable();
                } else {
                    mRuleIdx++;
                    Log.d(TAG, "prepare to delete rule with idx " + mRuleIdx);
                    deleteRule();
                }
                displayRulesApiCallResponse("response after deleting a rule :\n" + rule.toString());

            }
        });
    }
    private void displayRulesApiCallResponse(String result) {
        Log.d(TAG, result);
        mRulesAPICallResponse.setText(null);//clean previous text
        mRulesAPICallResponse.setText(result);
    }

    private void handleBtnsEnable() {
        if (mRuleIds == null) {
            mCreateRuleBtn.setEnabled(true);
            mDeleteRuleBtn.setEnabled(false);
        } else {
            mCreateRuleBtn.setEnabled(false);
            mDeleteRuleBtn.setEnabled(true);
        }
    }

}
