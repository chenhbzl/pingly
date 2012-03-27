package net.nologin.meep.pingly.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;
import net.nologin.meep.pingly.PinglyConstants;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class PinglyUtils {

	public static boolean activeNetConnectionPresent(Context ctx) {
		
	    ConnectivityManager cm = (ConnectivityManager)ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
	    NetworkInfo ni = cm.getActiveNetworkInfo();
	    return ni != null && ni.isConnected();
	    
	}
	
	public static String loadStringForName(Context context, String name){

        int resId = context.getResources().getIdentifier(name, "string", context.getPackageName());
        // avoiding "android.content.res.Resources$NotFoundException: String resource ID #0x0"
        // perhaps that's what _should_ happen..  Best not to kill the app cause of laziness I guess.
        if(resId <= 0){
            Log.e("Pingly","No string resource defined for key '" + name +"'");
            return "n/a";
        }
        return context.getString(resId);
    }

    // TODO: doc
    public static String loadStringForPlural(Context context, String name, int count){

        int resId = context.getResources().getIdentifier(name, "plurals", context.getPackageName());
        // avoiding "android.content.res.Resources$NotFoundException: String resource ID #0x0"
        // perhaps that's what _should_ happen..  Best not to kill the app cause of laziness I guess.
        if(resId <= 0){
            Log.e("Pingly","No plural resource defined for key '" + name +"'");
            return "n/a";
        }
        return context.getResources().getQuantityString(resId,count,count);
    }

    public static String[] enumToStringValuesArray(Context ctx, Class<?> c, String valueMethod){

        List<String> result = new ArrayList<String>();
        try {
            Method mVal = c.getMethod(valueMethod);

            for(Object enumElem : c.getEnumConstants()){
                String resVal = (String)mVal.invoke(enumElem);
                result.add(loadStringForName(ctx,resVal));
            }
        }
        catch(Exception e){
            Log.e(PinglyConstants.LOG_TAG,"Error parsing " + c,e);
        }

        return result.toArray(new String[result.size()]);
    }




}
