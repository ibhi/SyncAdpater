package in.noobish.mycontentprovider;

/**
 * Created by ibhi on 3/8/14.
 */

import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.Context;
import android.content.OperationApplicationException;
import android.content.SyncResult;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.JsonReader;
import android.util.Log;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Define a sync adapter for the app.
 *
 * <p>This class is instantiated in {@link SyncService}, which also binds SyncAdapter to the system.
 * SyncAdapter should only be initialized in SyncService, never anywhere else.
 *
 * <p>The system calls onPerformSync() via an RPC call through the IBinder object supplied by
 * SyncService.
 */
class SyncAdapter extends AbstractThreadedSyncAdapter {
    public static final String TAG = "SyncAdapter";

    /**
     * URL to fetch content from during a sync.
     *
     * <p>This points to the Android Developers Blog. (Side note: We highly recommend reading the
     * Android Developer Blog to stay up to date on the latest Android platform developments!)
     */
    private static final String GET_ALL_PRODUCTS_URL = "http://ibhi.byethost15.com/get_all_products.php";

    private static final String UPDATE_PRODUCT_URL = "http://ibhi.byethost15.com/update_product.php";

    /**
     * Network connection timeout, in milliseconds.
     */
    private static final int NET_CONNECT_TIMEOUT_MILLIS = 15000;  // 15 seconds

    /**
     * Network read timeout, in milliseconds.
     */
    private static final int NET_READ_TIMEOUT_MILLIS = 10000;  // 10 seconds

    /**
     * Content resolver, for performing database operations.
     */
    private final ContentResolver mContentResolver;

    /**
     * Project used when querying content provider. Returns all known fields.
     */
    private static final String[] PROJECTION = new String[] {
            ProductsContract.Product._ID,
            ProductsContract.Product.COLUMN_NAME_PRODUCT,
            ProductsContract.Product.COLUMN_NAME_PRICE,
            ProductsContract.Product.COLUMN_NAME_DESCRIPTION,
            ProductsContract.Product.DIRTY,
            ProductsContract.Product.VERSION
    };

    // Constants representing column positions from PROJECTION.
    public static final int COLUMN_ID = 0;
    public static final int COLUMN_PRODUCT = 1;
    public static final int COLUMN_PRICE = 2;
    public static final int COLUMN_DESCRIPTION = 3;
    public static final int COLUMN_DIRTY = 4;
    public static final int COLUMN_VERSION=5;

    /**
     * Constructor. Obtains handle to content resolver for later use.
     */
    public SyncAdapter(Context context, boolean autoInitialize) {
        super(context, autoInitialize);
        mContentResolver = context.getContentResolver();
    }

    /**
     * Constructor. Obtains handle to content resolver for later use.
     */
    public SyncAdapter(Context context, boolean autoInitialize, boolean allowParallelSyncs) {
        super(context, autoInitialize, allowParallelSyncs);
        mContentResolver = context.getContentResolver();
    }

    /**
     * Called by the Android system in response to a request to run the sync adapter. The work
     * required to read data from the network, parse it, and store it in the content provider is
     * done here. Extending AbstractThreadedSyncAdapter ensures that all methods within SyncAdapter
     * run on a background thread. For this reason, blocking I/O and other long-running tasks can be
     * run <em>in situ</em>, and you don't have to set up a separate thread for them.
     .
     *
     * <p>This is where we actually perform any work required to perform a sync.
     * {@link AbstractThreadedSyncAdapter} guarantees that this will be called on a non-UI thread,
     * so it is safe to peform blocking I/O here.
     *
     * <p>The syncResult argument allows you to pass information back to the method that triggered
     * the sync.
     */
    @Override
    public void onPerformSync(Account account, Bundle extras, String authority,
                              ContentProviderClient provider, SyncResult syncResult) {
        Log.i(TAG, "Beginning network synchronization");
        try {
            final URL location = new URL(GET_ALL_PRODUCTS_URL);
            InputStream stream = null;

            try {
                Log.i(TAG, "Streaming data from network: " + location);
                stream = downloadUrl(location);
                updateLocalProductData(stream, syncResult);
                // Makes sure that the InputStream is closed after the app is
                // finished using it.
            } finally {
                if (stream != null) {
                    stream.close();
                }
            }
        } catch (MalformedURLException e) {
            Log.wtf(TAG, "Feed URL is malformed", e);
            syncResult.stats.numParseExceptions++;
            return;
        } catch (IOException e) {
            Log.e(TAG, "Error reading from network: " + e.toString());
            syncResult.stats.numIoExceptions++;
            return;
        }catch (JSONException e) {
            Log.e(TAG, "Error parsing feed: " + e.toString());
            syncResult.stats.numParseExceptions++;
            return;
        }catch (ParseException e) {
            Log.e(TAG, "Error parsing feed: " + e.toString());
            syncResult.stats.numParseExceptions++;
            return;
        } catch (RemoteException e) {
            Log.e(TAG, "Error updating database: " + e.toString());
            syncResult.databaseError = true;
            return;
        } catch (OperationApplicationException e) {
            Log.e(TAG, "Error updating database: " + e.toString());
            syncResult.databaseError = true;
            return;
        }
        Log.i(TAG, "Network synchronization complete");
    }

