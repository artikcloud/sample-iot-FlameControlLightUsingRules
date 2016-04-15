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

import java.util.Map;

public class Rule {
    private String id = null;
    private String name = null;
    private String uid = null;
    private String aid = null;
    private String description = null;
    private Map<String, Object> rule = null;// ruleJSONBody
    private boolean enabled = true;
    private int languageVersion = 1;

    public String getId() {
        return id;
    }
    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }

    /**
     **/
    public String getUid() {
        return uid;
    }
    public void setUid(String uid) {
        this.uid = uid;
    }

    /**
     **/
    public String getAid() {
        return aid;
    }
    public void setAid(String aid) {
        this.aid = aid;
    }

    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }

    public Map getRule() {
        return rule;
    }
    public void setRule(Map<String, Object> rule) {
        this.rule = rule;
    }

    public Boolean getEnabled() {
        return enabled;
    }
    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public int getLanguageVersion() {
        return languageVersion;
    }
    public void setLanguageVersion(int languageVersion) {
        this.languageVersion = languageVersion;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("class rule {\n");
        sb.append("  id: ").append(id).append("\n");
        sb.append("  uid: ").append(uid).append("\n");
        sb.append("  name: ").append(name).append("\n");
        sb.append("  description: ").append(description).append("\n");
        sb.append("  rule:").append(rule).append("\n");
        sb.append("  enabled: ").append(enabled).append("\n");
        sb.append("  languageVersion: ").append(languageVersion).append("\n");
        sb.append("}\n");
        return sb.toString();
    }
}
