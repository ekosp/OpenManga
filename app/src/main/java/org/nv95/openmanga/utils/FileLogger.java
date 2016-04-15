package org.nv95.openmanga.utils;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v7.app.AlertDialog;
import android.widget.Toast;

import org.nv95.openmanga.OpenMangaApplication;
import org.nv95.openmanga.R;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by nv95 on 16.10.15.
 */
public class FileLogger implements Thread.UncaughtExceptionHandler {
    private static FileLogger instance;
    private Context context;
    private Thread.UncaughtExceptionHandler oldHandler;

    private FileLogger(Context context) {
        this.context = context;
    }

    public static FileLogger getInstance() {
        return instance;
    }

    public static void init(Context context) {
        instance = new FileLogger(context);
        instance.oldHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(instance);
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        final int logVersion = prefs.getInt("_log_version", 0);
        if (logVersion < OpenMangaApplication.getVersion()) {
            prefs.edit().putInt("_log_version", OpenMangaApplication.getVersion()).apply();
            instance.upgrade();
        }
    }

    private void upgrade() {
        try {
            File file = getLogFile(context);
            file.delete();
            FileOutputStream ostream = new FileOutputStream(file, true);
            ostream.write(("Init logger: " + AppHelper.getReadableDateTime(System.currentTimeMillis())
                    + "\nApp version: " + OpenMangaApplication.getVersion()
                    + " (" + OpenMangaApplication.getVersionName()
                    + ")\n\nDevice info:\n" + Build.FINGERPRINT
                    + "\nAndroid: " + Build.VERSION.RELEASE + " (API v" + Build.VERSION.SDK_INT + ")\n\n").getBytes());
            ostream.flush();
            ostream.close();
        } catch (Exception e) {
            //ну и фиг с ним
        }
    }

    public static void sendLog(final Context context) {
        new AlertDialog.Builder(context).setTitle(R.string.bug_report)
                .setMessage(R.string.bug_report_message)
                .setPositiveButton(R.string.send, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        File file = getLogFile(context);
                        if (!file.exists()) {
                            Toast.makeText(context, R.string.log_not_found, Toast.LENGTH_SHORT).show();
                            return;
                        }
                        Intent emailIntent = new Intent(android.content.Intent.ACTION_SEND);
                        emailIntent.setType("message/rfc822");
                        emailIntent.putExtra(android.content.Intent.EXTRA_EMAIL, new String[]
                                {"nvasya95@gmail.com"});
                        emailIntent.putExtra(android.content.Intent.EXTRA_SUBJECT,
                                "Error report for OpenManga");
                        emailIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse("file://" + file.getAbsolutePath()));
                        context.startActivity(Intent.createChooser(emailIntent, context.getString(R.string.bug_report)));
                    }
                }).setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        }).create().show();
    }

    public void report(String msg) {
        try {
            File file = getLogFile(context);
            FileOutputStream ostream = new FileOutputStream(file, true);
            msg += "\n **************** \n";
            ostream.write(msg.getBytes());
            ostream.flush();
            ostream.close();
            //Toast.makeText(context, R.string.exception_logged, Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void report(Exception e) {
        report(e.getMessage());
    }

    @Override
    public void uncaughtException(Thread thread, Throwable ex) {
        report("!CRASH\n" + ex.getMessage() + "\n\n" + ex.getCause() + "\n");
        if (oldHandler != null)
            oldHandler.uncaughtException(thread, ex);
    }

    protected static File getLogFile(Context context) {
        return new File(context.getExternalFilesDir("debug"), "log.txt");
    }
}