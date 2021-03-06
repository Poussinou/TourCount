/**
 * Based on ChangeLog.java
 * Copyright (C) 2011-2013, Karsten Priegnitz
 * <p/>
 * Permission to use, copy, modify, and distribute this piece of software
 * for any purpose with or without fee is hereby granted, provided that
 * the above copyright notice and this permission notice appear in the
 * source code of all copies.
 * <p/>
 * It would be appreciated if you mention the author in your change log,
 * contributors list or the like.
 *
 * @author: Karsten Priegnitz
 * @see: http://code.google.com/p/android-change-log/
 * <p/>
 * Adaptation for ViewHelp:
 * Copyright (c) 2016. Wilhelm Stein, Bonn, Germany.
 */

package sheetrock.panda.changelog;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Color;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.webkit.WebView;

import com.wmstein.tourcount.R;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Locale;

public class ViewHelp
{
    private final Context context;
    private String lastVersion, thisVersion;

    // this is the key for storing the version name in SharedPreferences
    private static final String VERSION_KEY = "PREFS_VERSION_KEY";

    private static final String NO_VERSION = "";

    /**
     * Constructor  <p/>
     * Retrieves the version names and stores the new version name in SharedPreferences
     *
     * @param context
     */
    public ViewHelp(Context context)
    {
        this(context, PreferenceManager.getDefaultSharedPreferences(context));
    }

    /**
     * Constructor <p/>
     * Retrieves the version names and stores the new version name in SharedPreferences
     *
     * @param context
     * @param sp      the shared preferences to store the last version name into
     */
    private ViewHelp(Context context, SharedPreferences sp)
    {
        this.context = context;

        // get version numbers
        this.lastVersion = sp.getString(VERSION_KEY, NO_VERSION);
        Log.d(TAG, "lastVersion: " + lastVersion);
        try
        {
            this.thisVersion = context.getPackageManager().getPackageInfo(
                context.getPackageName(), 0).versionName;
        } catch (NameNotFoundException e)
        {
            this.thisVersion = NO_VERSION;
            Log.e(TAG, "could not get version name from manifest!");
            e.printStackTrace();
        }
        Log.d(TAG, "appVersion: " + this.thisVersion);
    }

    /*********************************************************
     * @return an AlertDialog with a full change log displayed
     */
    public AlertDialog getFullLogDialog()
    {
        return this.getDialog(true);
    }

    private AlertDialog getDialog(boolean full)
    {
        WebView wv = new WebView(this.context);

        wv.setBackgroundColor(Color.BLACK);
        wv.loadDataWithBaseURL(null, this.getLog(full), "text/html", "UTF-8",
            null);

        AlertDialog.Builder builder = new AlertDialog.Builder(
            new ContextThemeWrapper(
                this.context, android.R.style.Theme_Holo_Dialog));
        builder.setTitle(
            context.getResources().getString(
                full ? R.string.viewhelp_full_title
                    : R.string.viewhelp_title))
            .setView(wv)
            .setCancelable(false)
            // OK button
            .setPositiveButton(
                context.getResources().getString(
                    R.string.viewhelp_ok_button),
                new DialogInterface.OnClickListener()
                {
                    public void onClick(DialogInterface dialog,
                                        int which)
                    {
                    }
                });

        if (!full)
        {
            // "more ..." button
            builder.setNegativeButton(R.string.changelog_show_full,
                new DialogInterface.OnClickListener()
                {
                    public void onClick(DialogInterface dialog, int id)
                    {
                        getFullLogDialog().show();
                    }
                });
        }

        return builder.create();
    }

    /**
     * modes for HTML-Lists (bullet, numbered)
     */
    private enum Listmode
    {
        NONE, ORDERED, UNORDERED,
    }

    private Listmode listMode = Listmode.NONE;
    private StringBuffer sb = null;

    private String getLog(boolean full)
    {
        // read viewhelp.txt file
        sb = new StringBuffer();
        try
        {
            String language = Locale.getDefault().toString().substring(0, 2);
            InputStream ins;
            if (language.equals("de"))
            {
                ins = context.getResources().openRawResource(R.raw.viewhelp_de);
            }
            else
            {
                ins = context.getResources().openRawResource(R.raw.viewhelp);
            }
            BufferedReader br = new BufferedReader(new InputStreamReader(ins));
            String line;
            while ((line = br.readLine()) != null)
            {
                line = line.trim();
                char marker = line.length() > 0 ? line.charAt(0) : 0;
                if (marker == '$')
                {
                    // begin of a version section
                    this.closeList();
                    String version = line.substring(1).trim();
                }
                switch (marker)
                {
                case '%':
                    // line contains version title
                    this.closeList();
                    sb.append("<div class='title'>"
                        + line.substring(1).trim() + "</div>\n");
                    break;
                case '_':
                    // line contains version title
                    this.closeList();
                    sb.append("<div class='subtitle'>"
                        + line.substring(1).trim() + "</div>\n");
                    break;
                case '!':
                    // line contains free text
                    this.closeList();
                    sb.append("<div class='freetext'>"
                        + line.substring(1).trim() + "</div>\n");
                    break;
                case '#':
                    // line contains numbered list item
                    this.openList(Listmode.ORDERED);
                    sb.append("<li>" + line.substring(1).trim() + "</li>\n");
                    break;
                case '*':
                    // line contains bullet list item
                    this.openList(Listmode.UNORDERED);
                    sb.append("<li>" + line.substring(1).trim() + "</li>\n");
                    break;
                default:
                    // no special character: just use line as is
                    this.closeList();
                    sb.append(line + " \n");
                }
            }
            this.closeList();
            br.close();
        } catch (IOException e)
        {
            e.printStackTrace();
        }

        return sb.toString();
    }

    private void openList(Listmode listMode)
    {
        if (this.listMode != listMode)
        {
            closeList();
            if (listMode == Listmode.ORDERED)
            {
                sb.append("<div class='list'><ol>\n");
            }
            else if (listMode == Listmode.UNORDERED)
            {
                sb.append("<div class='list'><ul>\n");
            }
            this.listMode = listMode;
        }
    }

    private void closeList()
    {
        if (this.listMode == Listmode.ORDERED)
        {
            sb.append("</ol></div>\n");
        }
        else if (this.listMode == Listmode.UNORDERED)
        {
            sb.append("</ul></div>\n");
        }
        this.listMode = Listmode.NONE;
    }

    private static final String TAG = "ViewHelp";

}
