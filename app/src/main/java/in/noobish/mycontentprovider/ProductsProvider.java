package in.noobish.mycontentprovider;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import java.util.HashMap;

/**
 * Created by ibhi on 1/8/14.
 */
public class ProductsProvider extends ContentProvider {
    DbHelper mDatabaseHelper;

    /**
     * Content authority for this provider.
     */
    private static final String AUTHORITY = ProductsContract.CONTENT_AUTHORITY;

    // The constants below represent individual URI routes, as IDs. Every URI pattern recognized by
    // this ContentProvider is defined using sUriMatcher.addURI(), and associated with one of these
    // IDs.
    //
    // When a incoming URI is run through sUriMatcher, it will be tested against the defined
    // URI patterns, and the corresponding route ID will be returned.
    /**
     * URI ID for route: /products
     */
    public static final int ROUTE_PRODUCTS = 1;

    /**
     * URI ID for route: /products/{ID}
     */
    public static final int ROUTE_PRODUCTS_ID = 2;

    /**
     * UriMatcher, used to decode incoming URIs.
     */
    private static final UriMatcher sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
    static {
        sUriMatcher.addURI(AUTHORITY, "products", ROUTE_PRODUCTS);
        sUriMatcher.addURI(AUTHORITY, "products/*", ROUTE_PRODUCTS_ID);
    }

    private static HashMap<String, String> STUDENTS_PROJECTION_MAP;

    @Override
    public boolean onCreate() {
        mDatabaseHelper = new DbHelper(getContext());
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        SQLiteDatabase db = mDatabaseHelper.getWritableDatabase();
        SQLiteQueryBuilder qb=new SQLiteQueryBuilder();
        qb.setTables(ProductsContract.Product.TABLE_NAME);

        final int match = sUriMatcher.match(uri);
        switch (match){
            case ROUTE_PRODUCTS_ID:
                String id=uri.getPathSegments().get(1);
                qb.appendWhere(ProductsContract.Product._ID + "=" + id);
                break;
            case ROUTE_PRODUCTS:
                qb.setProjectionMap(STUDENTS_PROJECTION_MAP);
                break;
            default:
                throw new UnsupportedOperationException("Unknown uri:" + uri);

        }
        Cursor c = qb.query(db, projection, selection,selectionArgs,null,null,sortOrder);
        c.setNotificationUri(getContext().getContentResolver(),uri);
        return c;
    }

    @Override
    public String getType(Uri uri) {
        final int match = sUriMatcher.match(uri);
        switch (match){
            case ROUTE_PRODUCTS:
                return ProductsContract.Product.CONTENT_TYPE;
            case ROUTE_PRODUCTS_ID:
                return ProductsContract.Product.CONTENT_ITEM_TYPE;
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
    }

    @Override
    public Uri insert(Uri uri, ContentValues contentValues) {
        SQLiteDatabase db = mDatabaseHelper.getWritableDatabase();
        assert db != null;
        final int match = sUriMatcher.match(uri);
        Uri result;
        switch (match){
            case ROUTE_PRODUCTS:
                long id=db.insert(ProductsContract.Product.TABLE_NAME, "", contentValues);
                result = Uri.parse(ProductsContract.Product.CONTENT_URI + "/" + id);
                break;
            case ROUTE_PRODUCTS_ID:
                throw new UnsupportedOperationException("Insert not supported on URI: " + uri);
            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }
        Context ctx = getContext();
        assert ctx != null;
        ctx.getContentResolver().notifyChange(uri,null);
        return result;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        SQLiteDatabase db = mDatabaseHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        int count = 0;
        switch(match){
            case ROUTE_PRODUCTS:
                count = db.delete(ProductsContract.Product.TABLE_NAME, selection,selectionArgs);
                break;
            case ROUTE_PRODUCTS_ID:
                String id = uri.getPathSegments().get(1);
                count = db.delete(ProductsContract.Product.TABLE_NAME, ProductsContract.Product._ID +  " = " + id + (!TextUtils.isEmpty(selection) ? "AND (" + selection + ')': ""), selectionArgs );
                break;
            default:
                throw new UnsupportedOperationException("Unknown Uri:" + uri);
        }
        Context ctx = getContext();
        assert ctx != null;
        ctx.getContentResolver().notifyChange(uri,null);
        return count;
    }

    @Override
    public int update(Uri uri, ContentValues contentValues, String selection, String[] selectionArgs) {
        SQLiteDatabase db = mDatabaseHelper.getWritableDatabase();
        final int match = sUriMatcher.match(uri);
        int count = 0;
        switch(match){
            case ROUTE_PRODUCTS:
                count = db.update(ProductsContract.Product.TABLE_NAME,contentValues, selection,selectionArgs);
                break;
            case ROUTE_PRODUCTS_ID:
                String id = uri.getPathSegments().get(1);
                count = db.update(ProductsContract.Product.TABLE_NAME,contentValues ,ProductsContract.Product._ID +  " = " + id + (!TextUtils.isEmpty(selection) ? "AND (" + selection + ')': ""),selectionArgs );
                break;
            default:
                throw new UnsupportedOperationException("Unknown Uri:" + uri);
        }
        Context ctx = getContext();
        assert ctx != null;
        ctx.getContentResolver().notifyChange(uri,null);
        return count;

    }

    /**
     * SQLite backend for @{link ProductsProvider}.
     *
     * Provides access to an disk-backed, SQLite datastore which is utilized by ProductsProvider. This
     * database should never be accessed by other parts of the application directly.
     */
    static class DbHelper extends SQLiteOpenHelper {
        /** Schema version. */
        public static final int DATABASE_VERSION = 5;
        /** Filename for SQLite file. */
        public static final String DATABASE_NAME = "products.db";

        private static final String TYPE_TEXT = " TEXT";
        private static final String TYPE_INTEGER = " INTEGER";
        private static final String COMMA_SEP = ",";
        /** SQL statement to create "entry" table. */
        private static final String SQL_CREATE_ENTRIES =
                "CREATE TABLE " + ProductsContract.Product.TABLE_NAME + " (" +
                        ProductsContract.Product._ID + " INTEGER PRIMARY KEY," +
                        ProductsContract.Product.COLUMN_NAME_PRODUCT  + TYPE_TEXT + COMMA_SEP +
                        ProductsContract.Product.COLUMN_NAME_PRICE + TYPE_INTEGER + COMMA_SEP +
                        ProductsContract.Product.COLUMN_NAME_DESCRIPTION + TYPE_TEXT + COMMA_SEP +
                        ProductsContract.Product.DIRTY + TYPE_INTEGER + COMMA_SEP +
                        ProductsContract.Product.VERSION + TYPE_INTEGER +")";

        /** SQL statement to drop "product" table. */
        private static final String SQL_DELETE_ENTRIES =
                "DROP TABLE IF EXISTS " + ProductsContract.Product.TABLE_NAME;

        public DbHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            db.execSQL(SQL_CREATE_ENTRIES);
            Log.d("IBHI", "Table created");
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            // This database is only a cache for online data, so its upgrade policy is
            // to simply to discard the data and start over
            db.execSQL(SQL_DELETE_ENTRIES);
            onCreate(db);
            Log.d("IBHI", "Table updates successfully");
        }
    }
}
