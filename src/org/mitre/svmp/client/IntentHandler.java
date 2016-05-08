/*
Copyright 2013 The MITRE Corporation, All Rights Reserved.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this work except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
package org.mitre.svmp.client;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.Toast;
import org.mitre.svmp.protocol.SVMPProtocol;

import com.google.protobuf.ByteString;
import java.io.FileOutputStream;
import java.io.File;
import android.webkit.MimeTypeMap;

/**
 * @author Joe Portner
 * Receives Intents from the server to act upon on the client-side
 */
public class IntentHandler {
    private static final String TAG = IntentHandler.class.getName();

    public static void inspect(SVMPProtocol.Response response, Context context) {
        SVMPProtocol.Intent intent = response.getIntent();
        switch(intent.getAction()) {
            case ACTION_DIAL:
                Log.d(TAG, String.format("Received 'call' Intent for number '%s'", intent.getData()));
                int telephonyEnabled = isTelephonyEnabled(context);
                if (telephonyEnabled == 0) {
                    Intent call = new Intent(Intent.ACTION_CALL);
                    call.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    call.setData(Uri.parse(intent.getData()));
                    context.startActivity(call);
                }
                else {
                    // phone calls are not supported on this device; send a Toast to the user to let them know
                    Toast toast = Toast.makeText(context, telephonyEnabled, Toast.LENGTH_LONG);
                    toast.show();
                }
                break;
            case ACTION_VIEW:
                Log.d(TAG, String.format("Received 'view' Intent for uri '%s'", intent.getData()));
                Intent view = new Intent(Intent.ACTION_VIEW);
                view.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                if(intent.hasFile()) {
                    SVMPProtocol.Intent.File f = intent.getFile();
					Log.e(TAG, "Receiving a file with filename: " + f.getFilename());
					saveToFile(f);
					File savedFile = new File("/sdcard/" + f.getFilename());
					view.setDataAndType(Uri.fromFile(savedFile), "application/pdf");
                } else {
                    view.setData(Uri.parse(intent.getData()));
                }

                context.startActivity(view);
                break;
            default:
                break;
        }
    }

    protected static void saveToFile(SVMPProtocol.Intent.File f) {
		try {
			ByteString bs = f.getData();
			byte[] arr = bs.toByteArray();
			FileOutputStream out = new FileOutputStream("/sdcard/" + f.getFilename());
			out.write(arr);
			out.close();
		} catch(Exception e) {}
	}
    // returns an error message if telephony is not enabled
    private static int isTelephonyEnabled(Context context){
        int resId = 0;
        TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        if (tm != null) {
            if (tm.getPhoneType() != TelephonyManager.PHONE_TYPE_GSM
                    && !context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_TELEPHONY))
                resId = R.string.intentHandler_toast_noTelephonyCDMA;
            else if (tm.getSimState() != TelephonyManager.SIM_STATE_READY)
                resId = R.string.intentHandler_toast_noTelephonyGSM;
        }
        return resId;
    }
}
