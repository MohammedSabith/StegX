package themarauders.in.stegnotesapp;

import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.stealthcopter.steganography.Steg;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class NoteSelect extends AppCompatActivity {
    private List<NotesBuilder> notesList = new ArrayList();
    private NotesAdapter nAdapter;
    private RecyclerView notesRecycler;
    private SQLiteDatabase db;
    private View popupView;
    private String secretkey, stegImgPath;
    private String stringToBeDecrypted;
    private AlertDialog dialog;
    private int idOfstringToBeDecrypted;
    private LinearLayout selectedLayout;
    private TextView selectedTextView;
    private boolean keyMatches = false;
    private boolean DECRYPT_ALL = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Intent intent = getIntent();
        secretkey = intent.getStringExtra("Key");
        Toast.makeText(getBaseContext(), "Recieved string: " + secretkey, Toast.LENGTH_SHORT).show();
        setContentView(R.layout.activity_note_select);
        Toolbar toolbar = findViewById(R.id.toolbar_notebook);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent i = new Intent(NoteSelect.this, NotebookActivity.class);
                i.putExtra("Key", secretkey);
                i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(i);

            }
        });

        notesRecycler = findViewById(R.id.notes);

        nAdapter = new NotesAdapter(notesList);
        RecyclerView.LayoutManager mLayoutManager =
                new LinearLayoutManager(getApplicationContext());
        notesRecycler.setLayoutManager(mLayoutManager);
        notesRecycler.setItemAnimator(new DefaultItemAnimator());
        notesRecycler.setAdapter(nAdapter);
        Open();
    }


//    private void prepareNotes() {
//        File directory;
//        directory = getFilesDir();
//        File[] files = directory.listFiles();
//        String theFile;
//        for (int f = 1; f <= files.length; f++) {
//            theFile = "Note" + f + ".txt";
//            NotesBuilder note = new NotesBuilder(theFile, Open(theFile));
//            notesList.add(note);
//        }
//
//    }

    public void Open() {

        String query = "SELECT * FROM note_info;";
        db = getBaseContext().openOrCreateDatabase("note.db", MODE_PRIVATE, null);
        Cursor cursor = db.rawQuery(query, null);
        String data, data1;

        if (cursor.moveToFirst()) {
            do {
                Log.e("database: ", "d.exitata = cursor.getString(0);");
                data = cursor.getString(0);
                Log.e("database: data0 = ", data);
                data1 = cursor.getString(1);
                Log.e("database: data1 = ", data1);
                NotesBuilder note = new NotesBuilder(data, data1);
                notesList.add(note);

            } while (cursor.moveToNext());
        }
        cursor.close();
    }

