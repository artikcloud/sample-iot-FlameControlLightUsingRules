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

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;

public class SAMILoginActivity extends Activity {
    static final String TAG = "SAMILoginActivity";

    private View mLoginView;
    private WebView mWebView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        mWebView = (WebView)findViewById(R.id.webview);
        mWebView.setVisibility(View.GONE);
        mLoginView = findViewById(R.id.ask_for_login);
        mLoginView.setVisibility(View.VISIBLE);
        Button button = (Button)findViewById(R.id.btn);

        Log.v(TAG, "::onCreate");
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                try {
                    Log.v(TAG, ": button is clicked.");
                    loadWebView();
                } catch (Exception e) {
                    Log.v(TAG, "Run into Exception");
                    e.printStackTrace();
                }
            }
        });

        // Reset to start a new session cleanly
        SAMISession.getInstance().reset();
    }

    @SuppressLint("SetJavaScriptEnabled")
    private void loadWebView() {
        Log.v(TAG, "::loadWebView");
        mLoginView.setVisibility(View.GONE);
        mWebView.setVisibility(View.VISIBLE);
        mWebView.getSettings().setJavaScriptEnabled(true);

        mWebView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String uri) {
                if (uri.startsWith(SAMISession.REDIRECT_URL)) {
                    // Redirect URL has format http://localhost:81/samidemo/index.php#expires_in=1209600&token_type=bearer&access_token=xxxx
                    // Extract OAuth2 access_token in URL
                    if (uri.indexOf("access_token=") != -1) {
                        String accessToken;
                        String containingStr = uri.split("access_token=")[1];
                        if (containingStr.indexOf("&") != -1) {
                            accessToken = containingStr.split("&")[0];
                        } else {
                            accessToken = containingStr;
                        }
                        onGetAccessToken(accessToken);
                    }
                    return true;
                }
                // Load the web page from URL (login and grant access)
                return super.shouldOverrideUrlLoading(view, uri);
            }
        });

        String url = SAMISession.getInstance().getAuthorizationRequestUri();
        Log.v(TAG, "webview loading url: " + url);
        mWebView.loadUrl(url);
    }


    private void onGetAccessToken(String accessToken)
    {
        Log.d(TAG, "onGetAccessToken(" + accessToken +")");
        SAMISession.getInstance().setAccessToken(accessToken);
        SAMISession.getInstance().setupSamiRestApis();
        startSamiActivity();
    }

    private void startSamiActivity() {
        Intent activityIntent = new Intent(this, RuleActivity.class);
        startActivity(activityIntent);
    }

}
