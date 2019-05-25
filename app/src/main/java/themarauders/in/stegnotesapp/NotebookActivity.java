package themarauders.in.stegnotesapp;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

public class NotebookActivity extends AppCompatActivity {
    public static final String ID = "id";
    public int value;
    EditText EditText1;
    private SQLiteDatabase db;
    private String entered_text;
    private String en_text, secretkey;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        secretkey = intent.getStringExtra("Key");
        Toast.makeText(getBaseContext(), "Recieved string: " + secretkey, Toast.LENGTH_SHORT).show();
        setContentView(R.layout.activity_notebook);
        EditText1 = findViewById(R.id.EditText1);
        Toolbar toolbar = findViewById(R.id.toolbar_notebook);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
//                File directory;
//                directory = getFilesDir();
//                File[] files = directory.listFiles();
//                f = (files.length) + 1;
//                Save("Note" + f + ".txt");

                entered_text = EditText1.getText().toString();
                AES aes = new AES();
                AES.setKey(secretkey);
                AES.encrypt(entered_text);
                en_text = AES.getEncryptedString();
                Log.e("ENCRYPTIONTEST", en_text);
                try {
                    db = getBaseContext().openOrCreateDatabase("note.db", MODE_PRIVATE, null);
                    final String create_db = "CREATE TABLE IF NOT EXISTS note_info("
                            + "id INTEGER PRIMARY KEY ,"
                            + "en_key TEXT);";
                    db.execSQL(create_db);
                    value = data_result();
                    String temp = Integer.toString(value);
                    Log.e("value=", temp);
                    value = value + 1;
                    final String insert_values = "INSERT INTO note_info VALUES('" + value + "','" + en_text + "')";
                    Log.e("Inserting value", insert_values);
                    db.execSQL(insert_values);
                    Toast.makeText(getBaseContext(),"Note saved",Toast.LENGTH_SHORT).show();

                } catch (Exception e) {
                    Log.e("ERROR", e.toString());
                }
                finally {
                    EditText1.setText("");
                    EditText1.setHint(R.string.type_here);
                }
            }
        });




//        EditText1.setText(Open("Note" + f + ".txt"));
    }


    public int data_result() {
        String query = "SELECT * FROM note_info";
        int i = 0;
        Cursor cursor = db.rawQuery(query, null);
        String data;

        if (cursor.moveToFirst()) {
            do {
                data = cursor.getString(0);
                i = Integer.parseInt(data);
                Log.e("Getting value", Integer.toString(i));

            } while (cursor.moveToNext());
        }
        cursor.close();
        return i;
    }








//    public void Save(String filename) {
//        try {
//            OutputStreamWriter out = new OutputStreamWriter(openFileOutput(filename, 0));
//            out.write(EditText1.getText().toString());
//            out.close();
//            Toast.makeText(this, filename + "Saved!", Toast.LENGTH_SHORT).show();
//
//        } catch (Throwable t) {
//            Toast.makeText(this, "Exception:" + t.toString(), Toast.LENGTH_SHORT).show();
//        }
//    }
//


    //
//    public boolean FileExists(String fname) {
//        File file = getBaseContext().getFileStreamPath(fname);
//        return file.exists();
//    }
//
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_notebook, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        Intent myIntent = new Intent(NotebookActivity.this, NoteSelect.class);
        myIntent.putExtra("Key", secretkey);
        NotebookActivity.this.startActivity(myIntent);
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
