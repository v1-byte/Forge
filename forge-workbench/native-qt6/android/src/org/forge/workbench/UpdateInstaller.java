package org.forge.workbench;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.net.Uri;
import android.provider.Settings;
import android.widget.Toast;
import androidx.core.content.FileProvider;
import java.io.File;

public final class UpdateInstaller {
    private UpdateInstaller() {}

    public static void install(Context context, String path) {
        try {
            File apk = new File(path);
            if (!apk.isFile() || apk.length() == 0) {
                Toast.makeText(context, "File update APK tidak ditemukan. Silakan unduh ulang.", Toast.LENGTH_LONG).show();
                return;
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !context.getPackageManager().canRequestPackageInstalls()) {
                Toast.makeText(context, "Izinkan Forge memasang update pada layar berikut, lalu kembali dan tekan Update lagi.", Toast.LENGTH_LONG).show();
                Intent settings = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                        Uri.parse("package:" + context.getPackageName()));
                settings.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(settings);
                return;
            }
            Uri uri = FileProvider.getUriForFile(context, context.getPackageName() + ".fileprovider", apk);
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, "application/vnd.android.package-archive");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            context.startActivity(intent);
        } catch (Exception error) {
            Toast.makeText(context, "Tidak dapat membuka installer Android: " + error.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}
