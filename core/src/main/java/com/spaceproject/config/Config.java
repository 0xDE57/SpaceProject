package com.spaceproject.config;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.SerializationException;

public abstract class Config {
    
    private final String fileName = "config/" + getClass().getSimpleName() + ".json";
    
    public abstract void loadDefault();
    
    public void saveToJson() {
        saveToJson(true);
    }
    
    public void saveToJson(boolean prettyPrint) {
        Json json = new Json();
        json.setUsePrototypes(false);
        String jsonString = json.toJson(this);
        if (prettyPrint) {
            jsonString = json.prettyPrint(jsonString);
        }

        try {
            FileHandle keyFile = Gdx.files.local(fileName);
            keyFile.writeString(jsonString, false);
            Gdx.app.log(getClass().getSimpleName(), "Saved: " + fileName);
            Gdx.app.debug(getClass().getSimpleName(), jsonString);
        } catch (GdxRuntimeException ex) {
            Gdx.app.error(getClass().getSimpleName(), "Could not save file: " + fileName , ex);
        }
    }
    
    public Config loadFromJson() {
        FileHandle keyFile = Gdx.files.local(fileName);
        String objectName = getClass().getSimpleName();
        
        try {
            if (keyFile.exists()) {
                Json json = new Json();
                json.setUsePrototypes(false);
                
                Config config = json.fromJson(getClass(), keyFile.readString());
                Gdx.app.log(objectName, "Loaded " + objectName + " from " + keyFile.path());
                Gdx.app.debug(objectName, json.toJson(config));
                return config;
            } else {
                Gdx.app.log(objectName, "Could not find file: " + fileName);
            }
        } catch (SerializationException e) {
            Gdx.app.error(objectName, "Could not load: " + fileName, e);
            //todo: handle missing field
            //com.badlogic.gdx.utils.SerializationException: Field not found: scale
            //if (e.detailMessage.contains(Field not found:) {
            //  extract field
            //  load default (badField = new Config.loadDefault().badField)
            //}
            //if (extra field) {
            //  dont load field?
        } catch (Exception e) {
            Gdx.app.error(objectName, "Could not load: " + fileName, e);
        }
        
        return null;
    }
    
}
