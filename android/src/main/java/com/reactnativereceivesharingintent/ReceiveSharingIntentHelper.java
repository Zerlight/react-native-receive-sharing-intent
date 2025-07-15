package com.reactnativereceivesharingintent;

import android.app.Application;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.provider.OpenableColumns;
import android.util.Log;

import androidx.annotation.RequiresApi;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableNativeMap;

import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Objects;

public class ReceiveSharingIntentHelper {

    private Context context;

    public ReceiveSharingIntentHelper(Application context){
        this.context = context;
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public void sendFileNames(Context context, Intent intent, Promise promise){
        try {
            if (intent == null) {
                promise.resolve(null);
                return;
            }
            
            String action = intent.getAction();
            String type = intent.getType();
            
            if (Objects.equals(action, Intent.ACTION_SEND) || Objects.equals(action, Intent.ACTION_SEND_MULTIPLE)) {
                if (type != null && type.startsWith("text/plain")) {
                    String text = intent.getStringExtra(Intent.EXTRA_TEXT);
                    String subject = intent.getStringExtra(Intent.EXTRA_SUBJECT);
                    WritableMap file = new WritableNativeMap();
                    if (text != null && text.startsWith("http")) {
                        file.putString("weblink", text);
                    } else {
                        file.putString("text", text);
                    }
                    file.putString("subject", subject);
                    WritableMap files = new WritableNativeMap();
                    files.putMap("0", file);
                    promise.resolve(files);
                } else {
                    WritableMap files = getMediaUris(intent, context);
                    promise.resolve(files);
                }
            } else if (Objects.equals(action, Intent.ACTION_VIEW)) {
                Uri uri = intent.getData();
                if (uri != null) {
                    String scheme = uri.getScheme();
                    if ("content".equals(scheme) || "file".equals(scheme)) {
                        WritableMap files = new WritableNativeMap();
                        WritableMap fileDetails = getFileDetails(context, uri);
                        files.putMap("0", fileDetails);
                        promise.resolve(files);
                    } else {
                        String link = intent.getDataString();
                        WritableMap files = new WritableNativeMap();
                        WritableMap file = new WritableNativeMap();
                        file.putString("weblink", link);
                        files.putMap("0", file);
                        promise.resolve(files);
                    }
                } else {
                    promise.resolve(null);
                }
            } else if ("android.intent.action.PROCESS_TEXT".equals(action)) {
                // This remains unchanged
                String text = intent.getStringExtra(intent.EXTRA_PROCESS_TEXT);
                WritableMap files = new WritableNativeMap();
                WritableMap file = new WritableNativeMap();
                file.putString("text", text);
                files.putMap("0", file);
                promise.resolve(files);
            } else {
                promise.resolve(null);
            }
        } catch (Exception e) {
            promise.reject("error", e.toString());
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    public WritableMap getMediaUris(Intent intent, Context context){
        if (intent == null) return null;

        WritableMap files = new WritableNativeMap();
        if (Intent.ACTION_SEND.equals(intent.getAction())) {
            Uri contentUri = intent.getParcelableExtra(Intent.EXTRA_STREAM);
            if (contentUri != null) {
                files.putMap("0", getFileDetails(context, contentUri));
            }
        } else if (Intent.ACTION_SEND_MULTIPLE.equals(intent.getAction())) {
            ArrayList<Uri> contentUris = intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM);
            if (contentUris != null) {
                int index = 0;
                for (Uri uri : contentUris) {
                    files.putMap(Integer.toString(index), getFileDetails(context, uri));
                    index++;
                }
            }
        }
        return files;
    }
    
    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    private WritableMap getFileDetails(Context context, Uri uri) {
        WritableMap file = new WritableNativeMap();
        ContentResolver contentResolver = context.getContentResolver();
        file.putString("mimeType", contentResolver.getType(uri));
        Cursor queryResult = contentResolver.query(uri, null, null, null, null);
        if (queryResult != null && queryResult.moveToFirst()) {
            try {
                int nameIndex = queryResult.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                if (nameIndex != -1) {
                    file.putString("fileName", queryResult.getString(nameIndex));
                }
            } catch (Exception e) {
                Log.e("ReceiveSharingIntent", "Error getting file name: " + e.getMessage());
            } finally {
                queryResult.close();
            }
        }
        String filePath = FileDirectory.INSTANCE.getAbsolutePath(context, uri);
        file.putString("filePath", filePath);
        file.putString("contentUri", uri.toString());
        
        return file;
    }

    public void clearFileNames(Intent intent){
        String type = intent.getType();
        if(type == null) return;
        if (type.startsWith("text")) {
          intent.removeExtra(Intent.EXTRA_TEXT);
        } else {
          intent.removeExtra(Intent.EXTRA_STREAM);
        }
    }
    
    public String getFileName(String file){
        return  file.substring(file.lastIndexOf('/') + 1);
    }

    public String getExtension(String file){
        return file.substring(file.lastIndexOf('.') + 1);
    }
}