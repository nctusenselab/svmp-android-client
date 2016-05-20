package org.mitre.svmp.client;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;
import org.mitre.svmp.protocol.SVMPProtocol;
import org.mitre.svmp.services.SessionService;

import com.google.protobuf.ByteString;
import java.io.FileOutputStream;
import java.io.File;
import android.webkit.MimeTypeMap;

public class FileHandler {
    private static final String TAG = FileHandler.class.getName();

    public static void inspect(SVMPProtocol.Response response, Context context) {
        SVMPProtocol.File svmpFile = response.getFile();
        SessionService.removeFromWaitingListStatic(svmpFile.getFilename());
        saveToFile(svmpFile);
    }
    protected static void saveToFile(SVMPProtocol.File f) {
		try {
			ByteString bs = f.getData();
			byte[] arr = bs.toByteArray();
			FileOutputStream out = new FileOutputStream("/sdcard/" + f.getFilename());
			out.write(arr);
			out.close();
		} catch(Exception e) {}
	}
}
