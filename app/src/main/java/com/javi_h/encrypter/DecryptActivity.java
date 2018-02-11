package com.javi_h.encrypter;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.provider.OpenableColumns;
import android.support.design.widget.TextInputEditText;
import android.support.v4.app.ShareCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.TextView;

import com.google.firebase.crash.FirebaseCrash;
import com.javi_h.encrypter.modelo.Encryption;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class DecryptActivity extends AppCompatActivity {

    TextView tInfo, tArch;
    File arch;
    TextInputEditText tPass;
    Button bDecrypt, bOpen;
    boolean external = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_decrypt);

        setTitle(R.string.decrypt_file);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        if(getIntent().getAction() == null){
            arch = (File) getIntent().getSerializableExtra("arch");
        }else{
            external = true;
            Uri returnUri = getIntent().getData();
            Cursor returnCursor =
                    getContentResolver().query(returnUri, null, null, null, null);
            int nameIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
            returnCursor.moveToFirst();
            String nombre = returnCursor.getString(nameIndex);
            arch = new File(Environment.getExternalStorageDirectory() + "/Encrypter/" + nombre);
        }
        tArch = findViewById(R.id.tArchD);
        tArch.setText(getString(R.string.selected_file) + ": " + arch.getAbsolutePath());
        tPass = findViewById(R.id.txtPass_dec);
        bDecrypt = findViewById(R.id.bDecrypt);
        bDecrypt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(tPass.getText().toString().length() > 0){
                    new Decrypt().execute();
                }else{
                    AlertDialog alertDialog = new AlertDialog.Builder(DecryptActivity.this).create();
                    alertDialog.setTitle(null);
                    alertDialog.setMessage(getString(R.string.enter_pass));
                    alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            });
                    alertDialog.show();
                }
            }
        });
        bOpen = findViewById(R.id.bOpen);

        tInfo = findViewById(R.id.tInfoD);
        tInfo.setVisibility(View.GONE);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    /**
     * AsyncTask para controlar el descifrado del archivo de forma asíncrona.
     */
    class Decrypt extends AsyncTask<Void, Void, Boolean> {

        ProgressDialog mProgressDialog;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mProgressDialog = new ProgressDialog(DecryptActivity.this);
            mProgressDialog.setMessage(getString(R.string.perform_decrypt));
            mProgressDialog.setIndeterminate(false);
            mProgressDialog.setCancelable(false);
            mProgressDialog.show();
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            File enc_dir = new File(Environment.getExternalStorageDirectory(), "Encrypter");
            if(!enc_dir.exists())
                enc_dir.mkdir();
            try {
                File out = new File(enc_dir, arch.getName().substring(0, arch.getName().indexOf(".encrypted")));
                if(external){
                    InputStream is = getContentResolver().openInputStream(getIntent().getData());
                    OutputStream fos = new FileOutputStream(arch);
                    byte[] buffer = new byte[1024];
                    int read;
                    while ((read = is.read(buffer)) != -1) {
                        fos.write(buffer, 0, read);
                    }
                    is.close();
                    is = null;
                    fos.flush();
                    fos.close();
                    fos = null;
                }
                Encryption.decrypt(tPass.getText().toString(), arch, out);
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(DecryptActivity.this);
                if(prefs.getBoolean("deleteTemp", true)){
                    if(arch.exists())
                        arch.delete();
                }
                return true;
            } catch (Exception e) {
                e.printStackTrace();
                FirebaseCrash.logcat(Log.ERROR, "DecryptActivity", "NPE caught");
                FirebaseCrash.report(e);
            }
            return false;
        }

        @Override
        protected void onPostExecute(Boolean result) {
            super.onPostExecute(result);
            mProgressDialog.dismiss();

            if(result){
                tInfo.setText(R.string.success_decrypt_msg);
                tInfo.setCompoundDrawablesWithIntrinsicBounds(R.drawable.success, 0, 0, 0);
                tInfo.setVisibility(View.VISIBLE);
                tPass.setEnabled(false);
                final File out = new File(Environment.getExternalStorageDirectory(), "Encrypter" + File.separator + arch.getName().substring(0, arch.getName().indexOf(".encrypted")));
                tArch.setText(getString(R.string.saved_to) + ": " + out.getAbsolutePath());
                bDecrypt.setVisibility(View.INVISIBLE);
                bOpen.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Uri contentUri = FileProvider.getUriForFile(DecryptActivity.this, "com.javi_h.encrypter", out);

                        Intent intent = ShareCompat.IntentBuilder.from(DecryptActivity.this)
                                .setStream(contentUri) // uri from FileProvider
                                .setType(getMimeType(out.getAbsolutePath()))
                                .getIntent()
                                .setAction(Intent.ACTION_VIEW)
                                .setDataAndType(contentUri, getMimeType(out.getAbsolutePath()))
                                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                        startActivity(intent);
                    }
                });
                bOpen.setVisibility(View.VISIBLE);
            }else{
                tInfo.setText(R.string.error_decrypt_msg);
                tInfo.setCompoundDrawablesWithIntrinsicBounds(R.drawable.error, 0, 0, 0);
                tInfo.setVisibility(View.VISIBLE);
            }
        }
    }

    /**
     * Extraer tipoMIME de un archivo.
     * @param URI local al alchivo
     * @return
     */
    private static String getMimeType(String url) {
        String type = null;
        String extension = MimeTypeMap.getFileExtensionFromUrl(url);
        if (extension != null) {
            type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
        }
        return type;
    }
}