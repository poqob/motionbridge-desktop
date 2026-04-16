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
                case "TAP_4":
                    return gson.fromJson(jsonObject, Tap4Event.class);
                case "DICT":
                    return gson.fromJson(jsonObject, DictationEvent.class);
                case "CLIP":
                    return gson.fromJson(jsonObject, ClipboardEvent.class);
                case "VOL":
                    return gson.fromJson(jsonObject, VolumeEvent.class);
                case "MUTE":
                    return gson.fromJson(jsonObject, MuteEvent.class);
                case "MEDIA":
                    return gson.fromJson(jsonObject, MediaEvent.class);
                case "SYS":
                    return gson.fromJson(jsonObject, SystemEvent.class);
                case "COPY":
                    return gson.fromJson(jsonObject, CopyEvent.class);
                case "PASTE":
                    return gson.fromJson(jsonObject, PasteEvent.class);
                case "AM_MODE":
                    return gson.fromJson(jsonObject, AMModeEvent.class);
                case "AM_SENS":
                    return gson.fromJson(jsonObject, AMSensEvent.class);
                case "AM_M":
                    return gson.fromJson(jsonObject, AMMoveEvent.class);
                default:
                    return null;
            }
        } catch (Exception e) {
            System.err.println("Failed to parse event JSON: " + jsonString);
            return null;
        }
    }
}
