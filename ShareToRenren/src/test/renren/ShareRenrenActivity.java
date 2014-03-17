package test.renren;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;

import com.renren.api.connect.android.AsyncRenren;
import com.renren.api.connect.android.Renren;
import com.renren.api.connect.android.Util;

import com.renren.api.connect.android.common.AbstractRequestListener;
import com.renren.api.connect.android.exception.RenrenAuthError;
import com.renren.api.connect.android.exception.RenrenError;
import com.renren.api.connect.android.photos.PhotoUploadRequestParam;
import com.renren.api.connect.android.photos.PhotoUploadResponseBean;
import com.renren.api.connect.android.view.ProfileNameView;
import com.renren.api.connect.android.view.ProfilePhotoView;
import com.renren.api.connect.android.view.RenrenAuthListener;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class ShareRenrenActivity extends Activity {	
	/**
	 * 标识数据状态的消息值，依次为发送成功，出现异常，出现严重错误
	 */
	private final static int DATA_COMPLETE = 0xffff;
	private final static int DATA_ERROR = 0xfffe;
	private final static int DATA_FAULT = 0xfffd;

	/**
	 * bundle中错误信息的标识符
	 */
	private final static String ERROR_MSG = "error_message";
	File file;
	/**
	 * 照片描述
	 */
	String caption ;

	/**
	 * 相册aid
	 */
	TextView photoAidValue;
	/**
	 * 照片的描述
	 */
	EditText photoCaptionValue;
	/**
	 * 照片描述的字数计数器
	 */
	TextView photoCaptionCounter;
	/**
	 * 照片的缩略图
	 */
	ImageView photoViewImage;

	/**
	 * 提交按钮，上传照片
	 */
	Button submit;
	/**
	 * 取消上传
	 */
	Button cancel;
	/**
	 * 上传的照片请求参数实体
	 */
	PhotoUploadRequestParam photoParam = new PhotoUploadRequestParam();

	private Renren renren;
	
	ProgressDialog progressDialog;
	
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.renren_sdk_upload_photo); 
        Intent intent = getIntent();
		renren = intent.getParcelableExtra(Renren.RENREN_LABEL);
		
        photoCaptionValue = (EditText) findViewById(R.id.renren_sdk_photo_caption_value);
        photoCaptionCounter = (TextView) findViewById(R.id.renren_sdk_photo_caption_counter);
        
        // 初始化头像和名字控件
     		ProfilePhotoView profilePhotoView = (ProfilePhotoView) findViewById(R.id.renren_sdk_profile_photo);
     		profilePhotoView.setUid(renren.getCurrentUid());

     		ProfileNameView profileNameView = (ProfileNameView) findViewById(R.id.renren_sdk_profile_name);
     		profileNameView.setUid(renren.getCurrentUid(), renren);
        
     		// 读取assets文件夹下的图片，保存在手机中
     		String fileName = "renren.png";
     		// 获取文件后缀，构造本地文件名
     		int _index = fileName.lastIndexOf('.');
     		// 文件保存在/sdcard目录下，以renren_前缀加系统毫秒数构造文件名
     		final String realName = "renren_" + System.currentTimeMillis()
     				+ fileName.substring(_index, fileName.length());
     		try {
     			InputStream is = this.getResources().getAssets().open(fileName);
     			BufferedOutputStream bos = new BufferedOutputStream(
     					this.openFileOutput(realName, Context.MODE_PRIVATE));
     			int length = 0;
     			byte[] buffer = new byte[1024];
     			while ((length = is.read(buffer)) != -1) {
     				bos.write(buffer, 0, length);
     			}
     			is.close();
     			bos.close();
     		} catch (MalformedURLException e) {
     			e.printStackTrace();
     		} catch (IOException e) {
     			e.printStackTrace();
     		}
     		String filePath = this.getFilesDir().getAbsolutePath() + "/"
     				+ realName;
     		
     		file = new File(filePath);
        
     		// 显示默认的照片描述和字数统计
     		if (caption != null) {
     			int length = caption.length();
     			if (length > PhotoUploadRequestParam.CAPTION_MAX_LENGTH) {
     				caption = caption.substring(0, 140);
     			}
     			photoCaptionValue.setText(caption);
     			int index = caption.length();
     			photoCaptionValue.setSelection(index);
     			photoCaptionCounter.setText(length + "/"
     					+ PhotoUploadRequestParam.CAPTION_MAX_LENGTH);
     		}
     		
     		// 增加相片描述文本框的监听事件
    		photoCaptionValue.addTextChangedListener(new TextWatcher() {

    			@Override
    			public void onTextChanged(CharSequence s, int start, int before,
    					int count) {
    				// 设置计数器
    				photoCaptionCounter.setText(s.length() + "/"
    						+ PhotoUploadRequestParam.CAPTION_MAX_LENGTH);
    			}

    			@Override
    			public void beforeTextChanged(CharSequence s, int start, int count,
    					int after) {

    			}

    			@Override
    			public void afterTextChanged(Editable s) {

    			}
    		});
    		
    		photoViewImage = (ImageView) findViewById(R.id.renren_sdk_photo_view_image);
    		// 设置缩略图
    		Bitmap bitmap = null;
    		try {
    			bitmap = BitmapFactory.decodeStream(new FileInputStream(file));
    		} catch (FileNotFoundException e) {
    			Util.logger("exception in setting thumbnail: " + e.getMessage());
    		}
    		photoViewImage.setImageBitmap(bitmap);
    		
    		submit = (Button) findViewById(R.id.renren_sdk_upload_photo_submit);
    		cancel = (Button) findViewById(R.id.renren_sdk_upload_photo_cancel);
    		
    		submit.setOnClickListener(new Button.OnClickListener() {
    			@Override
    			public void onClick(View v) {
    				// 设置caption参数
    				String caption = photoCaptionValue.getText().toString();
    				if (caption != null && !"".equals(caption.trim())) {
    					photoParam.setCaption(caption);
    				}
    				// 设置file参数
    				photoParam.setFile(file);
    				// 调用SDK异步上传照片的接口
    				new AsyncRenren(renren).publishPhoto(photoParam,
    						new AbstractRequestListener<PhotoUploadResponseBean>() {
    							@Override
    							public void onRenrenError(RenrenError renrenError) {
    								if (renrenError != null) {
    									
    								}
    							}

    							@Override
    							public void onFault(Throwable fault) {
    								if (fault != null) {
    									handler.sendEmptyMessage(DATA_FAULT);
    								}
    							}

    							@Override
    							public void onComplete(PhotoUploadResponseBean bean) {
    								if (bean != null) {
    									// 上传成功，直接显示成功
    									handler.sendEmptyMessage(DATA_COMPLETE);
    									   									
    								}
    							}
    						});

    				// 正在上传照片，显示进度框
    				progressDialog.show();
    			}
    		});
    		cancel.setOnClickListener(new Button.OnClickListener() {
    			@Override
    			public void onClick(View v) {
    				finish();
    			}
    		});
    }
    
    Handler handler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			super.handleMessage(msg);

			switch (msg.what) {
			case DATA_COMPLETE:				
				Toast.makeText(ShareRenrenActivity.this, "分享成功", Toast.LENGTH_SHORT).show();
				
				ShareRenrenActivity.this.finish();
				break;
			case DATA_ERROR:
				Toast.makeText(ShareRenrenActivity.this, "上传图片失败", Toast.LENGTH_SHORT).show();
				

				break;
			case DATA_FAULT:
				Toast.makeText(ShareRenrenActivity.this, "上传图片失败", Toast.LENGTH_SHORT).show();
				
				break;
			default:
				
				break;
			}
		}
	};
    
	/**
	 * 显示等待框
	 */
	protected void showProgress() {
		showProgress("Please wait", "progressing");
	}

	/**
	 * 显示等待框
	 * 
	 * @param title
	 * @param message
	 */
	protected void showProgress(String title, String message) {
		progressDialog = ProgressDialog.show(this, title, message);
	}

	/**
	 * 取消等待框
	 */
	protected void dismissProgress() {
		if (progressDialog != null) {
			try {
				progressDialog.dismiss();
			} catch (Exception e) {

			}
		}
	}
	
    
    
    
}