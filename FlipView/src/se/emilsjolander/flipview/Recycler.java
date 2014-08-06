package se.emilsjolander.flipview;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;

public class Recycler {

	static class Scrap {
		View v;
		boolean valid;

		public Scrap(View scrap, boolean valid) {
			this.v = scrap;
			this.valid = valid;
		}
	}

	/** Unsorted views that can be used by the adapter as a convert view. */
	private SparseArray<Scrap>[] scraps;
	private SparseArray<Scrap> currentScraps;

	private int viewTypeCount;

	void setViewTypeCount(int viewTypeCount) {
		if (viewTypeCount < 1) {
			throw new IllegalArgumentException("Can't have a viewTypeCount < 1");
		}
		// do nothing if the view type count has not changed.
		if (currentScraps != null && viewTypeCount == scraps.length) {
			return;
		}
		// noinspection unchecked
		@SuppressWarnings("unchecked")
		SparseArray<Scrap>[] scrapViews = new SparseArray[viewTypeCount];
		for (int i = 0; i < viewTypeCount; i++) {
			scrapViews[i] = new SparseArray<Scrap>();
		}
		this.viewTypeCount = viewTypeCount;
		currentScraps = scrapViews[0];
		this.scraps = scrapViews;
	}

	/** @return A view from the ScrapViews collection. These are unordered. */
	Scrap getScrapView(int position, int viewType) {
		if (viewTypeCount == 1) {
			return retrieveFromScrap(currentScraps, position);
		} else if (viewType >= 0 && viewType < scraps.length) {
			return retrieveFromScrap(scraps[viewType], position);
		}
		return null;
	}

	/**
	 * Put a view into the ScrapViews list. These views are unordered.
	 * 
	 * @param scrap
	 *            The view to add
	 */
	@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
	void addScrapView(View scrap, int position, int viewType) {
		// create a new Scrap
		Scrap item = new Scrap(scrap, true);
		
		if (viewTypeCount == 1) {
			currentScraps.put(position, item);
		} else {
			scraps[viewType].put(position, item);
		}
		if (Build.VERSION.SDK_INT >= 14) {
			scrap.setAccessibilityDelegate(null);
		}
	}

	static Scrap retrieveFromScrap(SparseArray<Scrap> scrapViews, int position) {
		int size = scrapViews.size();
		if (size > 0) {
			// See if we still have a view for this position.
			Scrap result = scrapViews.get(position, null);
			if (result != null) {
				scrapViews.remove(position);
				return result;
			}
			int index = size - 1;
			result = scrapViews.valueAt(index);
			scrapViews.removeAt(index);
			result.valid = false;
			return result;
		}
		return null;
	}

	void invalidateScraps() {
		for (SparseArray<Scrap> array : scraps) {
			for (int i = 0; i < array.size(); i++) {
				array.valueAt(i).valid = false;
			}
		}
	}

	void removeScraps(View current,View prev, View next) {
		for (SparseArray<Scrap> array : scraps) {
			for (int i = 0; i < array.size(); i++) {
				Scrap scrap = array.valueAt(i);
				if (!scrap.v.getTag().equals(current.getTag()) && !scrap.v.getTag().equals(prev.getTag()) && !scrap.v.getTag().equals(next.getTag())){
					removeScrap(scrap);
				}
			}
		}
	}

	void removeAllScraps() {
		for (SparseArray<Scrap> array : scraps) {
			for (int i = 0; i < array.size(); i++) {
				Scrap scrap = array.valueAt(i);
				removeScrap(scrap);
			}
		}
	}

	void removeScrap(Scrap scrap) {
		scrap.valid=false;
		ImageView imageView = (ImageView )scrap.v.findViewWithTag("image");
		Drawable drawable = imageView.getDrawable();
		if (drawable instanceof BitmapDrawable) {
			BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
			Bitmap bitmap = bitmapDrawable.getBitmap();
			bitmap.recycle();
		}
		unbindDrawables(scrap.v);
		scrap.v = null;
	}

	private void unbindDrawables(View view)
	{
		if (view.getBackground() != null)
		{
			view.getBackground().setCallback(null);
		}
		if (view instanceof ViewGroup && !(view instanceof AdapterView))
		{
			for (int i = 0; i < ((ViewGroup) view).getChildCount(); i++)
			{
				unbindDrawables(((ViewGroup) view).getChildAt(i));
			}
			((ViewGroup) view).removeAllViews();
		}
	}

}
