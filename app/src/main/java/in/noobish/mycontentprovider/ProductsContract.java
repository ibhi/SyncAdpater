package in.noobish.mycontentprovider;

import android.content.ContentResolver;
import android.net.Uri;
import android.provider.BaseColumns;

/**
 * Created by ibhi on 1/8/14.
 * */

 /**
 * Field and table name constants for
 * {@link com.example.android.network.sync.basicsyncadapter.provider.FeedProvider}.
 */

public class ProductsContract {
    private ProductsContract(){

    }

    /**
     * Content provider authority.
     */
    public static final String CONTENT_AUTHORITY = "in.noobish.mycontentprovider";

    /**
     * Base URI. (content://in.noobish.mycontentprovider)
     */
    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);

    /**
     * Path component for "product"-type resources..
     */
    private static final String PATH_PRODUCTS = "products";

    /**
     * Columns supported by "products" records.
     */
    public static class Product implements BaseColumns {
        /**
         * MIME type for lists of products.
         */
        public static final String CONTENT_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + "/vnd.mycontentprovider.products";
        /**
         * MIME type for individual product.
         */
        public static final String CONTENT_ITEM_TYPE =
                ContentResolver.CURSOR_ITEM_BASE_TYPE + "/vnd.mycontentprovider.product";

        /**
         * Fully qualified URI for "products" resources.
         */
        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_PRODUCTS).build();

        /**
         * Table name where records are stored for "product" resources.
         */
        public static final String TABLE_NAME = "product";

        /**
         * Product name.
         */
        public static final String COLUMN_NAME_PRODUCT = "product";
        /**
         * Product price.
         */
        public static final String COLUMN_NAME_PRICE = "price";
        /**
         * Product description.
         */
        public static final String COLUMN_NAME_DESCRIPTION = "description";

        public static final String _ID = "_id";

        public static final String DIRTY = "dirty";

        public static final String VERSION = "version";
    }

}
