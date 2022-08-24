package com.mintegral.mediation;

import com.mbridge.msdk.foundation.same.net.Aa;

import java.lang.reflect.Method;


public class AdapterTools {
    private static String TAG = "AdapterTools";


    public static void addChannel() {
        try {
            Aa a = new Aa();
            Class c = a.getClass();
            Method method = c.getDeclaredMethod("b", String.class);
            method.setAccessible(true);
            method.invoke(a, "Y+H6DFttYrPQYcIBicKwJQKQYrN=");
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
}