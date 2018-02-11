package com.javi_h.encrypter;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Environment;
import android.provider.OpenableColumns;
import android.support.design.widget.TextInputEditText;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.firebase.crash.FirebaseCrash;
import com.javi_h.encrypter.modelo.Encryption;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class EncryptActivity extends AppCompatActivity {

    TextView tInfo, tArch;
    File arch;
    TextInputEditText tPass;
    Button bEncrypt, bShare;
    boolean external = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_encrypt);
        setTitle(R.string.encrypt_file);
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
        tArch = findViewById(R.id.tArch);
        tArch.setText(getString(R.string.selected_file) + ": " + arch.getAbsolutePath());
        tPass = findViewById(R.id.txtPass);
        bEncrypt = findViewById(R.id.bEncrypt);
        bEncrypt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(tPass.getText().toString().length() > 5){
                    new Encrypt().execute();
                }else{
                    AlertDialog alertDialog = new AlertDialog.Builder(EncryptActivity.this).create();
                    alertDialog.setTitle(null);
                    alertDialog.setMessage(getString(R.string.pass_requirement));
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
        bShare = findViewById(R.id.bShare);

        tInfo = findViewById(R.id.tInfo);
        tInfo.setVisibility(View.GONE);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    /**
     * Ciframos en un hilo aparte.
     */
    class Encrypt extends AsyncTask<Void, Void, Boolean> {

        ProgressDialog mProgressDialog;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            mProgressDialog = new ProgressDialog(EncryptActivity.this);
            mProgressDialog.setMessage(getString(R.string.perform_encrypt));
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
                File out = new File(enc_dir, arch.getName() + ".encrypted");
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
                Encryption.encrypt(tPass.getText().toString(), arch, out);
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
                tInfo.setText(R.string.success_encrypt_msg);
                tInfo.setCompoundDrawablesWithIntrinsicBounds(R.drawable.success, 0, 0, 0);
                tInfo.setVisibility(View.VISIBLE);
                tPass.setEnabled(false);
                final File out = new File(Environment.getExternalStorageDirectory(), "Encrypter" + File.separator + arch.getName() + ".encrypted");
                tArch.setText(getString(R.string.saved_to) + ": " + out.getAbsolutePath());
                bEncrypt.setVisibility(View.INVISIBLE);
                bShare.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        // Compartir usando el FileProvider
                        Uri contentUri = FileProvider.getUriForFile(EncryptActivity.this, "com.javi_h.encrypter", out);

                        Intent shareIntent = new Intent(Intent.ACTION_SEND);
                        shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
                        shareIntent.setType("application/encrypted");
                        startActivity(Intent.createChooser(shareIntent, getString(R.string.share_using)));
                    }
                });
                bShare.setVisibility(View.VISIBLE);
            }else{
                tInfo.setText(R.string.error_encrypt_msg);
                tInfo.setCompoundDrawablesWithIntrinsicBounds(R.drawable.error, 0, 0, 0);
                tInfo.setVisibility(View.VISIBLE);
            }

        }
    }
}
