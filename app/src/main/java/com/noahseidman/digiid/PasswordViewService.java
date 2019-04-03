package com.noahseidman.digiid;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.net.Uri;
import android.os.*;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.*;
import android.view.accessibility.AccessibilityNodeInfo;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import com.google.common.net.InternetDomainName;
import com.noahseidman.digiid.adapter.MultiTypeDataBoundAdapter;
import com.noahseidman.digiid.databinding.PasswordViewBinding;
import com.noahseidman.digiid.listeners.SiteCallback;
import com.noahseidman.digiid.models.MainActivityDataModel;
import com.noahseidman.digiid.models.SiteViewModel;
import com.noahseidman.digiid.utils.DigiPassword;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;


/**
 * ChatHead Service
 */
public class PasswordViewService extends Service implements SiteCallback, View.OnClickListener, View.OnTouchListener {

    private PasswordViewBinding binding;
    private AccessibilityNodeInfo node;
    private MainActivityDataModel keyData = new MainActivityDataModel();
    private static int NOTIFICATION_ID = 13432;
    private SiteViewModel[] variations = new SiteViewModel[]{};
    public static boolean SHOWING = false;
    private WindowManager.LayoutParams params;
    private WindowManager windowManager;
    private int navBarHeight = 0;
    private int topBarsHeight = 0;
    Executor executor = Executors.newSingleThreadExecutor();
    Handler handler = new Handler(Looper.getMainLooper());

    public static void showWebsite(Context context, AccessibilityNodeInfo nodeInfo, String url) {
        if (Settings.canDrawOverlays(context) && isPasswordHelperEnabled(context)) {
            Intent intent = new Intent(context, PasswordViewService.class);
            intent.putExtra("show", true);
            intent.putExtra("node", nodeInfo);
            intent.putExtra("url", url);
            context.startService(intent);
        }
    }

    public static void showApp(Context context, AccessibilityNodeInfo nodeInfo, String packageName) {
        if (Settings.canDrawOverlays(context) && isPasswordHelperEnabled(context)) {
            Intent intent = new Intent(context, PasswordViewService.class);
            intent.putExtra("show", true);
            intent.putExtra("node", nodeInfo);
            intent.putExtra("packageName", packageName);
            context.startService(intent);
        }
    }

    public static void biometricAuth(Context context) {
        if (Settings.canDrawOverlays(context) && isPasswordHelperEnabled(context)) {
            Intent intent = new Intent(context, PasswordViewService.class);
            intent.putExtra("show", true);
            intent.putExtra("auth", true);
            context.startService(intent);
        }
    }

    public static void hide(Context context) {
        if (Settings.canDrawOverlays(context) && isPasswordHelperEnabled(context)) {
            Intent intent = new Intent(context, PasswordViewService.class);
            intent.putExtra("show", false);
            context.startService(intent);
        }
    }

    public static void showOverlaySettings(MainActivity context) {
        Intent myIntent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
        myIntent.setData(Uri.EMPTY.parse("package:" + context.getPackageName()));
        context.startActivityForResult(myIntent, context.getOVERLAY_SETTING());
    }

