package in.noobish.mycontentprovider;

import android.app.Activity;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import in.noobish.mycontentprovider.R;

public class EditProductActivity extends Activity {


    EditText ediTextName;
    EditText editTextPrice;
    EditText editTextDescription;


    long rowId;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_product);
        String productName;
        String productPrice;
        String productDescription;

        ediTextName=(EditText)findViewById(R.id.productName);
        editTextPrice = (EditText) findViewById(R.id.productPrice);
        editTextDescription= (EditText) findViewById(R.id.productDescription);
        Intent intent=getIntent();
        rowId= intent.getLongExtra("rowId",0);
        productName = intent.getStringExtra("name");
//        Log.d("IBHI", "Product Name is"+ productName);
        productPrice=intent.getStringExtra("price");
        productDescription=intent.getStringExtra("description");
        ediTextName.setText(productName);
        editTextPrice.setText(productPrice);
        editTextDescription.setText(productDescription);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.edit_product, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    public void onClickUpdateProduct(View view){
        ContentValues values=new ContentValues();

        values.put(ProductsContract.Product.COLUMN_NAME_PRODUCT, ediTextName.getText().toString());
        values.put(ProductsContract.Product.COLUMN_NAME_PRICE, editTextPrice.getText().toString());
        values.put(ProductsContract.Product.COLUMN_NAME_DESCRIPTION, editTextDescription.getText().toString());
        values.put(ProductsContract.Product.DIRTY, 1);
//        values.put(ProductsContract.Product.VERSION, 0);

        Uri updateUri = ContentUris.withAppendedId(ProductsContract.Product.CONTENT_URI, rowId);
        Log.d("IBHI", "Update URI is " + updateUri);
        int count=getContentResolver().update(updateUri, values, null, null);
        getContentResolver().notifyChange(ProductsContract.Product.CONTENT_URI,null,false);
        Toast.makeText(getBaseContext(), "Updated Successfully" + count, Toast.LENGTH_LONG).show();

    }
}
