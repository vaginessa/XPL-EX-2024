package eu.faircode.xlua;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.os.Binder;
import android.os.Parcel;
import android.util.Log;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import eu.faircode.xlua.api.objects.xlua.hook.xHook;
import eu.faircode.xlua.database.DatabaseQuerySnake;
import eu.faircode.xlua.api.xlua.XHookProvider;
import eu.faircode.xlua.database.MockUpdater;
import eu.faircode.xlua.database.XLuaUpdater;
import eu.faircode.xlua.utilities.DatabasePathUtil;

public class XGlobalCore {
    private static final String TAG = "XLua.XGlobalCore";
    private static final Object hookLock = new Object();
    private static final Object mockLock = new Object();

    private static HashMap<String, xHook> hooks = new HashMap<>();
    private static HashMap<String, xHook> builtIn = new HashMap<>();

    //public static int version = -1;

    private static final String DB_NAME_LUA = "xlua";
    private static final String DB_NAME_MOCK = "mock";

    private static XDataBase xLua_db = null;
    private static XDataBase xMock_db = null;

    private static boolean mock_init = false;

    final static String cChannelName = "xlua";

    public static XDataBase getLuaDatabase(Context context) {
        checkDatabases(context);
        synchronized (hookLock) {
            return xLua_db;
        }
    }

    public static XDataBase getMockDatabase(Context context) {
        checkDatabases(context);
        synchronized (mockLock) {
            return xMock_db;
        }
    }

    public static void checkDatabases(Context context) {
        try {
            synchronized (mockLock) {
                if(xMock_db == null) {
                    xMock_db = new XDataBase(DB_NAME_MOCK, context, true);
                    MockUpdater.reset();
                    mock_init = false;
                }else if(DebugUtil.isDebug())
                    DatabasePathUtil.log("XMock Database is db=" + xMock_db, false);
                if(!mock_init)
                    mock_init = MockUpdater.initDatabase(context, xMock_db);
            }

            synchronized (hookLock) {
                if(xLua_db == null) {
                    xLua_db = new XDataBase(DB_NAME_LUA, context, true);
                    XLuaUpdater.checkForUpdate(xLua_db);
                }else if(DebugUtil.isDebug())
                    DatabasePathUtil.log("XLua Database is db=" + xMock_db, false);

                if (hooks == null || hooks.isEmpty()) {
                    if(!xLua_db.isOpen(true))
                        return;

                    loadHooks(context);
                }else if(DebugUtil.isDebug())
                    DatabasePathUtil.log("XLua Hook Cache size=" + hooks.size(), false);
            }
        }catch (Throwable e) {
            DatabasePathUtil.log("Failed to check Databases\n" + e + "\n" + Log.getStackTraceString(e), true);
        }
    }

    private static void loadHooks(Context context) throws Throwable {
        DatabasePathUtil.log("<loadHooks> loading hooks, null cache", false);
        hooks = new HashMap<>();
        builtIn = new HashMap<>();

        // Read built-in definition
        PackageManager pm = context.getPackageManager();
        ApplicationInfo ai = pm.getApplicationInfo(BuildConfig.APPLICATION_ID, 0);
        for (xHook builtin : xHook.readHooks(context, ai.publicSourceDir)) {
            builtin.resolveClassName(context);
            builtIn.put(builtin.getId(), builtin);
            hooks.put(builtin.getId(), builtin);
        }


        DatabasePathUtil.log("loaded hook size=" + hooks.size(), false);
        DatabaseQuerySnake snake = DatabaseQuerySnake.create(xLua_db, xHook.Table.name);
        Cursor c = null;
        try {
            xLua_db.readLock();
            c = snake.query();

            int colDefinition = c.getColumnIndex("definition");
            while (c.moveToNext()) {
                String definition = c.getString(colDefinition);
                xHook hook = new xHook();
                hook.fromJSONObject(new JSONObject(definition));
                hook.resolveClassName(context);
                //Log.i(TAG, " loading hook=" + hook.getId());
                hooks.put(hook.getId(), hook);
            }
        }catch (Exception e) {
            DatabasePathUtil.log("Failed to init hooks, e=" + e + "\n" + Log.getStackTraceString(e), true);
        }finally {
            xLua_db.readUnlock();
            snake.clean(c);
        }

        DatabasePathUtil.log("Loaded hook definitions hooks=" + hooks.size() + " builtIns=" + builtIn.size(), false);
    }

