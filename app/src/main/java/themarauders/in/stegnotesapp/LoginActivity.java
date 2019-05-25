package themarauders.in.stegnotesapp;

import android.Manifest;
import android.app.AlertDialog;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.CursorLoader;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.stealthcopter.steganography.Steg;
import themarauders.in.stegnotesapp.*;

import org.apache.commons.codec.android.binary.Hex;
import org.apache.commons.codec.android.digest.DigestUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import com.google.zxing.qrcode.*;

import static android.graphics.Color.BLACK;
import static android.graphics.Color.WHITE;

/**
 * A login screen that offers login via email/password.
 */
public class LoginActivity extends AppCompatActivity implements LoaderCallbacks<Cursor> {

    /**
     * Id to identity READ_CONTACTS permission request.
     */
    private static final int GALLERY_PICTURE = 1;
    private static final int CAMERA_REQUEST = 2;
    /**
     * A dummy authentication store containing known user names and passwords.
     * TODO: remove after connecting to a real authentication system.
     */
    private static final String[] DUMMY_CREDENTIALS = new String[]{
            "foo@example.com:hello", "bar@example.com:world"
    };
    private String hashedPass, imageToBeSteganographedPath;
    /**
     * Keep track of the login task to ensure we can cancel it if requested.
     */
    private UserLoginTask mAuthTask = null;

    // UI references.
    private AutoCompleteTextView mEmailView;
    private EditText mPasswordView;
    private EditText mVerifyPasswordView;
    private TextView mHashedKeyView;
    private Button mShareButton, mStegButton, mDoneButton, mQRButton;
    private File ourFolder;
    private SQLiteDatabase sqLiteDatabase;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        // Set up the login form.

        mPasswordView = findViewById(R.id.password);
        mVerifyPasswordView = findViewById(R.id.verify_password);

        // Database!
        sqLiteDatabase = getBaseContext().openOrCreateDatabase("stegnotes.db", MODE_PRIVATE, null);
        sqLiteDatabase.execSQL("CREATE TABLE IF NOT EXISTS logins(secretKey TEXT);");
        Cursor query = sqLiteDatabase.rawQuery("SELECT * FROM  logins;", null);
        if (query.moveToFirst()) {
            hashedPass = query.getString(0);
            Intent noteActivity = new Intent(getApplicationContext(), NotebookActivity.class);
            noteActivity.putExtra("Key", hashedPass);
            startActivity(noteActivity);
            query.close();
            finish();
        }

        mVerifyPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == EditorInfo.IME_ACTION_DONE || id == EditorInfo.IME_NULL) {
                    attemptLogin();
                    return true;
                }
                return false;
            }
        });

        Button mEmailSignInButton = findViewById(R.id.email_sign_in_button);
        mEmailSignInButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptLogin();
            }
        });

    }


