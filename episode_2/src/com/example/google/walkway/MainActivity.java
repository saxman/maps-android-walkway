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
import com.example.google.walkway.model.Place.PlaceType;
import com.example.google.walkway.model.PlacesService;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Point;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.TreeSet;

public class MainActivity extends ActionBarActivity {
    /** Place types to display in the navigation drawer. */
    private static final PlaceType[] NAV_PLACE_TYPES = {
        PlaceType.PARK,
        PlaceType.MUSEUM,
        PlaceType.RESTAURANT,
        PlaceType.CAFE,
//        PlaceType.BAKERY,
//        PlaceType.BOOKSTORE,
//        PlaceType.WORSHIP,
        PlaceType.MONUMENT,
        PlaceType.SHOP,
//        PlaceType.THEATRE
    };

    private static final String LOG_TAG = MainActivity.class.getName();
    
    private TreeSet<Place.PlaceType> mSelectedPlaceTypes = new TreeSet<Place.PlaceType>();
    
    private GoogleMap mMap;

    private Marker[] mMarkers;

    /** The ViewPager for cycling through the list of places. */
    private ViewPager mPlaceViewPager;

    /** The initial camera position of the map. Used to reset the map state. */
    private CameraPosition mMapInitPosition;

    private DrawerLayout mDrawerLayout;
    private ListView mPlaceListView;
    private ListView mNavListView;

    private int mSelectedPlaceIndex = 0;
    
    private List<Place> mPlaces;
    
    // XXX do not use (use getDotMarkerBitmap())
    private Bitmap mDotMarkerBitmap;

    private ActionBarDrawerToggle mDrawerToggle;

    @SuppressLint("InlinedApi")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        supportRequestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY);
        setContentView(R.layout.activity_main);

        mSelectedPlaceTypes.addAll(Arrays.asList(NAV_PLACE_TYPES));
        mPlaces = retrievePlaces();
        
        setupMapIfNeeded();

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);

        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
        
        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout, R.drawable.ic_drawer,
                R.string.app_name, R.string.app_name) {

            public void onDrawerClosed(View view) {
                // getActionBar().setTitle(mTitle);

                if (view.equals(mNavListView)) {
                    // XXX no need to recreate the markers if the place
                    // types haven't changed.
                    mMap.clear();
                    mPlaces = retrievePlaces();

                    if (mPlaceViewPager != null) {
                        mPlaceViewPager.getAdapter().notifyDataSetChanged();
                        mPlaceViewPager.requestLayout();
                    }

                    addPlacesToMap();
                    setSelectedPlace(0);
                    showPlacesOnMap(true); // overrides animation from
                                           // setSelectedPlace
                    setupPlaceList();
                }
            }

            public void onDrawerOpened(View drawerView) {
                // getActionBar().setTitle(mDrawerTitle);
            }
        };

        mDrawerLayout.setDrawerListener(mDrawerToggle);
        
        setupPlaceViewPager();
        setupPlaceList();
        setupNavList();
        
        // Set the map padding once the UI views have dimensions.
        ViewTreeObserver observer = mDrawerLayout.getViewTreeObserver();
        observer.addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                removeListener();

                // Pad the top of the map to accommodate the transparent action
                // bar.
                TypedValue tv = new TypedValue();
                int actionbarHeight = 0;
                if (getTheme().resolveAttribute(R.attr.actionBarSize, tv, true))
                {
                    actionbarHeight = TypedValue.complexToDimensionPixelSize(tv.data,
                            getResources().getDisplayMetrics());
                }

                // Pad the bottom of the map to accommodate the place viewpager.
                int viewPagerHeight = 0;
                if (mPlaceViewPager != null) {
                    viewPagerHeight = mPlaceViewPager.getHeight();
                }

                // If the place list isn't in a DrawerLayout, pad the map for it.
                int placeListWidth = 0;
                // Use if listview overlays map.
