package com.example.jordan.sunshine.app;

import android.app.Application;

import com.facebook.stetho.Stetho;

/**
 * Created by jordan on 3/13/15.
 */
public class SunshineApplication extends Application {
    public void onCreate() {
        super.onCreate();
        Stetho.initialize(
                Stetho.newInitializerBuilder(this)
                        .enableDumpapp(
                                Stetho.defaultDumperPluginsProvider(this))
                        .enableWebKitInspector(
                                Stetho.defaultInspectorModulesProvider(this))
                        .build());
    }
}
