package com.spaceproject.utility;

import com.badlogic.ashley.core.Entity;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.GdxRuntimeException;
import com.badlogic.gdx.utils.Json;

public class SaveUtil {

    public static void saveToJson(long seed, Object object, boolean prettyPrint) {
        //save: /save/<date?><universe_seed>/
        //-universe seed
        //-ship levels and components
        //-last docked station
        //-stats
        //-planets textures - /save/<date?><universe_seed>/<planet_seed>.png

        String fileName = "save/" + seed + ".json";

        Json json = new Json();
        json.setUsePrototypes(false);
        //ECSUtil.printEntity((Entity) object);
        String jsonString = json.toJson(object);
        if (prettyPrint) {
            jsonString = json.prettyPrint(jsonString);
        }

        try {
            FileHandle keyFile = Gdx.files.local(fileName);
            keyFile.writeString(jsonString, false);
            Gdx.app.log(SaveUtil.class.getSimpleName(), "Saved: " + fileName);
            Gdx.app.debug(SaveUtil.class.getSimpleName(), jsonString);
        } catch (GdxRuntimeException ex) {
            Gdx.app.error(SaveUtil.class.getSimpleName(), "Could not save file: " + fileName , ex);
        }
    }

}
