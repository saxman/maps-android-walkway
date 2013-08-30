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
     * we need quick lookup.
     */
    private static final ArrayList<Place> PLACES = new ArrayList<Place>(5);

    /* Initialize the static collection. */
    static {
        PLACES.add(new Place("Ferry Building", 37.7955, -122.3937));
        PLACES.add(new Place("Exploratorium", 37.801434, -122.397561));
        PLACES.add(new Place("Coit Tower", 37.8025, -122.405833));
        PLACES.add(new Place("Dragon (Chinatown) Gate", 37.790582, -122.405624));
        PLACES.add(new Place("Union Square", 37.788056, -122.4075));
    }

    private GoogleMap mMap;

    /** A map from marker to place so we can get a place from marker events. */
    private HashMap<Marker, Place> mMarkerPlaceMap = new HashMap<Marker, Place>(PLACES.size());

    private Marker[] mMarkers = new Marker[PLACES.size()];
    
    /** The ViewPager for cycling through the list of places. */
    private ViewPager mPlaceViewPager;
    
    /** Bitmap used for drawing the (dot) markers that don't have focus. */
    private Bitmap mDotMarkerBitmap;
    
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
        mDotMarkerBitmap = Bitmap.createBitmap(25, 25, Bitmap.Config.ARGB_8888);
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

        for (Place place : PLACES) {
            LatLng point = new LatLng(place.lat, place.lng);
            Marker marker = mMap.addMarker(new MarkerOptions()
                    .position(point)
                    .icon(BitmapDescriptorFactory.fromBitmap(mDotMarkerBitmap)));
            
            mMarkerPlaceMap.put(marker, place);
            mMarkers[i++] = marker;
            
            builder.include(point);
        }

        // Show that the first marker has focus.
        mMarkers[0].setIcon(BitmapDescriptorFactory
                .defaultMarker(BitmapDescriptorFactory.HUE_AZURE));

        // Once the map has been loaded, zoom to the markers.
        mMap.setOnCameraChangeListener(new OnCameraChangeListener() {
            @Override
            public void onCameraChange(CameraPosition position) {
                mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 150));
                mMap.setOnCameraChangeListener(null);
            }
        });

        // When a marker is clicked, scroll the view pager to the corresponding
        // page.
        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                // XXX This is a slow operation, but we only have 5 markers at the moment.
                // Fastest way would be to go from maker to place (via map),
                // then from place to index (store index w/place on creation).
                int index = Arrays.asList(mMarkers).indexOf(marker);
                mPlaceViewPager.setCurrentItem(index, true);
                return false;
            }
        });

        // Setup the place view pager.
        mPlaceViewPager = (ViewPager) findViewById(R.id.pager);
        PlacePagerAdapter pagerAdapter = new PlacePagerAdapter();
        mPlaceViewPager.setAdapter(pagerAdapter);

        // Add an OnPageChangeListener so that we can change which marker has
        // focus as the page changes.
        mPlaceViewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            // The index of the last page that was shown.
            int lastPage = 0;

            @Override
            public void onPageScrollStateChanged(int state) {
            }

            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            }

            @Override
            public void onPageSelected(int position) {
                // Replace the previously selected marker with a dot.
                mMarkers[lastPage].setIcon(BitmapDescriptorFactory.fromBitmap(mDotMarkerBitmap));

                // Replace the currently selected maker with the full (focused)
                // marker.
                mMarkers[position].setIcon(BitmapDescriptorFactory
                        .defaultMarker(BitmapDescriptorFactory.HUE_AZURE));

                lastPage = position;
                
                // Move the camera to the new place.
                Place place = mMarkerPlaceMap.get(mMarkers[position]);
                mMap.animateCamera(CameraUpdateFactory.newLatLng(new LatLng(place.lat, place.lng)));
            }
        });
    }

    /**
     * PagerAdapter that creates Views of place info for the ViewPager.
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
                    Place place = PLACES.get(position);
                    mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                            new LatLng(place.lat, place.lng), 18f));
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
