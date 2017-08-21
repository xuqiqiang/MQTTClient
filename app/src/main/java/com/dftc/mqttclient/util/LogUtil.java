/**
 * Alibaba.com Inc.
 * Copyright (c) 2004-2016 All Rights Reserved.
 */
package com.dftc.mqttclient.util;

import android.os.Handler;
import android.os.Looper;
import android.widget.ScrollView;
import android.widget.TextView;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @version $Id: LogUtil.java,v 0.1 2016年6月2日 下午4:39:18  Exp $
 */
public class LogUtil {

    /**
     * 是否打印日志
     **/
    public static boolean showLog = true;

    static SimpleDateFormat fm = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss.SSS");

    private static StringBuilder mLogText;

    private static TextView mTextView;
    private static ScrollView mScrollView;

    private static Handler mHandler;

    public static void init(ScrollView sv) {
        mLogText = new StringBuilder();
        mScrollView = sv;
        mTextView = (TextView) sv.getChildAt(0);
        mHandler = new Handler(Looper.getMainLooper());
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mTextView.setText(mLogText.toString());
            }
        });
    }

    public static void destroy() {
        mLogText = null;
        mTextView = null;
        mHandler = null;
    }

    public static void clear() {
        mLogText = new StringBuilder();
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                mTextView.setText(mLogText.toString());
            }
        });
    }

    /**
     * 简单日志打印
     *
     * @param msg
     */
    public static void print(String msg) {
        if (showLog) {

            String source = "";

            try {
                StackTraceElement st = Thread.currentThread().getStackTrace()[3];
                source = "(" + st.getFileName() + ":" + st.getLineNumber()
                        + ") ";
            } catch (Exception e) {
            }

            String text = fm.format(new Date()) + " - " + source + msg;

            mLogText.append("\n").append(text);
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    mTextView.setText(mLogText.toString());
                    mScrollView.fullScroll(ScrollView.FOCUS_DOWN);
                }
            });
            System.out.println(text);
        }
    }

}
