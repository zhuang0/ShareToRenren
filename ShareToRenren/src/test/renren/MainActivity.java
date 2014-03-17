package test.renren;

import com.renren.api.connect.android.Renren;
import com.renren.api.connect.android.exception.RenrenAuthError;
import com.renren.api.connect.android.view.RenrenAuthListener;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends Activity {
	
	private static final String API_KEY = "6b1016db20c540e78bd1b20be4c707a3";	
	private static final String SECRET_KEY = "4723a695c09e4ddebbe8d87393d95fb4";	
	private static final String APP_ID = "105381";
	private Button shareBtn = null;
	private Renren renren;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        
        renren = new Renren(API_KEY, SECRET_KEY, APP_ID, this);
        final RenrenAuthListener listener = new RenrenAuthListener() {

			@Override
			public void onComplete(Bundle values) {
				Log.d("test",values.toString());
				Toast.makeText(MainActivity.this, 
						"登录成功，转向分享页面", 
						Toast.LENGTH_SHORT).show();
				Intent intent = new Intent(MainActivity.this, ShareRenrenActivity.class);
				intent.putExtra(Renren.RENREN_LABEL, renren);
				startActivity(intent);
			}

			@Override
			public void onRenrenAuthError(
					RenrenAuthError renrenAuthError) {
				Toast.makeText(MainActivity.this, 
						"登录失败，请重新登录", 
						Toast.LENGTH_SHORT).show();
				
			}

			@Override
			public void onCancelLogin() {
			}

			@Override
			public void onCancelAuth(Bundle values) {
			}
			
		};
        shareBtn =(Button) findViewById(R.id.sharebtn);
        shareBtn.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				renren.authorize(MainActivity.this, null, listener,1);
			}
		});
        
        
        
    }
}