//
//    private boolean mayRequestContacts() {
//        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
//            return true;
//        }
//        if (checkSelfPermission(READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
//            return true;
//        }
//        if (shouldShowRequestPermissionRationale(READ_CONTACTS)) {
//            Snackbar.make(mEmailView, R.string.permission_rationale, Snackbar.LENGTH_INDEFINITE)
//                    .setAction(android.R.string.ok, new View.OnClickListener() {
//                        @Override
//                        @TargetApi(Build.VERSION_CODES.M)
//                        public void onClick(View v) {
//                            requestPermissions(new String[]{READ_CONTACTS}, REQUEST_READ_CONTACTS);
//                        }
//                    });
//        } else {
//            requestPermissions(new String[]{READ_CONTACTS}, REQUEST_READ_CONTACTS);
//        }
//        return false;
//    }
//
//    /**
//     * Callback received when a permissions request has been completed.
//     */
//    @Override
//    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
//                                           @NonNull int[] grantResults) {
//        if (requestCode == REQUEST_READ_CONTACTS) {
//            if (grantResults.length == 1 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
//                populateAutoComplete();
//            }
//        }
//    }


    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    private void attemptLogin() {
        if (mAuthTask != null) {
            return;
        }

        // Reset errors.
        mPasswordView.setError(null);

        // Store values at the time of the login attempt.
        String password = mPasswordView.getText().toString();
        String verify_password = mVerifyPasswordView.getText().toString();


        boolean cancel = false;
        View focusView = null;

        // Check for a valid password, if the user entered one.
        if (TextUtils.isEmpty(password) || !isPasswordValid(password)) {
            mPasswordView.setError(getString(R.string.error_invalid_password));
            focusView = mPasswordView;
            cancel = true;
        }
        if(!(password.equals(verify_password))){
            mVerifyPasswordView.setError(getString(R.string.error_incorrect_password));
            focusView = mVerifyPasswordView;
            cancel = true;
        }


        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {

            // Request required permissions
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{
                                Manifest.permission.READ_EXTERNAL_STORAGE,
                                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                                Manifest.permission.CAMERA
                        }, 1);
            }

            AlertDialog.Builder mBuilder = new AlertDialog.Builder(LoginActivity.this);
            View popupView = getLayoutInflater().inflate(R.layout.export_options_layout, null);

            mHashedKeyView = popupView.findViewById(R.id.hashed_key_view);
            hashedPass = new String(Hex.encodeHex(DigestUtils.sha256(password)));
            mHashedKeyView.setText(hashedPass);

            mBuilder.setView(popupView);
            AlertDialog dialog = mBuilder.create();
            dialog.show();

            mShareButton = popupView.findViewById(R.id.share_button);
            mStegButton = popupView.findViewById(R.id.exportsteg_button);
            mQRButton = popupView.findViewById(R.id.qr_button);
            mDoneButton = popupView.findViewById(R.id.done_button);

            mShareButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent shareHashIntent = new Intent();
                    shareHashIntent.setAction(Intent.ACTION_SEND);
                    shareHashIntent.putExtra(Intent.EXTRA_TEXT, hashedPass);
                    shareHashIntent.setType("text/plain");
                    startActivity(Intent.createChooser(shareHashIntent, getResources().getText(R.string.send_to)));

                }
            });

            mQRButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    Log.e("qrcode", "onClick: Listener" );
                    try {
                        File ourFolder = new File(Environment.getExternalStorageDirectory().toString() + "/StegNotes/");
                        ourFolder.mkdirs();
                        Bitmap bitmap = encodeAsBitmap(hashedPass);
                        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 90, bytes);
                        File qrImage = new File(ourFolder.toString()+"/stegqr.jpg");
                        qrImage.createNewFile();
                        Log.e("QRBUTTON", "onClick: QRIMAGE "+qrImage.getAbsolutePath() );
                        FileOutputStream fileOutputStream = new FileOutputStream(qrImage);
                        fileOutputStream.write(bytes.toByteArray());

                    }
                    catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });

            mStegButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    startGalleryDialog();
                }
            });

            mDoneButton.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View view) {
                    sqLiteDatabase.execSQL("INSERT INTO logins VALUES('" + hashedPass + "');");
                    Cursor query = sqLiteDatabase.rawQuery("SELECT * FROM  logins;", null);
                    if (query.moveToFirst()) {
                        hashedPass = query.getString(0);
                        Intent noteActivity = new Intent(getApplicationContext(), NotebookActivity.class);
                        noteActivity.putExtra("Key", hashedPass);
                        startActivity(noteActivity);
                        query.close();
                        finish();
                    } else {
                        Toast.makeText(getApplicationContext(), "Error in saving the key! Restart the app", Toast.LENGTH_LONG).show();
                    }

                }
            });

        }
    }
    private void startGalleryDialog() {
        ourFolder = new File(Environment.getExternalStorageDirectory().toString() + "/StegNotes/");
        ourFolder.mkdirs();
        AlertDialog.Builder mGalleryOrCamDialog = new AlertDialog.Builder(LoginActivity.this);
        mGalleryOrCamDialog.setTitle(R.string.select_picture);
        mGalleryOrCamDialog.setMessage(R.string.gallery_or_camera);

        mGalleryOrCamDialog.setPositiveButton("Gallery", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                Intent pictureActionIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(pictureActionIntent, GALLERY_PICTURE);
            }
        });
        mGalleryOrCamDialog.setNegativeButton("Camera",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface arg0, int arg1) {

                        Intent intent = new Intent(
                                MediaStore.ACTION_IMAGE_CAPTURE);
                        File f = new File(ourFolder, "stegnotetemp.jpg"); // Creates new file at root of internal storage.
                        Log.e("CAMERA", "onClick: HERE");
                        intent.putExtra(MediaStore.EXTRA_OUTPUT,
                                FileProvider.getUriForFile(getApplicationContext(), getPackageName() + ".fileprovider", f)); // Puts the data into the created file
                        startActivityForResult(intent,
                                CAMERA_REQUEST);

                    }
                });
        mGalleryOrCamDialog.show();

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        Bitmap bitmap = null;

        if (resultCode == RESULT_OK && requestCode == CAMERA_REQUEST) {

            // Get the list of all files in sdcard/
            File f = new File(ourFolder.toString());
            // Check if any of the files match out temp image
            for (File temp : f.listFiles()) {
                if (temp.getName().equals("stegnotetemp.jpg")) {
                    f = temp;
                    imageToBeSteganographedPath = f.getPath();
                    break;
                }
            }
            if (!f.exists()) {
                Toast.makeText(getBaseContext(),
                        "Error while capturing image", Toast.LENGTH_LONG)
                        .show();
                return;
            }


        } else if (resultCode == RESULT_OK && requestCode == GALLERY_PICTURE) {

            // Intent has data
            if (data != null) {

                Uri selectedImage = data.getData();

                String[] filePath = {MediaStore.Images.Media.DATA}; // Get all images' data from the mediastore.
                Cursor c = getContentResolver().query(selectedImage, filePath,
                        null, null, null);  // Match the one with the same path(?) ad the Intent data().
                c.moveToFirst(); // store the matched query result int the first row
                int columnIndex = c.getColumnIndex(filePath[0]);
                imageToBeSteganographedPath = c.getString(columnIndex);
                c.close();

            }

        }
        if (imageToBeSteganographedPath != null) {
            Log.e("onActivityResult", imageToBeSteganographedPath + "\n\n\n\n\n\n\n\n\n");
            startSteg();
        }
    }

    private void startSteg() {
        // TEST - Background steganography
        String hiddenMessage = hashedPass;

        BitmapFactory.Options bmOptions = new BitmapFactory.Options();

        Bitmap bitmap = BitmapFactory.decodeFile(imageToBeSteganographedPath, bmOptions);
        Log.e("STEGENCODE", "File path: " + imageToBeSteganographedPath);

        // Save the new bitmap in that folder , with filename ending with date.
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String date = dateFormat.format(new java.util.Date());

        String newStegImagePath = ourFolder.getAbsolutePath();
        newStegImagePath = newStegImagePath + "/IMGST_" + date + ".png";

        try {
            File encodedBitmap = Steg.withInput(bitmap).encode(hiddenMessage).intoFile(newStegImagePath);
            Log.e("STEGENCODE", "Encoded!");


            Log.e("STEGIMG", "File path: " + newStegImagePath);


            bitmap = BitmapFactory.decodeFile(newStegImagePath, bmOptions);
            String decodedMessage = Steg.withInput(bitmap).decode().intoString();

            Toast.makeText(getApplicationContext(), "Seganography Image path:\n" + newStegImagePath, Toast.LENGTH_LONG).show();
            Log.e(getClass().getSimpleName(), "decoded string: " + decodedMessage);

        } catch (Exception e) {
            System.out.print("\n\n\n\n\nexceptopn\n\n\n\n\n");
        }
    }

    Bitmap encodeAsBitmap (String str) throws WriterException {
        BitMatrix result;
        try {
            result = new MultiFormatWriter().encode(str,
                    BarcodeFormat.QR_CODE, 1200, 1200, null);
        } catch (IllegalArgumentException iae) {
            // Unsupported format
            return null;
        }
        int w = result.getWidth();
        int h = result.getHeight();
        int[] pixels = new int[w * h];
        for (int y = 0; y < h; y++) {
            int offset = y * w;
            for (int x = 0; x < w; x++) {
                pixels[offset + x] = result.get(x, y) ? BLACK : WHITE;
            }
        }
        Bitmap bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        bitmap.setPixels(pixels, 0, 1200, 0, 0, w, h);
        return bitmap;
    }

    private boolean isPasswordValid(String password) {
        //TODO: Replace this with your own logic
        return password.length() > 4;
    }
