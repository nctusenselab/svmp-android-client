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

//import org.mitre.svmp.RemoteServerClient;
import org.mitre.svmp.protocol.SVMPProtocol;
import org.mitre.svmp.protocol.SVMPProtocol.IntentAction;
import org.mitre.svmp.protocol.SVMPProtocol.Request.RequestType;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;

import android.content.Intent;
import org.mitre.svmp.services.SessionService;
import org.mitre.svmp.activities.ConnectionList;
import android.os.Handler;

// file forwarding
import android.net.Uri;
import com.google.protobuf.ByteString;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;

import java.util.Date;
import java.text.SimpleDateFormat;

public class SendNetIntent extends Activity
{
	private static final String TAG = "SendNetIntent";
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		final Handler handler = new Handler();

		new Thread(new Runnable() {
			@Override
			public void run() {
				forwardIntent();
			}
		}).start();
		finish();
	}

	private void forwardIntent() {
		//Put together the Intent protobuffer.
		Log.i(TAG,"GOING TO SEND URL INTENT with URL: "+getIntent().getDataString());
		final SVMPProtocol.Request.Builder msg = SVMPProtocol.Request.newBuilder();
		SVMPProtocol.Intent.Builder intentProtoBuffer = SVMPProtocol.Intent.newBuilder();

		Intent it = getIntent();
		if(it.getAction().equals(Intent.ACTION_VIEW)) {
			intentProtoBuffer.setAction(IntentAction.ACTION_VIEW);
			intentProtoBuffer.setData(getIntent().getDataString());
			if(getIntent().getData().getScheme().equals("file")) { // handle file forwarding
				SVMPProtocol.File.Builder f = SVMPProtocol.File.newBuilder();
				f.setFilename(getIntent().getData().getLastPathSegment());
				f.setData(getByteString(getIntent().getData()));
				intentProtoBuffer.setFile(f);
				SessionService.recordFilesStatic(getIntent().getData().getLastPathSegment());
			}
		}
		else if(it.getAction().equals(Intent.ACTION_SEND)) {
			intentProtoBuffer.setAction(IntentAction.ACTION_SEND);
			Uri data = (Uri) it.getParcelableExtra(Intent.EXTRA_STREAM);
			if(data != null && data.getScheme().equals("file")) {
				SVMPProtocol.File.Builder f = SVMPProtocol.File.newBuilder();
				f.setFilename(data.getLastPathSegment());
				f.setData(getByteString(data));
				intentProtoBuffer.setFile(f);
				SessionService.recordFilesStatic(data.getLastPathSegment());
			}
		}

		//Set the Request message params and send it off.
		msg.setType(RequestType.INTENT);

		msg.setIntent(intentProtoBuffer.build());
//		RemoteServerClient.sendMessage(msg.build());

		Intent intent = new Intent();
		intent.setClass(this, ConnectionList.class);
		intent.putExtra("connectionID", SessionService.getConnectionID());
		startActivity(intent);

		String timeStamp = new SimpleDateFormat("HH.mm.ss.SS").format(new Date());
		Log.i(TAG, "Forwarding intent. Timestamp: " + timeStamp + " " + System.currentTimeMillis());

    	SessionService.sendMessageStatic(msg.build());
	}

	private ByteString getByteString(Uri uri) {
		try {
			InputStream iStream = getContentResolver().openInputStream(uri);
			byte[] inputData = getBytes(iStream);
			return ByteString.copyFrom(inputData);
		} catch(Exception e) {}
		return null;
	}
	private byte[] getBytes(InputStream inputStream) throws Exception {
    ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
    int bufferSize = 1024;
    byte[] buffer = new byte[bufferSize];

    int len = 0;
    while ((len = inputStream.read(buffer)) != -1) {
      byteBuffer.write(buffer, 0, len);
    }
    return byteBuffer.toByteArray();
  }
}
