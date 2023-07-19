package com.example.leserlight;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.PopupWindow;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.hjq.permissions.IPermissionInterceptor;
import com.hjq.permissions.OnPermissionCallback;
import com.hjq.permissions.OnPermissionPageCallback;
import com.hjq.permissions.Permission;
import com.hjq.permissions.PermissionFragment;
import com.hjq.permissions.XXPermissions;

import java.util.ArrayList;
import java.util.List;

public class PermissionAllow implements IPermissionInterceptor {

    public static final Handler HANDLER = new Handler(Looper.getMainLooper());
    private boolean mRequestFlag;
    private PopupWindow mPermissionPopup;

    @Override
    public void launchPermissionRequest(@NonNull Activity activity, @NonNull List<String> allPermissions, @Nullable OnPermissionCallback callback) {
        mRequestFlag = true;
        List<String> deniedPermissions = XXPermissions.getDenied(activity, allPermissions);
        String message = activity.getString(R.string.common_permission_message, PermissionChange.getPermissionString(activity, deniedPermissions));
        ViewGroup decorView = (ViewGroup) activity.getWindow().getDecorView();
        int activityOrientation = activity.getResources().getConfiguration().orientation;
        boolean showPopupWindow = activityOrientation == Configuration.ORIENTATION_PORTRAIT;
        for (String permission : allPermissions) {
            if (!XXPermissions.isSpecial(permission)) {
                continue;
            }
            if (XXPermissions.isGranted(activity, permission)) {
                continue;
            }
            showPopupWindow = false;
            break;
        }
        if (showPopupWindow) {

            PermissionFragment.launch(activity, new ArrayList<>(allPermissions), this, callback);
            HANDLER.postDelayed(() -> {
                if (!mRequestFlag) {
                    return;
                }
                if (activity.isFinishing() ||
                        (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && activity.isDestroyed())) {
                    return;
                }
                showPopupWindow(activity, decorView, message);
            }, 300);
        } else {
            new AlertDialog.Builder(activity)
                    .setTitle(R.string.common_permission_description)
                    .setMessage(message)
                    .setCancelable(false)
                    .setPositiveButton(R.string.common_permission_granted, (dialog, which) -> {
                        dialog.dismiss();
                        PermissionFragment.launch(activity, new ArrayList<>(allPermissions),
                                PermissionAllow.this, callback);
                    })
                    .setNegativeButton(R.string.common_permission_denied, (dialog, which) -> {
                        dialog.dismiss();
                        if (callback == null) {
                            return;
                        }
                        callback.onDenied(deniedPermissions, false);
                    })
                    .show();
        }
    }

    @Override
    public void grantedPermissionRequest(@NonNull Activity activity, @NonNull List<String> allPermissions,
                                         @NonNull List<String> grantedPermissions, boolean allGranted,
                                         @Nullable OnPermissionCallback callback) {
        if (callback == null) {
            return;
        }
        callback.onGranted(grantedPermissions, allGranted);
    }

    @Override
    public void deniedPermissionRequest(@NonNull Activity activity, @NonNull List<String> allPermissions,
                                        @NonNull List<String> deniedPermissions, boolean doNotAskAgain,
                                        @Nullable OnPermissionCallback callback) {
        if (callback != null) {
            callback.onDenied(deniedPermissions, doNotAskAgain);
        }
        if (doNotAskAgain) {
            if (deniedPermissions.size() == 1 && Permission.ACCESS_MEDIA_LOCATION.equals(deniedPermissions.get(0))) {
                Toast.makeText(activity, R.string.common_permission_media_location_hint_fail, Toast.LENGTH_SHORT).show();
                return;
            }
            showPermissionSettingDialog(activity, allPermissions, deniedPermissions, callback);
            return;
        }
        if (deniedPermissions.size() == 1) {

            String deniedPermission = deniedPermissions.get(0);

            if (Permission.ACCESS_BACKGROUND_LOCATION.equals(deniedPermission)) {
                Toast.makeText(activity, R.string.common_permission_background_location_fail_hint, Toast.LENGTH_SHORT).show();
                return;
            }
            if (Permission.BODY_SENSORS_BACKGROUND.equals(deniedPermission)) {
                Toast.makeText(activity, R.string.common_permission_background_sensors_fail_hint, Toast.LENGTH_SHORT).show();
                return;
            }
        }
        final String message;
        List<String> permissionNames = PermissionChange.permissionsToNames(activity, deniedPermissions);
        if (!permissionNames.isEmpty()) {
            message = activity.getString(R.string.common_permission_fail_assign_hint,
                    PermissionChange.listToString(activity, permissionNames));
        } else {
            message = activity.getString(R.string.common_permission_fail_hint);
        }
        Toast.makeText(activity, message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void finishPermissionRequest(@NonNull Activity activity, @NonNull List<String> allPermissions,
                                        boolean skipRequest, @Nullable OnPermissionCallback callback) {
        mRequestFlag = false;
        dismissPopupWindow();
    }

    private void showPopupWindow(Activity activity, ViewGroup decorView, String message) {
        if (mPermissionPopup == null) {
            View contentView = LayoutInflater.from(activity).inflate(R.layout.permission_popup, decorView, false);
            mPermissionPopup = new PopupWindow(activity);
            mPermissionPopup.setContentView(contentView);
            mPermissionPopup.setWidth(WindowManager.LayoutParams.MATCH_PARENT);
            mPermissionPopup.setHeight(WindowManager.LayoutParams.WRAP_CONTENT);
            mPermissionPopup.setAnimationStyle(android.R.style.Animation_Dialog);
            mPermissionPopup.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            mPermissionPopup.setTouchable(true);
            mPermissionPopup.setOutsideTouchable(true);
        }
        TextView messageView = mPermissionPopup.getContentView().findViewById(R.id.tv_permission_description_message);
        messageView.setText(message);
        mPermissionPopup.showAtLocation(decorView, Gravity.TOP, 0, 0);
    }

    private void dismissPopupWindow() {
        if (mPermissionPopup == null) {
            return;
        }
        if (!mPermissionPopup.isShowing()) {
            return;
        }
        mPermissionPopup.dismiss();
    }

    private void showPermissionSettingDialog(Activity activity, List<String> allPermissions,
                                             List<String> deniedPermissions, OnPermissionCallback callback) {
        if (activity == null || activity.isFinishing() ||
                (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1 && activity.isDestroyed())) {
            return;
        }
        final String message;
        List<String> permissionNames = PermissionChange.permissionsToNames(activity, deniedPermissions);
        if (!permissionNames.isEmpty()) {
            message = activity.getString(R.string.common_permission_manual_assign_fail_hint,
                    PermissionChange.listToString(activity, permissionNames));
        } else {
            message = activity.getString(R.string.common_permission_manual_fail_hint);
        }
        new AlertDialog.Builder(activity)
                .setTitle(R.string.common_permission_alert)
                .setMessage(message)
                .setPositiveButton(R.string.common_permission_goto_setting_page, (dialog, which) -> {
                    dialog.dismiss();
                    XXPermissions.startPermissionActivity(activity,
                            deniedPermissions, new OnPermissionPageCallback() {

                                @Override
                                public void onGranted() {
                                    if (callback == null) {
                                        return;
                                    }
                                    callback.onGranted(allPermissions, true);
                                }

                                @Override
                                public void onDenied() {
                                    showPermissionSettingDialog(activity, allPermissions, XXPermissions.getDenied(activity, allPermissions), callback);
                                }
                            });
                })
                .show();
    }

}