//                if (mPlaceListView.getVisibility() == View.VISIBLE) {
//                    placeListWidth = mPlaceListView.getWidth();
//                }
                
                mMap.setPadding(0, actionbarHeight, placeListWidth, viewPagerHeight);
                
                // Once the map is ready, add the markers to it.
                addPlacesToMap();
                showPlacesOnMap(false);
                setSelectedPlace(0);
            }

            @SuppressWarnings("deprecation")
            @SuppressLint("NewApi")
            private void removeListener() {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                    mDrawerLayout.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                } else {
                    mDrawerLayout.getViewTreeObserver().removeGlobalOnLayoutListener(this);
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        setupMapIfNeeded();
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mDrawerToggle.syncState();
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mDrawerToggle.onConfigurationChanged(newConfig);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (mDrawerToggle.onOptionsItemSelected(item)) {
          return true;
        }

        return super.onOptionsItemSelected(item);
    }

    
    @Override
    public void onBackPressed() {
        if (!mMap.getCameraPosition().equals(mMapInitPosition)) {
            showPlacesOnMap(true);
        } else {
            super.onBackPressed();
        }
    }

    private void setupMapIfNeeded() {
        if (mMap == null) {
            mMap = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map))
                    .getMap();

            if (mMap != null) {
                setupMap();
            }
        }
    }

    private void setupMap() {
        Log.d(LOG_TAG, "setupMap()");
        
        // Setup the map.
        FragmentManager fragmentManager = getSupportFragmentManager();
        SupportMapFragment mapFragment = (SupportMapFragment) fragmentManager
                .findFragmentById(R.id.map);

        mMap = mapFragment.getMap();
        mMap.getUiSettings().setZoomControlsEnabled(false);

        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                // This is a slow(ish) operation, but works for a few markers.
                int index = Arrays.asList(mMarkers).indexOf(marker);

                // If the currently selected place was re-selected, the show place details.
                if (mSelectedPlaceIndex == index) {
                    showPlaceDetails(index);
                }
                
                setSelectedPlace(index);
                
                return true;
            }
        });
    }

    private void setupPlaceViewPager() {
        Log.d(LOG_TAG, "setupPlaceViewPager()");
        
        mPlaceViewPager = (ViewPager) findViewById(R.id.place_viewpager);
        
        if (mPlaceViewPager == null || mPlaceViewPager.getVisibility() == View.INVISIBLE) {
            return;
        }
        
        mPlaceViewPager.setAdapter(new PlacePagerAdapter());

        // Add an OnPageChangeListener so that we can change which marker has
        // focus as the page changes.
        mPlaceViewPager.setOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            // When a new page is displayed, change the highlighted marker.
            @Override
            public void onPageSelected(int index) {
                setSelectedPlace(index);
            }

            @Override
            public void onPageScrollStateChanged(int state) {
            }

            @Override
            public void onPageScrolled(int index, float positionOffset, int positionOffsetPixels) {
            }
        });
    }
    
    private void setupPlaceList() {
        Log.d(LOG_TAG, "setupPlaceList()");
        
        mPlaceListView = (ListView) this.findViewById(R.id.place_list);
        ArrayAdapter<Place> placeAdapter = new PlaceArrayAdapter<Place>(this,
                R.layout.listview_item);
        mPlaceListView.setAdapter(placeAdapter);

        for (Place place : mPlaces) {
            placeAdapter.add(place);
        }

        mPlaceListView.setOnItemClickListener(new ListView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int index, long id) {
                // cannot use closeDrawer(mPlaceListView) since the list might not be a drawer.
                mDrawerLayout.closeDrawers();
                
                if (index == mSelectedPlaceIndex) {
                    showPlaceDetails(index);
                } else {
                    setSelectedPlace(index);
                }
            }
        });
        
        View rightHandle = findViewById(R.id.right_handle);
        if (rightHandle != null && rightHandle.getVisibility() == View.VISIBLE) {
            rightHandle.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    mDrawerLayout.openDrawer(mPlaceListView);
                }
            });
        }
    }
    
    private void setupNavList() {
        Log.d(LOG_TAG, "setupNavList()");
        
        mNavListView = (ListView) this.findViewById(R.id.nav_list);
        ArrayAdapter<String> navAdapter = new NavArrayAdapter<String>(this, R.layout.listview_item);
        mNavListView.setAdapter(navAdapter);

        for (PlaceType placeType : NAV_PLACE_TYPES) {
            navAdapter.add(placeType.name().toLowerCase(Locale.US));
        }
        navAdapter.add("div"); // Divider
        navAdapter.add("about"); // TODO create constants

        final int aboutIndex = navAdapter.getCount() - 1; // TODO should have a better way
        
        mNavListView.setOnItemClickListener(new ListView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int index, long id) {
                if (index == aboutIndex) {
                    mDrawerLayout.closeDrawer(mNavListView);
                    Intent intent = new Intent(MainActivity.this, AboutActivity.class);
                    startActivity(intent);
                }
            }
        });

        View leftHandle = findViewById(R.id.left_handle);
        leftHandle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mDrawerLayout.openDrawer(mNavListView);
            }
        });
    }
    
    /**
     * Show place details and street-level map for place.
     * 
     * @param index The index of the place in the PLACES collection.
     */
    private void showPlaceDetails(int index) {
        Log.d(LOG_TAG, String.format("showPlaceDetails(%d)", index));
        
        float zoomLevel = getResources().getInteger(R.integer.place_detail_zoom);
        int zoomTime = getResources().getInteger(R.integer.place_detail_zoom_ms);
        int tilt = getResources().getInteger(R.integer.place_detail_tilt);
        
        Place place = mPlaces.get(index);
        LatLng latlng = new LatLng(place.lat, place.lng);
        
        CameraPosition pos = CameraPosition.builder().tilt(tilt).target(latlng).zoom(zoomLevel)
                .build();
        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(pos), zoomTime, null);
    }
    
    private void setSelectedPlace(int index) {
        Log.d(LOG_TAG, String.format("showSelectedPlace(%d)", index));
        
        // Toggle the selected place in the listview.
        mPlaceListView.getChildAt(mSelectedPlaceIndex).setSelected(false);
        mPlaceListView.getChildAt(index).setSelected(true);
        
        if (mPlaceViewPager != null) {
            mPlaceViewPager.setCurrentItem(index);
        }
        
        // TODO reset the map zoom if details previously displayed
        
        // Set the non-selected place markers to a dots.
        mMarkers[mSelectedPlaceIndex].setIcon(BitmapDescriptorFactory
                .fromBitmap(getDotMarkerBitmap()));
        mMarkers[mSelectedPlaceIndex].setAnchor(.5f, .5f);
        
        // Replace the currently selected maker with the full marker.
        float hue = this.getResources().getInteger(R.integer.place_marker_hue);
        mMarkers[index].setIcon(BitmapDescriptorFactory.defaultMarker(hue));
        mMarkers[index].setAnchor(.5f, 1f);
        
        // Determine if the marker is in the middle 80% of the map view.
        LatLng coords = mMarkers[index].getPosition();
        Point point = mMap.getProjection().toScreenLocation(coords);
        
        View view = ((SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map)).getView();
        
        // XXX dupe code
        
        TypedValue tv = new TypedValue();
        int actionbarHeight = 0;
        if (getTheme().resolveAttribute(R.attr.actionBarSize, tv, true))
        {
            actionbarHeight = TypedValue.complexToDimensionPixelSize(tv.data,
                    getResources().getDisplayMetrics());
        }

        // Pad the bottom of the map to accommodate the place viewpager.
        int viewPagerHeight = 0;
        if (mPlaceViewPager != null) {
            viewPagerHeight = mPlaceViewPager.getHeight();
        }
        
        // XXX end dupe code
        
        int width = view.getWidth();
        int height = view.getHeight() - viewPagerHeight - actionbarHeight;
        point.y -= actionbarHeight;
        
        if (point.x < .2 * width || point.x > .8 * width || point.y < .2 * height
                || point.y > .8 * height) {
            int recenterTime = getResources().getInteger(R.integer.map_recenter_ms);
            mMap.animateCamera(CameraUpdateFactory.newLatLng(coords), recenterTime, null);
        }
              
        mSelectedPlaceIndex = index;
    }
    
    public void onPlaceTypeSelected(View view) {
        // Get the index of the row that contains the checkbox.
//        int index = mNavListView.getSelectedItemPosition();
        int index = mNavListView.indexOfChild((View) view.getParent());
        Place.PlaceType placeType = Place.PlaceType.values()[index];
        
        if (((CheckBox) view).isChecked()) {
            mSelectedPlaceTypes.add(placeType);
        } else {
            mSelectedPlaceTypes.remove(placeType);
        }
    }
    
    private List<Place> retrievePlaces() {
        Place.PlaceType[] placeTypes = mSelectedPlaceTypes.toArray(new Place.PlaceType[mSelectedPlaceTypes.size()]); 
        return PlacesService.getPlacesByType(placeTypes);
    }
    
    private void addPlacesToMap() {
        Log.d(LOG_TAG, "addPlacesToMap()");
        
        int i = 0;
        mMarkers = new Marker[mPlaces.size()];

        // Create the a marker for each of the places.
        for (Place place : mPlaces) {
            LatLng point = new LatLng(place.lat, place.lng);
            Marker marker = mMap.addMarker(new MarkerOptions()
                    .position(point)
                    .anchor(.5f, .5f)
                    .icon(BitmapDescriptorFactory.fromBitmap(getDotMarkerBitmap())));
            
            mMarkers[i++] = marker;
        }
    }
    
    private void showPlacesOnMap(boolean animate) {
        Log.d(LOG_TAG, String.format("showPlaceOnMap(%b)", animate));
        
        LatLngBounds.Builder builder = LatLngBounds.builder();

        for (Marker marker : mMarkers) {
            builder.include(marker.getPosition());
        }

        int px = getResources().getDimensionPixelSize(R.dimen.map_padding);

        if (animate) {
            int recenterTime = getResources().getInteger(R.integer.map_recenter_ms);
            mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), px),
                    recenterTime, null);
        } else {
            mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), px));
        }

        mMapInitPosition = mMap.getCameraPosition();
    }

    private Bitmap getDotMarkerBitmap() {    
        Log.d(LOG_TAG, "getDotMarkerBitmap()");
        
        if (mDotMarkerBitmap == null || mDotMarkerBitmap.isRecycled()) {
            // Create a marker bitmap from the dot shape drawable.
            int px = getResources().getDimensionPixelSize(R.dimen.map_dot_marker_size);
            mDotMarkerBitmap = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(mDotMarkerBitmap);
            Drawable shape = getResources().getDrawable(R.drawable.map_dot_red);
            shape.setBounds(0, 0, mDotMarkerBitmap.getWidth(), mDotMarkerBitmap.getHeight());
            shape.draw(canvas);            
        }
        
        return mDotMarkerBitmap;
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

            View view = inflater.inflate(R.layout.viewpager_place, null, false);

            // If the view is clicked, zoom in on its corresponding marker.
            view.setOnClickListener(new OnClickListener() {
                public void onClick(View view) {
                    showPlaceDetails(position);
                }
            });

            Place place = mPlaces.get(position);
            ((TextView) view.findViewById(R.id.place_name)).setText(place.name);

            ((ViewPager) collection).addView(view, 0);

            return view;
        }

        @Override
        public int getCount() {
            return mPlaces.size();
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

    private class NavArrayAdapter<T> extends ArrayAdapter<String> {
        int listItemResource;

        public NavArrayAdapter(Context context, int resource) {
            super(context, resource);
            
            this.listItemResource = resource;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater = (LayoutInflater) getContext()
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            String placeTypeStr = getItem(position);
            
            if ("div".equals(placeTypeStr)) {
                return inflater.inflate(R.layout.listview_divider, parent, false);
            }
            
            View rowView = inflater.inflate(listItemResource, parent, false);
            TextView textView = (TextView) rowView.findViewById(R.id.text1);
            ImageView imageView = (ImageView) rowView.findViewById(R.id.icon);
            
            if ("about".equals(placeTypeStr)) {
                textView.setText(R.string.about);
                imageView.setImageResource(R.drawable.icon_park);
            } else {
                // Get the pluralized string for the place type (e.g. museum_pl = "Museums").
                int resId = getResources().getIdentifier(placeTypeStr + "_pl", "string",
                        getPackageName());
                textView.setText(getResources().getString(resId));

                // Get the icon for the place type (e.g. icon_museum).
                resId = getResources().getIdentifier("icon_" + placeTypeStr, "drawable",
                        getPackageName());
                imageView.setImageResource(resId);
                
                // Make the checkbox visible.
                rowView.findViewById(R.id.place_type).setVisibility(View.VISIBLE);
            }
            
            return rowView;
        }
    }
    
    private class PlaceArrayAdapter<T extends Place> extends ArrayAdapter<T> {
        private int listItemResource;

        public PlaceArrayAdapter(Context context, int resource) {
            super(context, resource);

            this.listItemResource = resource;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            LayoutInflater inflater = (LayoutInflater) getContext()
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

            Place place = getItem(position);
            View rowView = inflater.inflate(listItemResource, parent, false);

            TextView textView = (TextView) rowView.findViewById(R.id.text1);
            textView.setText(place.name);

            String placeTypeStr = place.type.name().toLowerCase(Locale.US);
            int resId = getResources().getIdentifier("icon_" + placeTypeStr, "drawable", getPackageName());
            ImageView imageView = (ImageView) rowView.findViewById(R.id.icon);
            imageView.setImageResource(resId);

            return rowView;
        }
    }
}
