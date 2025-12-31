package com.feitian.diagnostics.diagnostics;

import android.content.Context;
import android.os.Environment;
import android.os.StatFs;
import java.io.File;
import java.util.Locale;

public class StorageTestV2 implements DiagnosticTest {

    @Override
    public String getName() {
        return "Storage & Security";
    }

    @Override
    public DiagnosticResult run(Context context) {
        StringBuilder details = new StringBuilder();
        DiagnosticStatus status = DiagnosticStatus.PASS;

        // 1. Storage Capacity Check
        try {
            File path = Environment.getDataDirectory();
            StatFs stat = new StatFs(path.getPath());
            long blockSize = stat.getBlockSizeLong();
            long totalBlocks = stat.getBlockCountLong();
            long availableBlocks = stat.getAvailableBlocksLong();

            long totalSpace = (totalBlocks * blockSize) / (1024 * 1024 * 1024); // GB
            long freeSpace = (availableBlocks * blockSize) / (1024 * 1024 * 1024); // GB

            details.append(String.format(Locale.US, "Storage: %d GB Total, %d GB Free\n", totalSpace, freeSpace));
            
            if (freeSpace < 1) { // Warning if less than 1GB free
                details.append("Warning: Low storage space.\n");
                status = DiagnosticStatus.WARNING;
            }
        } catch (Exception e) {
            details.append("Storage check failed: ").append(e.getMessage()).append("\n");
            status = DiagnosticStatus.FAIL;
        }

        // 2. Basic Security/Integrity Check (Root Detection)
        // Malicious attacks often target rooted POS devices to intercept card data.
        boolean isRooted = checkRootMethod1() || checkRootMethod2();
        details.append("Security Health: ").append(isRooted ? "RISK DETECTED (Rooted)" : "Secure");
        
        if (isRooted) {
            status = DiagnosticStatus.FAIL;
            details.append("\nAlert: Device integrity compromised. Potential for malicious activity.");
        }

        return new DiagnosticResult(getName(), status, details.toString());
    }

    private boolean checkRootMethod1() {
        String buildTags = android.os.Build.TAGS;
        return buildTags != null && buildTags.contains("test-keys");
    }

    private boolean checkRootMethod2() {
        String[] paths = { "/system/app/Superuser.apk", "/sbin/su", "/system/bin/su", "/system/xbin/su", "/data/local/xbin/su", "/data/local/bin/su", "/system/sd/xbin/su",
                "/system/bin/failsafe/su", "/data/local/su" };
        for (String path : paths) {
            if (new File(path).exists()) return true;
        }
        return false;
    }
}