    private SiteViewModel[] siteVariations(String url) {
        Uri uri = Uri.parse(url);
        final InternetDomainName name = InternetDomainName.from(uri.getHost()).topPrivateDomain();
        ArrayList<SiteViewModel> variations = new ArrayList();
        String[] splits = name.toString().split("\\.");
        if (splits.length > 0) {
            variations.add(new SiteViewModel(splits[0]));
        }
        variations.add(new SiteViewModel(name.toString()));
        return variations.toArray(new SiteViewModel[0]);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        keyData.populate(this, null);
        binding = PasswordViewBinding.inflate(LayoutInflater.from(this));
        binding.toolbar.setTitleTextColor(getResources().getColor(R.color.black));
        binding.toolbar.setTitle(R.string.name);
        binding.toolbar.setOnTouchListener(this);
        binding.close.setOnClickListener(this);
        binding.recycler.setLayoutManager(new LinearLayoutManager(this));
        int OVERLAY_TYPE;
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.N_MR1) {
            OVERLAY_TYPE = WindowManager.LayoutParams.TYPE_PHONE;
        } else {
            OVERLAY_TYPE = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        }
        params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                OVERLAY_TYPE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.START;
        params.x = 0;
        params.y = 0;
        windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        windowManager.addView(binding.getRoot(), params);
        binding.getRoot().setVisibility(View.GONE);
        SHOWING = false;
        windowManager.getDefaultDisplay().getMetrics(displayMetrics);
        startForeground(NOTIFICATION_ID, createNotification(this));
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        windowManager.removeView(binding.getRoot());
        stopForeground(true);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (BiometricActivity.Companion.getCanceled()) {
            node.performAction(AccessibilityNodeInfo.ACTION_NEXT_HTML_ELEMENT);
            node.performAction(AccessibilityNodeInfo.ACTION_CLEAR_FOCUS);
            handler.postDelayed(() -> BiometricActivity.Companion.setCanceled(false), 500);
            return Service.START_STICKY;
        }
        if (intent != null && intent.getExtras() != null && intent.getExtras().containsKey("node")) {
            node = intent.getParcelableExtra("node");
        }
        if (intent != null && intent.getExtras() != null && intent.getExtras().containsKey("packageName") && !TextUtils.isEmpty(intent.getStringExtra("packageName"))) {
            if (SHOWING) {
                return Service.START_STICKY;
            }
            binding.close.setVisibility(View.VISIBLE);
            getUrl(intent.getStringExtra("packageName"), url -> {
                processSiteVariations(url);
                promptBiometric(url);

            });
        } else if (intent != null && intent.getExtras() != null && intent.getExtras().containsKey("url") && !TextUtils.isEmpty(intent.getStringExtra("url"))) {
            if (SHOWING) {
                return Service.START_STICKY;
            }
            binding.close.setVisibility(View.GONE);
            processSiteVariations(intent.getStringExtra("url"));
            promptBiometric(intent.getStringExtra("url"));
        } else {
            processShow(intent);
        }
        return Service.START_STICKY;
    }

    private void promptBiometric(String url) {
        BiometricActivity.Companion.show(this, url);
    }

    private void processShow(Intent intent) {
        if (intent != null && intent.getExtras() != null && intent.getExtras().containsKey("show")) {
            if (!intent.getBooleanExtra("show", false)) {
                binding.getRoot().setVisibility(View.GONE);
                SHOWING = false;
            } else {
                binding.getRoot().setVisibility(View.VISIBLE);
                SHOWING = true;
            }
        }
    }

    private void processSiteVariations(String url) {
        SiteViewModel[] variations = siteVariations(url);
        if (!Arrays.equals(variations, this.variations)) {
            this.variations = variations;
            binding.recycler.setAdapter(new MultiTypeDataBoundAdapter(this, variations));
            Rect rect = new Rect();
            node.getBoundsInScreen(rect);
            params.x = rect.left;
            params.y = rect.top;
            windowManager.updateViewLayout(binding.getRoot(), params);
        }
    }

    public static boolean isPasswordHelperEnabled(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean("bubble", false);
    }

    public static void enablePasswordHelper(Context context, boolean enable) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putBoolean("bubble", enable);
        editor.apply();
    }

    @Override
    public void onClick(String urlVariation) {
        String password = new DigiPassword().getPassword(this, keyData.getSeed(), urlVariation, 0);
        Bundle arguments = new Bundle();
        arguments.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, password);
        node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, arguments);
        binding.getRoot().setVisibility(View.GONE);
        SHOWING = false;
    }

    @Override
    public void onClick(View v) {
        switch(v.getId()) {
            case R.id.close: {
                binding.getRoot().setVisibility(View.GONE);
                node.performAction(AccessibilityNodeInfo.ACTION_CLEAR_FOCUS);
                node.performAction(AccessibilityNodeInfo.ACTION_NEXT_HTML_ELEMENT);
                SHOWING = false;
            }
        }
    }

    private static Notification createNotification(Context context) {
        initChannels(context);
        final NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "digiid");
        builder.setWhen(System.currentTimeMillis());
        builder.setSmallIcon(R.drawable.ic_digiid);
        builder.setBadgeIconType(NotificationCompat.BADGE_ICON_SMALL);
        builder.setContentTitle(context.getString(R.string.name));
        builder.setContentText(context.getString(R.string.DigiPasswordWidget));
        builder.setOngoing(true);
        builder.setPriority(NotificationCompat.PRIORITY_MIN);
        builder.setCategory(NotificationCompat.CATEGORY_SERVICE);
        return builder.build();
    }

    public static void initChannels(Context context) {
        if (Build.VERSION.SDK_INT < 26) {
            return;
        }
        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationChannel channel = new NotificationChannel("digiid", "DigiID", NotificationManager.IMPORTANCE_LOW);
        channel.setDescription("DigiID Floating Password");
        channel.setSound(null, null);
        notificationManager.createNotificationChannel(channel);
    }

    private int initialX;
    private int initialY;
    private float initialTouchX;
    private float initialTouchY;
    private DisplayMetrics displayMetrics = new DisplayMetrics();

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                initialX = params.x;
                initialY = params.y;
                initialTouchX = event.getRawX();
                initialTouchY = event.getRawY();
                return true;
            case MotionEvent.ACTION_UP:
                return true;
            case MotionEvent.ACTION_OUTSIDE:
            case MotionEvent.ACTION_MOVE:
                params.y = initialY + (int) (event.getRawY() - initialTouchY);
                params.x = initialX + (int) (event.getRawX() - initialTouchX);

                if (params.x + binding.getRoot().getWidth() >= displayMetrics.widthPixels) {
                    params.x = displayMetrics.widthPixels - binding.getRoot().getWidth();
                }
                if (params.x < 0) {
                    params.x = 0;
                }
                if (params.y + binding.getRoot().getHeight() >= displayMetrics.heightPixels - getTopSystemWindowsHeight()) {
                    params.y = displayMetrics.heightPixels - getTopSystemWindowsHeight() - binding.getRoot().getHeight();
                }
                if (params.y < getNavBarHeight()) {
                    params.y = getNavBarHeight();
                }
                windowManager.updateViewLayout(binding.getRoot(), params);
                return true;
        }
        return false;
    }

    private int getTopSystemWindowsHeight() {
        if (topBarsHeight == 0) {
            topBarsHeight = getStatusBarHeight() + getToolBarHeight();
        }
        return topBarsHeight;
    }

    public int getStatusBarHeight() {
        int result = 0;
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }

    public int getToolBarHeight() {
        int[] attrs = new int[] {R.attr.actionBarSize};
        TypedArray ta = obtainStyledAttributes(attrs);
        int toolBarHeight = ta.getDimensionPixelSize(0, -1);
        ta.recycle();
        return toolBarHeight;
    }

    public int getNavBarHeight() {
        if (navBarHeight == 0) {
            int resourceId = getResources().getIdentifier("navigation_bar_height", "dimen", "android");
            if (resourceId > 0) {
                navBarHeight = getResources().getDimensionPixelSize(resourceId);
            }
        }
        return navBarHeight;
    }

    private void getUrl(String packageName, GetAppUrl getAppUrl) {
        executor.execute(() -> {
            String url = getAppUrl(packageName);
            if (TextUtils.isEmpty(url)) {
                try {
                    Document document = Jsoup.connect("https://play.google.com/store/apps/details?id=" + packageName).get();
                    Elements links = document.select("a");
                    for (Element link : links) {
                        String attribute = link.text();
                        if(attribute.equalsIgnoreCase("Visit website")){
                            link.attr("href");
                            String text = link.attr("href");
                            saveAppUrl(packageName, text);
                            handler.post(() -> getAppUrl.getUrl(text));
                            return;
                        }
                    }
                } catch(Exception e) {
                    e.printStackTrace();
                }

            } else {
                handler.post(() -> getAppUrl.getUrl(url));
            }
        });
    }

    private void saveAppUrl(String packageName, String url) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(packageName, url);
        editor.apply();
    }

    private String getAppUrl(String packageName) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        return prefs.getString(packageName, "");
    }

    interface GetAppUrl {
        void getUrl(String url);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}