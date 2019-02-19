package fr.inria.yifan.mysensor.Support;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import static fr.inria.yifan.mysensor.Support.Configuration.STORAGE_FILE_PATH;
import static java.lang.System.currentTimeMillis;

/**
 * This class provides methods including storing and reading data file.
 */

public class FilesIOHelper {

    private static final String TAG = "File IO helper";

    private Context mContext;

    // Constructor
    public FilesIOHelper(Context context) {
        super();
        this.mContext = context;
    }

    // Write file to storage
    public void saveFile(String filename, String filecontent) throws Exception {
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            File folder = new File(Environment.getExternalStorageDirectory().getCanonicalPath() + STORAGE_FILE_PATH);
            if (!folder.exists()) {
                boolean mkdir = folder.mkdir();
            }
            filename = folder + "/" + filename;
            //Log.d(TAG, filename);
            FileOutputStream output = new FileOutputStream(filename);
            output.write(filecontent.getBytes());
            output.close();
            Toast.makeText(mContext, "Success in writing file", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(mContext, "Failed in writing file", Toast.LENGTH_LONG).show();
        }
    }

    // Automatically save the log into local file
    public void autoSave(String filecontent) throws Exception {
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            File folder = new File(Environment.getExternalStorageDirectory().getCanonicalPath() + STORAGE_FILE_PATH);
            if (!folder.exists()) {
                boolean mkdir = folder.mkdir();
            }
            FileOutputStream output = new FileOutputStream(folder + "/" + android.os.Build.MODEL + "_" + currentTimeMillis() + "_AUTOSAVE.csv");
            output.write(filecontent.getBytes());
            output.close();
            Toast.makeText(mContext, "Success auto saving file", Toast.LENGTH_LONG).show();
        } else {
            Toast.makeText(mContext, "Failed auto saving file", Toast.LENGTH_LONG).show();
        }
    }

    // Read file from storage
    public String readFile(String filename) throws IOException {
        StringBuilder sb = new StringBuilder();
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            filename = Environment.getExternalStorageDirectory().getCanonicalPath() + STORAGE_FILE_PATH + "/" + filename;
            FileInputStream input = new FileInputStream(filename);
            byte[] temp = new byte[1024];
            int len;
            while ((len = input.read(temp)) > 0) {
                sb.append(new String(temp, 0, len));
            }
            input.close();
        } else {
            Toast.makeText(mContext, "Failed in reading file", Toast.LENGTH_LONG).show();
        }
        return sb.toString();
    }

    public Uri getFileUri(String filename) throws IOException {
        File file = new File(Environment.getExternalStorageDirectory().getCanonicalPath() + STORAGE_FILE_PATH + "/" + filename);
        return Uri.fromFile(file);
    }

    // Send file via e-mail
    public void sendFile(String address, String subject, Uri attachment) {
        String[] addresses = new String[]{address};
        Intent intent = new Intent(Intent.ACTION_SENDTO);
        intent.setData(Uri.parse("mailto:")); // only email apps should handle this
        intent.putExtra(Intent.EXTRA_EMAIL, addresses);
        intent.putExtra(Intent.EXTRA_SUBJECT, subject);
        intent.putExtra(Intent.EXTRA_STREAM, attachment);
        if (intent.resolveActivity(mContext.getPackageManager()) != null) {
            mContext.startActivity(intent);
        }
    }
}
