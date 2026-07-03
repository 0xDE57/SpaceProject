package com.spaceproject.generation;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
import com.badlogic.gdx.utils.GdxRuntimeException;

public class FontLoader {
    
    // fonts
    public static final String fontPressStart = "fonts/pressstart/PressStart2P.ttf";
    
    public static final String fontComfortaaLight = "fonts/comfortaa/ComfortaaLight.ttf";
    public static final String fontComfortaaRegular = "fonts/comfortaa/ComfortaaRegular.ttf";
    public static final String fontComfortaaBold = "fonts/comfortaa/ComfortaaBold.ttf";
    
    public static final String fontBitstreamVM = "fonts/bitstream/VeraMono.ttf";
    public static final String fontBitstreamVMBoldItalic = "fonts/bitstream/VeraMono-Bold-Italic.ttf";
    public static final String fontBitstreamVMBold = "fonts/bitstream/VeraMono-Bold.ttf";
    public static final String fontBitstreamVMItalic = "fonts/bitstream/VeraMono-italic.ttf";
    
    //skin resource/style name
    public static final String skinSmallFont = "smallFont";
    
    public static BitmapFont createFont(String font, FreeTypeFontParameter parameter) {
        FreeTypeFontGenerator generator;
        BitmapFont newFont;
        
        try {
            generator = new FreeTypeFontGenerator(Gdx.files.internal(font));
            newFont = generator.generateFont(parameter);
            generator.dispose();
            return newFont;
        } catch (GdxRuntimeException ex) {
            String msg = "Font not found: " + Gdx.files.getLocalStoragePath() + font;
            msg += ". Make sure '../assets' is appended to run configuration.";
            Gdx.app.error(FontLoader.class.getSimpleName(), msg, ex);
            //https://github.com/libgdx/libgdx/wiki/Gradle-and-Intellij-IDEA
            //Set Working directory to <project_path>/assets/
        }
        
        Gdx.app.log(FontLoader.class.getSimpleName(), "Loaded default font.");
        return new BitmapFont();
    }
    
    public static BitmapFont createFont(String font, int size) {
        FreeTypeFontParameter parameter = new FreeTypeFontParameter();
        parameter.size = size;
        return createFont(font, parameter);
    }
    
}
