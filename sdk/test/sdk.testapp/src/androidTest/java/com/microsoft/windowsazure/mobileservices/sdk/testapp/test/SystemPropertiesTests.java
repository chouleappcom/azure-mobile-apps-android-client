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
package com.microsoft.windowsazure.mobileservices.sdk.testapp.test;

import android.test.InstrumentationTestCase;
import android.util.Pair;

import com.google.common.util.concurrent.ListenableFuture;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.microsoft.windowsazure.mobileservices.MobileServiceClient;
import com.microsoft.windowsazure.mobileservices.http.NextServiceFilterCallback;
import com.microsoft.windowsazure.mobileservices.http.ServiceFilter;
import com.microsoft.windowsazure.mobileservices.http.ServiceFilterRequest;
import com.microsoft.windowsazure.mobileservices.http.ServiceFilterResponse;
import com.microsoft.windowsazure.mobileservices.sdk.testapp.framework.filters.ServiceFilterRequestMock;
import com.microsoft.windowsazure.mobileservices.sdk.testapp.framework.filters.ServiceFilterResponseMock;
import com.microsoft.windowsazure.mobileservices.sdk.testapp.test.types.SystemPropertyTestClasses.AllSystemPropertiesType;
import com.microsoft.windowsazure.mobileservices.sdk.testapp.test.types.SystemPropertyTestClasses.CreatedAtType;
import com.microsoft.windowsazure.mobileservices.sdk.testapp.test.types.SystemPropertyTestClasses.NamedSystemPropertiesType;
import com.microsoft.windowsazure.mobileservices.sdk.testapp.test.types.SystemPropertyTestClasses.NotSystemPropertyCreatedAtType;
import com.microsoft.windowsazure.mobileservices.sdk.testapp.test.types.SystemPropertyTestClasses.NotSystemPropertyUpdatedAtType;
import com.microsoft.windowsazure.mobileservices.sdk.testapp.test.types.SystemPropertyTestClasses.NotSystemPropertyVersionType;
import com.microsoft.windowsazure.mobileservices.sdk.testapp.test.types.SystemPropertyTestClasses.StringCreatedAtType;
import com.microsoft.windowsazure.mobileservices.sdk.testapp.test.types.SystemPropertyTestClasses.StringUpdatedAtType;
import com.microsoft.windowsazure.mobileservices.sdk.testapp.test.types.SystemPropertyTestClasses.UpdatedAtType;
import com.microsoft.windowsazure.mobileservices.sdk.testapp.test.types.SystemPropertyTestClasses.VersionType;
import com.microsoft.windowsazure.mobileservices.sdk.testapp.test.types.data.SystemPropertiesTestData;
import com.microsoft.windowsazure.mobileservices.table.MobileServiceJsonTable;
import com.microsoft.windowsazure.mobileservices.table.MobileServiceSystemProperty;
import com.microsoft.windowsazure.mobileservices.table.MobileServiceTable;
import okhttp3.Headers;
import okhttp3.Protocol;
import okhttp3.internal.http.StatusLine;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TimeZone;
import java.util.TreeMap;

public class SystemPropertiesTests extends InstrumentationTestCase {
    String appUrl = "";
    GsonBuilder gsonBuilder;

    protected void setUp() throws Exception {
        appUrl = "http://myapp.com/";
        gsonBuilder = new GsonBuilder();
        super.setUp();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
    }

    // Non Generic JSON

    // Insert Tests

    public void testInsertDoesNotRemoveNonSystemPropertiesWhenIdIsString() throws Throwable {
        String[] nonSystemProperties = SystemPropertiesTestData.NonSystemProperties;

        for (String nonSystemProperty : nonSystemProperties) {
            insertDoesNotRemovePropertyWhenIdIsString(nonSystemProperty);
        }
    }

