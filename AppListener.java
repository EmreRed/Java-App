import org.json.JSONException;
import org.json.JSONObject;

public interface AppListener {
    void onTrue(String action, JSONObject data) throws JSONException;
    void onFalse(String action, String message);
}