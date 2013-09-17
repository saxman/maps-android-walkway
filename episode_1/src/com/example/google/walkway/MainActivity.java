/*
 * Copyright (C) 2013 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.google.walkway;

import com.example.google.walkway.model.Place;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnCameraChangeListener;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class MainActivity extends FragmentActivity {
    /**
     * The list of places that we'd list to show on the map. An ArrayList since
     * we need quick lookup by index.
     */
    private static final ArrayList<Place> PLACES = new ArrayList<Place>();

    /* Initialize the static collection. */
    static {
        PLACES.add(new Place("Ferry Building", 37.7955, -122.3937));
        PLACES.add(new Place("Exploratorium", 37.801434, -122.397561));
        PLACES.add(new Place("Greenwich Street Stairs", 37.8030764, -122.4035185));
        PLACES.add(new Place("Coit Tower", 37.8025, -122.405833));
        PLACES.add(new Place("Dragon (Chinatown) Gate", 37.790582, -122.405624));
        PLACES.add(new Place("Union Square", 37.788056, -122.4075));
        PLACES.add(new Place("Yerba Buena Gardens", 37.785607, -122.402691));
    }

    // UI constants
    private static final float PLACE_DETAIL_ZOOM = 18f;
    private static final float PLACE_MARKER_HUE = BitmapDescriptorFactory.HUE_RED;
    private static final int PLACE_ANIMATION_MS = 350;
    private static final int MAP_RECENTER_ANIMATION_MS = 350;

