package in.noobish.mycontentprovider;

import android.util.JsonReader;
import android.util.JsonToken;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by ibhi on 3/8/14.
 */
public class ProductsParser {

    public List<Products> readProducts(InputStream in) throws IOException {
        JsonReader reader = new JsonReader(new InputStreamReader(in, "UTF-8"));
        List<Products> finalProducts;
        try {
            reader.beginObject();

            String name = reader.nextName();
            Log.d("IBHI", "Name is " + name);
            if (name.equals("products")){
                finalProducts = readProductsArray(reader);
            }
            else {

                finalProducts = null;
            }
            String success = reader.nextName();
            Log.d("IBHI", "Success is " + success);
            reader.skipValue();
            reader.endObject();

            return finalProducts;

        }
        finally{
            reader.close();
            in.close();

        }

    }


    public List<Products> readProductsArray(JsonReader reader) throws IOException {
        List<Products> products = new ArrayList<Products>();

        reader.beginArray();
        while (reader.hasNext()) {
            products.add(readProduct(reader));
        }
        reader.endArray();
        return products;
    }

    public Products readProduct(JsonReader reader) throws IOException {
        String id = null;
        String name = null;
        String price = null;
        String description = null;

        reader.beginObject();
        while (reader.hasNext()) {
            String tokenName = reader.nextName();
            if (tokenName.equals("pid")) {
                id = reader.nextString();
            } else if (tokenName.equals("name")) {
                name = reader.nextString();
            } else if (tokenName.equals("price") && reader.peek() != JsonToken.NULL) {
                price = reader.nextString();
            } else if (tokenName.equals("description")) {
                description = reader.nextString();
            } else {
                reader.skipValue();
            }
        }
        reader.endObject();
        return new Products(id,name,price,description );
    }
    public static class Products {
        public final String id;
        public final String name;
        public final String price;
        public final String description;
        Products(String id, String name, String price, String description) {
            this.id = id;
            this.name = name;
            this.price = price;
            this.description = description;
        }
    }
}
