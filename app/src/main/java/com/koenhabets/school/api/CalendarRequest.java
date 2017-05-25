package com.koenhabets.school.api;

import android.app.NotificationManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.toolbox.HttpHeaderParser;
import com.google.firebase.crash.FirebaseCrash;
import com.koenhabets.school.R;
import com.koenhabets.school.SchoolApp;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class CalendarRequest extends Request<String> {

    private static String url = "https://api.scholica.com/2.0/communities/1/calendar/schedule";

    private Response.Listener<String> responListener;
    private String requestToken;
    private String timeStamp;

    public CalendarRequest(String requestToken,
                           String timeStamp,
                           Response.Listener<String> responseListener,
                           Response.ErrorListener errorListener) {

        super(Method.POST, url, errorListener);

        this.requestToken = requestToken;
        this.timeStamp = timeStamp;
        this.responListener = responseListener;
    }

    public static String parseResponse(String response, String timeStamp) throws JSONException {
        Log.i("Result", response);
        JSONObject jsonObject = new JSONObject(response);
        JSONObject jsonMain = jsonObject.getJSONObject("result");
        SharedPreferences sharedPref = SchoolApp.getContext().getSharedPreferences("com.koenhabets.school", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPref.edit();
        editor.putString(timeStamp, jsonObject.toString());
        editor.apply();
        JSONArray jsonArray = jsonMain.getJSONArray("items");

        NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(SchoolApp.getContext());
        mBuilder.setSmallIcon(R.drawable.ic_time_table_black_24dp);
        mBuilder.setContentTitle("Rooster");
        mBuilder.setOngoing(true);
        NotificationCompat.InboxStyle inboxStyle = new NotificationCompat.InboxStyle();
        String resultString = "tada";

        for (int i = 0; i < jsonArray.length(); i++) {
            int uur = i + 1;
            String title = uur + ". Tussenuur";
            String lokaal = "";
            JSONObject vak = jsonArray.getJSONObject(i);
            if(!vak.has("custom_str")) {
                if (vak.has("title") && vak.has("subtitle")) {
                    title = vak.getString("title");
                    lokaal = vak.getString("subtitle");
                } else if (vak.has("type")) {
                }
                if (Objects.equals(title.substring(1), ". Culturele en kunstzinnige vorming")) {
                    title = title.charAt(0) + ". CKV";
                }
                Calendar calendar = Calendar.getInstance();
                int hour = calendar.get(Calendar.HOUR_OF_DAY);
                if (hour >= 12 && hour <= 16 && i == 0) {
                } else if (hour >= 12 && hour <= 16 && i == 1) {
                } else if (hour >= 13 && hour <= 16 && i == 2) {
                } else {
                    inboxStyle.addLine(title + " " + lokaal);
                    resultString += title + " " + lokaal + "\n";
                }
            }
        }
        NotificationManager mNotificationManager = (NotificationManager) SchoolApp.getContext().getSystemService(Context.NOTIFICATION_SERVICE);
        mBuilder.setStyle(inboxStyle);

        boolean notificatiecalendar = sharedPref.getBoolean("notificatie-calendar", true);
        String weekDay;
        SimpleDateFormat dayFormat = new SimpleDateFormat("EEEE", Locale.US);

        Calendar calendar = Calendar.getInstance();
        weekDay = dayFormat.format(calendar.getTime());
        if (notificatiecalendar && !Objects.equals(weekDay, "Saturday") && !Objects.equals(weekDay, "Sunday")) {
            mNotificationManager.notify(1, mBuilder.build());
        } else {
            mNotificationManager.cancel(1);
        }
        editor.putString("calnow", resultString);
        editor.apply();
        return response;
    }

    @Override
    protected Map<String, String> getParams() {
        Map<String, String> params = new HashMap<>();
        params.put("token", requestToken);
        params.put("time", timeStamp);
        params.put("show_tasks", "1");
        return params;
    }

    @Override
    protected Response<String> parseNetworkResponse(NetworkResponse response) {
        String data;
        try {
            data = new String(response.data, HttpHeaderParser.parseCharset(response.headers));
        } catch (UnsupportedEncodingException e) {
            data = new String(response.data);
        }
        try {
            String resultString = parseResponse(data, timeStamp);
            return Response.success(resultString, null);
        } catch (JSONException e) {
            e.printStackTrace();
            return Response.error(new ParseError(e));
        }
    }

    @Override
    protected void deliverResponse(String response) {
        try {
            responListener.onResponse(response);
        } catch (IllegalStateException e){
            e.printStackTrace();
        }
    }
}
