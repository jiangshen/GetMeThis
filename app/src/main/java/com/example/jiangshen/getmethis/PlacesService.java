package com.example.jiangshen.getmethis;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Created by Jesse on 10/24/2015.
 */
public class PlacesService {

    private String API_KEY;

    //Google Preferred Types
    private final ArrayList<String> types = new ArrayList<String>(Arrays.asList(new String[] {
            "accounting", "airport", "amusement_park", "aquarium", "art_gallery", "atm", "bakery",
            "bank", "bar", "beauty_salon", "bicycle_store","book_store","bowling_alley", "bus_station",
            "cafe campground", "car_dealer", "car_rental", "car_repair", "car_wash", "casino cemetery",
            "church", "city_hall", "clothing_store", "convenience_store", "courthouse", "dentist",
            "department_store", "doctor", "electrician", "electronics_store", "embassy", "establishment",
            "finance", "fire_station", "florist", "food", "funeral_home", "furniture_store", "gas_station",
            "general_contractor", "grocery_or_supermarket", "gym", "hair_care", "hardware_store", "health",
            "hindu_temple", "home_goods_store", "hospital", "insurance_agency", "jewelry_store", "laundry",
            "lawyer", "library", "liquor_store", "local_government_office", "locksmith","lodging",
            "meal_delivery","meal_takeaway","mosque","movie_rental","movie_theater","moving_company",
            "museum", "night_club", "painter", "park", "parking", "pet_store", "pharmacy", "physiotherapist",
            "place_of_worship","plumber","police","post_office","real_estate_agency","restaurant",
            "roofing_contractor","rv_park","school","shoe_store","shopping_mall","spa","stadium",
            "storage", "store", "subway_station", "synagogue", "taxi_stand", "train_station", "travel_agency",
            "university", "veterinary_care", "zoo",
    }));

    public PlacesService(String apikey) {
        this.API_KEY = apikey;
    }

    public void setApiKey(String apikey) {
        this.API_KEY = apikey;
    }

    public List<Place> findPlaces(double latitude, double longitude, String keyword)
    {
        String urlString = makeUrl(latitude, longitude, keyword);

        try {
            String json = getJSON(urlString);

            System.out.println(json);
            JSONObject object = new JSONObject(json);
            JSONArray array = object.getJSONArray("results");

            ArrayList<Place> arrayList = new ArrayList<Place>();
            for (int i = 0; i < array.length(); i++) {
                try {
                    Place place = Place.jsonToPontoReferencia((JSONObject) array.get(i));
                    Log.v("Places Services ", "" + place);

                    arrayList.add(place);
                } catch (Exception e) {
                    Log.d("PlacesService", e.getMessage());
                }
            }
            return arrayList;
        } catch (JSONException ex) {
            Logger.getLogger(PlacesService.class.getName()).log(Level.SEVERE, null, ex);
        }
        return null;
    }

    private String makeUrl(double latitude, double longitude, String keyword) {
        String[] words = keyword.split(", ");
        StringBuilder urlString = new StringBuilder("https://maps.googleapis.com/maps/api/place/search/json?");
        StringBuilder type = new StringBuilder();
        StringBuilder keywords = new StringBuilder();
        for (String s : words) {
            if (types.contains(s)) {
                type.append(type.length() > 0 ? "|" : "").append(s);
            } else {
                keywords.append(keywords.length() > 0 ? "|" : "").append(s);
            }
        }

        //if (place.equals("")) {
        urlString.append("&location=");
        urlString.append(Double.toString(latitude));
        urlString.append(",");
        urlString.append(Double.toString(longitude));
        urlString.append("&radius=5000");
//            if (!place.equals("")) {
//                urlString.append("&types="+place);
//            }
        if (!type.toString().equals("")) {
            urlString.append("&types=" + type.toString());
        }
        if (!keywords.toString().equals("")) {
            urlString.append("&keyword="+keywords.toString());
        }
        urlString.append("&key=" + API_KEY);
        //} else {
//            urlString.append("&location=");
//            urlString.append(Double.toString(latitude));
//            urlString.append(",");
//            urlString.append(Double.toString(longitude));
//            urlString.append("&radius=100000");
//            urlString.append("&types="+place);
//            urlString.append("&sensor=false&key=" + API_KEY);
//        }

        return urlString.toString();
        //return "https://maps.googleapis.com/maps/api/place/radarsearch/json?location=48.859294,2.347589&radius=5000&key=AIzaSyDWy1BBZPKzuXcEd_aD32Uqu4CipbbMcC0 ";
        //return "https://maps.googleapis.com/maps/api/place/search/json?location=-33.8670522,151.1957362&radius=500&sensor=true&key=AIzaSyDWy1BBZPKzuXcEd_aD32Uqu4CipbbMcC0";
    }

    protected String getJSON(String url) {
        return getUrlContents(url);
    }

    private String getUrlContents(String theUrl)
    {
        StringBuilder content = new StringBuilder();

        try {
            URL url = new URL(theUrl);
            URLConnection urlConnection = url.openConnection();
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()), 8);
            String line;
            while ((line = bufferedReader.readLine()) != null)
            {
                content.append(line + "\n");
            }
            bufferedReader.close();
        }

        catch (Exception e)
        {
            e.printStackTrace();
        }
        return content.toString();
    }
}