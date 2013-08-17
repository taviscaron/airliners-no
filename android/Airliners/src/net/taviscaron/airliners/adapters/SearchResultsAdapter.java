package net.taviscaron.airliners.adapters;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import net.taviscaron.airliners.R;
import net.taviscaron.airliners.data.ImageLoader;
import net.taviscaron.airliners.model.AircraftSearchResult;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * SearchResults table adapter
 * @author Andrei Senchuk
 */
public class SearchResultsAdapter extends BaseAdapter {
    protected ImageLoader imageLoader;
    protected List<AircraftSearchResult> results;
    protected Context context;

    public SearchResultsAdapter(Context context) {
        this.context = context.getApplicationContext();
        this.results = new ArrayList<AircraftSearchResult>();
        this.imageLoader = new ImageLoader(context, ImageLoader.THUMB_CACHE_TAG);
    }

    @Override
    public int getCount() {
        return results.size();
    }

    @Override
    public AircraftSearchResult getItem(int position) {
        return results.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View view, ViewGroup parent) {

        if(view == null) {
            view = LayoutInflater.from(context).inflate(R.layout.aircraft_search_result_item, parent, false);

            ViewHolder holder = new ViewHolder();
            holder.imageLoadingProgressBar = (ProgressBar)view.findViewById(R.id.aircraft_search_result_item_image_progress);
            holder.imageView = (ImageView)view.findViewById(R.id.aircraft_search_result_item_image);
            holder.titleView = (TextView)view.findViewById(R.id.aircraft_search_result_list_item_title);

            view.setTag(holder);
        }

        final ViewHolder holder = (ViewHolder)view.getTag();

        AircraftSearchResult result = getItem(position);

        holder.titleView.setText(String.format("%s %s", result.getAirline(), result.getAircraft()));
        holder.imageLoadingProgressBar.setVisibility(View.GONE);

        imageLoader.loadImage(result.getThumbUrl(), new ImageLoader.ImageLoaderCallback() {
            @Override
            public void imageLoaded(ImageLoader loader, String url, Bitmap bitmap) {
                holder.imageLoadingProgressBar.setVisibility(View.GONE);
                holder.imageView.setImageBitmap(bitmap);
                holder.imageView.setVisibility(View.VISIBLE);
            }

            @Override
            public void imageLoadFailed(ImageLoader loader, String url) {
                holder.imageLoadingProgressBar.setVisibility(View.GONE);
            }

            @Override
            public void imageLoadFromNetworkStarted(ImageLoader loader, String url) {
                holder.imageLoadingProgressBar.setVisibility(View.VISIBLE);
                holder.imageView.setVisibility(View.GONE);
            }
        });

        return view;
    }

    public void addAll(List<AircraftSearchResult> newResults) {
        results.addAll(newResults);
        notifyDataSetChanged();
    }

    private static class ViewHolder {
        ProgressBar imageLoadingProgressBar;
        ImageView imageView;
        TextView titleView;
    }
}