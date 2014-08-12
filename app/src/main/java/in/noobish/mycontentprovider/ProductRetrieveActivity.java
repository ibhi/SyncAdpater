package in.noobish.mycontentprovider;

import android.accounts.Account;
import android.app.ListActivity;
import android.app.LoaderManager;
import android.content.ContentResolver;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.content.SyncStatusObserver;
import android.database.Cursor;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.SimpleCursorAdapter;
import android.widget.TextView;

public class ProductRetrieveActivity extends ListActivity implements LoaderManager.LoaderCallbacks<Cursor>, AdapterView.OnItemClickListener{

    private static final String TAG = "ProductListActivity";

    /**
     * Cursor adapter for controlling ListView results.
     */
    private SimpleCursorAdapter mAdapter;

    /**
     * Handle to a SyncObserver. The ProgressBar element is visible until the SyncObserver reports
     * that the sync is complete.
     *
     * <p>This allows us to delete our SyncObserver once the application is no longer in the
     * foreground.
     */
    private Object mSyncObserverHandle;

    /**
     * Options menu used to populate ActionBar.
     */
    private Menu mOptionsMenu;

    String[] mProjection =
            {
                    ProductsContract.Product._ID,
                    ProductsContract.Product.COLUMN_NAME_PRODUCT,
                    ProductsContract.Product.COLUMN_NAME_PRICE,   // Contract class constant for the word column name
                    ProductsContract.Product.COLUMN_NAME_DESCRIPTION  // Contract class constant for the locale column name
            };

//    /**
//     * Projection for querying the content provider.
//     */
//    private static final String[] PROJECTION = new String[]{
//            ProductsContract.Product._ID,
//            ProductsContract.Product.COLUMN_NAME_PRODUCT,
//            ProductsContract.Product.COLUMN_NAME_PRICE,
//            ProductsContract.Product.COLUMN_NAME_DESCRIPTION
//    };
//
//    // Column indexes. The index of a column in the Cursor is the same as its relative position in
//    // the projection.
//    /** Column index for _ID */
//    private static final int COLUMN_ID = 0;
//    /** Column index for title */
//    private static final int COLUMN_NAME = 1;
//    /** Column index for link */
//    private static final int COLUMN_PRICE = 2;
//    /** Column index for published */
//    private static final int COLUMN_DESCRIPTION = 3;
//
//    /**
//     * List of Cursor columns to read from when preparing an adapter to populate the ListView.
//     */
//    private static final String[] FROM_COLUMNS = new String[]{
//            FeedContract.Entry.COLUMN_NAME_TITLE,
//            FeedContract.Entry.COLUMN_NAME_PUBLISHED
//    };
//
//    /**
//     * List of Views which will be populated by Cursor data.
//     */
//    private static final int[] TO_FIELDS = new int[]{
//            android.R.id.text1,
//            android.R.id.text2};


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SyncUtils.CreateSyncAccount(getApplicationContext());

        setContentView(R.layout.listview_layout);