//    private static final String LOG_TAG = MainActivity.class.getName();
    
    private GoogleMap mMap;

    /** A map from marker to place so we can get a place from marker events. */
    private HashMap<Marker, Place> mMarkerPlaceMap = new HashMap<Marker, Place>(PLACES.size());

    private Marker[] mMarkers = new Marker[PLACES.size()];

    /** The ViewPager for cycling through the list of places. */
    private ViewPager mPlaceViewPager;

    /** Bitmap used for drawing the (dot) markers that don't have focus. */
    private Bitmap mDotMarkerBitmap;

    /** The initial camera position of the map. Used to reset the map state. */
    private CameraPosition mMapInitPosition;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setUpMapIfNeeded();
    }

    @Override
    protected void onResume() {
        super.onResume();
        setUpMapIfNeeded();
    }

    @Override
    public void onBackPressed() {
        if (!mMap.getCameraPosition().equals(mMapInitPosition)) {
            // If the map is zoomed or panned, reset its position.
            // Oddly, cannot just re-use mMapInitPosition to re-position.
            // newCameraPosition(mMapInitPosition) != mMapInitPosition
            final LatLngBounds.Builder builder = LatLngBounds.builder();
            for (Marker marker : mMarkers) {
                builder.include(marker.getPosition());
            }
            int px = getResources().getDimensionPixelSize(R.dimen.map_padding);
            mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), px),
                    MAP_RECENTER_ANIMATION_MS, null);
            // XXX Need different animation duration for zoom vs recenter
        } else if (mPlaceViewPager.getCurrentItem() != 0) {
            // Reset the pager to its original state.
            mPlaceViewPager.setCurrentItem(0);
            // Cancel the camera animation triggered by the paging. Otherwise,
            // the first marker is centered and the next back will not close the
            // activity.
            mMap.stopAnimation();
        } else {
            // If the map is at default zoom and the first place is selected,
            // execute default back action.
            super.onBackPressed();
        }
    }

    private void setUpMapIfNeeded() {
        if (mMap == null) {
            mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                    .getMap();

            if (mMap != null) {
                setUpMap();
            }
        }
    }

    private void setUpMap() {
        // Create a marker bitmap from the dot shape drawable.
        int px = getResources().getDimensionPixelSize(R.dimen.map_dot_marker_size);
        mDotMarkerBitmap = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(mDotMarkerBitmap);
        Drawable shape = getResources().getDrawable(R.drawable.map_dot);
        shape.setBounds(0, 0, mDotMarkerBitmap.getWidth(), mDotMarkerBitmap.getHeight());
        shape.draw(canvas);

        // Setup the map.
        FragmentManager fragmentManager = getSupportFragmentManager();
        SupportMapFragment mapFragment = (SupportMapFragment) fragmentManager
                .findFragmentById(R.id.map);

        mMap = mapFragment.getMap();
        mMap.setMyLocationEnabled(true);
        
        // This builder will be used to set the initial map zoom/pos.
        final LatLngBounds.Builder builder = LatLngBounds.builder();
        int i = 0;

        // Create the markers for the map.
        for (Place place : PLACES) {
            LatLng point = new LatLng(place.lat, place.lng);
            Marker marker = mMap.addMarker(new MarkerOptions()
                    .position(point)
                    .anchor(.5f, .5f)
                    .icon(BitmapDescriptorFactory.fromBitmap(mDotMarkerBitmap)));
            
            mMarkerPlaceMap.put(marker, place);
            mMarkers[i++] = marker;
            
            builder.include(point);
        }

        // Show that the first marker has focus.
        mMarkers[0].setIcon(BitmapDescriptorFactory.defaultMarker(PLACE_MARKER_HUE));

        // Once the map has been loaded, move to the markers.
        mMap.setOnCameraChangeListener(new OnCameraChangeListener() {
            @Override
            public void onCameraChange(CameraPosition position) {
                int px = getResources().getDimensionPixelSize(R.dimen.map_padding);
                mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), px));
                mMapInitPosition = mMap.getCameraPosition();
                mMap.setOnCameraChangeListener(null);
            }
        });

        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                // This is a slow operation, but work for a few markers.
                // Better would be to go from maker to place (via map),
                // then from place to index (store index w/place on creation).
                int index = Arrays.asList(mMarkers).indexOf(marker);

                if (index == mPlaceViewPager.getCurrentItem()) {
                    // Zoom in if the marker is already selected.
                    zoomToPlace(index);
                } else {
                    mPlaceViewPager.setCurrentItem(index, true);
                }

                // Override default behavior. Otherwise, if the selected marker
                // is clicked, the above animation is overridden.
                return true;
            }
        });

        // Setup the place view pager.
        mPlaceViewPager = (ViewPager) findViewById(R.id.pager);
        mPlaceViewPager.setAdapter(new PlacePagerAdapter());

        // Add an OnPageChangeListener so that we can change which marker has
        // focus as the page changes.
        mPlaceViewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            // The index of the last page that was shown.
            int lastPagePosition = 0;

            /*
             * When a new page is displayed, change the highlighted marker.
             * @see android.support.v4.view.ViewPager.OnPageChangeListener#onPageSelected(int)
             */
            @Override
            public void onPageSelected(int position) {
                // Replace the previously selected marker with a dot.
                mMarkers[lastPagePosition].setIcon(BitmapDescriptorFactory.fromBitmap(mDotMarkerBitmap));

                // Replace the currently selected maker with the full marker.
                mMarkers[position].setIcon(BitmapDescriptorFactory.defaultMarker(PLACE_MARKER_HUE));

                lastPagePosition = position;
                LatLng coords = mMarkers[position].getPosition();
                
                // Move the camera to the new place.
                mMap.animateCamera(CameraUpdateFactory.newLatLng(coords), PLACE_ANIMATION_MS, null);
            }
            
            @Override
            public void onPageScrollStateChanged(int state) {
            }

            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }
        });
    }

    /**
     * Zoom to place to show street-level detail.
     * 
     * @param position The index of the place in the PLACES collection.
     */
    private void zoomToPlace(int position) {
        Place place = PLACES.get(position);
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(place.lat, place.lng),
                PLACE_DETAIL_ZOOM), MAP_RECENTER_ANIMATION_MS, null);
        // XXX Need independent animation ms for place zoom in/out
    }

    /**
     * PagerAdapter that creates Views with place info for the ViewPager.
     */
    private class PlacePagerAdapter extends PagerAdapter {
        @Override
        public Object instantiateItem(final View collection, final int position) {
            LayoutInflater inflater =
                    (LayoutInflater) collection.getContext().getSystemService(
                            Context.LAYOUT_INFLATER_SERVICE);

            View view = inflater.inflate(R.layout.view_pager_place, null, false);

            // If the view is clicked, zoom in on its corresponding marker.
            view.setOnClickListener(new OnClickListener() {
                public void onClick(View view) {
                    zoomToPlace(position);
                }
            });

            Place place = PLACES.get(position);
            ((TextView) view.findViewById(R.id.place_name)).setText(place.name);

            ((ViewPager) collection).addView(view, 0);

            return view;
        }

        @Override
        public int getCount() {
            return PLACES.size();
        }

        @Override
        public void destroyItem(View collection, int position, Object view) {
            ((ViewPager) collection).removeView((View) view);
        }

        @Override
        public boolean isViewFromObject(View view, Object object) {
            return view.equals(object);
        }
    }
}
