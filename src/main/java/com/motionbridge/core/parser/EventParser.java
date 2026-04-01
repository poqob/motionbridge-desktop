package com.motionbridge.core.parser;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.motionbridge.core.models.*;

public class EventParser {
    private static final Gson gson = new Gson();

    public static MBEvent parse(String jsonString) {
        try {
            JsonObject jsonObject = JsonParser.parseString(jsonString).getAsJsonObject();
            if (!jsonObject.has("t")) {
                return null; // Unknown format
            }

            String type = jsonObject.get("t").getAsString();
            switch (type) {
                case "M":
                    return gson.fromJson(jsonObject, MouseMoveEvent.class);
                case "C":
                    return gson.fromJson(jsonObject, MouseClickEvent.class);
                case "S":
                    return gson.fromJson(jsonObject, ScrollEvent.class);
                case "D":
                    return gson.fromJson(jsonObject, DimmerEvent.class);
                case "DRAG":
                    return gson.fromJson(jsonObject, MouseDragEvent.class);
                case "DRAG_START":
                    return gson.fromJson(jsonObject, MouseDragStartEvent.class);
                case "DRAG_END":
                    return gson.fromJson(jsonObject, MouseDragEndEvent.class);
                case "DOUBLE_CLICK":
                    return gson.fromJson(jsonObject, MouseDoubleClickEvent.class);
                case "SWIPE_3":
                    return gson.fromJson(jsonObject, Swipe3Event.class);
                default:
                    return null;
            }
        } catch (Exception e) {
            System.err.println("Failed to parse event JSON: " + jsonString);
            return null;
        }
    }
}