//    public boolean FileExists(String fname) {
//        File file = getBaseContext().getFileStreamPath(fname);
//        return file.exists();
//    }
//
//    public String Open(String fileName) {
//        String content = "";
//        if (FileExists(fileName)) {
//            try {
//                InputStream in = openFileInput(fileName);
//                if (in != null) {
//                    InputStreamReader tmp = new InputStreamReader(in);
//                    BufferedReader reader = new BufferedReader(tmp);
//                    String str;
//                    StringBuilder buf = new StringBuilder();
//                    while ((str = reader.readLine()) != null) {
//                        buf.append(str + "\n");
//                    }
//                    in.close();
//
//                    content = buf.toString();
//                    content = content.trim();
//                }
//            } catch (Throwable t) {
//                Toast.makeText(this, "Exception: " + t.toString(), Toast.LENGTH_LONG).show();
//            }
//        }
//        return content;
//    }

    public void clicked(View view) {
        selectedLayout = (LinearLayout) view;
        Log.e("clicked", "registered: ");
        selectedTextView = view.findViewById(R.id.title);
        idOfstringToBeDecrypted = Integer.parseInt(selectedTextView.getText().toString());
        selectedTextView = view.findViewById(R.id.content);
        stringToBeDecrypted = selectedTextView.getText().toString();
        if (!keyMatches) {
            AlertDialog.Builder mBuilder = new AlertDialog.Builder(NoteSelect.this);
            popupView = getLayoutInflater().inflate(R.layout.ask_steg_layout, null);
            mBuilder.setView(popupView);
            dialog = mBuilder.create();

            dialog.show();
        } else {
            String decryptedString = decryptSecretMessage();
            selectedTextView.setText(decryptedString);
        }
        Log.e(getBaseContext().toString(), "str to be dec = " + stringToBeDecrypted);
        selectedLayout.setClickable(false);

    }

    public void doneButtonHandler(View view) {
        EditText typedHash = popupView.findViewById(R.id.typed_hash);
        String typedHashString = typedHash.getText().toString();
        if (hashCompare(typedHashString, secretkey)) {
            Toast.makeText(getBaseContext(), "KEYMATCHED", Toast.LENGTH_SHORT).show();
            String decryptedString = decryptSecretMessage();
            dialog.dismiss();
            selectedTextView.setText(decryptedString);
            selectedLayout.setClickable(false);
        }
    }


    public void cancelButton(View view) {
        dialog.dismiss();
        finish();
        Intent intent = new Intent(getApplicationContext(), NoteSelect.class);
        intent.putExtra("Key", secretkey);
        startActivity(intent);
    }

    private String decryptSecretMessage() {
        AES.setKey(secretkey);
        AES.decrypt(stringToBeDecrypted);
        return AES.getDecryptedString();
    }

    private boolean hashCompare(String typedHashString, String secretkey) {
        Log.e("insidehashcompare", "hashCompare: " + typedHashString);
        keyMatches = typedHashString.equals(secretkey);
        return keyMatches;
    }

    public void getStegImg(View view) {
        startGalleryDialog();

    }

    private void startGalleryDialog() {
        android.app.AlertDialog.Builder mGalleryOrCamDialog = new android.app.AlertDialog.Builder(NoteSelect.this);
        mGalleryOrCamDialog.setTitle(R.string.select_picture);
        mGalleryOrCamDialog.setMessage(R.string.gallery_or_camera);

        mGalleryOrCamDialog.setPositiveButton("Gallery", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                Intent pictureActionIntent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                pictureActionIntent.addCategory(Intent.CATEGORY_OPENABLE);
                pictureActionIntent.setType("image/*");
                startActivityForResult(pictureActionIntent, 1);
            }
        });

        mGalleryOrCamDialog.show();

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK && requestCode == 1) {

            // Intent has data
            if (data != null) {

                Uri selectedImage = data.getData();

//                String[] filePath = {MediaStore.Images.Media.DATA}; // Get all images' data from the mediastore.
//                Cursor c = getContentResolver().query(selectedImage, filePath,
//                        null, null, null);  // Match the one with the same path(?) ad the Intent data().
//                c.moveToFirst(); // store the matched query result int the first row
//                int columnIndex = c.getColumnIndex(filePath[0]);
                File file = new File(selectedImage.getPath());
                stegImgPath = file.getAbsolutePath();
                Log.e("stegImgPath", stegImgPath + "\n\n\n\n\n\n\n\n\n");
                String temp = stegImgPath.substring(18, stegImgPath.length());
                Log.e("temp", temp + "\n\n\n\n\n\n\n\n\n");
                String sdcardpath = "/storage/emulated/0/";
                Log.e("sdcardpath", sdcardpath + "\n\n\n\n\n\n\n\n\n");
                stegImgPath = sdcardpath + temp;

//                c.close();

            }

        }
        if (stegImgPath != null) {
            Log.e("onActivityResult", stegImgPath + "\n\n\n\n\n\n\n\n\n");
            startSteg();
        }
    }

    private void startSteg() {

        BitmapFactory.Options bmOptions = new BitmapFactory.Options();


        Log.e("STEGENCODE", "File path: " + stegImgPath);


        try {
            Bitmap bitmap = BitmapFactory.decodeFile(stegImgPath, bmOptions);
            String decodedMessage = Steg.withInput(bitmap).decode().intoString();

            Toast.makeText(getApplicationContext(), "Seganography Image path:\n" + stegImgPath, Toast.LENGTH_LONG).show();
            Log.e(getClass().getSimpleName(), "decoded string: " + decodedMessage);
            if (hashCompare(decodedMessage, secretkey)) {
                Toast.makeText(getBaseContext(), "KEYMATCHED", Toast.LENGTH_SHORT).show();
                String decryptedString = decryptSecretMessage();
                dialog.dismiss();
                Log.e("Startsteg", "decString : " + decryptedString + "\n\t selectedTextView = " + selectedTextView.getText().toString());
                selectedTextView.setText(decryptedString);
            }

        } catch (Exception e) {
            System.out.print("\n\n\n\n\nexceptopn\n\n\n\n\n");
        }
    }
}
