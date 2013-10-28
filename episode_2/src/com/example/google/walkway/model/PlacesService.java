package com.example.google.walkway.model;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeSet;

public class PlacesService {
    /**
     * Temporary list of places that we'd list to show on the map.
     */
    private static final ArrayList<Place> PLACES = new ArrayList<Place>();

    /* Initialize the static collection. */
    static {
        PLACES.add(new Place("Ferry Building", 37.7955, -122.3937, Place.PlaceType.SHOP));
        PLACES.add(new Place("Exploratorium", 37.801434, -122.397561, Place.PlaceType.MUSEUM));
        PLACES.add(new Place("Greenwich Street Stairs", 37.8030764, -122.4035185, Place.PlaceType.PARK));
        PLACES.add(new Place("Coit Tower", 37.8025, -122.405833, Place.PlaceType.MONUMENT));
        PLACES.add(new Place("Dragon (Chinatown) Gate", 37.790582, -122.405624, Place.PlaceType.MONUMENT));
        PLACES.add(new Place("Union Square", 37.788056, -122.4075, Place.PlaceType.PARK));
        PLACES.add(new Place("Yerba Buena Gardens", 37.785607, -122.402691, Place.PlaceType.PARK));
    }
    
    public static List<Place> getPlacesByType(Place.PlaceType... types) {
        List<Place> places = new LinkedList<Place>();
        
        TreeSet<Place.PlaceType> typeTree = new TreeSet<Place.PlaceType>();
        for (Place.PlaceType type : types) {
            typeTree.add(type);
        }
        
        for (Place place : PLACES) {
            if (typeTree.contains(place.type)) {
                places.add(place);
            }
        }
        
        return places;
    }
}
