/*
Copyright (c) Microsoft Open Technologies, Inc.
All Rights Reserved
Apache 2.0 License
 
   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at
 
     http://www.apache.org/licenses/LICENSE-2.0
 
   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 
See the Apache Version 2.0 License for specific language governing permissions and limitations under the License.
 */

/**
 * ApiJsonOperationCallback.java
 */

package com.microsoft.windowsazure.mobileservices;

import com.google.gson.JsonElement;
import com.microsoft.windowsazure.mobileservices.http.ServiceFilterResponse;

/**
 * Callback used after a custom Api invocation is executed using JSON
 */
public interface ApiJsonOperationCallback {
    /**
     * Method to call when the operation is completed
     *
     * @param jsonElement The obtained jsonElement
     * @param exception   An exception representing the error, in case there was one
     * @param response    Response object
     */
    public void onCompleted(JsonElement jsonElement, Exception exception, ServiceFilterResponse response);
}