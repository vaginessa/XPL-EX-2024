package eu.faircode.xlua.api.objects.xmock.phone;

import android.content.ContentValues;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import eu.faircode.xlua.api.objects.IJsonSerial;
import eu.faircode.xlua.api.objects.ISerial;
import eu.faircode.xlua.api.objects.xmock.ConfigSetting;
import eu.faircode.xlua.utilities.BundleUtil;
import eu.faircode.xlua.utilities.CursorUtil;

public class MockPhoneConfig extends MockPhoneConfigBase implements IJsonSerial, Parcelable {
    public static final String JSON = "phoneconfigs.json";

    public MockPhoneConfig() { }
    public MockPhoneConfig(Parcel p) { fromParcel(p); }
    public MockPhoneConfig(Bundle b) { fromBundle(b); }
    public MockPhoneConfig(String name, LinkedHashMap<String, String> settings) {
        setName(name);
        setSettings(settings);
    }

    @Override
    public ContentValues createContentValues() {
        ContentValues cv = new ContentValues();
        cv.put("name", name);
        cv.put("settings", MockSettingsConversions.createSettingsString(settings));
        return cv;
    }

    @Override
    public void fromCursor(Cursor cursor) {
        if(cursor != null) {
            this.name = CursorUtil.getString(cursor, "name");
            this.settings = MockSettingsConversions.readSettingsFromString(CursorUtil.getString(cursor, "settings"));
        }
    }

    @Override
    public String toJSON() throws JSONException { return toJSONObject().toString(2); }

    @Override
    public JSONObject toJSONObject() throws JSONException {
        JSONObject jRoot = new JSONObject();
        jRoot.put("name", name);
        MockSettingsConversions.writeSettingsToJSON(jRoot, settings);
        //jRoot.put("settings", MockConfigConversions.convertMapToJson(settings).toString());
        return jRoot;
    }

    @Override
    public void fromJSONObject(JSONObject obj) throws JSONException {
        this.name = obj.getString("name");
        //this.settings =  MockConfigConversions.convertJsonToMap(obj.getString("settings"));
        this.settings = MockSettingsConversions.readSettingsFromJSON(obj);
    }

    @Override
    public Bundle toBundle() {
        Bundle b = new Bundle();
        b.putString("name", name);
        MockConfigConversions.writeSettingsToBundle(b, settings);
        return b;
    }

    @Override
    public void fromBundle(Bundle bundle) {
        if(bundle != null) {
            this.name = BundleUtil.readString(bundle, "name");
            this.settings = MockConfigConversions.readSettingsFromBundle(bundle);
            Log.i(TAG, "Config from bundle, name=" + name + " settings size=" + settings.size());
        }
    }

    @Override
    public void fromParcel(Parcel in) {
        if(in != null) {
            this.name = in.readString();
            //this.settings = MockConfigConversions.convertJsonToMap(in.readString());
            this.settings = MockSettingsConversions.readSettingsFromString(in.readString());
        }
    }

    @Override
    public int describeContents() { return 0; }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(name);
        dest.writeString(MockSettingsConversions.createSettingsString(settings));
        //dest.writeString(MockConfigConversions.convertMapToJson(settings).toString());
    }

    public LinkedHashMap<String, String> orderSettings(boolean setInternal) {
        List<ConfigSetting> settings = new ArrayList<>(MockConfigConversions.hashMapToListSettings(getSettings()));
        Collections.sort(settings, new Comparator<ConfigSetting>() {
            @Override
            public int compare(ConfigSetting o1, ConfigSetting o2) {



                return o1.getName().compareToIgnoreCase(o2.getName());
            }
        });

        LinkedHashMap<String, String> newMap = MockConfigConversions.listToHashMapSettings(settings, false);
        if(setInternal)
            setSettings(newMap);

        return newMap;
    }

    public static final Parcelable.Creator<MockPhoneConfig> CREATOR = new Parcelable.Creator<MockPhoneConfig>() {
        @Override
        public MockPhoneConfig createFromParcel(Parcel source) {
            return new MockPhoneConfig(source);
        }

        @Override
        public MockPhoneConfig[] newArray(int size) {
            return new MockPhoneConfig[size];
        }
    };

    public static class Table {
        public static final String name = "mockconfigs";
        public static final LinkedHashMap<String, String> columns = new LinkedHashMap<String, String>() {{
            put("name", "TEXT");
            put("settings", "TEXT");
        }};
    }
}