    /**
     * Read XML from an input stream, storing it into the content provider.
     *
     * <p>This is where incoming data is persisted, committing the results of a sync. In order to
     * minimize (expensive) disk operations, we compare incoming data with what's already in our
     * database, and compute a merge. Only changes (insert/update/delete) will result in a database
     * write.
     *
     * <p>As an additional optimization, we use a batch operation to perform all database writes at
     * once.
     *
     * <p>Merge strategy:
     * 1. Get cursor to all items in feed<br/>
     * 2. For each item, check if it's in the incoming data.<br/>
     *    a. YES: Remove from "incoming" list. Check if data has mutated, if so, perform
     *            database UPDATE.<br/>
     *    b. NO: Schedule DELETE from database.<br/>
     * (At this point, incoming database only contains missing items.)<br/>
     * 3. For any items remaining in incoming list, ADD to database.
     */
    public void updateLocalProductData(final InputStream stream, final SyncResult syncResult)
            throws IOException, JSONException, RemoteException,
            OperationApplicationException, ParseException {
        final ProductsParser productsParser = new ProductsParser();
        final ContentResolver contentResolver = getContext().getContentResolver();

        Log.i(TAG, "Parsing stream as JSON");
        final List<ProductsParser.Products> products = productsParser.readProducts(stream);
        Log.i(TAG, "Parsing complete. Found " + products.size() + " entries");


        ArrayList<ContentProviderOperation> batch = new ArrayList<ContentProviderOperation>();

        // Build hash table of incoming entries
        HashMap<String, ProductsParser.Products> productMap = new HashMap<String, ProductsParser.Products>();
        for (ProductsParser.Products e : products) {
            productMap.put(e.id, e);
        }

        // Get list of all items
        Log.i(TAG, "Fetching local entries for merge");
        Uri uri = ProductsContract.Product.CONTENT_URI; // Get all entries
        Cursor c = contentResolver.query(uri, PROJECTION, null, null, null);
        assert c != null;
        Log.i(TAG, "Found " + c.getCount() + " local entries. Computing merge solution...");

        // Find stale data
        String id;
        String name;
        String price;
        String description;
        int dirty;
        int version;

        while (c.moveToNext()) {
            syncResult.stats.numEntries++;
            id = c.getString(COLUMN_ID);
            name = c.getString(COLUMN_PRODUCT);
            price = c.getString(COLUMN_PRICE);
            description = c.getString(COLUMN_DESCRIPTION);
            dirty = c.getInt(COLUMN_DIRTY);
            version=c.getInt(COLUMN_VERSION);
            ProductsParser.Products match = productMap.get(id);
            if (match != null) {
                // Entry exists. Remove from entry map to prevent insert later.
                productMap.remove(id);
                // Check to see if the entry needs to be updated
                Uri existingUri = ProductsContract.Product.CONTENT_URI.buildUpon()
                        .appendPath(id).build();
                if(dirty == 0) {
                    if ((match.name != null && !match.name.equals(name)) ||
                            (match.price != null && !match.price.equals(price)) ||
                            (match.description != null && !match.description.equals(description))) {
                        // Update existing record
                        Log.i(TAG, "Scheduling update: " + existingUri);
                        batch.add(ContentProviderOperation.newUpdate(existingUri)
                                .withValue(ProductsContract.Product.COLUMN_NAME_PRODUCT, name)
                                .withValue(ProductsContract.Product.COLUMN_NAME_PRICE, price)
                                .withValue(ProductsContract.Product.COLUMN_NAME_DESCRIPTION, description)
                                .build());
                        syncResult.stats.numUpdates++;
                    } else {
                        Log.i(TAG, "No action: " + existingUri);
                    }
                }
                else {
                    if(dirty == 1) {
                        Log.i(TAG, "Data is locally modified and needs to update the server data");
                        final URL update_url = new URL(UPDATE_PRODUCT_URL);
                        Log.d("IBHI", "Local ID is " + id + "Name is " + name +"Price is " + price + "Description is " +description);
                        int result = updateServerData(update_url, id, name, price, description);
                        // TO DO
                        // Read and parse the input stream(in JSON format) for the success tag
                        if (result == 1) {
                            Log.d("IBHI", "The result success status is " + result);
                            Log.d("IBHI", "Local data is updated in the server successfully");
                        } else {
                            Log.d("IBHI", "Local data is not updated in the server");
                        }
                        Uri updateUri = ProductsContract.Product.CONTENT_URI.buildUpon()
                                .appendPath(id).build();
                        Log.d("IBHI", "Modifying dirty field for " + updateUri);
                        batch.add(ContentProviderOperation.newUpdate(updateUri)
                                .withValue(ProductsContract.Product.DIRTY, 0)
                                .build());

                    }
                    else {
                        Log.d("IBHI", "Unknown or null dirty flag" + dirty);
                    }

                }
            } else {
                // Entry doesn't exist. Remove it from the database.
                Uri deleteUri = ProductsContract.Product.CONTENT_URI.buildUpon()
                        .appendPath(id).build();
                Log.i(TAG, "Scheduling delete: " + deleteUri);
                batch.add(ContentProviderOperation.newDelete(deleteUri).build());
                syncResult.stats.numDeletes++;
            }
        }
        c.close();

        // Add new items
        for (ProductsParser.Products e : productMap.values()) {
            Log.i(TAG, "Scheduling insert: entry_id=" + e.id);
            batch.add(ContentProviderOperation.newInsert(ProductsContract.Product.CONTENT_URI)
                    .withValue(ProductsContract.Product.COLUMN_NAME_PRODUCT, e.name)
                    .withValue(ProductsContract.Product.COLUMN_NAME_PRICE, e.price)
                    .withValue(ProductsContract.Product.COLUMN_NAME_DESCRIPTION, e.description)
                    .build());
            syncResult.stats.numInserts++;
        }
        Log.i(TAG, "Merge solution ready. Applying batch update");
        mContentResolver.applyBatch(ProductsContract.CONTENT_AUTHORITY, batch);
        mContentResolver.notifyChange(
                ProductsContract.Product.CONTENT_URI, // URI where data was modified
                null,                           // No local observer
                false);                         // IMPORTANT: Do not sync to network
        // This sample doesn't support uploads, but if *your* code does, make sure you set
        // syncToNetwork=false in the line above to prevent duplicate syncs.
    }

