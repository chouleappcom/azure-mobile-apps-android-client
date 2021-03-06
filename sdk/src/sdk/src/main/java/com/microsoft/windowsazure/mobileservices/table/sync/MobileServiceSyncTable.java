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
 * MobileServiceSyncTable.java
 */
package com.microsoft.windowsazure.mobileservices.table.sync;

import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.SettableFuture;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.microsoft.windowsazure.mobileservices.MobileServiceClient;
import com.microsoft.windowsazure.mobileservices.MobileServiceList;
import com.microsoft.windowsazure.mobileservices.table.query.Query;
import com.microsoft.windowsazure.mobileservices.table.serialization.JsonEntityParser;

import java.util.List;

/**
 * Provides operations on local table.
 */
public class MobileServiceSyncTable<E> {
    private MobileServiceJsonSyncTable mInternalTable;
    private MobileServiceClient mClient;
    private Class<E> mClazz;

    /**
     * Constructor for MobileServiceTable
     *
     * @param name   The name of the represented table
     * @param client The MobileServiceClient used to invoke table operations
     * @param clazz  The class used for data serialization
     */
    public MobileServiceSyncTable(String name, MobileServiceClient client, Class<E> clazz) {
        this.mInternalTable = new MobileServiceJsonSyncTable(name, client);
        this.mClazz = clazz;
        this.mClient = client;
    }

    /**
     * @return the name of the represented table
     */
    public String getName() {
        return mInternalTable.getName();
    }

    /**
     * Performs a query against the remote table and stores results.
     *
     * @param query   an optional query to filter results
     * @param queryId id to identify the query
     * @return A ListenableFuture that is done when results have been pulled.
     */
    public ListenableFuture<Void> pull(Query query, String queryId) {
        ListenableFuture<Void> pull = this.mInternalTable.pull(query, queryId);

        final SettableFuture<Void> result = SettableFuture.create();

        Futures.addCallback(pull, new FutureCallback<Void>() {

            @Override
            public void onFailure(Throwable throwable) {
                result.setException(throwable);
            }

            @Override
            public void onSuccess(Void value) {
                result.set(value);
            }
        }, MoreExecutors.directExecutor());

        return result;
    }

    /**
     * Performs a query against the remote table and stores results.
     *
     * @param query an optional query to filter results
     * @return A ListenableFuture that is done when results have been pulled.
     */
    public ListenableFuture<Void> pull(Query query) {
        return pull(query, null);
    }

    /**
     * Performs a query against the local table and deletes the results.
     *
     * @param query an optional query to filter results
     * @return A ListenableFuture that is done when results have been purged.
     */
    public ListenableFuture<Void> purge(Query query) {
        ListenableFuture<Void> internalFuture = this.mInternalTable.purge(query);

        final SettableFuture<Void> future = SettableFuture.create();

        Futures.addCallback(internalFuture, new FutureCallback<Void>() {

            @Override
            public void onFailure(Throwable throwable) {
                future.setException(throwable);
            }

            @Override
            public void onSuccess(Void value) {
                future.set(value);
            }
        }, MoreExecutors.directExecutor());

        return future;
    }

    /**
     * Retrieve results from the local table.
     *
     * @param query an optional query to filter results
     * @return A ListenableFuture that is done when the results have been
     * retrieved.
     */
    public ListenableFuture<MobileServiceList<E>> read(Query query) {
        final SettableFuture<MobileServiceList<E>> future = SettableFuture.create();

        ListenableFuture<JsonElement> internalFuture = mInternalTable.read(query);

        Futures.addCallback(internalFuture, new FutureCallback<JsonElement>() {
            @Override
            public void onFailure(Throwable throwable) {
                future.setException(throwable);
            }

            @Override
            public void onSuccess(JsonElement result) {
                try {
                    if (result.isJsonObject()) {
                        JsonObject jsonObject = result.getAsJsonObject();

                        int count = jsonObject.get("count").getAsInt();
                        JsonElement elements = jsonObject.get("results");

                        List<E> list = parseResults(elements);
                        future.set(new MobileServiceList<E>(list, count));
                    } else {
                        List<E> list = parseResults(result);
                        future.set(new MobileServiceList<E>(list, list.size()));
                    }
                } catch (Exception e) {
                    future.setException(e);
                }
            }
        }, MoreExecutors.directExecutor());

        return future;
    }