        Cursor c = getContentResolver().query(ProductsContract.Product.CONTENT_URI, mProjection, null, null, null);
        mAdapter = new SimpleCursorAdapter(
                this,
                R.layout.activity_retrieve_students,
                c,
                new String[] {ProductsContract.Product.COLUMN_NAME_PRODUCT,ProductsContract.Product.COLUMN_NAME_PRICE, ProductsContract.Product.COLUMN_NAME_DESCRIPTION},
                new int[] {R.id.textProductName, R.id.textPrice, R.id.textDescription}
        );
        ListView listView = getListView();
        listView.setAdapter(mAdapter);
        listView.setOnItemClickListener(this);
        getLoaderManager().initLoader(0, null, this);



    }

    @Override
    public void onResume() {
        super.onResume();
        mSyncStatusObserver.onStatusChanged(0);

        // Watch for sync state changes
        final int mask = ContentResolver.SYNC_OBSERVER_TYPE_PENDING |
                ContentResolver.SYNC_OBSERVER_TYPE_ACTIVE;
        mSyncObserverHandle = ContentResolver.addStatusChangeListener(mask, mSyncStatusObserver);
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mSyncObserverHandle != null) {
            ContentResolver.removeStatusChangeListener(mSyncObserverHandle);
            mSyncObserverHandle = null;
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
//        super.onCreateOptionsMenu(menu);
        mOptionsMenu = menu;
        getMenuInflater().inflate(R.menu.student_retrieve, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        switch (item.getItemId()) {
            // If the user clicks the "Refresh" button.
            case R.id.menu_refresh:
                SyncUtils.TriggerRefresh();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Query the content provider for data.
     *
     * <p>Loaders do queries in a background thread. They also provide a ContentObserver that is
     * triggered when data in the content provider changes. When the sync adapter updates the
     * content provider, the ContentObserver responds by resetting the loader and then reloading
     * it.
     */

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        // We only have one loader, so we can ignore the value of i.
        // (It'll be '0', as set in onCreate().)
        return new CursorLoader(this,  // Context
                ProductsContract.Product.CONTENT_URI, // URI
                mProjection,                // Projection
                null,                           // Selection
                null,                           // Selection args
                null);                          // Sort
    }

    /**
     * Move the Cursor returned by the query into the ListView adapter. This refreshes the existing
     * UI with the data in the Cursor.
     */
    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        mAdapter.changeCursor(cursor);
    }

    /**
     * Called when the ContentObserver defined for the content provider detects that data has
     * changed. The ContentObserver resets the loader, and then re-runs the loader. In the adapter,
     * set the Cursor value to null. This removes the reference to the Cursor, allowing it to be
     * garbage-collected.
     */
    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {
        mAdapter.changeCursor(null);
    }


    /**
     * Set the state of the Refresh button. If a sync is active, turn on the ProgressBar widget.
     * Otherwise, turn it off.
     *
     * @param refreshing True if an active sync is occurring, false otherwise
     */
    public void setRefreshActionButtonState(boolean refreshing) {
        if (mOptionsMenu == null) {
            return;
        }

        final MenuItem refreshItem = mOptionsMenu.findItem(R.id.menu_refresh);
        if (refreshItem != null) {
            if (refreshing) {
                refreshItem.setActionView(R.layout.actionbar_indeterminate_progress);
            } else {
                refreshItem.setActionView(null);
            }
        }
    }

    /**
     * Create a new anonymous SyncStatusObserver. It's attached to the app's ContentResolver in
     * onResume(), and removed in onPause(). If status changes, it sets the state of the Refresh
     * button. If a sync is active or pending, the Refresh button is replaced by an indeterminate
     * ProgressBar; otherwise, the button itself is displayed.
     */
    private SyncStatusObserver mSyncStatusObserver = new SyncStatusObserver() {
        /** Callback invoked with the sync adapter status changes. */
        @Override
        public void onStatusChanged(int which) {
            runOnUiThread(new Runnable() {
                /**
                 * The SyncAdapter runs on a background thread. To update the UI, onStatusChanged()
                 * runs on the UI thread.
                 */
                @Override
                public void run() {
                    // Create a handle to the account that was created by
                    // SyncService.CreateSyncAccount(). This will be used to query the system to
                    // see how the sync status has changed.
                    Account account = GenericAccountService.GetAccount();
                    if (account == null) {
                        // GetAccount() returned an invalid value. This shouldn't happen, but
                        // we'll set the status to "not refreshing".
                        setRefreshActionButtonState(false);
                        return;
                    }

                    // Test the ContentResolver to see if the sync adapter is active or pending.
                    // Set the state of the refresh button accordingly.
                    boolean syncActive = ContentResolver.isSyncActive(
                            account, ProductsContract.CONTENT_AUTHORITY);
                    boolean syncPending = ContentResolver.isSyncPending(
                            account, ProductsContract.CONTENT_AUTHORITY);
                    setRefreshActionButtonState(syncActive || syncPending);
                }
            });
        }
    };

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long rowId) {
        Log.d("IBHI", "Product Name is:" + ((TextView)(view.findViewById(R.id.textProductName))).getText().toString());
        Intent intent = new Intent(this, EditProductActivity.class);
        intent.putExtra("rowId", rowId);
        intent.putExtra("position", position);
        intent.putExtra("name", ((TextView)(view.findViewById(R.id.textProductName))).getText().toString());
        intent.putExtra("price", ((TextView)(view.findViewById(R.id.textPrice))).getText().toString());
        intent.putExtra("description", ((TextView)(view.findViewById(R.id.textDescription))).getText().toString());
        startActivity(intent);
    }
}
