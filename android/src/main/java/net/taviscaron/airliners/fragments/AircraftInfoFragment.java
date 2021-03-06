package net.taviscaron.airliners.fragments;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import net.taviscaron.airliners.R;
import net.taviscaron.airliners.data.AircraftPhotoLoader;
import net.taviscaron.airliners.data.BaseLoader;
import net.taviscaron.airliners.data.ImageLoader;
import net.taviscaron.airliners.model.AircraftPhoto;
import net.taviscaron.airliners.util.CommonUtil;

/**
 * Aircraft Info Fragment
 * @author Andrei Senchuk
 */
public class AircraftInfoFragment extends Fragment {
    public static final String SAVED_AIRCRAFT_KEY = "savedAircraft";
    public static final String SAVED_AIRCRAFT_ID_KEY = "savedAircraftId";

    private AircraftPhoto aircraftPhoto;
    private AircraftPhotoLoader photoLoader;
    private ImageLoader imageLoader;
    private String initialAircraftId;
    private String aircraftPhotoPath;

    public interface StateListener {
        public void onAircraftInfoLoadStarted(String id);
        public void onAircraftInfoLoaded(AircraftPhoto photo);
    }

    private final ImageLoader.ImageLoaderCallback imageLoaderCallback = new ImageLoader.ImageLoaderCallback() {
        public void imageLoaded(ImageLoader loader, String url, Bitmap bitmap, String imageCachePath) {
            aircraftPhotoPath = imageCachePath;
            View view = getView();
            if(view != null) {
                ImageView imageView = (ImageView)view.findViewById(R.id.aircraft_info_photo_view);
                imageView.setVisibility(View.VISIBLE);
                imageView.setImageBitmap(bitmap);
                updateImageViewSize();

                view.findViewById(R.id.aircraft_info_photo_progress_bar).setVisibility(View.GONE);
            }
        }

        public void imageLoadFailed(ImageLoader loader, String url) {
            View view = getView();
            if(view != null) {
                view.findViewById(R.id.aircraft_info_photo_progress_bar).setVisibility(View.GONE);
                showImageLoadingError();
            }
        }

        public void imageLoadStarted(ImageLoader loader, String url) {
            View view = getView();
            if(view != null) {
                view.findViewById(R.id.aircraft_info_photo_progress_bar).setVisibility(View.VISIBLE);
                view.findViewById(R.id.aircraft_info_photo_view).setVisibility(View.INVISIBLE);
            }
        }
    };

