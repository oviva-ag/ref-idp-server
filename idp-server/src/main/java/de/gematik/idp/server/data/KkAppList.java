/*
 * Copyright (c) 2022 gematik GmbH
 * 
 * Licensed under the Apache License, Version 2.0 (the License);
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an 'AS IS' BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.gematik.idp.server.data;

import java.util.ArrayList;
import java.util.List;
import kong.unirest.json.JSONArray;
import kong.unirest.json.JSONObject;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class KkAppList {

    private final List<KkAppListEntry> appList = new ArrayList<>();

    public void add(final KkAppListEntry toBeAdded) {
        appList.add(toBeAdded);
    }

    public JSONObject getListAsJson() {
        final JSONObject jsonKkApplist = new JSONObject();
        final JSONArray jsonArr = new JSONArray();
        appList.forEach(e -> {
                final JSONObject jsonEntry = new JSONObject();
                jsonEntry.put("kk_app_name", e.getKkAppName());
                jsonEntry.put("kk_app_id", e.getKkAppId());
                jsonArr.put(jsonEntry);
            }
        );
        jsonKkApplist.put("kk_app_list", jsonArr);
        return jsonKkApplist;
    }

    public String getAppUri(final String kkAppId) {
        return appList.stream()
            .filter(e -> kkAppId.equals(e.getKkAppId()))
            .map(KkAppListEntry::getKkAppUri)
            .findFirst()
            .orElseThrow();
    }
}
