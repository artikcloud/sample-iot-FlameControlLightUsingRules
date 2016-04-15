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

import java.util.HashMap;
import java.util.Map;

import io.samsungsami.client.ApiException;
import io.samsungsami.client.ApiInvoker;

public class RulesApi {
    String basePath = null;
    ApiInvoker apiInvoker = ApiInvoker.getInstance();

    public void addHeader(String key, String value) {
        getInvoker().addDefaultHeader(key, value);
    }

    private ApiInvoker getInvoker() {
        return apiInvoker;
    }

    public void setBasePath(String basePath) {
        this.basePath = basePath;
    }

    public String postRule(Rule rule) throws ApiException {
        Object postBody = rule;

        // create path and map variables
        String path = "/rules".replaceAll("\\{format\\}","json");

        // query params
        Map<String, String> queryParams = new HashMap<>();
        Map<String, String> headerParams = new HashMap<>();

        String contentType = "application/json";

        try {
            String response = apiInvoker.invokeAPI(basePath, path, "POST", queryParams, postBody, headerParams, contentType);
            return response;
        } catch (ApiException ex) {
            if(ex.getCode() == 404) {
                return  null;
            }
            else {
                throw ex;
            }
        }
    }

    public String getRules() throws ApiException {
        Object postBody = null;

        // create path and map variables
        String path = "/rules".replaceAll("\\{format\\}","json");

        // query params
        Map<String, String> queryParams = new HashMap<>();
        Map<String, String> headerParams = new HashMap<>();

        String contentType = "application/json";

        try {
            String response = apiInvoker.invokeAPI(basePath, path, "GET", queryParams, postBody, headerParams, contentType);
            return response;
        } catch (ApiException ex) {
            if(ex.getCode() == 404) {
                return  null;
            }
            else {
                throw ex;
            }
        }
    }

    public String deleteRule(String ruleId) throws ApiException {
        Integer postBody = null;

        // create path and map variables
        String path = "/rules/{ruleId}".replaceAll("\\{format\\}","json").replaceAll("\\{" + "ruleId" + "\\}", apiInvoker.escapeString(ruleId.toString()));

        // query params
        Map<String, String> queryParams = new HashMap<>();
        Map<String, String> headerParams = new HashMap<>();

        String contentType = "application/json";

        try {
            String response = apiInvoker.invokeAPI(basePath, path, "DELETE", queryParams, postBody, headerParams, contentType);
            return response;
        } catch (ApiException ex) {
            if(ex.getCode() == 404) {
                return  null;
            } else {
                throw ex;
            }
        }
    }

}