    private final AircraftPhotoLoader.BaseLoaderCallback<String, AircraftPhoto> aircraftPhotoLoader = new AircraftPhotoLoader.BaseLoaderCallback<String, AircraftPhoto>() {
        public void loadStarted(BaseLoader<String, AircraftPhoto> loader) {
            notifyOnAircraftInfoLoadStarted(initialAircraftId);
            View view = getView();
            if(view != null) {
                view.findViewById(R.id.aircraft_info_progress_bar).setVisibility(View.VISIBLE);
                view.findViewById(R.id.aircraft_info_layout).setVisibility(View.GONE);
            }
        }

        public void loadFinished(BaseLoader<String, AircraftPhoto> loader, AircraftPhoto obj) {
            notifyOnAircraftInfoLoaded(obj);
            View view = getView();
            if(view != null) {
                view.findViewById(R.id.aircraft_info_progress_bar).setVisibility(View.GONE);
                if(obj != null) {
                    aircraftPhoto = obj;
                } else {
                    showLoadingError();
                }

                if(aircraftPhoto != null) {
                    updateView();
                }
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        photoLoader = new AircraftPhotoLoader(getActivity());
        imageLoader = new ImageLoader(getActivity(), ImageLoader.IMAGE_CACHE_TAG);

        if(savedInstanceState != null) {
            aircraftPhoto = (AircraftPhoto)savedInstanceState.getSerializable(SAVED_AIRCRAFT_KEY);
            initialAircraftId = savedInstanceState.getString(SAVED_AIRCRAFT_ID_KEY);

            if (aircraftPhoto == null && initialAircraftId != null) {
                loadAircraft(initialAircraftId);
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.aircraft_info_view, container, false);
        v.findViewById(R.id.aircraft_info_layout).setVisibility(View.GONE);
        v.findViewById(R.id.aircraft_info_progress_bar).setVisibility(View.GONE);
        return v;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if(aircraftPhoto != null) {
            outState.putSerializable(SAVED_AIRCRAFT_KEY, aircraftPhoto);
        }

        if(initialAircraftId != null) {
            outState.putString(SAVED_AIRCRAFT_ID_KEY, initialAircraftId);
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        if(aircraftPhoto != null) {
            updateView();
        }
    }

    public void loadAircraft(String id) {
        initialAircraftId = id;
        photoLoader.load(id, aircraftPhotoLoader);
    }

    protected void updateImageViewSize() {
        ImageView imageView = (ImageView)getView().findViewById(R.id.aircraft_info_photo_view);
        ViewGroup.LayoutParams lp = imageView.getLayoutParams();
        lp.height = (int)((float)imageView.getDrawable().getIntrinsicHeight() / imageView.getDrawable().getIntrinsicWidth() * imageView.getWidth());
        imageView.setLayoutParams(lp);
    }

    protected void updateView() {
        View view = getView();
        view.findViewById(R.id.aircraft_info_layout).setVisibility(View.VISIBLE);
        view.findViewById(R.id.aircraft_info_progress_bar).setVisibility(View.GONE);

        // thumb
        ImageView imageView = (ImageView)view.findViewById(R.id.aircraft_info_photo_view);
        imageView.setImageResource(android.R.color.transparent);
        imageLoader.loadImage(aircraftPhoto.getImageUrl(), imageLoaderCallback);

        // airline
        TextView airlineText = (TextView)view.findViewById(R.id.aircraft_info_airline);
        airlineText.setText(aircraftPhoto.getAirline());

        // aircraft
        TextView aircraftText = (TextView)view.findViewById(R.id.aircraft_info_aircraft);
        aircraftText.setText(aircraftPhoto.getAircraft());

        // taken at
        TextView takenAtText = (TextView)view.findViewById(R.id.aircraft_info_taken_at);
        takenAtText.setText(aircraftPhoto.getTakenAt());

        // taken on
        TextView takenOnText = (TextView)view.findViewById(R.id.aircraft_info_taken_on);
        takenOnText.setText(aircraftPhoto.getTakenOn());

        // registration
        TextView regText = (TextView)view.findViewById(R.id.aircraft_info_registration);
        regText.setText(aircraftPhoto.fullReg());

        // author
        TextView authorText = (TextView)view.findViewById(R.id.aircraft_info_author);
        authorText.setText(aircraftPhoto.getAuthor());

        // remark
        TextView remarkTitle = (TextView)view.findViewById(R.id.aircraft_info_remark_title);
        TextView remarkText = (TextView)view.findViewById(R.id.aircraft_info_remark);
        if(aircraftPhoto.getRemark() != null) {
            remarkText.setText(aircraftPhoto.getRemark());
            remarkText.setVisibility(View.VISIBLE);
            remarkTitle.setVisibility(View.VISIBLE);
        } else {
            remarkText.setVisibility(View.GONE);
            remarkTitle.setVisibility(View.GONE);
        }
    }

    protected void showLoadingError() {
        int messageId = (CommonUtil.isNetworkAvailable(getActivity())) ? R.string.error_aircraft_loading : R.string.error_network_unavailable;
        if(aircraftPhoto != null) {
            Toast.makeText(getActivity(), messageId, Toast.LENGTH_SHORT).show();
        } else {
            AlertDialogFragment.createAlert(getActivity(), messageId).show(getFragmentManager());
        }
    }

    protected void showImageLoadingError() {
        if(isResumed()) {
            if(CommonUtil.isNetworkAvailable(getActivity())) {
                Toast.makeText(getActivity(), R.string.error_photo_loading, Toast.LENGTH_SHORT).show();
            } else {
                AlertDialogFragment.createAlert(getActivity(), R.string.error_network_unavailable).show(getFragmentManager());
            }
        }
    }

    public String getAircraftPhotoPath() {
        return aircraftPhotoPath;
    }

    public AircraftPhoto getAircraftPhoto() {
        return aircraftPhoto;
    }

    private void notifyOnAircraftInfoLoadStarted(String id) {
        Activity activity = getActivity();
        if(activity instanceof StateListener) {
            ((StateListener) activity).onAircraftInfoLoadStarted(id);
        }
    }

    private void notifyOnAircraftInfoLoaded(AircraftPhoto photo) {
        Activity activity = getActivity();
        if(activity instanceof StateListener) {
            ((StateListener) activity).onAircraftInfoLoaded(photo);
        }
    }
}