    public static Collection<xHook> getHooks(XDataBase db, boolean all) {
        Log.i(TAG, "Getting Hooks all=" + all + " internal size list =" + hooks.size());
        List<String> collection = XHookProvider.getCollections(db, XUtil.getUserId(Binder.getCallingUid()));
        Log.i(TAG, "collection size=" + collection.size());
        List<xHook> hv = new ArrayList();
        synchronized (hookLock) {
            for (xHook hook : hooks.values())
                if (all || hook.isAvailable(null, collection))
                    hv.add(hook);
        }

        Collections.sort(hv, new Comparator<xHook>() {
            @Override
            public int compare(xHook h1, xHook h2) {
                return h1.getId().compareTo(h2.getId());
            }
        });

        Log.i(TAG, "Getting Hooks returning size=" + hv.size());

        return hv;
    }

    public static List<String> getGroups(XDataBase db) {
        List<String> groups = new ArrayList<>();
        List<String> collections = XHookProvider.getCollections(db, XUtil.getUserId(Binder.getCallingUid()));

        if(DebugUtil.isDebug()) {
            Log.i(TAG, "Returned Collections size=" + collections.size());
            String s = "testing collections=";
            for(String c : collections)
                s += " " + c;
            Log.i(TAG, s);
        }

        synchronized (hookLock) {
            for (xHook hook : hooks.values())
                if (hook.isAvailable(null, collections) && !groups.contains(hook.getGroup()))
                    groups.add(hook.getGroup());
        }

        Log.i(TAG, "Collection size=" + collections.size() + "  groups size=" + groups.size());

        return groups;
    }

    @SuppressLint("WrongConstant")
    public static void writeHookFromCache(
            MatrixCursor writeTo,
            String hookId,
            String used,
            String packageName,
            List<String> collections,
            boolean marshall) throws Throwable {
        synchronized (hookLock) {
            if (hooks.containsKey(hookId))  {
                xHook hook = hooks.get(hookId);
                if(hook == null)
                    return;

                if (hook.isAvailable(packageName, collections)) {
                    if (marshall) {
                        Parcel parcel = Parcel.obtain();
                        hook.writeToParcel(parcel, xHook.FLAG_WITH_LUA);
                        writeTo.newRow()
                                .add(parcel.marshall())
                                .add(used);
                        parcel.recycle();
                    } else //to not marshall use JSON
                        writeTo.newRow()
                                .add(hook.toJSON())
                                .add(used);
                }
            }else if(BuildConfig.DEBUG) {
                Log.w(TAG, "Hook " + hookId + " not found");
            }
        }
    }

    public static List<String> getHookIds(String pkg, List<String> collections) {
        List<String> hook_ids = new ArrayList<>();
        synchronized (hookLock) {
            for (xHook hook : hooks.values())
                if (hook.isAvailable(pkg, collections))
                    hook_ids.add(hook.getId());
        }

        return hook_ids;
    }

    public static xHook getHook(String hookId) {
        synchronized (hookLock) { if (hooks.containsKey(hookId)) return hooks.get(hookId); return null; }
    }

    public static xHook getHook(String hookId, String pkg, List<String> collections) {
        synchronized (hookLock) {
            if (hooks.containsKey(hookId)) {
                xHook hook = hooks.get(hookId);
                if (hook != null && hook.isAvailable(pkg, collections))
                    return hook;
            }

            return null;
        }
    }

    public static boolean updateHookCache(Context context, xHook hook, String extraId) {
        synchronized (hookLock) {
            if (hook == null) {
                if (hooks.containsKey(extraId) && hooks.get(extraId).isBuiltin()) {
                    Log.e(TAG, "Hook id Is built in, id=" + extraId + " , Not allowed to modify built in hooks!");
                    return false;
                }

                Log.i(TAG, "Deleting hook id=" + extraId);
                hooks.remove(extraId);
                if (builtIn.containsKey(extraId)) {
                    Log.i(TAG, "Restoring builtin id=" + extraId);
                    xHook builtin = builtIn.get(extraId);
                    // class name is already resolved
                    hooks.put(extraId, builtin);
                } else
                    Log.w(TAG, "Builtin not found id=" + extraId);
            } else {
                if (!hook.isBuiltin())
                    Log.i(TAG, "Storing hook id=" + extraId);
                hook.resolveClassName(context);
                hooks.put(extraId, hook);
            }

            return true;
        }
    }
}
