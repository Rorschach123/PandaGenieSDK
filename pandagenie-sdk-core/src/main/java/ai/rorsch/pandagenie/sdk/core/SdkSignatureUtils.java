package ai.rorsch.pandagenie.sdk.core;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Build;

import java.security.MessageDigest;
import java.util.Locale;

public final class SdkSignatureUtils {
    private SdkSignatureUtils() {}

    public static String getOwnSignatureSha256(Context context) throws PackageManager.NameNotFoundException {
        return getSignatureSha256(context, context.getPackageName());
    }

    public static String getSignatureSha256(Context context, String packageName) throws PackageManager.NameNotFoundException {
        PackageManager pm = context.getPackageManager();
        PackageInfo info;
        Signature[] signatures;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            info = pm.getPackageInfo(packageName, PackageManager.GET_SIGNING_CERTIFICATES);
            if (info.signingInfo == null) return "";
            signatures = info.signingInfo.hasMultipleSigners()
                    ? info.signingInfo.getApkContentsSigners()
                    : info.signingInfo.getSigningCertificateHistory();
        } else {
            info = pm.getPackageInfo(packageName, PackageManager.GET_SIGNATURES);
            signatures = info.signatures;
        }
        if (signatures == null || signatures.length == 0) return "";
        return sha256(signatures[0].toByteArray());
    }

    public static String sha256(byte[] data) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(data);
            StringBuilder builder = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                builder.append(String.format(Locale.US, "%02X", b));
            }
            return builder.toString();
        } catch (Exception e) {
            return "";
        }
    }

    public static String sha256Lower(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes("UTF-8"));
            StringBuilder builder = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                builder.append(String.format(Locale.US, "%02x", b));
            }
            return builder.toString();
        } catch (Exception e) {
            return "";
        }
    }

    public static String normalizeSignatureSha256(String signatureSha256) {
        if (signatureSha256 == null) return "";
        return signatureSha256.replace(":", "").replaceAll("\\s+", "").toUpperCase(Locale.US);
    }

    public static String identitySha256(String packageName, String signatureSha256, String role) {
        String pkg = packageName == null ? "" : packageName.trim();
        String signature = normalizeSignatureSha256(signatureSha256);
        String normalizedRole = role == null ? "" : role.trim().toLowerCase(Locale.US);
        return sha256Lower(pkg + "|" + signature + "|" + normalizedRole);
    }
}
