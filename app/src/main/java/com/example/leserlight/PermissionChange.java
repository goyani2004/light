package com.example.leserlight;

import android.content.Context;

import androidx.annotation.NonNull;

import com.hjq.permissions.Permission;

import java.util.ArrayList;
import java.util.List;

public class PermissionChange {
    public static String getPermissionString(Context context, List<String> permissions) {
        return listToString(context, permissionsToNames(context, permissions));
    }

    public static String listToString(Context context, List<String> hints) {
        if (hints == null || hints.isEmpty()) {
            return context.getString(R.string.common_permission_unknown);
        }
        StringBuilder builder = new StringBuilder();
        for (String text : hints) {
            if (builder.length() == 0) {
                builder.append(text);
            } else {
                builder.append("、")
                        .append(text);
            }
        }
        return builder.toString();
    }

    @NonNull
    public static List<String> permissionsToNames(Context context, List<String> permissions) {
        List<String> permissionNames = new ArrayList<>();
        if (context == null) {
            return permissionNames;
        }
        if (permissions == null) {
            return permissionNames;
        }
        for (String permission : permissions) {
            switch (permission) {
                case Permission.CAMERA: {
                    String hint = context.getString(R.string.common_permission_storage);
                    if (!permissionNames.contains(hint)) {
                        permissionNames.add(hint);
                    }
                    break;
                }
                default:
                    break;
            }
        }
        return permissionNames;
    }
}
