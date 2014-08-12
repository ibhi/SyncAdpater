package in.noobish.mycontentprovider;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;


public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
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
    public void onClickAddProduct(View view){
        //Add a new student
        ContentValues values=new ContentValues();
//        String productName = findViewById(R.id.)

        values.put(ProductsContract.Product.COLUMN_NAME_PRODUCT, ((EditText) findViewById(R.id.txtProductName)).getText().toString());
        values.put(ProductsContract.Product.COLUMN_NAME_PRICE, ((EditText) findViewById(R.id.txtPrice)).getText().toString());
        values.put(ProductsContract.Product.COLUMN_NAME_DESCRIPTION, ((EditText) findViewById(R.id.txtDescription)).getText().toString());
        values.put(ProductsContract.Product.DIRTY, 1);
        values.put(ProductsContract.Product.VERSION, 0);

        Uri uri=getContentResolver().insert(ProductsContract.Product.CONTENT_URI, values);
        Toast.makeText(getBaseContext(),uri.toString(),Toast.LENGTH_LONG).show();
    }

    public void onClickRetrieveProducts(View view){
        Intent intent = new Intent(this, ProductRetrieveActivity.class);
        startActivity(intent);
    }
}
