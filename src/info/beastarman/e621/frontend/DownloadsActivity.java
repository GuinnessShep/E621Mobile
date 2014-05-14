package info.beastarman.e621.frontend;

import java.io.InputStream;
import java.util.ArrayList;

import info.beastarman.e621.R;
import info.beastarman.e621.middleware.E621Middleware;
import android.os.Build;
import android.os.Bundle;
import android.os.Message;
import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class DownloadsActivity extends Activity
{
	public static String SEARCH = "search";
	public static String PAGE = "page";
	public static String LIMIT = "limit";

	public String search = "";
	public int page = 0;
	public int limit = 20;

	private ArrayList<String> downloads = null;
	private ArrayList<ImageView> imageViews = new ArrayList<ImageView>();
	
	E621Middleware e621 = null;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_search);
		
		e621 = E621Middleware.getInstance();
		
		search = getIntent().getExtras().getString(SearchActivity.SEARCH, "");
		page = getIntent().getExtras().getInt(SearchActivity.PAGE, 0);
		limit = getIntent().getExtras().getInt(SearchActivity.LIMIT, 20);

		((EditText) findViewById(R.id.searchInput)).setText(search);

		Resources res = getResources();
		String text = String.format(res.getString(R.string.page_counter),
				String.valueOf(page + 1),"...");

		TextView page_counter = (TextView) findViewById(R.id.page_counter);
		page_counter.setText(text);
	}
	
	@Override
	public void onStart() {
		super.onStart();
		
		downloads = e621.localSearch(page, limit, search);

		update_results();
	}
	
	@Override
	public void onStop() {
		super.onStop();

		for (ImageView img : imageViews) {
			Drawable drawable = img.getDrawable();
			if (drawable instanceof BitmapDrawable) {
				BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
				Bitmap bitmap = bitmapDrawable.getBitmap();
				bitmap.recycle();
			}
		}

		imageViews.clear();

		LinearLayout layout = (LinearLayout) findViewById(R.id.content_wrapper);
		layout.removeAllViews();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.downloads, menu);
		return true;
	}
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
		case R.id.action_settings:
			open_settings();
			return true;
		case R.id.action_export:
			export();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}
	
	public void export()
	{
		e621.export(search);
	}

	public void open_settings() {
		Intent intent;
		if (Build.VERSION.SDK_INT < 11) {
			intent = new Intent(this, SettingsActivityOld.class);
		} else {
			intent = new Intent(this, SettingsActivityNew.class);
		}
		startActivity(intent);
	}
	
	public void update_results() {
		final LinearLayout layout = (LinearLayout) findViewById(R.id.content_wrapper);
		layout.removeAllViews();
		
		/*
		Resources res = getResources();
		String text = String.format(res.getString(R.string.page_counter),
				String.valueOf(e621Search.current_page() + 1), String.valueOf(e621Search.total_pages()));

		TextView page_counter = (TextView) findViewById(R.id.page_counter);
		page_counter.setText(text);
		*/
		
		layout.post(new Runnable()
		{
			@Override
			public void run() {
				if (downloads.size() == 0) {
					TextView t = new TextView(getApplicationContext());
					t.setText(R.string.no_results);
					t.setGravity(Gravity.CENTER_HORIZONTAL);
					t.setPadding(0, 24, 0, 0);

					layout.addView(t);

					return;
				}

				DisplayMetrics dm = new DisplayMetrics();
				getWindowManager().getDefaultDisplay().getMetrics(dm);
				dm.widthPixels = layout.getWidth();

				for (final String img : downloads) {
					ImageView imgView = new ImageView(getApplicationContext());
					RelativeLayout rel = new RelativeLayout(getApplicationContext());
					ProgressBar bar = new ProgressBar(getApplicationContext());

					rel.setPadding(0, 20, 0, 20);

					imageViews.add(imgView);

					rel.addView(bar);
					rel.addView(imgView);
					layout.addView(rel);

					imgView.setTag(R.id.imageId, img.split(":")[0]);

					imgView.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							imageClick(v);
						}
					});

					RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) bar
							.getLayoutParams();
					layoutParams.addRule(RelativeLayout.CENTER_IN_PARENT,
							RelativeLayout.TRUE);
					bar.setLayoutParams(layoutParams);

					ImageViewHandler handler = new ImageViewHandler(imgView, dm, bar);
					
					new Thread(new ImageLoadRunnable(handler, img)).start();
				}
			}
		});
	}
	
	public void imageClick(View view) {
		String id = (String) view.getTag(R.id.imageId);

		Intent intent = new Intent(this, ImageActivity.class);
		intent.putExtra(ImageActivity.ID, id);
		startActivity(intent);
	}
	
	public void search(View view) {
		EditText editText = (EditText) findViewById(R.id.searchInput);
		String search = editText.getText().toString().trim();

		Intent intent = new Intent(this, DownloadsActivity.class);
		intent.putExtra(DownloadsActivity.SEARCH, search);
		startActivity(intent);
	}

	public void prev(View view)
	{
		if (page > 0)
		{
			Intent intent = new Intent(this, DownloadsActivity.class);
			intent.putExtra(DownloadsActivity.SEARCH, search);
			intent.putExtra(DownloadsActivity.PAGE, page - 1);
			intent.putExtra(DownloadsActivity.LIMIT, limit);
			startActivity(intent);
		}
	}

	public void next(View view)
	{
		Intent intent = new Intent(this, DownloadsActivity.class);
		intent.putExtra(DownloadsActivity.SEARCH, search);
		intent.putExtra(DownloadsActivity.PAGE, page + 1);
		intent.putExtra(DownloadsActivity.LIMIT, limit);
		startActivity(intent);
	}
	
	private class ImageLoadRunnable implements Runnable
	{
		ImageViewHandler handler;
		String id;
		
		public ImageLoadRunnable(ImageViewHandler handler, String id)
		{
			this.handler = handler;
			this.id = id;
		}

		@Override
		public void run() {
			InputStream in = e621.getDownloadedImage(id);
	    	Message msg = handler.obtainMessage();
	    	msg.obj = in;
	    	handler.sendMessage(msg);
		}
	}
}