    /**
     * Looks up an item from the local table.
     *
     * @param itemId the id of the item to look up
     * @return A ListenableFuture that is done when the item has been looked up.
     */
    public ListenableFuture<E> lookUp(String itemId) {
        final SettableFuture<E> future = SettableFuture.create();

        ListenableFuture<JsonObject> internalFuture = mInternalTable.lookUp(itemId);
        Futures.addCallback(internalFuture, new FutureCallback<JsonObject>() {
            @Override
            public void onFailure(Throwable throwable) {
                future.setException(throwable);
            }

            @Override
            public void onSuccess(JsonObject result) {
                try {

                    if (result == null) {
                        future.set(null);
                    }

                    future.set(parseResults(result).get(0));
                } catch (Exception e) {
                    future.setException(e);
                }
            }
        }, MoreExecutors.directExecutor());

        return future;
    }

    /**
     * Insert an item into the local table and enqueue the operation to be
     * synchronized on context push.
     *
     * @param item the item to be inserted
     * @return A ListenableFuture that is done when the item has been inserted,
     * returning a copy of the inserted item including id.
     */
    public ListenableFuture<E> insert(E item) {
        final SettableFuture<E> future = SettableFuture.create();

        final JsonObject json = mClient.getGsonBuilder().create().toJsonTree(item).getAsJsonObject();

        JsonElement idJsonObject = json.get("id");

        if (idJsonObject != null && !idJsonObject.isJsonNull()) {
            String itemId = idJsonObject.getAsString();

            ListenableFuture<JsonObject> lookUpInternalFuture = mInternalTable.lookUp(itemId);

            Futures.addCallback(lookUpInternalFuture, new FutureCallback<JsonObject>() {
                @Override
                public void onFailure(Throwable throwable) {
                    future.setException(throwable);
                }

                @Override
                public void onSuccess(JsonObject result) {

                    if (result != null) {
                        future.set(parseResults(result).get(0));
                        return;
                    }

                    insertInternal(json, future);
                }
            }, MoreExecutors.directExecutor());
        } else {
            insertInternal(json, future);
        }

        return future;
    }

    private void insertInternal(JsonObject json, final SettableFuture<E> finalFuture) {
        ListenableFuture<JsonObject> internalFuture = mInternalTable.insert(json);

        Futures.addCallback(internalFuture, new FutureCallback<JsonObject>() {
            @Override
            public void onFailure(Throwable throwable) {
                finalFuture.setException(throwable);
            }

            @Override
            public void onSuccess(JsonObject result) {
                finalFuture.set(parseResults(result).get(0));
            }
        }, MoreExecutors.directExecutor());
    }

    /**
     * Update an item in the local table and enqueue the operation to be
     * synchronized on context push.
     *
     * @param item the item to be updated
     * @return A ListenableFuture that is done when the item has been updated.
     */
    public ListenableFuture<Void> update(E item) {
        final SettableFuture<Void> future = SettableFuture.create();

        JsonObject json = mClient.getGsonBuilder().create().toJsonTree(item).getAsJsonObject();

        ListenableFuture<Void> internalFuture = mInternalTable.update(json);

        Futures.addCallback(internalFuture, new FutureCallback<Void>() {
            @Override
            public void onFailure(Throwable throwable) {
                future.setException(throwable);
            }

            @Override
            public void onSuccess(Void value) {
                future.set(value);
            }
        }, MoreExecutors.directExecutor());

        return future;
    }

    /**
     * Delete an item from the local table and enqueue the operation to be
     * synchronized on context push.
     *
     * @param item the item to be deleted
     * @return A ListenableFuture that is done when the item has been deleted.
     */
    public ListenableFuture<Void> delete(E item) {
        final SettableFuture<Void> future = SettableFuture.create();

        JsonObject json = mClient.getGsonBuilder().create().toJsonTree(item).getAsJsonObject();

        ListenableFuture<Void> internalFuture = mInternalTable.delete(json);

        Futures.addCallback(internalFuture, new FutureCallback<Void>() {
            @Override
            public void onFailure(Throwable throwable) {
                future.setException(throwable);
            }

            @Override
            public void onSuccess(Void value) {
                future.set(value);
            }
        }, MoreExecutors.directExecutor());

        return future;
    }

    /**
     * Delete an item from the local table and enqueue the operation to be
     * synchronized on context push.
     *
     * @param itemId the id of the item to be deleted
     * @return A ListenableFuture that is done when the item has been deleted.
     */
    public ListenableFuture<Void> delete(final String itemId) {
        final SettableFuture<Void> future = SettableFuture.create();

        ListenableFuture<Void> internalFuture = mInternalTable.delete(itemId);

        Futures.addCallback(internalFuture, new FutureCallback<Void>() {
            @Override
            public void onFailure(Throwable throwable) {
                future.setException(throwable);
            }

            @Override
            public void onSuccess(Void value) {
                future.set(value);
            }
        }, MoreExecutors.directExecutor());

        return future;
    }

    private List<E> parseResults(JsonElement results) {
        Gson gson = mClient.getGsonBuilder().create();
        return JsonEntityParser.parseResults(results, gson, mClazz);
    }
}