    /**
     * Given a string representation of a URL, sets up a connection and gets an input stream.
     */
    private InputStream downloadUrl(final URL url) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setReadTimeout(NET_READ_TIMEOUT_MILLIS /* milliseconds */);
        conn.setConnectTimeout(NET_CONNECT_TIMEOUT_MILLIS /* milliseconds */);
        conn.setRequestMethod("GET");
        conn.setDoInput(true);
        // Starts the query
        conn.connect();
        return conn.getInputStream();
    }

    private String getQuery(List<NameValuePair> params) throws UnsupportedEncodingException
    {
        StringBuilder result = new StringBuilder();
        boolean first = true;

        for (NameValuePair pair : params)
        {
            if (first)
                first = false;
            else
                result.append("&");
            result.append(URLEncoder.encode(pair.getName(), "UTF-8"));
            result.append("=");
            result.append(URLEncoder.encode(pair.getValue(), "UTF-8"));
        }

        return result.toString();
    }

    private int updateServerData(final URL url, final String id, final String name, final String price, final String description) throws IOException {
        HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
        httpURLConnection.setReadTimeout(NET_READ_TIMEOUT_MILLIS /* milliseconds */);
        httpURLConnection.setConnectTimeout(NET_CONNECT_TIMEOUT_MILLIS /* milliseconds */);
        httpURLConnection.setRequestMethod("POST");
        httpURLConnection.setDoInput(true);
        httpURLConnection.setDoOutput(true);
        httpURLConnection.setRequestProperty("Content-Type",
                "application/x-www-form-urlencoded");

        List<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair("pid", id));
        params.add(new BasicNameValuePair("name", name));
        params.add(new BasicNameValuePair("price", price));
        params.add(new BasicNameValuePair("description", description));

        httpURLConnection.setFixedLengthStreamingMode(getQuery(params).getBytes().length);

        OutputStream os = httpURLConnection.getOutputStream();
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
        writer.write(getQuery(params));
        writer.flush();
        writer.close();
        os.close();
        int result=0;

        // Starts the query
        httpURLConnection.connect();
        Log.d("IBHI", "The response code is " + httpURLConnection.getResponseCode());
        InputStream inputStream = httpURLConnection.getInputStream();
        JsonReader reader = new JsonReader(new InputStreamReader(inputStream, "UTF-8"));
        Log.d("IBHI", reader.toString());

        try {
            reader.beginObject();
            String success = reader.nextName();
            Log.d("IBHI", "Success is " + success);
            if (success.equals("success")) {
//                Log.d("IBHI", "Success is " + success);
                result = reader.nextInt();
                Log.d("IBHI", "The result code is " + result);
            }
            String msgName = reader.nextName();
            Log.d("IBHI", "Name is " + msgName);
            if (msgName.equals("message")){
//                Log.d("IBHI", "The message name is " + msgName);
                String message = reader.nextString();
                Log.d("IBHI", "Message from Server is " + message);
            }

            reader.endObject();

            return result;

        }
        finally{
            reader.close();
            inputStream.close();

        }

    }


}
