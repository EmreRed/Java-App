import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class App {

    String url = "https://example.com/";
    String loginActivity = "LoginActivity";
    private static String APP_OS = "android";
    private static String APP_USER_AGENT = "App Browser";
    private static int APP_VERSION = 1;
    private static String APP_DEVICE = "";
    private static String APP_APP = "";
    private static String APP_LANGUAGE = "";
    private Context APP_CONTEXT;
    String auth;
    private AppListener listener;
    AlertDialog.Builder builder;
    Context context;

    public App(Context context, String app) {
        APP_CONTEXT = context;
        APP_APP = app;
        APP_LANGUAGE = Locale.getDefault().getLanguage();
        APP_VERSION = BuildConfig.VERSION_CODE;
        APP_DEVICE = Settings.Secure.getString(APP_CONTEXT.getContentResolver(),
                Settings.Secure.ANDROID_ID);
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        this.auth = preferences.getString("auth", "");
        if(!((Activity) APP_CONTEXT).getLocalClassName().equals(this.loginActivity)) {
            this.check();
        }
    }

    public AppListener getListener() {
        return listener;
    }

    public void setListener(AppListener listener) {
        this.listener = listener;
    }

    void check(){
        if (this.auth != null && !this.auth.equals("")) {
            JSONObject json = new JSONObject();
            this.call(json,APP_APP,"check");
        } else {
            System.out.println(this.auth);
            Intent myIntent = new Intent(APP_CONTEXT, LoginActivity.class);
            APP_CONTEXT.startActivity(myIntent);
        }
    }

    public JSONObject login(String username, String password){
        if(username.equals("") || password.equals("")){
            listener.onFalse("login","Kullanıcı adı ve şifre girmelisiniz");
            return null;
        }
        JSONObject user = new JSONObject();
        try {
            user.put("username", username);
            user.put("password", password);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return this.call(user,"auth","login");
    }

    public JSONObject register(String name, String gsm){
        if(name.equals("") || gsm.equals("")){
            listener.onFalse("register","İsminizi ve GSM numaranızı girmelisiniz.");
            return null;
        }
        JSONObject user = new JSONObject();
        try {
            user.put("name", name);
            user.put("gsm", gsm);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return this.call(user,"auth","register");
    }

    JSONObject action(String act, JSONObject json){
        return this.call(json,APP_APP,act);
    }

    private JSONObject call(final JSONObject json, final String link, String action){
        JSONObject in_req = new JSONObject();
        final JSONObject request = new JSONObject();
        try {
            in_req.put("action", action);
            in_req.put("auth", this.auth);
            request.put("request", in_req);
            request.put("data", json);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        builder = new AlertDialog.Builder(APP_CONTEXT);
        builder.setCancelable(false);
        builder.setTitle("App");
        context = APP_CONTEXT;
        final Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    URL url = new URL(this.url+link);
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/json;charset=UTF-8");
                    conn.setRequestProperty("Accept","application/json");
                    conn.setRequestProperty("X-App-Os",APP_OS);
                    conn.setRequestProperty("X-App-App",APP_APP);
                    conn.setRequestProperty("X-App-Version",""+APP_VERSION);
                    conn.setRequestProperty("X-App-Device",APP_DEVICE);
                    conn.setRequestProperty("X-App-Language",APP_LANGUAGE);
                    conn.setRequestProperty("User-Agent",APP_USER_AGENT);
                    Log.e("dil",APP_LANGUAGE);
                    conn.setDoOutput(true);
                    conn.setDoInput(true);
                    DataOutputStream os = new DataOutputStream(conn.getOutputStream());
                    //os.writeBytes(URLEncoder.encode(jsonParam.toString(), "UTF-8"));
                    os.writeBytes(request.toString());

                    os.flush();
                    os.close();
                    String res = "";
                    Log.i("-->", request.toString());
                    if(conn.getResponseCode()==200){
                        InputStream in = new BufferedInputStream(conn.getInputStream());
                        if (in != null) {
                            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(in));
                            String line = "";

                            while ((line = bufferedReader.readLine()) != null)
                                res += line;
                        }
                        Log.i("-->",res);
                        in.close();
                    }
                    try {
                        JSONObject json = new JSONObject(res);
                        JSONObject result = json.getJSONObject("result");
                        if(result.getBoolean("success")){
                            JSONObject data = json.getJSONObject("data");
                            if(result.getString("action").equals("login") && data.getBoolean("success")){
                                String auth = data.getJSONObject("data").getString("auth");
                                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(APP_CONTEXT);
                                SharedPreferences.Editor editor = preferences.edit();
                                editor.putString("auth",auth);
                                editor.commit();
                            }
                            if(!result.getString("action").equals("check")) {
                                if (data.getBoolean("success")) {
                                    listener.onTrue(result.getString("action"),data.getJSONObject("data"));
                                } else {
                                    listener.onFalse(result.getString("action"),data.getString("message"));
                                }
                            }
                        }else{
                            if(result.getString("do")!=null){
                                final Activity activity = (Activity) context;
                                switch (result.getString("do")){
                                    case "exit":
                                        builder.setPositiveButton(context.getString(R.string.understood), new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialogInterface, int i) {
                                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                                                    activity.finishAffinity();
                                                } else{
                                                    activity.finish();
                                                    System.exit( 0 );
                                                }
                                            }
                                        });
                                        break;
                                    case "logout":
                                        builder.setPositiveButton(context.getString(R.string.understood), new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialogInterface, int i) {
                                                Intent myIntent = new Intent(APP_CONTEXT, LoginActivity.class);
                                                APP_CONTEXT.startActivity(myIntent);
                                            }
                                        });
                                        break;
                                    case "update":
                                        builder.setPositiveButton(context.getString(R.string.understood), new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialogInterface, int i) {
                                                try {
                                                    Intent intent = new Intent(Intent.ACTION_VIEW,Uri.parse("market://details?id=" + context.getApplicationContext().getPackageName()));
                                                    APP_CONTEXT.startActivity(intent);
                                                } catch (android.content.ActivityNotFoundException a) {
                                                    Intent intent = new Intent(Intent.ACTION_VIEW,Uri.parse("https://play.google.com/store/apps/details?id=" + context.getApplicationContext().getPackageName()));
                                                    APP_CONTEXT.startActivity(intent);
                                                }
                                            }
                                        });
                                        break;
                                    default:
                                        break;
                                }
                                builder.setMessage(result.getString("message"));
                                activity.runOnUiThread(new Runnable() {
                                    public void run() {
                                        builder.show();
                                    }
                                });
                            }
                        }
                    }   catch(Exception e){
                        e.printStackTrace();
                    }
                    conn.disconnect();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        thread.start();
        return null;
    }
}