//
//    /**
//     * Shows the progress UI and hides the login form.
//     */
//    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
//    private void showProgress(final boolean show) {
//        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
//        // for very easy animations. If available, use these APIs to fade-in
//        // the progress spinner.
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
//            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);
//
//            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
//            mLoginFormView.animate().setDuration(shortAnimTime).alpha(
//                    show ? 0 : 1).setListener(new AnimatorListenerAdapter() {
//                @Override
//                public void onAnimationEnd(Animator animation) {
//                    mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
//                }
//            });
//
//            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
//            mProgressView.animate().setDuration(shortAnimTime).alpha(
//                    show ? 1 : 0).setListener(new AnimatorListenerAdapter() {
//                @Override
//                public void onAnimationEnd(Animator animation) {
//                    mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
//                }
//            });
//        } else {
//            // The ViewPropertyAnimator APIs are not available, so simply show
//            // and hide the relevant UI components.
//            mProgressView.setVisibility(show ? View.VISIBLE : View.GONE);
//            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
//        }
//    }

    @Override
    public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
        return new CursorLoader(this,
                // Retrieve data rows for the device user's 'profile' contact.
                Uri.withAppendedPath(ContactsContract.Profile.CONTENT_URI,
                        ContactsContract.Contacts.Data.CONTENT_DIRECTORY), ProfileQuery.PROJECTION,

                // Select only email addresses.
                ContactsContract.Contacts.Data.MIMETYPE +
                        " = ?", new String[]{ContactsContract.CommonDataKinds.Email
                .CONTENT_ITEM_TYPE},

                // Show primary email addresses first. Note that there won't be
                // a primary email address if the user hasn't specified one.
                ContactsContract.Contacts.Data.IS_PRIMARY + " DESC");
    }

    @Override
    public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor cursor) {
        List<String> emails = new ArrayList<>();
        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            emails.add(cursor.getString(ProfileQuery.ADDRESS));
            cursor.moveToNext();
        }

        addEmailsToAutoComplete(emails);
    }

    @Override
    public void onLoaderReset(Loader<Cursor> cursorLoader) {

    }

    private void addEmailsToAutoComplete(List<String> emailAddressCollection) {
        //Create adapter to tell the AutoCompleteTextView what to show in its dropdown list.
        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(LoginActivity.this,
                        android.R.layout.simple_dropdown_item_1line, emailAddressCollection);

        mEmailView.setAdapter(adapter);
    }


    private interface ProfileQuery {
        String[] PROJECTION = {
                ContactsContract.CommonDataKinds.Email.ADDRESS,
                ContactsContract.CommonDataKinds.Email.IS_PRIMARY,
        };

        int ADDRESS = 0;
        int IS_PRIMARY = 1;
    }

    /**
     * Represents an asynchronous login/registration task used to authenticate
     * the user.
     */
    public class UserLoginTask extends AsyncTask<Void, Void, Boolean> {

        private final String mEmail;
        private final String mPassword;

        UserLoginTask(String email, String password) {
            mEmail = email;
            mPassword = password;
        }

        @Override
        protected Boolean doInBackground(Void... params) {
            // TODO: attempt authentication against a network service.

            try {
                // Simulate network access.
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                return false;
            }

            for (String credential : DUMMY_CREDENTIALS) {
                String[] pieces = credential.split(":");
                if (pieces[0].equals(mEmail)) {
                    // Account exists, return true if the password matches.
                    return pieces[1].equals(mPassword);
                }
            }

            // TODO: register the new account here.
            return true;
        }
//
//        @Override
//        protected void onPostExecute(final Boolean success) {
//            mAuthTask = null;
//            showProgress(false);
//
//            if (success) {
//                finish();
//            } else {
//                mPasswordView.setError(getString(R.string.error_incorrect_password));
//                mPasswordView.requestFocus();
//            }
//        }
//
//        @Override
//        protected void onCancelled() {
//            mAuthTask = null;
//            showProgress(false);
//        }
    }
}

