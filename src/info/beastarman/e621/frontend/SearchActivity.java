package info.beastarman.e621.frontend;

import java.io.IOException;
import java.util.ArrayList;

import info.beastarman.e621.R;
import info.beastarman.e621.api.E621Image;
import info.beastarman.e621.api.E621Search;
import info.beastarman.e621.middleware.ImageLoadRunnable;
import info.beastarman.e621.middleware.ImageViewHandler;
import info.beastarman.e621.middleware.OnlineImageNavigator;
import info.beastarman.e621.middleware.SearchQuery;
import info.beastarman.e621.views.LazyRunScrollView;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.app.ActionBar;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

public class SearchActivity extends BaseActivity
{
	public static String SEARCH = "search";
	public static String PAGE = "page";
	public static String LIMIT = "limit";
	
	public static String MIN_ID = "min_id";
	public static String MAX_ID = "max_id";

	public String search = "";
	public int page = 0;
	public int limit = 20;

	public String min_id = null;
	public String max_id = null;

	public String cur_min_id = null;
	public String cur_max_id = null;

	protected E621Search e621Search = null;
	private ArrayList<ImageView> imageViews = new ArrayList<ImageView>();

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_search);
		
		ActionBar actionBar = getActionBar();
	    actionBar.setDisplayHomeAsUpEnabled(true);

		search = getIntent().getStringExtra(SearchActivity.SEARCH);
		if(search == null)
		{
			search = "";
		}
		page = getIntent().getIntExtra(SearchActivity.PAGE, 0);
		limit = getIntent().getIntExtra(SearchActivity.LIMIT, 20);

		cur_min_id = min_id = getIntent().getStringExtra(SearchActivity.MIN_ID);
		cur_max_id = max_id = getIntent().getStringExtra(SearchActivity.MAX_ID);
		
		((EditText) findViewById(R.id.searchInput)).setText(search);
		
		Integer total_pages = getSearchResultsPages(search, limit);
		
		Resources res = getResources();
		
		String text;
		
		if(total_pages == null)
		{
			text = String.format(res.getString(R.string.page_counter),String.valueOf(page + 1),"...");
		}
		else
		{
			text = String.format(res.getString(R.string.page_counter),String.valueOf(page + 1),String.valueOf(total_pages));
		}
		
		TextView page_counter = (TextView) findViewById(R.id.page_counter);
		page_counter.setText(text);

		final Handler handler = new SearchHandler(this);

		new Thread(new Runnable() {
			public void run() {
				Message msg = handler.obtainMessage();
				msg.obj = get_results();
				handler.sendMessage(msg);
			}
		}).start();
	}
	
	protected Integer getSearchResultsPages(String search, int limit)
	{
		return e621.getSearchResultsPages(search,limit);
	}
	
	protected E621Search get_results()
	{
		try {
			return e621.post__index(search, page, limit);
		} catch (IOException e) {
			return null;
		}
	}

	@Override
	public void onStart() {
		super.onStart();

		if (e621Search != null) {
			update_results();
		}
	}

	@Override
	public void onStop() {
		super.onStop();

		for (ImageView img : imageViews) {
			Drawable drawable = img.getDrawable();
			if (drawable instanceof BitmapDrawable) {
				BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
				Bitmap bitmap = bitmapDrawable.getBitmap();
				
				if(bitmap != null)
				{
					bitmap.recycle();
				}
			}
		}

		imageViews.clear();

		LinearLayout layout = (LinearLayout) findViewById(R.id.content_wrapper);
		layout.removeAllViews();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
		case R.id.action_settings:
			open_settings();
			return true;
		case R.id.action_offine_search:
			offline_search();
			return true;
		case R.id.action_continue_later:
			continue_later();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}
	
	public void offline_search()
	{
		Intent intent = new Intent(this, DownloadsActivity.class);
		intent.putExtra(DownloadsActivity.SEARCH, search);
		startActivity(intent);
	}
	
	public void continue_later()
	{
		if(min_id != null && max_id != null)
		{
			new Thread(new Runnable()
			{
				@Override
				public void run()
				{
					e621.continue_later(SearchQuery.normalize(search), min_id, max_id);
				}
			}).start();
		}
		
		Intent intent = new Intent(this, MainActivity.class);
		startActivity(intent);
	}

	public void open_settings() {
		Intent intent;
		intent = new Intent(this, SettingsActivity.class);
		startActivity(intent);
	}

	public void update_results()
	{
		LinearLayout layout = (LinearLayout) findViewById(R.id.content_wrapper);
		layout.removeAllViews();

		if (e621Search == null) {
			TextView t = new TextView(getApplicationContext());
			t.setText(R.string.no_internet_no_results);
			t.setGravity(Gravity.CENTER_HORIZONTAL);
			t.setPadding(0, 24, 0, 0);

			layout.addView(t);

			return;
		}
		
		Resources res = getResources();
		String text = String.format(res.getString(R.string.page_counter),
				String.valueOf(e621Search.current_page() + 1), String.valueOf(e621Search.total_pages()));

		TextView page_counter = (TextView) findViewById(R.id.page_counter);
		page_counter.setText(text);
		
		if (e621Search.images.size() == 0) {
			TextView t = new TextView(getApplicationContext());
			t.setText(R.string.no_results);
			t.setGravity(Gravity.CENTER_HORIZONTAL);
			t.setPadding(0, 24, 0, 0);

			layout.addView(t);

			return;
		}

		int layout_width = layout.getWidth();
		
		LazyRunScrollView scroll = (LazyRunScrollView)findViewById(R.id.resultsScrollView);

		int image_y = 0;
		
		int position = e621Search.offset;
		
		for (final E621Image img : e621Search.images) {
			ImageView imgView = new ImageView(getApplicationContext());
			RelativeLayout rel = new RelativeLayout(getApplicationContext());
			ProgressBar bar = new ProgressBar(getApplicationContext());
			ImageButton download = new ImageButton(getApplicationContext());

			if(cur_min_id != null)
			{
				cur_min_id = String.valueOf(Math.min(Integer.parseInt(cur_min_id), Integer.parseInt(img.id)));
			}
			else
			{
				cur_min_id = img.id;
			}

			if(cur_max_id != null)
			{
				cur_max_id = String.valueOf(Math.max(Integer.parseInt(cur_max_id), Integer.parseInt(img.id)));
			}
			else
			{
				cur_max_id = img.id;
			}
			
			int image_height = (int) (layout_width * (((double)img.preview_height) / img.preview_width));
			
			LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(new LinearLayout.LayoutParams(
					layout_width,
					image_height));
			imgView.setLayoutParams(lp);
			
			RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(
				    RelativeLayout.LayoutParams.WRAP_CONTENT, 
				    RelativeLayout.LayoutParams.WRAP_CONTENT);
			params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE);
			params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT, RelativeLayout.TRUE);
			download.setLayoutParams(params);
			
			if(e621.isSaved(img))
			{
				download.setImageResource(android.R.drawable.ic_menu_delete);
				
				download.setOnClickListener(new View.OnClickListener() {
			        @Override
			        public void onClick(View v) {
			        	removeImage(img,(ImageButton)v);
			        }
			    });
			}
			else
			{
				download.setImageResource(android.R.drawable.ic_menu_save);
				
				download.setOnClickListener(new View.OnClickListener() {
			        @Override
			        public void onClick(View v) {
			        	saveImage(img,(ImageButton)v);
			        }
			    });
			}

			rel.setPadding(0, 20, 0, 20);

			imageViews.add(imgView);

			rel.addView(bar);
			rel.addView(imgView);
			rel.addView(download);
			layout.addView(rel);

			imgView.setTag(R.id.imagePosition, position);
			imgView.setTag(R.id.imageObject, img);

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

			ImageViewHandler handler = new ImageViewHandler(imgView, bar);
			
			scroll.addThread(new Thread(new ImageLoadRunnable(handler, img, e621,E621Image.PREVIEW)),image_y);
			
			image_y += image_height + 40;
			
			position++;
		}
	}
	
	public void saveImage(final E621Image img, final ImageButton v)
	{
		v.setImageResource(android.R.drawable.stat_sys_download);
		
		e621.saveImageAsync(img, this, new Runnable()
		{
			@Override
			public void run() {
				
				runOnUiThread(new Runnable()
				{
					@Override
					public void run() {
						v.setImageResource(android.R.drawable.ic_menu_delete);
						
						v.setOnClickListener(new View.OnClickListener() {
					        @Override
					        public void onClick(View v) {
					        	removeImage(img,(ImageButton)v);
					        }
					    });
					}
				});
			}
		}, new Runnable()
		{
			@Override
			public void run() {
				
				runOnUiThread(new Runnable()
				{
					@Override
					public void run() {
						v.setImageResource(android.R.drawable.ic_menu_save);
						
						v.setOnClickListener(new View.OnClickListener() {
					        @Override
					        public void onClick(View v) {
					        	saveImage(img,(ImageButton)v);
					        }
					    });
					}
				});
			}
		});
	}
	
	public void removeImage(final E621Image img, ImageButton v)
	{
		e621.deleteImage(img);
		
		v.setImageResource(android.R.drawable.ic_menu_save);
		
		v.setOnClickListener(new View.OnClickListener() {
	        @Override
	        public void onClick(View v) {
	        	saveImage(img,(ImageButton)v);
	        }
	    });
	}

	public void imageClick(View view) {
		Intent intent = new Intent(this, ImageActivity.class);
		intent.putExtra(ImageActivity.NAVIGATOR, new OnlineImageNavigator(
				(E621Image) view.getTag(R.id.imageObject),
				(Integer) view.getTag(R.id.imagePosition),
				search,
				e621Search));
		intent.putExtra(ImageActivity.INTENT,getIntent());
		startActivity(intent);
	}

	public void search(View view) {
		EditText editText = (EditText) findViewById(R.id.searchInput);
		String search = editText.getText().toString().trim();

		Intent intent = new Intent(this, SearchActivity.class);
		intent.putExtra(SearchActivity.SEARCH, search);
		startActivity(intent);
	}

	public void prev(View view)
	{
		if (page > 0)
		{
			if(e621Search != null && !e621Search.has_prev_page())
			{
				return;
			}
			Intent intent = new Intent(this, SearchActivity.class);
			intent.putExtra(SearchActivity.SEARCH, search);
			intent.putExtra(SearchActivity.PAGE, page - 1);
			intent.putExtra(SearchActivity.LIMIT, limit);
			intent.putExtra(SearchActivity.MIN_ID, cur_min_id);
			intent.putExtra(SearchActivity.MAX_ID, cur_max_id);
			startActivity(intent);
		}
	}

	public void next(View view)
	{
		if(e621Search != null && !e621Search.has_next_page())
		{
			return;
		}
		
		Intent intent = new Intent(this, SearchActivity.class);
		intent.putExtra(SearchActivity.SEARCH, search);
		intent.putExtra(SearchActivity.PAGE, page + 1);
		intent.putExtra(SearchActivity.LIMIT, limit);
		intent.putExtra(SearchActivity.MIN_ID, cur_min_id);
		intent.putExtra(SearchActivity.MAX_ID, cur_max_id);
		startActivity(intent);
	}

	private static class SearchHandler extends Handler {
		SearchActivity activity;

		public SearchHandler(SearchActivity activity) {
			this.activity = activity;
		}

		@Override
		public void handleMessage(Message msg) {
			E621Search result = (E621Search) msg.obj;
			activity.e621Search = result;
			activity.update_results();
		}
	};

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.search, menu);
		return true;
	}
}