    private void insertDoesNotRemovePropertyWhenIdIsString(final String property) throws Throwable {
        final String tableName = "MyTableName";

        final String jsonTestSystemProperty = property.replace("\\", "\\\\").replace("\"", "\\\"");
        final String responseContent = "{\"id\":\"an id\",\"String\":\"Hey\"}";

        MobileServiceClient client = null;

        try {
            client = new MobileServiceClient(appUrl, getInstrumentation().getTargetContext());
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        // Add a filter to handle the request and create a new json
        // object with an id defined
        client = client.withFilter(getTestFilter(responseContent));

        client = client.withFilter(new ServiceFilter() {
            @Override
            public ListenableFuture<ServiceFilterResponse> handleRequest(ServiceFilterRequest request, NextServiceFilterCallback nextServiceFilterCallback) {
                String content = request.getContent();
                JsonObject obj = new JsonParser().parse(content).getAsJsonObject();

                Map<String, JsonElement> properties = new TreeMap<String, JsonElement>(String.CASE_INSENSITIVE_ORDER);

                for (Entry<String, JsonElement> entry : obj.entrySet()) {
                    properties.put(entry.getKey(), entry.getValue());
                }

                assertTrue(properties.containsKey("id"));
                assertTrue(properties.containsKey("String"));
                assertTrue(properties.containsKey(property));

                return nextServiceFilterCallback.onNext(request);
            }
        });

        // Create get the MobileService table
        MobileServiceJsonTable msTable = client.getTable(tableName);

        JsonObject obj = new JsonParser().parse("{\"id\":\"an id\",\"String\":\"what\",\"" + jsonTestSystemProperty + "\":\"a value\"}").getAsJsonObject();

        try {
            // Call the insert method
            JsonObject jsonObject = msTable.insert(obj).get();

            // Asserts
            if (jsonObject == null) {
                fail("Expected result");
            }

        } catch (Exception exception) {
            fail(exception.getMessage());
        }
    }

    public void testInsertDoesNotRemovesNonSystemPropertiesWhenIdIsNull() throws Throwable {
        String[] nonSystemProperties = SystemPropertiesTestData.NonSystemProperties;

        for (String nonSystemProperty : nonSystemProperties) {
            insertDoesNotRemovePropertyWhenIdIsNull(nonSystemProperty);
        }
    }

    private void insertDoesNotRemovePropertyWhenIdIsNull(final String property) throws Throwable {

        final String tableName = "MyTableName";

        final String jsonTestSystemProperty = property.replace("\\", "\\\\").replace("\"", "\\\"");
        final String responseContent = "{\"id\":\"an id\",\"String\":\"Hey\"}";

        MobileServiceClient client = null;

        try {
            client = new MobileServiceClient(appUrl, getInstrumentation().getTargetContext());
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        // Add a filter to handle the request and create a new json
        // object with an id defined
        client = client.withFilter(getTestFilter(responseContent));

        client = client.withFilter(new ServiceFilter() {
            @Override
            public ListenableFuture<ServiceFilterResponse> handleRequest(ServiceFilterRequest request, NextServiceFilterCallback nextServiceFilterCallback) {
                String content = request.getContent();
                JsonObject obj = new JsonParser().parse(content).getAsJsonObject();

                Map<String, JsonElement> properties = new TreeMap<String, JsonElement>(String.CASE_INSENSITIVE_ORDER);

                for (Entry<String, JsonElement> entry : obj.entrySet()) {
                    properties.put(entry.getKey(), entry.getValue());
                }

                assertFalse(properties.containsKey("id"));
                assertTrue(properties.containsKey("String"));
                assertTrue(properties.containsKey(property));

                return nextServiceFilterCallback.onNext(request);
            }
        });

        // Create get the MobileService table
        MobileServiceJsonTable msTable = client.getTable(tableName);

        JsonObject obj = new JsonParser().parse("{\"id\":null,\"String\":\"what\",\"" + jsonTestSystemProperty + "\":\"a value\"}").getAsJsonObject();

        try {
            // Call the insert method
            JsonObject jsonObject = msTable.insert(obj).get();

            // Asserts
            if (jsonObject == null) {
                fail("Expected result");
            }

        } catch (Exception exception) {
            fail(exception.getMessage());
        }
    }


    public void testInsertUserParameterWithSystemPropertiesIntId() throws Throwable {
        List<EnumSet<MobileServiceSystemProperty>> allSystemProperties = SystemPropertiesTestData.AllSystemProperties;

        for (EnumSet<MobileServiceSystemProperty> systemProperties : allSystemProperties) {
            insertUserParameterWithSystemPropertiesIntId(systemProperties);
        }
    }

    private void insertUserParameterWithSystemPropertiesIntId(final EnumSet<MobileServiceSystemProperty> systemProperties) throws Throwable {

        final String tableName = "MyTableName";

        final String responseContent = "{\"id\":5,\"String\":\"Hey\"}";
        final JsonObject obj = new JsonParser().parse("{\"String\":\"what\"}").getAsJsonObject();

        MobileServiceClient client = null;

        try {
            client = new MobileServiceClient(appUrl, getInstrumentation().getTargetContext());
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        // Add a filter to handle the request and create a new json
        // object with an id defined
        client = client.withFilter(getTestFilter(responseContent));

        client = client.withFilter(new ServiceFilter() {
            @Override
            public ListenableFuture<ServiceFilterResponse> handleRequest(ServiceFilterRequest request, NextServiceFilterCallback nextServiceFilterCallback) {

                assertTrue(request.getUrl().contains("__systemproperties=createdAt"));

                return nextServiceFilterCallback.onNext(request);
            }
        });

        // Create get the MobileService table
        MobileServiceJsonTable msTable = client.getTable(tableName);



        List<Pair<String, String>> parameters = new ArrayList<Pair<String, String>>();
        parameters.add(new Pair<String, String>("__systemproperties", "createdAt"));

        try {
            // Call the insert method
            JsonObject jsonObject = msTable.insert(obj, parameters).get();

            // Asserts
            if (jsonObject == null) {
                fail("Expected result");
            }

        } catch (Exception exception) {
            fail(exception.getMessage());
        }
    }

    // Update Tests

    public void testUpdateRemovesSystemPropertiesWhenIdIsString() throws Throwable {
        String[] systemProperties = SystemPropertiesTestData.ValidSystemProperties;

        for (String systemProperty : systemProperties) {
            updateRemovesPropertyWhenIdIsString(systemProperty);
        }
    }

    private void updateRemovesPropertyWhenIdIsString(final String property) throws Throwable {

        final String tableName = "MyTableName";

        final String jsonTestSystemProperty = property.replace("\\", "\\\\").replace("\"", "\\\"");
        final String responseContent = "{\"id\":\"an id\",\"String\":\"Hey\"}";

        MobileServiceClient client = null;

        try {
            client = new MobileServiceClient(appUrl, getInstrumentation().getTargetContext());
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        // Add a filter to handle the request and create a new json
        // object with an id defined
        client = client.withFilter(getTestFilter(responseContent));

        client = client.withFilter(new ServiceFilter() {
            @Override
            public ListenableFuture<ServiceFilterResponse> handleRequest(ServiceFilterRequest request, NextServiceFilterCallback nextServiceFilterCallback) {
                String content = request.getContent();
                JsonObject obj = new JsonParser().parse(content).getAsJsonObject();

                Map<String, JsonElement> properties = new TreeMap<String, JsonElement>(String.CASE_INSENSITIVE_ORDER);

                for (Entry<String, JsonElement> entry : obj.entrySet()) {
                    properties.put(entry.getKey(), entry.getValue());
                }

                assertTrue(properties.containsKey("id"));
                assertTrue(properties.containsKey("String"));
                assertFalse(properties.containsKey(property));

                return nextServiceFilterCallback.onNext(request);
            }
        });

        // Create get the MobileService table
        MobileServiceJsonTable msTable = client.getTable(tableName);

        JsonObject obj = new JsonParser().parse("{\"id\":\"an id\",\"String\":\"what\",\"" + jsonTestSystemProperty + "\":\"a value\"}").getAsJsonObject();

        try {
            // Call the update method
            JsonObject jsonObject = msTable.update(obj).get();

            // Asserts
            if (jsonObject == null) {
                fail("Expected result");
            }

        } catch (Exception exception) {
            fail(exception.getMessage());
        }
    }

    public void testUpdateDoesNotRemoveNonSystemPropertiesWhenIdIsString() throws Throwable {
        String[] nonSystemProperties = SystemPropertiesTestData.NonSystemProperties;

        for (String nonSystemProperty : nonSystemProperties) {
            updateDoesNotRemovePropertyWhenIdIsString(nonSystemProperty);
        }
    }

    private void updateDoesNotRemovePropertyWhenIdIsString(final String property) throws Throwable {

        final String tableName = "MyTableName";

        final String jsonTestSystemProperty = property.replace("\\", "\\\\").replace("\"", "\\\"");
        final String responseContent = "{\"id\":\"an id\",\"String\":\"Hey\"}";

        MobileServiceClient client = null;

        try {
            client = new MobileServiceClient(appUrl, getInstrumentation().getTargetContext());
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        // Add a filter to handle the request and create a new json
        // object with an id defined
        client = client.withFilter(getTestFilter(responseContent));

        client = client.withFilter(new ServiceFilter() {
            @Override
            public ListenableFuture<ServiceFilterResponse> handleRequest(ServiceFilterRequest request, NextServiceFilterCallback nextServiceFilterCallback) {
                String content = request.getContent();
                JsonObject obj = new JsonParser().parse(content).getAsJsonObject();

                Map<String, JsonElement> properties = new TreeMap<String, JsonElement>(String.CASE_INSENSITIVE_ORDER);

                for (Entry<String, JsonElement> entry : obj.entrySet()) {
                    properties.put(entry.getKey(), entry.getValue());
                }

                assertTrue(properties.containsKey("id"));
                assertTrue(properties.containsKey("String"));
                assertTrue(properties.containsKey(property));

                return nextServiceFilterCallback.onNext(request);
            }
        });

        // Create get the MobileService table
        MobileServiceJsonTable msTable = client.getTable(tableName);

        JsonObject obj = new JsonParser().parse("{\"id\":\"an id\",\"String\":\"what\",\"" + jsonTestSystemProperty + "\":\"a value\"}").getAsJsonObject();

        try {
            // Call the update method
            JsonObject jsonObject = msTable.update(obj).get();

            // Asserts
            if (jsonObject == null) {
                fail("Expected result");
            }

        } catch (Exception exception) {
            fail(exception.getMessage());
        }
    }

    public void testUpdateDoesNotRemovesSystemPropertiesWhenIdIsInteger() throws Throwable {
        String[] systemProperties = SystemPropertiesTestData.ValidSystemProperties;

        for (String systemProperty : systemProperties) {
            updateDoesNotRemovePropertyWhenIdIsInteger(systemProperty);
        }
    }

    public void testUpdateDoesNotRemovesNonSystemPropertiesWhenIdIsInteger() throws Throwable {
        String[] nonSystemProperties = SystemPropertiesTestData.NonSystemProperties;

        for (String nonSystemProperty : nonSystemProperties) {
            updateDoesNotRemovePropertyWhenIdIsInteger(nonSystemProperty);
        }
    }

    private void updateDoesNotRemovePropertyWhenIdIsInteger(final String property) throws Throwable {

        final String tableName = "MyTableName";

        final String jsonTestSystemProperty = property.replace("\\", "\\\\").replace("\"", "\\\"");
        final String responseContent = "{\"id\":5,\"String\":\"Hey\"}";

        MobileServiceClient client = null;

        try {
            client = new MobileServiceClient(appUrl, getInstrumentation().getTargetContext());
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        // Add a filter to handle the request and create a new json
        // object with an id defined
        client = client.withFilter(getTestFilter(responseContent));

        client = client.withFilter(new ServiceFilter() {
            @Override
            public ListenableFuture<ServiceFilterResponse> handleRequest(ServiceFilterRequest request, NextServiceFilterCallback nextServiceFilterCallback) {
                String content = request.getContent();
                JsonObject obj = new JsonParser().parse(content).getAsJsonObject();

                Map<String, JsonElement> properties = new TreeMap<String, JsonElement>(String.CASE_INSENSITIVE_ORDER);

                for (Entry<String, JsonElement> entry : obj.entrySet()) {
                    properties.put(entry.getKey(), entry.getValue());
                }

                assertTrue(properties.containsKey("id"));
                assertTrue(properties.containsKey("String"));
                assertTrue(properties.containsKey(property));

                return nextServiceFilterCallback.onNext(request);
            }
        });

        // Create get the MobileService table
        MobileServiceJsonTable msTable = client.getTable(tableName);

        JsonObject obj = new JsonParser().parse("{\"id\":5,\"String\":\"what\",\"" + jsonTestSystemProperty + "\":\"a value\"}").getAsJsonObject();

        try {
            // Call the update method
            JsonObject jsonObject = msTable.update(obj).get();

            // Asserts
            if (jsonObject == null) {
                fail("Expected result");
            }

        } catch (Exception exception) {
            fail(exception.getMessage());
        }
    }

    public void testUpdateUserParameterWithSystemPropertiesIntId() throws Throwable {
        List<EnumSet<MobileServiceSystemProperty>> allSystemProperties = SystemPropertiesTestData.AllSystemProperties;

        for (EnumSet<MobileServiceSystemProperty> systemProperties : allSystemProperties) {
            updateUserParameterWithSystemPropertiesIntId(systemProperties);
        }
    }

    private void updateUserParameterWithSystemPropertiesIntId(final EnumSet<MobileServiceSystemProperty> systemProperties) throws Throwable {

        final String tableName = "MyTableName";

        final String responseContent = "{\"id\":5,\"String\":\"Hey\"}";
        final JsonObject obj = new JsonParser().parse("{\"id\":5,\"String\":\"what\"}").getAsJsonObject();

        MobileServiceClient client = null;

        try {
            client = new MobileServiceClient(appUrl, getInstrumentation().getTargetContext());
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        // Add a filter to handle the request and create a new json
        // object with an id defined
        client = client.withFilter(getTestFilter(responseContent));

        client = client.withFilter(new ServiceFilter() {
            @Override
            public ListenableFuture<ServiceFilterResponse> handleRequest(ServiceFilterRequest request, NextServiceFilterCallback nextServiceFilterCallback) {

                assertTrue(request.getUrl().contains("__systemproperties=createdAt"));

                return nextServiceFilterCallback.onNext(request);
            }
        });

        // Create get the MobileService table
        MobileServiceJsonTable msTable = client.getTable(tableName);



        List<Pair<String, String>> parameters = new ArrayList<Pair<String, String>>();
        parameters.add(new Pair<String, String>("__systemproperties", "createdAt"));

        try {
            // Call the update method
            JsonObject jsonObject = msTable.update(obj, parameters).get();

            // Asserts
            if (jsonObject == null) {
                fail("Expected result");
            }

        } catch (Exception exception) {
            fail(exception.getMessage());
        }
    }

    // Query Tests

    // Generic

    // Insert Tests

    // String Id Type

    public void testInsertRemovesCreatedAtSerializedNameStringId() throws Throwable {

        final String responseContent = "{\"id\":\"an id\"}";

        MobileServiceClient client = null;

        try {
            client = new MobileServiceClient(appUrl, getInstrumentation().getTargetContext());
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        // Add a filter to handle the request and create a new json
        // object with an id defined
        client = client.withFilter(getTestFilter(responseContent));

        client = client.withFilter(new ServiceFilter() {
            @Override
            public ListenableFuture<ServiceFilterResponse> handleRequest(ServiceFilterRequest request, NextServiceFilterCallback nextServiceFilterCallback) {
                String content = request.getContent();
                JsonObject obj = new JsonParser().parse(content).getAsJsonObject();

                Map<String, JsonElement> properties = new TreeMap<String, JsonElement>(String.CASE_INSENSITIVE_ORDER);

                for (Entry<String, JsonElement> entry : obj.entrySet()) {
                    properties.put(entry.getKey(), entry.getValue());
                }

                assertTrue(properties.containsKey("id"));
                assertFalse(properties.containsKey("createdAt"));

                return nextServiceFilterCallback.onNext(request);
            }
        });

        // Create get the MobileService table
        MobileServiceTable<CreatedAtType> msTable = client.getTable(CreatedAtType.class);

        CreatedAtType element = new CreatedAtType();
        element.Id = "an id";
        element.CreatedAt = new GregorianCalendar(2012, 00, 18).getTime();

        try {
            // Call the insert method
            CreatedAtType entity = msTable.insert(element).get();

            if (entity == null) {
                fail("Expected result");
            }

        } catch (Exception exception) {
            fail(exception.getMessage());
        }
    }

    public void testInsertRemovesCreatedAtPropertyNameStringId() throws Throwable {

        final String responseContent = "{\"id\":\"an id\"}";

        MobileServiceClient client = null;

        try {
            client = new MobileServiceClient(appUrl, getInstrumentation().getTargetContext());
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        // Add a filter to handle the request and create a new json
        // object with an id defined
        client = client.withFilter(getTestFilter(responseContent));

        client = client.withFilter(new ServiceFilter() {
            @Override
            public ListenableFuture<ServiceFilterResponse> handleRequest(ServiceFilterRequest request, NextServiceFilterCallback nextServiceFilterCallback) {
                String content = request.getContent();
                JsonObject obj = new JsonParser().parse(content).getAsJsonObject();

                Map<String, JsonElement> properties = new TreeMap<String, JsonElement>(String.CASE_INSENSITIVE_ORDER);

                for (Entry<String, JsonElement> entry : obj.entrySet()) {
                    properties.put(entry.getKey(), entry.getValue());
                }

                assertTrue(properties.containsKey("id"));
                assertFalse(properties.containsKey("createdAt"));

                return nextServiceFilterCallback.onNext(request);
            }
        });

        // Create get the MobileService table
        MobileServiceTable<NamedSystemPropertiesType> msTable = client.getTable(NamedSystemPropertiesType.class);

        NamedSystemPropertiesType element = new NamedSystemPropertiesType();
        element.Id = "an id";
        element.createdAt = new GregorianCalendar(2012, 00, 18).getTime();

        try {
            // Call the insert method
            NamedSystemPropertiesType entity = msTable.insert(element).get();

            if (entity == null) {
                fail("Expected result");
            }

        } catch (Exception exception) {
            fail(exception.getMessage());
        }
    }

    public void testInsertRemovesUpdatedAtSerializedNameStringId() throws Throwable {

        final String responseContent = "{\"id\":\"an id\"}";

        MobileServiceClient client = null;

        try {
            client = new MobileServiceClient(appUrl, getInstrumentation().getTargetContext());
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        // Add a filter to handle the request and create a new json
        // object with an id defined
        client = client.withFilter(getTestFilter(responseContent));

        client = client.withFilter(new ServiceFilter() {
            @Override
            public ListenableFuture<ServiceFilterResponse> handleRequest(ServiceFilterRequest request, NextServiceFilterCallback nextServiceFilterCallback) {
                String content = request.getContent();
                JsonObject obj = new JsonParser().parse(content).getAsJsonObject();

                Map<String, JsonElement> properties = new TreeMap<String, JsonElement>(String.CASE_INSENSITIVE_ORDER);

                for (Entry<String, JsonElement> entry : obj.entrySet()) {
                    properties.put(entry.getKey(), entry.getValue());
                }

                assertTrue(properties.containsKey("id"));
                assertFalse(properties.containsKey("updatedAt"));

                return nextServiceFilterCallback.onNext(request);
            }
        });

        // Create get the MobileService table
        MobileServiceTable<UpdatedAtType> msTable = client.getTable(UpdatedAtType.class);

        UpdatedAtType element = new UpdatedAtType();
        element.Id = "an id";
        element.UpdatedAt = new GregorianCalendar(2012, 00, 18).getTime();

        try {
            // Call the insert method
            UpdatedAtType entity = msTable.insert(element).get();

            if (entity == null) {
                fail("Expected result");
            }

        } catch (Exception exception) {
            fail(exception.getMessage());
        }
    }

    public void testInsertRemovesVersionSerializedNameStringId() throws Throwable {

        final String responseContent = "{\"id\":\"an id\"}";

        MobileServiceClient client = null;

        try {
            client = new MobileServiceClient(appUrl, getInstrumentation().getTargetContext());
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        // Add a filter to handle the request and create a new json
        // object with an id defined
        client = client.withFilter(getTestFilter(responseContent));

        client = client.withFilter(new ServiceFilter() {
            @Override
            public ListenableFuture<ServiceFilterResponse> handleRequest(ServiceFilterRequest request, NextServiceFilterCallback nextServiceFilterCallback) {
                String content = request.getContent();
                JsonObject obj = new JsonParser().parse(content).getAsJsonObject();

                Map<String, JsonElement> properties = new TreeMap<String, JsonElement>(String.CASE_INSENSITIVE_ORDER);

                for (Entry<String, JsonElement> entry : obj.entrySet()) {
                    properties.put(entry.getKey(), entry.getValue());
                }

                assertTrue(properties.containsKey("id"));
                assertFalse(properties.containsKey("version"));

                return nextServiceFilterCallback.onNext(request);
            }
        });

        // Create get the MobileService table
        MobileServiceTable<VersionType> msTable = client.getTable(VersionType.class);

        VersionType element = new VersionType();
        element.Id = "an id";
        element.Version = "a version";

        try {
            // Call the insert method
            VersionType entity = msTable.insert(element).get();

            if (entity == null) {
                fail("Expected result");
            }

        } catch (Exception exception) {
            fail(exception.getMessage());
        }
    }

    public void testInsertRemovesAllSystemSerializedNameStringId() throws Throwable {

        final String responseContent = "{\"id\":\"an id\"}";
        MobileServiceClient client = null;

        try {
            client = new MobileServiceClient(appUrl, getInstrumentation().getTargetContext());
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        // Add a filter to handle the request and create a new json
        // object with an id defined
        client = client.withFilter(getTestFilter(responseContent));

        client = client.withFilter(new ServiceFilter() {
            @Override
            public ListenableFuture<ServiceFilterResponse> handleRequest(ServiceFilterRequest request, NextServiceFilterCallback nextServiceFilterCallback) {
                String content = request.getContent();
                JsonObject obj = new JsonParser().parse(content).getAsJsonObject();

                Map<String, JsonElement> properties = new TreeMap<String, JsonElement>(String.CASE_INSENSITIVE_ORDER);

                for (Entry<String, JsonElement> entry : obj.entrySet()) {
                    properties.put(entry.getKey(), entry.getValue());
                }

                assertTrue(properties.containsKey("id"));
                assertFalse(properties.containsKey("createdAt"));
                assertFalse(properties.containsKey("updatedAt"));
                assertFalse(properties.containsKey("version"));

                return nextServiceFilterCallback.onNext(request);
            }
        });

        // Create get the MobileService table
        MobileServiceTable<AllSystemPropertiesType> msTable = client.getTable(AllSystemPropertiesType.class);

        AllSystemPropertiesType element = new AllSystemPropertiesType();
        element.Id = "an id";
        element.CreatedAt = new GregorianCalendar(2012, 00, 18).getTime();
        element.UpdatedAt = new GregorianCalendar(2012, 00, 18).getTime();
        element.Version = "a version";

        try {
            // Call the insert method
            AllSystemPropertiesType entity = msTable.insert(element).get();

            if (entity == null) {
                fail("Expected result");
            }

        } catch (Exception exception) {
            fail(exception.getMessage());
        }
    }

    public void testInsertDoesNotRemoveNonSystemCreatedAtPropertyNameStringId() throws Throwable {

        final String responseContent = "{\"id\":\"an id\"}";

        MobileServiceClient client = null;

        try {
            client = new MobileServiceClient(appUrl, getInstrumentation().getTargetContext());
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        // Add a filter to handle the request and create a new json
        // object with an id defined
        client = client.withFilter(getTestFilter(responseContent));

        client = client.withFilter(new ServiceFilter() {
            @Override
            public ListenableFuture<ServiceFilterResponse> handleRequest(ServiceFilterRequest request, NextServiceFilterCallback nextServiceFilterCallback) {
                String content = request.getContent();
                JsonObject obj = new JsonParser().parse(content).getAsJsonObject();

                Map<String, JsonElement> properties = new TreeMap<String, JsonElement>(String.CASE_INSENSITIVE_ORDER);

                for (Entry<String, JsonElement> entry : obj.entrySet()) {
                    properties.put(entry.getKey(), entry.getValue());
                }

                assertTrue(properties.containsKey("id"));
                assertTrue(properties.containsKey("_CreatedAt"));

                return nextServiceFilterCallback.onNext(request);
            }
        });

        // Create get the MobileService table
        MobileServiceTable<NotSystemPropertyCreatedAtType> msTable = client.getTable(NotSystemPropertyCreatedAtType.class);

        NotSystemPropertyCreatedAtType element = new NotSystemPropertyCreatedAtType();
        element.Id = "an id";
        element._CreatedAt = new GregorianCalendar(2012, 00, 18).getTime();

        try {
            // Call the insert method
            NotSystemPropertyCreatedAtType entity = msTable.insert(element).get();

            if (entity == null) {
                fail("Expected result");
            }

        } catch (Exception exception) {
            fail(exception.getMessage());
        }
    }

    public void testInsertDoesNotRemoveNonSystemUpdatedAtPropertyNameStringId() throws Throwable {

        final String responseContent = "{\"id\":\"an id\"}";

        MobileServiceClient client = null;

        try {
            client = new MobileServiceClient(appUrl, getInstrumentation().getTargetContext());
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        // Add a filter to handle the request and create a new json
        // object with an id defined
        client = client.withFilter(getTestFilter(responseContent));

        client = client.withFilter(new ServiceFilter() {
            @Override
            public ListenableFuture<ServiceFilterResponse> handleRequest(ServiceFilterRequest request, NextServiceFilterCallback nextServiceFilterCallback) {
                String content = request.getContent();
                JsonObject obj = new JsonParser().parse(content).getAsJsonObject();

                Map<String, JsonElement> properties = new TreeMap<String, JsonElement>(String.CASE_INSENSITIVE_ORDER);

                for (Entry<String, JsonElement> entry : obj.entrySet()) {
                    properties.put(entry.getKey(), entry.getValue());
                }

                assertTrue(properties.containsKey("id"));
                assertTrue(properties.containsKey("_UpdatedAt"));

                return nextServiceFilterCallback.onNext(request);
            }
        });

        // Create get the MobileService table
        MobileServiceTable<NotSystemPropertyUpdatedAtType> msTable = client.getTable(NotSystemPropertyUpdatedAtType.class);

        NotSystemPropertyUpdatedAtType element = new NotSystemPropertyUpdatedAtType();
        element.Id = "an id";
        element._UpdatedAt = new GregorianCalendar(2012, 00, 18).getTime();

        try {
            // Call the insert method
            NotSystemPropertyUpdatedAtType entity = msTable.insert(element).get();

            if (entity == null) {
                fail("Expected result");
            }

        } catch (Exception exception) {
            fail(exception.getMessage());
        }
    }

    public void testInsertDoesNotRemoveNonSystemVersionPropertyNameStringId() throws Throwable {

        final String responseContent = "{\"id\":\"an id\"}";

        MobileServiceClient client = null;

        try {
            client = new MobileServiceClient(appUrl, getInstrumentation().getTargetContext());
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        // Add a filter to handle the request and create a new json
        // object with an id defined
        client = client.withFilter(getTestFilter(responseContent));

        client = client.withFilter(new ServiceFilter() {
            @Override
            public ListenableFuture<ServiceFilterResponse> handleRequest(ServiceFilterRequest request, NextServiceFilterCallback nextServiceFilterCallback) {
                String content = request.getContent();
                JsonObject obj = new JsonParser().parse(content).getAsJsonObject();

                Map<String, JsonElement> properties = new TreeMap<String, JsonElement>(String.CASE_INSENSITIVE_ORDER);

                for (Entry<String, JsonElement> entry : obj.entrySet()) {
                    properties.put(entry.getKey(), entry.getValue());
                }

                assertTrue(properties.containsKey("id"));
                assertTrue(properties.containsKey("_Version"));

                return nextServiceFilterCallback.onNext(request);
            }
        });

        // Create get the MobileService table
        MobileServiceTable<NotSystemPropertyVersionType> msTable = client.getTable(NotSystemPropertyVersionType.class);

        NotSystemPropertyVersionType element = new NotSystemPropertyVersionType();
        element.Id = "an id";
        element._Version = "a version";

        try {
            // Call the insert method
            NotSystemPropertyVersionType entity = msTable.insert(element).get();

            if (entity == null) {
                fail("Expected result");
            }

        } catch (Exception exception) {
            fail(exception.getMessage());
        }
    }


    public void testLookupDeserializesCreateAtToDate() throws Throwable {

        final String responseContent = "{\"id\":\"an id\",\"createdAt\":\"2000-01-01T07:59:59.000Z\"}";

        MobileServiceClient client = null;

        try {
            client = new MobileServiceClient(appUrl, getInstrumentation().getTargetContext());
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        // Add a filter to handle the request and create a new json
        // object with an id defined
        client = client.withFilter(getTestFilter(responseContent));

        // Create get the MobileService table
        MobileServiceTable<CreatedAtType> msTable = client.getTable(CreatedAtType.class);

        try {
            // Call the lookUp method
            CreatedAtType entity = msTable.lookUp("an id").get();

            // Asserts
            if (entity == null) {
                fail("Expected result");
            } else {
                assertTrue(entity instanceof CreatedAtType);

                GregorianCalendar calendar = new GregorianCalendar(2000, 00, 01, 07, 59, 59);
                calendar.setTimeZone(TimeZone.getTimeZone("UTC"));

                assertEquals("an id", entity.Id);
                assertEquals(calendar.getTime(), entity.CreatedAt);
            }

        } catch (Exception exception) {
            fail(exception.getMessage());
        }
    }

    public void testLookupDeserializesCreateAtToString() throws Throwable {

        final String responseContent = "{\"id\":\"an id\",\"createdAt\":\"2000-01-01T07:59:59.000Z\"}";

        MobileServiceClient client = null;

        try {
            client = new MobileServiceClient(appUrl, getInstrumentation().getTargetContext());
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        // Add a filter to handle the request and create a new json
        // object with an id defined
        client = client.withFilter(getTestFilter(responseContent));

        // Create get the MobileService table
        MobileServiceTable<StringCreatedAtType> msTable = client.getTable(StringCreatedAtType.class);

        try {
            // Call the lookUp method
            StringCreatedAtType entity = msTable.lookUp("an id").get();

            // Asserts
            if (entity == null) {
                fail("Expected result");
            } else {
                assertTrue(entity instanceof StringCreatedAtType);

                assertEquals("an id", entity.Id);
                assertEquals("2000-01-01T07:59:59.000Z", entity.CreatedAt);
            }

        } catch (Exception exception) {
            fail(exception.getMessage());
        }
    }

    public void testLookupDeserializesUpdateAtToDate() throws Throwable {

        final String responseContent = "{\"id\":\"an id\",\"updatedAt\":\"2000-01-01T07:59:59.000Z\"}";

        MobileServiceClient client = null;

        try {
            client = new MobileServiceClient(appUrl, getInstrumentation().getTargetContext());
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        // Add a filter to handle the request and create a new json
        // object with an id defined
        client = client.withFilter(getTestFilter(responseContent));

        // Create get the MobileService table
        MobileServiceTable<UpdatedAtType> msTable = client.getTable(UpdatedAtType.class);

        try {
            // Call the lookUp method
            UpdatedAtType entity = msTable.lookUp("an id").get();

            // Asserts
            if (entity == null) {
                fail("Expected result");
            } else {
                assertTrue(entity instanceof UpdatedAtType);

                GregorianCalendar calendar = new GregorianCalendar(2000, 00, 01, 07, 59, 59);
                calendar.setTimeZone(TimeZone.getTimeZone("UTC"));

                assertEquals("an id", entity.Id);
                assertEquals(calendar.getTime(), entity.UpdatedAt);
            }

        } catch (Exception exception) {
            fail(exception.getMessage());
        }
    }

    public void testLookupDeserializesUpdateAtToString() throws Throwable {

        final String responseContent = "{\"id\":\"an id\",\"updatedAt\":\"2000-01-01T07:59:59.000Z\"}";

        MobileServiceClient client = null;

        try {
            client = new MobileServiceClient(appUrl, getInstrumentation().getTargetContext());
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        // Add a filter to handle the request and create a new json
        // object with an id defined
        client = client.withFilter(getTestFilter(responseContent));

        // Create get the MobileService table
        MobileServiceTable<StringUpdatedAtType> msTable = client.getTable(StringUpdatedAtType.class);

        try {
            // Call the lookUp method
            StringUpdatedAtType entity = msTable.lookUp("an id").get();

            // Asserts
            if (entity == null) {
                fail("Expected result");
            } else {
                assertTrue(entity instanceof StringUpdatedAtType);

                assertEquals("an id", entity.Id);
                assertEquals("2000-01-01T07:59:59.000Z", entity.UpdatedAt);
            }

        } catch (Exception exception) {
            fail(exception.getMessage());
        }
    }

    public void testLookupDeserializesVersionToString() throws Throwable {

        final String responseContent = "{\"id\":\"an id\",\"version\":\"AAAAAAAAH2o=\"}";
        MobileServiceClient client = null;

        try {
            client = new MobileServiceClient(appUrl, getInstrumentation().getTargetContext());
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        // Add a filter to handle the request and create a new json
        // object with an id defined
        client = client.withFilter(getTestFilter(responseContent));

        // Create get the MobileService table
        MobileServiceTable<VersionType> msTable = client.getTable(VersionType.class);

        try {
            // Call the lookUp method
            VersionType entity = msTable.lookUp("an id").get();

            // Asserts
            if (entity == null) {
                fail("Expected result");
            } else {
                assertTrue(entity instanceof VersionType);

                assertEquals("an id", entity.Id);
                assertEquals("AAAAAAAAH2o=", entity.Version);
            }

        } catch (Exception exception) {
            fail(exception.getMessage());
        }
    }

    public void testUpdateSetsIfMatchWithVersion() throws Throwable {
        List<Pair<String, String>> versions = SystemPropertiesTestData.VersionsSerialize;

        for (Pair<String, String> version : versions) {
            updateSetsIfMatchWithVersion(version);
        }
    }

    private void updateSetsIfMatchWithVersion(final Pair<String, String> version) throws Throwable {

        final String responseContent = "{\"id\":\"an id\",\"version\":\"AAAAAAAAH2o=\"}";

        MobileServiceClient client = null;

        try {
            client = new MobileServiceClient(appUrl, getInstrumentation().getTargetContext());
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        // Add a filter to handle the request and create a new json
        // object with an id defined
        client = client.withFilter(getTestFilter(responseContent));

        client = client.withFilter(new ServiceFilter() {
            @Override
            public ListenableFuture<ServiceFilterResponse> handleRequest(ServiceFilterRequest request, NextServiceFilterCallback nextServiceFilterCallback) {
                boolean hasHeaderIfMatch = false;

                Headers headers = request.getHeaders();

                for (int i = 0; i < request.getHeaders().size(); i++) {
                    if (headers.name(i).equalsIgnoreCase("If-Match")) {
                        assertTrue(headers.value(i).equalsIgnoreCase(version.second));

                        hasHeaderIfMatch = true;
                    }
                }

                assertTrue(hasHeaderIfMatch);

                return nextServiceFilterCallback.onNext(request);
            }
        });

        // Create get the MobileService table
        MobileServiceTable<VersionType> msTable = client.getTable(VersionType.class);

        VersionType element = new VersionType();
        element.Id = "an id";
        element.Version = version.first;

        try {
            // Call the update method
            VersionType entity = msTable.update(element).get();

            // Asserts
            if (entity == null) {
                fail("Expected result");
            }
        } catch (Exception exception) {
            fail(exception.getMessage());
        }
    }

    public void testUpdateSetsVersionWithETag() throws Throwable {
        List<Pair<String, String>> versions = SystemPropertiesTestData.VersionsDeserialize;

        for (Pair<String, String> version : versions) {
            updateSetsVersionWithEtag(version);
        }
    }

    private void updateSetsVersionWithEtag(final Pair<String, String> version) throws Throwable {

        final String responseContent = "{\"id\":\"an id\"}";

        MobileServiceClient client = null;

        try {
            client = new MobileServiceClient(appUrl, getInstrumentation().getTargetContext());
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }

        // Add a filter to handle the request and create a new json
        // object with an id defined
        client = client.withFilter(new ServiceFilter() {
            @Override
            public ListenableFuture<ServiceFilterResponse> handleRequest(ServiceFilterRequest request, NextServiceFilterCallback nextServiceFilterCallback) {
                // Create a mock response simulating an error
                ServiceFilterResponseMock response = new ServiceFilterResponseMock();
                response.setStatus((new StatusLine(Protocol.HTTP_2, 200, "")));
                response.setContent(responseContent);
                response.setHeaders(new Headers.Builder().add("ETag",version.second).build());
                // create a mock request to replace the existing one
                ServiceFilterRequestMock requestMock = new ServiceFilterRequestMock(response);
                return nextServiceFilterCallback.onNext(requestMock);
            }
        });

        // Create get the MobileService table
        MobileServiceTable<VersionType> msTable = client.getTable(VersionType.class);

        VersionType element = new VersionType();
        element.Id = "an id";

        try {
            // Call the update method
            VersionType entity = msTable.update(element).get();

            // Asserts
            if (entity == null) {
                fail("Expected result");
            } else {
                assertTrue(entity instanceof VersionType);

                assertEquals("an id", entity.Id);
                assertEquals(version.first, entity.Version);
            }
        } catch (Exception exception) {
            fail(exception.getMessage());
        }
    }

    // Test Filter
    private ServiceFilter getTestFilter(String content) {
        return getTestFilter(200, content);
    }

    private ServiceFilter getTestFilter(final int statusCode, final String content) {
        return new ServiceFilter() {

            @Override
            public ListenableFuture<ServiceFilterResponse> handleRequest(ServiceFilterRequest request, NextServiceFilterCallback nextServiceFilterCallback) {

                // Create a mock response simulating an error
                ServiceFilterResponseMock response = new ServiceFilterResponseMock();
                response.setStatus((new StatusLine(Protocol.HTTP_2, statusCode, "")));
                response.setContent(content);

                // create a mock request to replace the existing one
                ServiceFilterRequestMock requestMock = new ServiceFilterRequestMock(response);
                return nextServiceFilterCallback.onNext(requestMock);
            }
        };
    }
}
